package com.protocols.shakeup.logic;


import com.api.API;
import com.api.ProtocolHelper;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.protocols.shakeup.pojo.Param;
import com.protocols.shakeup.pojo.TargetFormation;
import com.protocols.shakeup.pojo.Text;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.LocationNotReadyException;
import org.javatuples.Pair;

import javax.swing.*;

public class ShakeupHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {this.protocolString = Text.PROTOCOL_TEXT;}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {return null;}

	@Override
	public void openConfigurationDialogFX() {
		com.setup.Param.simStatus = com.setup.Param.SimulatorState.STARTING_UAVS;
	}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {
		//readIniFile("Shakeup_settings.ini");
		int numUAVs = API.getArduSim().getNumUAVs();
		Formation ground = FormationFactory.newFormation(Param.startLayout);
		ground.init(numUAVs,Param.minDistance);
		UAVParam.groundFormation.set(ground);
		// Air and ground formation are the same
		UAVParam.airFormation.set(ground);

		//API.getCopter(0).getSafeTakeOffHelper().setTakeOffAlgorithm(TakeOffAlgorithm.SIMPLIFIED.getName());
		TargetFormation[] formations = new TargetFormation[Param.formations.length];
		formations[0] = new TargetFormation(Param.endLayout.name(),numUAVs,Param.minDistance,0);
		//TODO set for more formations
		/*
		for(int i = 0;i < Param.formations.length;i++) {
			// formations[i] = new TargetFormation(Param.endLayout.getName(), numUAVs, 10, 0);
		}
		 */
		Param.flightFormations = formations;
	}

	@Override
	public String setInitialState() {return null;}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Location3D masterLocation = new Location3D(Param.masterInitialLatitude, Param.masterInitialLongitude,0);
		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
		Formation f = UAVParam.groundFormation.get();
		double yaw = 0;
		for(int i = 0;i<numUAVs;i++){
			try {
				startingLocation[i] = new Pair<>(f.get3DUTMLocation(masterLocation.getUTMLocation3D(),i).getGeo(),yaw);
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
				return null;
			}
		}
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;}

	@Override
	public void startThreads() {
		for (int i = 0; i < API.getArduSim().getNumUAVs(); i++) {
			ShakeupListenerThread listner = new ShakeupListenerThread(i);
			listner.start();
		}
	}

	@Override
	public void setupActionPerformed() {
		while (API.getArduSim().isSetupFinished()) {
			API.getArduSim().sleep(200);
		}

	}

	@Override
	public void startExperimentActionPerformed() {
	}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {return null;}

	@Override
	public String getExperimentConfiguration() {return null;}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}

}
