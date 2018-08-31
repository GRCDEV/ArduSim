package main;

import api.Tools;
import sim.board.BoardParam;
import uavController.UAVParam;

/** This class contains text descriptions used on the application and on the simulator GUI. */

public class Text {
	
	// Application name
	public static final String APP_NAME = "ArduSim";
	
	// Default protocol
	public static final String PROTOCOL_NONE = "none";
	
	// Simulator parameters parsing errors
	public static final String COMPANION_ERROR = "When running as a PC companion no other parameters are needed.";
	public static final String INI_FILE_NOT_FOUND = "ardusim.ini file not found. Using default parameters values.";
	public static final String INI_FILE_EMPTY = "ardusim.ini file is empty. Using default parameters values.";
	public static final String INI_FILE_WASTE_PARAMETER_WARNING = "ardusim.ini file contains an invalid parameter:";
	public static final String INI_FILE_MISSING_PARAMETER_WARNING = "parameter missing. Using default value.";
	public static final String INI_FILE_PROTOCOL_NOT_FOUND_ERROR = "No protocol specified in INI file.";
	public static final String INI_FILE_SPEED_NOT_FOUND_ERROR = "No flight speed specified in INI file.";
	public static final String PROTOCOL_NOT_FOUND_ERROR = "protocol was not found. Valid protocols:";
	public static final String SPEED_ERROR = "The UAV speed must be a valid positive number.";
	public static final String INI_FILE_PARAM_NOT_FOUND_ERROR = "param not found. Using default value:";
	public static final String INI_FILE_PARAM_NOT_VALID_ERROR = "param not valid value:";
	public static final String INI_FILE_PARAM_SERIAL_PORT_WARNING = "The serial link could not work properly if the serial port name is wrong:";
	public static final String INI_FILE_PARAM_BROADCAST_IP_WARNING = "Broadcast IP:";
	public static final String PROTOCOL_GETTING_CLASSES_ERROR = "Unable to retrieve the existing classes in " + APP_NAME;
	public static final String PROTOCOL_GETTING_PROT_CLASSES_ERROR = "Unable to retrieve classes implementing protocols in " + APP_NAME;
	public static final String PROTOCOL_LOADING_ERROR = "Unable to load existing protocols in " + APP_NAME + ". At least one protocol must be implemented.";
	public static final String PROTOCOL_GETTING_PROTOCOL_CLASSES_ERROR = "Unable to retrieve instances for the current protocol classes in " + APP_NAME;
	public static final String PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR = "No valid implementation was found for protocol: ";
	public static final String PROTOCOL_MANY_IMPLEMENTATIONS_ERROR = "More than one implementation was found for the protocol: ";
	
