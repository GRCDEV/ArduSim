package mbcap.gui;

import javax.swing.SwingUtilities;

import api.API;
import main.Text;
import main.api.ArduSim;
import main.api.GUI;
import main.api.ValidationTools;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPText;

/** This class contains exclusively static methods used by the GUI.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPGUITools {

	/** Checks the validity of the configuration of the protocol. */
	public static boolean isValidProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		GUI gui = API.getGUI(0);
		ValidationTools validationTools = API.getValidationTools();
		
		// Simulation parameters
		String validating = panel.missionsTextField.getText();
		if (validationTools.isEmpty(validating)) {
			gui.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		
		// Beaconing parameters
		validating = panel.beaconingPeriodTextField.getText();
		if (!validationTools.isValidPositiveLong(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_PERIOD_ERROR);
			return false;
		}
		validating = panel.numBeaconsTextField.getText();
		if (!validationTools.isValidPositiveInteger(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_REFRESH_ERROR);
			return false;
		}
		validating = panel.hopTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.HOP_TIME_ERROR);
			return false;
		}
		validating = panel.minSpeedTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.MIN_SPEED_ERROR);
			return false;
		}
		validating = panel.beaconExpirationTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_EXPIRATION_ERROR);
			return false;
		}
		double beaconExpirationTime = Double.parseDouble(validating);

		// Collision avoidance protocol parameters
		validating = panel.collisionRiskDistanceTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_DISTANCE_ERROR_1);
			return false;
		}
		ArduSim ardusim = API.getArduSim();
		boolean checkCollision = ardusim.collisionIsCheckEnabled();
		if (checkCollision) {
			double collisionRiskDistance = Double.parseDouble(validating);
			double collisionDistance = ardusim.collisionGetHorizontalDistance();
			if (collisionRiskDistance < collisionDistance + 1) {
				gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_DISTANCE_ERROR_2);
				return false;
			}
		}
		validating = panel.collisionRiskAltitudeDifferenceTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_ALTITUDE_ERROR_1);
			return false;
		}
		if (checkCollision) {
			double collisionRiskAltitudeDifference = Double.parseDouble(validating);
			double collisionAltitudeDifference = ardusim.collisionGetVerticalDistance();
			if (collisionRiskAltitudeDifference <= collisionAltitudeDifference) {
				gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_ALTITUDE_ERROR_2);
				return false;
			}
		}
		validating = panel.maxTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_TIME_ERROR);
			return false;
		}
		validating = panel.riskCheckPeriodTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.CHECK_PERIOD_ERROR);
			return false;
		}
		validating = panel.packetLossTextField.getText();
		if (!validationTools.isValidPositiveInteger(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.PACKET_LOSS_ERROR);
			return false;
		}
		validating = panel.gpsErrorTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.GPS_ERROR_ERROR);
			return false;
		}
		validating = panel.standStillTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.HOVERING_TIMEOUT_ERROR);
			return false;
		}
		validating = panel.passingTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.OVERTAKE_TIMEOUT_ERROR);
			return false;
		}
		validating = panel.solvedTimeTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.RESUME_MODE_DELAY_ERROR);
			return false;
		}
		validating = panel.recheckTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.RECHECK_DELAY_ERROR);
			return false;
		}
		validating = panel.deadlockTimeoutTextField.getText();
		if (!validationTools.isValidPositiveInteger(validating)) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_1);
			return false;
		}
		double deadlockTimeout = Integer.parseInt(validating);
		if (deadlockTimeout < beaconExpirationTime) {
			gui.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_2);
			return false;
		}
		return true;
	}

	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Simulation parameters
		API.getArduSim().setNumUAVs(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));
		
		// Beaconing parameters
		MBCAPParam.beaconingPeriod = Long.parseLong(panel.beaconingPeriodTextField.getText());
		MBCAPParam.numBeacons = Integer.parseInt(panel.numBeaconsTextField.getText());
		MBCAPParam.hopTime = Double.parseDouble(panel.hopTimeTextField.getText());
		MBCAPParam.hopTimeNS = (long) (MBCAPParam.hopTime * 1000000000l);
		MBCAPParam.minSpeed = Double.parseDouble(panel.minSpeedTextField.getText());
		MBCAPParam.beaconExpirationTime = (long) (Double.parseDouble(panel.beaconExpirationTimeTextField.getText()) * 1000000000l);

		// Collision avoidance protocol
		MBCAPParam.collisionRiskDistance = Double.parseDouble(panel.collisionRiskDistanceTextField.getText());
		MBCAPParam.collisionRiskAltitudeDifference = Double.parseDouble(panel.collisionRiskAltitudeDifferenceTextField.getText());
		MBCAPParam.collisionRiskTime = (long) (Double.parseDouble(panel.maxTimeTextField.getText()) * 1000000000l);
		MBCAPParam.riskCheckPeriod = (long) (Double.parseDouble(panel.riskCheckPeriodTextField.getText()) * 1000000000l);
		MBCAPParam.packetLossThreshold = Integer.parseInt(panel.packetLossTextField.getText());
		MBCAPParam.gpsError = Double.parseDouble(panel.gpsErrorTextField.getText());
		MBCAPParam.safePlaceDistance = 2 * MBCAPParam.gpsError + MBCAPParam.EXTRA_ERROR + MBCAPParam.PRECISION_MARGIN;
		MBCAPParam.standStillTimeout = (long) (Double.parseDouble(panel.standStillTimeTextField.getText()) * 1000000000l);
		MBCAPParam.passingTimeout = (long) (Double.parseDouble(panel.passingTimeTextField.getText()) * 1000000000l);
		MBCAPParam.resumeTimeout = (long) (Double.parseDouble(panel.solvedTimeTextField.getText()) * 1000000000l);
		MBCAPParam.recheckTimeout = (long) (Double.parseDouble(panel.recheckTextField.getText()) * 1000l);
		MBCAPParam.globalDeadlockTimeout = Integer.parseInt(panel.deadlockTimeoutTextField.getText()) * 1000000000l;
	}

	/** Loads the default protocol configuration from variables. */
	public static void loadDefaultProtocolConfiguration(final MBCAPConfigDialogPanel panel) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Beaconing parameters
				panel.beaconingPeriodTextField.setText("" + MBCAPParam.beaconingPeriod);
				panel.numBeaconsTextField.setText("" + MBCAPParam.numBeacons);
				panel.hopTimeTextField.setText("" + MBCAPParam.hopTime);
				panel.minSpeedTextField.setText("" + MBCAPParam.minSpeed);
				panel.beaconExpirationTimeTextField.setText("" + ((double) MBCAPParam.beaconExpirationTime) / 1000000000l);

				// Collision avoidance protocol parameters
				panel.collisionRiskDistanceTextField.setText("" + MBCAPParam.collisionRiskDistance);
				panel.collisionRiskAltitudeDifferenceTextField.setText("" + MBCAPParam.collisionRiskAltitudeDifference);
				panel.maxTimeTextField.setText("" + ((double) MBCAPParam.collisionRiskTime) / 1000000000l);
				panel.riskCheckPeriodTextField.setText("" + ((double) MBCAPParam.riskCheckPeriod) / 1000000000l);
				panel.packetLossTextField.setText("" + MBCAPParam.packetLossThreshold);
				panel.gpsErrorTextField.setText("" + MBCAPParam.gpsError);
				panel.standStillTimeTextField.setText("" + ((double) MBCAPParam.standStillTimeout) / 1000000000l);
				panel.passingTimeTextField.setText("" + ((double) MBCAPParam.passingTimeout) / 1000000000l);
				panel.solvedTimeTextField.setText("" + ((double) MBCAPParam.resumeTimeout) / 1000000000l);
				panel.recheckTextField.setText("" + ((double) MBCAPParam.recheckTimeout) / 1000l);
				panel.deadlockTimeoutTextField.setText("" + (int) (MBCAPParam.globalDeadlockTimeout / 1000000000l));
			}
		});
	}

}
