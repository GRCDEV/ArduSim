package vision.logic;


/** This class includes parameters specifically related to the communication with the flight controller.
 * <p>Developed by: Jamie Wubben, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class visionParam {

	// Parameters used to detect when a UAV reaches the last waypoint
		public static final double LAST_WP_THRESHOLD = 2.0; // (m) Maximum distance considered to assert that the UAV has reached the last waypoint
		public static double ALTITUDE = 20.0;
		public static final String PTYHON_SERVER_IP = "127.0.0.1";
		public static enum status {LAND, DESCEND, MOVE, ROTATE, LOITER};
		
		//default place in the field
		public static double LATITUDE = 39.725319;
		public static double LONGITUDE = -0.733674;
		
		//Values for overriding RC controller
		public static int rollMin;
		public static int rollTrim;
		public static int rollMax;
		public static int rollDeadzone;
		
		public static int pitchMin;
		public static int pitchTrim;
		public static int pitchMax;
		public static int pitchDeadzone;
		
		public static int throttleMin;
		public static int throttleTrim;
		public static int throttleMax;
		public static int throttleDeadzone;
		
		public static int yawMin;
		public static int yawTrim;
		public static int yawMax;
		public static int yawDeadzone;
		
}
