package sim.board;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import api.pojo.LogPoint;

/** This class contains the parameters that change the behaviour of the panel used to show the UAVs movement.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class BoardParam {

	// Screen update parameters
	public static int screenDelay = 500; // (ms) between updates
	public static final int MIN_SCREEN_DELAY = 100;
	public static final int MAX_SCREEN_DELAY = 2000;
	public static double minScreenMovement = 5.0; // (px) min. pixels displacement to move the UAV
	public static final double MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD = 100.0;
	
	// Wind parameters
	public static BufferedImage arrowImageRotated; // main window arrow image
	
	// UAV real path to be drawn
	public static List<Shape>[] uavPXPathLines;
	
	// Scale bar parameters
	public static final int SCALE_ORIGIN = 20; // (px) Distance to the upper-left corner
	public static final int MIN_PX_SCALE_LENGTH = 100; // (px) Minimum length of the bar
	public static final int MAX_PX_SCALE_LENGTH = 300; // (px) Maximum length of the bar
	public static final int HALF_LINE_LENGTH = 8; // (px) Half of the vertical separation line length
	public static final int TEXT_OFFSET = 10; // (px) Distance between the bar and the text
	public static final Color SCALE_COLOR = Color.DARK_GRAY; // Color of the bar
	// Panel dimensions (image to draw on the main panel)
	public static final int MIN_SCALE_PANEL_HEIGHT = SCALE_ORIGIN + HALF_LINE_LENGTH + 15;
	public static final int MIN_SCALE_PANEL_WIDTH = SCALE_ORIGIN + MAX_PX_SCALE_LENGTH + 100;
	public static volatile BufferedImage scaleBarImage = null;
	
	// Lines format
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
	public static final Stroke STROKE_TRACK = new BasicStroke(3f);
	private static final float[] DASHING_PATTERN = { 2f, 2f };
	public static final Stroke STROKE_WP_LIST = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
	DASHING_PATTERN, 2.0f);
	
	// Previous and current UAV position in UTM and Screen coordinates
	public static LogPoint[] uavPrevUTMLocation;
	public static LogPoint[] uavPrevPXLocation;
	public static LogPoint[] uavCurrentUTMLocation;
	public static LogPoint[] uavCurrentPXLocation;
	
	// UTM-Screen coordinate scale calculus
	public static double xUTMmin = Double.POSITIVE_INFINITY;	// Minimum and maximum values of the UTM rectangle including elements to be drawn
	public static double xUTMmax = Double.NEGATIVE_INFINITY;
	public static double yUTMmin = Double.POSITIVE_INFINITY;
	public static double yUTMmax = Double.NEGATIVE_INFINITY;
	public static final double SCALE_MAGNIFIER = 1.2; // Makes room between objects and the panel limits
	public static final double MAX_HALF_RANGE = 500; // (m) Minimum distance to the panel limits when no missions are loaded
	public static final double MIN_SEPARATION = 100; // (m) Minimum surface covered with UAVs when no missions are loaded
	public static double boardUTMx0, boardUTMy0; // (m) Bottom-left corner UTM coordinates
	public static double screenScale; // pixels/UTM scale
	public static volatile boolean drawAll = false; // It's time to start drawing
	public static AtomicInteger numUAVsDrawn = new AtomicInteger();	// Number of UAVs currently drawn
	public static volatile int numMissionsDrawn = 0; // Number of missions currently drawn
	public static AtomicInteger rescaleQueries = new AtomicInteger(); // Number of rescale queries. Used when loading a mission, or maybe when a UAV goes near the panel limits
	
	// Background map parameters
	public static double boardUpLeftUTMX;
	public static double boardUpLeftUTMY; // (m) Upper-left panel corner UTM coordinates
	public static final int MAX_IMAGE_PX = 640; // (px) Maximum image size allowed by Google Static Maps in freeware mode
	public static BackgroundMap[][] map; // Set of tiles that conform the map
	public static String[][] mapDownloadErrorText;	// Set of tiles of text to be shown when the download fails
	public static short[] brightness = new short[256]; // Auxiliar array to brighten the map
	
	// Text shown in the lower-left corner
	public static AtomicReference<String> panelText = new AtomicReference<>();

}
