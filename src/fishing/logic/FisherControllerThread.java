package fishing.logic;


import api.API;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Location3D;
import api.pojo.location.Location3DGeo;
import api.pojo.location.Location3DUTM;
import fishing.pojo.VectorMath;
import main.Param;
import main.api.ArduSim;
import main.api.ArduSimNotReadyException;
import main.api.Copter;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.MoveTo;
import main.api.TakeOff;
import main.api.TakeOffListener;
import main.uavController.UAVParam;


public class FisherControllerThread extends Thread{
	
	public static volatile boolean startExperiment = false;
	
	private int uavID;
	private double [] vDirection;
	private boolean waypointReached,batteryAlarm,fligthTimeReached;
	
	private ArduSim ardusim;
	private Copter copter;
	private GUI gui;
	
	public FisherControllerThread (int uavID) {
		
		this.uavID= uavID;
		this.vDirection = FishingParam.vOrigin;
		this.waypointReached = false;
		this.batteryAlarm = false;
		this.fligthTimeReached = false;
		
		this.ardusim = API.getArduSim();
		this.copter = API.getCopter(uavID);
		this.gui = API.getGUI(uavID);
	}
	
	@Override
	public void run() {
		
		Location3DGeo GeoBoatPoint, GeoNextPoint;
		Location2DUTM UTMActualPoint, UTMPreviousPoint=new Location2DUTM(0,0),UTMNextPoint=new Location2DUTM(0,0);
		long startTime,currentTime;
		double distanceToNextPoint, distanceToBoat,distanceTotal;
		double initialVoltage,minimumVoltage,currentVoltage;
		double coeficienteAverageUsage,coeficientReturnUsage,coeficientEstimate;
		
		
		gui.log("Dron " + this.uavID + " esperando");
		while(!FisherControllerThread.startExperiment) {
			ardusim.sleep(100);
		}
		
		TakeOff takeOff = copter.takeOff(FishingParam.UavAltitude, new TakeOffListener() {
			
			@Override
			public void onFailure() {
				gui.log("Error en el despegue");
			}
			
			@Override
			public void onCompleteActionPerformed() {
				// Nothing to do, waiting with Thread.join()
			}
		});
		takeOff.start();
		try {
			takeOff.join();
		} catch (InterruptedException e1) {}
		
		//Battery stats
		if(Param.role ==ArduSim.SIMULATOR) minimumVoltage = UAVParam.VIRT_BATTERY_ALARM_VOLTAGE;
		else minimumVoltage = UAVParam.lipoBatteryAlarmVoltage; 
		initialVoltage=UAVParam.uavCurrentStatus[this.uavID].getVoltage();
		
		//Initial time
		startTime = System.currentTimeMillis();
		gui.log("Dron " + this.uavID + " empezando a moverse");
		distanceTotal=0;
		while(API.getArduSim().isExperimentInProgress()) {
			
			//Return to boat after a while or when battery is low.
			if(fligthTimeReached || batteryAlarm) {
				
				//Follow boat until end of boat's mission, then land
				while (!FisherReceiverThread.landSignal) {
					
					try {
						GeoBoatPoint = Location3DUTM.getGeo3D(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1], FisherReceiverThread.boatAltitude);
						copter.moveTo(GeoBoatPoint);
					} catch (ArduSimNotReadyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ardusim.sleep(500);
				}
				
				copter.land();
				
			}
			else if(FisherReceiverThread.landSignal) {
				
				try {
					Double x = FisherReceiverThread.posBoat[0];
					Double y = FisherReceiverThread.posBoat[1];
					Location3D landPoint = new Location3D(x, y, FisherReceiverThread.boatAltitude);
					
					MoveTo moveTo = copter.moveTo(landPoint, new MoveToListener() {
						
						@Override
						public void onFailure() {
							// TODO Tratar el error
						}
						
						@Override
						public void onCompleteActionPerformed() {
							// No necesario porque esperamos al hilo con Thread.join()
						}
					});
					moveTo.start();
					try {
						moveTo.join();
					} catch (InterruptedException e) {}
					copter.land();						// TODO esto solo se tiene que hacer una vez ¿es así?
				} catch (ArduSimNotReadyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else {
				
				gui.log("");
				gui.log("-------------------------------------------------------------------------------------------------------------");
				gui.log("");
				gui.log("Vector dirección UTM:" + vDirection[0] + ", " +vDirection[1]);
				UTMActualPoint=copter.getLocationUTM();
				UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
				UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
				try {
					GeoNextPoint= new Location3DGeo(UTMNextPoint.getGeo(), FishingParam.UavAltitude);
					gui.log("Moviendose al punto UTM: "+UTMNextPoint.x + ", " + UTMNextPoint.y);
					copter.moveTo(GeoNextPoint);
					waypointReached = false;
					while(!waypointReached && !fligthTimeReached && !batteryAlarm && !FisherReceiverThread.landSignal) {
						ardusim.sleep(100);
						gui.log("#####################################################################################################");
						
						
						UTMPreviousPoint.x = UTMActualPoint.x;
						UTMPreviousPoint.y = UTMActualPoint.y;
						UTMActualPoint = copter.getLocationUTM();
						distanceTotal+= UTMPreviousPoint.distance(UTMActualPoint);
						distanceToBoat = UTMActualPoint.distance(new Location2DUTM(FisherReceiverThread.posBoat[0],FisherReceiverThread.posBoat[1]));
						UTMNextPoint.x = FisherReceiverThread.posBoat[0] + vDirection[0];
						UTMNextPoint.y = FisherReceiverThread.posBoat[1] + vDirection[1];
						GeoNextPoint= new Location3DGeo(UTMNextPoint.getGeo(), FishingParam.UavAltitude);
						gui.log("Moviendose al punto UTM: "+UTMNextPoint.x + ", " + UTMNextPoint.y);
						copter.moveTo(GeoNextPoint);
						
						distanceToNextPoint = UTMActualPoint.distance(UTMNextPoint);
						gui.log("Distancia al siguiente punto UTM (m): " + distanceToNextPoint);
						gui.log("Treshold de distancia con el waypoint: "+ FishingParam.distanceTreshold);
						if(distanceToNextPoint <= FishingParam.distanceTreshold) waypointReached = true;
						
						currentTime = (System.currentTimeMillis() - startTime) / 1000;
						gui.log("Tiempo de vuelo (s): " + currentTime);
						if(currentTime >= FishingParam.FLIGTH_MAX_TIME) fligthTimeReached=true;
						
						currentVoltage = UAVParam.uavCurrentStatus[this.uavID].getVoltage();
						gui.log("Bateria actual (V): " + currentVoltage);
						
						coeficienteAverageUsage = (initialVoltage-currentVoltage)/distanceTotal;
						gui.log("Voltage gastado: " + (initialVoltage-currentVoltage));
						gui.log("Distancia recorrida: " + distanceTotal);
						gui.log("Estimación consumo medio (V/m): " + coeficienteAverageUsage);
						
						gui.log("Distancia al barco (m): " + distanceToBoat);
						coeficientReturnUsage = (currentVoltage-minimumVoltage)/distanceToBoat;
						gui.log("Estimación consumo de retorno al barco (V/m): " + coeficientReturnUsage);
						
						coeficientEstimate = coeficientReturnUsage/coeficienteAverageUsage;
						gui.log("Estimación alarma: " + coeficientEstimate);
						//if(coeficientEstimate <= UavFishingParam.estimateTreshold) batteryAlarm=true;
						gui.log("Baterry: " + batteryAlarm + "   Distance: " + waypointReached + "   Time: " + fligthTimeReached);
						gui.log("\n#####################################################################################################\n");
						//TODO te aconsejo encarecidamente que utilices un StringBuilder para concatenar todo este texto y llames a gui.log() un a sola vez
					}
					//Rotates the direction vector $rotationAngle degrees
					vDirection = VectorMath.rotateVector(vDirection, FishingParam.rotationAngle, FishingParam.clockwise);
					gui.log("\n-------------------------------------------------------------------------------------------------------------\n");
				} catch (ArduSimNotReadyException e) {
					e.printStackTrace();
					API.getGUI(0).log(e.getMessage());
					//TODO quizá convendría enviar orden de volver al barco
				}
			}
		}
	}
}
