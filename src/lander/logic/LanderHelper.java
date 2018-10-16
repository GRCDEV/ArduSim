package lander.logic;

import static lander.pojo.State.*;
import static scanv2.pojo.State.FINISH;
import static scanv2.pojo.State.LANDING;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.swing.JFrame;
import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;

import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import lander.gui.LanderConfiDialog;
import main.Text;
import noneTest.Nonev2Param;
import scanv2.logic.ScanParam;
import scanv2.logic.ScanText;
import sim.board.BoardPanel;
import sim.logic.SimParam;
import uavController.UAVParam;

public class LanderHelper extends ProtocolHelper{

	@Override
	public void setProtocol() {
		// TODO Auto-generated method stub
		
		this.protocolString = "Lander Solutions";
		
	}

	@Override
	public boolean loadMission() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		// TODO Auto-generated method stub
		LanderConfiDialog ConfigDialog = new LanderConfiDialog();
		ConfigDialog.setVisible(true);
		
	}

	@Override
	public void initializeDataStructures() {
		// TODO Auto-generated method stub
		GUI.log("Lander setup parameters");
		
		
		try {
			LanderParam.sensor = LanderParam.mIsSimulation ? new LanderSensorRecognition() : null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GUI.log("Lander sensor setup done.");
		LanderParam.ready = false;
		
		int numUAVs = Tools.getNumUAVs();
		/*
		LanderParam.idPrev = new AtomicLongArray(numUAVs);
		LanderParam.idNext = new AtomicLongArray(numUAVs);
		LanderParam.takeoffAltitude = new AtomicDoubleArray(numUAVs);
		LanderParam.data = new AtomicReferenceArray<>(numUAVs);
		LanderParam.uavMissionReceivedUTM = new AtomicReferenceArray<>(numUAVs);
		LanderParam.uavMissionReceivedGeo = new AtomicReferenceArray<>(numUAVs);*/
		
		LanderParam.state = new AtomicIntegerArray(numUAVs);
		LanderParam.moveSemaphore = new AtomicIntegerArray(numUAVs);
		LanderParam.wpReachedSemaphore = new AtomicIntegerArray(numUAVs);
		
		
	}

	@Override
	public String setInitialState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rescaleDataStructures() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadResources() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rescaleShownResources() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(LanderParam.LocationStart, 0.0);
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
		
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// TODO Auto-generated method stub
		//return false;
		return true;
	}

	@Override
	public void startThreads() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setupActionPerformed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startExperimentActionPerformed() {
		// TODO Auto-generated method stub
		int c=0;
		
		
		int numUAVs = Tools.getNumUAVs();
		GUI.log("NUMERO DE UAV: " + numUAVs);
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(LanderParam.STATE_CHANGE_TIMEOUT);
		}
		
		
		/** TAKING OFF PHASE */
		//GUI.log(Tools.getNumUAVs(), ScanText.TAKING_OFF);
		double altitude = 5; //LanderParam.altitude;  //Tomo una altitud referencial menor a la declarada para el vuelo 
												//esto es para cuando se eleva y busca formarciòn
		double thresholdAltitude = altitude * 0.95;
		
		LanderParam.state.set(numUAVs-1, TAKING_OFF);
		
		GUI.log("1- Estado de esta variable:" + LanderParam.state.get(numUAVs-1)); //Estado 4
		
		if (!Copter.takeOffNonBlocking(numUAVs-1, altitude)) {
			GUI.exit(LanderText.TAKE_OFF_ERROR + " " + 3);
		}
		//GUI.logVerbose(Tools.getNumUAVs(), ScanText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		int waitingTime;
		
		while (LanderParam.state.get(numUAVs-1) == TAKING_OFF) {
			// Discard message=>
			//Copter.receiveMessage(Tools.getNumUAVs()-1, Nonev2Param.RECEIVING_TIMEOUT);
			// Wait until target altitude is reached
			c=c+1;
			//GUI.log("Estoy adentro while: c=" + c);
			
			if (System.currentTimeMillis() - cicleTime > LanderParam.TAKE_OFF_CHECK_TIMEOUT) {
				GUI.logVerbose(SimParam.prefix[numUAVs-1] + Text.ALTITUDE_TEXT
						+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAVs-1].getZ())
						+ " " + Text.METERS);
				if (UAVParam.uavCurrentData[numUAVs-1].getZRelative() >= thresholdAltitude) {
					LanderParam.state.set(numUAVs-1, FOLLOWING_MISSION);
					GUI.log("Cambie de estado en" + c); //0:00:13 Cambie de estado en585391619
					
				} else {
					cicleTime = cicleTime + LanderParam.TAKE_OFF_CHECK_TIMEOUT;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		/*
		if (!Copter.moveUAV(numUAVs-1, LanderParam.LocationEnd, LanderParam.altitude, 20, 2)) {
		}*/
		
		
		while (LanderParam.state.get(numUAVs-1) == FOLLOWING_MISSION) {
			/** MOVE_TO_WP PHASE */
			GUI.updateProtocolState(Tools.getNumUAVs()-1, LanderText.MOVE_TO_WP);
			GUI.log(Tools.getNumUAVs()-1, LanderText.MOVE_TO_WP);
			GUI.logVerbose(Tools.getNumUAVs()-1, LanderText.LISTENER_WAITING);
			
			//GeoCoordinates geo = missionGeo[currentWP];
			GeoCoordinates geo = LanderParam.LocationEnd;
			UTMCoordinates utm = Tools.geoToUTM(geo.latitude, geo.longitude);
			Point2D.Double destination = new Point2D.Double(utm.x, utm.y);
			
			//double relAltitude = mission[currentWP].z;
			//UAVParam.uavCurrentData[numUAVs-1].getZRelative()
			double relAltitude = LanderParam.altitude;
			if (!Copter.moveUAVNonBlocking(Tools.getNumUAVs()-1, geo, (float)relAltitude)) {
				//GUI.exit(ScanText.MOVE_ERROR + " " + selfId);
			}
			cicleTime = System.currentTimeMillis();
			GUI.log("antes de entrar al while" + LanderParam.moveSemaphore.get(Tools.getNumUAVs()-1)); //Estado 4
			
			while (LanderParam.moveSemaphore.get(Tools.getNumUAVs()-1) == 0) {
				// Discard message
				Copter.receiveMessage(Tools.getNumUAVs()-1, LanderParam.RECEIVING_TIMEOUT);
				// Wait until target location is reached
				if (System.currentTimeMillis() - cicleTime > LanderParam.MOVE_CHECK_TIMEOUT) {
					if (UAVParam.uavCurrentData[Tools.getNumUAVs()-1].getUTMLocation().distance(destination) <= LanderParam.MIN_DISTANCE_TO_WP
							&& Math.abs(relAltitude - UAVParam.uavCurrentData[Tools.getNumUAVs()-1].getZRelative()) <= 0.2) 
					{
						LanderParam.moveSemaphore.incrementAndGet(Tools.getNumUAVs()-1);
						
						//GUI.log("Entre y cambio de estado"); //Estado 4
						LanderParam.state.set(numUAVs-1, LANDING);
					} 
					else 
					{
						
						//GUI.log("Espero un tiempo"); //Estado 4
						
						cicleTime = cicleTime + LanderParam.MOVE_CHECK_TIMEOUT;
						waitingTime = (int)(cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
		}
		
		
		
		
		//Copter.moveUAV(numUAVs, LanderParam.LocationEnd, LanderParam.altitude, 20, 2);
		
		
		//if (!Copter.takeOff(0, LanderParam.altitude) || !Copter.moveUAV(Tools.getNumUAVs()-1, LanderParam.LocationEnd, LanderParam.altitude, 20, 2)) {
			//Tratar error
		//}
		
		//Copter.moveUAV(numUAV, geo, relAltitude, destThreshold, altThreshold)moveUAV()
		
		
		//Copter.setFlightMode(0, FlightMode.LAND_ARMED);
		
		
		/** LANDING PHASE */
		GUI.updateProtocolState(numUAVs-1, LanderText.LANDING_UAV);
		if (!Copter.setFlightMode(numUAVs-1, FlightMode.LAND_ARMED)) {
			GUI.exit(LanderText.LAND_ERROR + " ");
		}
		GUI.log(numUAVs-1, LanderText.LANDING);
		GUI.logVerbose(numUAVs-1, LanderText.LISTENER_WAITING);
		cicleTime = System.currentTimeMillis();
		while (LanderParam.state.get(numUAVs-1) == LANDING) {
			// Discard message
			Copter.receiveMessage(numUAVs-1, LanderParam.RECEIVING_TIMEOUT);
			
			if (System.currentTimeMillis() - cicleTime > LanderParam.LAND_CHECK_TIMEOUT) {
				if(!Copter.isFlying(numUAVs-1)) {
					LanderParam.state.set(numUAVs-1, FINISH);
				} else {
					cicleTime = cicleTime + LanderParam.LAND_CHECK_TIMEOUT;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		
		
	}

	@Override
	public void forceExperimentEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getExperimentResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO Auto-generated method stub
		
	}

}
