package sim.logic;

import api.Copter;
import api.Tools;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FakeSenderThread extends Thread {
	
	public static boolean fakeCommunicationsEnabled = false;
	public static int period = 200;
	public static int messageSize = 705;
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
		while (true) {//(Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			Copter.sendBroadcastMessage(numUAV, message);
			Tools.waiting(period);
			
//			cicleTime = cicleTime + period;
//			waitingTime = (int)(cicleTime - System.currentTimeMillis());
//			if (waitingTime > 0) {
//				GUIHelper.waiting(waitingTime + (int)(10 * Math.random()));
//			}
			
		}
	}

}
