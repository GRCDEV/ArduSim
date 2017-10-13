package mbcap.logic;

import java.awt.geom.Point2D;
import java.io.File;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.javatuples.Pair;

import api.API;
import api.GUIHelper;
import api.MissionHelper;
import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Param.Protocol;
import main.Text;
import mbcap.gui.MBCAPGUITools;
import mbcap.gui.MBCAPGUIParam;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.pojo.Beacon;
import mbcap.pojo.PointTime;
import mbcap.pojo.ProgressState;
import sim.logic.SimParam;
import uavController.UAVParam;

/** This class contains exclusively static methods used by the MBCAP protocol. */

public class MBCAPHelper {
	
	/** Initializes protocol specific data structures when the simulator starts. */
	@SuppressWarnings("unchecked")
	public static void initializeProtocolDataStructures() {
		MBCAPGUIParam.predictedLocation = new AtomicReferenceArray<List<Point3D>>(Param.numUAVs);

		MBCAPParam.event = new AtomicIntegerArray(Param.numUAVs);
		MBCAPParam.state = new MBCAPState[Param.numUAVs];
		MBCAPParam.idAvoiding = new AtomicLongArray(Param.numUAVs);
		MBCAPParam.projectPath = new AtomicIntegerArray(Param.numUAVs);

		MBCAPParam.selfBeacon = new AtomicReferenceArray<Beacon>(Param.numUAVs);
		MBCAPParam.beacons = new ConcurrentHashMap[Param.numUAVs];
		MBCAPParam.impactLocationUTM = new ConcurrentHashMap[Param.numUAVs];
		MBCAPParam.impactLocationPX = new ConcurrentHashMap[Param.numUAVs];

		MBCAPParam.targetPointUTM = new Point2D.Double[Param.numUAVs];
		MBCAPParam.targetPointGeo = new GeoCoordinates[Param.numUAVs];
		MBCAPParam.beaconsStored = new ArrayList[Param.numUAVs];

		for (int i = 0; i < Param.numUAVs; i++) {
			MBCAPParam.state[i] = MBCAPState.NORMAL;
			MBCAPParam.idAvoiding.set(i, MBCAPParam.ID_AVOIDING_DEFAULT);
			MBCAPParam.projectPath.set(i, 1);		// Begin projecting the predicted path over the theoretical mission

			MBCAPParam.beacons[i] = new ConcurrentHashMap<Long, Beacon>();
			MBCAPParam.impactLocationUTM[i] = new ConcurrentHashMap<Long, Point3D>();
			MBCAPParam.impactLocationPX[i] = new ConcurrentHashMap<Long, Point2D.Double>();
			
			MBCAPParam.beaconsStored[i] =  new ArrayList<Beacon>();
		}

		MBCAPParam.progress = new ArrayList[Param.numUAVs];

		for (int i=0; i < Param.numUAVs; i++) {
			MBCAPParam.progress[i] = new ArrayList<ProgressState>();
		}
	}

	/** Executes the protocol threads. */
	public static void startMBCAPThreads() {
		BeaconingThread thread;
		try {
			(new BrokerThread()).start();
			for (int i = 0; i < Param.numUAVs; i++) {
				thread = new BeaconingThread(i);
				Param.controllers[i].setWaypointReached(thread);
				thread.start();
				(new CollisionDetectorThread(i)).start();
			}
			MissionHelper.log(MBCAPText.ENABLING);
		} catch (SocketException | UnknownHostException e) {
			GUIHelper.exit(Text.THREAD_START_ERROR);
		}
	}
	
