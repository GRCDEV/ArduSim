package mbcap.logic;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import mbcap.pojo.Beacon;
import mbcap.pojo.ProgressState;

/** This class contains parameters related to MBCAP protocol. */

public class MBCAPParam {
	
	// Maximum distance to the last waypoint used to stop calculating future positions
	public static final double DISTANCE_TO_MISSION_END = 2.0;

	// Protocol progress parameter
	public static List<ProgressState>[] progress;

	// Beaconing parameters
	public static int beaconingPeriod = 200;						// (ms) Time between beacons
	public static int numBeacons = 5;								// Between a new future positions calculus
	public static long beaconExpirationTime = 10 * 1000000000l;		// (ns) Beacon validity before being ignored
	public static final long BEACON_EXPIRATION_CHECK_PERIOD = 2 * 1000;	// (ms) Expired beacons removing check frequency
	public static double beaconFlyingTime = 25;						// (s) Included in predicted positions
	public static double hopTime = 0.5;								// (s) Between two predicted positions
	public static long hopTimeNS = (long) (hopTime * 1000000000l);	// (ns) The same in nanoseconds
	public static final int MAX_POINTS = 119;						// Maximum number of predicted points that fit on a UDP frame

	public static double minSpeed = 1.0; // (m/s) To calculate the predicted future positions
	public static AtomicIntegerArray event; // Event number included in the beacon
	public static MBCAPState[] state; // Protocol state included in the beacon
	public static AtomicLongArray idAvoiding; // UAV with risk of collision included in the beacon
	public static final long ID_AVOIDING_DEFAULT = -1; // Default value (not avoiding collision)
	public static final int POINTS_SIZE = 70; // Initial predicted positions list size
	public static final int DISTANCES_SIZE = 20; // Initial distances list size, used to calculate the predicted points
	// Parameter to decide whether the predicted path must be projected over the theoretical mission or not.
	public static AtomicIntegerArray projectPath;		// 1 means project, 0 means do not project

	// Collision risk detection parameters
	public static double collisionRiskDistance = 20; // (m) Distance between points to assert collision risk (UTM coordinates)
	public static double collisionRiskScreenDistance; // (px) The previous distance, but in screen coordinates
	public static long collisionRiskTime = 1 * hopTimeNS; // (ns) Half of the time range to assert collision risk
	public static double collisionRiskAltitudeDifference = 50; // (m) Altitude difference to assert collision risk
	public static double reactionDistance = 90; // (m) Distance between the UAV and the collision risk point to assert collision risk
	public static long riskCheckPeriod = 2 * 1000000000l; // (ns) Time between risk collision checks
	public static AtomicReferenceArray<Beacon> selfBeacon;
	// Beacons transmitted by the broker to the CollisionDetector
	public static Map<Long, Beacon>[] beacons;
	// Parameters that store the point where the collision risk is detected
	public static Map<Long, Point3D>[] impactLocationUTM;
	public static Map<Long, Point2D.Double>[] impactLocationPX;
	
	// Parameters related to the actions undertaken when a collision risk takes place
	public static double safePlaceDistance = 20; // (m) Minimum safety distance to the other UAV path
	public static final double PRECISION_MARGIN = 0.5; // (m) In case a precision error occurs
	public static final double SAFETY_DISTANCE_RANGE = 1; // (m) Maximum distance to consider that the UAV has reached the safety position
	public static Point2D.Double[] targetPointUTM;			// Safety position to move towards (UTM coordinates)
	public static GeoCoordinates[] targetPointGeo;	// Safety position to move towards (Mercator coordinates)
	// Stabilization parameters (when the UAV is moving aside)
	public static final double STABILIZATION_SPEED = 0.2;				// (m/s) When it is stopped
	public static final int STABILIZATION_WAIT_TIME = 200;				// (ms) Passively waiting the UAV to stop
	public static final long STABILIZATION_TIMEOUT = 30 * 1000000000l;	// (ns) Global timeout while waiting the UAV to stop

	public static long standStillTimeout = 5 * 1000000000l; // (ns) Timeout while the UAV stands still
	public static long passingTimeout = 3 * 1000000000l; // (ns) Timeout when the UAV starts overtaking the other UAV before checking if it was successful
	public static long solvedTimeout = 4 * 1000000000l; // (ns) Timeout when going back to normal state before checking if another UAV considers that there is collision risk with you
	public static long globalDeadlockTimeout = 1 * 50 * 1000000000l; // (ns) Base timeout to consider a deadlock (maximum time available applying a protocol state)
	public static long[] uavDeadlockTimeout;	// (ns) Previous timeout, but adapted to each UAV
	public static final long DEADLOCK_TIMEOUT_BASE = 20 *  1000000000l; // (ns) Added during the calculus

	public static List<Beacon>[] beaconsStored; // To log the sent beacons
	
	// Waiting timeout between threads
	public static final int SHORT_WAITING_TIME = 200; // (ms)
	
	// Lines format
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
	
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
