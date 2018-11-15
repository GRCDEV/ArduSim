package uavFishing.logic;


import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import uavController.UAVCurrentStatus;
import uavFishing.pojo.VectorMath;


public class FisherControllerThread extends Thread{
	
	public static volatile boolean startExperiment = false;
	
	private int uavID;
	private double [] vDirection;
	private boolean waypointReached;
	
	
	
	public FisherControllerThread (int uavID) {
		
		this.uavID= uavID;
		this.vDirection = UavFishingParam.vOrigin;
		this.waypointReached = false;
	}
	
	@Override
	public void run() {
		
		GeoCoordinates GeoNextPoint,GeoBoatPoint;
		UTMCoordinates UTMActualPoint, UTMBoat,UTMNextPoint=new UTMCoordinates(0,0);
		long startTime,currentTime;
		double distanceToNextPoint, distanceToBoat,totalDistance;
		double initialVoltage,minimumVoltage,currentVoltage;
		boolean fligthTimeReached = false;
		
		
		GUI.log("Dron " + this.uavID + " esperando");
		while(!FisherControllerThread.startExperiment) {
			Tools.waiting(100);
		}
		
		if(!Copter.takeOff(this.uavID, UavFishingParam.UavAltitude))
		{
			GUI.log("Error en el despegue");
			return;
		}
		
		//Start flight
		startTime = System.currentTimeMillis();
		GUI.log("Dron " + this.uavID + " empezando a moverse");
		while(Tools.isExperimentInProgress()) {
			
			//Return to boat after a while
			if(fligthTimeReached) {
				
				//Follow boat until end of boat's mission, then land
				while (!FisherReceiverThread.landSignal) {
					
					GeoBoatPoint = Tools.UTMToGeo(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]);
					Copter.moveUAVNonBlocking(this.uavID, GeoBoatPoint, (float)FisherReceiverThread.boatAltitude);
					Tools.waiting(500);
				}
				
				Copter.landUAV(uavID);
				
			}
			else {
				
				UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
				UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
				GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y );
				Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
				waypointReached = false;
				while(!waypointReached && !fligthTimeReached) {
					Tools.waiting(100);
					UTMActualPoint = Copter.getUTMLocation(this.uavID);
					UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
					UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
					GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y);
					Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
					distanceToNextPoint = UTMActualPoint.distance(UTMNextPoint);
					if(distanceToNextPoint <= 10) waypointReached = true;
					currentTime = (System.currentTimeMillis() - startTime) / 1000;
					if(currentTime >= UavFishingParam.FLIGTH_MAX_TIME) fligthTimeReached=true;
			
				}
				//Rotates the direction vector $rotationAngle degrees
				vDirection = VectorMath.rotateVector(vDirection, UavFishingParam.rotationAngle, UavFishingParam.clockwise);
			}
		}
	}
}
