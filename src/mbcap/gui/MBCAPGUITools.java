package mbcap.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import api.GUI;
import api.Tools;
import api.pojo.Point3D;
import main.Text;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.logic.MBCAPText;
import mbcap.pojo.ProgressState;
import sim.board.BoardPanel;

/** This class contains exclusively static methods used by the GUI. */

public class MBCAPGUITools {

	/** Checks the validity of the configuration of the protocol. */
	public static boolean isValidProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Simulation parameters
		String validating = panel.missionsTextField.getText();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		
		// Beaconing parameters
		validating = (String) panel.beaconingPeriodTextField.getText();
		if (!Tools.isValidInteger(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_PERIOD_ERROR);
			return false;
		}
		validating = (String) panel.numBeaconsTextField.getText();
		if (!Tools.isValidInteger(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_REFRESH_ERROR);
			return false;
		}
		validating = (String) panel.beaconExpirationTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.BEACON_EXPIRATION_ERROR);
			return false;
		}
		double beaconExpirationTime = Double.parseDouble(validating);
		validating = (String) panel.beaconFlyingTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_1);
			return false;
		}
		validating = (String) panel.hopTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.HOP_TIME_ERROR);
			return false;
		}
		double flyingTime = Double.parseDouble(panel.beaconFlyingTimeTextField.getText());
		double maxFlyingTime = (MBCAPParam.MAX_POINTS-1)*MBCAPParam.hopTime;
		if (flyingTime > maxFlyingTime) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_2 + " "
					+ String.format( "%.2f", maxFlyingTime ) + " " + MBCAPText.SECONDS + ".");
			return false;
		}
		double hopTime = Double.parseDouble(panel.hopTimeTextField.getText());
		double points = flyingTime/hopTime;
		if (Math.floor(points) - points != 0) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_3);
			return false;
		}
		if (!Tools.isValidPositiveDouble((String) panel.minSpeedTextField.getText())) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.MIN_SPEED_ERROR);
			return false;
		}

		// Collision avoidance protocol parameters
		validating = (String) panel.collisionRiskDistanceTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_DISTANCE_ERROR_1);
			return false;
		}
		double collisionRiskDistance = Double.parseDouble(validating);
		boolean checkCollision = Tools.isCollisionCheckEnabled();
		double collisionDistance = 0;
		if (checkCollision) {
			collisionDistance = Tools.getCollisionHorizontalDistance();
			if (collisionRiskDistance <= collisionDistance) {
				GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_DISTANCE_ERROR_2);
				return false;
			}
		}
		validating = (String) panel.collisionRiskAltitudeDifferenceTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_ALTITUDE_ERROR_1);
			return false;
		}
		double collisionRiskAltitudeDifference = Double.parseDouble(validating);
		double collisionAltitudeDifference;
		if (checkCollision) {
			collisionAltitudeDifference = Tools.getCollisionVerticalDistance();
			if (collisionRiskAltitudeDifference <= collisionAltitudeDifference) {
				GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_ALTITUDE_ERROR_2);
				return false;
			}
		}
		validating = (String) panel.maxTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.WARN_TIME_ERROR);
			return false;
		}
		validating = (String) panel.reactionDistanceTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.CHECK_THRESHOLD_ERROR_1);
			return false;
		}
		double reactionDistance = Double.parseDouble(validating);
		if (reactionDistance <= collisionRiskDistance) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.CHECK_THRESHOLD_ERROR_2);
			return false;
		}
		validating = (String) panel.riskCheckPeriodTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.CHECK_PERIOD_ERROR);
			return false;
		}
		validating = (String) panel.safePlaceDistanceTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.SAFE_DISTANCE_ERROR_1);
			return false;
		}
		if (checkCollision) {
			double safePlaceDistance = Double.parseDouble(validating);
			if (safePlaceDistance <= collisionDistance) {
				GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.SAFE_DISTANCE_ERROR_2);
				return false;
			}
		}
		validating = (String) panel.standStillTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.HOVERING_TIMEOUT_ERROR);
			return false;
		}
		validating = (String) panel.passingTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.OVERTAKE_TIMEOUT_ERROR);
			return false;
		}
		validating = (String) panel.solvedTimeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.RESUME_MODE_DELAY_ERROR);
			return false;
		}
		validating = (String) panel.deadlockTimeoutTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_1);
			return false;
		}
		double deadlockTimeout = Double.parseDouble(validating);
		if (deadlockTimeout < beaconExpirationTime) {
			GUI.warn(MBCAPText.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_2);
			return false;
		}
		return true;
	}

	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Simulation parameters
		Tools.setNumUAVs(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));
		
		// Beaconing parameters
		MBCAPParam.beaconingPeriod = Integer.parseInt((String) panel.beaconingPeriodTextField.getText());
		MBCAPParam.numBeacons = Integer.parseInt((String) panel.numBeaconsTextField.getText());
		MBCAPParam.beaconExpirationTime = (long) (Double
				.parseDouble((String) panel.beaconExpirationTimeTextField.getText()) * 1000000000l);
		MBCAPParam.beaconFlyingTime = Double.parseDouble((String) panel.beaconFlyingTimeTextField.getText());

		MBCAPParam.hopTime = Double.parseDouble((String)panel.hopTimeTextField.getText());
		MBCAPParam.hopTimeNS = (long) (MBCAPParam.hopTime * 1000000000l);
		MBCAPParam.minSpeed = Double.parseDouble((String) panel.minSpeedTextField.getText());

		// Collision avoidance protocol
		MBCAPParam.collisionRiskDistance = Double
				.parseDouble((String) panel.collisionRiskDistanceTextField.getText());
		MBCAPParam.collisionRiskAltitudeDifference = Double
				.parseDouble((String) panel.collisionRiskAltitudeDifferenceTextField.getText());
		MBCAPParam.collisionRiskTime = (long) (Double.parseDouble((String) panel.maxTimeTextField.getText()) * 1000000000l);
		MBCAPParam.reactionDistance = Double.parseDouble((String) panel.reactionDistanceTextField.getText());
		MBCAPParam.riskCheckPeriod = (long) (Double.parseDouble((String) panel.riskCheckPeriodTextField.getText())
				* 1000000000l);
		MBCAPParam.safePlaceDistance = Double.parseDouble((String) panel.safePlaceDistanceTextField.getText());
		MBCAPParam.standStillTimeout = (long) (Double.parseDouble((String) panel.standStillTimeTextField.getText()) * 1000000000l);
		MBCAPParam.passingTimeout = (long) (Double.parseDouble((String) panel.passingTimeTextField.getText()) * 1000000000l);
		MBCAPParam.solvedTimeout = (long) (Double.parseDouble((String) panel.solvedTimeTextField.getText()) * 1000000000l);
		MBCAPParam.globalDeadlockTimeout = Integer.parseInt((String) panel.deadlockTimeoutTextField.getText())
				* 1000000000l;
	}

	/** Loads the default protocol configuration from variables. */
	public static void loadDefaultProtocolConfiguration(final MBCAPConfigDialogPanel panel) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Beaconing parameters
				panel.beaconingPeriodTextField.setText("" + MBCAPParam.beaconingPeriod);
				panel.numBeaconsTextField.setText("" + MBCAPParam.numBeacons);
				panel.beaconExpirationTimeTextField.setText("" + ((double) MBCAPParam.beaconExpirationTime) / 1000000000l);
				panel.beaconFlyingTimeTextField.setText("" + MBCAPParam.beaconFlyingTime);
				panel.hopTimeTextField.setText("" + MBCAPParam.hopTime);
				panel.minSpeedTextField.setText("" + MBCAPParam.minSpeed);

				// Collision avoidance protocol parameters
				panel.collisionRiskDistanceTextField.setText("" + MBCAPParam.collisionRiskDistance);
				panel.collisionRiskAltitudeDifferenceTextField.setText("" + MBCAPParam.collisionRiskAltitudeDifference);
				panel.maxTimeTextField.setText("" + ((double) MBCAPParam.collisionRiskTime) / 1000000000l);
				panel.reactionDistanceTextField.setText("" + MBCAPParam.reactionDistance);
				panel.riskCheckPeriodTextField.setText("" + ((double) MBCAPParam.riskCheckPeriod) / 1000000000l);
				panel.safePlaceDistanceTextField.setText("" + MBCAPParam.safePlaceDistance);
				panel.standStillTimeTextField.setText("" + ((double) MBCAPParam.standStillTimeout) / 1000000000l);
				panel.passingTimeTextField.setText("" + ((double) MBCAPParam.passingTimeout) / 1000000000l);
				panel.solvedTimeTextField.setText("" + ((double) MBCAPParam.solvedTimeout) / 1000000000l);
				panel.deadlockTimeoutTextField.setText("" + (int) (((double) MBCAPParam.globalDeadlockTimeout) / 1000000000l));
			}
		});
	}

	/** Draws the circles that represent the future locations of the UAV. */
	public static void drawPredictedLocations(Graphics2D g) {
		Point3D predictedUTMLocation;
		List<Point3D> locationsUTM;
		Point2D.Double predictedPXLocation;
		Ellipse2D.Double ellipse;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			locationsUTM = MBCAPGUIParam.predictedLocation.get(i);
			if (locationsUTM != null && locationsUTM.size() > 0) {
				g.setColor(GUI.getUAVColor(i));
				for (int j = 0; j < locationsUTM.size(); j++) {
					predictedUTMLocation = locationsUTM.get(j);
					predictedPXLocation = GUI.locatePoint(predictedUTMLocation.x, predictedUTMLocation.y);
					ellipse = new Ellipse2D.Double(predictedPXLocation.x - MBCAPParam.collisionRiskScreenDistance,
							predictedPXLocation.y - MBCAPParam.collisionRiskScreenDistance, 2 * MBCAPParam.collisionRiskScreenDistance,
							2 * MBCAPParam.collisionRiskScreenDistance);
					g.draw(ellipse);
				}
			}
		}
	}

	/** Stores (or removes when p==null) the collision risk location that is drawn. */
	public static void locateImpactRiskMark(Point3D riskUTMLocation, int numUAV, long beaconId) {
		if (!Tools.isRealUAV()) {
			if (riskUTMLocation == null) {
				MBCAPParam.impactLocationPX[numUAV].remove(beaconId);
			} else {
				Point2D.Double riskPXLocation = GUI.locatePoint(riskUTMLocation.x, riskUTMLocation.y);
				MBCAPParam.impactLocationPX[numUAV].put(beaconId, riskPXLocation);
			}
		}
	}
	
	/** Removes all the collision risk locations used for drawing. */
	public static void removeImpactRiskMarks() {
		int numUAVs = Tools.getNumUAVs();
		for (int i=0; i<numUAVs; i++) {
			MBCAPParam.impactLocationUTM[i].clear();
			MBCAPParam.impactLocationPX[i].clear();
		}
	}

	/** Draws the image that represents a collision risk, when needed. */
	public static void drawImpactRiskMarks(Graphics2D g2, BoardPanel p) {
		Iterator<Map.Entry<Long, Point2D.Double>> entries;
		Map.Entry<Long, Point2D.Double> entry;
		Point2D.Double riskLocationPX;
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			entries = MBCAPParam.impactLocationPX[i].entrySet().iterator();
			while (entries.hasNext()) {
				entry = entries.next();
				riskLocationPX = entry.getValue();
				// AffineTransform is applied in reverse order
				AffineTransform trans = new AffineTransform();
				trans.translate(riskLocationPX.x, riskLocationPX.y);
				trans.scale(MBCAPGUIParam.exclamationDrawScale, MBCAPGUIParam.exclamationDrawScale);
				trans.translate(-MBCAPGUIParam.exclamationImage.getWidth() / 2,
						-MBCAPGUIParam.exclamationImage.getHeight() / 2);
				g2.drawImage(MBCAPGUIParam.exclamationImage, trans, p);
			}
		}
	}

	/** Update the protocol state. */
	public static void updateState(int numUAV, MBCAPState state) {
		// Update the protocol state
		MBCAPParam.state[numUAV] = state;
		// Update the record of states used
		MBCAPParam.progress[numUAV].add(new ProgressState(state, System.currentTimeMillis()));

		// Update the log in the main window
		GUI.log(GUI.getUAVPrefix(numUAV) + MBCAPText.CAP + " = " + state.getName());
		// Update the progress dialog
		GUI.updateprotocolState(numUAV, state.getName());
	}

}
