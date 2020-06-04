package muscop.logic;

/** This class contains text descriptions for messages used by this protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPText {
	
	// Configuration dialog
	public static final String CONFIGURATION_DIALOG_TITLE_SWARM = "MUSCOP Protocol Configuration";
	public static final String MISSION_SELECT = "Flight mission:";
	public static final String BUTTON_SELECT = "...";
	public static final String GROUND_TEXT = "Ground formation:";
	public static final String FORMATION_TEXT = "Formation";
	public static final String DISTANCE_TEXT = "Min. distance between UAVs:";
	public static final String DISTANCE_TEXT_ERROR = "The min. distance between UAVs must be a positive double";
	public static final String TAKEOFF_STRATEGY = "Take off strategy";
	public static final String CLUSTERS = "Number of clusters";
	public static final String AIR_TEXT = "Flying formation:";
	public static final String LANDING_TEXT = "Landing:";
	public static final String PROTOCOL_TEXT = "MUSCOP";
	public static final String ENABLING = "MUSCOP protocol enabled...";
	
	// Protocol states
	public static final String START = "Start";						// Master UAV detects slaves
	public static final String SETUP = "Setup";						// Data sharing to coordinate the take off
	public static final String SEND_MISSION = "Sending mission";	// Blocking slaves until all have the mission
	public static final String TAKING_OFF = "Taking off";			// Performing the take off
	public static final String SETUP_FINISHED = "Setup finished";	// Waiting for the experiment to start
	public static final String MOVE_TO_WP = "Moving to WP";			// Moving to a waypoint
	public static final String WP_REACHED = "WP reached";			// Waypoint reached
	public static final String MOVE_TO_LAND = "Move to land";		// Moving to the final land location
	public static final String LANDING = "Landing";					// UAV landing
	public static final String FINISH = "Finished";					// The flight is over
	
	// Progress messages
	//   from master
	public static final String MASTER_START_LISTENER = "Master listener detecting slaves.";
	public static final String MASTER_DETECTED_UAVS = "Number of UAVs detected by master:";
	public static final String MASTER_DATA_TALKER = "Master talker sending data with mission.";
	public static final String MASTER_DATA_ACK_LISTENER = "Master listener waiting for data ack.";
	
	public static final String CENTER_WP_REACHED_ACK_LISTENER = "Center listener waiting for waypoint reached ack.";
	public static final String CENTER_SEND_MOVE = "Center talker sending move order.";
	public static final String CENTER_SEND_LAND = "Center talker sending land order.";
	//   from slave
	public static final String SLAVE_WAIT_LIST_TALKER = "Slave talker sending data ack.";
	public static final String SLAVE_WAIT_DATA_LISTENER = "Slave listener waiting for data.";
	
	public static final String NO_CENTER_WAIT_ORDER_LISTENER = "No center listener waiting move to waypoint or land order.";
	public static final String NO_CENTER_WAYPOINT_REACHED_ACK_TALKER = "No center talker sending waypoint reached ack.";
	//   from master or slave
	public static final String LISTENER_WAITING = "Listener waiting.";
	public static final String TALKER_WAITING = "Talker waiting.";
	public static final String LISTENER_FINISHED = "Listener finished.";
	public static final String TALKER_FINISHED = "Talker finished.";
	
	// Error messages
	public static final String MOVE_ERROR_1 = "Unable to move to a waypoint. UAV";
	public static final String MOVE_ERROR_2 = "Unable to move to target location. UAV";
	public static final String LAND_ERROR = "Unable to land the UAV";
	
	public static final String UAVS_START_ERROR_1 = "Failed locating the home position of the master UAV.";
	public static final String UAVS_START_ERROR_2 = "No valid coordinates could be found to stablish the home of the master UAV.";
	public static final String BAD_INPUT = "Please, check the selected information.";
	public static final String MAX_WP_REACHED = "Maximum number of waypoints reached, please use less than 59";
	

	
}