	// Configuration dialog window:
	public static final String OK = "OK";
	public static final String RESTORE_DEFAULTS = "Restore defaults";
	public static final String BUTTON_SELECT = "...";
	public static final String CONFIGURATION_DIALOG_TITLE = "Simulator configuration";
	public static final String SIMULATION_PARAMETERS = "Simulation parameters:";
	public static final String GENERAL_PARAMETERS = "General parameters:";
	public static final String ARDUCOPTER_PATH = "ArduCopter path:";
	public static final String BASE_PATH_DIALOG_TITLE = "Select the SITL arducopter executable file";
	public static final String BASE_PATH_DIALOG_SELECTION = "ArduCopter file";
	public static final String BASE_PATH_DIALOG_EXTENSION = "exe";
	public static final String MISSIONS_DIALOG_TITLE = "Select the file(s) with UAVs paths";
	public static final String MISSIONS_DIALOG_SELECTION_1 = "Google Earth kml File";
	public static final String FILE_EXTENSION_KML = "kml";
	public static final String MISSIONS_DIALOG_SELECTION_2 = "Waypoint Files";
	public static final String DEFAULT_BASE_NAME = "experiment";
	public static final String FILE_EXTENSION_WAYPOINTS = "waypoints";
	public static final String FILE_EXTENSION_TXT = "txt";
	public static final String SPEEDS_FILE = "Speeds file:";
	public static final String SPEEDS_DIALOG_TITLE = "Select the csv file with the UAVs speeds";
	public static final String SPEEDS_DIALOG_SELECTION = "Speeds csv file";
	public static final String FILE_EXTENSION_CSV = "csv";
	public static final String UAV_NUMBER = "Number of UAVs:";
	public static final String PERFORMANCE_PARAMETERS = "Performance parameters:";
	public static final String SCREEN_REFRESH_RATE = "Screen refresh rate:";
	public static final String REDRAW_DISTANCE = "Minimum screen redraw distance:";
	public static final String LOGGING = "Enable arducopter logging:";
	public static final String BATTERY = "Restrict battery capacity:";
	public static final String BATTERY_SIZE = "Battery capacity:";
	public static final String BATTERY_THRESHOLD = "Low level battery threshold:";
	public static final String CPU_MEASUREMENT_ENABLED = "Measure CPU use:";
	public static final String RENDER = "Rendering quality:";
	public static final String RENDER_QUALITY1 = "Maximum performance";
	public static final String RENDER_QUALITY2 = "Text smoothed";
	public static final String RENDER_QUALITY3 = "Text and lines smoothed";
	public static final String RENDER_QUALITY4 = "Maximum quality";
	public static final String VERBOSE_LOGGING_ENABLE = "Enable verbose logging:";
	public static final String VERBOSE_STORAGE_ENABLE = "Enable verbose storage:";
	public static final String UAV_PROTOCOL_USED = "UAV synchronization protocol:";
	public static final String COMMUNICATIONS = "UAV to UAV communications parameters:";
	public static final String BROADCAST_IP = "Broadcast IP address:";
	public static final String BROADCAST_PORT = "Broadcast address port:";
	public static final String CARRIER_SENSING = "Enable carrier sensing:";
	public static final String CARRIER_SENSING_ENABLED = "Carrier sensing enabled:";
	public static final String PACKET_COLLISION_DETECTION = "Enable packet collision detection:";
	public static final String PACKET_COLLISION_DETECTION_ENABLED = "Packet Collision detection enabled:";
	public static final String BUFFER_SIZE = "Receiving buffer size:";
	public static final String BUFFER_SIZE_ERROR_1 = "The receiving buffer must be a positive number of bytes.";
	public static final String BUFFER_SIZE_ERROR_2 = "The receiving buffer must be big enough to store at least one frame (" + Tools.DATAGRAM_MAX_LENGTH + ")";
	public static final String WIFI_MODEL = "Wireless communications model:";
	public static final String FIXED_RANGE_DISTANCE = "Fixed range distance:";
	
