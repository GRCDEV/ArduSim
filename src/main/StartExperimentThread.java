package main;

/** This class allows the asynchronous start of a mission when the UAV is in stabilize mode on the ground.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

import java.util.concurrent.atomic.AtomicInteger;

import api.Copter;

public class StartExperimentThread extends Thread {
	
	public static final AtomicInteger UAVS_TESTING = new AtomicInteger();
	
	private int numUAV;
	private Double altitude;
	
	@SuppressWarnings("unused")
	private StartExperimentThread() {}
	
	/** Starts the experiment in auto mode if altitude==null, otherwise starts with general takeoff in guided mode. */
	public StartExperimentThread(int numUAV, Double altitude) {
		this.numUAV = numUAV;
		this.altitude = altitude;
	}

	@Override
	public void run() {
		if (this.altitude == null) {
			if (Copter.startMissionFromGround(numUAV)) {
				StartExperimentThread.UAVS_TESTING.incrementAndGet();
			}
		} else {
			if (Copter.takeOffNonBlocking(numUAV, altitude)) {
				StartExperimentThread.UAVS_TESTING.incrementAndGet();
			}
		}
		
	}
}
