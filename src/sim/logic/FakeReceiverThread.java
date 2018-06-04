package sim.logic;

import api.Copter;
import main.Param;

public class FakeReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	
	@SuppressWarnings("unused")
	private FakeReceiverThread() {}
	
	public FakeReceiverThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		
		while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			Copter.receiveMessage(numUAV);
		}
	}

	
}
