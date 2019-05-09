package fme.logic;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.ProtocolHelper;
import api.pojo.GeoCoordinates;
import sim.board.BoardPanel;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FMeHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = FMeText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeDataStructures() {
		// TODO Auto-generated method stub

	}

	@Override
	public String setInitialState() {
		// TODO Auto-generated method stub
		return null;
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
	public void drawResources(Graphics2D graphics, BoardPanel panel) {
		// TODO Auto-generated method stub

	}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startThreads() {
		// TODO Auto-generated method stub

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
