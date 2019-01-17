package mbcapOLD.logic;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Param.SimulatorState;
import mbcap.gui.MBCAPConfigDialog;
import mbcap.gui.MBCAPGUIParam;
import mbcap.gui.MBCAPGUITools;
import mbcap.gui.MBCAPPCCompanionDialog;
import mbcap.logic.BeaconingThread;
import mbcap.logic.CollisionDetectorThread;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPText;
import mbcap.logic.MBCAPv3Helper;
import mbcap.logic.ReceiverThread;
import mbcap.pojo.ProgressState;
import mbcapOLD.gui.OLDMBCAPConfigDialog;
import mbcapOLD.gui.OLDMBCAPGUIParam;
import mbcapOLD.gui.OLDMBCAPGUITools;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcapOLD.pojo.OLDBeacon;
import mbcapOLD.pojo.OLDPointTime;
import mbcapOLD.pojo.OLDProgressState;
import sim.board.BoardPanel;
import sim.logic.CollisionDetector;
import sim.logic.SimParam;
import uavController.UAVParam;

/** This class contains exclusively static methods used by the MBCAP protocol. */

public class OLDMBCAPHelper extends ProtocolHelper {
	
	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V3 + "OLD";
	}
	
	@Override
	public boolean loadMission() {
		return true;
	}
	
	@Override
	public void openConfigurationDialog() {
		new OLDMBCAPConfigDialog();
	}

	/** Initializes protocol specific data structures when the simulator starts. */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeDataStructures() {
		OLDMBCAPGUIParam.predictedLocation = new AtomicReferenceArray<List<Point3D>>(Param.numUAVs);

		OLDMBCAPParam.event = new AtomicIntegerArray(Param.numUAVs);
		MBCAPParam.eventDeadlockSolved = new AtomicIntegerArray(Param.numUAVs);
		MBCAPParam.eventDeadlockFailed = new AtomicIntegerArray(Param.numUAVs);
		MBCAPParam.state = new MBCAPState[Param.numUAVs];
		OLDMBCAPParam.idAvoiding = new AtomicLongArray(Param.numUAVs);
		OLDMBCAPParam.projectPath = new AtomicIntegerArray(Param.numUAVs);

		OLDMBCAPParam.selfBeacon = new AtomicReferenceArray<OLDBeacon>(Param.numUAVs);
		OLDMBCAPParam.beacons = new ConcurrentHashMap[Param.numUAVs];
		OLDMBCAPParam.impactLocationUTM = new ConcurrentHashMap[Param.numUAVs];
		OLDMBCAPParam.impactLocationPX = new ConcurrentHashMap[Param.numUAVs];

		OLDMBCAPParam.targetPointUTM = new Point2D.Double[Param.numUAVs];
		OLDMBCAPParam.targetPointGeo = new GeoCoordinates[Param.numUAVs];
		OLDMBCAPParam.beaconsStored = new ArrayList[Param.numUAVs];

		for (int i = 0; i < Param.numUAVs; i++) {
			MBCAPParam.state[i] = MBCAPState.NORMAL;
			OLDMBCAPParam.idAvoiding.set(i, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
			OLDMBCAPParam.projectPath.set(i, 1);		// Begin projecting the predicted path over the theoretical mission

			OLDMBCAPParam.beacons[i] = new ConcurrentHashMap<Long, OLDBeacon>();
			OLDMBCAPParam.impactLocationUTM[i] = new ConcurrentHashMap<Long, Point3D>();
			OLDMBCAPParam.impactLocationPX[i] = new ConcurrentHashMap<Long, Point2D.Double>();
			
			OLDMBCAPParam.beaconsStored[i] =  new ArrayList<OLDBeacon>();
		}

		OLDMBCAPParam.progress = new ArrayList[Param.numUAVs];

		for (int i=0; i < Param.numUAVs; i++) {
			OLDMBCAPParam.progress[i] = new ArrayList<ProgressState>();
		}
		
		OLDMBCAPParam.uavDeadlockTimeout = new long[Param.numUAVs];
		
		double maxBeaconDistance;
		for (int i=0; i < Param.numUAVs; i++) {
			maxBeaconDistance = UAVParam.initialSpeeds[i] * OLDMBCAPParam.beaconFlyingTime;
			if (OLDMBCAPParam.reactionDistance < maxBeaconDistance) {
				maxBeaconDistance = OLDMBCAPParam.reactionDistance;
			}
			long aux = OLDMBCAPParam.DEADLOCK_TIMEOUT_BASE + Math.round((maxBeaconDistance * 2 / UAVParam.initialSpeeds[i]) * 1000000000l);
			OLDMBCAPParam.uavDeadlockTimeout[i] = Math.max(OLDMBCAPParam.globalDeadlockTimeout, aux);
		}
	}

	@Override
	public String setInitialState() {
		return MBCAPState.NORMAL.getName();
	}

	@Override
	public void rescaleDataStructures() {
		// Rescale the safety circles diameter
		UTMCoordinates locationUTM = null;
		boolean found = false;
		int numUAVs = Tools.getNumUAVs();
		for (int i=0; i<numUAVs && !found; i++) {
			locationUTM = Copter.getUTMLocation(i);
			if (locationUTM != null) {
				found = true;
			}
		}
		Point2D.Double a = GUI.locatePoint(locationUTM.x, locationUTM.y);
		Point2D.Double b = GUI.locatePoint(locationUTM.x + OLDMBCAPParam.collisionRiskDistance, locationUTM.y);
		OLDMBCAPParam.collisionRiskScreenDistance =b.x - a.x;
	}
	
	@Override
	public void loadResources() {
		// Load the image used to show the risk location
		URL url = OLDMBCAPHelper.class.getResource(OLDMBCAPGUIParam.EXCLAMATION_IMAGE_PATH);
		try {
			OLDMBCAPGUIParam.exclamationImage = ImageIO.read(url);
			OLDMBCAPGUIParam.exclamationDrawScale = OLDMBCAPGUIParam.EXCLAMATION_PX_SIZE
					/ OLDMBCAPGUIParam.exclamationImage.getWidth();
		} catch (IOException e) {
			GUI.exit(MBCAPText.WARN_IMAGE_LOAD_ERROR);
		}
	}
	
	@Override
	public void rescaleShownResources() {
		Iterator<Map.Entry<Long, Point3D>> entries;
		Map.Entry<Long, Point3D> entry;
		Point3D riskLocationUTM;
		Point2D.Double riskLocationPX;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			// Collision risk locations
			entries = OLDMBCAPParam.impactLocationUTM[i].entrySet().iterator();
			OLDMBCAPParam.impactLocationPX[i].clear();
			while (entries.hasNext()) {
				entry = entries.next();
				riskLocationUTM = entry.getValue();
				riskLocationPX = GUI.locatePoint(riskLocationUTM.x, riskLocationUTM.y);
				OLDMBCAPParam.impactLocationPX[i].put(entry.getKey(), riskLocationPX);
			}
		}
	}
	
	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		if (!Tools.isCollisionDetected()) {
			g2.setStroke(MBCAPParam.STROKE_POINT);
			OLDMBCAPGUITools.drawPredictedLocations(g2);
			OLDMBCAPGUITools.drawImpactRiskMarks(g2, p);
		}
	}
	
	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		// Gets the current coordinates from the mission when it is loaded, and the heading pointing towards the next waypoint
		int numUAVs = Tools.getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocations = new Pair[numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		List<Waypoint>[] missions = Tools.getLoadedMissions();
		List<Waypoint> mission;
		for (int i = 0; i < numUAVs; i++) {
			mission = missions[i];
			if (mission != null) {
				waypointFound = false;
				for (int j=0; j<mission.size() && !waypointFound; j++) {
					waypoint1 = mission.get(j);
					if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
						waypoint1pos = j;
						waypointFound = true;
					}
				}
				if (!waypointFound) {
					GUI.exit(MBCAPText.UAVS_START_ERROR_2 + " " + Tools.getIdFromPos(i));
				}
				waypointFound = false;
				for (int j=waypoint1pos+1; j<mission.size() && !waypointFound; j++) {
					waypoint2 = mission.get(j);
					if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
						waypointFound = true;
					}
				}
				if (waypointFound) {
					// We only can set a heading if at least two points with valid coordinates are found
					p1UTM = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
					p2UTM = Tools.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
					incX = p2UTM.x - p1UTM.x;
					incY = p2UTM.y - p1UTM.y;
					if (incX != 0 || incY != 0) {
						if (incX == 0) {
							if (incY > 0)	heading = 0.0;
							else			heading = 180.0;
						} else if (incY == 0) {
							if (incX > 0)	heading = 90;
							else			heading = 270.0;
						} else {
							double gamma = Math.atan(incY/incX);
							if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
							else 			heading = 270.0 - gamma * 180 / Math.PI;
						}
					}
				}
			} else {
				// Assuming that all UAVs have a mission loaded
				GUI.exit(MBCAPText.APP_NAME + ": " + MBCAPText.UAVS_START_ERROR_1 + " " + Tools.getIdFromPos(i) + ".");
			}
			startingLocations[i] = Pair.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
		}
		return startingLocations;
	}
	
	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// No special configuration is needed, as the missions are automatically loaded
		return true;
	}
	
	@Override
	public void startThreads() {
		OLDBeaconingThread thread;
		for (int i = 0; i < Param.numUAVs; i++) {
			thread = new OLDBeaconingThread(i);
			Copter.setWaypointReachedListener(thread);
			thread.start();
			(new OLDReceiverThread(i)).start();
			(new OLDCollisionDetectorThread(i)).start();
		}
		GUI.log(OLDMBCAPText.ENABLING);
	}
	
	@Override
	public void setupActionPerformed() {
		
	}

	@Override
	public void startExperimentActionPerformed() {
		if (!Copter.startMissionsFromGround()) {
			GUI.warn(this.protocolString, MBCAPText.START_MISSION_ERROR);
		}
	}
	
	@Override
	public void forceExperimentEnd() {
		// When the UAVs are close to the last waypoint a LAND command is issued
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			Copter.landIfMissionEnded(i, UAVParam.LAST_WP_THRESHOLD);
		}
	}
	
	@Override
	public String getExperimentResults() {
		// 1. Calculus of the experiment length and protocol times
		long startTime = Tools.getExperimentStartTime();
		int numUAVs = Tools.getNumUAVs();
		long[] uavsTotalTime = new long[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			uavsTotalTime[i] = Tools.getExperimentEndTime(i) - startTime;
		}
		StringBuilder sb = new StringBuilder(2000);
		long[] uavNormalTime = new long[numUAVs];
		long[] uavStandStillTime = new long[numUAVs];
		long[] uavMovingTime = new long[numUAVs];
		long[] uavGoOnPleaseTime = new long[numUAVs];
		long[] uavPassingTime = new long[numUAVs];
		long[] uavEmergencyLandTime = new long[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			long endTime = Tools.getExperimentEndTime(i);
			sb.append(MBCAPText.UAV_ID).append(" ").append(Tools.getIdFromPos(i)).append("\n");
			if (OLDMBCAPParam.progress[i].size() == 0) {
				// In this case, only the global time is available
				uavNormalTime[i] = endTime - startTime;
			} else {
				// Different steps are available, and calculated
				ProgressState[] progress = OLDMBCAPParam.progress[i].toArray(new ProgressState[OLDMBCAPParam.progress[i].size()]);

				MBCAPState iniState = MBCAPState.NORMAL;
				long iniTime = startTime;
				MBCAPState curState;
				long curTime;
				for (int j = 0; j < progress.length; j++) {
					curState = progress[j].state;
					curTime = progress[j].time;
					switch (iniState) {
					case NORMAL:
						uavNormalTime[i] = uavNormalTime[i] + curTime - iniTime;
						break;
					case STAND_STILL:
						uavStandStillTime[i] = uavStandStillTime[i] + curTime - iniTime;
						break;
					case MOVING_ASIDE:
						uavMovingTime[i] = uavMovingTime[i] + curTime - iniTime;
						break;
					case GO_ON_PLEASE:
						uavGoOnPleaseTime[i] = uavGoOnPleaseTime[i] + curTime - iniTime;
						break;
					case OVERTAKING:
						uavPassingTime[i] = uavPassingTime[i] + curTime - iniTime;
						break;
					case EMERGENCY_LAND:
						uavEmergencyLandTime[i] = uavEmergencyLandTime[i] + curTime - iniTime;
						break;
					}
					iniState = curState;
					iniTime = curTime;
				}
				// From the last to the testEndTime
				switch (progress[progress.length - 1].state) {
				case NORMAL:
					uavNormalTime[i] = uavNormalTime[i] + endTime - progress[progress.length - 1].time;
					break;
				case STAND_STILL:
					uavStandStillTime[i] = uavStandStillTime[i] + endTime - progress[progress.length - 1].time;
					break;
				case MOVING_ASIDE:
					uavMovingTime[i] = uavMovingTime[i] + endTime - progress[progress.length - 1].time;
					break;
				case GO_ON_PLEASE:
					uavGoOnPleaseTime[i] = uavGoOnPleaseTime[i] + endTime - progress[progress.length - 1].time;
					break;
				case OVERTAKING:
					uavPassingTime[i] = uavPassingTime[i] + endTime - progress[progress.length - 1].time;
					break;
				case EMERGENCY_LAND:
					uavEmergencyLandTime[i] = uavEmergencyLandTime[i] + endTime - progress[progress.length - 1].time;
					break;
				}
			}
			sb.append(MBCAPState.NORMAL.getName()).append(" = ").append(Tools.timeToString(0, uavNormalTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavNormalTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPState.STAND_STILL.getName()).append(" = ").append(Tools.timeToString(0, uavStandStillTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavStandStillTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPState.MOVING_ASIDE.getName()).append(" = ").append(Tools.timeToString(0, uavMovingTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavMovingTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPState.GO_ON_PLEASE.getName()).append(" = ").append(Tools.timeToString(0, uavGoOnPleaseTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavGoOnPleaseTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPState.OVERTAKING.getName()).append(" = ").append(Tools.timeToString(0, uavPassingTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavPassingTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPState.EMERGENCY_LAND.getName()).append(" = ").append(Tools.timeToString(0, uavEmergencyLandTime[i])).append(" (")
			.append(String.format("%.2f%%", 100 * uavEmergencyLandTime[i] / (double) uavsTotalTime[i])).append(")\n");
			sb.append(MBCAPText.SITUATIONS_SOLVED).append(" ").append(OLDMBCAPParam.event.get(i)).append("\n");
			sb.append(MBCAPText.DEADLOCKS).append(" ").append(MBCAPParam.eventDeadlockSolved.get(i)).append("\n");
			sb.append(MBCAPText.DEADLOCKS_FAILED).append(" ").append(MBCAPParam.eventDeadlockFailed.get(i)).append("\n");
		}
		
		
		//TODO quitar
		sb.append("Colisiones: " + CollisionDetector.collisions + "\n");//TODO quitar
		
		
		return sb.toString();
	}
	
	@Override
	public String getExperimentConfiguration() {
		return "";
	}
	
	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {

	}
	
	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		
	}
	
	/** Predicts the future positions of the UAV. Returns empty list in case of problems. */
	public static List<Point3D> getPredictedPath(int numUAV, double speed, double acceleration, Point2D.Double currentUTMLocation, double currentZ) {
		List<Point3D> predictedPath = new ArrayList<Point3D>(OLDMBCAPParam.POINTS_SIZE);
		// Multiple cases to study:
		
		// 1. At the beginning, without GPS, the initial position can be null
		if (currentUTMLocation == null) {
			return predictedPath;
		}
		
		// 2. If the UAV is in the "go on, please" state, only the current position and the predicted collision risk location are sent
		if (MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE) {
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			Point3D riskLocation = OLDMBCAPParam.impactLocationUTM[numUAV].get(OLDMBCAPParam.selfBeacon.get(numUAV).idAvoiding);
			if (riskLocation != null) {
				predictedPath.add(riskLocation);
			} else {
				predictedPath.add(new Point3D(0, 0, 0));	// If no risk point was detected, a fake point is sent
			}
			return predictedPath;
		}

		// 3. If moving slowly or still but not in the "stand still" state, only the current position is sent
		if (speed < OLDMBCAPParam.minSpeed && MBCAPParam.state[numUAV] != MBCAPState.STAND_STILL) {
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			return predictedPath;
		}

		// 4. If the UAV is moving aside, send the current position and prediction towards the destination safe point
		if (MBCAPParam.state[numUAV] == MBCAPState.MOVING_ASIDE) {
			Point2D.Double destination = OLDMBCAPParam.targetPointUTM[numUAV];
			double length = currentUTMLocation.distance(destination);
			int numLocations1 = (int)Math.ceil(length/(speed * OLDMBCAPParam.hopTime));
			int numLocations2 = (int) Math.ceil(OLDMBCAPParam.beaconFlyingTime / OLDMBCAPParam.hopTime);
			int numLocations = Math.min(numLocations1, numLocations2);
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			double xUTM, yUTM;
			double incX = (destination.x - currentUTMLocation.x) * (speed * OLDMBCAPParam.hopTime)/length;
			double incY = (destination.y - currentUTMLocation.y) * (speed * OLDMBCAPParam.hopTime)/length;
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
				GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.WAYPOINT_LOST);
				return predictedPath;
			}
			
			// 5. In the stand still state, send the waypoint list so the other UAV could decide whether to move aside or not
			if (MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL) {
				// The first point is the current position
				predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
				// The rest of the points are the waypoints not reached jet
//				for (int i=posNextWaypoint; i<mission.size(); i++) {
//					predictedPath.add(new Point3D(mission.get(i).x, mission.get(i).y, mission.get(i).z));
//				}
//				return predictedPath;
				
				
				//TODO en la versión antigua también limitamos el número de waypoints enviados o se bloquea
				int locations = 1;
				int i=posNextWaypoint;
				while (i<mission.size() && locations < MBCAPParam.MAX_BEACON_LOCATIONS
						&& mission.get(i).distance(currentUTMLocation) < MBCAPParam.MAX_WAYPOINT_DISTANCE) {
					predictedPath.add(new Point3D(mission.get(i).x, mission.get(i).y, mission.get(i).z));
					locations++;
					i++;
				}
				// May be the next waypoint is too far, so we add an additional waypoint
				if (i<mission.size() && locations < MBCAPParam.MAX_BEACON_LOCATIONS) {
					predictedPath.add(new Point3D(mission.get(i).x, mission.get(i).y, mission.get(i).z));
					locations++;
				}
				return predictedPath;
				
				
				
				
				
			}

			// 6. When really close to the last waypoint, only the current location is sent
			if (posNextWaypoint == mission.size()) {
				posNextWaypoint--; // So it could be drawn until the end
				if (currentUTMLocation.distance(mission.get(posNextWaypoint)) < OLDMBCAPParam.DISTANCE_TO_MISSION_END) {
					predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
					return predictedPath;
				}
			}

			// 7. General case
			// Initial location calculated depending on the protocol version
			Pair<Point2D.Double, Integer> currentLocation = OLDMBCAPHelper.getCurrentLocation(numUAV, mission, currentUTMLocation, posNextWaypoint);
			currentUTMLocation = currentLocation.getValue0();
			posNextWaypoint = currentLocation.getValue1();
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			// Calculate the rest of the points depending on the protocol version
			if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1)
					|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
					|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD")) {
				OLDMBCAPHelper.getPredictedLocations1(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
			} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V4)) {
				OLDMBCAPHelper.getPredictedLocations2(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
			}
		}
		return predictedPath;
	}

	/** Calculates the first point of the predicted positions list depending on the protocol version.
	 * <p>It also decides which is the next waypoint the UAV is moving towards, also depending on the protocol version. */
	private static Pair<Point2D.Double, Integer> getCurrentLocation(int numUAV, List<WaypointSimplified> mission, Point2D.Double currentUTMLocation, int posNextWaypoint) {
		Point2D.Double baseLocation = null;
		int nextWaypointPosition = posNextWaypoint;
		if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1)) { // MBCAP v1
			// The next waypoint position has been already set
			// The first predicted position is the current coordinates
			baseLocation = currentUTMLocation;

		} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
				|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD")
				|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V4)) {
			// MBCAP v2, MBCAP v3 or MBCAP v4
			if (OLDMBCAPParam.projectPath.get(numUAV) == 0) {
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
					Point2D.Double prevIntersection = OLDMBCAPHelper.getIntersection(currentUTMLocation, location1, location2);
					Point2D.Double postIntersection = OLDMBCAPHelper.getIntersection(currentUTMLocation, location2, location3);
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
		List<Double> distances = new ArrayList<Double>(OLDMBCAPParam.DISTANCES_SIZE);
		double totalDistance = 0.0;
		// MBCAP v1 or MBCAP v2 (no acceleration present)
		if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1) || ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
				|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD") && acceleration == 0.0)) {
			totalDistance = OLDMBCAPParam.beaconFlyingTime * speed;
		} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD")) {
			// MBCAP v3 (constant acceleration present)
			totalDistance = acceleration*OLDMBCAPParam.beaconFlyingTime*OLDMBCAPParam.beaconFlyingTime/2.0
					+ speed*OLDMBCAPParam.beaconFlyingTime;
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
		int remainingLocations = (int) Math.ceil(OLDMBCAPParam.beaconFlyingTime / OLDMBCAPParam.hopTime);
		double incDistance = 0.0;
		double prevSegmentsLength = 0.0;

		if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1) || ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
				|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD") && acceleration == 0.0)) {
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

			if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1) || ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
					|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD")	&& acceleration == 0)) {
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
			} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3 + "OLD")) {
				// the distance increment has to be calculated each time
				double maxAcumDistance = Math.min(currentSegmentLength + prevSegmentsLength, totalDistance);
				double maxTotalTime = (-speed + Math.sqrt(speed*speed + 2*acceleration*maxAcumDistance))/acceleration;
				int totalHops = (int)Math.floor(maxTotalTime/OLDMBCAPParam.hopTime);

				while (locations < totalHops
						&& longPrev < prevSegmentsLength + currentSegmentLength
						&& longPrev < totalDistance) {
					double time = (locations+1)*OLDMBCAPParam.hopTime;
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
		List<Double> distances = new ArrayList<Double>(OLDMBCAPParam.DISTANCES_SIZE);
		if (acceleration == 0.0) {
			// The same solution from the previous protocol versions
			double totalDistance = OLDMBCAPParam.beaconFlyingTime * speed;
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
			int remainingLocations = (int) Math.ceil(OLDMBCAPParam.beaconFlyingTime / OLDMBCAPParam.hopTime);
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
			int totalLocations = (int) Math.ceil(OLDMBCAPParam.beaconFlyingTime / OLDMBCAPParam.hopTime);
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
					double time = (locations+1)*OLDMBCAPParam.hopTime;
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

	/** Calculates if two UAVs have collision risk.
	 * <p>Requires to be in the normal protocol state. */
	public static boolean hasCollisionRisk(int numUAV, OLDBeacon selfBeacon, OLDBeacon receivedBeacon) {
		double distance;
		long selfTime, beaconTime;
		int maxPoints = (int)Math.ceil(OLDMBCAPParam.reactionDistance / (selfBeacon.speed * OLDMBCAPParam.hopTime)) + 1;
		Point3D selfPoint, receivedPoint;
		for (int i = 0; i < selfBeacon.points.size() && i < maxPoints; i++) {
			selfTime = selfBeacon.time + i * OLDMBCAPParam.hopTimeNS;
			selfPoint = selfBeacon.points.get(i);
			for (int j = 0; j < receivedBeacon.points.size(); j++) {
				// Do not check if the other UAV is in Go on, please and the location is the detected risk location
				if (j == 1
						&& (receivedBeacon.state == MBCAPState.GO_ON_PLEASE.getId() || receivedBeacon.state == MBCAPState.STAND_STILL.getId())) {
					break;
				}
				
				boolean risky = true;
				// Temporal collision risk (only if the other UAV is also in the normal protocol state)
				if (receivedBeacon.state == MBCAPState.NORMAL.getId()) {
					beaconTime = receivedBeacon.time + j * OLDMBCAPParam.hopTimeNS;
					if (Math.abs(selfTime - beaconTime) >= OLDMBCAPParam.collisionRiskTime) {
						risky = false;
					}
				}
				
				// X,Y, Z collision risk
				if (risky) {
					receivedPoint = receivedBeacon.points.get(j);
					distance = selfPoint.distance(receivedPoint);
					if (distance < OLDMBCAPParam.collisionRiskDistance
							&& Math.abs(selfPoint.z - receivedPoint.z) < OLDMBCAPParam.collisionRiskAltitudeDifference) {
						OLDMBCAPParam.impactLocationUTM[numUAV].put(receivedBeacon.uavId, selfPoint);
						OLDMBCAPGUITools.locateImpactRiskMark(selfPoint, numUAV, receivedBeacon.uavId);
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Calculates if the current UAV has overtaken another UAV. */
	public static boolean hasOvertaken(int numUAV, long avoidingId, Point2D.Double target) {
		// Overtaken happened if the distance to the target UAV and to the risk location is increasing
		boolean success = MBCAPv3Helper.isMovingAway(numUAV, target);
		if (!success) {
			return false;
		}
		Point3D riskLocation = OLDMBCAPParam.impactLocationUTM[numUAV].get(avoidingId);
		if (riskLocation == null) {
			return true;
		} else {
			return MBCAPv3Helper.isMovingAway(numUAV, riskLocation);
		}
	}

	/** Checks if the UAV has to move aside, and that safe point when needed. */
	public static boolean needsReposition(int numUAV, List<Point3D> avoidPredictedLocations) {
		// Errors detection
		if (avoidPredictedLocations == null || avoidPredictedLocations.size() <= 1) {
			GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.REPOSITION_ERROR_1);
			return false;
		}
		if (SimParam.zone < 0) {
			GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.REPOSITION_ERROR_2);
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
				auxLocation = OLDMBCAPHelper.getSafeLocation(currentUTMLocation, prevLocation, postLocation);
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
			OLDMBCAPParam.targetPointUTM[numUAV] = newLocation;										// To show on screen
			OLDMBCAPParam.targetPointGeo[numUAV] = Tools.UTMToGeo(newLocation.x, newLocation.y);	// To order the movement
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
			if (Math.abs(currentLocation.x-prev.x)>OLDMBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			if (currentLocation.x < prev.x) {
				x = prev.x - OLDMBCAPParam.safePlaceDistance - OLDMBCAPParam.PRECISION_MARGIN;
			} else {
				x = prev.x + OLDMBCAPParam.safePlaceDistance + OLDMBCAPParam.PRECISION_MARGIN;
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
			if (Math.abs(currentLocation.y-prev.y)>OLDMBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			if (currentLocation.y < prev.y) {
				y = prev.y - OLDMBCAPParam.safePlaceDistance - OLDMBCAPParam.PRECISION_MARGIN;
			} else {
				y = prev.y + OLDMBCAPParam.safePlaceDistance + OLDMBCAPParam.PRECISION_MARGIN;
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
			if (distIntersection>OLDMBCAPParam.safePlaceDistance) {
				return null;
			}
			// Has to move apart case
			double ds = OLDMBCAPParam.safePlaceDistance + OLDMBCAPParam.PRECISION_MARGIN;
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
	
}
