package scanv2.logic;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.AtomicDoubleArray;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;

public class ScanParam {
	
	// Global parameters
	// Initial distance between UAV when they are on the ground (only in simulation)
	public static int initialDistanceBetweenUAV = 1;
	// Distance between UAVs while following the mission
	public static int initialDistanceBetweenUAVreal = 5;
	
	public static final int MASTER_POSITION = 0; // Position of master UAV into array of UAVs
	public static final long SIMULATION_MASTER_ID = 0;
	public static volatile Long idMaster = null; // Id of the master UAV (known by the master real UAV or in simulations)
	public static final String[] MAC = new String[] { "b8:27:eb:74:0c:d1", "00:c0:ca:90:32:05" };// MACs of master with standard format
	public static final long[] MAC_ID = new long[] { 202481593486545L, 202481593486545L };// MACs of master with long format
	public static final long BROADCAST_MAC_ID = 281474976710655L; // Broadcast MAC (0xFFFFFFFFFFFF)
	
	public static volatile double masterHeading; // Master UAV Heading//TODO necessary adapt for real UAVs

	// Timeouts
	public static final int RECEIVING_TIMEOUT = 50;			// (ms) The port is unlocked after this time when receiving messages
	public static final int SENDING_TIMEOUT = 200;			// (ms) Time between packets sent
	public static final int TAKE_OFF_CHECK_TIMEOUT = 250;	// (ms) Between checks if the target altitude has been reached
	public static final int MOVE_CHECK_TIMEOUT = 200;		// (ms) Between checks if the target location has been reached
	public static final int LAND_CHECK_TIMEOUT = 250;		// (ms) Between checks if the UAV has landed
	public static final int STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
	// Wait between ACK
	
	// Data beacon info
	public static AtomicLongArray idPrev;					// id of the previous UAV to takeoff
	public static AtomicLongArray idNext;					// id of the next UAV to takeoff
	public static AtomicDoubleArray takeoffAltitude;		// (m) Altitude for the takeoff
	public static AtomicReferenceArray<byte[]> data;		// Master: array containing the data sent to the slaves
	public static volatile int masterPosition;				// Master: position of master in the previous array
	public static AtomicReferenceArray<Point3D[]> uavMissionReceivedUTM; // Mission for each UAV in UTM coordinates
	public static AtomicReferenceArray<GeoCoordinates[]> uavMissionReceivedGeo; // Matrix with individual missions of each Drone GEO
	// Maximum number of waypoints
	public static final int MAX_WAYPOINTS = 59;	// MAX_MTU - 2 - 8x4 - 4 - 2xn = 18 Bytes remaining
	
	// Thread and protocol coordination
	public static AtomicIntegerArray state;
	public static AtomicIntegerArray moveSemaphore;
	public static AtomicIntegerArray wpReachedSemaphore;
	
	// Distance from which it is accepted that you have reached the waypoint
	public static final double MIN_DISTANCE_TO_WP = 1.0;

}
