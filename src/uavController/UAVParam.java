package uavController;

import java.awt.Shape;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import org.mavlink.messages.MAV_PARAM_TYPE;

import api.pojo.AtomicDoubleArray;
import api.pojo.FlightMode;
import api.pojo.LastLocations;
import api.pojo.Point3D;
import api.pojo.RCValues;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import sim.pojo.IncomingMessage;
import sim.pojo.IncomingMessageQueue;

/** This class includes parameters specifically related to the communication with the flight controller. */

public class UAVParam {

	// ArduSim-flight controller connection parameters
	// TCP (on virtual UAVs)
	public static final int MAX_SITL_INSTANCES = 256;	// Maximum number of SITL instances allowed by ArduCopter implementation
	public static final String MAV_NETWORK_IP = "127.0.0.1";
	public static final int MAX_PORT = 65535;			// Maximum port value
	public static final int MAV_INITIAL_PORT = 5760;	// SITL initial listening port
	public static final int[] MAV_INTERNAL_PORT = new int[] {5501, 5502, 5503};
	public static final int MAV_FINAL_PORT = Math.min(MAV_INITIAL_PORT + 10*(MAX_SITL_INSTANCES-1), MAX_PORT);
	
	public static final int PORT_CHECK_TIMEOUT = 200;	// (ms) Timeout while checking if a port is available
	public static Integer[] mavPort;					// List of ports really used by SITL instances
	// Serial port (on real UAVs)
	public static volatile String serialPort = "/dev/ttyAMA0";
	public static volatile int baudRate = 57600;
	
	// UAV-UAV connection parameters
	// TCP (on real UAVs)
	public static volatile String broadcastIP = "192.168.1.255";// Broadcast IP
	public static volatile int broadcastPort = 14650;			// Broadcast port
	public static DatagramSocket sendSocket;					// Sending socket
	public static DatagramPacket sendPacket;					// Sending packet
	public static DatagramSocket receiveSocket;					// Receiving socket
	public static DatagramPacket receivePacket;					// Receiving packet
	// Range check and receiving virtual buffer (on virtual UAVs)
	public static volatile int distanceCalculusPeriod;						// (ms) Distance calculus between UAVs period
																			// It is the minimum of the range check period and the collision check period when applied
	public static volatile boolean distanceCalculusIsOnline = false;		// Whether the distance calculus service is online or not
	public static AtomicReference<Double>[][] distances;					// (m) Stored distances between UAVs
	public static final int RANGE_CHECK_PERIOD = 1000;						// (ms) Between UAVs range check
	public static AtomicBoolean[][] isInRange;								// Matrix containing the range check result
	public static AtomicReferenceArray<IncomingMessage> prevSentMessage;	// Stores the last sent message for each UAV
	public static boolean pCollisionEnabled = true;							// Whether the packet collision detection is enabled or not
	public static ReentrantLock[] lock;										// Locks for concurrence when detecting  packet collisions
	public static boolean carrierSensingEnabled = true;						// Whether the carrier sensing is enabled or not
	public static IncomingMessageQueue[] mBuffer;							// Array with the message queues used as buffers
	public static final int RECEIVING_BUFFER_PACKET_SIZE = 350;				// (packets) Initial size of the incoming queue q
	public static int receivingBufferSize = 163840;							// (bytes) By default, the Raspberry Pi 3 receiving buffer size
	public static AtomicLongArray maxCompletedTEndTime;						// Array with the later completed transfer finish time for each sending UAV when using collision detection
	public static final int MESSAGE_WAITING_TIME = 1;						// (ms) Waiting time to check if a message arrives
	public static ConcurrentSkipListSet<IncomingMessage>[] vBuffer;			// Array with virtual buffer to calculate packet collisions when using packet collision detection
	public static final int V_BUFFER_SIZE_FACTOR = 3;						// vBuffer is this times the size of mBuffer
	public static int receivingvBufferSize = V_BUFFER_SIZE_FACTOR * receivingBufferSize;	// (bytes) Virtual buffer size
	public static final double BUFFER_FULL_THRESHOLD = 0.8;					// Size of the buffer when it is flushed to avoid it to be filled
	public static int receivingvBufferTrigger = (int)Math.round(BUFFER_FULL_THRESHOLD * receivingvBufferSize);// (bytes) Virtual buffer level when it is flushed to avoid it to be filled
	public static AtomicIntegerArray vBufferUsedSpace;						// (bytes) Array containing the current level of the vBuffer
	public static ConcurrentHashMap<String, String> communicationsClosed;	// Contains the communication threads that have stop to send or receive messages (<=numUAVs)
	public static final int CLOSSING_WAITING_TIME = 5000;					// (ms) Time to wait while the communications are being closed
	// Statistics
	public static int[] sentPacket, packetWaitedPrevSending, packetWaitedMediaAvailable;
	public static AtomicIntegerArray receiverOutOfRange, receiverWasSending, receiverVirtualQueueFull, receiverQueueFull, successfullyReceived;
	public static AtomicIntegerArray receivedPacket, discardedForCollision, successfullyEnqueued, successfullyProcessed;
	
