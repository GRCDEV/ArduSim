package noneTest;


import static noneTest.Nonev2State.*;


import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;


import java.util.List;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.esotericsoftware.minlog.Log;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.AtomicDoubleArray;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Text;
import mbcap.logic.MBCAPv3Helper;

import sim.board.BoardPanel;
import sim.logic.SimParam;
import uavController.UAVParam;

/** Implementation of the protocol NONE to allow the user to simply follow missions. */

public class Nonev2Helper extends ProtocolHelper {
	
	private MBCAPv3Helper copy;
	private boolean isMaster;
	public Nonev2Helper() {
		//this.copy = new MBCAPv3Helper();
	}

	@Override
	public void setProtocol() {
		this.protocolString = "Nonev2";
	}

	@Override
	public boolean loadMission() {
		//return true;
		//this.isMaster = ScanHelper.isMaster(numUAV);
		// Only the master UAV has a mission (on real UAV, numUAV == 0)
		this.isMaster = Nonev2Helper.isMaster(0);
		
		GUI.log("LoadMission idMaster=" + this.isMaster);
		
		if (this.isMaster) {
			Nonev2Param.idMaster = Tools.getIdFromPos(Nonev2Param.MASTER_POSITION);
		}//	The slave in a real UAV has SwarmProtParam.idMaster == null
		
		return isMaster;
				
		
	}

	@Override
	public void openConfigurationDialog() {
		Nonev2Param.idMaster = Nonev2Param.SIMULATION_MASTER_ID;
		
		GUI.log("openConfigurationDialog idMaster=" + Nonev2Param.idMaster);
		
		new Nonev2ConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		
		int numUAVs = Tools.getNumUAVs();
		
		Nonev2Param.idPrev = new AtomicLongArray(numUAVs);
		Nonev2Param.idNext = new AtomicLongArray(numUAVs);
		Nonev2Param.takeoffAltitude = new AtomicDoubleArray(numUAVs);
		Nonev2Param.data = new AtomicReferenceArray<>(numUAVs);
		Nonev2Param.uavMissionReceivedUTM = new AtomicReferenceArray<>(numUAVs);
		Nonev2Param.uavMissionReceivedGeo = new AtomicReferenceArray<>(numUAVs);
		
		Nonev2Param.state = new AtomicIntegerArray(numUAVs);
		Nonev2Param.moveSemaphore = new AtomicIntegerArray(numUAVs);
		Nonev2Param.wpReachedSemaphore = new AtomicIntegerArray(numUAVs);
		
		GUI.log("State tinen:"+ Nonev2Param.state);
		
		//System.out.println("State tinen:"+ Nonev2Param.state);
	}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		//return this.copy.setStartingLocation();
		
