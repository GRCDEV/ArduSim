package api;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.javatuples.Pair;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import followme.logic.FollowMeHelper;
import main.Param;
import main.Tools;
import pollution.PollutionHelper;
import main.Param.Protocol;
import main.Param.SimulatorState;
import main.Text;
import sim.board.BoardPanel;
import sim.board.BoardParam;
import sim.logic.SimTools;
import swarm.SwarmText;
import swarmprot.logic.SwarmProtHelper;
import swarmprot.logic.SwarmProtParam;
import uavController.UAVParam;
import uavFishing.logic.UavFishingHelper;

/**
 * This class consists exclusively of static methods that allow to launch
 * threads, and set specific configuration for swarm based protocols.
 * <p>
 * The methods are sorted as they are called by the application on its different
 * stages.
 * <p>
 * Most of these methods have their equivalent on the class MissionHelper, where
 * example code can be analyzed.
 * <p>
 * When developing a new protocol, the developer has to include code in this
 * methods, if not stated otherwise.
 */

public class SwarmHelper {

	/**
	 * Asserts if it is needed to load a mission, when using protocol on a real UAV.
	 */
	public static boolean loadMission() {

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.loadMission();
		}

		// Add here code to decide if the UAV has to load a mission, depending on the
		// protocol logic

		return false;
	}

	/** Opens the configuration dialog of swarm protocols. */
	@SuppressWarnings("unchecked")
	public static void openSwarmConfigurationDialog() {

		// Add here the configuration window of any swarm protocol
		// or set Param.sim_status = SimulatorState.STARTING_UAVS to go to the next step

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.openSwarmConfigurationDialog();
		}
		// TODO
		if (Param.selectedProtocol == Protocol.POLLUTION) {
			PollutionHelper.openConfigurationDialog();
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			SwarmHelper.log("SwarmConfigurationDialog --> Mas tarde");
		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			UavFishingHelper.openConfigurationDialog();
		}

		//TODO esto debera de hacerse dentro del OK del cuadro de configuracion del dialogo
		if(Param.selectedProtocol != Protocol.SWARM_PROT_V1) {
			Param.simStatus = SimulatorState.STARTING_UAVS;
		}

	}

	/** Initializes data structures related to swarm protocols. */
	public static void initializeSwarmDataStructures() {

		// Add here the initialization of data structures needed for any swarm protocol

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.initializeProtocolDataStructures();
		}
		
		// TODO
		if (Param.selectedProtocol == Protocol.POLLUTION) {
			PollutionHelper.initializeDataStructures();
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			FollowMeHelper.initializeProtocolDataStructures();
		}
	}

	/**
	 * Sets the initial value of the protocol state in the progress window for
	 * mission based protocols.
	 */
	public static void setSwarmProtocolInitialState(JLabel label) {

		// Add here the initial value of the protocol in the progress dialog
		//TODO el texto en SwarmText
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			label.setText("INICIO");
		}
	}

	/**
	 * Updates the value of the protocol state in the progress window for swarm
	 * protocols.
	 * <p>
	 * Do not modify this method.
	 */
	public static void setSwarmState(int numUAV, String text) {
		SimTools.updateprotocolState(numUAV, text);
	}

	/**
	 * Updates the text shown in the upper-right corner when swarm protocols change
	 * the state.
	 * <p>
	 * Do not modify this method.
	 */
	public static void setSwarmGlobalInformation(String text) {
		SimTools.updateGlobalInformation(text);
	}

	/**
	 * Updates the log in the main window.
	 * <p>
	 * Do not modify this method.
	 */
	public static void log(String text) {
		SimTools.println(text);
	}

	/**
	 * Rescales swarm protocols specific data structures when the visualization
	 * scale changes.
	 */
	public static void rescaleSwarmDataStructures() {

		// Add here a function to locate any shown information in the main window

	}

	/**
	 * Rescales for visualization the resources used in swarm protocols, each time
	 * the visualization scale changes.
	 */
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

	/**
	 * Gets the initial position of the UAVs from the mission of the master and
	 * other source for the other UAVs.
	 */
	public static Pair<GeoCoordinates, Double>[] getSwarmStartingLocation() {

		// Add here code to locate the starting position of the UAVs from its mission or
		// any other source

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			return SwarmProtHelper.getSwarmStartingLocationV1();
		}
		
		// TODO
		if (Param.selectedProtocol == Protocol.POLLUTION) {
			return PollutionHelper.getStartingLocation();
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			return FollowMeHelper.getSwarmStartingLocation();
		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			return UavFishingHelper.getStartingLocation();
		}
		
		
		return null;

	}

	/**
	 * Sends the initial configuration needed by the mission based protocol to a
	 * specific UAV.
	 * <p>
	 * Returns true if the operation was successful.
	 */
	public static boolean sendBasicSwarmConfig(int numUAV) {

		// Add here code to send a mission or whatever operation previous to the setup
		// process
		boolean success = false;

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			success = SwarmProtHelper.sendBasicSwarmConfig(numUAV);
		}
		
		if (Param.selectedProtocol == Protocol.POLLUTION) {
			success = true;
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			// De momento nada
			success = true;
		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			success = UavFishingHelper.sendBasicConfig(numUAV);
		}
		
		return success;
	}

	/** Launch threads related to swarm protocols. */
	public static void launchSwarmThreads() {

		// Add here to launch threads for any swarm protocol

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.startSwarmThreads();
		}
		if (Param.selectedProtocol == Protocol.POLLUTION) {
			PollutionHelper.launchThreads();
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			FollowMeHelper.startFollowMeThreads();
		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			UavFishingHelper.launchThreads();
		}
	}

	/** Setup of swarm protocols, when the user pushes the configuration button. */
	public static void swarmSetupActionPerformed() {

		// Add here to do an initial setup of any swarm protocol. The method must wait
		// until finished

		// Finally, change the simulator state
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
//			Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {

			FollowMeHelper.openFollowMeConfigurationDialog();
			// Modo de vuelo Loiter / Loiter_Armed
			// UAVParam.flightMode.set(0, Mode.LOITER);
			Param.simStatus = SimulatorState.READY_FOR_TEST;
		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			Param.simStatus = SimulatorState.READY_FOR_TEST;
		}
	}

	/** Starts the movement of each UAV of swarm protocols. */
	public static void startSwarmTestActionPerformed() {

		// Add here code to activate the protocol

		/**
		 * Starts the movement of each UAV by setting the initial speed, the first
		 * waypoint and changing to auto mode.
		 */

		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			SwarmProtHelper.startSwarmTestActionPerformedV1();
		}
		if (Param.selectedProtocol == Protocol.FOLLOW_ME_V1) {
			// SwarmHelper.log("startSwarmTestActionPerformed ");
			SwarmHelper.log("Ready for test...");
			// Param.simStatus = SimulatorState.READY_FOR_TEST;

			// Param.simStatus = SimulatorState.READY_FOR_TEST;
			// Modo de vuelo Loiter / Loiter_Armed
			// UAVParam.flightMode.set(0, Mode.LOITER);
			if (!API.armEngines(0) || !API.setMode(0, UAVParam.Mode.LOITER) || !API.setThrottle(0)) {
				// Tratar el fallo
			}

		}
		if (Param.selectedProtocol == Protocol.UAVFISHING) {
			
			UavFishingHelper.startTestActionPerformed();
		}

	}

	/** Detects the end of the swarm experiment. */
	public static void detectSwarmEnd() {

		// Add here code to detect the end of the experiment and force the UAVs to land,
		// if needed (i.e. when the user pushes a new button)

	}

	/** Logs the experiment main results. */
	public static String getSwarmTestResults() {
		String results = "";

		// Add here code to log the experiment results to the results dialog, as in
		// MissionHelper.getMissionTestResults

		return results;
	}

	/** Logs the swarm protocols configuration. */
	public static String getSwarmProtocolConfig() {
		String configuration = "";

		// Add here code to log the swarm protocols configuration to the results dialog,
		// as in MissionHelper.getMissionProtocolParameters

		return configuration;
	}

	/** Logging to file of specific information related to swarm protocols. */
	public static void logSwarmData(String folder, String baseFileName) {

		// Add here to store any information generated by any swarm protocol, as in
		// MissionHelper.logMissionData

	}


}
