package sim.logic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import api.pojo.LogPoint;

/** This class contains parameters related to the simulation platform. */

public class SimParam {

	// Whether the progress dialog is showing or not
	public static volatile boolean progressShowing = false;

	// Wind parameters
	public static final String ARROW_IMAGE_PATH = "/files/wind.png"; // arrow image path
	public static BufferedImage arrowImage; // arrow image
	public static final int ARROW_PANEL_SIZE = 36; // (px) arrow image size

	// Cygwin and SITL location parameters and temporary folders
	public static final String CYGWIN_PATH1 = "C:" + File.separator + "cygwin" + File.separator
			+ "bin" + File.separator + "bash.exe";	// 32bits platform
	public static final String CYGWIN_PATH2 = "C:" + File.separator + "cygwin64" + File.separator
			+ "bin" + File.separator + "bash.exe";	// 64bits platform
	public static volatile String cygwinPath = null;	// Final file path found
	public static final String SITL_WINDOWS_FILE_NAME = "ArduCopter.elf";
	public static final String SITL_LINUX_FILE_NAME = "arducopter";
	public static volatile String sitlPath = null;	// Final SITL file path
	public static final String PARAM_FILE_NAME = "copter.parm";
	public static volatile String paramPath = null;	// Final copter parameters file path
	public static Process[] processes;				// Processes used to launch SITL
	public static final String TEMP_FOLDER_PREFIX = "virtual_uav_temp_";

	public static final int CONSOLE_READ_RETRY_WAITING_TIME = 100; // (ms)
	public static final long SITL_STARTING_TIMEOUT = 10 * 1000000000l; // (ns)

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

}
