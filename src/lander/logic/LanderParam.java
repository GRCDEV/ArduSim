package lander.logic;

import java.util.concurrent.atomic.AtomicIntegerArray;

//import smile.data.SparseDataset;

public class LanderParam {

	
	public static api.pojo.GeoCoordinates LocationStart;
	public static api.pojo.GeoCoordinates LocationEnd;
	
		
	public static int altitude;
	//public static int width;
	public static double distMax;
	public static int otherValue;
	
	public static boolean mIsSimulation;
	public static String LanderDataFile;
	public static LanderSensorInterface sensor;
	
	public static volatile boolean ready;

	//public static SparseDataset measurements;
	public static final double pThreshold = 5.0;
	
	
	// Timeouts
	public static final int RECEIVING_TIMEOUT = 50;			// (ms) The port is unlocked after this time when receiving messages
	public static final int SENDING_TIMEOUT = 200;			// (ms) Time between packets sent
	public static final int TAKE_OFF_CHECK_TIMEOUT = 250;	// (ms) Between checks if the target altitude has been reached
	public static final int MOVE_CHECK_TIMEOUT = 200;		// (ms) Between checks if the target location has been reached
	public static final int LAND_CHECK_TIMEOUT = 250;		// (ms) Between checks if the UAV has landed
	public static final int STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
	// Wait between ACK
		
	
	// Thread and protocol coordination
	public static AtomicIntegerArray state;
	public static AtomicIntegerArray moveSemaphore;
	public static AtomicIntegerArray wpReachedSemaphore;
	
	// Distance from which it is accepted that you have reached the waypoint
	public static final double MIN_DISTANCE_TO_WP = 1.0;
}