	public static final String TOT_SENT_PACKETS = "Total sent packets:";
	public static final String TOT_WAITED_PREV_SENDING = "Waiting for the previous packet to be sent:";
	public static final String TOT_WAITED_MEDIA_AVAILABLE = "Waiting the media to be available (carrier sensing):";
	public static final String TOT_POTENTIALLY_RECEIVED = "Potentially received packets:";
	public static final String TOT_OUT_OF_RANGE = "Not received due to range:";
	public static final String TOT_LOST_RECEIVER_WAS_SENDING = "Not received because the receiver was sending:";
	public static final String TOT_VIRTUAL_QUEUE_WAS_FULL = "Discarded because the virtual receiving buffer was full:";
	public static final String TOT_QUEUE_WAS_FULL = "Discarded because the receiving buffer was full:";
	public static final String TOT_RECEIVED = "Received packets:";
	public static final String TOT_RECEIVED_IN_VBUFFER = "Received packets in virtual buffer:";
	public static final String TOT_REMAINING_IN_VBUFFER = "Remaining in virtual buffers:";
	public static final String TOT_REMAINING_IN_BUFFER = "Remaining in buffers:";
	public static final String TOT_PROCESSED = "Processed in virtual buffers:";
	public static final String TOT_DISCARDED_FOR_COLLISION = "Discarded due to collisions:";
	public static final String TOT_USED_OK = "Used:";
	public static final String COLLISION_PARAMETERS = "UAV Collision detection parameters:";
	public static final String COLLISION_ENABLE = "Enable collision detection:";
	public static final String COLLISION_PERIOD = "Check period:";
	public static final String COLLISION_PERIOD_ERROR = "The collision check period must be a valid positive number.";
	public static final String COLLISION_DISTANCE = "Distance threshold:";
	public static final String COLLISION_DISTANCE_THRESHOLD_ERROR = "The distance to assert collision must be a valid positive number.";
	public static final String COLLISION_ALTITUDE = "Altitude difference threshold:";
	public static final String COLLISION_ALTITUDE_THRESHOLD_ERROR = "The altitude difference to assert collision must be a valid positive number.";
	public static final String COLLISION_DETECTED = "Collision detected!";
	public static final String COLLISION_TITLE = "Collision detected";
	public static final String COLLISION_DETECTED_ERROR_1 = "Emergency landing due to the collision between the UAVs";
	public static final String COLLISION_DETECTED_ERROR_2 = "It is suggested to close the application.\nCollision detected between the UAVs";
	public static final String WIND = "Wind:";
	public static final String WIND_PARAMETERS = "Wind parameters:";
	public static final String WIND_ENABLE = "Enable wind:";
	public static final String WIND_DIRECTION = "Direction:";
	public static final String WIND_SPEED = "Speed:";
	// Error messages:
	public static final String SITL_SELECTION_ERROR = "SITL file selection warning";
	public static final String SITL_ERROR_1 = "The SITL file must be executable.";
	public static final String SITL_ERROR_2 = "The following file must be in the same folder as SITL:";
	public static final String SITL_ERROR_3 = "The path of SITL executable must be selected.";
	public static final String MISSIONS_SELECTION_ERROR = "Missions selection warning";
	public static final String MISSIONS_ERROR_1 = "No more than one kml file must be selected.";
	public static final String MISSIONS_ERROR_2 = "kml or waypoints files could be selected, but not at the same time.";
	public static final String MISSIONS_ERROR_3 = "The kml file is not valid.";
	public static final String MISSIONS_ERROR_4 = "No valid files were found.";
	public static final String MISSIONS_ERROR_5 = "The file(s) with the UAV(s) mission must be selected.";
	public static final String SPEEDS_SELECTION_ERROR = "Speeds selection warning";
	public static final String SPEEDS_ERROR_1 = "The csv file is not valid.\nOne value by row and without header.";
	public static final String SPEEDS_ERROR_2 = "The file with the UAV(s) speed must be selected.";
	public static final String VALIDATION_WARNING = "Validation warning";
	public static final String UAVS_NUMBER_ERROR = "A valid number of UAVs to use must be selected.";
	public static final String SCREEN_DELAY_ERROR_1 = "The time between screen refresh must be a valid positive integer.";
	public static final String SCREEN_DELAY_ERROR_2 = "The time between screen refresh must be between " + BoardParam.MIN_SCREEN_DELAY
			+ " and " + BoardParam.MAX_SCREEN_DELAY + " ms.";
	public static final String MIN_SCREEN_MOVEMENT_ERROR_1 = "The minimal screen movement of a UAV must be a valid positive number.";
	public static final String MIN_SCREEN_MOVEMENT_ERROR_2 = "The minimal screen movement of a UAV must be less than "
			+ BoardParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD + " pixels.";
	public static final String BATTERY_ERROR_1 = "The battery capacity must be a valid positive integer.";
	public static final String BATTERY_ERROR_2 = "The battery capacity can be a maximum of " + UAVParam.VIRT_BATTERY_MAX_CAPACITY + " " + Text.BATTERY_CAPACITY;
	public static final String WIRELESS_MODEL_ERROR_1 = "The wireless range must be a valid positive number.";
	public static final String WIRELESS_MODEL_ERROR_2 = "The wireless range must be less than "
			+ Param.FIXED_MAX_RANGE + " meters.";
	public static final String WIND_DIRECTION_ERROR = "The wind direction must be a valid positive integer.";
	public static final String WIND_SPEED_ERROR_1 = "The wind speed must be a valid positive number.";
	public static final String WIND_SPEED_ERROR_2 = "The wind speed must be greater or equal to " + UAVParam.WIND_THRESHOLD;
	public static final String BIND_ERROR_1 = "Unable to bind port to receive commands from the computer assistant.";
	public static final String BIND_ERROR_2 = "Unable to open socket to send status information to the computer assistant.";
	public static final String BIND_ERROR_3 = "Unable to bind port to receive status information from the UAVs.";
	public static final String CPU_ERROR_1 = "Unable to load the Windows library needed to analyze CPU usage. CPU analysis disabled.";
	public static final String CPU_ERROR_2 = "Unable to retrieve the number of CPU cores available. CPU analysis disabled.";
	public static final String CPU_ERROR_3 = "Unable to retrieve ArduSim process Id. CPU analysis disabled.";
	public static final String CPU_ERROR_4 = "Unable to retrieve CPU usage data. CPU analysis disabled.";
	public static final String SHUTDOWN_ERROR = "Operating system not supported.";
	
