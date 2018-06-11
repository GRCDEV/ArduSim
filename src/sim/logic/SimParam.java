package sim.logic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import api.pojo.LogPoint;
import main.Text;

/** This class contains parameters related to the simulation platform. */

public class SimParam {

	// Whether the progress dialog is showing or not
	public static volatile boolean progressShowing = false;
	
	// Performance parameters
	public static volatile boolean arducopterLoggingEnabled = false;
	// Rendering quality levels
	public static volatile RenderQuality renderQuality = RenderQuality.Q3;
	public enum RenderQuality {
		Q1(0, Text.RENDER_QUALITY1),
		Q2(1, Text.RENDER_QUALITY2),
		Q3(2, Text.RENDER_QUALITY3),
		Q4(3, Text.RENDER_QUALITY4);
		
		private final int id;
		private final String name;
		private RenderQuality(int id, String name) {
			this.id = id;
			this.name = name;
		}
		public int getId() {
			return this.id;
		}
		public String getName() {
			return this.name;
		}
		public static RenderQuality getHighestIdRenderQuality() {
			RenderQuality res = null;
			for (RenderQuality rq : RenderQuality.values()) {
				if (res == null) {
					res = rq;
				} else {
					if (rq.id >= res.id) {
						res = rq;
					}
				}
			}
			return res;
		}
		public static String getRenderQualityNameById(int id) {
			for (RenderQuality rq : RenderQuality.values()) {
				if (rq.getId() == id) {
					return rq.getName();
				}
			}
			return "";
		}
		public static RenderQuality getRenderQualityByName(String name) {
			for (RenderQuality rq : RenderQuality.values()) {
				if (rq.name.equals(name)) {
					return rq;
				}
			}
			return null;
		}
	}

	// Wind parameters
	public static final String ARROW_IMAGE_PATH = "/files/wind.png"; // arrow image path
	public static BufferedImage arrowImage; // arrow image
	public static final int ARROW_PANEL_SIZE = 36; // (px) arrow image size

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

	public static final int CONSOLE_READ_RETRY_WAITING_TIME = 100; // (ms)
	public static final long SITL_STARTING_TIMEOUT = 20 * 1000000000l; // (ns)
	
	// Parameters needed to draw and store the log of the UAVs path
	// Received UTM positions, time and speed, and whether they are received during the experiment
	public static BlockingQueue<LogPoint>[] uavUTMPathReceiving;
	public static final int UAV_POS_QUEUE_INITIAL_SIZE = 5000; // Initial size of the queue
	public static final int UAV_POS_QUEUE_FULL_THRESHOLD = 500; // Minimum queue free space
	// Persistent storage for the UTM coordinates drawn. Used to rescale drawing and to store log
	public static List<LogPoint>[] uavUTMPath;
	public static final int PATH_INITIAL_SIZE = 1000; // Initial size of the lists

	// UAV drawing parameters
	public static final String UAV_IMAGE_PATH = "files/UAV.png"; // UAV image path
	public static BufferedImage uavImage; // UAV image
	public static final double UAV_PX_SIZE = 25.0; // (px) UAV screen size
	public static double uavImageScale; // Scale to be applied to the UAV (original size-screen size)

	// Lines format
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
	// Array with the available colors to identify each UAV. Used for missions or other protocol elements
	public static final Color[] COLOR = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA,
			Color.ORANGE, Color.PINK, Color.YELLOW };

	// Information shown in the progress dialog
	public static double[] xUTM, yUTM, z;
	public static double[] speed;

	// Geographic zone and letter of the UTM projection. Gathered the first time a location is received from the UAV
	public static int zone = -1;
	public static char letter;

	public static int boardPXWidth, boardPXHeight; // (px) Board panel size
	
	// Waiting timeout between threads
	public static final int SHORT_WAITING_TIME = 200; // (ms)
	public static final int LONG_WAITING_TIME = 1000;  // (ms)

	// Prefix added to log lines to identify each UAV
	public static String[] prefix;
	
	// Progress dialog parameters
	public static final int DIALOG_WIDTH = 280; // (px) Desired dialog width
	
}
