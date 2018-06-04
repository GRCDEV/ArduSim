package uavFishing.logic;

import api.Copter;

public class BoatThread extends Thread {
	
	byte[] message;
	int uavID = 0;
	
	@Override
	public void run() {
		
		// Calcular mensaje con posicion barco + velocidad + heading
				
		Copter.sendBroadcastMessage(uavID, message);
		
		
	}
	
}
