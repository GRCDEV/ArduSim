package uavController;

import java.awt.Shape;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_PARAM_TYPE;

import api.pojo.LastPositions;
import api.pojo.UAVCurrentData;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Text;

/** This class includes parameters specifically related to the communication with the flight controller. */

public class UAVParam {

	// Application-SITL connection parameters
	//   TCP
	public static final String MAV_NETWORK_IP = "127.0.0.1";
	public static final int MAV_INITIAL_PORT = 5760; // SITL listening port
	//   Serial port
	public static final String SERIAL_CONTROLLER_NAME = "gnu.io.rxtx.SerialPorts";
	public static final String SERIAL_PORT = "/dev/ttyAMA0";
	public static final int BAUD_RATE = 57600;
	
	// Received information
	public static UAVCurrentData[] uavCurrentData;
	
	// Last n UAV known positions of the UAV
	public static final int LOCATIONS_SIZE = 3;		// n positions (never less than 2
	public static LastPositions[] lastLocations;	// Each UAV has an object with the last received locations sorted
	
	// Startup parameters
	public static double[] initialSpeeds;				// (m/s) Initial UAVs speed
	public static final double MIN_FLYING_ALTITUDE = 5;	// (m) Minimum relative initial flight altitude
	public static final int ALTITUDE_WAIT = 500;		// (ms) Time between checks while take off
	public static final double WIND_THRESHOLD = 0.5;	// Minimum wind speed accepted by the simulator, when used

	public static final int BATTERY_CHARGE = 500000000;	// (mA) Initial battery level
	public static double[] RTLAltitude;					// (m) RTL altitude retrieved from the flight controller
	public static double[] RTLAltitudeFinal;			// (m) Altitude to keep when reach home location when in RTL mode

	// MAVLink waiting parameters
	public static AtomicInteger numMAVLinksOnline = new AtomicInteger();	// Number of MAVLink links stablished
	public static final long MAVLINK_ONLINE_TIMEOUT = 30 * 1000000000l;		// (ns) Global timeout
	public static final int MAVLINK_WAIT = 500;								// (ms) Passive waiting timeout

	// GPS fix waiting parameters
	public static AtomicInteger numGPSFixed = new AtomicInteger();	// Number of UAVs sending valid coordinates
	public static final long GPS_FIX_TIMEOUT = 300 * 1000000000l;	// (ns) Global timeout while waiting GPS coordinates
	public static final int GPS_FIX_WAIT = 500;						// (ms) Passively waiting GPS coordinates

	// Command ACK waiting timeout (ms)
	public static final int COMMAND_WAIT = 200;

	// Stabilization parameters (when the UAV is stopping)
	public static final double STABILIZATION_SPEED = 0.2;				// (m/s) When it is stopped
	public static final int STABILIZATION_WAIT_TIME = 200;				// (ms) Passively waiting the UAV to stop
	public static final long STABILIZATION_TIMEOUT = 30 * 1000000000l;	// (ns) Global timeout while waiting the UAV to stop

	// Filter to compensate the acceleration oscilation, applied when a new location is received from the UAV
	public static final double ACCELERATION_THRESHOLD = 0.2;	// [0, 1] 1=new value, 0=previous value
	public static final double MIN_ACCELERATION = 0.2;			// (m/s²) low pass filter
	public static final double MAX_ACCELERATION = 5;			// (m/s²) high pass filter

	// Period between heartbeats sent to the flight controller (ns)
	public static final long HEARTBEAT_PERIOD =  950000000l;
	
	// Minumum base mode to assert that the UAV is flying
	public static final int MIN_MODE_TO_BE_FLYING = 209;