	// Progress dialog window:
	public static final String PROGRESS_DIALOG_TITLE = "Test progress";
	public static final String UAV_ID = "UAV";
	public static final String X_COORDINATE = "x =";
	public static final String Y_COORDINATE = "y =";
	public static final String Z_COORDINATE = "z =";
	public static final String SPEED_1 = "speed =";
	public static final String MAV_MODE = "MAV mode:";
	public static final String NULL_TEXT = "----";
	public static final String PROTOCOL_STATUS = "Protocol state:";
	
	// Main window:
	public static final String CONFIGURATION_PANEL = "Configuration panel";
	public static final String SETUP_TEST = "Setup";
	public static final String SHOW_PROGRESS = "Show progress";
	public static final String START_TEST = "Start test";
	public static final String EXIT = "Exit";
	public static final String WAITING_MAVLINK = "Waiting for MAVLink connection...";
	public static final String WAITING_GPS = "Waiting for GPS fix...";
	public static final String WAITING_CONFIGURATION_UPLOAD = "Waiting for initial configuration upload...";
	public static final String COPYRIGHT = "\u00A9 Google";
	public static final String DRAWING_PANEL = "Drawing panel";
	public static final String LOADING_UAV_IMAGE_ERROR = "The UAV image could not be loaded.";
	// Progress shown in the interaction panel
	public static final String STARTING_ENVIRONMENT = "Starting environment...";
	public static final String UAVS_ONLINE = "UAVs connected...";
	public static final String READY_TO_FLY = "Ready to fly.";
	public static final String READY_TO_START = "Ready to start.";
	public static final String TEST_IN_PROGRESS = "Test in progress...";
	public static final String TEST_FINISHED = "Test finished.";
	public static final String SHUTTING_DOWN = "Shutting down...";
	public static final String CONFIGURATION_IN_PROGRESS = "Configuration in progress...";
	// Progress shown in the log
	public static final String USING_RAM_DRIVE = "Using RAM drive for temporary files.";
	public static final String USING_HARD_DRIVE = "Using physical drive for temporary files.";
	public static final String INSTALL_IMDISK = "It is suggested to install ImDisk in order to improve performance.";
	public static final String USE_ADMIN = "It is suggested to run " + Text.APP_NAME + " as administrator in order to improve performance.";
	public static final String INSTALL_IMDISK_USE_ADMIN = "It i suggested to install ImDisk, and run " + Text.APP_NAME + " as administrator in order to improve performance.";
	public static final String USE_ROOT = "It is suggested to run " + Text.APP_NAME + " as root in order to improve performance.";
	public static final String PROTOCOL_IN_USE = "Protocol in use:";
	public static final String WIRELESS_MODEL_IN_USE = "Wireless model in use:";
	public static final String SIMULATED_WIND_SPEED = "Wind speed:";
	public static final String STARTING_UAVS = "Starting up virtual UAVs...";
	public static final String SITL_UP = "SITL instance is up.";
	public static final String CONTROLLERS_STARTED = "UAVs controllers started...";
	public static final String GPS_OK = "GPS fix acquired...";
	public static final String COMMUNICATIONS_ONLINE = "Simulated communications online...";
	public static final String COLLISION_DETECTION_ONLINE = "Collision detection online...";
	public static final String SEND_BASIC_CONFIGURATION = "Preparing UAVs internal configuration...";
	public static final String WAITING_FOR_USER = "Waiting for user interaction.";
	public static final String TAKING_OFF = "Take off in progress...";
	public static final String TEST_START = "Test started...";
	public static final String SHUTTING_DOWN_COMM = "Blocking virtual communications...";
	public static final String EXITING = "UAV(s) closed. Time to go...";
	public static final String ALTITUDE_TEXT = "altitude";
	
