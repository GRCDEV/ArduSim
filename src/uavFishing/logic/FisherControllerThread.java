package uavFishing.logic;


import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import main.Param;
import uavController.UAVParam;
import uavFishing.pojo.VectorMath;


public class FisherControllerThread extends Thread{
	
	public static volatile boolean startExperiment = false;
	
	private int uavID;
	private double [] vDirection;
	private boolean waypointReached,batteryAlarm,fligthTimeReached;
	
	
	
	public FisherControllerThread (int uavID) {
		
		this.uavID= uavID;
		this.vDirection = UavFishingParam.vOrigin;
		this.waypointReached = false;
		this.batteryAlarm = false;
		this.fligthTimeReached = false;
	}
	
	@Override
	public void run() {
		
		GeoCoordinates GeoNextPoint,GeoBoatPoint;
		UTMCoordinates UTMActualPoint, UTMPreviousPoint,UTMNextPoint=new UTMCoordinates(0,0);
		long startTime,currentTime;
		double distanceToNextPoint, distanceToBoat,distanceTotal;
		double initialVoltage,minimumVoltage,currentVoltage;
		double coeficienteAverageUsage,coeficientReturnUsage,coeficientEstimate;
		
		
		GUI.log("Dron " + this.uavID + " esperando");
		while(!FisherControllerThread.startExperiment) {
			Tools.waiting(100);
		}
		
		if(!Copter.takeOff(this.uavID, UavFishingParam.UavAltitude))
		{
			GUI.log("Error en el despegue");
			return;
		}
		
		//Battery stats
		if(Param.role ==Tools.SIMULATOR) minimumVoltage = UAVParam.VIRT_BATTERY_ALARM_VOLTAGE;
		else minimumVoltage = UAVParam.lipoBatteryAlarmVoltage; 
		initialVoltage=UAVParam.uavCurrentStatus[this.uavID].getVoltage();
		
		//Initial time
		startTime = System.currentTimeMillis();
		GUI.log("Dron " + this.uavID + " empezando a moverse");
		distanceTotal=0;
		while(Tools.isExperimentInProgress()) {
			
			//Return to boat after a while or when battery is low.
			if(fligthTimeReached || batteryAlarm) {
				
				//Follow boat until end of boat's mission, then land
				while (!FisherReceiverThread.landSignal) {
					
					GeoBoatPoint = Tools.UTMToGeo(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]);
					Copter.moveUAVNonBlocking(this.uavID, GeoBoatPoint, (float)FisherReceiverThread.boatAltitude);
					Tools.waiting(500);
				}
				
				Copter.landUAV(uavID);
				
			}
			else {
				UTMActualPoint=Copter.getUTMLocation(uavID);
				UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
				UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
				GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y );
				Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
				waypointReached = false;
				while(!waypointReached && !fligthTimeReached && !batteryAlarm) {
					Tools.waiting(100);
					UTMPreviousPoint = UTMActualPoint;
					UTMActualPoint = Copter.getUTMLocation(this.uavID);
					distanceTotal+= UTMPreviousPoint.distance(UTMActualPoint);
					distanceToBoat = UTMActualPoint.distance(new UTMCoordinates(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]));
					UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
					UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
					GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y);
					Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
					distanceToNextPoint = UTMActualPoint.distance(UTMNextPoint);
					if(distanceToNextPoint <= UavFishingParam.distanceTreshold) waypointReached = true;
					currentTime = (System.currentTimeMillis() - startTime) / 1000;
					if(currentTime >= UavFishingParam.FLIGTH_MAX_TIME) fligthTimeReached=true;
					
					currentVoltage = UAVParam.uavCurrentStatus[this.uavID].getVoltage();
					coeficienteAverageUsage = (initialVoltage-currentVoltage)/distanceTotal;
					coeficientReturnUsage = (currentVoltage-minimumVoltage)/distanceToBoat;
					coeficientEstimate = coeficientReturnUsage/coeficienteAverageUsage;
					if(coeficientEstimate <= UavFishingParam.estimateTreshold) batteryAlarm=true;
			
				}
				//Rotates the direction vector $rotationAngle degrees
				vDirection = VectorMath.rotateVector(vDirection, UavFishingParam.rotationAngle, UavFishingParam.clockwise);
			}
		}
	}
}
