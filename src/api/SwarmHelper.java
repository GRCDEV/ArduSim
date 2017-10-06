package api;

import java.awt.Graphics2D;

import javax.swing.JLabel;

import org.javatuples.Pair;

import api.pojo.GeoCoordinates;
import main.Param;
import main.Param.Protocol;
import main.Param.SimulatorState;
import sim.board.BoardPanel;
import sim.logic.SimTools;

/** This class consists exclusively of static methods that allow to launch threads, and set specific configuration for swarm based protocols.
 *  <p>The methods are sorted as they are called by the application on its different stages.
 *  <p>Most of these methods have their equivalent on the class MissionHelper, where example code can be analized.
 *  <p>When developing a new protocol, the developer has to include code in this methods, if not stated otherwise. */

public class SwarmHelper {
	
	/** Asserts if it is needed to load a mission, when using protocol on a real UAV. */
	public static boolean loadMission() {
		
		if (Param.selectedProtocol == Protocol.SWARM_PROT_V1) {
			// Only the master UAV has a mission
//			true si es el master
			
		}
		
		// Add here code to decide if the UAV has to load a mission, depending on the protocol logic
		
		return false;
	}
	
	/** Opens the configuration dialog of swarm protocols. */
	public static void openSwarmConfigurationDialog() {
		
		// Add here the configuration window of any swarm protocol
		//  or set Param.sim_status = SimulatorState.STARTING_UAVS to go to the next step
		Param.simStatus = SimulatorState.STARTING_UAVS;
	}

	/** Initializes data structures related to swarm protocols. */
	public static void initializeSwarmDataStructures() {
		
		// Add here the initialization of data structures needed for any swarm protocol
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
	public static Pair<GeoCoordinates, Double>[] getSwarmStartingLocation() {
		
		// Add here code to locate the starting position of the UAVs from its mission or any other source
		
		return null; // Must be modified
	}
	
	/** Sends the initial configuration needed by the mission based protocol to a specific UAV.
	 * <p>Returns true if the operation was successful. */
	public static boolean sendBasicSwarmConfig(int numUAV) {
		
		// Add here code to send a mission or whatever operation previous to the setup process
		
		return false;	// In case of error, return false
	}
	
	/** Launch threads related to swarm protocols. */
	public static void launchSwarmThreads() {
		
		// Add here to launch threads for any swarm protocol
		
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
	
}