		/** Position the master on the map */
		Nonev2Param.masterHeading = 0.0;
		
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		List<Waypoint>[] missions = Tools.getLoadedMissions();
		if (missions != null && missions[Nonev2Param.MASTER_POSITION] != null) {
			waypointFound = false;
			for (int j = 0; j < missions[Nonev2Param.MASTER_POSITION].size() && !waypointFound; j++) {
				waypoint1 = missions[Nonev2Param.MASTER_POSITION].get(j);
				if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				GUI.exit(Nonev2Text.UAVS_START_ERROR_2 + " " + Tools.getIdFromPos(Nonev2Param.MASTER_POSITION));
			}
			waypointFound = false;
			for (int j = waypoint1pos + 1; j < missions[Nonev2Param.MASTER_POSITION].size()
					&& !waypointFound; j++) {
				waypoint2 = missions[Nonev2Param.MASTER_POSITION].get(j);
				if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				Nonev2Param.masterHeading = Nonev2Helper.getMasterHeading(waypoint1, waypoint2);
			}
		} else {
			JOptionPane.showMessageDialog(null, Nonev2Text.UAVS_START_ERROR_1 + " " + (Nonev2Param.MASTER_POSITION + 1) + ".",
					Nonev2Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		int numUAVs = Tools.getNumUAVs();
		GeoCoordinates[] startingLocations = posSlaveFromMasterGeo(Nonev2Param.masterHeading, waypoint1, numUAVs - 1,
				Nonev2Param.initialDistanceBetweenUAV);
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocationsFinal = new Pair[numUAVs];
		int aux = 0;
		int destAux = 0;
		while (aux < Nonev2Param.MASTER_POSITION) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], Nonev2Param.masterHeading);
			aux++;
			destAux++;
		}

		startingLocationsFinal[destAux] = Pair
				.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), Nonev2Param.masterHeading);
		destAux++;
		while (aux < startingLocations.length) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], Nonev2Param.masterHeading);
			aux++;
			destAux++;
		}

		return startingLocationsFinal;
	}

	/** Calculates the master heading on simulation. */
	private static double getMasterHeading(Waypoint wp1, Waypoint wp2) {
		// We only can set a heading if at least two points with valid coordinates are found
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		double heading = 0.0;
		p1UTM = Tools.geoToUTM(wp1.getLatitude(), wp1.getLongitude());
		p2UTM = Tools.geoToUTM(wp2.getLatitude(), wp2.getLongitude());
		incX = p2UTM.x - p1UTM.x;
		incY = p2UTM.y - p1UTM.y;
		if (incX != 0 || incY != 0) {
			if (incX == 0) {
				if (incY > 0)
					heading = 0.0;
				else
					heading = 180.0;
			} else if (incY == 0) {
				if (incX > 0)
					heading = 89.9;
				else
					heading = 270.0;
			} else {
				double gamma = Math.atan(incY / incX);
				if (incX > 0)
					heading = 90 - gamma * 180 / Math.PI;
				else
					heading = 270.0 - gamma * 180 / Math.PI;
			}
		}
		return heading;
	}

	/** Returns the locations of slave UAVs given the master location, in geographic coordinates, for simulation. */
	private static GeoCoordinates[] posSlaveFromMasterGeo(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] aux = posSlaveFromMasterUTM(heading, waypoint1, numOfUAVs, initialDistanceBetweenUAV);
		GeoCoordinates[] result = new GeoCoordinates[aux.length];
		for (int i = 0; i < aux.length; i++) {
			result[i] = Tools.UTMToGeo(aux[i].x, aux[i].y);
		}
		return result;
	}

	/** Returns the locations of slave UAVs given the master location, in UTM coordinates, for simulation. */
	private static Point2D.Double[] posSlaveFromMasterUTM(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		UTMCoordinates initialPoint = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		return posSlaveFromMasterLinear(heading, new Point2D.Double(initialPoint.x, initialPoint.y),
				numOfUAVs, initialDistanceBetweenUAV);

	}

	/** Locates the slaves on the map according to master position, forming a straight line perpendicular to the master heading. */
	protected static Point2D.Double[] posSlaveFromMasterLinear(double heading, Point2D.Double initialPoint, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] startingLocations = new Point2D.Double[numOfUAVs];
		Point2D.Double destPointP = new Point2D.Double(initialPoint.x, initialPoint.y);
		Point2D.Double destPointI = new Point2D.Double(initialPoint.x, initialPoint.y);

		for (int i = 0; i < numOfUAVs; i++) {
			if (i % 2 == 0) {
				destPointP.x = destPointP.x
						+ initialDistanceBetweenUAV * Math.cos((heading * Math.PI) / 180)
						+ 0 * Math.sin((heading * Math.PI) / 180);
				destPointP.y = destPointP.y + 0 * Math.cos((heading * Math.PI) / 180)
				- initialDistanceBetweenUAV * Math.sin((heading * Math.PI) / 180);
				startingLocations[i] = new Point2D.Double(destPointP.x, destPointP.y);

			} else {
				destPointI.x = destPointI.x
						- (initialDistanceBetweenUAV * Math.cos((heading * Math.PI) / 180)
								+ 0 * Math.sin((heading * Math.PI) / 180));
				destPointI.y = destPointI.y - (0 * Math.cos((heading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((heading * Math.PI) / 180));
				startingLocations[i] = new Point2D.Double(destPointI.x, destPointI.y);
			}

		}
		return startingLocations;
	}
	
	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		//return this.copy.sendInitialConfiguration(numUAV);
		
		//Protocolv2NoneHelper.isMaster()
		
		GUI.log("SendInitialC numUAV = " +  numUAV  + "La variable es" + Nonev2Helper.isMaster(numUAV));
			
		if (Nonev2Helper.isMaster(numUAV)) {
			return Copter.cleanAndSendMissionToUAV(numUAV, Tools.getLoadedMissions()[numUAV]);
		} else {
			return true;
		}
	}

	@Override
	public void startThreads() {}

	@Override
	public void setupActionPerformed() {
		GUI.log("Presiono SetupActionPerformed");
	}

	@Override
	public void startExperimentActionPerformed() {
		//this.copy.startExperimentActionPerformed();
		//GUI.log("Entre por aqui start:");
		
		int c=0;
		
		
		int numUAVs = Tools.getNumUAVs();
		GUI.log("NUMERO DE UAV: " + numUAVs);
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(Nonev2Param.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		List<WaypointSimplified> screenMission = Tools.getUAVMissionSimplified(numUAVs-1);
		Point3D[] centerMission = new Point3D[screenMission.size()];
		uav2DPosition[] currentLocations = null;
		
		GUI.log("valor del isMaster : " + this.isMaster);
		
		//Nonev2Helper.isMaster(numUAV)
		//if (this.isMaster)
		if (Nonev2Helper.isMaster(0)) 
		{
			GUI.log("Entre por el SI MASTER");
			
			//GUI.log(numUAV, ScanText.SETUP);
			//GUI.log(numUAV, ScanText.DETECTED + UAVsDetected.size() + " UAVs");
			
			// 1. Calculate the take-off altitude
			double takeoffAltitude = screenMission.get(0).z;
			double takeOffAltitudeStepOne;
			if (takeoffAltitude <= 5.0) {
				takeOffAltitudeStepOne = 2.0;
			} else if (takeoffAltitude >= 10.0) {
				takeOffAltitudeStepOne = 5.0;
			} else {
				takeOffAltitudeStepOne = takeoffAltitude / 2;
			}
			
			
			Triplet<Long, Integer, uav2DPosition[]> result = Nonev2Helper.getBestFormation(currentLocations, Nonev2Param.masterHeading);
			Nonev2Param.masterPosition = result.getValue1();	// Position of master in the reorganized vector
			long centerId = result.getValue0();
			uav2DPosition[] takeoffLocations = result.getValue2();

			GUI.log(numUAVs, "Center UAV id: " + centerId + ", master UAV position in the take off locations array: " + Nonev2Param.masterPosition);

			if (screenMission.size() > Nonev2Param.MAX_WAYPOINTS) {
				GUI.exit(Nonev2Text.MAX_WP_REACHED);
			}
			// 3. Calculus of the missions to be sent to the slaves and storage of the mission to be followed by the master
			// 3.1. Calculus of the mission to be followed by the UAV in the center of the formation
			//Point3D[] centerMission = new Point3D[screenMission.size()];
			//	First, find the center UAV in the formation
			boolean found = false;
			int centerPosition = 0;
			for (int r = 0; r < takeoffLocations.length && !found; r++) {
				if (takeoffLocations[r].id == centerId) {
					centerPosition = r;
					found = true;
				}
			}
			// Then, calculus of the mission of the central drone
			centerMission[0] = new Point3D(takeoffLocations[centerPosition].x, takeoffLocations[centerPosition].y, takeoffAltitude);
			
			for (int i = 1; i < screenMission.size(); i++) 
			{
				WaypointSimplified wp = screenMission.get(i);
				centerMission[i] = new Point3D(wp.x, wp.y, wp.z);
			}
			
			//Point3D[] mission = new Point3D[screenMission.size()];
			GeoCoordinates[] missionGeo = new GeoCoordinates[screenMission.size()];
			
			for (int i = 0; i < centerMission.length; i++) {
				missionGeo[i] = Tools.UTMToGeo(centerMission[i].x, centerMission[i].y);
			}
			
			Nonev2Param.uavMissionReceivedUTM.set(numUAVs-1, centerMission);
			Nonev2Param.uavMissionReceivedGeo.set(numUAVs-1, missionGeo);

			GUI.updateProtocolState(numUAVs-1, Nonev2Text.SETUP_FINISHED);
			Nonev2Param.state.set(numUAVs-1, SETUP_FINISHED);
			
			GUI.log("Finalize por el SI MASTER");
			
		}
		
		
		/** TAKING OFF PHASE */
		//GUI.log(Tools.getNumUAVs(), ScanText.TAKING_OFF);
		
		/** TAKING OFF PHASE */
		GUI.log(numUAVs-1, Nonev2Text.TAKING_OFF);
		double altitude = Nonev2Param.takeoffAltitude.get(numUAVs-1);
		double thresholdAltitude = altitude * 0.95;
		
		//double altitude = 20;//Nonev2Param.altitude;
		//double thresholdAltitude = altitude * 0.95;
		
		Nonev2Param.state.set(numUAVs-1, TAKING_OFF);
		
		GUI.log("1- Estado de esta variable:" + Nonev2Param.state.get(numUAVs-1)); //Estado 4
		
		if (!Copter.takeOffNonBlocking(numUAVs-1, altitude)) {
			GUI.exit(Nonev2Text.TAKE_OFF_ERROR + " " + 3);
		}
		//GUI.logVerbose(Tools.getNumUAVs(), ScanText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		int waitingTime;
		
		while (Nonev2Param.state.get(numUAVs-1) == TAKING_OFF) {
			// Discard message=>
			//Copter.receiveMessage(Tools.getNumUAVs()-1, Nonev2Param.RECEIVING_TIMEOUT);
			// Wait until target altitude is reached
			c=c+1;
			//GUI.log("Estoy adentro while: c=" + c);
			
			if (System.currentTimeMillis() - cicleTime > Nonev2Param.TAKE_OFF_CHECK_TIMEOUT) {
				GUI.logVerbose(SimParam.prefix[numUAVs-1] + Text.ALTITUDE_TEXT
						+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAVs-1].getZ())
						+ " " + Text.METERS);
				if (UAVParam.uavCurrentData[numUAVs-1].getZRelative() >= thresholdAltitude) {
					Nonev2Param.state.set(numUAVs-1, FOLLOWING_MISSION);
					GUI.log("Cambie de estado en" + c); //0:00:13 Cambie de estado en585391619
					
				} else {
					cicleTime = cicleTime + Nonev2Param.TAKE_OFF_CHECK_TIMEOUT;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
	
	
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		
		
		
		//Point3D[] mission = Tools.getLoadedMissions()[numUAVs];
		
		//Tools.getUAVMissionSimplified(numUAVs-1);
		
		Point3D[] mission = Nonev2Param.uavMissionReceivedUTM.get(numUAVs-1);
		GeoCoordinates[] missionGeo = Nonev2Param.uavMissionReceivedGeo.get(numUAVs-1);
		
		
		
		//Point3D[] mission = Nonev2Param.uavMissionReceivedUTM.get(numUAVs-1);
		//GeoCoordinates[] missionGeo = Nonev2Param.uavMissionReceivedGeo.get(numUAVs-1);
		//Map<Long, Long> reached = null;
		
		GUI.log("Mision Geo:" + missionGeo[0]); //Estado 4
		
		
		
		//if (this.isMaster) {
		//	reached = new HashMap<>(currentLocations.length);
		//}
		
		
		int currentWP = 0;
		while (Nonev2Param.state.get(Tools.getNumUAVs()-1) == FOLLOWING_MISSION) {
			/** MOVE_TO_WP PHASE */
			GUI.updateProtocolState(Tools.getNumUAVs()-1, Nonev2Text.MOVE_TO_WP);
			GUI.log(Tools.getNumUAVs()-1, Nonev2Text.MOVE_TO_WP);
			GUI.logVerbose(Tools.getNumUAVs()-1, Nonev2Text.LISTENER_WAITING);
			
			
			GeoCoordinates geo = missionGeo[currentWP];
			UTMCoordinates utm = Tools.geoToUTM(geo.latitude, geo.longitude);
			Point2D.Double destination = new Point2D.Double(utm.x, utm.y);
			
			double relAltitude = mission[currentWP].z;
			if (!Copter.moveUAVNonBlocking(Tools.getNumUAVs()-1, geo, (float)relAltitude)) {
				//GUI.exit(ScanText.MOVE_ERROR + " " + selfId);
			}
			cicleTime = System.currentTimeMillis();
			
			GUI.log("antes de entrar al while" + Nonev2Param.moveSemaphore.get(Tools.getNumUAVs()-1)); //Estado 4
			
			while (Nonev2Param.moveSemaphore.get(Tools.getNumUAVs()-1) == currentWP) {
				// Discard message
				
				//Copter.receiveMessage(Tools.getNumUAVs()-1, Nonev2Param.RECEIVING_TIMEOUT);
				// Wait until target location is reached
				if (System.currentTimeMillis() - cicleTime > Nonev2Param.MOVE_CHECK_TIMEOUT) {
					if (UAVParam.uavCurrentData[Tools.getNumUAVs()-1].getUTMLocation().distance(destination) <= Nonev2Param.MIN_DISTANCE_TO_WP
							&& Math.abs(relAltitude - UAVParam.uavCurrentData[Tools.getNumUAVs()-1].getZRelative()) <= 0.2) {
						Nonev2Param.moveSemaphore.incrementAndGet(Tools.getNumUAVs()-1);
					} else {
						cicleTime = cicleTime + Nonev2Param.MOVE_CHECK_TIMEOUT;
						waitingTime = (int)(cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
		}
		
		
		//Copter.moveUAV(numUAVs, LanderParam.LocationEnd, LanderParam.altitude, 20, 2);
		
		
		//if (!Copter.takeOff(0, LanderParam.altitude) || !Copter.moveUAV(Tools.getNumUAVs()-1, LanderParam.LocationEnd, LanderParam.altitude, 20, 2)) {
			//Tratar error
		//}
		
		//Copter.moveUAV(numUAV, geo, relAltitude, destThreshold, altThreshold)moveUAV()
		
		
		//Copter.setFlightMode(0, FlightMode.LAND_ARMED);
		
		
		
	}

	@Override
	public void forceExperimentEnd() {
		this.copy.forceExperimentEnd();
	}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName) {}

	/** Checks the validity of the configuration of the protocol. */
	public static boolean isValidProtocolConfiguration(Nonev2ConfigDialogPanel panel) {
		// Simulation parameters
		String validating = panel.missionsTextField.getText();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		return true;
	}
	
	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(Nonev2ConfigDialogPanel panel) {
		// Simulation parameters
		Tools.setNumUAVs(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
	
	/** Provides the factorial of a number. */
	public static int factorial(int n) {
		int resultado = 1;
		for (int i = 1; i <= n; i++) {
			resultado *= i;
		}
		return resultado;
	}
	
	/** Asserts if the UAV is master. */
	public static boolean isMaster(int numUAV) {
		boolean b = false;
		int role = Tools.getArduSimRole();
		//Tools.se
		if (role == Tools.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = Tools.getIdFromPos(Nonev2Param.MASTER_POSITION);
			for (int i = 0; i < Nonev2Param.MAC_ID.length; i++) {
				if (Nonev2Param.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == Tools.SIMULATOR) {
			if (numUAV == Nonev2Param.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}
	
	/** Provides the best formation for the initial locations, given the target heading:
	 * <p>Long. Id of the UAV in the center of the formation.
	 * <p>Integer. Position of the master UAV in the formation.
	 * <p>uav2DPosition[]. Formation that better suits a safe takeoff.
	 * <p>Method only used by the master UAV. */
	public static Triplet<Long, Integer, uav2DPosition[]> getBestFormation(uav2DPosition[] currentLocations, double masterHeading) {

		int masterPosReo = 0;
		uav2DPosition[] bestFormation = null;
		double distanceOfBestFormation = Double.MAX_VALUE;

		// We look for the drone that behave like center drone
		for (int i = 0; i < currentLocations.length; i++) {
			// We obtain coordinates for the drones that surround the central.
			Point2D.Double[] formation = Nonev2Helper.posSlaveFromMasterLinear(masterHeading,
					currentLocations[i], currentLocations.length - 1, Nonev2Param.initialDistanceBetweenUAVreal);
			Point2D.Double[] flightFormationFinal = Arrays.copyOf(formation, formation.length + 1);

			// Add to the end of the array the one that is being viewed if it is the center
			flightFormationFinal[flightFormationFinal.length - 1] = currentLocations[i];

			// Copy of Ids
			Long[] ids = new Long[flightFormationFinal.length];
			for (int j = 0; j < currentLocations.length; j++) {
				ids[j] = currentLocations[j].id;
			}

			// Calculate all possible combinations of ids
			Permutation<Long> prueba = new Permutation<Long>(ids);
			long[] bestForCurrentCenter = null;
			double dOfCurrentCenter = Double.MAX_VALUE;
			int j = Nonev2Helper.factorial(ids.length);
			while (j > 0) {
				// Test of a permutation
				Long[] mix = prueba.next();
				double acum = 0;
				// Calculation of the sum of distances to the square of each drone on the ground
				// to the position that would correspond
				// to it in the air according to the current combination of ids
				for (int k = 0; k < currentLocations.length; k++) {
					uav2DPosition c = currentLocations[k];
					boolean found = false;
					int pos = -1;
					// Find the position "pos" in the air for current Ids combination
					for (int n = 0; n < mix.length && !found; n++) {

						if (mix[n] == c.id) {
							pos = n;
							found = true;
						}
					}
					// Adjustment of least squares. The distance is exaggerated to know which is
					// closest
					acum = acum + Math.pow(c.distance(flightFormationFinal[pos]), 2);
				}

				if (i == 1) {
					GUI.logVerbose(Arrays.toString(mix));
				}

				if (bestForCurrentCenter == null || acum < dOfCurrentCenter) {
					bestForCurrentCenter = new long[mix.length];
					for (int t = 0; t < mix.length; t++) {
						bestForCurrentCenter[t] = mix[t];
					}
					dOfCurrentCenter = acum;

				}
				j--;
			}

			if (bestFormation == null || dOfCurrentCenter < distanceOfBestFormation) {
				j = 0;
				bestFormation = new uav2DPosition[bestForCurrentCenter.length];
				while (j < bestForCurrentCenter.length) {
					bestFormation[j] = new uav2DPosition(flightFormationFinal[j].x, flightFormationFinal[j].y,
							bestForCurrentCenter[j], masterHeading);
					distanceOfBestFormation = dOfCurrentCenter;
					boolean found = false;
					for (int x = 0; x < bestForCurrentCenter.length; x++) {
						if (bestForCurrentCenter[j] == Nonev2Param.idMaster) {
							found = true;
						}
					}
					if (found) {
						masterPosReo = j;
					}
					j++;
				}

			}
		}

		// Calculus of the UAV in the center of the formation
		long centerGlobal = Long.MAX_VALUE;
		double globalError = Double.MAX_VALUE;
		for (int i = 0; i < bestFormation.length; i++) {
			double acum = 0;
			for (int j = 0; j < bestFormation.length; j++) {
				if (i != j) {
					acum += Math.pow(bestFormation[i].distance(bestFormation[j]), 2);
				}
			}
			if (acum < globalError) {
				centerGlobal = bestFormation[i].id;
			}
		}

		return Triplet.with(centerGlobal, masterPosReo, bestFormation);
	}
}
