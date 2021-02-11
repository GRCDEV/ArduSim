package com.protocols.mbcap.logic;

/** This class allows the asynchronous start of a main.java.com.protocols.mission when the UAV is in stabilize mode on the ground.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

import com.api.API;

import java.util.concurrent.atomic.AtomicInteger;

public class StartExperimentThread extends Thread {
	
	public static final AtomicInteger UAVS_TESTING = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private StartExperimentThread() {}
	
	/** Starts the experiment in auto mode. */
	public StartExperimentThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		if (API.getCopter(numUAV).getMissionHelper().start()) {
			StartExperimentThread.UAVS_TESTING.incrementAndGet();
		}
		
	}
}
