package uavFishing.logic;

import api.Copter;
import api.Tools;

public class FisherReceiverThread  extends Thread{
	byte [] message;
	@Override
	public void run() {
		
		while(Tools.isExperimentInProgress()) {
			
			message = Copter.receiveMessage(1);
		
		
		}
	}
	
}
