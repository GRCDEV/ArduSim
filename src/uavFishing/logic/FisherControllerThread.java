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
		UTMCoordinates UTMActualPoint, UTMPreviousPoint=new UTMCoordinates(0,0),UTMNextPoint=new UTMCoordinates(0,0);
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
			else if(FisherReceiverThread.landSignal) {
				
				GeoBoatPoint = Tools.UTMToGeo(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]);
				//Copter.moveUAVNonBlocking(this.uavID, GeoBoatPoint, (float)FisherReceiverThread.boatAltitude);
				Copter.moveUAV(this.uavID, GeoBoatPoint, (float)FisherReceiverThread.boatAltitude, 1, 0);
				Copter.landUAV(uavID);
				
			}
			else {
				GUI.log("");
				GUI.log("-------------------------------------------------------------------------------------------------------------");
				GUI.log("");
				GUI.log("Vector dirección UTM:" + vDirection[0] + ", " +vDirection[1]);
				UTMActualPoint=Copter.getUTMLocation(uavID);
				UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
				UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
				GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y );
				GUI.log("Moviendose al punto UTM: "+UTMNextPoint.x + ", " + UTMNextPoint.y);
				Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
				waypointReached = false;
				while(!waypointReached && !fligthTimeReached && !batteryAlarm && !FisherReceiverThread.landSignal) {
					Tools.waiting(100);
					GUI.log("#####################################################################################################");
					
					
					UTMPreviousPoint.x = UTMActualPoint.x;
					UTMPreviousPoint.y = UTMActualPoint.y;
					UTMActualPoint = Copter.getUTMLocation(this.uavID);
					distanceTotal+= UTMPreviousPoint.distance(UTMActualPoint);
					distanceToBoat = UTMActualPoint.distance(new UTMCoordinates(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]));
					UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
					UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
					GeoNextPoint=Tools.UTMToGeo(UTMNextPoint.x,UTMNextPoint.y);
					GUI.log("Moviendose al punto UTM: "+UTMNextPoint.x + ", " + UTMNextPoint.y);
					Copter.moveUAVNonBlocking(this.uavID, GeoNextPoint, (float)UavFishingParam.UavAltitude);
					
					distanceToNextPoint = UTMActualPoint.distance(UTMNextPoint);
					GUI.log("Distancia al siguiente punto UTM (m): " + distanceToNextPoint);
					GUI.log("Treshold de distancia con el waypoint: "+ UavFishingParam.distanceTreshold);
					if(distanceToNextPoint <= UavFishingParam.distanceTreshold) waypointReached = true;
					
					currentTime = (System.currentTimeMillis() - startTime) / 1000;
					GUI.log("Tiempo de vuelo (s): " + currentTime);
					if(currentTime >= UavFishingParam.FLIGTH_MAX_TIME) fligthTimeReached=true;
					
					currentVoltage = UAVParam.uavCurrentStatus[this.uavID].getVoltage();
					GUI.log("Bateria actual (V): " + currentVoltage);
					
					coeficienteAverageUsage = (initialVoltage-currentVoltage)/distanceTotal;
					GUI.log("Voltage gastado: " + (initialVoltage-currentVoltage));
					GUI.log("Distancia recorrida: " + distanceTotal);
					GUI.log("Estimación consumo medio (V/m): " + coeficienteAverageUsage);
					
					GUI.log("Distancia al barco (m): " + distanceToBoat);
					coeficientReturnUsage = (currentVoltage-minimumVoltage)/distanceToBoat;
					GUI.log("Estimación consumo de retorno al barco (V/m): " + coeficientReturnUsage);
					
					coeficientEstimate = coeficientReturnUsage/coeficienteAverageUsage;
					GUI.log("Estimación alarma: " + coeficientEstimate);
					//if(coeficientEstimate <= UavFishingParam.estimateTreshold) batteryAlarm=true;
					GUI.log("Baterry: " + batteryAlarm + "   Distance: " + waypointReached + "   Time: " + fligthTimeReached);
					GUI.log("");
					GUI.log("#####################################################################################################");
					GUI.log("");
			
				}
				//Rotates the direction vector $rotationAngle degrees
				vDirection = VectorMath.rotateVector(vDirection, UavFishingParam.rotationAngle, UavFishingParam.clockwise);
				GUI.log("");
				GUI.log("-------------------------------------------------------------------------------------------------------------");
				GUI.log("");
			}
		}
	}
}
