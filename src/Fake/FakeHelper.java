package Fake;

import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2DGeo;
import main.Param;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;

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
		try {
			FileWriter writer = new FileWriter("setup.txt");
			writer.write("starting setup");
			for(int i=0;i<10;i++){
				writer.write(("doing the setup" + i));
				API.getArduSim().sleep(1000);
			}
			writer.write("setup finished");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startExperimentActionPerformed() {
		try {
			FileWriter writer = new FileWriter("experiment.txt");
			writer.write("starting experiment");
			for(int i=0;i<10;i++){
				writer.write(("doing the exeriment" + i));
				API.getArduSim().sleep(1000);
			}
			writer.write("experiment finished");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