	// Communications finite state machine. States of the MAVLink protocol
	public static AtomicIntegerArray MAVStatus;
	public static final int MAV_STATUS_OK = 0;
	public static final int MAV_STATUS_REQUEST_MODE = 1;
	public static final int MAV_STATUS_ACK_MODE = 2;
	public static final int MAV_STATUS_ERROR_MODE = 3;
	public static AtomicReferenceArray<UAVParam.Mode> flightMode; // Current flight mode
	public static UAVParam.Mode[] newFlightMode;
	public static final int MAV_STATUS_REQUEST_ARM = 4;
	public static final int MAV_STATUS_ACK_ARM = 5;
	public static final int MAV_STATUS_ERROR_ARM = 6;
	public static final int MAV_STATUS_REQUEST_TAKE_OFF = 7;
	public static final int MAV_STATUS_ACK_TAKE_OFF = 8;
	public static final int MAV_STATUS_ERROR_TAKE_OFF = 9;
	public static double[] takeOffAltitude;
	public static final int MAV_STATUS_SET_SPEED = 10;
	public static final int MAV_STATUS_ACK_SET_SPEED = 11;
	public static final int MAV_STATUS_ERROR_SET_SPEED = 12;
	public static double[] newSpeed;
	public static final int MAV_STATUS_SET_PARAM = 13;
	public static final int MAV_STATUS_ACK_PARAM = 14;
	public static final int MAV_STATUS_ERROR_1_PARAM = 15;
	public static ControllerParam[] newParam;
	public static double[] newParamValue;		// Parameter value to send or received from the UAV controller
	public static final int MAV_STATUS_GET_PARAM = 16;
	public static final int MAV_STATUS_WAIT_FOR_PARAM = 17;
	public static final int MAV_STATUS_ERROR_2_PARAM = 18;
	public static final int MAV_STATUS_SET_CURRENT_WP = 19;
	public static final int MAV_STATUS_ACK_CURRENT_WP = 20;
	public static final int MAV_STATUS_ERROR_CURRENT_WP = 21;
	public static AtomicIntegerArray currentWaypoint;
	public static int[] newCurrentWaypoint;
	public static final int WP_LIST_SIZE = 50;
	public static final int MAX_CURRENT_WP_CHECKS = 50;
	public static final int MAV_STATUS_CLEAR_WP_LIST = 22;
	public static final int MAV_STATUS_ACK_CLEAR_WP_LIST = 23;
	public static final int MAV_STATUS_ERROR_CLEAR_WP_LIST = 24;
	public static final int MAV_STATUS_SEND_WPS = 25;
	public static final int MAV_STATUS_SENDING_WPS = 26;
	public static final int MAV_STATUS_ERROR_SENDING_WPS = 27;
	public static List<Waypoint>[] missionGeoLoaded;	// Missions loaded from file(s), in Geographic coordinates
	public static Waypoint[][] newGeoMission;			// Missions that are about to be sent to the UAV, in Geographic coordinates
	public static List<Waypoint>[] currentGeoMission;	// Missions retrieved from the UAV, in Geographic coordinates
	public static AtomicReferenceArray<List<WaypointSimplified>> missionUTMSimplified; // Missions simplified from the previous one for drawing, in UTM coordinates
	public static List<Shape>[] MissionPx;				// Missions to be drawn on screen, in pixel coordinates
	public static final int MAV_STATUS_REQUEST_WP_LIST = 28;
	public static final int MAV_STATUS_REQUEST_WP0 = 29;
	public static final int MAV_STATUS_REQUEST_WPS = 30;
	public static final int MAV_STATUS_ERROR_REQUEST_WP_LIST = 31;
	public static final int MAV_STATUS_THROTTLE_ON = 32;
	public static final int MAV_STATUS_THROTTLE_ON_ERROR = 33;
	public static int[] stabilizationThrottle;	// By default is 1500 (altitude stabilized)
	public static final int MAV_STATUS_MOVE_UAV = 34;
	public static final int MAV_STATUS_MOVE_UAV_ERROR = 35;
	public static float[][] newLocation;	// [latitude, longitude, relative altitude] where to move the UAV

	// Ardupilot flight modes
	public enum Mode {

		STABILIZE(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				0, Text.STABILIZE),
		STABILIZE_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED, // only 209, not as the rest (217)
				0, Text.STABILIZE_ARMED),
		GUIDED(
				MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				4, Text.GUIDED),
		GUIDED_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				4, Text.GUIDED_ARMED),
		AUTO(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				3, Text.AUTO),
		AUTO_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				3, Text.AUTO_ARMED),
		LOITER(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				5, Text.LOITER),
		LOITER_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				5, Text.LOITER_ARMED),
		RTL(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				6, Text.RTL),
		RTL_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				6, Text.RTL_ARMED),
		CIRCLE(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				7, Text.CIRCLE),
		CIRCLE_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				7, Text.CIRCLE_ARMED),
		POSHOLD(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				16, Text.POSHOLD),
		POSHOLD_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				16, Text.POSHOLD_ARMED),
		BRAKE(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				17, Text.BRAKE),
		BRAKE_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				17, Text.BRAKE_ARMED),
		AVOID_ADSB(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				19, Text.AVOID_ADSB),
		AVOID_ADSB_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				19, Text.AVOID_ADSB_ARMED),
		LAND(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				9, Text.LAND),
		LAND_ARMED(MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				9, Text.LAND_ARMED),
		THROW(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				18, Text.THROW),
		DRIFT(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				11, Text.DRIFT),
		ALT_HOLD(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				2, Text.ALT_HOLD),
		SPORT(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				13, Text.SPORT),
		ACRO(MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED
				+ MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED,
				1, Text.ACRO);

		private final int baseMode;
		private final long customMode;
		private final String modeName;

		private Mode(int baseMode, long customMode, String modeName) {
			this.baseMode = baseMode;
			this.customMode = customMode;
			this.modeName = modeName;
		}

		public int getBaseMode() {
			return baseMode;
		}

		public long getCustomMode() {
			return customMode;
		}
		
		public String getMode() {
			return modeName;
		}

		/**
		 * Return the ardupilot flight mode corresponding to the base and custom values.
		 * <p>If no valid flight mode is found, it returns null. */
		public static UAVParam.Mode getMode(int base, long custom) {
			Mode res = null;
			for (Mode p : Mode.values()) {
				if (p.baseMode == base && p.customMode == custom) {
					res = p;
					break;
				}
			}
			return res;
		}
	}

	// Parameters of the UAV or the simulator
	public enum ControllerParam {
		POSITION_FREQUENCY("SR0_POSITION", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),	// Location message frequency
		BATTERY_CAPACITY("BATT_CAPACITY", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),		// Virtual battery capacity
		WIND_DIRECTION("SIM_WIND_DIR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),		// Wind direction
		WIND_SPEED("SIM_WIND_SPD", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),			// Wind speed
		MAX_THROTTLE("RC3_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),				// Maximum value for throttle
		MIN_THROTTLE("RC3_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),				// Minimum value for throttle
		RTL_ALTITUDE("RTL_ALT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),				// Altitude during return to launch
		RTL_ALTITUDE_FINAL("RTL_ALT_FINAL", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),	// Loiter altitude when not desired to land after return to launch
		CIRCLE_RADIUS("CIRCLE_RADIUS", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32);		// Circle mode radius

		private final String id;
		private final int type;

		private ControllerParam(String id, int type) {
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return id;
		}

		public int getType() {
			return type;
		}
	}
	
}
