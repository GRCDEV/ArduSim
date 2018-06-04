package mbcap.logic;

/** This class allows the asynchronous start of a mission when the UAV is in stabilize mode on the ground. */

import java.util.concurrent.atomic.AtomicInteger;

import api.Copter;

public class StartMissionThread extends Thread {
	
	public static final AtomicInteger UAVS_TESTING = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private StartMissionThread() {}
	
	public StartMissionThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		if (Copter.startMissionFromGround(numUAV)) {
			StartMissionThread.UAVS_TESTING.incrementAndGet();
		}
	}
}