	// UAVs collision detection parameters
	public static volatile boolean collisionCheckEnabled = false;	// Whether the collision check is enabled or not
	public static volatile double collisionCheckPeriod = 0.5;		// (s) Between two checks
	public static int appliedCollisionCheckPeriod;					// The same parameter but in milliseconds
	public static volatile double collisionDistance = 10;			// (m) Distance to assert that a collision has happened (UTM coordinates)
	public static volatile double collisionAltitudeDifference = 20;	// (m) Altitude difference to assert that a collision has happened
	public static volatile double collisionScreenDistance;					// (px) The previous distance, but in screen coordinates
	public static volatile boolean collisionDetected = false; 		// Can be used to stop protocols when a collision happens
	
	// Parameters used to detect when a UAV reaches the last waypoint
	public static final double LAST_WP_THRESHOLD = 1.0; // (m) Maximum distance considered to assert that the UAV has reached the last waypoint
	
	// Received information
	public static UAVCurrentData[] uavCurrentData;
	public static UAVCurrentStatus[] uavCurrentStatus;
	
	// Last n UAV known positions of the UAV
	public static final int LOCATIONS_SIZE = 3;		// n positions (never less than 2)
	public static LastLocations<Point3D>[] lastLocations;	// Each UAV has an object with the last received locations sorted
	
	// Startup parameters
	public static double[] initialSpeeds;				// (m/s) Initial UAVs speed
	public static final double MIN_FLYING_ALTITUDE = 5;	// (m) Minimum relative initial flight altitude
	public static final int ALTITUDE_WAIT = 500;		// (ms) Time between checks while take off
	public static final double WIND_THRESHOLD = 0.5;	// Minimum wind speed accepted by the simulator, when used
	public static double[] RTLAltitude;					// (m) RTL altitude retrieved from the flight controller
	public static double[] RTLAltitudeFinal;			// (m) Altitude to keep when reach home location when in RTL mode
	public static AtomicIntegerArray mavId;				// ID of the multicopter in the MAVLink protocol
	public static final int MAV_ID = 1;					// ID of the multicopter in the MAVLink protocol by default
	public static AtomicIntegerArray gcsId;				// ID of the GCS authorized to send commands to the flight controller
														//   255 for Mission Planner and DroidPlanner
														//   252 for APM Planner 2
	public static AtomicIntegerArray RCmapRoll;			// Channel the roll is mapped to
	public static AtomicIntegerArray RCmapPitch;		// Channel the pitch is mapped to
	public static AtomicIntegerArray RCmapThrottle;		// Channel the throttle is mapped to
	public static AtomicIntegerArray RCmapYaw;			// Channel the yaw is mapped to
	public static AtomicIntegerArray[] RCminValue;		// Minimum value of all the 8 channels
	public static AtomicIntegerArray[] RCtrimValue;		// Trim value of all the 8 channels
	public static AtomicIntegerArray[] RCmaxValue;		// Maximum value of all the 8 channels
	public static AtomicIntegerArray[] flightModeMap;	// Mapping of the 6 flight modes used in the remote control switch
	public static int[][] customModeToFlightModeMap;	// Mapping from real custom flight mode to FLTMODEx (provides x)
														//   WARNING: not all modes are mapped in the remote control. Unmapped modes return -1

