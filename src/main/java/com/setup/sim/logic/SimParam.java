package com.setup.sim.logic;

import com.api.API;
import com.api.pojo.location.LogPoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** This class contains parameters related to the simulation platform.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SimParam {

	// path of top-level .properties
	public static File resourcesFile;
	static String fs = File.separator;
	public static final File missionParameterFile = new File(API.getFileTools().getSourceFolder().toString()
			+ fs + "main" + fs + "resources" + fs + "protocols" + fs +  "mission" + fs + "missionParam.properties");
	// path to protocol specific parameter file
	public static File protocolParamFile;
	// Detects when the communications are online
	public static volatile boolean communicationsOnline = false;
	
	// Performance parameters
	public static volatile boolean arducopterLoggingEnabled = false;

	public static BufferedImage arrowImage; // arrow image

	// Administrator user check and ImDisk installation check and drive parameters
	public static boolean userIsAdmin;			// Only initialized if using simulation, not in a real UAV
	public static final String FAKE_FILE_NAME = "ArduSimFakeFile(remove when found).txt";
	public static boolean imdiskIsInstalled;	// Only initialized if using simulation under Windows, not in a real UAV
	public static final String IMDISK_PATH = "\\system32\\imdisk.exe";
	public static final String IMDISK_REGISTRY_PATH = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ImDisk";
	public static final String IMDISK_REGISTRY_KEY = "DisplayName";
	public static final String IMDISK_REGISTRY_VALUE = "ImDisk Virtual Disk Driver";
	public static final String IMDISK_REGISTRY_PATH2 = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ImDiskApp";
	public static final String IMDISK_REGISTRY_VALUE2 = "ImDisk Toolkit";
	public static final String RAM_DRIVE_NAME = "ArduSimTemp";	// Maximux size = 11 characters
	public static final String[] WINDOWS_DRIVES = new String[] {"Z", "Y", "X", "W", "V", "U", "T", "S", "R", "Q", "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F", "E", "D", "C", "B", "A"};
	public static final int UAV_RAM_DRIVE_SIZE = 5;			// (MB) RAM drive size needed by one UAV (without logging)
	public static final int LOGGING_RAM_DRIVE_SIZE = 45;	// (MB) RAM drive size needed by one UAV for logging purposes
	public static final int MIN_FAT32_SIZE = 260;			// (MB) RAM drive minimum size when using FAT32 file system (65527clusters*4KB)
	public static boolean usingRAMDrive = false;
	
	// Cygwin and SITL location parameters and temporary folders
	public static final String CYGWIN_PATH1 = "C:" + File.separator + "cygwin" + File.separator
			+ "bin" + File.separator + "bash.exe";	// 32bits platform
	public static final String CYGWIN_PATH2 = "C:" + File.separator + "cygwin64" + File.separator
			+ "bin" + File.separator + "bash.exe";	// 64bits platform
	public static volatile String cygwinPath = null;	// Final file path found
	public static final String SITL_WINDOWS_FILE_NAME = "arducopter.exe";
	public static final String SITL_LINUX_FILE_NAME = "arducopter";
	public static volatile String sitlPath = null;	// Final SITL file path
	public static final String PARAM_FILE_NAME = "copter.parm";
	public static volatile String paramPath = null;	// Final copter parameters file path
	public static Process[] processes;				// Processes used to launch SITL
	public static String tempFolderBasePath;
	public static final String LOG_FOLDER = "logs";
	public static final String LOG_SOURCE_FILENAME = "00000001";
	public static final String LOG_SOURCE_FILEEXTENSION = "BIN";
	public static final String LOG_DESTINATION_FILENAME = "ArduCopter_log";
	public static final String TEMP_FOLDER_PREFIX = "virtual_uav_temp_";

	public static final long CONSOLE_READ_RETRY_WAITING_TIME = 100; // (ms)
	public static final long SITL_STARTING_TIMEOUT = 20 * 1000000000L; // (ns)
	
	// Parameters needed to draw and store the log of the UAVs path
	// Persistent storage for the UTM coordinates drawn. Used to rescale drawing and to store log
	public static List<LogPoint>[] uavUTMPath;
	public static final int PATH_INITIAL_SIZE = 1000; // Initial size of the lists

	// UAV drawing parameters
	public static final String UAV_IMAGE_PATH = "main" + fs + "resources" + fs + "setup" + fs + "UAV.png";// "main/resources/setup/UAV.png"; // UAV image path
	public static BufferedImage uavImage; // UAV image
	public static final int UAV_PX_SIZE = 25; // (px) UAV screen size

	// Drawing parameters:
	public static int screenUpdatePeriod = 500;					// (ms) Time between screen updates
	public static final int MIN_SCREEN_UPDATE_PERIOD = 100;		// (ms)   minimum value
	public static final int MAX_SCREEN_UPDATE_PERIOD = 2000;	// (ms)   maximum value
	public static int minScaleUpdatePeriod = 1000;				// (ms) Minimum time between two consecutive map scale updates
	public static String bingKey = null;						// Bing key loaded from ardusim.ini to use Bing images as background
	public static int failedMapDownloadCheckPeriod = -1;		// (days) Number of days before checking again if a missing map image is available. A negative value means never check again if it is available. 0 means always check if it is available.
	public static double minScreenMovement = 5.0;							// (px) Minimum traveled distance to add a new segment to the path of the UAV
	public static final double MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD = 100.0;	// (px)   maximum value
	
	// Array with the available colors to identify each UAV. Used for missions or other protocol elements
	public static final Color[] COLOR = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA,
			Color.ORANGE, Color.PINK, Color.YELLOW };
	
	// Screen level where elements are drawn
	public static final int WIND_LEVEL = Integer.MAX_VALUE;
	public static final int UAV_LEVEL = Integer.MAX_VALUE - 2;
	public static final int COLLISION_LEVEL = Integer.MAX_VALUE - 3;
	
	public static final int MISSION_LEVEL = -5;
	public static final int PATH_LEVEL = -4;

	// Information shown in the progress dialog
	public static double[] xUTM, yUTM, z;
	public static double[] speed;

	// Waiting timeout between threads
	public static final long SHORT_WAITING_TIME = 200;	// (ms)
	public static final long LONG_WAITING_TIME = 1000;	// (ms)

	// Prefix added to log lines to identify each UAV
	public static String[] prefix;
	
	// Progress dialog parameters
	public static final int DIALOG_WIDTH = 280; // (px) Desired dialog width
	
}
