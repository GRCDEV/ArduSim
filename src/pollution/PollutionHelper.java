package pollution;

import java.awt.Graphics2D;

import org.javatuples.Pair;

import api.Copter;
import api.ProtocolHelper;
import api.pojo.GeoCoordinates;
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
		try {
			PollutionParam.sensor = PollutionParam.isSimulation ? new PollutionSensorSim() : null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(PollutionParam.startLocation, 0.0);
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
	

}
