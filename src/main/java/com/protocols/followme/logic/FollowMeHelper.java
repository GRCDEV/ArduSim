package com.protocols.followme.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.swarm.SwarmParam;
import com.api.swarm.formations.Formation;
import com.api.copter.CopterParam;
import es.upv.grc.mapper.*;
import com.protocols.followme.gui.FollowMeConfigDialogApp;
import com.protocols.followme.gui.FollowmeSimProperties;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.copter.Copter;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;


/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class FollowMeHelper extends ProtocolHelper {

	FollowMeListenerThread master;

	@Override
	public void setProtocol() {
		this.protocolString = FollowMeText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public JDialog openConfigurationDialog() { return null;}

	@Override
	public void openConfigurationDialogFX() {
		Platform.runLater(()->new FollowMeConfigDialogApp().start(new Stage()));
	}

	@Override
	public void configurationCLI() {
		FollowmeSimProperties properties = new FollowmeSimProperties();
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
			ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND );
			System.exit(0);
		}
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = API.getArduSim().getNumUAVs();
		AtomicInteger[] state = new AtomicInteger[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			state[i] = new AtomicInteger();	// Implicit value State.START, as it is equals to 0
		}
		
		FollowMeParam.state = state;	
	}

	@Override
	public String setInitialState() {
		return FollowMeText.START;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Location3D masterLocation = new Location3D(FollowMeParam.masterInitialLatitude, FollowMeParam.masterInitialLongitude,0);
		
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
		Location3DUTM location;
		// We put the master UAV in the position 0 of the formation
		// Another option would be to put the master UAV in the center of the ground formation, and the remaining UAVs surrounding it
		startingLocation[0] = Pair.with(masterLocation.getGeoLocation(), FollowMeParam.masterInitialYaw);
		Formation groundFormation = UAVParam.groundFormation.get();
		Location3DUTM offsetMasterToCenterUAV = groundFormation.get3DUTMLocation(masterLocation.getUTMLocation3D(),0);
		for (int i = 1; i < numUAVs; i++) {
			Location3DUTM offsetToCenterUAV = groundFormation.get3DUTMLocation(masterLocation.getUTMLocation3D(),i);
			location = new Location3DUTM(new Location2DUTM(masterLocation.getUTMLocation().x - offsetMasterToCenterUAV.x + offsetToCenterUAV.x,
					masterLocation.getUTMLocation().y - offsetMasterToCenterUAV.y + offsetToCenterUAV.y),0);
			try {
				startingLocation[i] = Pair.with(location.getGeo(), FollowMeParam.masterInitialYaw);
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
				API.getGUI(0).exit(e.getMessage());
			}
		}
		
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// The following code is valid for ArduCopter version 3.6.12 or lower
		Copter copter = API.getCopter(numUAV);
		if (numUAV == SwarmParam.masterId) {
			String version = UAVParam.arducopterVersion.get();
			if(version.contains("3.5")) {
				return copter.setParameter(CopterParam.LOITER_SPEED_357, FollowMeParam.masterSpeed);
			}else if(version.contains("3.6")){
				return copter.setParameter(CopterParam.LOITER_SPEED_36X, FollowMeParam.masterSpeed);
			}else {
				API.getGUI(0).log("this version is not checked yet,"
						+ " if ardusim does not run as expected please check"
						+ " src:main.java.com.protocols.followme:followmeHelper.java:sendInitialConfiguration");
				return copter.setParameter(CopterParam.LOITER_SPEED_36X, FollowMeParam.masterSpeed);
			}
		}
		return true;
	}

	@Override
	public void startThreads() {
		int numUAVs = API.getArduSim().getNumUAVs();
		master = new FollowMeListenerThread(0);
		master.start();
		for (int i = 1; i < numUAVs; i++) {
			new FollowMeListenerThread(i).start();
		}
		API.getGUI(0).log(FollowMeText.ENABLING);
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
	public void startExperimentActionPerformed() {
	}

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
