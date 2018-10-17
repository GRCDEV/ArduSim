package uavFishing.logic;

import java.awt.geom.Point2D;

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
		UTMCoordinates UTMActualPoint,UTMNextPoint, UTMBoat;
		double distance;
		
		while(!Tools.isExperimentInProgress()) {
			Tools.waiting(100);
		}
		
		if(!Copter.takeOff(uavID, 5))
		{
			GUI.log("Error en el despegue");
			return;
		}
		while(Tools.isExperimentInProgress()) {
			
			// Calcular nueva posición
			
			UTMActualPoint = Copter.getUTMLocation(uavID);
			vPosOrigin = VectorMath.rotateVector(vPosOrigin, UavFishingParam.angle, UavFishingParam.clockwise);
			UTMNextPoint = new UTMCoordinates(UTMActualPoint.x+vPosOrigin[0], UTMActualPoint.y+vPosOrigin[1]);
			distance = UTMActualPoint.distance(UTMNextPoint);// no parece que la utilices para nada
			
			UTMBoat = Copter.getUTMLocation(UavFishingParam.boatID);
			GeoNextPoint=Tools.UTMToGeo((UTMBoat.x + vPosOrigin[0]),UTMBoat.y + vPosOrigin[1]);
			Copter.moveUAVNonBlocking(uavID, GeoNextPoint, 5);
//			Copter.moveUAV(uavID, GeoNextPoint, 5, 0.95*distance, 0);
			
			
			
			
			//
			
		}
		
	}
}
