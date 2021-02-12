package com.protocols.chemotaxis.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.protocols.chemotaxis.gui.PollutionConfigDialog;
import com.protocols.chemotaxis.pojo.ValueSet;
import es.upv.grc.mapper.Location2DGeo;
import com.api.Copter;
import com.api.GUI;
import com.api.TakeOff;
import com.api.TakeOffListener;
import org.javatuples.Pair;

import javax.swing.*;

public class ChemotaxisHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Chemotaxis";
	}
	
	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public JDialog openConfigurationDialog() {
		return new PollutionConfigDialog();
		//com.setup.sim.logic.SimTools.println(PollutionParam.startLocation.latitude + ", " + PollutionParam.startLocation.longitude);
	}//TODO

	@Override
	public void openConfigurationDialogFX() {

	}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {
		GUI gui = API.getGUI(0);
		// Sensor setup
		gui.log("Pollution sensor setup.");
		try {
			ChemotaxisParam.sensor = ChemotaxisParam.isSimulation ? new ChemotaxisSensorSim() : null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		gui.log("Pollution sensor setup done.");
		
		// Coordinates setup
		ChemotaxisParam.origin = ChemotaxisParam.startLocation.getUTM();
		ChemotaxisParam.origin.x -= ChemotaxisParam.width/2.0;
		ChemotaxisParam.origin.y -= ChemotaxisParam.length/2.0;
		
		// Measurement structure
		ChemotaxisParam.measurements_set = new ValueSet();
		
		ChemotaxisParam.ready = false;
	}//TODO

	@Override
	public String setInitialState() {
		// TODO 
		return null;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Pair<Location2DGeo, Double> startCoordinates = new Pair<>(ChemotaxisParam.startLocation, 0.0);
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
	}//TODO

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;
	}

	@Override
	public void startThreads() {
		new ChemotaxisThread().start();
	}

	@Override
	public void setupActionPerformed() {
		
	}

	@Override
	public void startExperimentActionPerformed() {
		/* Takeoff */
		Copter copter = API.getCopter(0);
		TakeOff takeOff = copter.takeOff(ChemotaxisParam.altitude, new TakeOffListener() {
			
			@Override
			public void onFailure() {
				// TODO tratar el error
			}
			
			@Override
			public void onCompleteActionPerformed() {
				// Nothing to do, just waiting the end with Thread.join()
			}
		});
		takeOff.start();
		try {
			takeOff.join();
		} catch (InterruptedException ignored) {}
		
		ChemotaxisParam.ready = true;
		
	}

	@Override
	public void forceExperimentEnd() {
		// TODO 
	}

	@Override
	public String getExperimentResults() {
		// TODO 
		return null;
	}
	
	@Override
	public String getExperimentConfiguration() {
		// TODO 
		return null;
	}
	
	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO 
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO 
	}
	

}