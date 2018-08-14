package lander.logic;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import lander.gui.LanderConfiDialog;
import main.Param;
import sim.board.BoardPanel;

public class LanderHelper extends ProtocolHelper{

	@Override
	public void setProtocol() {
		// TODO Auto-generated method stub
		
		this.protocolString = "WillianProtocols";
		
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
		
		GUI.log("NUMERO DE UAV: " + Tools.getNumUAVs());
		if (!Copter.takeOff(0, LanderParam.altitude) || !Copter.moveUAV(Tools.getNumUAVs()-1, LanderParam.LocationEnd, LanderParam.altitude, 20, 2)) {
			//Tratar error
		}
		
		//Copter.moveUAV(numUAV, geo, relAltitude, destThreshold, altThreshold)moveUAV()
		
		
		Copter.setFlightMode(0, FlightMode.LAND_ARMED);
		
		
		
		
		
		
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
