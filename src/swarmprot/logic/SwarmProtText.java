package swarmprot.logic;

/** This class contains text descriptions for messages used by this protocol. */

public class SwarmProtText {
	
	// Configuration dialog
	public static final String CONFIGURATION_DIALOG_TITLE_SWARM = "Swarm Protocol Configuration";
	public static final String MISSIONS_DIALOG_TITLE = "Select the file(s) with UAVs paths";
	public static final String MISSIONS_DIALOG_SELECTION_1 = "Google Earth kml File";
	public static final String FILE_EXTENSION_KML = "kml";
	public static final String MISSIONS_DIALOG_SELECTION_2 = "Waypoint Files";
	public static final String FILE_EXTENSION_WAYPOINTS = "waypoints";
	public static final String BUTTON_SELECT = "...";
	public static final String MISSIONS_SELECTION_ERROR = "Missions selection warning";
	public static final String MISSIONS_ERROR_3 = "The kml file is not valid.";
	
	public static final String PROTOCOL_TEXT = "Swarm protocol";
	public static final String LOAD_MISSION_KML = "Loading the .kml file into the master dron";
	public static final String CONFIGURE_MASTER_MISSION = "Set up the mission in the master dron";
	public static final String LANDING = "Landing";
	public static final String ENABLING = "Swarm protocol enabled...";
	
	// Log Messages
	public static final String DETECTED = "The Master UAV has detected ";
	
	// Protocol states
	public static final String START = "Start";						// Master UAV detects slaves
	public static final String SETUP = "Setup";						// Master sends mission to slaves
	public static final String SETUP_FINISHED = "Setup finished";	// Waiting for the experiment to start
	public static final String WAIT_TAKE_OFF = "Wait take off";		// Waiting his turn to take off;
	public static final String TAKING_OFF = "Taking off";		// Performing the take off;
	public static final String MOVE_TO_WP = "Moving to WP";		// Moving to a waypoint;
	public static final String WP_REACHED = "WP reached";		// Waypoint reached;
	public static final String LANDING_UAV = "Landing";
	public static final String FINISH = "Finished";				// The flight is over;
	
	// Progress messages
	//   from master
	public static final String MASTER_START_LISTENER = "Master listener detecting slaves";
	public static final String MASTER_SEND_DATA = "Master talker sending data with mission";
	public static final String MASTER_DATA_ACK_LISTENER = "Master listener waiting for data ack";
	public static final String MASTER_WP_REACHED_ACK_LISTENER = "Master listener waiting for waypoint reached ack";
	public static final String MASTER_SEND_MOVE = "Master talker sending move order";
	public static final String MASTER_SEND_LAND = "Master talker sending land order";
	//   from slave
	public static final String SLAVE_START_TALKER = "Slave talker sending hello";
	public static final String SLAVE_WAIT_LIST_TALKER = "Slave talker sending data ack";
	public static final String SLAVE_WAIT_DATA_LISTENER = "Slave listener waiting for data";
	public static final String SLAVE_WAIT_ORDER_LISTENER = "Slave listener waiting move to waypoint or land order";
	public static final String SLAVE_SEND_WP_REACHED_ACK = "Slave talker sending waypoint reached ack";
	//   from master or slave
	public static final String LISTENER_WAITING = "Listener waiting";
	public static final String TALKER_WAITING = "Talker waiting";
	public static final String WAITING_TAKE_OFF = "Listener waiting take off command";
	public static final String TAKE_OFF_COMMAND = "Talker sending take off command";
	public static final String LISTENER_FINISHED = "Listener finished";
	public static final String TALKER_FINISHED = "Talker finished";
	
	// Error messages
	public static final String TAKE_OFF_ERROR = "Unable to perform the take off of the UAV";
	public static final String MOVE_ERROR = "Unable to move to a waypoint. UAV";
	public static final String LAND_ERROR = "Unable to land the UAV";
	
	public static final String FATAL_ERROR = "Fatal error";
	public static final String UAVS_START_ERROR_1 = "Failed locating the home position of the UAV";
	public static final String UAVS_START_ERROR_2 = "No valid coordinates could be found to stablish the home of the UAV";
	public static final String BAD_INPUT = "Please, check the selected information.";
	public static final String MAX_WP_REACHED = "Maximum number of waypoints reached, please use less than 60";
	

	
}
