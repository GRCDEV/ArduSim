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
import mbcap.gui.MBCAPPCCompanionDialog;
import mbcap.gui.MBCAPConfigDialog;
import mbcap.gui.MBCAPGUIParam;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.pojo.Beacon;
import mbcap.pojo.PointTime;
import mbcap.pojo.ProgressState;
import sim.board.BoardPanel;
import uavController.UAVParam;

public class MBCAPv3Helper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V3;
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
		MBCAPParam.state = new MBCAPState[numUAVs];
		MBCAPParam.idAvoiding = new AtomicLongArray(numUAVs);
		MBCAPParam.projectPath = new AtomicIntegerArray(numUAVs);

		MBCAPParam.selfBeacon = new AtomicReferenceArray<Beacon>(numUAVs);
		MBCAPParam.beacons = new ConcurrentHashMap[numUAVs];
		MBCAPParam.impactLocationUTM = new ConcurrentHashMap[numUAVs];
		MBCAPParam.impactLocationPX = new ConcurrentHashMap[numUAVs];

		MBCAPParam.targetPointUTM = new Point2D.Double[numUAVs];
		MBCAPParam.targetPointGeo = new GeoCoordinates[numUAVs];
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
		
		MBCAPParam.uavDeadlockTimeout = new long[numUAVs];
		
		double maxBeaconDistance, speed;
		for (int i=0; i < numUAVs; i++) {
			speed = Copter.getPlannedSpeed(i);
			maxBeaconDistance = speed * MBCAPParam.beaconFlyingTime;
			if (MBCAPParam.reactionDistance < maxBeaconDistance) {
				maxBeaconDistance = MBCAPParam.reactionDistance;
			}
			long aux = MBCAPParam.DEADLOCK_TIMEOUT_BASE + Math.round((maxBeaconDistance * 2 / speed) * 1000000000l);
			MBCAPParam.uavDeadlockTimeout[i] = Math.max(MBCAPParam.globalDeadlockTimeout, aux);
		}
	}

	@Override
	public String setInitialState() {
		return MBCAPState.NORMAL.getName();
	}

	@Override
	public void rescaleDataStructures() {
		// Rescale the safety circles diameter
		Point2D.Double locationUTM = null;
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
		URL url = MBCAPv3Helper.class.getResource(MBCAPGUIParam.EXCLAMATION_IMAGE_PATH);
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
		//Calculates the screen position of the points where the collision risk was detected
		Iterator<Map.Entry<Long, Point3D>> entries;
		Map.Entry<Long, Point3D> entry;
		Point3D riskLocationUTM;
		Point2D.Double riskLocationPX;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			entries = MBCAPParam.impactLocationUTM[i].entrySet().iterator();
			MBCAPParam.impactLocationPX[i].clear();
			while (entries.hasNext()) {
				entry = entries.next();
				riskLocationUTM = entry.getValue();
				riskLocationPX = GUI.locatePoint(riskLocationUTM.x, riskLocationUTM.y);
				MBCAPParam.impactLocationPX[i].put(entry.getKey(), riskLocationPX);
			}
		}
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		if (!Tools.isCollisionDetected()) {
			g2.setStroke(MBCAPParam.STROKE_POINT);
			MBCAPGUITools.drawPredictedLocations(g2);
			MBCAPGUITools.drawImpactRiskMarks(g2, p);
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
					incX = p2UTM.Easting - p1UTM.Easting;
					incY = p2UTM.Northing - p1UTM.Northing;
					if (incX != 0 || incY != 0) {
						if (incX == 0) {
							if (incY > 0)	heading = 0.0;
							else			heading = 180.0;
						} else if (incY == 0) {
							if (incX > 0)	heading = 89.9;	// SITL fails changing the flight mode if heading is close to 90 degrees
							else			heading = 270.0;
						} else {
							double gamma = Math.atan(incY/incX);
							if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
							else 			heading = 270.0 - gamma * 180 / Math.PI;
						}
						// Same correction due to the SITL error
						if (heading < 90 && heading > 89.9) {
							heading = 89.9;
						}
						if (heading > 90 && heading < 90.1) {
							heading = 90.1;
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
		return Copter.cleanAndSendMissionToUAV(numUAV, Tools.getLoadedMissions()[numUAV]);
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
		}
		return sb.toString();
	}

	@Override
	public String getExperimentConfiguration() {
		StringBuilder sb = new StringBuilder(2000);
		sb.append(MBCAPText.BEACONING_PARAM);
		sb.append("\n\t").append(MBCAPText.BEACON_INTERVAL).append(" ").append(MBCAPParam.beaconingPeriod).append(" ").append(MBCAPText.MILLISECONDS);
		sb.append("\n\t").append(MBCAPText.BEACON_REFRESH).append(" ").append(MBCAPParam.numBeacons);
		sb.append("\n\t").append(MBCAPText.BEACON_EXPIRATION).append(" ").append(String.format( "%.2f", MBCAPParam.beaconExpirationTime*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.TIME_WINDOW).append(" ").append(MBCAPParam.beaconFlyingTime).append(" ").append(MBCAPText.MILLISECONDS);
		sb.append("\n\t").append(MBCAPText.INTERSAMPLE).append(" ").append(MBCAPParam.hopTime).append(" ").append(MBCAPText.MILLISECONDS);
		sb.append("\n\t").append(MBCAPText.MIN_ADV_SPEED).append(" ").append(MBCAPParam.minSpeed).append(" ").append(MBCAPText.METERS_PER_SECOND);
		sb.append("\n").append(MBCAPText.AVOID_PARAM);
		sb.append("\n\t").append(MBCAPText.WARN_DISTANCE).append(" ").append(MBCAPParam.collisionRiskDistance).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.WARN_ALTITUDE).append(" ").append(MBCAPParam.collisionRiskAltitudeDifference).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.WARN_TIME).append(" ").append(String.format( "%.2f", MBCAPParam.collisionRiskTime*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.CHECK_THRESHOLD).append(" ").append(MBCAPParam.reactionDistance).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.CHECK_PERIOD).append(" ").append(String.format( "%.2f", MBCAPParam.riskCheckPeriod*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.SAFE_DISTANCE).append(" ").append(MBCAPParam.safePlaceDistance).append(" ").append(MBCAPText.METERS);
		sb.append("\n\t").append(MBCAPText.HOVERING_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.standStillTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.OVERTAKE_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.passingTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.RESUME_MODE_DELAY).append(" ").append(String.format( "%.2f", MBCAPParam.solvedTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		sb.append("\n\t").append(MBCAPText.DEADLOCK_TIMEOUT).append(" ").append(String.format( "%.2f", MBCAPParam.globalDeadlockTimeout*0.000000001 )).append(" ").append(MBCAPText.SECONDS);
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void logData(String folder, String baseFileName) {
		// Logging to file the error predicting the location during the experiment (and the beacons itself if needed).
		int numUAVs = Tools.getNumUAVs();
		List<PointTime>[] realUAVPaths = new ArrayList[numUAVs];

		// 1. UAV path calculus (only experiment path, and ignoring repeated positions)
		LogPoint realPostLocation, realPrevLocation;
		Double x, y;
		for (int i=0; i<numUAVs; i++) {
			int j = 0;
			realPrevLocation = null;
			y = x = null;
			realUAVPaths[i] = new ArrayList<PointTime>();

			List<LogPoint> fullPath = Tools.getUTMPath(i);
			while (j<fullPath.size()) {
				realPostLocation = fullPath.get(j);

				// Considers only not repeated locations and only generated during the experiment
				if (fullPath.get(j).inTest) {
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
						realUAVPaths[i].add(new PointTime(fullPath.get(j).time, x, y));
					}
				}
				j++;
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
		PointTime predictedLocation;
		List<PointTime>[][] totalPredictedLocations = new ArrayList[numUAVs][];
		boolean verboseStore = Tools.isVerboseStorageEnabled();
		for (int i=0; i<numUAVs; i++) {
			totalPredictedLocations[i] = new ArrayList[MBCAPParam.beaconsStored[i].size()];
			
			if (verboseStore) {
				// Store each beacon also
				beaconsFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.BEACONS_SUFIX);
				sb1 = new StringBuilder(2000);
				sb1.append("time,x1,y2,x2,y2,...,xn,yn\n");
			}
			List<Beacon> beacons = MBCAPParam.beaconsStored[i];
			// j beacons
			Beacon beacon;
			for (j=0; j<beacons.size(); j++) {
				beacon = beacons.get(j);
				if (beacon.points!=null && beacon.points.size()>0) {
					if (verboseStore) {
						sb1.append(beacon.time);
					}
					List<Point3D> locations = beacon.points;
					totalPredictedLocations[i][j] = new ArrayList<PointTime>();
					for (int k=0; k<locations.size(); k++) {
						if (verboseStore) {
							sb1.append(",").append(Tools.round(locations.get(k).x, 3))
								.append(",").append(Tools.round(locations.get(k).y, 3));
						}
						predictedLocation = new PointTime(beacon.time + MBCAPParam.hopTimeNS*k, locations.get(k).x, locations.get(k).y);
						// Predicted positions for later calculus of the error in prediction
						totalPredictedLocations[i][j].add(predictedLocation);
					}
					if (verboseStore) {
						sb1.append("\n");
					}
				}
			}
			if (verboseStore) {
				Tools.storeFile(beaconsFile, sb1.toString());
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
			// Only store information if useful beacons were found
			if (numBeacons > 0) {
				double[] maxBeaconDistance = new double[numBeacons]; // One per beacon
				double[] meanBeaconDistance = new double[numBeacons];
				if (Tools.isVerboseStorageEnabled()) {
					// Log the line from the real to the predicted location, with maximum error
					maxErrorFile = new File(folder + File.separator + baseFileName + "_" + Tools.getIdFromPos(i) + "_" + MBCAPText.MAX_ERROR_LINES_SUFIX);
					sb2 = new StringBuilder(2000);
				}
				int pos = 0;
				for (j=0; j<predictedLocations.length; j++) {
					if (predictedLocations[j] != null) {
						Pair<Point2D.Double, Point2D.Double> pair =
								beaconErrorCalculation(realUAVPath, predictedLocations, j,
										maxBeaconDistance, pos, meanBeaconDistance);
						pos++;
						if (pair!=null && Tools.isVerboseStorageEnabled()) {
							sb2.append("._LINE\n");
							sb2.append(Tools.round(pair.getValue0().x, 3)).append(",")
								.append(Tools.round(pair.getValue0().y, 3)).append("\n");
							sb2.append(Tools.round(pair.getValue1().x, 3)).append(",")
								.append(Tools.round(pair.getValue1().y, 3)).append("\n\n");
						}
					}
				}
				if (Tools.isVerboseStorageEnabled()) {
					Tools.storeFile(maxErrorFile, sb2.toString());
				}
				
				// 4. Storage of the mean and maximum error on each beacon
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
				
				// 5. Calculus and storage of the mean and maximum error on each position of each beacon
				int size = 0;
				for (int m = 0; m<predictedLocations.length; m++) {
					if (predictedLocations[m].size()>size) {
						size = predictedLocations[m].size();
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
				GUI.log(Copter.getUAVPrefix(numUAV) + MBCAPText.WAYPOINT_LOST);
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
			Pair<Point2D.Double, Integer> currentLocation = MBCAPv3Helper.getCurrentLocation(numUAV, mission, currentUTMLocation, posNextWaypoint);
			currentUTMLocation = currentLocation.getValue0();
			posNextWaypoint = currentLocation.getValue1();
			predictedPath.add(new Point3D(currentUTMLocation.x, currentUTMLocation.y, currentZ));
			// Calculate the rest of the points depending on the protocol version
			if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1)
					|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
					|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3)) {
				MBCAPv3Helper.getPredictedLocationsV1(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
			} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V4)) {
				MBCAPv3Helper.getPredictedLocationsV2(numUAV, speed, acceleration, currentUTMLocation, mission, posNextWaypoint, currentWaypoint, currentZ, predictedPath);
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
				|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3)
				|| ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V4)) {
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
					Point2D.Double prevIntersection = MBCAPv3Helper.getIntersection(currentUTMLocation, location1, location2);
					Point2D.Double postIntersection = MBCAPv3Helper.getIntersection(currentUTMLocation, location2, location3);
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
	private static void getPredictedLocationsV1(int numUAV, double speed, double acceleration,
			Point2D.Double currentUTMLocation, List<WaypointSimplified> mission, int posNextWaypoint, int currentWaypoint, double currentZ,
			List<Point3D> predictedPath) {
		// 1. Calculus of the segments over which we have to obtain the points
		List<Double> distances = new ArrayList<Double>(MBCAPParam.DISTANCES_SIZE);
		double totalDistance = 0.0;
		// MBCAP v1 or MBCAP v2 (no acceleration present)
		if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1) || ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
				|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3) && acceleration == 0.0)) {
			totalDistance = MBCAPParam.beaconFlyingTime * speed;
		} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3)) {
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

		if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V1) || ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V2)
				|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3) && acceleration == 0.0)) {
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
					|| (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3) && acceleration == 0)) {
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
			} else if (ProtocolHelper.selectedProtocol.equals(MBCAPText.MBCAP_V3)) {
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
	private static void getPredictedLocationsV2(int numUAV, double speed, double acceleration,
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
	
	/** Calculates if two UAVs have collision risk.
	 * <p>Requires to be in the normal protocol state. */
	public static boolean hasCollisionRisk(int numUAV, Beacon selfBeacon, Beacon receivedBeacon) {
		double distance;
		long selfTime, beaconTime;
		int maxPoints = (int)Math.ceil(MBCAPParam.reactionDistance / (selfBeacon.speed * MBCAPParam.hopTime)) + 1;
		Point3D selfPoint, receivedPoint;
		for (int i = 0; i < selfBeacon.points.size() && i < maxPoints; i++) {
			selfTime = selfBeacon.time + i * MBCAPParam.hopTimeNS;
			selfPoint = selfBeacon.points.get(i);
			for (int j = 0; j < receivedBeacon.points.size(); j++) {
				// Do not check if the other UAV is in Go on, please and the location is the detected risk location
				if (receivedBeacon.state == MBCAPState.GO_ON_PLEASE.getId() && j == 1) {
					continue;
				}
				
				boolean risky = true;
				// Temporal collision risk (only if the other UAV is also in the normal protocol state)
				if (receivedBeacon.state == MBCAPState.NORMAL.getId()) {
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
						MBCAPParam.impactLocationUTM[numUAV].put(receivedBeacon.uavId, selfPoint);
						MBCAPGUITools.locateImpactRiskMark(selfPoint, numUAV, receivedBeacon.uavId);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/** Calculates if the current UAV has overtaken another UAV. */
	public static boolean OvertakingFinished(int numUAV, long avoidingId, Point2D.Double target) {
		// Overtaken happened if the distance to the target UAV and to the risk location is increasing
		Point3D riskLocation = MBCAPParam.impactLocationUTM[numUAV].get(avoidingId);
		Point3D[] lastLocations = Copter.getLastKnownLocations(numUAV);
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
	
	/** Checks if the UAV has to move aside, and it gets that safe point when needed. */
	public static boolean needsToMoveAside(int numUAV, List<Point3D> avoidPredictedLocations) {
		// Errors detection
		if (avoidPredictedLocations == null || avoidPredictedLocations.size() <= 1) {
			GUI.log(Copter.getUAVPrefix(numUAV) + MBCAPText.REPOSITION_ERROR_1);
			return false;
		}
		if (!Tools.isUTMZoneSet()) {
			GUI.log(Copter.getUAVPrefix(numUAV) + MBCAPText.REPOSITION_ERROR_2);
			return false;
		}

		Point2D.Double currentUTMLocation = Copter.getUTMLocation(numUAV);

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
				auxLocation = MBCAPv3Helper.getSafeLocation(currentUTMLocation, prevLocation, postLocation);
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
			MBCAPParam.targetPointGeo[numUAV] = Tools.UTMToGeo(newLocation.x, newLocation.y);	// To order the movement
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

}
