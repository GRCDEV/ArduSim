package fishing.logic;



import com.esotericsoftware.kryo.io.Output;

import api.API;
import es.upv.grc.mapper.Location2DUTM;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;

public class BoatThread extends Thread {
	
	byte[] message;
	String messagetxt;
	public double heading,altitude;
	Location2DUTM location;
	int uavID;
	Output output;
	private CommLink link;
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	
	public BoatThread (int uavID) {
		
		this.uavID= uavID;
		this.message = new byte[FishingParam.DATAGRAM_MAX_LENGTH];
		this.messagetxt = "";
		this.output = new Output(message);
		
		this.link = API.getCommLink(uavID);
		this.copter = API.getCopter(uavID);
		this.gui = API.getGUI(uavID);
		this.ardusim = API.getArduSim();
	}
	
	
	@Override
	public void run() {
		
		gui.log("Boat waiting");
		while (!ardusim.isExperimentInProgress()) {
			ardusim.sleep(100);
		}
		gui.log("Boat starts broadcast");
		
		// Crear mensaje con posicion barco + altitud + heading
		while(ardusim.isExperimentInProgress()) {
		
		location = copter.getLocationUTM();
		altitude = copter.getAltitude();
		heading = copter.getHeading();
		
		output.clear();
		output.writeDouble(location.x);
		output.writeDouble(location.y);
		output.writeDouble(altitude);
		output.writeDouble(heading);
		output.writeBoolean(copter.getMissionHelper().isLastWaypointReached());
		output.flush();
		link.sendBroadcastMessage(message);
		
		}
	}
	
}
