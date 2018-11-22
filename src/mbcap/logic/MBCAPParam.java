package mbcap.logic;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import mbcap.pojo.Beacon;
import mbcap.pojo.ProgressState;

/** This class contains parameters related to MBCAP protocol.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class MBCAPParam {
	
	// Beaconing parameters
	public static int beaconingPeriod = 200;						// (ms) Time between beacons
	public static int numBeacons = 5;								// Between a new future positions calculus
	public static double hopTime = 0.5;								// (s) Between two predicted positions
	public static long hopTimeNS = (long) (hopTime * 1000000000l);	// (ns) The same in nanoseconds
	public static double minSpeed = 1.0;							// (m/s) To calculate the predicted future positions
	public static long beaconExpirationTime = 10 * 1000000000l;		// (ns) Beacon validity before being ignored
	
	// Collision risk detection parameters
	public static double collisionRiskDistance = 20; 				// (m) Distance between points to assert collision risk (UTM coordinates)
	public static double collisionRiskScreenDistance;				// (px) The previous distance, but in screen coordinates
	public static double collisionRiskAltitudeDifference = 50;		// (m) Altitude difference to assert collision risk
	public static long collisionRiskTime = 1 * hopTimeNS;			// (ns) Half of the time range to assert collision risk
	public static long riskCheckPeriod = 1 * 1000000000l;			// (ns) Time between risk collision checks
	public static int packetLossThreshold = 10;						// Number of data packets that can be lost for reception
	public static double gpsError = 2.5;							// (m) GPS error
	public static long standStillTimeout = 5 * 1000000000l;			// (ns) Timeout while the UAV stands still
	public static long passingTimeout = 5 * 1000000000l;			// (ns) Timeout when the UAV starts overtaking the other UAV before checking if it was successful
	public static long resumeTimeout = 4 * 1000000000l;				// (ns) Timeout when going back to normal state before checking if another UAV considers that there is collision risk with you
	public static long recheckTimeout = 5 * 1000;					// (ms) Timeout to check if there is a collision risk with the same UAV, once solved the previous risk
	public static long globalDeadlockTimeout = 120 * 1000000000l;// (ns) Base timeout to consider a deadlock (maximum time available applying a protocol state)

	// Additional parameters
	public static final long BEACON_EXPIRATION_CHECK_PERIOD = 2 * 1000;	// (ms) Expired beacons removing check frequency
	public static final double DISTANCE_TO_MISSION_END = 2.0;			// (m) Maximum distance to the last waypoint used to stop calculating future positions
	public static final int SHORT_WAITING_TIME = 200; 					// (ms) Waiting timeout between threads
	public static final Stroke STROKE_POINT = new BasicStroke(1f);		// Shown circles format
	// Moving aside parameters
	public static double safePlaceDistance = 2 * MBCAPParam.gpsError;	// (m) Minimum safety distance to the other UAV path
	public static final double PRECISION_MARGIN = 0.5;					// (m) In case a precision error occurs
	public static final double SAFETY_DISTANCE_RANGE = 1;				// (m) Maximum distance to consider that the UAV has reached the safety position
	public static final double STABILIZATION_SPEED = 0.2;				// (m/s) When it is stopped
	public static final int STABILIZATION_WAIT_TIME = 200;				// (ms) Time passively waiting the UAV to stop
	public static final long STABILIZATION_TIMEOUT = 30 * 1000000000l;	// (ns) Global timeout while waiting the UAV to stop
	
	// Parameter to decide whether the predicted path must be projected over the theoretical mission or not.
	public static AtomicIntegerArray projectPath;		// 1 means project, 0 means do not project
	
	// Data structures for storing data
	public static AtomicIntegerArray event;						// Event number included in the beacon
	public static AtomicLongArray idAvoiding;					// UAV with risk of collision included in the beacon
	public static final long ID_AVOIDING_DEFAULT = -1;			// Default value (not avoiding collision)
	public static final int POINTS_SIZE = 59;					// Initial predicted positions list size
	public static final int DISTANCES_SIZE = 20;				// Initial distances list size, used to calculate the predicted points
	public static AtomicReferenceArray<Beacon> selfBeacon;		// Last sent beacon from the current UAV
	public static Map<Long, Beacon>[] beacons;					// Beacons received from other UAVs
	public static List<Beacon>[] beaconsStored;					// Used to log the sent beacons
	public static List<ProgressState>[] progress;				// Used to register when the state of the protocol is modified
	public static Map<Long, Point3D>[] impactLocationUTM;		// Detected risk location in UTM coordinates
	public static Map<Long, Point2D.Double>[] impactLocationPX;	// Detected risk location shown on screen
	public static AtomicReferenceArray<UTMCoordinates> targetLocationUTM;	// Safety location to move towards (UTM coordinates)
	public static AtomicReferenceArray<Point2D.Double> targetLocationPX;	// Safety location to move towards (screen coordinates)
	
	public static MBCAPState[] state; // Protocol state included in the beacon
	
	// MBCAP finite state machine states enumerator
	public enum MBCAPState {
		NORMAL((short)1, MBCAPText.STATE_NORMAL),
		STAND_STILL((short)2, MBCAPText.STATE_STAND_STILL),
		MOVING_ASIDE((short)3, MBCAPText.STATE_MOVING_ASIDE),
		GO_ON_PLEASE((short)4, MBCAPText.STATE_GO_ON_PLEASE),
		OVERTAKING((short)5, MBCAPText.STATE_OVERTAKING),
		EMERGENCY_LAND((short)6, MBCAPText.STATE_EMERGENCY_LAND);
		
		private final short id;
		private final String name;
		private MBCAPState(short id, String name) {
			this.id = id;
			this.name = name;
		}
		public short getId() {
			return this.id;
		}
		public String getName() {
			return this.name;
		}
		public static MBCAPState getSatateById(short id) {
			for (MBCAPState p : MBCAPState.values()) {
				if (p.getId() == id) {
					return p;
				}
			}
			return null;
		}
	}
	
}
