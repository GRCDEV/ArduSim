package uavFishing.logic;


import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import uavFishing.pojo.VectorMath;


public class FisherControllerThread extends Thread{
	
	
	private int uavID;
	private double [] vPosOrigin;
	
	
	
	public FisherControllerThread (int uavID) {
		
		this.uavID= uavID;
		this.vPosOrigin = UavFishingParam.vOrigin;
	}
	
	@Override
	public void run() {
		
		GeoCoordinates GeoNextPoint,GeoActualPoint;
		UTMCoordinates UTMActualPoint, UTMBoat,UTMNextPoint=new UTMCoordinates(0,0);
		double distance;
		GUI.log("Dron " + this.uavID + " esperando");
		while(!Tools.isExperimentInProgress()) {
			Tools.waiting(100);
		}
		
		if(!Copter.takeOff(this.uavID, 25))
		{
			GUI.log("Error en el despegue");
			return;
		}
		GUI.log("Dron " + this.uavID + " empezando a moverse");
		while(Tools.isExperimentInProgress()) {
			
			// Calcular nueva posición
			
			UTMActualPoint = Copter.getUTMLocation(this.uavID);
			vPosOrigin = VectorMath.rotateVector(vPosOrigin, UavFishingParam.angle, UavFishingParam.clockwise);
			UTMBoat = Copter.getUTMLocation(UavFishingParam.boatID);
			UTMNextPoint.x = UTMBoat.x + vPosOrigin[0];
			UTMNextPoint.y = UTMBoat.y + vPosOrigin[1];
			GeoNextPoint=Tools.UTMToGeo((UTMBoat.x + vPosOrigin[0]),UTMBoat.y + vPosOrigin[1]);
			distance = UTMActualPoint.distance(UTMNextPoint);
			Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, 25);
			while(distance > 1) {
				
				UTMBoat = Copter.getUTMLocation(UavFishingParam.boatID);
				UTMActualPoint = Copter.getUTMLocation(this.uavID);
				distance = UTMActualPoint.distance(UTMNextPoint);
				
			}

			
		}
		
	}
}
