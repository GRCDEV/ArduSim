package none;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import main.Text;
import mbcap.logic.MBCAPv3Helper;
import sim.board.BoardPanel;

/** Implementation of the protocol NONE to allow the user to simply follow missions.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ProtocolNoneHelper extends ProtocolHelper {
	
	private MBCAPv3Helper copy;
	
	public ProtocolNoneHelper() {
		this.copy = new MBCAPv3Helper();
	}

	@Override
	public void setProtocol() {
		this.protocolString = "None";
	}

	@Override
	public boolean loadMission() {
		return true;
	}

	@Override
	public void openConfigurationDialog() {
		new NoneConfigDialog();
	}

	@Override
	public void initializeDataStructures() {}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		return this.copy.setStartingLocation();
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return this.copy.sendInitialConfiguration(numUAV);
	}

	@Override
	public void startThreads() {}

	@Override
	public void setupActionPerformed() {}

	@Override
	public void startExperimentActionPerformed() {
		this.copy.startExperimentActionPerformed();
	}

	@Override
	public void forceExperimentEnd() {
		this.copy.forceExperimentEnd();
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

	/** Checks the validity of the configuration of the protocol. */
	public static boolean isValidProtocolConfiguration(NoneConfigDialogPanel panel) {
		// Simulation parameters
		String validating = panel.missionsTextField.getText();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		return true;
	}
	
	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(NoneConfigDialogPanel panel) {
		// Simulation parameters
		Tools.setNumUAVs(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
}
