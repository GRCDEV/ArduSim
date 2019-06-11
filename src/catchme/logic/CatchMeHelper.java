package catchme.logic;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.location.Location2DGeo;

public class CatchMeHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Catch Me";
	}

	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		API.getArduSim().setProtocolConfigured();
	}

	@Override
	public void initializeDataStructures() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String setInitialState() {
		return "Starting";
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
	public void drawResources(Graphics2D graphics, main.sim.board.BoardPanel panel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		return new Pair[] {Pair.with(new Location2DGeo(CatchMeParams.LATITUDE, CatchMeParams.LONGITUDE), 0)};
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void startThreads() {
		ListenerThread listen = new ListenerThread();
		listen.start();
		
	}

	@Override
	public void setupActionPerformed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startExperimentActionPerformed() {
		// TODO Auto-generated method stub
		
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
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO Auto-generated method stub
		
	}

}
