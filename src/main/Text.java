package main;

import sim.board.BoardParam;
import uavController.UAVParam;

/** This class contains text descriptions used on the application and on the simulator GUI. */

public class Text {
	
	// Application name
	public static final String APP_NAME = "ArduSim";
	
	// Protocol versions
	public static final String PROTOCOL_NONE = "none";
	
	// Configuration dialog window:
	public static final String OK = "OK";
	public static final String RESTORE_DEFAULTS = "Restore defaults";
	public static final String BUTTON_SELECT = "...";
	public static final String CONFIGURATION_DIALOG_TITLE = "Simulator configuration";
	public static final String SIMULATION_PARAMETERS = "Simulation parameters:";
	public static final String GENERAL_PARAMETERS = "General parameters";
	public static final String SIMULATION_TYPE = "Type of simulation:";
	public static final String SIMULATION_MISSION_BASED = "Mission based";
	public static final String SIMULATION_SWARM = "UAV swarm";
	public static final String ARDUCOPTER_PATH = "ArduCopter path:";
	public static final String BASE_PATH_DIALOG_TITLE = "Select the SITL arducopter executable file";
	public static final String BASE_PATH_DIALOG_SELECTION = "ArduCopter file";
	public static final String BASE_PATH_DIALOG_EXTENSION = "elf";
	public static final String MISSIONS_SELECTION = "UAV mission file(s):";
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
	public static final String LOGGING = "ArduCopter logging:";
	public static final String BATTERY = "Battery capacity limited:";
	public static final String RENDER = "Rendering quality:";
	public static final String RENDER_QUALITY1 = "Minimum quality";
	public static final String RENDER_QUALITY2 = "Text smoothed";
	public static final String RENDER_QUALITY3 = "Text and lines smoothed";
	public static final String RENDER_QUALITY4 = "Maximum quality";
	public static final String UAV_PROTOCOL_USED = "UAV synchronization protocol:";
	public static final String WIFI_MODEL = "Wireless communications model:";
	public static final String FIXED_RANGE_DISTANCE = "Fixed range distance:";
	public static final String WIND = "Wind:";
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
	public static final String BATTERY_ERROR_2 = "The battery capacity can be a maximum of " + UAVParam.MAX_BATTERY_CAPACITY + " " + Text.BATTERY_CAPACITY;
	public static final String WIRELESS_MODEL_ERROR_1 = "The wireless range must be a valid positive number.";
	public static final String WIRELESS_MODEL_ERROR_2 = "The wireless range must be less than "
			+ Param.FIXED_MAX_RANGE + " meters.";
	public static final String WIND_DIRECTION_ERROR = "The wind direction must be a valid number.";
	public static final String WIND_SPEED_ERROR_1 = "The wind speed must be a valid positive number.";
	public static final String WIND_SPEED_ERROR_2 = "The wind speed must be greater or equal to " + UAVParam.WIND_THRESHOLD;
	
	// Progress dialog window:
	public static final String PROGRESS_DIALOG_TITLE = "Test progress";
	public static final String UAV_ID = "UAV";
	public static final String X_COORDINATE = "x =";
	public static final String Y_COORDINATE = "y =";
	public static final String Z_COORDINATE = "z =";
	public static final String SPEED_1 = "speed =";
	public static final String MAV_MODE = "MAV mode:";
	public static final String NULL_TEXT = "----";
	public static final String PROTOCOL_STATUS = "Protocol status:";
	
	// Main window:
	public static final String CONFIGURATION_PANEL = "Configuration panel";
	public static final String MISSION_BASED_CONFIGURATION = "Take off";
	public static final String SWARM_BASED_CONFIGURATION = "Setup";
	public static final String SHOW_PROGRESS = "Show progress";
	public static final String START_TEST = "Start test";
	public static final String EXIT = "Exit";
	public static final String WAITING_MAVLINK = "Waiting for MAVLink connection...";
	public static final String WAITING_GPS = "Waiting for GPS fix...";
	public static final String WAITING_MISSION_UPLOAD = "Waiting for mission upload...";
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
	// Progress shown in the log
	public static final String USING_RAM_DRIVE = "Using RAM drive for temporary files.";
	public static final String USING_HARD_DRIVE = "Using physical drive for temporary files.";
	public static final String INSTALL_IMDISK = "It is suggested to install ImDisk in order to improve performance.";
	public static final String USE_ADMIN = "It is suggested to run " + Text.APP_NAME + " as administrator in order to improve performance.";
	public static final String INSTALL_IMDISK_USE_ADMIN = "It i suggested to install ImDisk, and run " + Text.APP_NAME + " as administrator in order to improve performance.";
	public static final String USE_ROOT = "It is suggested to run " + Text.APP_NAME + " as root in order to improve performance.";
	public static final String CAP_IN_USE = "Collision avoidance protocol in use:";
	public static final String WIRELESS_MODEL_IN_USE = "Wireless model in use:";
	public static final String SIMULATED_WIND_SPEED = "Wind speed:";
	public static final String STARTING_UAVS = "Starting up virtual UAVs...";
	public static final String SITL_UP = "SITL instance is up.";
	public static final String CONTROLLERS_STARTED = "UAVs controllers started...";
	public static final String GPS_OK = "GPS fix acquired...";
	public static final String SEND_MISSION = "Setting missions if needed...";
	public static final String WAITING_FOR_USER = "Waiting for user interaction.";
	public static final String TAKING_OFF = "Take off in progress...";
	public static final String TEST_START = "Test started...";
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
	// Parsing speeds file
	public static final String SPEEDS_NOT_FOUND = "No valid speeds file was found on current folder.";
	public static final String SPEEDS_ERROR_3 = "Only one csv file must be stored on the current folder.";
	public static final String SPEEDS_ERROR_4 = "UAV loaded speed must be greater than 0.";
	public static final String SPEEDS_CSV_SELECTED = "Using first speeds value found on file:";
	public static final String SPEEDS_PARSING_ERROR_1 = "Speeds file: No speed values found.";
	public static final String SPEEDS_PARSING_ERROR_2 = "Speeds file: Wrong format in line:";
	// Parsing protocol file
	public static final String PROTOCOL_NOT_FOUND = "No valid protocol file was found on current folder.";
	
