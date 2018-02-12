package sim.logic;

import api.API;
import api.GUIHelper;
import main.Param;

public class FakeSenderThread extends Thread {
	
	public static int period;
	public static int messageSize;
	private int numUAV; // UAV identifier, beginning from 0

	@SuppressWarnings("unused")
	private FakeSenderThread() {}

	public FakeSenderThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
//		int waitingTime;
		byte[] message = new byte[messageSize];
//		long cicleTime = System.currentTimeMillis();
		while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			API.sendBroadcastMessage(numUAV, message);
			GUIHelper.waiting(period);
			
//			cicleTime = cicleTime + period;
//			waitingTime = (int)(cicleTime - System.currentTimeMillis());
//			if (waitingTime > 0) {
//				GUIHelper.waiting(waitingTime + (int)(10 * Math.random()));
//			}
			
		}
	}

}
