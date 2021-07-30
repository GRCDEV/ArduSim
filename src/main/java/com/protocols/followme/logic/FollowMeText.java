package com.protocols.followme.logic;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

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
	public static final String DISTANCE_TEXT = "Min. distance between UAVs:";
	public static final String DISTANCE_TEXT_ERROR = "The min. distance between UAVs must be a positive double";
	public static final String TAKEOFF_STRATEGY = "Take off strategy";
	public static final String AIR_TEXT = "Flying formation:";
	public static final String INITIAL_RELATIVE_ALTITUDE = "Initial relative altitude:";
	public static final String INITIAL_RELATIVE_ALTITUDE_ERROR = "The take off altitude must be a positive double";
	public static final String LANDING_TEXT = "Landing formation:";
	public static final String SIMULATED_DATA = "Simulated flight data:";
	public static final String SIMULATED_DATA_DIALOG_TITLE = "Select the .txt file with the real RC values";
	public static final String DATA_TXT_FILE = "Data txt file";
	public static final String DATA_TXT_EXTENSION = "txt";
	public static final String SIMULATED_DATA_ERROR = "Invalid file contents.";
	public static final String MASTER_UAV_SPEED = "Master UAV speed:";
	public static final String MASTER_UAV_SPEED_ERROR = "The master UAV speed must be a positive integer";
	public static final String COMM_PERIOD = "Master location advise period:";
	public static final String BAD_INPUT = "Please, check the selected information.";
	
	// Protocol states
	public static final String START = "Start";							// Master UAV detects slaves
	public static final String TAKE_OFF = "Take off";						// Safe take off of all the UAVs
	public static final String SETUP_FINISHED = "Setup finished";		// Waiting for the experiment to start
	public static final String FOLLOWING = "Following";					// Master UAV manually controlled, and slaves following master UAV
	public static final String MOVE_TO_LAND = "Move to land";			// Moving to the final land location
	public static final String LANDING = "Landing";						// UAV landing
	public static final String FINISH = "Finished";						// The flight is over
	
	// General messages
	public static final String ENABLING = "Enabling " + PROTOCOL_TEXT + " protocol...";

	public static final String MASTER_DETECTED_UAVS = "Number of UAVs detected by master:";
	public static final String MASTER_WAIT_ALTITUDE = "Master talker waiting to reach altitude before sending location.";
	public static final String MASTER_SEND_LAND = "Master talker sending land order.";
	//   from slave
	public static final String SLAVE_START_LISTENER = "Slave listener detecting master.";
	public static final String SLAVE_WAIT_ORDER_LISTENER = "Slave listener waiting move to or land order.";
	//   from master or slave
	public static final String LISTENER_WAITING = "Listener waiting.";
	public static final String LISTENER_FINISHED = "Listener finished.";
	public static final String TALKER_FINISHED = "Talker finished.";
	
	// Errors
	public static final String MOVE_ERROR = "Unable to move to target location. UAV";
	public static final String MASTER_LOITER_ERROR = "Unable to change to loiter";
	public static final String LAND_ERROR = "Unable to land the UAV";
	
	// Thread names
	public static final String LISTENER_THREAD = "FollowMe listener: ";
	public static final String TALKER_THREAD = "Followme talker: ";
	
}
