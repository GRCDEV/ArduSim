package followme.logic;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FollowMeText {
	public static final String PROTOCOL_TEXT = "Follow Me";
	
	// Configuration dialog
	public static final String CONFIGURATION_DIALOG_TITLE_SWARM = "FollowMe Protocol Configuration";
	public static final String GROUND_LOCATION = "Ground master location:";
	public static final String LATITUDE = "Latitude:";
	public static final String LATITUDE_ERROR = "Latitude value must be a value in degrees";
	public static final String LONGITUDE = "Longitude:";
	public static final String LONGITUDE_ERROR = "Longitude value must be a value in degrees";
	public static final String YAW = "Yaw:";
	public static final String YAW_ERROR = "Yaw value must be a value in degrees";
	public static final String GROUND_TEXT = "Ground formation:";
	public static final String FORMATION_TEXT = "Formation";
	public static final String DISTANCE_TEXT = "Min. distance between UAVs";
	public static final String DISTANCE_TEXT_ERROR = "The min. distance between UAVs must be a positive double";
	public static final String AIR_TEXT = "Flying formation:";
	public static final String INITIAL_RELATIVE_ALTITUDE = "Initial relative altitude:";
	public static final String INITIAL_RELATIVE_ALTITUDE_ERROR = "The take off altitude must be a positive double";
	public static final String LANDING_TEXT = "Landing formation:";
	public static final String SIMULATED_DATA = "Simulated flight data:";
	public static final String SIMULATED_DATA_DIALOG_TITLE = "Select the .txt file with the real RC values";
	public static final String DATA_TXT_FILE = "Data txt file";
	public static final String SIMULATED_DATA_ERROR = "Invalid file contents.";
	public static final String MASTER_UAV_SPEED = "Master UAV speed:";
	public static final String MASTER_UAV_SPEED_ERROR = "The master UAV speed must be a positive integer";
	public static final String COMM_PERIOD = "Master location advise period:";
	public static final String BAD_INPUT = "Please, check the selected information.";
	
	// Protocol states
	public static final String START = "Start";							// Master UAV detects slaves
	public static final String SETUP = "Setup";							// Master sends mission to slaves
	public static final String READY_TO_FLY = "Ready to fly";			// Blocking slaves until all have the mission
	public static final String WAIT_TAKE_OFF = "Wait take off";			// Waiting its turn to take off
	public static final String TAKING_OFF = "Taking off";				// Performing the take off
	public static final String MOVE_TO_TARGET = "Move to target";		// Moving to the final takeoff location
	public static final String TARGET_REACHED = "Target reached";		// Starting location reached
	public static final String WAIT_SLAVES = "Waiting slaves";			// Master waiting slaves to complete take off
	public static final String READY_TO_START = "Ready to start";		// Blocking UAVs until all reach the starting location
	public static final String SETUP_FINISHED = "Setup finished";		// Waiting for the experiment to start
	public static final String FOLLOWING = "Following";					// Master UAV manually controlled, and slaves following master UAV
	public static final String MOVE_TO_LAND = "Move to land";			// Moving to the final land location
	public static final String LANDING = "Landing";						// UAV landing
	public static final String FINISH = "Finished";						// The flight is over
	
	// General messages
	public static final String ENABLING = "Enabling " + PROTOCOL_TEXT + " protocol...";
	
	// Progress messages
	//   from master
	public static final String MASTER_START_LISTENER = "Master listener detecting slaves.";
	public static final String MASTER_DETECTED_UAVS = "Number of UAVs detected by master:";
	public static final String MASTER_DATA_TALKER = "Master talker sending data with take off location.";
	public static final String MASTER_DATA_ACK_LISTENER = "Master listener waiting for data ack.";
	public static final String MASTER_READY_TO_FLY_ACK_LISTENER = "Mater listener waiting slaves to confirm they are ready to fly.";
	public static final String MASTER_READY_TO_FLY_TALKER = "Master talker says that everybody is ready to fly.";
	public static final String MASTER_TARGET_REACHED_ACK_LISTENER = "Master listener waiting for target reached ack.";
	public static final String MASTER_TAKEOFF_END_ACK_LISTENER = "Master listener waiting other UAVs to confirm they finished takeoff.";
	public static final String MASTER_TAKEOFF_END_TALKER = "Master talker says that everybody finished the takeoff step.";
	public static final String MASTER_WAIT_ALTITUDE = "Master talker waiting altitude to start protocol.";
	public static final String MASTER_SEND_LAND = "Master talker sending land order.";
	//   from slave
	public static final String SLAVE_START_TALKER = "Slave talker sending hello.";
	public static final String SLAVE_WAIT_LIST_TALKER = "Slave talker sending data ack.";
	public static final String SLAVE_WAIT_DATA_LISTENER = "Slave listener waiting for data.";
	public static final String SLAVE_WAIT_READY_TO_FLY_LISTENER = "Slave listener waiting timeout for takeoff.";
	public static final String SLAVE_READY_TO_FLY_CONFIRM_TALKER = "Slave talker confirms that everybody is ready to fly.";
	public static final String SLAVE_WAIT_TAKEOFF_END_ACK = "No center waiting takeoff end ack from master UAV";
	public static final String SLAVE_WAIT_TAKEOFF_END_LISTENER = "Slave waiting timeout to end setup.";
	public static final String SLAVE_TARGET_REACHED_TALKER = "Slave talker sending target reached ack.";
	public static final String SLAVE_TAKEOFF_END_ACK_TALKER = "Slave talker confirms that everybody finished the takeoff step.";
	public static final String SLAVE_WAIT_ORDER_LISTENER = "Slave listener waiting move to or land order.";
	//   from master or slave
	public static final String LISTENER_WAITING = "Listener waiting.";
	public static final String TALKER_WAITING = "Talker waiting.";
	public static final String LISTENER_WAITING_TAKE_OFF = "Listener waiting take off command.";
	public static final String TALKER_TAKE_OFF_COMMAND = "Talker sending take off command.";
	public static final String LISTENER_FINISHED = "Listener finished.";
	public static final String TALKER_FINISHED = "Talker finished.";
	
	// Errors
	public static final String TAKE_OFF_ERROR = "Unable to perform the take off of the UAV";
	public static final String MOVE_ERROR = "Unable to move to target location. UAV";
	public static final String MASTER_LOITER_ERROR = "Unable to change to loiter";
	public static final String LAND_ERROR = "Unable to land the UAV";
	
}
