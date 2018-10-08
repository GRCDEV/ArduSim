package pollution;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.pojo.GeoCoordinates;
import pollution.pojo.*;
import sim.board.BoardPanel;

public class PollutionHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Chemotaxis";
	}
	
	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		PollutionConfigDialog cofigDialog = new PollutionConfigDialog();
		cofigDialog.setVisible(true);
		//sim.logic.SimTools.println(PollutionParam.startLocation.latitude + ", " + PollutionParam.startLocation.longitude);
	}//TODO

	@Override
	public void initializeDataStructures() {
		// Sensor setup
		GUI.log("Pollution sensor setup.");
		try {
			PollutionParam.sensor = PollutionParam.isSimulation ? new PollutionSensorSim() : null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GUI.log("Pollution sensor setup done.");
		
		// Coordinates setup
		PollutionParam.origin = api.Tools.geoToUTM(PollutionParam.startLocation.latitude, PollutionParam.startLocation.longitude);
		PollutionParam.origin.x -= PollutionParam.width/2.0;
		PollutionParam.origin.y -= PollutionParam.length/2.0;
		
		// Measurement structure
		PollutionParam.measurements_temp = new ArrayList<Value>();
		PollutionParam.measurements_set = new ValueSet();
		
		PollutionParam.ready = false;
	}//TODO

	@Override
	public String setInitialState() {
		// TODO 
		return null;
	}

	@Override
	public void rescaleDataStructures() {
		// TODO 
	}

	@Override
	public void loadResources() {
		// TODO 
	}
	
	@Override
	public void rescaleShownResources() {
		// TODO 
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		// TODO 
		if (PollutionParam.ready) {
			synchronized(PollutionParam.measurements_temp) {
				Iterator<Value> itr = PollutionParam.measurements_temp.iterator();
				while(itr.hasNext()) PollutionParam.measurements_set.add(itr.next());
				PollutionParam.measurements_temp.clear();
			}
			pollution.gui.DrawTool.drawValueSet(g2, PollutionParam.measurements_set);
			pollution.gui.DrawTool.drawBounds(g2);
		}
	}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(PollutionParam.startLocation, 0.0);
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
	}//TODO

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;
	}

	@Override
	public void startThreads() {
		new Pollution().start();
	}

	@Override
	public void setupActionPerformed() {
		
	}

	@Override
	public void startExperimentActionPerformed() {
		/* Takeoff */
		if (!Copter.takeOff(0, PollutionParam.altitude)) {
			//TODO tratar el error
		}
		PollutionParam.ready = true;
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
	public void logData(String folder, String baseFileName) {
		// TODO 
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO 
	}
	

}
