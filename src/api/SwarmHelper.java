package api;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.javatuples.Pair;

import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Tools;
import main.Param.Protocol;
import main.Param.SimulatorState;
import main.Text;
import sim.board.BoardPanel;
import sim.board.BoardParam;
import sim.logic.SimParam;
import sim.logic.SimTools;
import swarmprot.logic.SwarmProtHelper;
import swarmprot.logic.SwarmProtParam;
import uavController.UAVParam;

/** This class consists exclusively of static methods that allow to launch threads, and set specific configuration for swarm based protocols.
 *  <p>The methods are sorted as they are called by the application on its different stages.
 *  <p>Most of these methods have their equivalent on the class MissionHelper, where example code can be analized.
 *  <p>When developing a new protocol, the developer has to include code in this methods, if not stated otherwise. */

public class SwarmHelper {
	
	/** Asserts if it is needed to load a mission, when using protocol on a real UAV. */
	public static boolean loadMission() {
		
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			// Only the master UAV has a mission
			/** You get the id = mac for real drone */
			for (int i = 0; i < SwarmProtParam.MACId.length; i++) {
				if (SwarmProtParam.MACId[i] == Param.id[SwarmProtParam.posMaster]) {
					SwarmProtParam.idMaster = Param.id[SwarmProtParam.posMaster];
					return true;
				}
			}
		}
		
		// Add here code to decide if the UAV has to load a mission, depending on the protocol logic
		
