package fishing.logic;


import com.esotericsoftware.kryo.io.Input;

import api.API;
import main.api.ArduSim;
import main.api.GUI;
import main.api.communications.CommLink;

public class FisherReceiverThread  extends Thread{
	
	private byte [] message;
	String messagetxt;
	private int uavID;
	private Input input;
	private CommLink link;
	private GUI gui;
	private ArduSim ardusim;
	public static volatile double[] posBoat;
	public static volatile double angle,heading,radius,boatAltitude;
	public static volatile boolean landSignal;
	
	
	public FisherReceiverThread (int uavID) {
		
		this.uavID = uavID;
		this.message = new byte[FishingParam.DATAGRAM_MAX_LENGTH];	
		this.input = new Input(this.message);
		this.link = API.getCommLink(uavID);
		this.gui = API.getGUI(uavID);
		this.ardusim = API.getArduSim();
		this.messagetxt = "";
		posBoat = new double [2];
	}
	
	@Override
	public void run() {
		
		
		gui.log("Hilo de escucha esperando");
		while(!ardusim.isExperimentInProgress()) {
			ardusim.sleep(100);
		}
		
		gui.log("Hilo de escucha escuchando");
		while(ardusim.isExperimentInProgress()) {
			
			message = link.receiveMessage(uavID);
			
			if ( message != null) {
				input.setBuffer(message);
				input.setPosition(0);
				posBoat[0]=input.readDouble();
				posBoat[1]=input.readDouble();
				boatAltitude = input.readDouble();
				heading = input.readDouble();
				landSignal = input.readBoolean();

			}
			
			
			
		
		
		}
	}
	
}
