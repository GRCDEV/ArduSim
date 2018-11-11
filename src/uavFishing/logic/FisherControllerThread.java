package uavFishing.logic;


import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import uavFishing.pojo.VectorMath;


public class FisherControllerThread extends Thread{
	
	public static volatile boolean startExperiment = false;
	
	private int uavID;
	private double [] vPosOrigin;
	private boolean waypointReached;
	
	
	
	public FisherControllerThread (int uavID) {
		
		this.uavID= uavID;
		this.vPosOrigin = UavFishingParam.vOrigin;
	}
	
	@Override
	public void run() {
		
		GeoCoordinates GeoNextPoint,GeoBoatPoint;
		UTMCoordinates UTMActualPoint, UTMBoat,UTMNextPoint=new UTMCoordinates(0,0);
		long startTime,currentTime;
		double distance;
		int timeWaiting;
		GUI.log("Dron " + this.uavID + " esperando");
		while(!FisherControllerThread.startExperiment) {
			Tools.waiting(100);
		}
		
		if(!Copter.takeOff(this.uavID, UavFishingParam.UavAltitude))
		{
			GUI.log("Error en el despegue");
			return;
		}
		startTime = System.currentTimeMillis();
		GUI.log("Dron " + this.uavID + " empezando a moverse");
		while(Tools.isExperimentInProgress()) {
			
			
			currentTime = (System.currentTimeMillis() - startTime) / 1000;
			if(currentTime > 300) {
				
				while (!FisherReceiverThread.landSignal) {
					
					GeoBoatPoint = Tools.UTMToGeo(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]);
					Copter.moveUAVNonBlocking(this.uavID, GeoBoatPoint, (float)FisherReceiverThread.boatAltitude);
					Tools.waiting(500);
				}
				
				Copter.landUAV(uavID);
				
			}
			else {
			
			// Calcular nueva posición
				vPosOrigin = VectorMath.rotateVector(vPosOrigin, UavFishingParam.rotationAngle, UavFishingParam.clockwise);
				
				UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vPosOrigin[0];
				UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vPosOrigin[1];
				GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y );
				Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
				timeWaiting=0;
				waypointReached = false;
				while(!waypointReached) {
					UTMActualPoint = Copter.getUTMLocation(this.uavID);

					UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vPosOrigin[0];
					UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vPosOrigin[1];
					GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y);
					Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
					distance = UTMActualPoint.distance(UTMNextPoint);
					if(distance <= 5 || timeWaiting>=8000) waypointReached = true;
					Tools.waiting(100);
					timeWaiting+=100;

				}	
			}
		}
	}
}
