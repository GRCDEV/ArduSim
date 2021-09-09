package com.protocols.followme.logic;

import com.protocols.followme.pojo.RemoteInput;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class FollowMeParam {
	// General parameters
	public static double slavesStartingAltitude = 19;	// (m) Relative altitude where the UAVs finish the take off process
	public static int masterSpeed = 500;				// (cm/s) Maximum speed of the master UAV during flight (in Loiter mode)
	public static long sendPeriod = 1000;				// (ms) Period between messages from the master during flight
	
	// Simulation parameters
	public static Queue<RemoteInput> masterData = null;					// Real UAV RC values used during a flight
	public static volatile double masterInitialLatitude = 39.482594;	// (degrees) Latitude for simulations
	public static volatile double masterInitialLongitude = -0.346265;	// (degrees) Longitude for simulations
	public static volatile double masterInitialYaw = 0.0;				// (rad) Initial heading of the master UAV for simulations
	public static double altitude = 60;
	
	/** Current protocol state for each UAV. */
	public static AtomicInteger[] state;
	
	// Timeouts
	public static final long STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
	public static final long SENDING_TIMEOUT = 200;			// (ms) Time between packets sent
	public static final long LAND_CHECK_TIMEOUT = 250;		// (ms) Between checks if the UAV has landed
	
}
