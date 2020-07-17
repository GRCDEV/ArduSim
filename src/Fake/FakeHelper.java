package Fake;

import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2DGeo;
import main.Param;
import org.javatuples.Pair;

import javax.swing.*;

public class FakeHelper extends ProtocolHelper{

	@Override
	public void setProtocol() {
		this.protocolString = "FAKE";
	}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {
		return null;
	}

	@Override
	public void openConfigurationDialogFX() {
		Param.simStatus = Param.SimulatorState.STARTING_UAVS;
	}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {
	}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];
		for(int id=0;id<numUAVs;id++) {
			// just a random location e.g. UPV campus vera
			startingLocations[id] = Pair.with(new Location2DGeo(39.725064, -0.733661),0.0);
		}
		return startingLocations;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;
	}

	@Override
	public void startThreads() {
	}

	@Override
	public void setupActionPerformed() {
		System.out.println("starting setup");
		for(int i=0;i<10;i++){
			System.out.println("doing the setup" + i);
			API.getArduSim().sleep(1000);
		}
		System.out.println("setup finished");
	}

	@Override
	public void startExperimentActionPerformed() {
		System.out.println("starting experiment");
			for(int i=0;i<10;i++){
				System.out.println("doing the exeriment" + i);
				API.getArduSim().sleep(1000);
			}
		System.out.println("experiment finished");
		Param.simStatus = Param.SimulatorState.TEST_FINISHED;
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
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		
	}

}