	// Battery levels
	public static final double CHARGED_VOLTAGE = 4.2;				// (V) Cell voltage at maximum charge
	public static final double NOMINAL_VOLTAGE = 3.7;				// (V) Nominal cell voltage of a cell
	public static final double ALARM_VOLTAGE = 3.5;				// (V) 20% charge. Minimum cell voltage under load (already flying)
	public static final double FINAL_VOLTAGE = 3.75;				// (V) 20% charge. Minimum cell voltage without load (just when finished the flight)
	public static final double FULLY_DISCHARGED_VOLTAGE = 3.2;		// (V) 0% charge. Minimum cell voltage without load
	public static final double STORAGE_VOLTAGE = 3.8;				// (V) Cell voltage for storage purposes
	public static final double BATTERY_DEPLETED_THRESHOLD = 0.2;	// (%) Battery energy level to rise the alarm
	public static final int BATTERY_DEPLETED_ACTION = 1;	// 0 Disabled
															// 1 Land (RTL if flying in auto mode)
															// 2 RTL
	public static final int BATTERY_PRINT_PERIOD = 5;	// (messages) Number of battery checks between output messages
	// Real battery
	public static volatile int lipoBatteryCells = 4;			// Number of cells of the LiPo battery
	public static volatile double lipoBatteryChargedVoltage = lipoBatteryCells * CHARGED_VOLTAGE;	// (V) Maximum charge (16.8V)
	public static volatile double lipoBatteryNominalVoltage = lipoBatteryCells * NOMINAL_VOLTAGE;	// (V) Standard voltage (14.8V)
	public static volatile double lipoBatteryAlarmVoltage = lipoBatteryCells * ALARM_VOLTAGE;		// (V) Voltage on flight to rise an alarm (14V)
	public static volatile double lipoBatteryFinalVoltage = lipoBatteryCells * FINAL_VOLTAGE;		// (V) Voltage at the end without load (15V)
	public static volatile double lipoBatteryDischargedVoltage = lipoBatteryCells * FULLY_DISCHARGED_VOLTAGE;	// (V) Minimum to avoid damage (12.8V)
	public static volatile double lipoBatteryStorageVoltage = lipoBatteryCells * STORAGE_VOLTAGE;	// (V) Level for storage (15.2V)
	public static volatile int lipoBatteryCapacity = 3300;	// (mAh) Standard battery capacity
	// Virtual battery
	public static final int VIRTUAL_BATTERY_CELLS = 3;		// Number of cells of the virtual battery on ArduSim
	public static final double VIRT_BATTERY_CHARGED_VOLTAGE = VIRTUAL_BATTERY_CELLS * CHARGED_VOLTAGE;	// (V) Maximum charge (12.6V)
	public static final double VIRT_BATTERY_NOMINAL_VOLTAGE = VIRTUAL_BATTERY_CELLS * NOMINAL_VOLTAGE;	// (V) Standard voltage (11.1V)
	public static final double VIRT_BATTERY_ALARM_VOLTAGE = VIRTUAL_BATTERY_CELLS * ALARM_VOLTAGE;		// (V) Voltage on flight to rise an alarm (10.5V)
	public static final double VIRT_BATTERY_FINAL_VOLTAGE = VIRTUAL_BATTERY_CELLS * FINAL_VOLTAGE;		// (V) Voltage at the end without load (11.25V)
	public static final double VIRT_BATTERY_DISCHARGED_VOLTAGE = VIRTUAL_BATTERY_CELLS * FULLY_DISCHARGED_VOLTAGE;	// (V) Minimum to avoid damage (9.6V)
	public static final double VIRT_BATTERY_STORAGE_VOLTAGE = VIRTUAL_BATTERY_CELLS * STORAGE_VOLTAGE;	// (V) Level for storage (11.4V)
	public static final int VIRT_BATTERY_MAX_CAPACITY = 500000000;	// (mAh) Maximum initial battery capacity
	// Real or virtual battery
	public static int batteryCapacity;	// (mAh) Used battery capacity
	public static int batteryLowLevel;	// (mAh) Calculated battery energy level to rise the alarm

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

	// Filter to compensate the acceleration oscillation, applied when a new location is received from the UAV
	public static final double ACCELERATION_THRESHOLD = 0.2;	// [0, 1] 1=new value, 0=previous value
	public static final double MIN_ACCELERATION = 0.2;			// (m/s²) low pass filter
	public static final double MAX_ACCELERATION = 5;			// (m/s²) high pass filter

	// Period between heartbeats sent to the flight controller (ns)
	public static final long HEARTBEAT_PERIOD =  950000000l;
	
	// Minumum base mode to assert that the UAV is flying
	public static final int MIN_MODE_TO_BE_FLYING = 209;
	
	// Flight mode change timeout (since the command was accepted by the flight controller until that change is received through a heartbeat
	public static final int MODE_CHANGE_TIMEOUT = 5000;	// (ms)
	
	// Auxiliary variable needed to ensure that the message thrown when the UAV gets to the end is shown only once
	public static boolean[] lastWaypointReached;
	
	// RC Channels message sent values
	public static AtomicReference<RCValues>[] rcs;

