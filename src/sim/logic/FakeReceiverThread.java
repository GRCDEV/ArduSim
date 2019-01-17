package sim.logic;

import api.Copter;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

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
