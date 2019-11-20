package mission.logic;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2DGeo;
import main.Text;
import mbcap.logic.MBCAPHelper;
import mission.gui.MissionConfigDialog;
import mission.gui.MissionConfigDialogPanel;

/** Implementation of the protocol Mission to allow the user to simply follow missions. It is based on MBCAP implementation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionHelper extends ProtocolHelper {
	
	private MBCAPHelper copy;
	
	public MissionHelper() {
		this.copy = new MBCAPHelper();
	}

	@Override
	public void setProtocol() {
		this.protocolString = "Mission";
	}

	@Override
	public boolean loadMission() {
		return true;
	}

	@Override
	public JDialog openConfigurationDialog() {
		return new MissionConfigDialog();
	}

	@Override
	public void initializeDataStructures() {}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
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
	public static boolean isValidProtocolConfiguration(MissionConfigDialogPanel panel) {
		// Simulation parameters
		String validating = panel.missionsTextField.getText();
		if (API.getValidationTools().isEmpty(validating)) {
			API.getGUI(0).warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		return true;
	}
	
	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(MissionConfigDialogPanel panel) {
		// Simulation parameters
		API.getArduSim().setNumUAVs(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
}
