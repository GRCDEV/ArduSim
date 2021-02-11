package com.protocols.mbcap.logic;

import es.upv.grc.mapper.DrawableImageGeo;
import es.upv.grc.mapper.DrawableSymbolGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import com.protocols.mbcap.pojo.Beacon;
import com.protocols.mbcap.pojo.MBCAPState;
import com.protocols.mbcap.pojo.ProgressState;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** This class contains parameters related to MBCAP protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPParam {
	
	// Beaconing parameters
	public static final int MAX_BEACON_LOCATIONS = 59;				// Maximum number of locations that can be sent in the beacon
	public static final double MAX_WAYPOINT_DISTANCE = 400;			// (m) Maximum distance to a waypoint to be included in the beacon while in stand still state
	public static long beaconingPeriod = 200;						// (ms) Time between beacons
	public static int numBeacons = 5;								// Between a new future positions calculus
	public static double hopTime = 0.5;								// (s) Between two predicted positions
	public static long hopTimeNS = (long) (hopTime * 1000000000l);	// (ns) The same in nanoseconds
	public static double minSpeed = 1.0;							// (m/s) To calculate the predicted future positions
	public static long beaconExpirationTime = 10 * 1000000000l;		// (ns) Beacon validity before being ignored
	
	// Collision risk detection parameters
	public static double collisionRiskDistance = 20; 				// (m) Distance between points to assert collision risk (UTM coordinates)
	public static double collisionRiskAltitudeDifference = 50;		// (m) Altitude difference to assert collision risk
	public static long collisionRiskTime = 1 * hopTimeNS;			// (ns) Half of the time range to assert collision risk
	public static long riskCheckPeriod = 1 * 1000000000l;			// (ns) Time between risk collision checks
	public static int packetLossThreshold = 10;						// Number of data packets that can be lost for reception
	public static double gpsError = 2.5;							// (m) GPS error
	public static long standStillTimeout = 5 * 1000000000l;			// (ns) Timeout while the UAV stands still
	public static long passingTimeout = 5 * 1000000000l;			// (ns) Timeout when the UAV starts overtaking the other UAV before checking if it was successful
	public static long resumeTimeout = 4 * 1000000000l;				// (ns) Timeout when going back to normal state before checking if another UAV considers that there is collision risk with you
	public static long recheckTimeout = 5 * 1000;					// (ms) Timeout to check if there is a collision risk with the same UAV, once solved the previous risk
	public static long globalDeadlockTimeout = 120 * 1000000000l;	// (ns) Base timeout to consider a deadlock (maximum time available applying a protocol state)

	// Additional parameters
	public static final long BEACON_EXPIRATION_CHECK_PERIOD = 2 * 1000;	// (ms) Expired beacons removing check frequency
	public static final double DISTANCE_TO_MISSION_END = 2.0;			// (m) Maximum distance to the last waypoint used to stop calculating future positions
	public static final long SHORT_WAITING_TIME = 200; 					// (ms) Waiting timeout between threads
	public static final Stroke STROKE_POINT = new BasicStroke(1f);		// Shown circles format
	// Moving aside parameters
	public static final double EXTRA_ERROR = 1.5;						// (m) Additional distance error for the safety distance (curve compensation)
	public static final double PRECISION_MARGIN = 1;					// (m) In case a precision error happens during breaking
	public static double safePlaceDistance = 2 * MBCAPParam.gpsError + EXTRA_ERROR + PRECISION_MARGIN;// (m) Minimum safety distance to the other UAV path
	public static final double SAFETY_DISTANCE_RANGE = 1;				// (m) Maximum distance to consider that the UAV has reached the safety position
	public static final double STABILIZATION_SPEED = 0.6;				// (m/s) When it is stopped
	public static final long STABILIZATION_WAIT_TIME = 200;				// (ms) Time passively waiting the UAV to stop
	public static final long STABILIZATION_TIMEOUT = 10 * 1000000000l;	// (ns) Global timeout while waiting the UAV to stop
	
	// Parameter to decide whether the predicted path must be projected over the theoretical main.java.com.protocols.mission or not.
	public static AtomicInteger[] projectPath;		// 1 means project, 0 means do not project
	
	// Equation parameters for the minimum distance between a UAV and a main.java.com.protocols.mission segment when near of a waypoint to avoid the other UAV when moving towards the next segment
	//  d=f(speed, angle between main.java.com.protocols.mission segments)
	public static final double[][] FUNCTION_DISTANCE_VS_SPEED = new double[][] {{0, 0, 0, 0},
		{0.37783, 0.06666666666666, -0.0085714285714262, 0.059047619047619},
		{0.74388, 0.16428571428571, -0.06, 0.112},
		{1.13956, 0.17857142857144, -0.09571428571429, 0.148},
		{1.27618, 0.2190476190476, -0.10428571428571, 0.15180952380952}};	// d = [1] + [2] * speed + [3] * speed^2 for each angle [0] in radians
	public static final double[][] FUNCTION_DISTANCE_VS_ALPHA = new double[][] {{0, 0, 0, 0},
		{2.5, -0.0041484183177452, 1.6173509925798, -0.60732719765111},
		{5, -0.028184494059779, 4.7381059350027, 1.3302321210998},
		{7.5, -0.030910306099195, 10.557940446505, -3.3652974603568},
		{10, -0.093797386913902, 18.318356442452, -5.5283946551365},
		{12.5, -0.16173390395179, 28.802004999439, -8.4664805885628},
		{15, -0.23662178632741, 42.085324900865, -12.306741240534}};	// d = [1] + [2] * angle + [3] * angle^2 for each speed [0] in m/s
	// Equation parameters for the minimum distance to a waypoint where the UAV starts to change the trajectory depending on the speed
	public static final double[] FUNCTION_WAYPOINT_THRESHOLD = new double[] {0.61904761904762, -0.44142857142857, 0.49466666666666667};	// d = [1] + [2] * speed + [3] * speed^2
	
	// Data structures for storing data
	public static AtomicInteger[] event;						// Event number included in the beacon
	public static AtomicInteger[] deadlockSolved;				// Number of deadlock events solved
	public static AtomicInteger[] deadlockFailed;				// Number of deadlock events failed
	public static AtomicLong[] idAvoiding;						// UAV with risk of collision included in the beacon
	public static final long ID_AVOIDING_DEFAULT = -1;			// Default value (not avoiding collision)
	public static final int POINTS_SIZE = 59;					// Initial predicted positions list size
	public static final int DISTANCES_SIZE = 20;				// Initial distances list size, used to calculate the predicted points
	public static AtomicReference<Beacon>[] selfBeacon;			// Last sent beacon from the current UAV
	public static Map<Long, Beacon>[] beacons;					// Beacons received from other UAVs
	public static List<Beacon>[] beaconsStored;					// Used to log the sent beacons
	public static List<ProgressState>[] progress;				// Used to register when the state of the protocol is modified
	public static Map<Long, Location3DUTM>[] impactLocationUTM;			// Detected risk location in UTM coordinates
	public static Map<Long, DrawableImageGeo>[] impactLocationScreen;	// Detected risk location shown on screen
	public static AtomicReference<Location2DUTM>[] targetLocationUTM;	// Safety location to move towards (UTM coordinates)
	public static AtomicReference<DrawableSymbolGeo>[] targetLocationScreen;	// Safety location to move towards (screen coordinates)
	
	/** Current protocol state for each UAV. */
	public static AtomicReference<MBCAPState>[] state;

}
