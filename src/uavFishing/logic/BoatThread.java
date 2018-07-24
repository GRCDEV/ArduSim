package uavFishing.logic;



import com.esotericsoftware.kryo.io.Output;
import api.Copter;
import api.Tools;

public class BoatThread extends Thread {
	
	byte[] message;

	int uavID;
	Output output;
	
	public BoatThread (int uavID) {
		
		this.uavID= uavID;
		this.message = new byte[UavFishingParam.DATAGRAM_MAX_LENGTH];	
		this.output = new Output(message);
		
	}
	
	
	@Override
	public void run() {
		
		// Calcular mensaje con posicion barco + velocidad + heading
		while(Tools.isExperimentInProgress()) {
		output.clear();
		output.writeDouble(Copter.getUTMLocation(uavID).getX());
		output.writeDouble(Copter.getUTMLocation(uavID).getY());
		output.writeDouble(UavFishingParam.heading);
		output.writeDouble(UavFishingParam.radius);
		output.writeDouble(UavFishingParam.angle);
		output.flush();
		
		Copter.sendBroadcastMessage(uavID, message);
		
		}
	}
	
}
