package mission;

/** This class allows the asinchronous start of a mission when the UAV is in stabilize mode on the ground. */

import java.util.concurrent.atomic.AtomicInteger;

import api.API;
import uavController.UAVParam;

public class StartMissionTestThread extends Thread {
	
	public static final AtomicInteger UAVS_TESTING = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private StartMissionTestThread() {}
	
	public StartMissionTestThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		StartMissionTestThread.startMissionTest(numUAV);
	}
	
	public static void startMissionTest(int numUAV) {
		// Documentation says: While on the ground, 1st arm, 2nd auto mode, 3rd some throttle, and the mission begins
		//    If the copter is flying, the take off waypoint will be considered to be completed, and the UAV goes to the next waypoint
		if (
				API.setMode(numUAV, UAVParam.Mode.STABILIZE)
				&& API.armEngines(numUAV)
				&& API.setMode(numUAV, UAVParam.Mode.AUTO)
				&& API.setThrottle(numUAV)
				//&& API.armEngines(numUAV)
				//&& API.setThrottle(numUAV)
				//&& API.setMode(numUAV, UAVParam.Mode.AUTO_ARMED)
				) {
			StartMissionTestThread.UAVS_TESTING.incrementAndGet();
		}
		
	}
}