	/** Predicts the future positions of the UAV. Returns empty list in case of problems. */
	public static List<Point3D> getPredictedPath(int numUAV, double speed, double acceleration, Point2D.Double currentUTMLocation, double currentZ) {
		List<Point3D> predictedPath = new ArrayList<Point3D>(MBCAPParam.POINTS_SIZE);
		// Multiple cases to study:
		
		// 1. At the beginning, without GPS, the initial position can be null
		if (currentUTMLocation == null) {
			return predictedPath;
		}
		
		// 2. If the UAV is in the "go on, please" state, only the current position and the predicted collision risk location are sent
		if (MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE) {
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			Point3D riskLocation = MBCAPParam.impactLocationUTM[numUAV].get(MBCAPParam.selfBeacon.get(numUAV).idAvoiding);
			if (riskLocation != null) {
				predictedPath.add(riskLocation);
			} else {
				predictedPath.add(new Point3D(0, 0, 0));	// If no risk point was detected, a fake point is sent
			}
			return predictedPath;
		}

		// 3. If moving slowly or still but not in the "stand still" state, only the current position is sent
		if (speed < MBCAPParam.minSpeed && MBCAPParam.state[numUAV] != MBCAPState.STAND_STILL) {
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			return predictedPath;
		}

		// 4. If the UAV is moving aside, send the current position and prediction towards the destination safe point
		if (MBCAPParam.state[numUAV] == MBCAPState.MOVING_ASIDE) {
			Point2D.Double destination = MBCAPParam.targetPointUTM[numUAV];
			double length = currentUTMLocation.distance(destination);
			int numLocations1 = (int)Math.ceil(length/(speed * MBCAPParam.hopTime));
			int numLocations2 = (int) Math.ceil(MBCAPParam.beaconFlyingTime / MBCAPParam.hopTime);
			int numLocations = Math.min(numLocations1, numLocations2);
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			double xUTM, yUTM;
			double incX = (destination.x - currentUTMLocation.x) * (speed * MBCAPParam.hopTime)/length;
			double incY = (destination.y - currentUTMLocation.y) * (speed * MBCAPParam.hopTime)/length;
			for (int i=1; i<numLocations; i++) {
				xUTM = currentUTMLocation.x + i*incX;
				yUTM = currentUTMLocation.y + i*incY;
				predictedPath.add(new Point3D(xUTM, yUTM, currentZ));
			}
			return predictedPath;
		}

		// Reasonable hypothesis: The UAV moves in the waypoint list in increasing order
		// First of all, we have to locate the waypoint the UAV is moving toward (posNextWaypoint)
		int currentWaypoint = UAVParam.currentWaypoint.get(numUAV);
		List<WaypointSimplified> mission = UAVParam.missionUTMSimplified.get(numUAV);
		if (mission != null) {
			int posNextWaypoint = 0;
			boolean found = false;
			for (int i = 0; i < mission.size() && !found; i++) {
				if (mission.get(i).numSeq >= currentWaypoint) {
					posNextWaypoint = i + 1;
					found = true;
				}
			}
			if (!found) {
				MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.WAYPOINT_LOST);
				return predictedPath;
			}
			
			// 5. In the stand still state, send the waypoint list so the other UAV could decide whether to move aside or not
			if (MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL) {
				// The first point is the current position
				predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
				// The rest of the points are the waypoints not reached jet
				for (int i=posNextWaypoint; i<mission.size(); i++) {
					predictedPath.add(new Point3D(mission.get(i).x, mission.get(i).y, mission.get(i).z));
				}
				return predictedPath;
			}

			// 6. When really close to the last waypoint, only the current location is sent
			if (posNextWaypoint == mission.size()) {
				posNextWaypoint--; // So it could be drawn until the end
				if (currentUTMLocation.distance(mission.get(posNextWaypoint)) < MBCAPParam.DISTANCE_TO_MISSION_END) {
					predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
					return predictedPath;
				}
			}

			// 7. General case
			// Initial location calculated depending on the protocol version
			Pair<Point2D.Double, Integer> currentLocation = MBCAPHelper.getCurrentLocation(numUAV, mission, currentUTMLocation, posNextWaypoint);
			currentUTMLocation = currentLocation.getValue0();
			posNextWaypoint = currentLocation.getValue1();
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			// Calculate the rest of the points depending on the protocol version
			if (Param.selectedProtocol == Protocol.MBCAP_V1
					|| Param.selectedProtocol == Protocol.MBCAP_V2
					|| Param.selectedProtocol == Protocol.MBCAP_V3) {
				MBCAPHelper.getPredictedLocations1(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
			} else if (Param.selectedProtocol == Protocol.MBCAP_V4) {
				MBCAPHelper.getPredictedLocations2(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
			}
			// In case a problem keeps the list empty
			if (predictedPath.size() == 0) {
				predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			}
		}
		return predictedPath;
	}

	/** Calculates the first point of the predicted positions list depending on the protocol version.
	 * <p>It also decides which is the next waypoint the UAV is moving towards, also depending on the protocol version. */
	private static Pair<Point2D.Double, Integer> getCurrentLocation(int numUAV, List<WaypointSimplified> mission, Point2D.Double currentUTMLocation, int posNextWaypoint) {
		Point2D.Double baseLocation = null;
		int nextWaypointPosition = posNextWaypoint;
		if (Param.selectedProtocol.getId() == 1) { // MBCAP v1
			// The next waypoint position has been already set
			// The first predicted position is the current coordinates
			baseLocation = currentUTMLocation;

		} else if (Param.selectedProtocol.getId() == 2
				|| Param.selectedProtocol.getId() == 3
				|| Param.selectedProtocol.getId() == 4) {
			// MBCAP v2, MBCAP v3 or MBCAP v4
			if (MBCAPParam.projectPath.get(numUAV) == 0) {
				// The next waypoint position has been already set
				// The first predicted position is the current coordinates
				baseLocation = currentUTMLocation;
			} else {
				// It is necessary to project the current location over the planned path
				WaypointSimplified sp1 = null;
				Point2D.Double location1 = null;
				if (posNextWaypoint>1) {
					sp1 = mission.get(posNextWaypoint-2);	// Segment 1 previous point
					location1 = new Point2D.Double(sp1.x, sp1.y);
				}

				// If we are over the first mission segment, the first predicted position is the current coordinates
				if (location1 == null) {
					baseLocation = currentUTMLocation;
					// The next waypoint position has been already set
				} else {
					// In any other case... (there is a previous segment in the planned path)
					// The current location can be projected over the previous or the following segment
					WaypointSimplified sp2 = mission.get(posNextWaypoint-1);	// Segments 1-2 joining point
					Point2D.Double location2 = new Point2D.Double(sp2.x, sp2.y);
					WaypointSimplified sp3 = mission.get(posNextWaypoint);		// Segment 2 final point
					Point2D.Double location3 = new Point2D.Double(sp3.x, sp3.y);
					// Intersection calculus with both segments
					Point2D.Double prevIntersection = MBCAPHelper.getIntersection(currentUTMLocation, location1, location2);
					Point2D.Double postIntersection = MBCAPHelper.getIntersection(currentUTMLocation, location2, location3);
					// Check the segment over which this point is projected
					int projectionCase = -1;	// -1 value over the vertex (when this point projects over the intersection of both segments)
					// 0 value under the vertex (when this point projects under the intersection of both segments)
					// 1 projects over the first segment
					// 2 projects over the second segment
					// 1. Checking the projection over the first segment
					if (Math.abs(location2.getX() - location1.getX()) == 0.0) {
						// Vertical line
						if (prevIntersection.getY() >= Math.min(location1.getY(), location2.getY())
								&& prevIntersection.getY() <= Math.max(location1.getY(), location2.getY())) {
							// It projects over the first segment
							projectionCase = 1;
						}
					} else {
						// Any other case
						if (prevIntersection.getX() >= Math.min(location1.getX(), location2.getX())
								&& prevIntersection.getX() <= Math.max(location1.getX(), location2.getX())) {
							// It projects over the first segment
							projectionCase = 1;
						}
					}
					// 2. Checking the projection over the second segment
					if (Math.abs(location3.getX() - location2.getX()) == 0.0) {
						// Vertical line
						if (postIntersection.getY() >= Math.min(location2.getY(), location3.getY())
								&& postIntersection.getY() <= Math.max(location2.getY(), location3.getY())) {
							// It projects over the first segment
							if (projectionCase == -1) {
								// Only over the second segment
								projectionCase = 2;
							} else {
								// Over both segments
								projectionCase = 0;
							}
						}
					} else {
						// Any other case
						if (postIntersection.getX() >= Math.min(location2.getX(), location3.getX())
								&& postIntersection.getX() <= Math.max(location2.getX(), location3.getX())) {
							// It projects over the first segment
							if (projectionCase == -1) {
								// Only over the second segment
								projectionCase = 2;
							} else {
								// Over both segments
								projectionCase = 0;
							}
						}
					}
					// 3. Considering the four cases
					switch (projectionCase) {
					case 1: // Projected over the first segment
						nextWaypointPosition = posNextWaypoint - 1;
						baseLocation = new Point2D.Double(prevIntersection.getX(), prevIntersection.getY());
						break;
					case 2: // Projected over the second segment
						// posNextWaypoint is the same
						baseLocation = new Point2D.Double(postIntersection.getX(), postIntersection.getY());
						break;
					case -1: // projected out of both segments
						// posNextWaypoint is the same
						baseLocation = new Point2D.Double(location2.getX(), location2.getY());
						break;
					case 0: // projected over both segments, it requires to check distances
						if (currentUTMLocation.distance(prevIntersection) <= currentUTMLocation.distance(postIntersection)) {
							nextWaypointPosition = posNextWaypoint - 1;
							baseLocation = new Point2D.Double(prevIntersection.getX(), prevIntersection.getY());
						} else {
							// posNextWaypoint is the same
							baseLocation = new Point2D.Double(postIntersection.getX(), postIntersection.getY());
						}
						break;
					}
				}
			}
		}
		return Pair.with(baseLocation, nextWaypointPosition);
	}

	/** Calculates the predicted positions in MBCAP protocol, versions 1, 2 y 3 */
	private static void getPredictedLocations1(int numUAV, double speed, double acceleration,
			Point2D.Double currentUTMLocation, List<WaypointSimplified> mission, int posNextWaypoint, int currentWaypoint, double currentZ,
			List<Point3D> predictedPath) {
		// 1. Calculus of the segments over which we have to obtain the points
		List<Double> distances = new ArrayList<Double>(MBCAPParam.DISTANCES_SIZE);
		double totalDistance = 0.0;
		// MBCAP v1 or MBCAP v2 (no acceleration present)
		if (Param.selectedProtocol.getId() == 1 || Param.selectedProtocol.getId() == 2
				|| (Param.selectedProtocol.getId() == 3 && acceleration == 0.0)) {
			totalDistance = MBCAPParam.beaconFlyingTime * speed;
		} else if (Param.selectedProtocol.getId() == 3) {
			// MBCAP v3 (constant acceleration present)
			totalDistance = acceleration*MBCAPParam.beaconFlyingTime*MBCAPParam.beaconFlyingTime/2.0
					+ speed*MBCAPParam.beaconFlyingTime;
		}
		// 2. Calculus of the last waypoint included in the points calculus, and the total distance
		double distanceAcum = 0.0;
		// Distance of the first segment
		distanceAcum = currentUTMLocation.distance(mission.get(posNextWaypoint));
		distances.add(distanceAcum);
		int posLastWaypoint = posNextWaypoint;
		// Distance of the rest of segments until the last waypoint
		if (distanceAcum < totalDistance) {
			double increment = 0;
			for (int i = posNextWaypoint + 1; i < mission.size() && distanceAcum <= totalDistance; i++) {
				increment = mission.get(i - 1).distance(mission.get(i));
				distances.add(increment);
				distanceAcum = distanceAcum + increment;
				posLastWaypoint = i;
			}
		}

		// 3. Predicted points calculus
		int remainingLocations = (int) Math.ceil(MBCAPParam.beaconFlyingTime / MBCAPParam.hopTime);
		double incDistance = 0.0;
		double prevSegmentsLength = 0.0;

		if (Param.selectedProtocol.getId() ==1 || Param.selectedProtocol.getId() == 2
				|| (Param.selectedProtocol.getId() == 3 && acceleration == 0.0)) {
			incDistance = totalDistance / remainingLocations;
		}

		double xp, yp, zp;
		WaypointSimplified prevWaypoint = new WaypointSimplified(currentWaypoint - 1, currentUTMLocation.x, currentUTMLocation.y, currentZ);
		WaypointSimplified nextWaypoint;

		int i = 0;
		int locations = 0;
		double currentSegmentLength;
		distanceAcum = 0;
		double remainingSegment;
		double prevRemainingSegment = 0;
		double longPrev = 0;
		// Following all the distances segments
		while (i < distances.size() && locations < remainingLocations && distanceAcum < totalDistance && posNextWaypoint <= posLastWaypoint) {
			nextWaypoint = mission.get(posNextWaypoint);
			currentSegmentLength = distances.get(i);

			if (Param.selectedProtocol.getId() == 1 || Param.selectedProtocol.getId() == 2
					|| (Param.selectedProtocol.getId() == 3	&& acceleration == 0)) {
				remainingSegment = Math.min(currentSegmentLength, totalDistance - distanceAcum + prevRemainingSegment);

				// Following the current segment
				while (locations < remainingLocations && distanceAcum < totalDistance && remainingSegment >= incDistance) {
					xp = prevWaypoint.x + (nextWaypoint.x - prevWaypoint.x) * (incDistance * (locations + 1)) / currentSegmentLength;
					yp = prevWaypoint.y + (nextWaypoint.y - prevWaypoint.y) * (incDistance * (locations + 1)) / currentSegmentLength;
					zp = prevWaypoint.z + (nextWaypoint.z - prevWaypoint.z) * (incDistance * (locations + 1)) / currentSegmentLength;
					predictedPath.add(new Point3D(xp, yp, zp));
					locations++;
					remainingSegment = remainingSegment - incDistance;
					distanceAcum = distanceAcum + incDistance;
				}

				remainingLocations = remainingLocations - locations;
				prevRemainingSegment = remainingSegment;

				i++;
				posNextWaypoint++;
				prevWaypoint = nextWaypoint;
				locations = 0;
			} else if (Param.selectedProtocol.getId() == 3) {
				// the distance increment has to be calculated each time
				double maxAcumDistance = Math.min(currentSegmentLength + prevSegmentsLength, totalDistance);
				double maxTotalTime = (-speed + Math.sqrt(speed*speed + 2*acceleration*maxAcumDistance))/acceleration;
				int totalHops = (int)Math.floor(maxTotalTime/MBCAPParam.hopTime);

				while (locations < totalHops
						&& longPrev < prevSegmentsLength + currentSegmentLength
						&& longPrev < totalDistance) {
					double time = (locations+1)*MBCAPParam.hopTime;
					double d = acceleration*time*time/2.0 + speed*time;
					incDistance = d - prevSegmentsLength;

					xp = prevWaypoint.x + (nextWaypoint.x - prevWaypoint.x) * incDistance / currentSegmentLength;
					yp = prevWaypoint.y + (nextWaypoint.y - prevWaypoint.y) * incDistance / currentSegmentLength;
					zp = prevWaypoint.z + (nextWaypoint.z - prevWaypoint.z) * incDistance / currentSegmentLength;
					predictedPath.add(new Point3D(xp, yp, zp));
					locations++;

					longPrev = d;
				}
				prevSegmentsLength = prevSegmentsLength + currentSegmentLength;

				i++;
				posNextWaypoint++;
				prevWaypoint = nextWaypoint;
			}
		}
	}

	/** Calculates the predicted position in MBCAP protocol, v 4 */
	private static void getPredictedLocations2(int numUAV, double speed, double acceleration,
			Point2D.Double currentUTMLocation, List<WaypointSimplified> mission, int posNextWaypoint, int currentWaypoint, double currentZ,
			List<Point3D> predictedPath) {
		// Only works with MBCAP version 4

		double distanceAcum;
		List<Double> distances = new ArrayList<Double>(MBCAPParam.DISTANCES_SIZE);
		if (acceleration == 0.0) {
			// The same solution from the previous protocol versions
			double totalDistance = MBCAPParam.beaconFlyingTime * speed;
			distanceAcum = currentUTMLocation.distance(mission.get(posNextWaypoint));
			distances.add(distanceAcum);
			int posLastWaypoint = posNextWaypoint;
			if (distanceAcum < totalDistance) {
				double increment = 0;
				for (int i = posNextWaypoint + 1; i < mission.size() && distanceAcum <= totalDistance; i++) {
					increment = mission.get(i - 1).distance(mission.get(i));
					distances.add(increment);
					distanceAcum = distanceAcum + increment;
					posLastWaypoint = i;
				}
			}
			int remainingLocations = (int) Math.ceil(MBCAPParam.beaconFlyingTime / MBCAPParam.hopTime);
			double incDistance = totalDistance / remainingLocations;
			double xp, yp, zp;
			WaypointSimplified prevWaypoint = new WaypointSimplified(currentWaypoint - 1, currentUTMLocation.x, currentUTMLocation.y, currentZ);
			WaypointSimplified nextWaypoint;
			int i = 0;
			int locations = 0;
			double currentSegmentLength;
			distanceAcum = 0;
			double remainingSegment;
			double prevRemainingSegment = 0;
			while (i < distances.size()
					&& locations < remainingLocations
					&& distanceAcum < totalDistance
					&& posNextWaypoint <= posLastWaypoint) {
				nextWaypoint = mission.get(posNextWaypoint);
				currentSegmentLength = distances.get(i);
				remainingSegment = Math.min(currentSegmentLength, totalDistance - distanceAcum + prevRemainingSegment);
				while (locations < remainingLocations && distanceAcum < totalDistance && remainingSegment >= incDistance) {
					xp = prevWaypoint.x + (nextWaypoint.x - prevWaypoint.x) * (incDistance * (locations + 1)) / currentSegmentLength;
					yp = prevWaypoint.y + (nextWaypoint.y - prevWaypoint.y) * (incDistance * (locations + 1)) / currentSegmentLength;
					zp = prevWaypoint.z + (nextWaypoint.z - prevWaypoint.z) * (incDistance * (locations + 1)) / currentSegmentLength;
					predictedPath.add(new Point3D(xp, yp, zp));
					locations++;
					remainingSegment = remainingSegment - incDistance;
					distanceAcum = distanceAcum + incDistance;
				}
				remainingLocations = remainingLocations - locations;
				prevRemainingSegment = remainingSegment;
				i++;
				posNextWaypoint++;
				prevWaypoint = nextWaypoint;
				locations = 0;
			}
		} else {
			// With acceleration present.
			// 1. Calculus of the total distance
			double maxDistance = currentUTMLocation.distance(mission.get(posNextWaypoint));
			distances.add(maxDistance);
			int posLastWaypoint = posNextWaypoint;
			double increment = 0;
			for (int i = posNextWaypoint + 1; i < mission.size(); i++) {
				increment = mission.get(i - 1).distance(mission.get(i));
				distances.add(increment);
				maxDistance = maxDistance + increment;
				posLastWaypoint = i;
			}

			// 2. Predicted points calculus
			int totalLocations = (int) Math.ceil(MBCAPParam.beaconFlyingTime / MBCAPParam.hopTime);
			int locations = 0;
			double incDistance;
			WaypointSimplified prevWaypoint = new WaypointSimplified(currentWaypoint - 1, currentUTMLocation.x, currentUTMLocation.y, currentZ);
			WaypointSimplified nextWaypoint;
			double xp, yp, zp;
			int i = 0;
			double prevSegmentsLength = 0.0;
			double currentSegmentLength;
			double currentSpeed;
			distanceAcum = 0.0;
			double currentDistance;
			boolean goOn = true;
			while (goOn
					&& i < distances.size()
					&& locations < totalLocations
					&& distanceAcum < maxDistance
					&& posNextWaypoint <= posLastWaypoint) {
				nextWaypoint = mission.get(posNextWaypoint);
				currentSegmentLength = distances.get(i);
				
				while (goOn
						&& locations < totalLocations
						&& distanceAcum < prevSegmentsLength + currentSegmentLength
						&& distanceAcum < maxDistance) {
					double time = (locations+1)*MBCAPParam.hopTime;
					if (acceleration>0) {
						currentSpeed = acceleration*acceleration*Math.log((acceleration+time)/acceleration)+speed;
					} else { // acceleration<0 (acceleration==0 already taken into account)
						currentSpeed = acceleration*acceleration*Math.log(Math.abs(acceleration)/(Math.abs(acceleration)+time))+speed;
					}
					
					if (currentSpeed>0) {
						if (acceleration>0) {
							currentDistance = acceleration*acceleration*(time+acceleration)*Math.log((acceleration+time)/acceleration)
									+ speed*time - acceleration*acceleration*time;
						} else { // acceleration<0
							currentDistance = acceleration*acceleration*(time+Math.abs(acceleration))*Math.log(Math.abs(acceleration)/(Math.abs(acceleration)+time))
									+ speed*time + acceleration*acceleration*time;
						}
						
						if (currentDistance <= prevSegmentsLength + currentSegmentLength) {
							incDistance = currentDistance - prevSegmentsLength;
							xp = prevWaypoint.x + (nextWaypoint.x - prevWaypoint.x) * incDistance / currentSegmentLength;
							yp = prevWaypoint.y + (nextWaypoint.y - prevWaypoint.y) * incDistance / currentSegmentLength;
							zp = prevWaypoint.z + (nextWaypoint.z - prevWaypoint.z) * incDistance / currentSegmentLength;
							predictedPath.add(new Point3D(xp, yp, zp));
							locations++;
						}
						distanceAcum = currentDistance;
					} else {
						// Don't consider new points if the UAV is stopped
						goOn = false;
					}
				}
				prevSegmentsLength = prevSegmentsLength + currentSegmentLength;
				
				i++;
				posNextWaypoint++;
				prevWaypoint = nextWaypoint;
			}
		}
	}

	/** Gets the intersection point of a line (prev->post) and the perpendicular one which includes a third point (currentUTMLocation). */
	private static Point2D.Double getIntersection(Point2D.Double currentUTMLocation, Point2D.Double prev, Point2D.Double post) {
		double x, y;
		// Vertical line case
		if (post.x - prev.x == 0) {
			x = prev.x;
			y = currentUTMLocation.y;
		} else if (post.y - prev.y == 0) {
			// Horizontal line case
			y = prev.y;
			x = currentUTMLocation.x;
		} else {
			// General case
			double prevX = prev.getX();
			double prevY = prev.getY();
			double slope = (post.getY() - prevY) / (post.getX() - prevX);
			x = (currentUTMLocation.getY() - prevY + currentUTMLocation.getX() / slope + slope * prevX) / (slope + 1 / slope);
			y = prevY + slope*(x - prevX);
		}
		return new Point2D.Double(x, y);
	}

	/** Calculates if two UAVs have collision risk. */
	public static boolean hasCollisionRisk(int numUAV, Beacon selfBeacon, Beacon receivedBeacon) {
		// Requires to be in the normal protocol state
		double distance;
		long selfTime, beaconTime;
		for (int i = 0; i < selfBeacon.points.size(); i++) {
			for (int j = 0; j < receivedBeacon.points.size(); j++) {
				// Do not check if the other UAV is in Go on, please and the location is the detected risk location
				if (receivedBeacon.state == MBCAPState.GO_ON_PLEASE.getId() && j == 1) {
					continue;
				}
				
				boolean risky = true;
				// Temporal collision risk (only if the other UAV is also in the normal protocol state)
				if (receivedBeacon.state == MBCAPState.NORMAL.getId()) {
					selfTime = selfBeacon.time + i * MBCAPParam.hopTimeNS;
					beaconTime = receivedBeacon.time + j * MBCAPParam.hopTimeNS;
					if (Math.abs(selfTime - beaconTime) >= MBCAPParam.collisionRiskTime) {
						risky = false;
					}
				}
				
				// X,Y collision risk
				if (risky) {
					distance = selfBeacon.points.get(i).distance(receivedBeacon.points.get(j));
					if (distance >= MBCAPParam.collisionRiskDistance) {
						risky = false;
					}
				}
				
				// Z collision risk
				if (risky) {
					if (Math.abs(selfBeacon.points.get(i).z
							- receivedBeacon.points.get(j).z) >= MBCAPParam.collisionRiskAltitudeDifference) {
						risky = false;
					}
				}

				// Checking the distance between the UAV and the collision risk point
				if (risky
						&& selfBeacon.speed*MBCAPParam.hopTime*i
							- selfBeacon.speed*(System.nanoTime() - selfBeacon.time)*0.000000001
							< MBCAPParam.reactionDistance) {
					Point3D p = new Point3D(selfBeacon.points.get(i).x, selfBeacon.points.get(i).y, selfBeacon.points.get(i).z);
					MBCAPParam.impactLocationUTM[numUAV].put(receivedBeacon.uavId, p);
					MBCAPGUITools.locateImpactRiskMark(p, numUAV, receivedBeacon.uavId);
					return true;
				}
			}
		}
		return false;
	}

	/** Calculates if the current UAV has overtaken another UAV. */
	public static boolean hasOvertaken(int numUAV, long avoidingId, Point2D.Double target) {
		// Overtaken happened if the distance to the target UAV and to the risk location is increasing
		Point3D riskLocation = MBCAPParam.impactLocationUTM[numUAV].get(avoidingId);
		Point3D[] lastLocations = UAVParam.lastLocations[numUAV].getLastPositions();
		double distPrevToTarget, distPostToTarget, distPostToRisk;
		double distPrevToRisk = 0;
		int i = 0;
		while (lastLocations[i] == null) {
			i++;
		}
		distPrevToTarget = lastLocations[i].distance(target);
		if (riskLocation != null) {
			distPrevToRisk = lastLocations[i].distance(riskLocation);
		}
		i++;
		while (i < lastLocations.length) {
			distPostToTarget = lastLocations[i].distance(target);
			if (distPrevToTarget >= distPostToTarget) {
				return false;
			}
			distPrevToTarget = distPostToTarget;
			if (riskLocation != null) {
				distPostToRisk = lastLocations[i].distance(riskLocation);
				if (distPrevToRisk >= distPostToRisk) {
					return false;
				}
				distPrevToRisk = distPostToRisk;
			}
			i++;
		}
		return true;
	}

	/** Checks if the UAV has to move aside, and that safe point when needed. */
	public static boolean needsReposition(int numUAV, List<Point3D> avoidPredictedLocations) {
		// Errors detection
		if (avoidPredictedLocations == null || avoidPredictedLocations.size() <= 1) {
			MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.REPOSITION_ERROR_1);
			return false;
		}
		if (SimParam.zone < 0) {
			MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.REPOSITION_ERROR_2);
			return false;
		}

