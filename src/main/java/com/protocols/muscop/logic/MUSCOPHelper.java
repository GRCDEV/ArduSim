package com.protocols.muscop.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.MissionHelper;
import com.api.swarm.formations.Formation;
import com.api.pojo.location.Waypoint;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.protocols.muscop.gui.MuscopConfigDialogApp;
import com.protocols.muscop.gui.MuscopSimProperties;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;


/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class MUSCOPHelper extends ProtocolHelper {

	private DroneThread master;

	@Override
	public void setProtocol() {
		this.protocolString = "MUSCOP";
	}

	@Override
	public boolean loadMission() { return true; }

	@Override
	public JDialog openConfigurationDialog() {return null;}

	@Override
	public void openConfigurationDialogFX() {
		Platform.runLater(()->new MuscopConfigDialogApp().start(new Stage()));
	}

	@Override
	public void configurationCLI() {
		MuscopSimProperties properties = new MuscopSimProperties();
		ResourceBundle resources;
		try {
			FileInputStream fis = new FileInputStream(SimParam.protocolParamFile);
			resources = new PropertyResourceBundle(fis);
			fis.close();
			Properties p = new Properties();
			for(String key: resources.keySet()){
				p.setProperty(key,resources.getString(key));
			}
			properties.storeParameters(p,resources);
		} catch (IOException e) {
			e.printStackTrace();
			ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND );
			System.exit(0);
		}

	}

	@Override
	public void initializeDataStructures() { }

	@Override
	public String setInitialState() {
		return "START";
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		int numUAVs = API.getArduSim().getNumUAVs();
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];

		MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
		List<Waypoint>[] missions = missionHelper.getMissionsLoaded();
		// missions[0].get(0) is somewhere in Africa
		Location3DUTM start = new Location3DUTM(missions[0].get(1).getUTM(),0);
		Formation f = UAVParam.groundFormation.get();
		double yaw = 0;
		for(int i = 0;i<numUAVs;i++){
			try {
				startingLocation[i] = new Pair<>(f.get3DUTMLocation(start,i).getGeo(),yaw);
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
				return null;
			}
		}
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// No need of specific configuration, as the mission is automatically loaded
		return true;
	}

	@Override
	public void startThreads() {
		master = new DroneThread(0);
		master.start();
		for (int i = 1; i < API.getArduSim().getNumUAVs(); i++) {
			DroneThread t = new DroneThread(i);
			t.start();
		}
	}

	@Override
	public void setupActionPerformed() {
		while(!master.isSetupDone()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
	}

}
