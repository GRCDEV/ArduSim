package uavFishing.logic;


import com.esotericsoftware.kryo.io.Input;
import api.Copter;
import api.GUI;
import api.Tools;

public class FisherReceiverThread  extends Thread{
	
	private byte [] message;
	String messagetxt;
	private int uavID;
	private Input input;
	public static volatile double[] posBoat;
	public static volatile double angle,heading,radius,altitude;
	public static volatile boolean landSignal;
	
	
	public FisherReceiverThread (int uavID) {
		
		this.uavID = uavID;
		this.message = new byte[UavFishingParam.DATAGRAM_MAX_LENGTH];	
		this.input = new Input(this.message);
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
			
			message = Copter.receiveMessage(uavID);
			input.setBuffer(message);
			if ( message != null) {
				
				input.setPosition(0);
				posBoat[0]=input.readDouble();
				posBoat[1]=input.readDouble();
				altitude = input.readDouble();
				heading = input.readDouble();
				landSignal = input.readBoolean();

			}
			
			
			
		
		
		}
	}
	
}