	// Parsing mission(s) file(s)
	public static final String MISSION_NOT_FOUND = "No valid mission file was found on current folder.";
	public static final String MISSIONS_ERROR_6 = "Only one kml file must be stored on the current folder.";
	public static final String MISSION_XML_SELECTED = "Using first mission found on file:";
	public static final String MISSIONS_ERROR_7 = "Only one waypoints file must be stored on the current folder.";
	public static final String MISSION_WAYPOINTS_SELECTED = "Using mission found on file:";
	public static final String LINE_TAG = "LineString";
	public static final String COORDINATES_TAG = "coordinates";
	public static final String XML_PARSING_ERROR_1 = "No path lines found.";
	public static final String XML_PARSING_ERROR_2 = "Not enough points to define a path in line:";
	public static final String XML_PARSING_ERROR_3 = "Wrong file format.";
	public static final String XML_PARSING_ERROR_4 = "Error parsing line/point:";
	public static final String FILE_HEADER = "QGC WPL 110";
	public static final String FILE_PARSING_ERROR_1 = "Waypoint file error: Nothing useful found.";
	public static final String FILE_PARSING_ERROR_2 = "Waypoint file error: Format not valid.";
	public static final String FILE_PARSING_ERROR_3 = "Waypoint file error: Wrong length in line:";
	public static final String FILE_PARSING_ERROR_4 = "Waypoint file error: Wrong format in line:";
	public static final String FILE_PARSING_ERROR_5 = "Waypoint file error: Waypoint 0 is needed but ignored, and\n waypoint 1 must be a take off.";
	public static final String FILE_PARSING_ERROR_6 = "Waypoint file error: Waypoints are not in the propper sequence order.";
	// Parsing speed(s) file(s)
	public static final String SPEEDS_PARSING_ERROR_1 = "Speeds file: No speed values found.";
	public static final String SPEEDS_PARSING_ERROR_2 = "Speeds file: Wrong format in line:";
	
	// Results dialog window:
	public static final String RESULTS_TITLE = "Results";
	public static final String RESULTS_DIALOG_TITLE = "Select destination";
	public static final String RESULTS_DIALOG_SELECTION = "Text files";
	public static final String CONFIGURATION = "configuration";
	
	// Closing dialog
	public static final String CLOSING_QUESTION = "Do you really want to close " + Text.APP_NAME + "?";
	public static final String CLOSING_DIALOG_TITLE = "Please, confirm";
	
	// Logs stored
	public static final String LOG_GLOBAL = "Global";
	public static final String LOG_TOTAL_TIME = "Total time";
	public static final String LOG_SPEED = "Initial speed";
	public static final String LOG_BATTERY = "Battery capacity";
	public static final String MISSION_SUFIX = "mission_AutoCAD.scr";
	public static final String PATH_SUFIX = "path.csv";
	public static final String PATH_2D_SUFIX = "path_AutoCAD.scr";
	public static final String PATH_3D_SUFIX = "path_AutoCAD3d.scr";
	public static final String MOBILITY_NS2_SUFIX_2D = "mobility_NS2.txt";
	public static final String MOBILITY_NS2_SUFIX_3D = "mobility_NS2_3D.txt";
	public static final String MOBILITY_OMNET_SUFIX_2D = "mobility_OMNeT-INET-BoonMotionModel.txt";
	public static final String MOBILITY_OMNET_SUFIX_3D = "mobility_OMNeT-INET-BoonMotionModel_3D.txt";
	public static final String CPU_SUFIX = "CPU.txt";
	
	// General options and units
	public static final String YES_OPTION = "yes";
	public static final String NO_OPTION = "no";
	public static final String OPTION_ENABLED = "enabled";
	public static final String OPTION_DISABLED = "disabled";
	public static final String RESULTS_SAVE_DATA = "Save";
	public static final String RESULTS_IGNORE_DATA = "Close";
	public static final String MILLISECONDS = "ms";
	public static final String PIXELS = "pixels";
	public static final String METERS = "m";
	public static final String SECONDS = "s";
	public static final String METERS_PER_SECOND = "m/s";
	public static final String BATTERY_CAPACITY = "mAh";
	public static final String BYTES = "bytes";
	public static final String DEGREE_SYMBOL= "\u00B0";
	
