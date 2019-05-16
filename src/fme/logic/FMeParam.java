package fme.logic;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.formations.FlightFormation.Formation;
import fme.pojo.RemoteInput;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FMeParam {
	// General parameters
	public static final int MASTER_POSITION = 0; // Position of master UAV into array of UAVs
	public static final long SIMULATION_MASTER_ID = 0;
	public static final String[] MAC = new String[] { "b8:27:eb:57:4c:0e", "b8:27:eb:02:19:5b" };// MACs of master (Hexacopter) with standard format
	public static final long[] MAC_ID = new long[] { 202481591602190L, 202481586018651L };// MACs of master with long format
	
	public static double slavesStartingAltitude = 19;			// (m) Relative altitude where the UAVs finish the take off process
	public static int masterSpeed = 500;						// (cm/s) Maximum speed of the master UAV during flight (in Loiter mode)
	public static final double MIN_DISTANCE_TO_TARGET = 1.0;	// (m) Distance from which it is accepted that you have reached the target
	
	// Simulation parameters
	public static Queue<RemoteInput> masterData = null;					// Real UAV RC values used during a flight
	public static volatile double masterInitialLatitude = 39.482594;	// (degrees) Latitude for simulations
	public static volatile double masterInitialLongitude = -0.346265;	// (degrees) Longitude for simulations
	public static volatile double masterInitialYaw = 0.0;				// (rad) Initial heading of the master UAV for simulations
	
	// Thread coordination
	public static AtomicIntegerArray state;
	
	// Timeouts
	public static final int STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
	public static final int SENDING_TIMEOUT = 200;			// (ms) Time between packets sent
	public static final int RECEIVING_TIMEOUT = 50;			// (ms) The port is unlocked after this time when receiving messages
	public static final long SETUP_TIMEOUT = 2000;			// (ms) Timeout to assert that the configuration step has finished
	public static final int TAKE_OFF_CHECK_TIMEOUT = 250;	// (ms) Between checks if the target altitude has been reached
	public static final int TAKE_OFF_LOG_TIMEOUT = 1000;	// (ms) Between checks to show the current altitude during takeoff
	public static final int HOVERING_TIMEOUT = 500;			// (ms) Waiting time after takeoff before moving to first waypoint
	public static final int MOVE_CHECK_TIMEOUT = 200;		// (ms) Between checks if the target location has been reached
	public static final long TAKEOFF_TIMEOUT = 2000;		// (ms) Timeout to assert that the takeoff has finished
	public static final int LAND_CHECK_TIMEOUT = 250;		// (ms) Between checks if the UAV has landed
	
	// Messages sent
	public static int sendPeriod = 1000;							// (ms) Period between messages from the master during flight
	public static AtomicReference<byte[][]> data;					// Master: array containing the data sent to the slaves
	public static AtomicLongArray idNext;							// id of the next UAV to takeoff
	public static AtomicReferenceArray<Formation> flyingFormation;	// Formation used while flying
	
	
}
