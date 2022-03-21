package com.protocols.mbcap.logic;

import com.api.API;
import com.api.pojo.FlightMode;
import com.api.pojo.location.WaypointSimplified;
import com.esotericsoftware.kryo.io.Output;
import com.uavController.UAVParam;
import es.upv.grc.mapper.*;
import com.setup.Param;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.GUI;
import com.api.MissionHelper;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.protocols.mbcap.gui.MBCAPSimProperties;
import com.protocols.mbcap.pojo.Beacon;
import com.protocols.mbcap.pojo.MBCAPState;
import org.javatuples.Pair;
import org.javatuples.Quintet;

import java.awt.*;
import java.sql.SQLOutput;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** This class sends data packets to other UAVs, by real or simulated broadcast, so others can detect risk of collision.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class BeaconingThread extends Thread {
	
	private AtomicReference<DrawableCirclesGeo> predictedLocations;
	private AtomicInteger event;
	private AtomicReference<MBCAPState> currentState;
	private AtomicLong idAvoiding;
	private AtomicInteger projectPath;
	private AtomicReference<Beacon> beacon;
	private AtomicReference<Location2DUTM> targetLocationUTM;
	private List<Beacon> beaconsStored;
	private Map<Long, Location3DUTM> impactLocationUTM;
	
	private int numUAV; // UAV identifier, beginning from 0
	private LowLevelCommLink link;
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	private Copter copter;
	private long selfID;
	private GUI gui;
	double minDistance = Double.MAX_VALUE;

	@SuppressWarnings("unused")
	private BeaconingThread() {}

	public BeaconingThread(int numUAV) {
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			this.predictedLocations = MBCAPSimProperties.predictedLocation[numUAV];
		}
		this.event = MBCAPParam.event[numUAV];
		this.currentState = MBCAPParam.state[numUAV];
		this.idAvoiding = MBCAPParam.idAvoiding[numUAV];
		this.projectPath = MBCAPParam.projectPath[numUAV];
		this.beacon = MBCAPParam.selfBeacon[numUAV];
		this.targetLocationUTM = MBCAPParam.targetLocationUTM[numUAV];
		this.beaconsStored = MBCAPParam.beaconsStored[numUAV];
		this.impactLocationUTM = MBCAPParam.impactLocationUTM[numUAV];
		
		this.numUAV = numUAV;
		this.link = LowLevelCommLink.getCommLink(numUAV);
		this.outBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		this.copter = API.getCopter(numUAV);
		this.selfID = this.copter.getID();
		this.gui = API.getGUI(numUAV);
	}

	@Override
	public void run() {
		Beacon selfBeacon = null;
		long waitingTime;
		short prevState = 0;

		// Send beacons while the UAV is flying during the experiment
		// The protocol is stopped when two UAVs collide
		long cicleTime = System.currentTimeMillis();
		long startTime = Long.MIN_VALUE;
		long stopTime = Long.MIN_VALUE;
		ArduSim ardusim = API.getArduSim();
		boolean timestarted = false;
		boolean timestopped = false;
		while (ardusim.isExperimentInProgress() && copter.isFlying()
				&& !ardusim.collisionIsDetected()) {

			if(copter.getAltitude() > 4.8 && !timestarted){
				timestarted = true;
				startTime = System.currentTimeMillis();
				System.out.println("START TIME");
			}

			if(copter.getFlightMode() == FlightMode.LAND_ARMED && !timestopped){
				stopTime = System.currentTimeMillis() - startTime;
				timestopped = true;
				System.out.println("STOP TIME");
			}

			logMinDistance();
			// Each beacon is sent a number of times before renewing the predicted positions
			for (long i = 0; i < MBCAPParam.numBeacons; i++) {
				
				// The first time it is needed to calculate the predicted positions
				if (i == 0) {
					selfBeacon = this.getBeacon();
					beacon.set(selfBeacon);
					message = selfBeacon.sendBuffer;
					prevState = selfBeacon.state;

					// Beacon store for logging purposes, while the experiment is in progress
					if (ardusim.isStoreDataEnabled()
							&& ardusim.getExperimentEndTime()[numUAV] == 0) {
						beaconsStored.add(new Beacon(selfBeacon));
					}
				} else {
					// In any other case, only time, state and idAvoiding are updated
					message = this.getBufferUpdated(selfBeacon);
					// Create a new beacon if the state is changed
					if (selfBeacon.state != prevState) {
						selfBeacon = this.getBeacon();
						beacon.set(selfBeacon);
						message = selfBeacon.sendBuffer;
						prevState = selfBeacon.state;

						// Beacon store for logging purposes
						if (ardusim.isStoreDataEnabled()
								&& ardusim.getExperimentEndTime()[numUAV] == 0) {
							beaconsStored.add(new Beacon(selfBeacon));
						}
						i = 0;	// Reset value to start again
					}
				}
				if (!selfBeacon.points.isEmpty()) {
					link.sendBroadcastMessage(message);
				}

				cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		System.out.println("DATA: " + minDistance + ";" + stopTime);
		// Stop sending and drawing future positions when the UAV lands
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			DrawableCirclesGeo current = predictedLocations.getAndSet(null);
			if (current != null) {
				try {
					Mapper.Drawables.removeDrawable(current);
				} catch (GUIMapPanelNotReadyException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void logMinDistance() {
		double distance = UAVParam.distances[0][1].get();
		if(distance < minDistance){
			minDistance = distance;
		}
	}
	
	/** Creates a beacon to send data. Don't use to receive data.
	 * <p>This method always should be followed by the getBuffer() method.</p> */
	public Beacon getBeacon() {
		// 1. Getting the needed information
		Quintet<Long, Location2DUTM, Double, Double, Double> uavcurrentData = copter.getData();
		Long time = uavcurrentData.getValue0();
		Location2DUTM currentLocation = uavcurrentData.getValue1();
		double currentZ = uavcurrentData.getValue2();
		float plannedSpeed = (float)copter.getPlannedSpeed();
		double speed = uavcurrentData.getValue3();
		double acceleration = uavcurrentData.getValue4();
		List<Location3DUTM> points = this.getPredictedPath(speed, acceleration, currentLocation, currentZ);
		List<Location2DGeo> circles = new ArrayList<>();
		for (Location3DUTM loc : points) {
			try {
				circles.add(loc.getGeo());
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
			}
		}
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			DrawableCirclesGeo current = predictedLocations.get();
			if (current == null) {
				try {
					predictedLocations.set(Mapper.Drawables.addCirclesGeo(3, circles, MBCAPParam.collisionRiskDistance,
							Color.BLACK, MBCAPParam.STROKE_POINT));
				} catch (GUIMapPanelNotReadyException e) {
					e.printStackTrace();
				}
			} else {
				current.updateLocations(circles);
			}
		}

		// 2. Beacon building
		Beacon res = new Beacon();
		res.uavId = selfID;
		res.event = (short)event.get();
		res.state = currentState.get().getId();
		res.plannedSpeed = plannedSpeed;
		res.speed = speed;
		res.time = time;
		res.points = points;

		// 3. Buffer building
		output.reset();
		output.writeLong(res.uavId);
		output.writeShort(res.event);
		res.statePos = output.position();
		output.writeShort(res.state);
		res.isLandingPos = output.position();
		res.isLanding = !copter.isFlying() || this.isLanding();
		if (res.isLanding) {
			output.writeShort(1);
		} else {
			output.writeShort(0);
		}
		res.idAvoidingPos = output.position();
		res.idAvoiding = idAvoiding.get();
		output.writeLong(res.idAvoiding);
		output.writeFloat(res.plannedSpeed);
		output.writeFloat((float)res.speed);
		res.timePos = output.position();
		output.writeLong(System.nanoTime() - res.time);
		if (res.points == null) {
			output.writeShort(0);
		} else {
			output.writeShort(res.points.size());
			for (int i = 0; i < res.points.size(); i++) {
				output.writeDouble(res.points.get(i).x);
				output.writeDouble(res.points.get(i).y);
				output.writeDouble(res.points.get(i).z);
			}
		}
		output.flush();
		res.dataSize = output.position();
		res.sendBuffer = Arrays.copyOf(outBuffer, output.position());

		return res;
	}
	
	/** Analyzes if the UAV is landing or performing RTL, and the protocol must be disabled. */
	private boolean isLanding() {
		if (copter.getFlightMode().getCustomMode() == 9) {	// Land flight mode
			return true;
		}
		return copter.getMissionHelper().isLastWaypointReached();
	}
	
	/** Predicts the future positions of the UAV. Returns empty list in case of problems. */
	private List<Location3DUTM> getPredictedPath(double speed, double acceleration, Location2DUTM currentUTMLocation, double currentZ) {
		List<Location3DUTM> predictedPath = new ArrayList<>(MBCAPParam.POINTS_SIZE);
		// Multiple cases to study:
		
		// 1. At the beginning, without GPS, the initial position can be null
		if (currentUTMLocation == null) {
			return predictedPath;
		}
		
		// 2. If the UAV is in the "go on, please" state, only the current position and the predicted collision risk location are sent
		MBCAPState state = currentState.get();
		if (state == MBCAPState.GO_ON_PLEASE) {
			predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			Location3DUTM riskLocation = impactLocationUTM.get(beacon.get().idAvoiding);
			// If no risk point was detected, a fake point is sent
			predictedPath.add(Objects.requireNonNullElseGet(riskLocation, () -> new Location3DUTM(0, 0, 0)));
			return predictedPath;
		}

		// 3. If moving slowly or still but not in the "stand still" state, only the current position is sent
		if (speed < MBCAPParam.minSpeed && state != MBCAPState.STAND_STILL) {
			predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			return predictedPath;
		}

		// 4. If the UAV is moving aside, send the current position and prediction towards the destination safe point
		if (state == MBCAPState.MOVING_ASIDE) {
			Location2DUTM destination = targetLocationUTM.get();
			double length = destination.distance(currentUTMLocation);
			int numLocations1 = (int)Math.ceil(length/(speed * MBCAPParam.hopTime));
			int numLocations2 = (int)Math.ceil(this.getReactionDistance(speed) / (speed * MBCAPParam.hopTime));
			int numLocations = Math.min(numLocations1, numLocations2);
			predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			double xUTM, yUTM;
			double incX = (destination.x - currentUTMLocation.x) * (speed * MBCAPParam.hopTime)/length;
			double incY = (destination.y - currentUTMLocation.y) * (speed * MBCAPParam.hopTime)/length;
			for (int i=1; i<numLocations; i++) {
				xUTM = currentUTMLocation.x + i*incX;
				yUTM = currentUTMLocation.y + i*incY;
				predictedPath.add(new Location3DUTM(xUTM, yUTM, currentZ));
			}
			return predictedPath;
		}

		// Reasonable hypothesis: The UAV moves in the waypoint list in increasing order
		// First of all, we have to locate the waypoint the UAV is moving toward (posNextWaypoint)
		MissionHelper helper = copter.getMissionHelper();
		int currentWaypoint = helper.getCurrentWaypoint();
		List<WaypointSimplified> mission = helper.getSimplified();
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
				gui.logUAV(MBCAPText.WAYPOINT_LOST);
				return predictedPath;
			}
			
			// 5. In the stand still state, send remaining waypoints so the other UAV could decide whether to move aside or not
			if (state == MBCAPState.STAND_STILL) {
				// The first point is the current position
				predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
				// The rest of the points are the waypoints not reached jet, if they fit in the beacon
				int locations = 1;
				int i=posNextWaypoint;
				while (i<mission.size() && locations < MBCAPParam.MAX_BEACON_LOCATIONS
						&& mission.get(i).distance(currentUTMLocation) < MBCAPParam.MAX_WAYPOINT_DISTANCE) {
					predictedPath.add(new Location3DUTM(mission.get(i).x, mission.get(i).y, mission.get(i).z));
					locations++;
					i++;
				}
				// May be the next waypoint is too far, so we add an additional waypoint
				if (i<mission.size() && locations < MBCAPParam.MAX_BEACON_LOCATIONS) {
					predictedPath.add(new Location3DUTM(mission.get(i).x, mission.get(i).y, mission.get(i).z));
				}
				return predictedPath;
			}

			// 6. When really close to the last waypoint, only the current location is sent
			if (posNextWaypoint == mission.size()) {
				posNextWaypoint--; // So it could be drawn until the end
				if (currentUTMLocation.distance(mission.get(posNextWaypoint)) < MBCAPParam.DISTANCE_TO_MISSION_END) {
					predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
					return predictedPath;
				}
			}

			// 7. General case
			// Initial location calculated depending on the protocol version
			Pair<Location2DUTM, Integer> currentLocation = this.getCurrentLocation(mission, currentUTMLocation, posNextWaypoint);
			currentUTMLocation = currentLocation.getValue0();
			posNextWaypoint = currentLocation.getValue1();
			predictedPath.add(new Location3DUTM(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			// Calculate the rest of the points
			this.getPredictedLocations(speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
		}
		return predictedPath;
	}
	
	/** Get the reaction distance given the current speed. It ignores the possible current acceleration. */
	private double getReactionDistance(double speed) {
		double d = 0;
		if (speed >= MBCAPParam.minSpeed) {
			if (speed < 6) {
				d = 1.7 * speed - 0.1;
			} else if (speed < 8) {
				d = 1.3 * speed + 2.3;
			} else {
				d = 1.87 * speed - 2.26;
			}
			d = d + MBCAPParam.gpsError
					+ MBCAPParam.riskCheckPeriod / 1000000000L * speed
					+ (MBCAPParam.packetLossThreshold * MBCAPParam.beaconingPeriod) / 1000.0 * speed;
		}
		return d;
	}

	/** Calculates the first point of the predicted positions list.
	 * <p>It also decides which is the next waypoint the UAV is moving towards.</p> */
	private Pair<Location2DUTM, Integer> getCurrentLocation(List<WaypointSimplified> mission, Location2DUTM currentUTMLocation, int posNextWaypoint) {
		Location2DUTM baseLocation;
		int nextWaypointPosition = posNextWaypoint;
		if (projectPath.get() == 0) {
			// The next waypoint position has been already set
			// The first predicted position is the current coordinates
			baseLocation = currentUTMLocation;
		} else {
			// It is necessary to project the current location over the planned path
			if (posNextWaypoint <= 1) {
				// If we are over the first main.java.com.protocols.mission segment, the first predicted position is the current coordinates
				baseLocation = currentUTMLocation;
				// The next waypoint position has been already set
			} else {
				// In any other case... (there is a previous segment in the planned path)
				// The current location can be projected over the previous or the following segment
				WaypointSimplified sp1 = mission.get(posNextWaypoint-2);	// Segment 1 previous point
				Location2DUTM location1 = new Location2DUTM(sp1.x, sp1.y);
				WaypointSimplified sp2 = mission.get(posNextWaypoint-1);	// Segments 1-2 joining point
				Location2DUTM location2 = new Location2DUTM(sp2.x, sp2.y);
				WaypointSimplified sp3 = mission.get(posNextWaypoint);		// Segment 2 final point
				Location2DUTM location3 = new Location2DUTM(sp3.x, sp3.y);

				// Detect if moving towards the conflicting waypoint or from it
				boolean waypointOvertaken = MBCAPHelper.isMovingAway(copter.getLocationUTMLastKnown(), location2);
				if (waypointOvertaken) {
					baseLocation = MBCAPHelper.getIntersection(currentUTMLocation, location2, location3);
				} else {
					baseLocation = MBCAPHelper.getIntersection(currentUTMLocation, location1, location2);
					nextWaypointPosition--;
				}
			}
		}
		return Pair.with(baseLocation, nextWaypointPosition);
	}

	/** Calculates the predicted positions. */
	private void getPredictedLocations(double speed, double acceleration,
			Location2DUTM currentUTMLocation, List<WaypointSimplified> mission, int posNextWaypoint, int currentWaypoint, double currentZ,
			List<Location3DUTM> predictedPath) {
		// 1. Calculus of the segments over which we have to obtain the points
		List<Double> distances = new ArrayList<>(MBCAPParam.DISTANCES_SIZE);
		double totalDistance;
		// MBCAP v1 or MBCAP v2 (no acceleration present)
		int remainingLocations = (int)Math.ceil(this.getReactionDistance(speed) / (speed * MBCAPParam.hopTime));
		double flyingTime = remainingLocations * MBCAPParam.hopTime;
		if (acceleration == 0.0) {
			totalDistance = flyingTime * speed;
		} else if (acceleration < -0.6) {
			totalDistance = 0;
		} else {
			// Constant acceleration present
			totalDistance = acceleration * flyingTime * flyingTime / 2.0 + speed * flyingTime;
		}
		
		// 2. Calculus of the last waypoint included in the points calculus, and the total distance
		double distanceAcum;
		// Distance of the first segment
		distanceAcum = currentUTMLocation.distance(mission.get(posNextWaypoint));
		distances.add(distanceAcum);
		int posLastWaypoint = posNextWaypoint;
		// Distance of the rest of segments until the last waypoint
		if (distanceAcum < totalDistance) {
			double increment;
			for (int i = posNextWaypoint + 1; i < mission.size() && distanceAcum <= totalDistance; i++) {
				increment = mission.get(i - 1).distance(mission.get(i));
				distances.add(increment);
				distanceAcum = distanceAcum + increment;
				posLastWaypoint = i;
			}
		}

		// 3. Predicted points calculus
		double incDistance = 0.0;
		double prevSegmentsLength = 0.0;

		if (acceleration == 0.0) {
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
			if (acceleration == 0) {
				remainingSegment = Math.min(currentSegmentLength, totalDistance - distanceAcum + prevRemainingSegment);

				// Following the current segment
				while (locations < remainingLocations && distanceAcum < totalDistance && remainingSegment >= incDistance) {
					xp = prevWaypoint.x + (nextWaypoint.x - prevWaypoint.x) * (incDistance * (locations + 1)) / currentSegmentLength;
					yp = prevWaypoint.y + (nextWaypoint.y - prevWaypoint.y) * (incDistance * (locations + 1)) / currentSegmentLength;
					zp = prevWaypoint.z + (nextWaypoint.z - prevWaypoint.z) * (incDistance * (locations + 1)) / currentSegmentLength;
					predictedPath.add(new Location3DUTM(xp, yp, zp));
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
			} else {
				// the distance increment has to be calculated for each main.java.com.protocols.mission segment
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
					predictedPath.add(new Location3DUTM(xp, yp, zp));
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
	
	/** Updates (time, state, landing, and avoidingId) and returns the buffer of the beacon. */
	public byte[] getBufferUpdated(Beacon beacon) {
		output.setPosition(beacon.statePos);
		beacon.state = currentState.get().getId();
		output.writeShort(beacon.state);
		output.setPosition(beacon.isLandingPos);
		beacon.isLanding = !copter.isFlying() || this.isLanding();
		if (beacon.isLanding) {
			output.writeShort(1);
		} else {
			output.writeShort(0);
		}
		output.setPosition(beacon.idAvoidingPos);
		beacon.idAvoiding = idAvoiding.get();
		output.writeLong(beacon.idAvoiding);
		output.setPosition(beacon.timePos);
		output.writeLong(System.nanoTime() - beacon.time);
		output.setPosition(beacon.dataSize);
		output.flush();
		return Arrays.copyOf(outBuffer, beacon.dataSize);
	}

}