	// General errors
	public static final String PORT_ERROR = "Communications error";
	public static final String PORT_ERROR_1 = "Some ports are in use, so only can be simulated a maximum of ";
	public static final String PORT_ERROR_2 = "It was not possible to get valid ports to connect to SITL instances.";
	public static final String MAC_ERROR = "Not valid MAC address could be found on any network interface.";
	public static final String MESSAGE_ERROR = "Error sending a message.";
	public static final String FATAL_ERROR = "Fatal error";
	public static final String ARROW_IMAGE_LOAD_ERROR = "The wind arrow image could not be loaded.";
	public static final String UAV_OUT_OF_SCREEN_ERROR = "A UAV came out of the screen.";
	public static final String SIMPLIFYING_WAYPOINT_LIST_ERROR = "A back home waypoint was found but home is not defined.\n"
			+ "Error simplifying the waypoint list of the UAV ";
	public static final String MOUNT_DRIVE_ERROR_1 = "No available drive letter was found.";
	public static final String MOUNT_DRIVE_ERROR_2 = "No drives were found on the system.";
	public static final String TEMP_PATH_ERROR = "It was not possible to define a folder for temporary files.";
	public static final String UAVS_START_ERROR_1 = "failed starting the virtual UAVs.\nIs another instance of " + Text.APP_NAME + " already running?.";
	public static final String UAVS_START_ERROR_2 = "It was not possible to create temporal folders in:";
	public static final String UAVS_START_ERROR_3 = Text.APP_NAME + " for Windows requires Cygwin:";
	public static final String UAVS_START_CYGWIN_ERROR = "Cygwin not found.";
	public static final String UAVS_START_ERROR_4 = "The running operating system is not compatible with " + Text.APP_NAME + ".";
	public static final String UAVS_START_ERROR_5 = "Virtual UAV starting timeout running instance:";
	public static final String THREAD_START_ERROR = "Failed to bind socket to IP.";
	public static final String INITIAL_CONFIGURATION_ERROR_1 = "Failed forcing GPS data messages on the UAVs";
	public static final String INITIAL_CONFIGURATION_ERROR_2 = "Failed sending the initial configuration to the UAVs";
	public static final String BATTERY_FAILING = "Battery depleting on ";
	public static final String BATTERY_FAILING2 = "Battery failing.";
	public static final String BATTERY_LEVEL = "Battery level:";
	public static final String BATTERY_LEVEL2 = "Battery level of UAV";
	public static final String TAKE_OFF_ERROR_1 = "Failed executing the take off of the UAV";
	public static final String WIRELESS_ERROR = "Error. The function Tools.isInRange() must be modified.";
	public static final String DOWNLOAD_ERROR = "Image not available";
	public static final String DISMOUNT_DRIVE_ERROR = "Failed dismounting the virtual RAM drive.";
	public static final String STORE_WARNING = "Store warning";
	public static final String STORE_QUESTION = "Do you want to overwrite the file?";
	public static final String LAST_WAYPOINT_REACHED = "Last waypoint reached.";
	public static final String LAND_ERROR = "Was unable to land.";
	
	// System properties
	public static final String HOME_DIR = "user.home";
	public static final String SCROLLBAR_WIDTH = "ScrollBar.width";
	public static final String OPERATING_SYSTEM_WINDOWS = "Windows OS detected.";
	public static final String OPERATING_SYSTEM_LINUX = "Linux OS detected.";
	public static final String OPERATING_SYSTEM_MAC = "macOS detected.";
	
	// MAVLink messages
	public static final String WAYPOINT_REACHED = "Reached waypoint";
	
