package swarmprot.logic;

/** This class contains text descriptions for messages used by this protocol. */

public class SwarmProtText {
	
	public static final String PROTOCOL_TEXT = "Swarm protocol";
	public static final String LOAD_MISSION_KML = "Loading the .kml file into the master dron";
	public static final String CONFIGURE_MASTER_MISSION = "Set up the mission in the master dron";
	public static final String LANDING = "Landing all the drones";
	public static final String ALL_LANDING = "All the Drones are landing";
	public static final String ENABLING = "Swarm protocol enabled...";
	
	//Log Messages
	public static final String DETECTED = "The Master UAV has detected ";
	
	//Simulator states
	public static final String START = "Master dron detects his slaves";
	public static final String SEND_DATA ="The master dron sends flight information of speed and altitude of the first phase" ;
	public static final String WAIT_LIST = "Get their flight route";
	public static final String SEND_LIST = "The master dron sends the flight route";
	public static final String WAIT_TAKE_OFF = "Wait his turn to take off";
	public static final String SEND_TAKE_OFF = "Master wait his turn to take off";
	public static final String TAKING_OFF = "Began its takeoff";
	public static final String MOVE_TO_WP = "Moves to waypoint";
	public static final String WP_REACHED = "Waypoint reached";
	public static final String LANDING_UAV = "Landing";
	public static final String FINISH = "The flight is over";
	
	//States Strings Master
	public static final String MASTER_START_TALKER = "Wait";
	public static final String MASTER_START_LISTENER = "Detect his slaves";
	public static final String MASTER_SEND_DATA_TALKER = "Send MSJ2, with altitude and IDs for taking off";
	public static final String MASTER_SEND_DATA_LISTENER = "Waiting for the ACK2";
	public static final String MASTER_SEND_LIST_TALKER = "Sens MSJ3, with modified route";
	public static final String MASTER_SEND_LIST_LISTENER = "Waiting for the ACK3";
	
	//State Strings Slave
	public static final String SLAVE_START_TALKER = "Send his ID";
	public static final String SLAVE_START_LISTENER = "Waiting for MSJ2";
	public static final String SLAVE_WAIT_LIST_TALKER = "Send ACK2";
	public static final String SLAVE_WAIT_LIST_LISTENER = "Waiting for MSJ3, route";
	public static final String SLAVE_WAIT_TAKE_OFF_TALKER = "Send ACK3";
	public static final String SLAVE_WAIT_TAKE_OFF_LISTENER = "Waiting for it's moment for take off";
	
	//State String commons TODO
	public static final String TAKING_OFF_COMMON = "Starting its takeoff ";
	public static final String MOVE_TO_WP_COMMON = "Moving to WP ";
	
	//Fault messages
	public static final String SEND_MISSION_ERROR = "Error sending the mission to the UAV";
	public static final String LANDING_ERROR = "Impossible to make the landing";
	public static final String START_MISSION_ERROR = "Error starting the mission of the UAV";
	public static final String MASTER_SOCKET_READ_ERROR = "Error processing information received";
	public static final String MASTER_SOCKET_ERROR = "Master failure while listening to socket";

}