	// Communications finite state machine. States of the MAVLink protocol
	public static AtomicIntegerArray MAVStatus;
	public static final int MAV_STATUS_OK = 0;
	public static final int MAV_STATUS_REQUEST_MODE = 1;
	public static final int MAV_STATUS_ACK_MODE = 2;
	public static final int MAV_STATUS_ERROR_MODE = 3;
	public static AtomicReferenceArray<FlightMode> flightMode; // Current flight mode
	public static FlightMode[] newFlightMode;
	public static volatile boolean flightStarted = false;	// Detects when at least one UAV has started to fly
	public static final int MAV_STATUS_REQUEST_ARM = 4;
	public static final int MAV_STATUS_ACK_ARM = 5;
	public static final int MAV_STATUS_ERROR_ARM = 6;
	public static final int MAV_STATUS_REQUEST_TAKE_OFF = 7;
	public static final int MAV_STATUS_ACK_TAKE_OFF = 8;
	public static final int MAV_STATUS_ERROR_TAKE_OFF = 9;
	public static AtomicDoubleArray takeOffAltitude;
	public static final int MAV_STATUS_SET_SPEED = 10;
	public static final int MAV_STATUS_ACK_SET_SPEED = 11;
	public static final int MAV_STATUS_ERROR_SET_SPEED = 12;
	public static double[] newSpeed;
	public static final int MAV_STATUS_SET_PARAM = 13;
	public static final int MAV_STATUS_ACK_PARAM = 14;
	public static final int MAV_STATUS_ERROR_1_PARAM = 15;
	public static ControllerParam[] newParam;
	public static AtomicDoubleArray newParamValue;		// Parameter value to send or received from the UAV controller
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
	public static final int MAV_STATUS_RECOVER_CONTROL = 34;
	public static final int MAV_STATUS_RECOVER_ERROR = 35;
	public static AtomicIntegerArray overrideOn;	// ([0,1]) 1 = RC Channels can be overridden
	
	public static final int MAV_STATUS_MOVE_UAV = 36;
	public static final int MAV_STATUS_ACK_MOVE_UAV = 37;
	public static final int MAV_STATUS_MOVE_UAV_ERROR = 38;
	public static float[][] newLocation;	// [latitude, longitude, relative altitude] where to move the UAV
	
	// Potentiometer levels for the six flight modes configurable in the remote control (min, used, max)
	public static final int[][] RC5_MODE_LEVEL = new int[][] {{0, 1000, 1230}, {1231, 1295, 1360},
		{1361, 1425, 1490}, {1491, 1555, 1620}, {1621, 1685, 1749}, {1750, 1875, 2000}};
	
