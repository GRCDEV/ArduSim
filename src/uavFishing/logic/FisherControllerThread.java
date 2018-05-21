package uavFishing.logic;

import main.Param;
import main.Param.SimulatorState;
import api.API;

public class FisherControllerThread extends Thread{
	
	double angle = 0;
	
	
	@Override
	public void run() {
		
		while(Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			
			angle += UavFishingParam.heading;
			
			// Calcular nueva posición
			
			//API.moveUAV(numUAV, geo, relAltitude, destThreshold, altThreshold)
			
			//
			
		}
		
	}
}
