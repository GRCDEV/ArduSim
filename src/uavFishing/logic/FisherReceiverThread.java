package uavFishing.logic;


import com.esotericsoftware.kryo.io.Input;
import api.Copter;
import api.GUI;
import api.Tools;

public class FisherReceiverThread  extends Thread{
	
	private byte [] message;
	private int uavID;
	Input input;
	public double[] posReferencia;
	public double angle,heading,radius;
	
	
	public FisherReceiverThread (int uavID) {
		
		this.uavID = uavID;
		this.message = new byte[UavFishingParam.DATAGRAM_MAX_LENGTH];	
		this.input = new Input(message);
		this.posReferencia = new double [2];
	}
	
	@Override
	public void run() {
		
		
		GUI.log("Hilo de escucha esperando");
		while(!Tools.isExperimentInProgress()) {
			Tools.waiting(100);
		}
		
		GUI.log("Hilo de escucha empezando a escuchar");
		while(Tools.isExperimentInProgress()) {
			
			message = Copter.receiveMessage(0);
			if ( message != null) {
				input.setPosition(0);
				posReferencia[0]=input.readDouble();
				posReferencia[1]=input.readDouble();
				heading = input.readDouble();
				angle = input.readDouble();
				radius = input.readDouble();
			}
			
			
		
		
		}
	}
	
}