		return false;
	}
	
	/** Opens the configuration dialog of swarm protocols. */
	@SuppressWarnings("unchecked")
	public static void openSwarmConfigurationDialog() {
		
		// Add here the configuration window of any swarm protocol
		//  or set Param.sim_status = SimulatorState.STARTING_UAVS to go to the next step
		
		/** Si pongo una pantalla de preconfiguración, se hace aqui */

		/** We load the file directly with the mission. In a future add menu */
		List<Waypoint> mission = Tools.loadMission(GUIHelper.getCurrentFolder());
		
		if (mission != null) {
			/** The master is assigned the first mission in the list */
			UAVParam.missionGeoLoaded = new ArrayList[Param.numUAVs];
			UAVParam.missionGeoLoaded[SwarmProtParam.posMaster] = mission;

		} else {
			JOptionPane.showMessageDialog(null, Text.MISSIONS_ERROR_3, Text.MISSIONS_SELECTION_ERROR,
					JOptionPane.WARNING_MESSAGE);

			return;
		}
		SwarmProtParam.idMaster = SwarmProtParam.idMasterSimulation;
		
		Param.simStatus = SimulatorState.STARTING_UAVS;
	}

	/** Initializes data structures related to swarm protocols. */
	public static void initializeSwarmDataStructures() {
		
		// Add here the initialization of data structures needed for any swarm protocol
		
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.initializeProtocolDataStructures();
		}
	}

	/** Sets the initial value of the protocol state in the progress window for mission based protocols. */
	public static void setSwarmProtocolInitialState(JLabel label) {

		// Add here the initial value of the protocol in the progress dialog

	}
	
	/** Updates the value of the protocol state in the progress window for swarm protocols.
	 * <p>Do not modify this method. */
	public static void setSwarmState(int numUAV, String text) {
		SimTools.updateprotocolState(numUAV, text);
	}
	
	/** Updates the text shown in the upper-right corner when swarm protocols change the state.
	 * <p>Do not modify this method. */
	public static void setSwarmGlobalInformation(String text) {
		SimTools.updateGlobalInformation(text);
	}
	
	/** Updates the log in the main window.
	 * <p>Do not modify this method. */
	public static void log(String text) {
		SimTools.println(text);
	}
	
	/** Rescales swarm protocols specific data structures when the visualization scale changes. */
	public static void rescaleSwarmDataStructures() {
		
		// Add here a function to locate any shown information in the main window
		
	}
	
	/** Rescales for visualization the resources used in swarm protocols, each time the visualization scale changes. */
	public static void rescaleSwarmResources() {
		
		// Add here a function to redraw any resource used for any swarm protocol
		
	}
	
	/** Draw resources used in swarm protocols. */
	public static void drawSwarmResources(Graphics2D g2, BoardPanel p) {
		
		// Add here a function to draw any resource related to any swarm protocol
		
	}
	
	/** Loads needed resources for swarm protocols. */
	public static void loadSwarmResources() {
		
		// Add here to load resources for any swarm protocol
		
	}
	
	/** Gets the initial position of the UAVs from the mission of the master and other source for the other UAVs. */
	@SuppressWarnings("unchecked")
	public static Pair<GeoCoordinates, Double>[] getSwarmStartingLocation() {
		
		// Add here code to locate the starting position of the UAVs from its mission or any other source
		
		/** Position the master on the map */
		Pair<GeoCoordinates, Double>[] startingLocations = new Pair[Param.numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;

		if (UAVParam.missionGeoLoaded != null && UAVParam.missionGeoLoaded[SwarmProtParam.posMaster] != null) {
			waypointFound = false;
			for (int j=0; j<UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].size() && !waypointFound; j++) {
				waypoint1 = UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].get(j);
				if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				GUIHelper.exit(Text.UAVS_START_ERROR_6 + " " + Param.id[SwarmProtParam.posMaster]);
			}
			waypointFound = false;
			for (int j=waypoint1pos+1; j<UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].size() && !waypointFound; j++) {
				waypoint2 = UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].get(j);
				if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				// We only can set a heading if at least two points with valid coordinates are found
				p1UTM = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
				p2UTM = GUIHelper.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
				incX = p2UTM.Easting - p1UTM.Easting;
				incY = p2UTM.Northing - p1UTM.Northing;
				if (incX != 0 || incY != 0) {
					if (incX == 0) {
						if (incY > 0)	heading = 0.0;
						else			heading = 180.0;
					} else if (incY == 0) {
						if (incX > 0)	heading = 89.9;
						else			heading = 270.0;
					} else {
						double gamma = Math.atan(incY/incX);
						if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
						else 			heading = 270.0 - gamma * 180 / Math.PI;
					}
				}
			}
		} else {
			JOptionPane.showMessageDialog(null,
					Text.APP_NAME + " " + Text.UAVS_START_ERROR_5 + " " + (SwarmProtParam.posMaster + 1) + ".",
					Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		startingLocations[SwarmProtParam.posMaster] = Pair.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()),
				heading);
		
		/** Position the slaves on the map according to master position */
		SwarmProtParam.masterHeading = heading;
		UTMCoordinates initialPoint = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		UTMCoordinates destPointP = new UTMCoordinates(initialPoint.Easting, initialPoint.Northing, initialPoint.Zone, initialPoint.Letter);
		UTMCoordinates destPointI = new UTMCoordinates(initialPoint.Easting, initialPoint.Northing, initialPoint.Zone, initialPoint.Letter);
		GeoCoordinates destPointFinal;

		for (int i = 0; i < Param.numUAVs; i++) {
			if (i != SwarmProtParam.posMaster) {
				if (i % 2 == 0) {
					destPointP.Easting = SwarmProtParam.initialDistanceBetweenUAV
							* Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180) + destPointP.Easting;
					destPointP.Northing = 0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							- SwarmProtParam.initialDistanceBetweenUAV
									* Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ destPointP.Northing;

					destPointFinal = GUIHelper.UTMToGeo(destPointP.Easting, destPointP.Northing);
					startingLocations[i] = Pair.with(new GeoCoordinates(destPointFinal.latitude, destPointFinal.longitude),
							SwarmProtParam.masterHeading);
				} else {
					destPointI.Easting = destPointI.Easting - (SwarmProtParam.initialDistanceBetweenUAV
							* Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));
					destPointI.Northing = destPointI.Northing
							- (0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
									- SwarmProtParam.initialDistanceBetweenUAV
											* Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));

					destPointFinal = GUIHelper.UTMToGeo(destPointI.Easting, destPointI.Northing);
					startingLocations[i] = Pair.with(new GeoCoordinates(destPointFinal.latitude, destPointFinal.longitude),
							SwarmProtParam.masterHeading);
				}
			}
		}
		
		return startingLocations;
	}
	
	/** Sends the initial configuration needed by the mission based protocol to a specific UAV.
	 * <p>Returns true if the operation was successful. */
	public static boolean sendBasicSwarmConfig(int numUAV) {
		
		// Add here code to send a mission or whatever operation previous to the setup process
		
		boolean success = true;
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {
			success = false;
			// Erases, sends and retrieves the planned mission. Blocking procedure
			if (API.clearMission(numUAV)
					&& API.sendMission(numUAV, UAVParam.missionGeoLoaded[numUAV])
					&& API.getMission(numUAV)
					&& API.setCurrentWaypoint(numUAV, 0)) {
				MissionHelper.simplifyMission(numUAV);
				Param.numMissionUAVs.incrementAndGet();
				if (!Param.IS_REAL_UAV) {
					BoardParam.rescaleQueries.incrementAndGet();
				}
				success = true;
			}
		}
		
		return success;
	}
	
	/** Launch threads related to swarm protocols. */
	public static void launchSwarmThreads() {
		
		// Add here to launch threads for any swarm protocol
		
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
//			SwarmProtHelper.startSwarmThreads();
		}
	}
	
	/** Setup of swarm protocols, when the user pushes the configuration button. */
	public static void swarmSetupActionPerformed() {
		
		// Add here to do an initial setup of any swarm protocol. The method must wait until finished
		
		
		
		// Finally, change the simulator state
		Param.simStatus = SimulatorState.READY_FOR_TEST;
	}
	
	/** Starts the movement of each UAV of swarm protocols. */
	public static void startSwarmTestActionPerformed() {
		
		// Add here code to activate the protocol
		
		/**
		 * Starts the movement of each UAV by setting the initial speed, the first
		 * waypoint and changing to auto mode.
		 */
//		inicializar máquina de estados y añadir este código a un hilo
		for (int i = 0; i < Param.numUAVs; i++) {
			/** If you drone is master */
			if (Param.id[i] == SwarmProtParam.idMaster) {

				/** We create a list with the points of the mission */
				List<WaypointSimplified> listOfPoints = UAVParam.missionUTMSimplified.get(SwarmProtParam.posMaster);

				for (int j = 1; j < listOfPoints.size(); j++) {
					/** We obtain each of those points that are given in X and Y */
					WaypointSimplified pointMasterxy = listOfPoints.get(j);

					/** They are converted to Geometrical coordinates */
					GeoCoordinates pointMasterGEO = GUIHelper.UTMToGeo(pointMasterxy.x, pointMasterxy.y);

					API.moveUAV(SwarmProtParam.posMaster, pointMasterGEO, (float)pointMasterxy.z);
				}

			}

		}
	}
	
	/** Detects the end of the swarm experiment. */
	public static void detectSwarmEnd( ) {
		
		// Add here code to detect the end of the experiment and force the UAVs to land, if needed (i.e. when the user pushes a new button)
		
	}
	
	/** Logs the experiment main results. */
	public static String getSwarmTestResults() {
		String results = "";
		
		// Add here code to log the experiment results to the results dialog, as in MissionHelper.getMissionTestResults
		
		return results;
	}
	
	/** Logs the swarm protocols configuration. */
	public static String getSwarmProtocolConfig() {
		String configuration = "";
		
		// Add here code to log the swarm protocols configuration to the results dialog, as in MissionHelper.getMissionProtocolParameters
		
		return configuration;
	}
	
	/** Logging to file of specific information related to swarm protocols. */
	public static void logSwarmData(String folder, String baseFileName) {
		
		// Add here to store any information generated by any swarm protocol, as in MissionHelper.logMissionData
		
	}
	
	private static void takeoff() {
		for (int i = 0; i < Param.numUAVs; i++) {
			if (!API.setMode(i, UAVParam.Mode.GUIDED) || !API.armEngines(i) || !API.doTakeOff(i)) {
				JOptionPane.showMessageDialog(null, Text.TAKE_OFF_ERROR_1 + " " + (i + 1), Text.FATAL_ERROR,
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}

		}
		/** The application must wait until the UAV reaches the planned altitude */
		for (int i = 0; i < Param.numUAVs; i++) {
			while (UAVParam.uavCurrentData[i].getZRelative() < 0.95 * UAVParam.takeOffAltitude[i]) {
				if (Param.VERBOSE_LOGGING) {
					SwarmHelper.log(SimParam.prefix[i] + Text.ALTITUDE_TEXT + " = "
							+ String.format("%.2f", UAVParam.uavCurrentData[i].getZ()) + " " + Text.METERS);
				}
				GUIHelper.waiting(SwarmProtParam.ALTITUDE_WAIT);
			}
		}
	}
	
}
