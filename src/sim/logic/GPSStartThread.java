package sim.logic;

import java.util.concurrent.atomic.AtomicInteger;

import api.Copter;
import uavController.UAVParam.ControllerParam;

/** This class forces all UAVs to send their current location, asynchronously. */

public class GPSStartThread extends Thread {
	
	public static final AtomicInteger GPS_FORCED = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private GPSStartThread() {}
	
	public GPSStartThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		GPSStartThread.forceGPS(numUAV);
	}
	
	/** Forces a specific UAV to send GPS position. */
	public static void forceGPS(int numUAV) {
		if (Copter.setParameter(numUAV, ControllerParam.POSITION_FREQUENCY, 4)) {
			GPSStartThread.GPS_FORCED.incrementAndGet();
		}
	}
	
}
