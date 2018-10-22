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
		
		GeoCoordinates GeoNextPoint;
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
		GUI.log("Dron " + this.uavID + "empezando a calcular waypoints");
		while(Tools.isExperimentInProgress()) {
			
			// Calcular nueva posición
			
			UTMActualPoint = Copter.getUTMLocation(uavID);
			
			vPosOrigin = VectorMath.rotateVector(vPosOrigin, UavFishingParam.angle, UavFishingParam.clockwise);
			GUI.log("Vector calculado: "+ vPosOrigin[0] + "," + vPosOrigin[1]);
			UTMBoat = Copter.getUTMLocation(UavFishingParam.boatID);
			UTMNextPoint.x = UTMBoat.x + vPosOrigin[0];
			UTMNextPoint.y = UTMBoat.y + vPosOrigin[1];
			GUI.log("UTMcoordenadas calculadas: " + UTMNextPoint.x + "," + UTMNextPoint.y);
			GeoNextPoint=Tools.UTMToGeo((UTMBoat.x + vPosOrigin[0]),UTMBoat.y + vPosOrigin[1]);
			GUI.log("Geocoordenadas calculadas: " + GeoNextPoint.latitude + "," + GeoNextPoint.longitude);
			GUI.log("Uav " + this.uavID + ": Moviendose al punto " + GeoNextPoint.latitude + ", " + GeoNextPoint.longitude);
			Copter.moveUAVNonBlocking(uavID, GeoNextPoint, 25);
			while(UTMActualPoint.distance(UTMNextPoint) > 0 ) {
				GUI.log("Distancia al siguiente punto: " + UTMActualPoint.distance(UTMNextPoint));
				GUI.log("Punto actual: " + UTMActualPoint.x + "," + UTMActualPoint.y);
//				GUI.log("Distancia al barco: " + UTMActualPoint.distance(UTMBoat));
				UTMBoat = Copter.getUTMLocation(UavFishingParam.boatID);
				UTMActualPoint = Copter.getUTMLocation(this.uavID);
				Tools.waiting(5000);
			}
//			Copter.moveUAV(uavID, GeoNextPoint, 5, 0.95*distance, 25);
			
			
			
		
			
		}
		
	}
}
