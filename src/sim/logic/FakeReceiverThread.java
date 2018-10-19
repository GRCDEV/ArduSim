package sim.logic;

import api.Copter;

public class FakeReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	
	@SuppressWarnings("unused")
	private FakeReceiverThread() {}
	
	public FakeReceiverThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		
		while (true) {//(Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			Copter.receiveMessage(numUAV);
//			Tools.waiting(100);
		}
	}

	
}
