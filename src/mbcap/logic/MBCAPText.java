package mbcap.logic;

/** This class contains messages used by MBCAP protocol. */

public class MBCAPText {

	// Configuration dialog window:
	public static final String CANCEL = "Cancel";
	public static final String CONFIGURATION = "configuration";
	public static final String BEACONING_PARAM = "Beaconing parameters:";
	public static final String BEACON_INTERVAL = "Beacon interval:";
	public static final String BEACON_REFRESH = "Beacon renewal rate:";
	public static final String BEACON_EXPIRATION = "Beacon expiration time:";
	public static final String TIME_WINDOW = "Look ahead time window:";
	public static final String INTERSAMPLE = "Intersample time:";
	public static final String MIN_ADV_SPEED = "Minimum advertisement speed:";
	public static final String AVOID_PARAM = "Collision avoidance protocol parameters:";
	public static final String WARN_DISTANCE = "Collision warning distance:";
	public static final String WARN_ALTITUDE = "Collision warning altitude offset:";
	public static final String WARN_TIME = "Collision warning time offset:";
	public static final String CHECK_THRESHOLD = "Protocol threshold:";
	public static final String CHECK_PERIOD = "Risk check period:";
	public static final String SAFE_DISTANCE = "Minimum safety distance:";
	public static final String HOVERING_TIMEOUT = "Hovering timeout:";
	public static final String OVERTAKE_TIMEOUT = "Overtake delay timeout:";
	public static final String RESUME_MODE_DELAY = "Default flight mode resume delay:";
	public static final String DEADLOCK_TIMEOUT = "Deadlock base timeout:";
	// Error messages:
	public static final String BEACON_PERIOD_ERROR = "The time between beacons must be a valid positive integer.";
	public static final String BEACON_REFRESH_ERROR = "The number of beacons repetitions must be a valid positive integer.";
	public static final String BEACON_EXPIRATION_ERROR = "The beacon expiration time must be a valid positive number.";
	public static final String HOP_TIME_ERROR = "The time between predicted points must be a valid positive number.";
	public static final String FLYING_TIME_ERROR_1 = "The included flying time must be a valid positive number.";
	public static final String FLYING_TIME_ERROR_2 = "The included flying time depends on the time\nbetween points and must be les than";
	public static final String FLYING_TIME_ERROR_3 = "The included flying time must be multiple of the time between predicted points.";
	public static final String MIN_SPEED_ERROR = "The minimal speed must be a valid positive number.";
	public static final String WARN_DISTANCE_ERROR_1 = "The distance between UAVs must be a valid positive number.";
	public static final String WARN_DISTANCE_ERROR_2 = "The distance between UAVs must be greater than the distance defined to detect collision.";
	public static final String WARN_ALTITUDE_ERROR_1 = "The altitude difference must be a valid positive number.";
	public static final String WARN_ALTITUDE_ERROR_2 = "The altitude difference must be greater than the distance defined to detect collision.";
	public static final String WARN_TIME_ERROR = "The time difference must be a valid positive number.";
	public static final String CHECK_THRESHOLD_ERROR_1 = "The maximum distance to react to a risk must be a valid positive number.";
	public static final String CHECK_THRESHOLD_ERROR_2 = "The maximum distance to react to a risk must be greater than the distance to the risk.";
	public static final String CHECK_PERIOD_ERROR = "The collision risk check period must be a valid positive number.";
	public static final String SAFE_DISTANCE_ERROR_1 = "The distance between the incoming UAV and safe place must be a valid positive number.";
	public static final String SAFE_DISTANCE_ERROR_2 = "The distance to a safe place must be greater than the distance to assert a collision.";
	public static final String HOVERING_TIMEOUT_ERROR = "The stand still waiting time must be a valid positive number.";
	public static final String OVERTAKE_TIMEOUT_ERROR = "The waiting time to check overtaking must be a valid positive number.";
	public static final String RESUME_MODE_DELAY_ERROR = "The minimum time to listen to a waiting UAV must be a valid positive number.";
	public static final String DEADLOCK_TIMEOUT_ERROR_1 = "The deadlock timeout must be a valid positive number.";
	public static final String DEADLOCK_TIMEOUT_ERROR_2 = "The deadlock timeout must be greater than the beacon expiration time.";

