package uavFishing.logic;

import api.API;
import main.Param;
import main.Param.SimulatorState;

public class FisherReceiverThread  extends Thread{
	byte [] message;
	@Override
	public void run() {
		
		while(Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			
			message = API.receiveMessage(1);
		
		
		}
	}
	
}