		Point2D.Double currentUTMLocation = UAVParam.uavCurrentData[numUAV].getUTMLocation();

		// Checking the distance of the UAV to the segments of the mission of the other UAV
		Point2D.Double prevLocation, postLocation;
		Point2D.Double newLocation = null;
		boolean isInSafePlace = false;
		while (!isInSafePlace) {
			boolean foundConflict = false;
			for (int i=1; i<avoidPredictedLocations.size(); i++) {
				prevLocation = new Point2D.Double(avoidPredictedLocations.get(i-1).x, avoidPredictedLocations.get(i-1).y);
				postLocation = new Point2D.Double(avoidPredictedLocations.get(i).x, avoidPredictedLocations.get(i).y);
				// One segment check
				Point2D.Double auxLocation;
				auxLocation = MBCAPHelper.getSafeLocation(currentUTMLocation, prevLocation, postLocation);
				if (auxLocation!=null) {
					newLocation = auxLocation;
					currentUTMLocation = auxLocation;
					foundConflict = true;
				}
			}
			if (!foundConflict) {
				isInSafePlace = true;
			}
		}

		// If new coordinates have been found, it means that the UAV must move to a safer position
		if (newLocation != null) {
			MBCAPParam.targetPointUTM[numUAV] = newLocation;										// To show on screen
			MBCAPParam.targetPointGeo[numUAV] = GUIHelper.UTMToGeo(newLocation.x, newLocation.y);	// To order the movement
			return true;
		}
		return false;
	}

	/** Calculates the safe place to move aside from a path segment.
	 * <p>Returns null if there is no need of moving aside. */
	private static Point2D.Double getSafeLocation(Point2D.Double currentLocation,
			Point2D.Double prev, Point2D.Double post) {
		double x, y;
		// Vertical line case
		if (post.x - prev.x == 0) {
			// currentPos out of the segment case
			if (currentLocation.y<Math.min(post.y, prev.y)
					|| currentLocation.y>Math.max(post.y, prev.y)) {
				return null;
			}
			// Farthest enough case
			if (Math.abs(currentLocation.x-prev.x)>MBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			if (currentLocation.x < prev.x) {
				x = prev.x - MBCAPParam.safePlaceDistance - MBCAPParam.PRECISION_MARGIN;
			} else {
				x = prev.x + MBCAPParam.safePlaceDistance + MBCAPParam.PRECISION_MARGIN;
			}
			y = currentLocation.y;
		} else if (post.y - prev.y == 0) {
			// Horizontal line case
			// currentPos out of the segment case
			if (currentLocation.x<Math.min(post.x, prev.x)
					|| currentLocation.x>Math.max(post.x, prev.x)) {
				return null;
			}
			// Farthest enough case
			if (Math.abs(currentLocation.y-prev.y)>MBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			if (currentLocation.y < prev.y) {
				y = prev.y - MBCAPParam.safePlaceDistance - MBCAPParam.PRECISION_MARGIN;
			} else {
				y = prev.y + MBCAPParam.safePlaceDistance + MBCAPParam.PRECISION_MARGIN;
			}
			x = currentLocation.x;
		} else {
			// General case
			double currentX = currentLocation.getX();
			double currentY = currentLocation.getY();
			double prevX = prev.getX();
			double prevY = prev.getY();
			double postX = post.getX();
			double postY = post.getY();
			double incX = postX - prevX;
			double incY = postY - prevY;
			double slope = incY / incX;
			double slopeInv = 1 / slope;
			double iX, iY;
			// Intersection point between the segment and the perpendicular line passing through the current position
			iX = (currentY - prevY + slopeInv * currentX + slope * prevX) / (slope + slopeInv);
			iY = prevY + slope*(iX - prevX);
			// currentPos out of the segment case
			if (iX<Math.min(post.x, prev.x)
					|| iX>Math.max(post.x, prev.x)) {
				return null;
			}
			// Farthest enough case
			double distIntersection = Math.sqrt(Math.pow(currentX-iX, 2) + Math.pow(currentY-iY, 2));
			if (distIntersection>MBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			double ds = MBCAPParam.safePlaceDistance + MBCAPParam.PRECISION_MARGIN;
			double d12 = Math.sqrt(Math.pow(postX - prevX, 2) + Math.pow(postY - prevY, 2));
			double incXS = ds / d12 * Math.abs(incY);
			if (currentX <= iX) {
				x = iX - incXS;
			} else {
				x = iX + incXS;
			}
			y = currentY - incX / incY * (x - currentX);
		}

		// Returns the safe place in UTM coordinates
		return new Point2D.Double(x, y);
	}

	/** Lands the UAVs when a collision happens. */
	public static void landAllUAVs() {
		for (int i=0; i<Param.numUAVs; i++) {
			if (API.setMode(i, UAVParam.Mode.LAND_ARMED)) {
				MissionHelper.log(SimParam.prefix[i] + MBCAPText.LANDING);
				
			} else {
				MissionHelper.log(SimParam.prefix[i] + MBCAPText.LANDING_ERROR);
			}
		}
	}
	
	/** Logging to file the error predicting the location during the experiment (and the beacons itself if needed). */
	@SuppressWarnings("unchecked")
	public static void storeBeaconsError(String folder, String baseFileName) {
		List<PointTime>[] realUAVPaths = new ArrayList[Param.numUAVs];

		// 1. UAV path calculus (only experiment path, and ignoring repeated positions)
		LogPoint realPostLocation, realPrevLocation;
		Double x, y;
		for (int i=0; i<Param.numUAVs; i++) {
			int j = 0;
			realPrevLocation = null;
			y = x = null;
			realUAVPaths[i] = new ArrayList<PointTime>();

			while (j<SimParam.uavUTMPath[i].size()) {
				realPostLocation = SimParam.uavUTMPath[i].get(j);

				// Considers only not repeated locations and only generated during the experiment
				if (SimParam.uavUTMPath[i].get(j).inTest) {
					x = realPostLocation.x;
					y = realPostLocation.y;
					if (realPrevLocation == null) {
						// First test location
						realPrevLocation = realPostLocation;
					} else if (realPostLocation.x!=realPrevLocation.x || realPostLocation.y!=realPrevLocation.y || realPostLocation.z!=realPrevLocation.z) {
						// Moved
						realPrevLocation = realPostLocation;
					} else {
						// Don't moved
						x = null;
						y = null;
					}

					// Real locations
					if (x != null) {
						realUAVPaths[i].add(new PointTime(SimParam.uavUTMPath[i].get(j).time, x, y));
					}
				}
				j++;
			}
		}
		
		// 2. Predicted positions calculus and storage of beacons
		File beaconsFile, maxErrorFile, beaconsErrorFile, timeErrorFile;
		StringBuilder sb1, sb2, sb3, sb4;
		int j;
		PointTime predictedLocation;
		List<PointTime>[][] totalPredictedLocations = new ArrayList[Param.numUAVs][];
		for (int i=0; i<Param.numUAVs; i++) {
			totalPredictedLocations[i] = new ArrayList[MBCAPParam.beaconsStored[i].size()];
			
			if (Param.VERBOSE_STORE) {
				// Store each beacon also
				beaconsFile = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + MBCAPText.BEACONS_SUFIX);
				sb1 = new StringBuilder(2000);
				sb1.append("time,x1,y2,x2,y2,...,xn,yn\n");
			}
			List<Beacon> beacons = MBCAPParam.beaconsStored[i];
			// j beacons
			Beacon beacon;
			for (j=0; j<beacons.size(); j++) {
				beacon = beacons.get(j);
				if (beacon.points!=null && beacon.points.size()>0) {
					sb1.append(beacon.time);
					List<Point3D> locations = beacon.points;
					totalPredictedLocations[i][j] = new ArrayList<PointTime>();
					for (int k=0; k<locations.size(); k++) {
						if (Param.VERBOSE_STORE) {
							sb1.append("," + locations.get(k).x + "," + locations.get(k).y);
						}
						predictedLocation = new PointTime(beacon.time + MBCAPParam.hopTimeNS*k, locations.get(k).x, locations.get(k).y);
						// Predicted positions for later calculus of the error in prediction
						totalPredictedLocations[i][j].add(predictedLocation);
					}
					if (Param.VERBOSE_STORE) {
						sb1.append("\n");
					}
				}
			}
			if (Param.VERBOSE_STORE) {
				GUIHelper.storeFile(beaconsFile, sb1.toString());
			}

			// 3. Calculus of the mean and maximum error on each beacon
			PointTime[] realUAVPath = new PointTime[realUAVPaths[i].size()];
			realUAVPath = realUAVPaths[i].toArray(realUAVPath);
			List<PointTime>[] predictedLocations = totalPredictedLocations[i];
			int numBeacons = 0; // Number of beacons with useful information
			for (int k=0; k<predictedLocations.length; k++) {
				if (predictedLocations[k] != null) {
					numBeacons++;
				}
			}
			double[] maxBeaconDistance = new double[numBeacons]; // One per beacon
			double[] meanBeaconDistance = new double[numBeacons];
			if (Param.VERBOSE_STORE) {
				// Log the line from the real to the predicted location, with maximum error
				maxErrorFile = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + MBCAPText.MAX_ERROR_LINES_SUFIX);
				sb2 = new StringBuilder(2000);
			}
			int pos = 0;
			for (j=0; j<predictedLocations.length; j++) {
				if (predictedLocations[j] != null) {
					Pair<Point2D.Double, Point2D.Double> pair =
							MBCAPHelper.beaconErrorCalculation(realUAVPath, predictedLocations, j,
									maxBeaconDistance, pos, meanBeaconDistance);
					pos++;
					if (pair!=null && Param.VERBOSE_STORE) {
						sb2.append("._LINE\n");
						sb2.append(pair.getValue0().x + "," + pair.getValue0().y + "\n");
						sb2.append(pair.getValue1().x + "," + pair.getValue1().y + "\n\n");
					}
				}
			}
			if (Param.VERBOSE_STORE) {
				GUIHelper.storeFile(maxErrorFile, sb2.toString());
			}
			
			// 4. Storage of the mean and maximum error on each beacon
			beaconsErrorFile = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + MBCAPText.BEACON_TOTAL_ERROR_SUFIX);
			sb3 = new StringBuilder(2000);
			sb3.append("max(m),mean(m)\n");
			for (int k=0; k<maxBeaconDistance.length-1; k++) {
				sb3.append(maxBeaconDistance[k] + "," + meanBeaconDistance[k] + "\n");
			}
			sb3.append(maxBeaconDistance[maxBeaconDistance.length-1] + "," + meanBeaconDistance[meanBeaconDistance.length-1]);
			GUIHelper.storeFile(beaconsErrorFile, sb3.toString());
			
			// 5. Calculus and storage of the mean and maximum error on each position of each beacon
			int size = 0;
			for (int m = 0; m<predictedLocations.length; m++) {
				if (predictedLocations[m].size()>size) {
					size = predictedLocations[m].size();
				}
			}
			double[] maxTimeDistance = new double[size];
			double[] meanTimeDistance = new double[size];
			MBCAPHelper.timeErrorCalculation(realUAVPath, predictedLocations, maxTimeDistance, meanTimeDistance);
			timeErrorFile = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + MBCAPText.BEACON_POINT_ERROR_SUFIX);
			sb4 = new StringBuilder(2000);
			sb4.append("max(m),mean(m)\n");
			for (int k=0; k<maxTimeDistance.length-1; k++) {
				sb4.append(maxTimeDistance[k] + "," + meanTimeDistance[k] + "\n");
			}
			sb4.append(maxTimeDistance[maxTimeDistance.length-1] + "," + meanTimeDistance[meanTimeDistance.length-1]);
			GUIHelper.storeFile(timeErrorFile, sb4.toString());
		}
	}
	
	/** Auxiliary method to calculate the mean and maximum error in the prediction error of each beacon. */
	private static Pair<Point2D.Double, Point2D.Double> beaconErrorCalculation(
			PointTime[] realUAVPath, List<PointTime>[] predictedLocations, int predictedPos,
			double[] maxBeaconDistance, int distancePos, double[] meanBeaconDistance) {
		double dist;
		int num = 0;
		Point2D.Double ini, fin = ini = null;
		List<PointTime> predictedLocs = predictedLocations[predictedPos];
		PointTime predictedLocation, realPrev, realPost;
		predictedLocation = null;
		Point2D.Double realIntersection;
		int prev, post;

		// Calculus for each one of the predicted locations
		for (int i=0; i<predictedLocs.size(); i++) {
			predictedLocation = predictedLocs.get(i);
			// Ignores points too far on time or outside the real path
			if (predictedLocs.get(0).distance(predictedLocation) <= MBCAPParam.reactionDistance
					&& predictedLocation.time >= realUAVPath[0].time
					&& predictedLocation.time <= realUAVPath[realUAVPath.length-1].time) {
				// Locating the real previous and next points
				prev = -1;
				post = -1;
				boolean found = false;
				for (int j=1; j<realUAVPath.length && !found; j++) {
					if (predictedLocation.time >= realUAVPath[j-1].time && predictedLocation.time <= realUAVPath[j].time) {
						found = true;
						prev = j - 1;
						post = j;
					}
				}
				// Interpolating the point in the segment over the real path
				realPrev = realUAVPath[prev];
				realPost = realUAVPath[post];
				double incT = Math.abs((predictedLocation.time-realPrev.time)/(realPost.time-realPrev.time));
				double x = realPrev.x + (realPost.x-realPrev.x) * incT;
				double y = realPrev.y + (realPost.y-realPrev.y) * incT;
				realIntersection = new Point2D.Double(x, y);
				// Checking if the distance is greater than the previous
				dist = predictedLocation.distance(realIntersection);
				if (dist > maxBeaconDistance[distancePos]) {
					maxBeaconDistance[distancePos] = dist;
					ini = predictedLocation;
					fin = realIntersection;
				}
				meanBeaconDistance[distancePos] = meanBeaconDistance[distancePos] + dist;
				num++;
			}
		}
		// Due to concurrence, the first beacon can be sent before real[0].time, so no points are useful
		if (num > 0) {
			meanBeaconDistance[distancePos] = meanBeaconDistance[distancePos]/num;
		}

		if (ini!=null && fin!=null) {
			return Pair.with(ini, fin);
		} else {
			return null;
		}
	}

	/** Auxiliary method to calculate the maximum and mean error on each position of each beacon. */
	private static void timeErrorCalculation(PointTime[] realUAVPath, List<PointTime>[] predictedLocations,
			double[] maxTimeDistance, double[] meanTimeDistance) {
		double dist;
		int[] num = new int[maxTimeDistance.length];
		List<PointTime> predictedLocs;
		PointTime predictedLocation, realPrev, realPost;
		Point2D.Double realIntersection;
		int prev, post;

		for (int i=0; i<predictedLocations.length; i++) {
			predictedLocs = predictedLocations[i];
			for (int j=0; j<predictedLocs.size(); j++) {
				predictedLocation = predictedLocs.get(j);
				// Ignores points out of the real path
				if (predictedLocation.time >= realUAVPath[0].time
						&& predictedLocation.time <= realUAVPath[realUAVPath.length-1].time) {
					// Locating the real previous and next points
					prev = -1;
					post = -1;
					boolean found = false;
					for (int k=1; k<realUAVPath.length && !found; k++) {
						if (predictedLocation.time >= realUAVPath[k-1].time && predictedLocation.time <= realUAVPath[k].time) {
							found = true;
							prev = k - 1;
							post = k;
						}
					}
					// Interpolating the point in the segment over the real path
					realPrev = realUAVPath[prev];
					realPost = realUAVPath[post];
					double incT = Math.abs((predictedLocation.time-realPrev.time)/(realPost.time-realPrev.time));
					double x = realPrev.x + (realPost.x-realPrev.x) * incT;
					double y = realPrev.y + (realPost.y-realPrev.y) * incT;
					realIntersection = new Point2D.Double(x, y);
					// Checking if the distance is greater than the previous
					dist = predictedLocation.distance(realIntersection);
					if (dist>maxTimeDistance[j]) {
						maxTimeDistance[j] = dist;
					}
					meanTimeDistance[j] = meanTimeDistance[j] + dist;
					num[j]++;
				}
			}
		}

		for (int i=0; i<meanTimeDistance.length; i++) {
			meanTimeDistance[i] = meanTimeDistance[i]/num[i];
		}
	}
	
}