	// Progress shown in the log or the interaction panel
	public static final String ENABLING = "Collision avoidance protocol enabled...";
	public static final String COLLISION_DETECTED = "Collision detected!";
	public static final String COLLISION_RISK_DETECTED = "Collision avoidance protocol in progress...";

	// Progress dialog
	public static final String CAP = "CAP";

	// General errors
	public static final String WARN_IMAGE_LOAD_ERROR = "The collision warning image could not be loaded.";
	public static final String ERROR_BEACON = "Error sending a beacon.";
	public static final String COLLISION_DETECTED_ERROR_1 = "Emergency landing due to the collision between the UAVs";
	public static final String COLLISION_DETECTED_ERROR_2 = "It is suggested to close the application.\nCollision detected between the UAVs";
	public static final String COLLISION_TITLE = "Collision detected";
	public static final String WAYPOINT_LOST = "Current waypoint not found.";
	public static final String REPOSITION_ERROR_1 = "No information enough to decide where to step aside.";
	public static final String REPOSITION_ERROR_2 = "The UTM projection zone has not been already defined.";
	
	// Protocol messages
	public static final String PROT_TIMED = "Protocol timeout.";
	public static final String MISSION_RESUME = "Mission resumed.";
	public static final String MISSION_RESUME_ERROR = "Error resuming the mission.";
	public static final String DEADLOCK = "Landing due to interlock situation with another UAV.";
	public static final String DEADLOCK_ERROR = "It was not possible to land after an interlock situation.";
	public static final String PROT_ERROR = "Protocol error";
	public static final String RISK_DETECTED = "Stop. Collision risk detected with UAV";
	public static final String RISK_DETECTED_ERROR = "Error stopping the UAV.";
	public static final String RESUMING_MISSION = "Going on permission granted by UAV";
	public static final String RESUMING_MISSION_ERROR = "Error resuming the mission.";
	public static final String MOVING = "Moving to a safe place";
	public static final String MOVING_ERROR = "Error trying to move.";
	public static final String MOVED = "Moved to a safe place.";
	public static final String GRANT_PERMISSION = "Granting permission to go on to UAV";
	public static final String MISSION_RESUMED = "Mission resumed. Collision risk avoided with UAV";
	public static final String SAFE_PLACE = "Safe place reached.";
	public static final String WAYPOINT_REACHED = "Last waypoint reached.";
	public static final String LANDING = "Landing due to protocol error.";
	public static final String LANDING_ERROR = "It was not possible to land.";

	// Protocol versions
	public static final String MBCAP_V1 = "MBCAP v1";
	public static final String MBCAP_V2 = "MBCAP v2";
	public static final String MBCAP_V3 = "MBCAP v3";
	public static final String MBCAP_V4 = "MBCAP v4";

	// Flying modes
	public static final String STATE_NORMAL = "Normal";
	public static final String STATE_STAND_STILL = "Stand still";
	public static final String STATE_MOVING_ASIDE = "Moving aside";
	public static final String STATE_GO_ON_PLEASE = "Go on, please";
	public static final String STATE_OVERTAKING = "Overtaking";
	public static final String STATE_EMERGENCY_LAND = "Emergency land";

	// Logs stored
	public static final String BEACONS_SUFIX = "beacons.csv";
	public static final String MAX_ERROR_LINES_SUFIX = "max_error_per_beacon.scr";
	public static final String BEACON_TOTAL_ERROR_SUFIX = "error_per_beacon.csv";
	public static final String BEACON_POINT_ERROR_SUFIX = "error_per_time.csv";

}
