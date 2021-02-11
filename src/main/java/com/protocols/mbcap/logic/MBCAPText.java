package com.protocols.mbcap.logic;

/** This class contains messages used by MBCAP protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPText {
	
	// Application name
	public static final String APP_NAME = "ArduSim";

	// Configuration dialog window:
	public static final String OK = "OK";
	public static final String RESTORE_DEFAULTS = "Restore defaults";
	public static final String CONFIGURATION = "configuration";
	
	public static final String SIMULATION_PARAMETERS = "Simulation parameters:";
	public static final String MISSIONS_SELECTION = "UAV main.java.com.protocols.mission file(s):";
	public static final String BUTTON_SELECT = "...";
	public static final String UAV_NUMBER = "Number of UAVs:";
	
	public static final String SITUATIONS_SOLVED = "Number of collisions avoided:";
	public static final String DEADLOCKS = "Number of deadlocks solved:";
	public static final String DEADLOCKS_FAILED = "Number of deadlocks failed:";
	public static final String BEACONING_PARAM = "Beaconing parameters:";
	public static final String BEACON_INTERVAL = "Beacon interval:";
	public static final String BEACON_REFRESH = "Beacon renewal rate:";
	public static final String INTERSAMPLE = "Intersample time:";
	public static final String MIN_ADV_SPEED = "Minimum advertisement speed:";
	public static final String BEACON_EXPIRATION = "Beacon expiration time:";
	public static final String AVOID_PARAM = "Collision avoidance protocol parameters:";
	public static final String WARN_DISTANCE = "Collision warning distance:";
	public static final String WARN_ALTITUDE = "Collision warning altitude offset:";
	public static final String WARN_TIME = "Collision warning time offset:";
	public static final String CHECK_PERIOD = "Risk check period:";
	public static final String PACKET_LOSS_THRESHOLD = "Maximum number of expected consecutive packets lost:";
	public static final String GPS_ERROR = "GPS expected error:";
	public static final String HOVERING_TIMEOUT = "Hovering timeout:";
	public static final String OVERTAKE_TIMEOUT = "Overtake delay timeout:";
	public static final String RESUME_MODE_DELAY = "Default flight mode resume delay:";
	public static final String RECHECK_DELAY = "Check again risk with the same UAV delay:";
	public static final String DEADLOCK_TIMEOUT = "Deadlock base timeout:";
	// Error messages:
	public static final String VALIDATION_WARNING = "Validation warning";
	public static final String BEACON_PERIOD_ERROR = "The time between beacons must be a valid positive integer.";
	public static final String BEACON_REFRESH_ERROR = "The number of beacons repetitions must be a valid positive integer.";
	public static final String HOP_TIME_ERROR = "The time between predicted points must be a valid positive number.";
	public static final String MIN_SPEED_ERROR = "The minimal speed must be a valid positive number.";
	public static final String BEACON_EXPIRATION_ERROR = "The beacon expiration time must be a valid positive number.";
	public static final String WARN_DISTANCE_ERROR_1 = "The distance between UAVs must be a valid positive number.";
	public static final String WARN_DISTANCE_ERROR_2 = "The distance between UAVs must be greater by one than the distance defined to detect collision.";
	public static final String WARN_ALTITUDE_ERROR_1 = "The altitude difference must be a valid positive number.";
	public static final String WARN_ALTITUDE_ERROR_2 = "The altitude difference must be greater than the distance defined to detect collision.";
	public static final String WARN_TIME_ERROR = "The time difference must be a valid positive number.";
	public static final String CHECK_PERIOD_ERROR = "The collision risk check period must be a valid positive number.";
	public static final String PACKET_LOSS_ERROR = "The number of consecutive data packets that can be lost during transmission must be a valid positive integer.";
	public static final String GPS_ERROR_ERROR = "The GPS distance error value must be a valid positive number.";
	public static final String HOVERING_TIMEOUT_ERROR = "The stand still waiting time must be a valid positive number.";
	public static final String OVERTAKE_TIMEOUT_ERROR = "The waiting time to check overtaking must be a valid positive number.";
	public static final String RESUME_MODE_DELAY_ERROR = "The minimum time to listen to a waiting UAV must be a valid positive number.";
	public static final String RECHECK_DELAY_ERROR = "The minimum time before checking again if there is collision risk with the same UAV must be a valid positive number.";
	public static final String DEADLOCK_TIMEOUT_ERROR_1 = "The deadlock timeout must be a valid positive number.";
	public static final String DEADLOCK_TIMEOUT_ERROR_2 = "The deadlock timeout must be greater than the beacon expiration time.";

	// Progress shown in the log or the interaction panel
	public static final String ENABLING = "Collision avoidance protocol enabled...";
	public static final String COLLISION_RISK_DETECTED = "Collision avoidance protocol in progress...";

	// Progress dialog
	public static final String CAP = "CAP";

	// General errors
	public static final String UAVS_START_ERROR_1 = "Failed locating the home position of the UAV";
	public static final String UAVS_START_ERROR_2 = "No valid coordinates could be found to stablish the home of the UAV";
	public static final String WARN_IMAGE_LOAD_ERROR = "The collision warning image could not be loaded.";
	public static final String WAYPOINT_LOST = "Current waypoint not found.";
	public static final String REPOSITION_ERROR_1 = "No information enough to decide where to step aside.";
	
	// Units
	public static final String MILLISECONDS = "ms";
	public static final String METERS = "m";
	public static final String SECONDS = "s";
	public static final String METERS_PER_SECOND = "m/s";
	
	// Protocol messages
	public static final String PROT_TIMED = "Protocol timeout.";
	public static final String MISSION_RESUME = "Mission resumed.";
	public static final String MISSION_RESUME_ERROR = "Error resuming the main.java.com.protocols.mission.";
	public static final String DEADLOCK = "Landing due to interlock situation with another UAV.";
	public static final String DEADLOCK_ERROR = "It was not possible to land after an interlock situation.";
	public static final String LOCATION_ERROR = "Unable to land after failing getting the current location of the UAV.";
	public static final String PROT_ERROR = "Protocol error";
	public static final String RISK_DETECTED = "Stop. Collision risk detected with UAV";
	public static final String RISK_DETECTED_ERROR = "Error stopping the UAV.";
	public static final String RESUMING_MISSION = "Going on permission granted by UAV";
	public static final String RESUMING_MISSION_ERROR = "Error resuming the main.java.com.protocols.mission.";
	public static final String MOVING = "Moving to a safe place";
	public static final String MOVING_ERROR = "Error trying to move.";
	public static final String MOVED = "Moved to a safe place.";
	public static final String GRANT_PERMISSION = "Granting permission to go on to UAV";
	public static final String MISSION_RESUMED = "Mission resumed. Collision risk avoided with UAV";
	public static final String SAFE_PLACE = "Safe place reached.";

	// Protocol name
	public static final String MBCAP_TEXT = "MBCAP";

	// Flying modes
	public static final String STATE_NORMAL = "Normal";
	public static final String STATE_STAND_STILL = "Stand still";
	public static final String STATE_MOVING_ASIDE = "Moving aside";
	public static final String STATE_GO_ON_PLEASE = "Go on, please";
	public static final String STATE_OVERTAKING = "Overtaking";
	public static final String STATE_EMERGENCY_LAND = "Emergency land";

	// Logs stored
	public static final String UAV_ID = "UAV";
	public static final String BEACONS_SUFIX = "beacons.csv";
	public static final String MAX_ERROR_LINES_SUFIX = "max_error_per_beacon.scr";
	public static final String BEACON_TOTAL_ERROR_SUFIX = "error_per_beacon.csv";
	public static final String BEACON_POINT_ERROR_SUFIX = "error_per_time.csv";
	
	// PC Companion
	public static final String ID = "id";
	public static final String EVENT = "event";
	public static final String FLIGHT_MODE = "flight mode";
	public static final String ID_AVOIDING = "id avoiding";
	public static final String SPEED = "speed";
	public static final String X = "x";
	public static final String Y = "y";
	public static final String Z = "z";
	
	// General errors
	public static final String START_MISSION_ERROR = "Error starting the main.java.com.protocols.mission of the UAVs";
	public static final String MOVING_ERROR_2 = "Can't stabilize position in destination position.";
	

}
