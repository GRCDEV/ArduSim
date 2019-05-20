package uavFishing.logic;



import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.UTMCoordinates;
import main.communications.CommLink;

public class BoatThread extends Thread {
	
	byte[] message;
	String messagetxt;
	public double heading,altitude;
	UTMCoordinates location;
	int uavID;
	Output output;
	CommLink link;
	
	public BoatThread (int uavID) {
		
		this.uavID= uavID;
		this.message = new byte[UavFishingParam.DATAGRAM_MAX_LENGTH];
		this.messagetxt = "";
		this.output = new Output(message);
		
		this.link = API.getCommLink(uavID);
		
	}
	
	
	@Override
	public void run() {
		
		GUI.log("Boat waiting");
		while (!Tools.isExperimentInProgress()) {
			Tools.waiting(100);
		}
		GUI.log("Boat starts broadcast");
		
		// Crear mensaje con posicion barco + altitud + heading
		while(Tools.isExperimentInProgress()) {
		
		location = Copter.getUTMLocation(uavID);
		altitude = Copter.getZ(uavID);
		heading = Copter.getHeading(uavID);
		
		output.clear();
		output.writeDouble(location.x);
		output.writeDouble(location.y);
		output.writeDouble(altitude);
		output.writeDouble(heading);
		output.writeBoolean(Copter.isLastWaypointReached(uavID));
		output.flush();
		link.sendBroadcastMessage(message);
		
		}
	}
	
}