	// Results dialog window:
	public static final String RESULTS_TITLE = "Results";
	public static final String RESULTS_DIALOG_TITLE = "Select destination";
	public static final String RESULTS_DIALOG_SELECTION = "Text files";
	
	// Closing dialog
	public static final String CLOSING_QUESTION = "Do you really want to close " + Text.APP_NAME + "?";
	public static final String CLOSING_DIALOG_TITLE = "Please, confirm";
	
	// Logs stored
	public static final String LOG_GLOBAL = "Global";
	public static final String LOG_TOTAL_TIME = "Total time";
	public static final String LOG_SPEED = "Initial speed";
	public static final String LOG_BATTERY = "Battery capacity";
	
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
	public static final String DEGREE_SYMBOL= "\u00B0";
	
	// General errors
	public static final String PORT_ERROR = "Communications error";
	public static final String PORT_ERROR_1 = "Some ports are in use, so only can be simulated a maximum of ";
	public static final String PORT_ERROR_2 = "It was not possible to get valid ports to connect to SITL instances.";
	public static final String MAC_ERROR = "Not valid MAC address could be found on any network interface.";
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
	public static final String UAVS_START_ERROR_5 = "Failed locating the home position of the UAV";
	public static final String UAVS_START_ERROR_6 = "No valid coordinates could be found to stablish the home of the UAV";
	public static final String THREAD_START_ERROR = "Failed to bind socket to IP.";
	public static final String INITIAL_CONFIGURATION_ERROR_1 = "Failed forcing GPS data messages on the UAVs";
	public static final String INITIAL_CONFIGURATION_ERROR_2 = "Failed sending the initial configuration to the UAVs";
	public static final String BATTERY_FAILING = "Battery depleting on ";
	public static final String TAKE_OFF_ERROR_1 = "Failed executing the take off of the UAV";
	public static final String WIRELESS_ERROR = "Error. The function Tools.isInRange() must be modified.";
	public static final String DOWNLOAD_ERROR = "Image not available";
	public static final String DISMOUNT_DRIVE_ERROR = "Failed dismounting the virtual RAM drive.";
	
	// System properties
	public static final String HOME_DIR = "user.home";
	public static final String SCROLLBAR_WIDTH = "ScrollBar.width";
	public static final String OPERATING_SYSTEM_WINDOWS = "Windows OS detected.";
	public static final String OPERATING_SYSTEM_LINUX = "Linux OS detected.";
	
	// MAVLink messages
	public static final String WAYPOINT_REACHED = "Reached waypoint";
	
	// MAVLink errors
	public static final String MAVLINK_ERROR = "Unable to stablish MAVLink connection with all UAVs.";
	public static final String GPS_FIX_ERROR = "Unable to get GPS fix from all UAVs.";
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
	public static final String STABILIZE_ALTITUDE = "Altitude stabilized.";
	public static final String STABILIZE_ALTITUDE_ERROR = "Error getting the altitude control.";
	public static final String MOVING_ERROR_1 = "Error changing position.";
	public static final String MOVING_ERROR_2 = "Can't increase speed while moving to a new position.";
	public static final String MOVING_ERROR_3 = "Can't stabilize position in destination position.";
	public static final String MISSION_DELETE = "Previous mission erased.";
	public static final String MISSION_DELETE_ERROR = "Error erasing the current mission.";
	public static final String MISSION_SENT = "Mission sent.";
	public static final String MISSION_SENT_ERROR_1 = "The waypoint list is null or empty.";
	public static final String MISSION_SENT_ERROR_2 = "Takeoff altitude is lower than the minimum flying altitude";
	public static final String MISSION_SENT_ERROR_3 = "Error sending the mission.";
	public static final String MISSION_GET = "Mission retrieved from the UAV.";
	public static final String MISSION_GET_ERROR = "Error retrieving the current mission from the UAV.";
	public static final String SERIAL_ERROR_1 = "Error: Serial port is currently in use.";
	public static final String SERIAL_ERROR_2 = "Error: RXTX only works with serial ports.";
	public static final String SERIAL_ERROR_3 = "Failed conecting to the serial port.";
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

}