	// MAVLink errors
	public static final String MAVLINK_ERROR = "Unable to stablish MAVLink connection with all UAVs.";
	public static final String GPS_FIX_ERROR_1 = "Unable to get GPS fix from all UAVs.";
	public static final String GPS_FIX_ERROR_2 = "Unable to get GPS fix.";
	public static final String PARAMETER_1 = "New parameter value:";
	public static final String PARAMETER_ERROR_1 = "Error modifying parameter:";
	public static final String PARAMETER_2 = "Received new parameter:";
	public static final String PARAMETER_ERROR_2 = "Error getting parameter:";
	public static final String FLIGHT_MODE = "New flight mode";
	public static final String FLIGHT_MODE_ERROR_1 = "Error changing the flight mode.";
	public static final String ARM_ENGINES = "Engines armed.";
	public static final String ARM_ENGINES_ERROR = "Error arming engines.";
	public static final String TAKE_OFF = "Taking off.";
	public static final String TAKE_OFF_ERROR_2 = "Error taking off.";
	public static final String SPEED_2 = "New speed";
	public static final String SPEED_2_ERROR = "Error changing flight speed.";
	public static final String CURRENT_WAYPOINT = "New current waypoint";
	public static final String CURRENT_WAYPOINT_ERROR = "Error setting the new current waypoint.";
	public static final String STOP = "UAV stationary.";
	public static final String STOP_ERROR_1 = "Can't stabilize position.";
	public static final String STOP_ERROR_2 = "Error stopping.";
	public static final String STABILIZE_ALTITUDE = "Altitude control enabled.";
	public static final String STABILIZE_ALTITUDE_ERROR = "Error getting the altitude control.";
	public static final String RETURN_RC_CONTROL = "Control returned to the remote controller.";
	public static final String RETURN_RC_CONTROL_ERROR = "Error returning control to the remote controller.";
	public static final String RC_CHANNELS_OVERRIDE_FORBIDEN_ERROR = "Overriding RC channels is not allowed.";
	public static final String MOVING_ERROR_1 = "Error changing position.";
	public static final String MISSION_DELETE = "Previous mission erased.";
	public static final String MISSION_DELETE_ERROR = "Error erasing the current mission.";
	public static final String MISSION_SENT = "Mission sent.";
	public static final String MISSION_SENT_ERROR_1 = "The waypoint list is null or empty.";
	public static final String MISSION_SENT_ERROR_2 = "Takeoff altitude is lower than the minimum flying altitude";
	public static final String MISSION_SENT_ERROR_3 = "Error sending the mission.";
	public static final String MISSION_GET = "Mission retrieved from the UAV.";
	public static final String MISSION_GET_ERROR = "Error retrieving the current mission from the UAV.";
	public static final String SERIAL_ERROR = "Failed conecting to the serial port.";
	public static final String TCP_ERROR = "Failed connecting to SITL through TCP.";
	public static final String FLIGHT_MODE_ERROR_2 = "Reporting an unknown flying mode.";
	public static final String IMPOSSIBLE_WAYPOINT_ERROR = "Received a waypoint out of range.";
	public static final String NOT_REQUESTED_ACK_ERROR = "Not requested ACK received:";
	
	// Wireless models
	public static final String WIRELESS_MODEL_NONE = "unrestricted";
	public static final String WIRELESS_MODEL_FIXED_RANGE = "fixed range";
	public static final String WIRELESS_MODEL_5GHZ = "802.11a with 5dBi antenna";
	
	// Flight modes
	public static final String LAND = "Land";
	public static final String LAND_ARMED = "Land_armed";
	public static final String STABILIZE = "Stabilize";
	public static final String STABILIZE_ARMED = "Stabilize_armed";
	public static final String THROW = "Throw";
	public static final String DRIFT = "Drift";
	public static final String ALT_HOLD = "Alt_hold";
	public static final String SPORT = "Sport";
	public static final String ACRO = "Acro";
	public static final String GUIDED = "Guided";
	public static final String GUIDED_ARMED = "Guided_armed";
	public static final String AUTO = "Auto";
	public static final String AUTO_ARMED = "Auto_armed";
	public static final String RTL = "RTL";
	public static final String RTL_ARMED = "RTL_armed";
	public static final String POSHOLD = "Poshold";
	public static final String POSHOLD_ARMED = "Poshold_armed";
	public static final String BRAKE = "Brake";
	public static final String BRAKE_ARMED = "Brake_armed";
	public static final String LOITER = "Loiter";
	public static final String LOITER_ARMED = "Loiter_armed";
	public static final String AVOID_ADSB = "Avoid_Adsb";
	public static final String AVOID_ADSB_ARMED = "Avoid_Adsb_armed";
	public static final String CIRCLE = "Circle";
	public static final String CIRCLE_ARMED = "Circle_armed";
	
	// PC Companion
	public static final String NUM_UAVS_COUNTER = " UAVs connected";
	public static final String PROTOCOL = "Protocol:";
	public static final String RECOVER_CONTROL = "Recover control";
	public static final String EMERGENCY_ACTIONS = "Emergency actions";
	public static final String IDENTIFIER_HEADER = "identifier";
	public static final String MAC_HEADER = "MAC";
	public static final String IP_HEADER = "IP";
	public static final String STATUS_HEADER = "status";
	public static final String DIALOG_TITLE = "Warning";
	public static final String SETUP_WARNING = "Are you sure that the safety switch is armed?";
	public static final String EMERGENCY_NOT_FOUND = "Emergency command not found.";
	public static final String EMERGENCY_SUCCESS = "Emergency command applied successfully:";
	public static final String EMERGENCY_FAILED = "Emergency command failed:";
	
	

}
