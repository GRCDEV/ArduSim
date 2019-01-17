package muscop.logic;

/** This class contains text descriptions for messages used by this protocol.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ScanText {
	
	// Configuration dialog
	public static final String CONFIGURATION_DIALOG_TITLE_SWARM = "Swarm Protocol Configuration";
	public static final String MISSION_SELECT = "Flight mission:";
	public static final String BUTTON_SELECT = "...";
	public static final String GROUND_TEXT = "Ground formation:";
	public static final String FORMATION_TEXT = "Formation";
	public static final String DISTANCE_TEXT = "Min. distance between UAVs";
	public static final String AIR_TEXT = "Flying formation:";
	public static final String LANDING_TEXT = "Landing:";
	public static final String PROTOCOL_TEXT = "Scan protocol v2";
	public static final String LANDING = "Landing";
	public static final String ENABLING = "Swarm protocol enabled...";
	
	// Protocol states
	public static final String START = "Start";							// Master UAV detects slaves
	public static final String SETUP = "Setup";							// Master sends mission to slaves
	public static final String READY_TO_FLY = "Ready to fly";			// Blocking slaves until all have the mission
	public static final String WAIT_TAKE_OFF = "Wait take off";			// Waiting its turn to take off
	public static final String TAKING_OFF = "Taking off";				// Performing the take off
	public static final String MOVE_TO_TARGET = "Move to target";		// Moving to the final takeoff location
	public static final String TARGET_REACHED = "Target reached";		// Starting location reached
	public static final String READY_TO_START = "Ready to start";		// Blocking UAVs until all reach the starting location
	public static final String SETUP_FINISHED = "Setup finished";		// Waiting for the experiment to start
	public static final String MOVE_TO_WP = "Moving to WP";				// Moving to a waypoint
	public static final String WP_REACHED = "WP reached";				// Waypoint reached
	public static final String LAND_LOCATION_REACHED = "Move to land";	// Moving to the final land location
	public static final String LANDING_UAV = "Landing";					// UAV landing
	public static final String FINISH = "Finished";						// The flight is over
	
	// Progress messages
	//   from master
	public static final String MASTER_START_LISTENER = "Master listener detecting slaves.";
	public static final String MASTER_DETECTED_UAVS = "Number of UAVs detected by master:";
	public static final String MASTER_DATA_TALKER = "Master talker sending data with mission.";
	public static final String MASTER_DATA_ACK_LISTENER = "Master listener waiting for data ack.";
	public static final String MASTER_READY_TO_FLY_ACK_LISTENER = "Mater listener waiting slaves to confirm they are ready to fly.";
	public static final String MASTER_READY_TO_FLY_TALKER = "Master talker says that everybody is ready to fly.";
	
	public static final String CENTER_TARGET_REACHED_ACK_LISTENER = "Center listener waiting for target reached ack.";
	public static final String CENTER_TAKEOFF_END_ACK_LISTENER = "Center listener waiting other UAVs to confirm they finished takeoff.";
	public static final String CENTER_TAKEOFF_END_TALKER = "Center talker says that everybody finished the takeoff step.";
	public static final String CENTER_WP_REACHED_ACK_LISTENER = "Center listener waiting for waypoint reached ack.";
	public static final String CENTER_SEND_MOVE = "Center talker sending move order.";
	public static final String CENTER_SEND_LAND = "Center talker sending land order.";
	//   from slave
	public static final String SLAVE_START_TALKER = "Slave talker sending hello.";
	public static final String SLAVE_WAIT_LIST_TALKER = "Slave talker sending data ack.";
	public static final String SLAVE_WAIT_DATA_LISTENER = "Slave listener waiting for data.";
	public static final String SLAVE_WAIT_READY_TO_FLY_LISTENER = "Slave listener waiting timeout for takeoff.";
	public static final String SLAVE_READY_TO_FLY_CONFIRM_TALKER = "Slave talker confirms that everybody is ready to fly.";
	
	public static final String NO_CENTER_WAIT_TAKEOFF_END_LISTENER = "No center waiting timeout to end takeoff.";
	public static final String NO_CENTER_TARGET_REACHED_TALKER = "No center talker sending target reached ack.";
	public static final String NO_CENTER_TAKEOFF_END_ACK_TALKER = "No center talker confirms that everybody finished the takeoff step.";
	public static final String NO_CENTER_WAIT_ORDER_LISTENER = "No center listener waiting move to waypoint or land order.";
	public static final String NO_CENTER_WAYPOINT_REACHED_ACK_TALKER = "No center talker sending waypoint reached ack.";
	//   from master or slave
	public static final String LISTENER_WAITING = "Listener waiting.";
	public static final String TALKER_WAITING = "Talker waiting.";
	public static final String LISTENER_WAITING_TAKE_OFF = "Listener waiting take off command.";
	public static final String TALKER_TAKE_OFF_COMMAND = "Talker sending take off command.";
	public static final String LISTENER_FINISHED = "Listener finished.";
	public static final String TALKER_FINISHED = "Talker finished.";
	
	// Error messages
	public static final String TAKE_OFF_ERROR = "Unable to perform the take off of the UAV";
	public static final String MOVE_ERROR_1 = "Unable to move to a waypoint. UAV";
	public static final String MOVE_ERROR_2 = "Unable to move to target location. UAV";
	public static final String LAND_ERROR = "Unable to land the UAV";
	
	public static final String FATAL_ERROR = "Fatal error";
	public static final String UAVS_START_ERROR_1 = "Failed locating the home position of the UAV";
	public static final String UAVS_START_ERROR_2 = "No valid coordinates could be found to stablish the home of the UAV";
	public static final String BAD_INPUT = "Please, check the selected information.";
	public static final String CENTER_ID_NOT_FOUND = "Center UAV not found in the target flight formation.";
	public static final String CENTER_LOCATION_NOT_FOUND = "Center UAV location not found.";
	public static final String MAX_WP_REACHED = "Maximum number of waypoints reached, please use less than 59";
	

	
}
