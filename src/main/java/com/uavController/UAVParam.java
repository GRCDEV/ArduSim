package com.uavController;

import com.uavController.atomicDoubleArray.AtomicDoubleArray;
import com.api.copter.CopterParam;
import com.api.pojo.*;
import com.api.pojo.location.Waypoint;
import com.api.pojo.location.WaypointSimplified;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DGeo;
import io.dronefleet.mavlink.annotations.MavlinkMessageInfo;
import io.dronefleet.mavlink.common.GlobalPositionInt;
import com.api.copter.CopterParamLoaded;
import com.api.swarm.formations.Formation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.*;

/** This class includes parameters specifically related to the communication with the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class UAVParam {

	// ArduSim-flight controller connection parameters
	// TCP (on virtual UAVs)
	//TODO check max sitl instances
	public static final int MAX_SITL_INSTANCES = 1001;	// Maximum number of SITL instances allowed (only limited by hardware capabilities)
	public static final String MAV_NETWORK_IP = "127.0.0.1";
	public static final int MAX_PORT = 65535;			// Maximum port value
	public static final int MAV_INITIAL_PORT = 5760;	// SITL initial listening port
	public static final int[] MAV_INTERNAL_PORT = new int[] {5501, 5502, 5503, 9005};
	public static final int[] MAV_PORT_OFFSET = new int[] {MAV_INTERNAL_PORT[0] - MAV_INITIAL_PORT,
			MAV_INTERNAL_PORT[1] - MAV_INITIAL_PORT,MAV_INTERNAL_PORT[2] - MAV_INITIAL_PORT,
			MAV_INTERNAL_PORT[3] - MAV_INITIAL_PORT};
	
	public static final int PORT_CHECK_TIMEOUT = 200;	// (ms) Timeout while checking if a port is available
	public static Integer[] mavPort;					// List of ports really used by SITL instances
	// Serial port (on real UAVs)
	public static volatile String serialPort = "/dev/ttyAMA0";
	public static volatile int baudRate = 57600;

	public static boolean usingOmnetpp;

	// UAV-UAV TCP connection parameters (on real UAVs)
	public static volatile String broadcastIP = "192.168.1.255";// Broadcast IP
	public static volatile int broadcastPort = 14650;			// Broadcast port
	public static volatile int internalBroadcastPort = 14750;	// Broadcast for internal communication (e.g. communication outside of the protocol not by user)
	
	// UAVs collision detection parameters
	public static volatile long distanceCalculusPeriod;						// (ms) Distance calculus between UAVs period
																			// It is the minimum of the range check period and the collision check period when applied
	public static volatile boolean distanceCalculusIsOnline = false;		// Whether the distance calculus service is online or not
	public static AtomicReference<Double>[][] distances;					// (m) Stored distances between UAVs
	public static volatile boolean collisionCheckEnabled = false;	// Whether the collision check is enabled or not
	public static volatile boolean stopAtCollision;					// stop the experiment if collision detected. Note collisioncheck enable must be true
	public static volatile double collisionCheckPeriod = 0.5;		// (s) Between two checks
	public static long appliedCollisionCheckPeriod;					// (ms) The same parameter but in milliseconds
	public static volatile double collisionDistance = 5;			// (m) Distance to assert that a collision has happened (UTM coordinates)
	public static volatile double collisionAltitudeDifference = 20;	// (m) Altitude difference to assert that a collision has happened
	public static volatile boolean collisionDetected = false; 		// Can be used to stop main.java.com.api.protocols when a collision happens
	
	// Parameters used to detect when a UAV reaches the last waypoint
	public static final double LAST_WP_THRESHOLD = 2.0; // (m) Maximum distance considered to assert that the UAV has reached the last waypoint
	
	// Received information
	public static UAVCurrentData[] uavCurrentData;
	public static UAVCurrentStatus[] uavCurrentStatus;
	
	// Last n UAV known positions of the UAV
	public static final int LOCATIONS_SIZE = 3;		// n positions (never less than 2)
	public static ConcurrentBoundedQueue<Location2DUTM>[] lastUTMLocations;	// Each UAV has an object with the last received locations sorted
	
	// Flight formation (when used)
	public static AtomicReference<Formation> groundFormation = new AtomicReference<>();
	public static AtomicReference<Formation> airFormation = new AtomicReference<>();
	// Initial distance between UAV when they are on the ground (only in simulation)
	public static volatile double groundDistanceBetweenUAV = 10.0;
	// Minimum distance between UAVs while following the main.java.com.protocols.mission
	public static volatile double airDistanceBetweenUAV = 50.0;
	// Minimum distance between UAVs when they land on the ground
	public static volatile double landDistanceBetweenUAV = 2.5;
	
	// Startup parameters
	public static double[] initialSpeeds;				// (m/s) Initial UAVs speed
	public static double initialAltitude = 0;			// (m) Initial altitude for all UAVs during simulation
	public static double minAltitude = 5.0;				// (m) Minimum waypoint relative altitude for KML missions
	public static volatile boolean overrideAltitude = false;	// Whether to override or not the altitude in KML missions, with the following value
	public static volatile double minFlyingAltitude = minAltitude;// (m) Waypoint relative altitude for KML missions, when stored values are overrided
	public static final double WIND_THRESHOLD = 0.5;	// (m/s) Minimum wind speed accepted by the simulator, when used
	public static double[] RTLAltitude;					// (m) RTL altitude retrieved from the flight controller
	public static double[] RTLAltitudeFinal;			// (m) Altitude to keep when reach home location when in RTL mode
	public static volatile boolean overrideYaw = false;	// Whether to override or not the yaw behavior while following a main.java.com.protocols.mission, with the following value
	public static volatile int yawBehavior = 2;			// 0=Fixed, 1=Face next waypoint, 2=Face next waypoint except RTL, 3=Face along GPS course
	public static final String[] YAW_VALUES = new String[] {"Fixed", "Face next waypoint", "Face next WP except RTL", "Face along GPS course"};
	
	public static AtomicIntegerArray mavId;				// ID of the multicopter in the MAVLink protocol
	public static AtomicIntegerArray gcsId;				// ID of the GCS authorized to send commands to the flight controller
														//   255 for Mission Planner and DroidPlanner
														//   252 for APM Planner 2
	public static int[] RCmapRoll;				// Channel the roll is mapped to
	public static int[] RCmapPitch;				// Channel the pitch is mapped to
	public static int[] RCmapThrottle;			// Channel the throttle is mapped to
	public static int[] RCmapYaw;				// Channel the yaw is mapped to
	public static int[] stabilizationThrottle;	// Middle throttle value (approx. 1500, mid stick)
	public static int[] throttleDZ;				// Deadzone around stabilization throttle
	public static int[][] RCminValue;			// Minimum value of all the 8 channels
	public static int[][] RCtrimValue;			// Trim value of all the 8 channels
	public static int[][] RCmaxValue;			// Maximum value of all the 8 channels
	public static int[][] RCDZValue;			// Dead zone around trim or bottom of all the 8 channels
	public static int[][] flightModeMap;		// Mapping of the 6 flight modes used in the remote control switch
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
	public static final double VIRT_BATTERY_ALARM_VOLTAGE = VIRTUAL_BATTERY_CELLS * ALARM_VOLTAGE;		// (V) Voltage on flight to rise an alarm (10.5V)
	public static final int VIRT_BATTERY_MAX_CAPACITY = 500000000;	// (mAh) Maximum initial battery capacity
	// Real or virtual battery
	public static int batteryCapacity;	// (mAh) Used battery capacity
	public static int batteryLowLevel;	// (mAh) Calculated battery energy level to rise the alarm

	// MAVLink waiting parameters
	public static AtomicInteger numMAVLinksOnline = new AtomicInteger();	// Number of MAVLink links stablished
	public static final long MAVLINK_ONLINE_TIMEOUT = 60 * 1000000000L;		// (ns) Global timeout
	public static final long MAVLINK_WAIT = 500;							// (ms) Passive waiting timeout

	// GPS fix waiting parameters
	public static AtomicInteger numGPSFixed = new AtomicInteger();	// Number of UAVs sending valid coordinates
	public static final long GPS_FIX_TIMEOUT = 300 * 1000000000L;	// (ns) Global timeout while waiting GPS coordinates
	public static final long GPS_FIX_WAIT = 500;					// (ms) Passively waiting GPS coordinates

	// Command ACK waiting timeout (ms)
	public static final long COMMAND_WAIT = 200;

	// Stabilization parameters (when the UAV is stopping)
	public static final double STABILIZATION_SPEED = 0.6;				// (m/s) When it is stopped
	public static final long STABILIZATION_WAIT_TIME = 200;				// (ms) Passively waiting the UAV to stop
	public static final long STABILIZATION_TIMEOUT = 5 * 1000000000L;	// (ns) Global timeout while waiting the UAV to stop

	// Filter to compensate the acceleration oscillation, applied when a new location is received from the UAV
	public static final double ACCELERATION_THRESHOLD = 0.2;	// [0, 1] 1=new value, 0=previous value
	public static final double MIN_ACCELERATION = 0.2;			// (m/s²) low pass filter
	public static final double MAX_ACCELERATION = 5;			// (m/s²) high pass filter

	// Period between heartbeats sent to the flight controller (ns)
	public static final long HEARTBEAT_PERIOD =  950000000L;
	
	// Minumum base mode to assert that the UAV is flying
	public static final int MIN_MODE_TO_BE_FLYING = 209;
	
	// Flight mode change timeout (since the command was accepted by the flight controller until that change is received through a heartbeat
	public static final long MODE_CHANGE_TIMEOUT = 5000;	// (ms)
	
	// Auxiliary variable needed to ensure that the message thrown when the UAV gets to the end is shown only once
	public static AtomicBoolean[] lastWaypointReached;
	
	// RC Channels message sent values
	public static AtomicReference<RCValues>[] rcs;
	
	// Target location for continuous movement
	public static AtomicReference<Location3DGeo>[] target;
	public static AtomicReference<float[]>[] targetSpeed;

	// Communications finite state machine. States of the MAVLink protocol
	public static AtomicIntegerArray MAVStatus;
	public static final int MAV_STATUS_OK = 0;
	public static final int MAV_STATUS_REQUEST_MODE = 1;
	public static final int MAV_STATUS_ACK_MODE = 2;
	public static final int MAV_STATUS_ERROR_MODE = 3;
	public static AtomicReferenceArray<FlightMode> flightMode; // Current flight mode
	public static FlightMode[] newFlightMode;
	public static AtomicIntegerArray flightStarted;	// Used to detect if the UAVs have started flight
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
	public static CopterParam[] newParam;
	public static AtomicDoubleArray newParamValue;		// Parameter value to send or received from the UAV controller
	public static AtomicIntegerArray newParamIndex;
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
	public static Waypoint[] lastWP;					// Last waypoint of the retrieved main.java.com.protocols.mission
	public static Location2DUTM[] lastWPUTM;			// Coordinates of the last waypoint of the retrieved main.java.com.protocols.mission
	public static AtomicReferenceArray<List<WaypointSimplified>> missionUTMSimplified; // Missions simplified from the previous one for drawing, in UTM coordinates
	public static final int MAV_STATUS_REQUEST_WP_LIST = 28;
	public static final int MAV_STATUS_REQUEST_WP0 = 29;
	public static final int MAV_STATUS_REQUEST_WPS = 30;
	public static final int MAV_STATUS_ERROR_REQUEST_WP_LIST = 31;
	public static final int MAV_STATUS_THROTTLE_ON = 32;
	public static final int MAV_STATUS_THROTTLE_ON_ERROR = 33;
	public static final int MAV_STATUS_RECOVER_CONTROL = 34;
	public static final int MAV_STATUS_RECOVER_ERROR = 35;
	public static AtomicIntegerArray overrideOn;	// ([0,1]) 1 = RC Channels can be overridden
	public static final int MAV_STATUS_MOVE_UAV = 36;
	public static final int MAV_STATUS_ACK_MOVE_UAV = 37;
	public static final int MAV_STATUS_MOVE_UAV_ERROR = 38;
	public static float[][] newLocation;	// [latitude, longitude, relative altitude] where to move the UAV
	public static AtomicInteger messageId = new AtomicInteger(-1);
	public static final int globalPositionIntId = GlobalPositionInt.class.getAnnotation(MavlinkMessageInfo.class).id();
	public static final int MAV_STATUS_REQUEST_ALL_PARAM = 39;
	public static final int MAV_STATUS_TIMEOUT_ALL_PARAM = 40;
	public static final int MAV_STATUS_ERROR_ALL_PARAM = 41;
	public static final int MAV_STATUS_REQUEST_MESSAGE = 42;
	public static final int MAV_STATUS_ERROR_REQUEST_MESSAGE = 43;
	public static final int MAV_STATUS_ACK_REQUEST_MESSAGE = 44;
	public static AtomicLong[] lastParamReceivedTime;	// (ms) When the last param was received when loading all parameters
	public static final long ALL_PARAM_TIMEOUT = 3000;	// (ms) Timeout waiting all parameters to be loaded
	public static final long VERSION_TIMEOUT = 5000; 	// (ms) Timeout waiting ArduCopter version to be read
	public static AtomicReference<String> arducopterVersion = new AtomicReference<>();
	public static double SIM_SPEEDUP = 1; // Speedup for ArduCopter 1 = realtime, 2 = 2x faster then realtime, 0.5 = 2x slower then realtime.
	
	// Potentiometer levels for the six flight modes configurable in the remote control (min, used, max)
	public static final int[][] RC5_MODE_LEVEL = new int[][] {{0, 1000, 1230}, {1231, 1295, 1360},
		{1361, 1425, 1490}, {1491, 1555, 1620}, {1621, 1685, 1749}, {1750, 1875, 2000}};
	
	public static Map<String, CopterParamLoaded>[] loadedParams;
	public static int[] totParams;
	public static boolean[][] paramLoaded;
}
