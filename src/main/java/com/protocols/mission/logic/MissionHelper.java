package com.protocols.mission.logic;

import com.api.*;
import com.api.copter.MoveToListener;
import com.api.copter.TakeOffListener;
import com.api.swarm.formations.Formation;
import com.api.pojo.location.Waypoint;
import com.protocols.mission.gui.MissionDialogApp;
import com.protocols.mission.gui.MissionSimProperties;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;
import es.upv.grc.mapper.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/** Implementation of the protocol Mission to allow the user to simply follow missions. It is based on MBCAP implementation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Mission";
	}

	@Override
	public boolean loadMission() {
		return true;
	}

	@Override
	public JDialog openConfigurationDialog() { return null;}

	@Override
	public void openConfigurationDialogFX() {
		Platform.runLater(()->new MissionDialogApp().start(new Stage()));
	}

	@Override
	public void configurationCLI() {
		MissionSimProperties properties = new MissionSimProperties();
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
	public void initializeDataStructures() {}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		int numUAVs = API.getArduSim().getNumUAVs();
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];

		com.api.MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
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
	public boolean sendInitialConfiguration(int numUAV) { return true; }

	@Override
	public void startThreads() {}

	@Override
	public void setupActionPerformed() {
		int numUAVs = API.getArduSim().getNumUAVs();
		List<Thread> threads = new ArrayList<>();
		for(int i = 0;i<numUAVs;i++){
			Thread t = API.getCopter(i).takeOff(10, new TakeOffListener() {
				@Override
				public void onCompleteActionPerformed() {

				}

				@Override
				public void onFailure() {

				}
			});
			threads.add(t);
			t.start();
		}
		//wait for take off to be finished
		for(Thread t:threads){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void startExperimentActionPerformed() {
		int numUAVs = API.getArduSim().getNumUAVs();
		com.api.MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
		List<Waypoint>[] missions = missionHelper.getMissionsLoaded();
		List<Thread> threads = new ArrayList<>();
		Formation f = UAVParam.groundFormation.get();
		for (int wp_index =2;wp_index<missions[0].size();wp_index++) {
			Waypoint wp = missions[0].get(wp_index);
			for(int numUAV=0;numUAV<numUAVs;numUAV++) {
				API.getGUI(numUAV).logUAV("Moving to WP: " + (wp_index-1));
				try {
					Location2DUTM locInSwarm = f.get3DUTMLocation(new Location3DUTM(wp.getUTM(),0),numUAV);
					Location3D loc = new Location3D(locInSwarm.getGeo(),10);
					Thread t = API.getCopter(numUAV).moveTo(loc, new MoveToListener() {
						@Override
						public void onCompleteActionPerformed() {

						}

						@Override
						public void onFailure() {

						}
					});
					threads.add(t);
					t.start();
				} catch (LocationNotReadyException e) {
					e.printStackTrace();
				}
			}
			for(Thread t:threads){
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		for(int numUAV=0;numUAV<numUAVs;numUAV++){
			API.getCopter(numUAV).land();
		}
	}

	@Override
	public void forceExperimentEnd() {

	}

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
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
}
