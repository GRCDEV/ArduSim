package mbcap.logic;

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
import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param.SimulatorState;
import mbcap.gui.MBCAPConfigDialog;
import mbcap.gui.MBCAPGUIParam;
import mbcap.gui.MBCAPGUITools;
import mbcap.gui.MBCAPPCCompanionDialog;
import mbcap.pojo.Beacon;
import mbcap.pojo.ErrorPoint;
import mbcap.pojo.MBCAPState;
import mbcap.pojo.ProgressState;
import sim.board.BoardPanel;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class MBCAPHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_TEXT;
	}
	
	@Override
	public boolean loadMission() {
		return true;
	}

	@Override
	public void openConfigurationDialog() {
		new MBCAPConfigDialog();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initializeDataStructures() {
		int numUAVs = Tools.getNumUAVs();
		MBCAPGUIParam.predictedLocation = new AtomicReferenceArray<List<Point3D>>(numUAVs);

		MBCAPParam.event = new AtomicIntegerArray(numUAVs);
		MBCAPParam.eventDeadlockSolved = new AtomicIntegerArray(numUAVs);
		MBCAPParam.eventDeadlockFailed = new AtomicIntegerArray(numUAVs);
		MBCAPParam.state = new MBCAPState[numUAVs];
		MBCAPParam.idAvoiding = new AtomicLongArray(numUAVs);
		MBCAPParam.projectPath = new AtomicIntegerArray(numUAVs);

		MBCAPParam.selfBeacon = new AtomicReferenceArray<Beacon>(numUAVs);
		MBCAPParam.beacons = new ConcurrentHashMap[numUAVs];
		MBCAPParam.impactLocationUTM = new ConcurrentHashMap[numUAVs];
		MBCAPParam.impactLocationPX = new ConcurrentHashMap[numUAVs];

		MBCAPParam.targetLocationUTM = new AtomicReferenceArray<UTMCoordinates>(numUAVs);
		MBCAPParam.targetLocationPX = new AtomicReferenceArray<Point2D.Double>(numUAVs);
		MBCAPParam.beaconsStored = new ArrayList[numUAVs];

		for (int i = 0; i < numUAVs; i++) {
			MBCAPParam.state[i] = MBCAPState.NORMAL;
			MBCAPParam.idAvoiding.set(i, MBCAPParam.ID_AVOIDING_DEFAULT);
			MBCAPParam.projectPath.set(i, 1);		// Begin projecting the predicted path over the theoretical mission

			MBCAPParam.beacons[i] = new ConcurrentHashMap<Long, Beacon>();
			MBCAPParam.impactLocationUTM[i] = new ConcurrentHashMap<Long, Point3D>();
			MBCAPParam.impactLocationPX[i] = new ConcurrentHashMap<Long, Point2D.Double>();
			
			MBCAPParam.beaconsStored[i] =  new ArrayList<Beacon>();
		}

		MBCAPParam.progress = new ArrayList[numUAVs];

		for (int i=0; i < numUAVs; i++) {
			MBCAPParam.progress[i] = new ArrayList<ProgressState>();
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
		Point2D.Double b = GUI.locatePoint(locationUTM.x + MBCAPParam.collisionRiskDistance, locationUTM.y);
		MBCAPParam.collisionRiskScreenDistance =b.x - a.x;
	}

	@Override
	public void loadResources() {
		// Load the image used to show the risk location
		URL url = MBCAPHelper.class.getResource(MBCAPGUIParam.EXCLAMATION_IMAGE_PATH);
		try {
			MBCAPGUIParam.exclamationImage = ImageIO.read(url);
			MBCAPGUIParam.exclamationDrawScale = MBCAPGUIParam.EXCLAMATION_PX_SIZE
					/ MBCAPGUIParam.exclamationImage.getWidth();
		} catch (IOException e) {
			GUI.exit(MBCAPText.WARN_IMAGE_LOAD_ERROR);
		}
	}
	
	@Override
	public void rescaleShownResources() {
		Iterator<Map.Entry<Long, Point3D>> entries;
		Map.Entry<Long, Point3D> entry;
		Point3D riskLocationUTM;
		UTMCoordinates targetLocationUTM;
		Point2D.Double riskLocationPX, targetLocationPX;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			// Collision risk locations
			entries = MBCAPParam.impactLocationUTM[i].entrySet().iterator();
			MBCAPParam.impactLocationPX[i].clear();
			while (entries.hasNext()) {
				entry = entries.next();
				riskLocationUTM = entry.getValue();
				riskLocationPX = GUI.locatePoint(riskLocationUTM.x, riskLocationUTM.y);
				MBCAPParam.impactLocationPX[i].put(entry.getKey(), riskLocationPX);
			}
			
			// Target locations
			targetLocationUTM = MBCAPParam.targetLocationUTM.get(i);
			if (targetLocationUTM == null) {
				MBCAPParam.targetLocationPX.set(i, null);
			} else {
				targetLocationPX = GUI.locatePoint(targetLocationUTM.x, targetLocationUTM.y);
				MBCAPParam.targetLocationPX.set(i, targetLocationPX);
			}
		}
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		if (!Tools.isCollisionDetected()) {
			g2.setStroke(MBCAPParam.STROKE_POINT);
			MBCAPGUITools.drawPredictedLocations(g2);
			MBCAPGUITools.drawImpactRiskMarks(g2, p);
			MBCAPGUITools.drawSafetyLocation(g2);
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
		BeaconingThread thread;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			thread = new BeaconingThread(i);
			Copter.setWaypointReachedListener(thread);
			thread.start();
			(new ReceiverThread(i)).start();
			(new CollisionDetectorThread(i)).start();
		}
		GUI.log(MBCAPText.ENABLING);
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
			if (MBCAPParam.progress[i].size() == 0) {
				// In this case, only the global time is available
				uavNormalTime[i] = endTime - startTime;
			} else {
				// Different steps are available, and calculated
				ProgressState[] progress = MBCAPParam.progress[i].toArray(new ProgressState[MBCAPParam.progress[i].size()]);

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
			sb.append(MBCAPText.SITUATIONS_SOLVED).append(" ").append(MBCAPParam.event.get(i)).append("\n");
			sb.append(MBCAPText.DEADLOCKS).append(" ").append(MBCAPParam.eventDeadlockSolved.get(i)).append("\n");
			sb.append(MBCAPText.DEADLOCKS_FAILED).append(" ").append(MBCAPParam.eventDeadlockFailed.get(i)).append("\n");
		}
		return sb.toString();
	}

	@Override
	public String getExperimentConfiguration() {
		StringBuilder sb = new StringBuilder(2000);
		sb.append(MBCAPText.BEACONING_PARAM);
		sb.append("\n\t").append(MBCAPText.BEACON_INTERVAL).append(" ").append(MBCAPParam.beaconingPeriod).append(" ").append(MBCAPText.MILLISECONDS);
		sb.append("\n\t").append(MBCAPText.BEACON_REFRESH).append(" ").append(MBCAPParam.numBeacons);
		sb.append("\n\t").append(MBCAPText.INTERSAMPLE).append(" ").append(MBCAPParam.hopTime).append(" ").append(MBCAPText.MILLISECONDS);
		sb.append("\n\t").append(MBCAPText.MIN_ADV_SPEED).append(" ").append(MBCAPParam.minSpeed).append(" ").append(MBCAPText.METERS_PER_SECOND);
		sb.append("\n\t").append(MBCAPText.BEACON_EXPIRATION).append(" ").append(String.format( "%.2f", MBCAPParam.beaconExpirationTime*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n").append(MBCAPText.AVOID_PARAM);
		sb.append("\n\t").append(MBCAPText.WARN_DISTANCE).append(" ").append(MBCAPParam.collisionRiskDistance).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.WARN_ALTITUDE).append(" ").append(MBCAPParam.collisionRiskAltitudeDifference).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.WARN_TIME).append(" ").append(String.format( "%.2f", MBCAPParam.collisionRiskTime*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.CHECK_PERIOD).append(" ").append(String.format( "%.2f", MBCAPParam.riskCheckPeriod*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.PACKET_LOSS_THRESHOLD).append(" ").append(MBCAPParam.packetLossThreshold);
		sb.append("\n\t").append(MBCAPText.GPS_ERROR).append(" ").append(MBCAPParam.gpsError).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.HOVERING_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.standStillTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.OVERTAKE_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.passingTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.RESUME_MODE_DELAY).append(" ").append(String.format( "%.2f", MBCAPParam.resumeTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.RECHECK_DELAY).append(" ").append(String.format( "%.2f", MBCAPParam.recheckTimeout*0.001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.DEADLOCK_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.globalDeadlockTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		return sb.toString();
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// Logging to file the error predicting the location during the experiment (and the beacons itself if needed).
		int numUAVs = Tools.getNumUAVs();
		@SuppressWarnings("unchecked")
		List<ErrorPoint>[] realUAVPaths = new ArrayList[numUAVs];

		// 1. UAV path calculus (only experiment path, and ignoring repeated positions)
		LogPoint realPostLocation, realPrevLocation;
		double time;
		int inTestState = SimulatorState.TEST_IN_PROGRESS.getStateId();
		Double x, y;
		for (int i=0; i<numUAVs; i++) {
			realPrevLocation = null;
			realUAVPaths[i] = new ArrayList<ErrorPoint>();

			List<LogPoint> fullPath = Tools.getUTMPath(i);
			for (int j = 0; j < fullPath.size(); j++) {
				realPostLocation = fullPath.get(j);

				// Considers only not repeated locations and only generated during the experiment
				if (realPostLocation.getSimulatorState() == inTestState) {
					time = realPostLocation.getTime();
					x = realPostLocation.x;
					y = realPostLocation.y;
					if (realPrevLocation == null) {
						// First test location
						realUAVPaths[i].add(new ErrorPoint(0, x, y));
						if (realPostLocation.getTime() != 0) {
							realUAVPaths[i].add(new ErrorPoint(time, x, y));
						}
						realPrevLocation = realPostLocation;
					} else if (realPostLocation.x!=realPrevLocation.x || realPostLocation.y!=realPrevLocation.y || realPostLocation.z!=realPrevLocation.z) {
						// Moved
						realUAVPaths[i].add(new ErrorPoint(realPostLocation.getTime(), x, y));
						realPrevLocation = realPostLocation;
					}
				}
			}
		}
		
		// 2. Predicted positions calculus and storage of beacons
		File beaconsFile = null;
		File maxErrorFile = null;
		File beaconsErrorFile, timeErrorFile;
		StringBuilder sb1 = null;
		StringBuilder sb2 = null;
		StringBuilder sb3, sb4;
		int j;
		ErrorPoint predictedLocation;
		@SuppressWarnings("unchecked")
		List<List<ErrorPoint>>[] totalPredictedLocations = new ArrayList[numUAVs];
		boolean verboseStore = Tools.isVerboseStorageEnabled();
		// For each UAV
		for (int i=0; i<numUAVs; i++) {
			List<Beacon> beacons = MBCAPParam.beaconsStored[i];
			totalPredictedLocations[i] = new ArrayList<List<ErrorPoint>>(beacons.size());
			
			if (verboseStore) {
				// Store each beacon also
				beaconsFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.BEACONS_SUFIX);
				sb1 = new StringBuilder(2000);
				sb1.append("time(s),x1,y2,x2,y2,...,xn,yn\n");
			}
			
			// For each beacon
			Beacon beacon;
			for (j=0; j<beacons.size(); j++) {
				beacon = beacons.get(j);
				time = Tools.round(((double) (beacon.time - baseNanoTime)) / 1000000000l, 9);
				if (time >= 0 && time <= realUAVPaths[i].get(realUAVPaths[i].size() - 1).time
						&& beacon.points!=null && beacon.points.size()>0) {
					if (verboseStore) {
						sb1.append(time);
					}
					
					List<Point3D> locations = beacon.points;
					List<ErrorPoint> predictions = new ArrayList<ErrorPoint>(beacon.points.size());
					// For each point in each beacon
					for (int k=0; k<locations.size(); k++) {
						if (verboseStore) {
							sb1.append(",").append(Tools.round(locations.get(k).x, 3))
								.append(",").append(Tools.round(locations.get(k).y, 3));
						}
						predictedLocation = new ErrorPoint(time + MBCAPParam.hopTime*k, locations.get(k).x, locations.get(k).y);
						// Predicted positions for later calculus of the error in prediction
						predictions.add(predictedLocation);
					}
					totalPredictedLocations[i].add(predictions);
					if (verboseStore) {
						sb1.append("\n");
					}
				}
			}
			if (verboseStore) {
				Tools.storeFile(beaconsFile, sb1.toString());
			}

			// 3. Calculus of the mean and maximum distance error on each beacon
			//    Only store information if useful beacons were found
			int numBeacons = totalPredictedLocations[i].size();
			if (numBeacons > 0) {
				ErrorPoint[] realUAVPath = realUAVPaths[i].toArray(new ErrorPoint[realUAVPaths[i].size()]);
				List<List<ErrorPoint>> predictedLocations = totalPredictedLocations[i];
				double[] maxBeaconDistance = new double[numBeacons]; // One per beacon
				double[] meanBeaconDistance = new double[numBeacons];
				if (Tools.isVerboseStorageEnabled()) {
					// Log the line from the real to the predicted location, with maximum error
					maxErrorFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.MAX_ERROR_LINES_SUFIX);
					sb2 = new StringBuilder(2000);
				}
				// For each beacon get the max and mean distance errors
				for (j=0; j<predictedLocations.size(); j++) {
					Pair<UTMCoordinates, UTMCoordinates> pair =
							beaconErrorCalculation(realUAVPath, predictedLocations, j,
									maxBeaconDistance, j, meanBeaconDistance);
					if (pair!=null && Tools.isVerboseStorageEnabled()) {
						sb2.append("._LINE\n");
						sb2.append(Tools.round(pair.getValue0().x, 3)).append(",")
							.append(Tools.round(pair.getValue0().y, 3)).append("\n");
						sb2.append(Tools.round(pair.getValue1().x, 3)).append(",")
							.append(Tools.round(pair.getValue1().y, 3)).append("\n\n");
					}
				}
				if (Tools.isVerboseStorageEnabled()) {
					Tools.storeFile(maxErrorFile, sb2.toString());
				}
				
				// 4. Storage of the mean and maximum distance error on each beacon
				beaconsErrorFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.BEACON_TOTAL_ERROR_SUFIX);
				sb3 = new StringBuilder(2000);
				sb3.append("max(m),mean(m)\n");
				for (int k=0; k<maxBeaconDistance.length-1; k++) {
					sb3.append(Tools.round(maxBeaconDistance[k], 3)).append(",")
						.append(Tools.round(meanBeaconDistance[k], 3)).append("\n");
				}
				sb3.append(Tools.round(maxBeaconDistance[maxBeaconDistance.length-1], 3)).append(",")
					.append(Tools.round(meanBeaconDistance[meanBeaconDistance.length-1], 3));
				Tools.storeFile(beaconsErrorFile, sb3.toString());
				
				// 5. Calculus and storage of the mean and maximum distance error on each position of each beacon
				// First, get the maximum size of the beacons
				int size = 0;
				for (int m = 0; m<predictedLocations.size(); m++) {
					if (predictedLocations.get(m).size()>size) {
						size = predictedLocations.get(m).size();
					}
				}
				
				double[] maxTimeDistance = new double[size];
				double[] meanTimeDistance = new double[size];
				timeErrorCalculation(realUAVPath, predictedLocations, maxTimeDistance, meanTimeDistance);
				timeErrorFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.BEACON_POINT_ERROR_SUFIX);
				sb4 = new StringBuilder(2000);
				sb4.append("max(m),mean(m)\n");
				for (int k=0; k<maxTimeDistance.length-1; k++) {
					sb4.append(Tools.round(maxTimeDistance[k], 3)).append(",")
						.append(Tools.round(meanTimeDistance[k], 3)).append("\n");
				}
				sb4.append(Tools.round(maxTimeDistance[maxTimeDistance.length-1], 3)).append(",")
					.append(Tools.round(meanTimeDistance[meanTimeDistance.length-1], 3));
				Tools.storeFile(timeErrorFile, sb4.toString());
			}
		}
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		MBCAPPCCompanionDialog.mbcap = new MBCAPPCCompanionDialog(PCCompanionFrame);
	}
	
	/** Auxiliary method to calculate the mean and maximum error in the prediction error of each beacon.
	 * Also returns the starting and ending point of the line that represents the maximum distance error in the beacon. */
	private static Pair<UTMCoordinates, UTMCoordinates> beaconErrorCalculation(
			ErrorPoint[] realUAVPath, List<List<ErrorPoint>> predictedLocations, int predictedPos,
			double[] maxBeaconDistance, int distancePos, double[] meanBeaconDistance) {
		double dist;
		int num = 0;
		UTMCoordinates ini, fin = ini = null;
		List<ErrorPoint> predictedLocs = predictedLocations.get(predictedPos);
		ErrorPoint predictedLocation, realPrev, realPost;
		predictedLocation = null;
		UTMCoordinates realIntersection;
		int prev, post;

		// Calculus for each one of the predicted locations
		for (int i=0; i<predictedLocs.size(); i++) {
			predictedLocation = predictedLocs.get(i);
			// Ignores points too far on time or outside the real path
			if (predictedLocation.time >= realUAVPath[0].time
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
				realIntersection = new UTMCoordinates(x, y);
				
				dist = predictedLocation.distance(realIntersection);
				meanBeaconDistance[distancePos] = meanBeaconDistance[distancePos] + dist;
				// Checking if the distance is greater than the previous
				if (dist > maxBeaconDistance[distancePos]) {
					maxBeaconDistance[distancePos] = dist;
					ini = predictedLocation;
					fin = realIntersection;
				}
				
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
	private static void timeErrorCalculation(ErrorPoint[] realUAVPath, List<List<ErrorPoint>> predictedLocations,
			double[] maxTimeDistance, double[] meanTimeDistance) {
		double dist;
		int[] num = new int[maxTimeDistance.length];
		List<ErrorPoint> predictedLocs;
		ErrorPoint predictedLocation, realPrev, realPost;
		UTMCoordinates realIntersection;
		int prev, post;

		// For each beacon
		for (int i=0; i<predictedLocations.size(); i++) {
			predictedLocs = predictedLocations.get(i);
			// For each position in the beacon
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
					realIntersection = new UTMCoordinates(x, y);
					
					dist = predictedLocation.distance(realIntersection);
					meanTimeDistance[j] = meanTimeDistance[j] + dist;
					// Checking if the distance is greater than the previous
					if (dist>maxTimeDistance[j]) {
						maxTimeDistance[j] = dist;
					}
					num[j]++;
				}
			}
		}

		for (int i=0; i<meanTimeDistance.length; i++) {
			if (num[i] != 0) {
				meanTimeDistance[i] = meanTimeDistance[i]/num[i];
			}
		}
	}
	
	/** Predicts the future positions of the UAV. Returns empty list in case of problems. */
	public static List<Point3D> getPredictedPath(int numUAV, double speed, double acceleration, UTMCoordinates currentUTMLocation, double currentZ) {
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
			UTMCoordinates destination = MBCAPParam.targetLocationUTM.get(numUAV);
			double length = destination.distance(currentUTMLocation);
			int numLocations1 = (int)Math.ceil(length/(speed * MBCAPParam.hopTime));
			int numLocations2 = (int)Math.ceil(MBCAPHelper.getReactionDistance(speed) / (speed * MBCAPParam.hopTime));
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
		int currentWaypoint = Copter.getCurrentWaypoint(numUAV);
		List<WaypointSimplified> mission = Tools.getUAVMissionSimplified(numUAV);
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
				GUI.log(numUAV, MBCAPText.WAYPOINT_LOST);
				return predictedPath;
			}
			
			// 5. In the stand still state, send remaining waypoints so the other UAV could decide whether to move aside or not
			if (MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL) {
				// The first point is the current position
				predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
				// The rest of the points are the waypoints not reached jet, if they fit in the beacon
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
				if (currentUTMLocation.distance(mission.get(posNextWaypoint)) < MBCAPParam.DISTANCE_TO_MISSION_END) {
					predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
					return predictedPath;
				}
			}

			// 7. General case
			// Initial location calculated depending on the protocol version
			Pair<UTMCoordinates, Integer> currentLocation = MBCAPHelper.getCurrentLocation(numUAV, mission, currentUTMLocation, posNextWaypoint);
			currentUTMLocation = currentLocation.getValue0();
			posNextWaypoint = currentLocation.getValue1();
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			// Calculate the rest of the points
			MBCAPHelper.getPredictedLocations(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
		}
		return predictedPath;
	}

	/** Calculates the first point of the predicted positions list.
	 * <p>It also decides which is the next waypoint the UAV is moving towards.</p> */
	private static Pair<UTMCoordinates, Integer> getCurrentLocation(int numUAV, List<WaypointSimplified> mission, UTMCoordinates currentUTMLocation, int posNextWaypoint) {
		UTMCoordinates baseLocation = null;
		int nextWaypointPosition = posNextWaypoint;
		if (MBCAPParam.projectPath.get(numUAV) == 0) {
			// The next waypoint position has been already set
			// The first predicted position is the current coordinates
			baseLocation = currentUTMLocation;
		} else {
			// It is necessary to project the current location over the planned path
			if (posNextWaypoint <= 1) {
				// If we are over the first mission segment, the first predicted position is the current coordinates
				baseLocation = currentUTMLocation;
				// The next waypoint position has been already set
			} else {
				// In any other case... (there is a previous segment in the planned path)
				// The current location can be projected over the previous or the following segment
				WaypointSimplified sp1 = mission.get(posNextWaypoint-2);	// Segment 1 previous point
				UTMCoordinates location1 = new UTMCoordinates(sp1.x, sp1.y);
				WaypointSimplified sp2 = mission.get(posNextWaypoint-1);	// Segments 1-2 joining point
				UTMCoordinates location2 = new UTMCoordinates(sp2.x, sp2.y);
				WaypointSimplified sp3 = mission.get(posNextWaypoint);		// Segment 2 final point
				UTMCoordinates location3 = new UTMCoordinates(sp3.x, sp3.y);

				// Detect if moving towards the conflicting waypoint or from it
				boolean waypointOvertaken = MBCAPHelper.isMovingAway(numUAV, location2);
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
	private static void getPredictedLocations(int numUAV, double speed, double acceleration,
			UTMCoordinates currentUTMLocation, List<WaypointSimplified> mission, int posNextWaypoint, int currentWaypoint, double currentZ,
			List<Point3D> predictedPath) {
		// 1. Calculus of the segments over which we have to obtain the points
		List<Double> distances = new ArrayList<Double>(MBCAPParam.DISTANCES_SIZE);
		double totalDistance = 0.0;
		// MBCAP v1 or MBCAP v2 (no acceleration present)
		int remainingLocations = (int)Math.ceil(MBCAPHelper.getReactionDistance(speed) / (speed * MBCAPParam.hopTime));
		double flyingTime = remainingLocations * MBCAPParam.hopTime;
		if (acceleration == 0.0) {
			totalDistance = flyingTime * speed;
		} else {
			// Constant acceleration present
			totalDistance = acceleration * flyingTime * flyingTime / 2.0 + speed * flyingTime;
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
			} else {
				// the distance increment has to be calculated for each mission segment
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

	/** Gets the intersection point of a line (prev->post) and the perpendicular one which includes a third point (currentUTMLocation). */
	private static UTMCoordinates getIntersection(Point2D.Double currentUTMLocation, Point2D.Double prev, Point2D.Double post) {
		double currentX = currentUTMLocation.x;
		double currentY = currentUTMLocation.y;
		double prevX = prev.x;
		double prevY = prev.y;
		double postX = post.x;
		double postY = post.y;
		
		double x, y;
		// Vertical line case
		if (postX - prev.x == 0) {
			x = prev.x;
			y = currentY;
		} else if (postY - prev.y == 0) {
			// Horizontal line case
			y = prev.y;
			x = currentX;
		} else {
			// General case
			
			double slope = (postY - prevY) / (postX - prevX);
			x = (currentY - prevY + currentX / slope + slope * prevX) / (slope + 1 / slope);
			y = prevY + slope*(x - prevX);
		}
		return new UTMCoordinates(x, y);
	}
	
	/** Calculates if two UAVs have collision risk.
	 * <p>Requires to be in the normal protocol state.
	 * Returns null if no risk has been detected, or the location of the risky point otherwise.</p> */
	public static Point3D hasCollisionRisk(int numUAV, Beacon selfBeacon, Beacon receivedBeacon) {
		double distance;
		long selfTime, beaconTime;
		Point3D selfPoint, receivedPoint;
		boolean checkTime = receivedBeacon.state == MBCAPState.NORMAL.getId()
				&& receivedBeacon.speed >= MBCAPParam.minSpeed
				&& selfBeacon.speed >= MBCAPParam.minSpeed;
		for (int i = 0; i < selfBeacon.points.size(); i++) {
			selfTime = selfBeacon.time + i * MBCAPParam.hopTimeNS;
			selfPoint = selfBeacon.points.get(i);
			for (int j = 0; j < receivedBeacon.points.size(); j++) {
				// Do not check if the other UAV is in Go on, please and the location is the detected risk location
				if (j == 1
						&& (receivedBeacon.state == MBCAPState.GO_ON_PLEASE.getId() || receivedBeacon.state == MBCAPState.STAND_STILL.getId())) {
					break;
				}
				
				boolean risky = true;
				// Temporal collision risk (only if the other UAV is also in the normal protocol state, and with enough speed)
				if (checkTime) {
					beaconTime = receivedBeacon.time + j * MBCAPParam.hopTimeNS;
					if (Math.abs(selfTime - beaconTime) >= MBCAPParam.collisionRiskTime) {
						risky = false;
					}
				}
				
				// X,Y, Z collision risk
				if (risky) {
					receivedPoint = receivedBeacon.points.get(j);
					distance = selfPoint.distance(receivedPoint);
					if (distance < MBCAPParam.collisionRiskDistance
							&& Math.abs(selfPoint.z - receivedPoint.z) < MBCAPParam.collisionRiskAltitudeDifference) {
						return selfPoint;
					}
				}
			}
		}
		return null;
	}
	
	/** Get the reaction distance given the current speed. It ignores the possible current acceleration. */
	private static double getReactionDistance(double speed) {
		double d = 0;
		if (speed >= MBCAPParam.minSpeed) {
			if (speed < 5) {
				d = 2.76 * speed - 0.42;
			} else if (speed < 7.5) {
				d = 0.58 * speed + 10.48;
			} else {
				d = 1.8 * speed + 1.33;
			}
			d = d + MBCAPParam.gpsError
					+ MBCAPParam.riskCheckPeriod / 1000000000l * speed
					+ (MBCAPParam.packetLossThreshold * MBCAPParam.beaconingPeriod) / 1000.0 * speed;
		}
		return d;
	}
	
	/** Calculates if the current UAV has overtaken another UAV. */
	public static boolean overtakingFinished(int numUAV, long avoidingId, Point3D target) {
		// Overtaken happened if the distance to the target UAV and to the risk location is increasing
		boolean success = MBCAPHelper.isMovingAway(numUAV, target);
		if (!success) {
			return false;
		}
		if (MBCAPParam.impactLocationUTM[numUAV] != null) {
			Point3D riskLocation = MBCAPParam.impactLocationUTM[numUAV].get(avoidingId);
			if (riskLocation != null) {
				success = MBCAPHelper.isMovingAway(numUAV, riskLocation);
				if (!success) {
					return false;
				}
			}
		}
		
		// AND the UAV has moved far enough
		return target.distance(MBCAPParam.selfBeacon.get(numUAV).points.get(0)) > MBCAPParam.collisionRiskDistance;
	}
	
	/** Calculates if the current UAV is moving away from a target point. */
	public static boolean isMovingAway(int numUAV, Point2D.Double target) {
		// true if the distance to the target is increasing
		UTMCoordinates[] lastLocations = Copter.getLastKnownUTMLocations(numUAV);
		if (lastLocations.length <= 1) {
			return false;	// There is no information enough to decide
		}
		double distPrevToTarget, distPostToTarget;
		distPrevToTarget = lastLocations[0].distance(target);
		for (int i = 1; i < lastLocations.length; i++) {
			distPostToTarget = lastLocations[i].distance(target);
			if (distPrevToTarget >= distPostToTarget) {
				return false;
			}
			distPrevToTarget = distPostToTarget;
		}
		return true;
	}
	
	/** Checks if the UAV has to move aside, and it gets that safe point when needed. */
	public static boolean needsToMoveAside(int numUAV, List<Point3D> avoidPredictedLocations, double plannedSpeed) {
		// Errors detection
		if (avoidPredictedLocations == null || avoidPredictedLocations.size() <= 1) {
			GUI.log(numUAV, MBCAPText.REPOSITION_ERROR_1);
			return false;
		}
		int locations = avoidPredictedLocations.size();
		
		
		Point3D[][] segment = new Point3D[locations - 1][2];
		for (int i = 0; i < segment.length; i++) {
			segment[i][0] = avoidPredictedLocations.get(i);
			segment[i][1] = avoidPredictedLocations.get(i+1);
		}

		UTMCoordinates currentUTMLocation = Copter.getUTMLocation(numUAV);
		
		// Calculus of the angle with each segment with the previous and next segment
		double[][] angles;
		if (segment.length == 1) {
			angles = new double[][] {{0, 0}};
		} else {
			angles = new double[segment.length][2];
			Double angle = 0.0;
			for (int i = 0; i < segment.length; i++) {
				if (angle != null) {
					angles[i][0] = angle;
				}
				if (i == segment.length - 1) {
					angle = 0.0;
				} else {
					angle = MBCAPHelper.getAngleDifference(segment[i][0], segment[i][1], segment[i + 1][0], segment[i + 1][1]);
				}
				if (angle != null) {
					angles[i][1] = angle;
				}
			}
		}
		
		// Checking the distance of the UAV to the segments of the mission of the other UAV, and to the vertex of the mission
		UTMCoordinates newLocation = null;
		boolean isInSafePlace = false;
		boolean foundConflict;
		UTMCoordinates auxLocation;
		double waypointThreshold = MBCAPHelper.getWaypointThreshold(plannedSpeed);
		while (!isInSafePlace) {
			foundConflict = false;
			for (int i = 0; i < segment.length; i++) {
				auxLocation = MBCAPHelper.getSegmentSafeLocation(currentUTMLocation, segment[i][0], segment[i][1], angles[i],
						plannedSpeed, waypointThreshold);
				if (auxLocation != null) {
					currentUTMLocation = auxLocation;
					newLocation = auxLocation;
					foundConflict = true;
				}
			}
			if (!foundConflict) {
				// Check if the UAV is close to any vertex (waypoint) of the mission
				Point3D currentWaypoint;
				for (int i = 1; i < locations - 1; i++) {
					currentWaypoint = avoidPredictedLocations.get(i);
					auxLocation = MBCAPHelper.getWaypointSafeLocation(currentUTMLocation, currentWaypoint);
					if (auxLocation != null) {
						currentUTMLocation = auxLocation;
						newLocation = auxLocation;
						foundConflict = true;
					}
				}
				if (!foundConflict) {
					isInSafePlace = true;
				}
			}
		}

		// If new coordinates have been found, it means that the UAV must move to a safer position
		if (newLocation != null) {
			MBCAPParam.targetLocationUTM.set(numUAV, newLocation);
			MBCAPParam.targetLocationPX.set(numUAV, GUI.locatePoint(newLocation.x, newLocation.y));
			return true;
		}
		return false;
	}
	
	private static double getWaypointThreshold(double speed) {
		double[] function = MBCAPParam.FUNCTION_WAYPOINT_THRESHOLD;
		return function[0] + function[1] * speed + function[2] * speed * speed;
	}
	
	/** Get the angle between two lines.
	 * <p>Returns null if any error happens, or the angle [-Math.PI, Math.PI] otherwise (positive if the second line turns left).</p> */
	private static Double getAngleDifference(Point2D.Double l0Start, Point2D.Double l0End,
			Point2D.Double l1Start, Point2D.Double l1End) {
		Double l0Angle = MBCAPHelper.getAngle(l0Start, l0End);
		Double l1Angle = MBCAPHelper.getAngle(l1Start, l1End);
		if (l0Angle == null || l1Angle == null) {
			return null;
		}
		double res = l1Angle - l0Angle;
		if (res < -Math.PI) {
			res = Math.PI * 2 + res;
		}
		if (res > Math.PI) {
			res = -Math.PI * 2 + res;
		}
		return res;
	}

	/** Get the angle between a line, and the line that goes from left to right (X axis, positive direction). */
	private static Double getAngle(Point2D.Double Start, Point2D.Double End) {
		double angle;
		double incX = End.x - Start.x;
		double incY = End.y - Start.y;
		if (incX == 0) {
			if (incY > 0) {
				angle = Math.PI / 2;
			} else if (incY < 0) {
				angle = -Math.PI / 2;
			} else {
				return null;
			}
		} else {
			angle = Math.atan(incY / incX);
			if (incX < 0) {
				if (angle > 0) {
					angle = angle - Math.PI;
				} else {
					angle = angle + Math.PI;
					// if the UAV is moving in opposite direction, the angle will be: Math.PI
				}
			}
		}
		return angle;
	}
	
	/** Calculates the safe place to move aside from a path segment.
	 * <p>Returns null if there is no need of moving aside.</p> */
	private static UTMCoordinates getSegmentSafeLocation(UTMCoordinates currentLocation,
			Point3D prev, Point3D post, double[] angles, double plannedSpeed, double waypointThreshold) {
		double currentX = currentLocation.x;
		double currentY = currentLocation.y;
		double prevX = prev.x;
		double prevY = prev.y;
		double postX = post.x;
		double postY = post.y;
		double x, y;
		
		UTMCoordinates intersection = MBCAPHelper.getIntersection(currentLocation, prev, post);
		
		// currentLocation out of the segment case
		double incX = postX - prevX;
		double incY = postY - prevY;
		if (incX == 0) {
			// Vertical line case
			if (currentY < Math.min(prevY, postY)
					|| currentY > Math.max(prevY, postY)) {
				return null;
			}
		} else if (incY == 0) {
			// Horizontal line case
			if (currentX < Math.min(prevX, postX)
					|| currentX > Math.max(prevX, postX)) {
				return null;
			}
		} else {
			// General case
			if (intersection.x < Math.min(prevX, postX)
					|| intersection.x > Math.max(prevX, postX)) {
				return null;
			}
		}
		
		// Maybe it is close to one of the waypoints
		boolean goToTheOtherSide = false;
		double maxDistance = 0;
		Double currentAngle = MBCAPHelper.getAngleDifference(prev, post, prev, currentLocation);
		double currentDistance = currentLocation.distance(intersection);
		if (currentAngle != null) {
			double dPrev = intersection.distance(prev);
			double dPost = intersection.distance(post);

			boolean isPrevCloser = dPrev <= dPost;
			// Analyze the waypoint the UAV is closer to, and if the distance is adequate
			if (isPrevCloser && dPrev < waypointThreshold) {
				// UAV in the inner side of the waypoint
				if ((currentAngle > 0 && angles[0] > 0 && angles[0] <= Math.PI / 2)
						|| (currentAngle < 0 && angles[0] < 0 && angles[0] >= -Math.PI / 2)) {
					// Analyze if the distance to the segment is enough
					maxDistance = MBCAPHelper.getCurveDistance(plannedSpeed, angles[0]);
					if (currentDistance < maxDistance + MBCAPParam.safePlaceDistance) {
						goToTheOtherSide = true;
					}
				}
			}
			if (!isPrevCloser && dPost < waypointThreshold) {
				// UAV in the inner side of the waypoint
				if ((currentAngle > 0 && angles[1] > 0 && angles[1] <= Math.PI / 2)
						|| (currentAngle < 0 && angles[1] < 0 && angles[1] >= -Math.PI / 2)) {
					// Analyze if the distance to the segment is enough
					maxDistance = MBCAPHelper.getCurveDistance(plannedSpeed, angles[1]);
					if (currentDistance < maxDistance + MBCAPParam.safePlaceDistance) {
						goToTheOtherSide = true;
					}
				}
			}
		}
		
		// Far enough case
		if (!goToTheOtherSide && currentDistance > MBCAPParam.safePlaceDistance) {
			return null;
		}
		
		// Has to move apart case
		if (incX == 0) {
			// Vertical line case
			if (currentX < prevX) {
				if (goToTheOtherSide) {
					x = prevX + MBCAPParam.safePlaceDistance + 0.1;	// We add a little margin due to double precision errors
				} else {
					x = prevX - MBCAPParam.safePlaceDistance - 0.1;
				}
			} else {
				if (goToTheOtherSide) {
					x = prevX - MBCAPParam.safePlaceDistance - 0.1;
				} else {
					x = prevX + MBCAPParam.safePlaceDistance + 0.1;
				}
			}
			y = currentY;
		} else if (incY == 0) {
			// Horizontal line case
			if (currentY < prevY) {
				if (goToTheOtherSide) {
					y = prevY + MBCAPParam.safePlaceDistance + 0.1;
				} else {
					y = prevY - MBCAPParam.safePlaceDistance - 0.1;
				}
			} else {
				if (goToTheOtherSide) {
					y = prevY - MBCAPParam.safePlaceDistance - 0.1;
				} else {
					y = prevY + MBCAPParam.safePlaceDistance + 0.1;
				}
			}
			x = currentX;
		} else {
			// General case
			double ds = MBCAPParam.safePlaceDistance + 0.1;
			double d12 = prev.distance(post);
			double incXS = ds / d12 * Math.abs(incY);
			if (currentX <= intersection.x) {
				if (goToTheOtherSide) {
					x = intersection.x + incXS;
				} else {
					x = intersection.x - incXS;
				}
			} else {
				if (goToTheOtherSide) {
					x = intersection.x - incXS;
				} else {
					x = intersection.x + incXS;
				}
			}
			y = currentY - incX / incY * (x - currentX);
		}
		UTMCoordinates res = new UTMCoordinates(x, y);
		
		// Returns the safe place in UTM coordinates
		return res;
	}
	
	/** Get the maximum distance a UAV moves aside a waypoint performing a mission, depending on the flight speed and the angle between the two segments of the mission.
	 * <p> We get the values from experimental equations for the previous and next values available, and then we interpolate for the given speed and angle. */
	private static double getCurveDistance(double speed, double angle) {
		
		double[][] fSpeed = MBCAPParam.FUNCTION_DISTANCE_VS_SPEED;
		double[][] fAlpha = MBCAPParam.FUNCTION_DISTANCE_VS_ALPHA;
		double alpha = Math.abs(angle);
		
		double[] dS = new double[2];
		
		// We already know that the first equation (i == 0) is for the angle 0 radians, so the previous value starts in 0
		int prevSpeed = 0;
		int postSpeed;
		for (int i = 1; i < fSpeed.length; i++) {
			if (fSpeed[i][0] < alpha) {
				prevSpeed = i;
			}
		}
		double inc;
		if (prevSpeed == fSpeed.length - 1) {
			postSpeed = prevSpeed;
			inc = 0;
		} else {
			postSpeed = prevSpeed + 1;
			inc = (alpha - fSpeed[prevSpeed][0]) / (fSpeed[postSpeed][0] -fSpeed[prevSpeed][0]);
		}
		dS[0] = fSpeed[prevSpeed][1] + fSpeed[prevSpeed][2] * speed + fSpeed[prevSpeed][3] * speed * speed;
		dS[1] = fSpeed[postSpeed][1] + fSpeed[postSpeed][2] * speed + fSpeed[postSpeed][3] * speed * speed;
		
		double dSpeed = dS[0] + (dS[1] - dS[0]) * inc;
		
		double[] dA = new double[2];
		int prevAngle = 0;
		int postAngle;
		for (int i = 1; i < fAlpha.length; i++) {
			if (fAlpha[i][0] < speed) {
				prevAngle = i;
			}
		}
		
		if (prevAngle == fAlpha.length - 1) {
			postAngle = prevAngle;
			inc = 0;
		} else {
			postAngle = prevAngle + 1;
			inc = (speed - fAlpha[prevAngle][0]) / (fAlpha[postAngle][0] -fAlpha[prevAngle][0]);
		}
		dA[0] = fAlpha[prevAngle][1] + fAlpha[prevAngle][2] * alpha + fAlpha[prevAngle][3] * alpha * alpha;
		dA[1] = fAlpha[postAngle][1] + fAlpha[postAngle][2] * alpha + fAlpha[postAngle][3] * alpha * alpha;
		
		double dAngle = dA[0] + (dA[1] - dA[0]) * inc;
		
		return Math.max(dSpeed, dAngle);
	}
	
	/** Calculates the safe place to move aside from a waypoint.
	 * <p>Returns null if there is no need of moving aside.</p> */
	private static UTMCoordinates getWaypointSafeLocation(UTMCoordinates currentUTMLocation, Point3D currentWaypoint) {
		double currentDistance = currentUTMLocation.distance(currentWaypoint);
		if (currentDistance > MBCAPParam.safePlaceDistance) {
			return null;
		}
		double incX, incY;
		incX = currentUTMLocation.x - currentWaypoint.x;
		incY = currentUTMLocation.y - currentWaypoint.y;
		double x, y;
		if (incX == 0) {
			// Vertical line case
			if (currentUTMLocation.y < currentWaypoint.y) {
				y = currentWaypoint.y - MBCAPParam.safePlaceDistance - 0.1;	// We add a little margin due to double precision errors;
			} else {
				y = currentWaypoint.y + MBCAPParam.safePlaceDistance + 0.1;
			}
			x = currentUTMLocation.x;
		} else if (incY == 0) {
			// Horizontal line case
			if (currentUTMLocation.x < currentWaypoint.x) {
				x = currentWaypoint.x - MBCAPParam.safePlaceDistance - 0.1;
			} else {
				x = currentWaypoint.x + MBCAPParam.safePlaceDistance + 0.1;
			}
			y = currentUTMLocation.y;
		} else {
			// General case
			double ds = MBCAPParam.safePlaceDistance + 0.1;
			double incXS = ds / currentDistance * Math.abs(incX);
			if (currentUTMLocation.x < currentWaypoint.x) {
				x = currentWaypoint.x - incXS;
			} else {
				x = currentWaypoint.x + incXS;
			}
			y = currentWaypoint.y + incY / incX * (x - currentWaypoint.x);
		}
		return new UTMCoordinates(x, y);
	}

}
