package uavFishing.logic;

import api.Tools;

public class FisherControllerThread extends Thread{
	
	double angle = 0;
	
	
	@Override
	public void run() {
		
		while(Tools.isExperimentInProgress()) {
			
			angle += UavFishingParam.heading;
			
			// Calcular nueva posición
			
			//API.moveUAV(numUAV, geo, relAltitude, destThreshold, altThreshold)
			
			//
			
		}
		
	}
}