	// Parameters of the UAV or the simulator
	public enum ControllerParam {
		LOGGING("LOG_BITMASK", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),				// Mask of logs enabled
		// Information requested to the flight controller
		POSITION_FREQUENCY("SR0_POSITION", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),	// Location message frequency
		STATISTICS("SR0_EXT_STAT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),				// Statistics including CPU load, and battery remaining
		// Battery configuration
		BATTERY_CAPACITY("BATT_CAPACITY", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),		// Virtual battery capacity
		BATTERY_MONITOR("BATT_MONITOR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),		// Enable battery voltage and current monitoring
																					// 0 Disabled, 3 voltage, 4 voltage and current, (5,6,7 others)
		BATTERY_VOLTAGE_PIN("BATT_VOLT_PIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Pin used to measure voltage (don't touch)
		BATTERY_VOLTAGE_MULTIPLIER("BATT_VOLT_MULT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Related to voltage measurement (don't touch)
		BATTERY_OFFSET("BATT_AMP_OFFSET", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Voltage offset at 0 current on sensor (don't touch)
		BATTERY_CURRENT_PIN("BATT_CURR_PIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Pin used to measure current (don't touch)
		BATTERY_DENSITY("BATT_AMP_PERVOLT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Amperes included in 1 volt (don't touch)
		
		BATTERY_CAPACITY2("BATT2_CAPACITY", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),	// Second battery parameters
		
		// Battery failsafe options
		BATTERY_FAILSAFE_ACTION("FS_BATT_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),		// Action when battery low
																							// 0 Disabled, 1 Land, 2 RTL
		BATTERY_VOLTAGE_DEPLETION_THRESHOLD("FS_BATT_VOLTAGE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),// (V) Lower limit to do a RTL (0 disabled)
		BATTERY_CURRENT_DEPLETION_THRESHOLD("FS_BATT_MAH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// (mAh) Remaining to trigger failsafe (50 mAh increment, 0 disabled)
		// Wind options
		WIND_DIRECTION("SIM_WIND_DIR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),		// Wind direction
		WIND_SPEED("SIM_WIND_SPD", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),			// Wind speed
		// Other failsafe types options
		GCS_LOST_FAILSAFE("FS_GCS_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),		// Failsafe when connection lost with RC_OVERRIDE in use
																					// 0 Disabled, 1 RTL, 2 continue if auto mode during mission
		REMOTE_LOST_FAILSAFE("FS_THR_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),	// Failsafe when connection lost with remote
																					// 0 Disabled, 1 RTL, 2 continue if auto mode during mission, 3 LAND
		REMOTE_LOST_FAILSAFE_THROTTLE_VALUE("FS_THR_VALUE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),	// Throttle value adopted to failsafe when connection lost with remote
		CRASH_FAILSAFE("FS_CRASH_CHECK", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),		// Failsafe to disarm motors (value=1) if a crash is detected
		GPS_FAILSAFE_ACTION("FS_EKF_ACTION", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),	// Action when GPS position is lost (EKF error)
																					// 1 Land, 2 AltHold, 3 Land even in Stabilize mode
		GPS_FAILSAFE_THRESHOLD("FS_EKF_THRESH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),	// Precision to assert EKF error
																						// 0.6 Strict, 0.8 Default, 1.0 Relaxed
		// Specific parameters requested to the flight controller
		SINGLE_GCS("SYSID_ENFORCE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),			// Whether only one system id is accepted for commands or not
		GCS_ID("SYSID_MYGCS", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// Identifier of the GCS allowed to send commands
		RCMAP_ROLL("RCMAP_ROLL", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),				// RC channel used for roll
		RCMAP_PITCH("RCMAP_PITCH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),				// RC channel used for pitch
		RCMAP_THROTTLE("RCMAP_THROTTLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),		// RC channel used for throttle
		DZ_THROTTLE("THR_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Dead zone above and below mid throttle for speed control in AltHold, Loiter or PosHold flight modes
		RCMAP_YAW("RCMAP_YAW", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// RC channel used for yaw
		MAX_RC1("RC1_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC1 (typically, roll)
		TRIM_RC1("RC1_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC1 (typically, roll)
		MIN_RC1("RC1_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC1 (typically, roll)
		DZ_RC1("RC1_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom (typically, roll)
		MAX_RC2("RC2_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC2 (typically, pitch)
		TRIM_RC2("RC2_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC2 (typically, pitch)
		MIN_RC2("RC2_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC2 (typically, pitch)
		DZ_RC2("RC2_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom (typically, pitch)
		MAX_RC3("RC3_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC3 (typically, throttle)
		TRIM_RC3("RC3_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC3 (typically, throttle)
		MIN_RC3("RC3_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC3 (typically, throttle)
		DZ_RC3("RC3_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom (typically, throttle)
		MAX_RC4("RC4_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC4 (typically, yaw)
		TRIM_RC4("RC4_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC4 (typically, yaw)
		MIN_RC4("RC4_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC4 (typically, yaw)
		DZ_RC4("RC4_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom (typically, yaw)
		MAX_RC5("RC5_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC5 (always, flight mode)
		TRIM_RC5("RC5_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC5 (always, flight mode)
		MIN_RC5("RC5_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC5 (always, flight mode)
		DZ_RC5("RC5_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom (always, flight mode)
		MAX_RC6("RC6_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC6
		TRIM_RC6("RC6_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC6
		MIN_RC6("RC6_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC6
		DZ_RC6("RC6_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom
		MAX_RC7("RC7_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC7
		TRIM_RC7("RC7_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC7
		MIN_RC7("RC7_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC7
		DZ_RC7("RC7_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom
		MAX_RC8("RC8_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Maximum value for RC8
		TRIM_RC8("RC8_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Trim value for RC8
		MIN_RC8("RC8_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),					// (us) Minimum value for RC8
		DZ_RC8("RC8_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),						// (us) Dead zone around trim or bottom
		FLTMODE1("FLTMODE1", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 1 (x<=1230)
		FLTMODE2("FLTMODE2", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 2 (1230<x<=1360)
		FLTMODE3("FLTMODE3", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 3 (1360<x<=1490)
		FLTMODE4("FLTMODE4", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 4 (1490<x<=1620)
		FLTMODE5("FLTMODE5", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 5 (1620<x<=1749)
		FLTMODE6("FLTMODE6", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),					// Flight custom mode for RC level 6 (x>1749)
		RTL_ALTITUDE("RTL_ALT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),				// Altitude during return to launch
		RTL_ALTITUDE_FINAL("RTL_ALT_FINAL", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),	// Loiter altitude when not desired to land after return to launch
		// Others
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
