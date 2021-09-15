package com.setup.sim.logic;

import com.api.API;
import com.api.copter.CopterParam;
import com.api.hiddenFunctions.HiddenFunctions;
import com.uavController.UAVParam;

import java.util.concurrent.atomic.AtomicInteger;

/** This class forces all UAVs to send their current location, asynchronously.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class GPSEnableThread extends Thread {
	
	public static final AtomicInteger GPS_FORCED = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private GPSEnableThread() {}
	
	public GPSEnableThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		GPSEnableThread.forceGPS(numUAV);
	}
	
	/** Forces a specific UAV to send GPS position. */
	public static void forceGPS(int numUAV) {
		if(!HiddenFunctions.requestForMessage(UAVParam.globalPositionIntId,numUAV,true)) {
			System.out.println("error requesting for message");
		}
		if (API.getCopter(numUAV).setParameter(CopterParam.POSITION_FREQUENCY, 4)) {
			GPSEnableThread.GPS_FORCED.incrementAndGet();
		}
	}
	
}
