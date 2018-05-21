package uavFishing.logic;

import api.API;

public class BoatThread extends Thread {
	
	byte[] message;
	int uavID = 0;
	
	@Override
	public void run() {
		
		// Calcular mensaje con posicion barco + velocidad + heading
				
		API.sendBroadcastMessage(uavID, message);
		
		
	}
	
}
