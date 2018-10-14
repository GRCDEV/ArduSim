package uavFishing.logic;

import java.awt.geom.Point2D;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
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
		Point2D.Double UTMActualPoint,UTMNextPoint;
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
			
			UTMActualPoint=Copter.getUTMLocation(uavID);
			vPosOrigin = VectorMath.rotateVector(vPosOrigin, UavFishingParam.angle, UavFishingParam.clockwise);
			UTMNextPoint = new Point2D.Double(UTMActualPoint.getX()+vPosOrigin[0], UTMActualPoint.getY()+vPosOrigin[1]);
			distance = UTMActualPoint.distance(UTMNextPoint);
			GeoNextPoint=Tools.UTMToGeo((Copter.getUTMLocation(UavFishingParam.boatID).getX() + vPosOrigin[0]),Copter.getUTMLocation(UavFishingParam.boatID).getY() + vPosOrigin[1]);
			Copter.moveUAVNonBlocking(uavID, GeoNextPoint, 5);
//			Copter.moveUAV(uavID, GeoNextPoint, 5, 0.95*distance, 0);
			
			
			
			
			//
			
		}
		
	}
}
