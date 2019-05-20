package uavFishing.logic;


import com.esotericsoftware.kryo.io.Input;

import api.API;
import api.Copter;
import api.GUI;
import api.Tools;
import main.communications.CommLink;

public class FisherReceiverThread  extends Thread{
	
	private byte [] message;
	String messagetxt;
	private int uavID;
	private Input input;
	private CommLink link;
	public static volatile double[] posBoat;
	public static volatile double angle,heading,radius,boatAltitude;
	public static volatile boolean landSignal;
	
	
	public FisherReceiverThread (int uavID) {
		
		this.uavID = uavID;
		this.message = new byte[UavFishingParam.DATAGRAM_MAX_LENGTH];	
		this.input = new Input(this.message);
		this.link = API.getCommLink(uavID);
		this.messagetxt = "";
		this.posBoat = new double [2];
	}
	
	@Override
	public void run() {
		
		
		GUI.log("Hilo de escucha esperando");
		while(!Tools.isExperimentInProgress()) {
			Tools.waiting(100);
		}
		
		GUI.log("Hilo de escucha escuchando");
		int i=0;
		while(Tools.isExperimentInProgress()) {
			
			message = link.receiveMessage(uavID);
			input.setBuffer(message);
			if ( message != null) {
				
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
