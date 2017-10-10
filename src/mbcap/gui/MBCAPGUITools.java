package mbcap.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import api.GUIHelper;
import api.MissionHelper;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.logic.MBCAPText;
import mbcap.pojo.ProgressState;
import sim.board.BoardPanel;
import sim.board.BoardParam;
import sim.gui.MainWindow;
import sim.logic.SimParam;
import uavController.UAVParam;

/** This class contains exclusively static methods used by the GUI. */

public class MBCAPGUITools {

	/** Checks the validity of the configuration of the protocol. */
	public static boolean isValidProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Connection parameters
		if (!GUIHelper.isValidPort((String) panel.portTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.PORT_ERROR);
			return false;
		}

		// Beaconing parameters
		if (!GUIHelper.isValidInteger((String) panel.beaconingPeriodTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.BEACON_PERIOD_ERROR);
			return false;
		}
		if (!GUIHelper.isValidInteger((String) panel.numBeaconsTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.BEACON_REFRESH_ERROR);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.beaconExpirationTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.BEACON_EXPIRATION_ERROR);
			return false;
		}
		double beaconExpirationTime = Double.parseDouble((String) panel.beaconExpirationTimeTextField.getText());
		if (!GUIHelper.isValidPositiveDouble((String) panel.beaconFlyingTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_1);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.hopTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.HOP_TIME_ERROR);
			return false;
		}
		double flyingTime = Double.parseDouble(panel.beaconFlyingTimeTextField.getText());
		double maxFlyingTime = (MBCAPParam.MAX_POINTS-1)*MBCAPParam.hopTime;
		if (flyingTime > maxFlyingTime) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_2 + " "
					+ String.format( "%.2f", maxFlyingTime ) + " " + Text.SECONDS + ".");
			return false;
		}
		double hopTime = Double.parseDouble(panel.hopTimeTextField.getText());
		double points = flyingTime/hopTime;
		if (Math.floor(points) - points != 0) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.FLYING_TIME_ERROR_3);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.minSpeedTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.MIN_SPEED_ERROR);
			return false;
		}

		// Collision detection parameters
		if (!GUIHelper.isValidPositiveDouble((String) panel.collisionCheckPeriodTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.COLLISION_PERIOD_ERROR);
			return false;
		}
		String validating = (String) panel.collisionDistanceTextField.getText();
		if (!GUIHelper.isValidPositiveDouble(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.COLLISION_DET_THRESHOLD_ERROR);
			return false;
		}
		double collisionDistance = Double.parseDouble(validating);

		// Collision avoidance protocol parameters
		validating = (String) panel.collisionRiskDistanceTextField.getText();
		if (!GUIHelper.isValidPositiveDouble(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.WARN_DISTANCE_ERROR);
			return false;
		}
		double collisionRiskDistance = Double.parseDouble(validating);
		if (!GUIHelper.isValidPositiveDouble((String) panel.collisionRiskAltitudeDifferenceTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.WARN_ALTITUDE_ERROR);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.maxTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.WARN_TIME_ERROR);
			return false;
		}
		validating = (String) panel.reactionDistanceTextField.getText();
		if (!GUIHelper.isValidPositiveDouble(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.CHECK_THRESHOLD_ERROR_1);
			return false;
		}
		double reactionDistance = Double.parseDouble(validating);
		if (reactionDistance <= collisionRiskDistance) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.CHECK_THRESHOLD_ERROR_2);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.riskCheckPeriodTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.CHECK_PERIOD_ERROR);
			return false;
		}
		validating = (String) panel.safePlaceDistanceTextField.getText();
		if (!GUIHelper.isValidPositiveDouble(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.SAFE_DISTANCE_ERROR_1);
			return false;
		}
		double safePlaceDistance = Double.parseDouble(validating);
		if (safePlaceDistance <= collisionDistance) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.SAFE_DISTANCE_ERROR_2);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.standStillTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.HOVERING_TIMEOUT_ERROR);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.passingTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.OVERTAKE_TIMEOUT_ERROR);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.solvedTimeTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.RESUME_MODE_DELAY_ERROR);
			return false;
		}
		if (!GUIHelper.isValidPositiveDouble((String) panel.deadlockTimeoutTextField.getText())) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_1);
			return false;
		}
		double deadlockTimeout = Double.parseDouble((String) panel.deadlockTimeoutTextField.getText());
		if (deadlockTimeout < beaconExpirationTime) {
			GUIHelper.warn(Text.VALIDATION_WARNING, MBCAPText.DEADLOCK_TIMEOUT_ERROR_2);
			return false;
		}
		return true;
	}

	/** Stores the configuration of the protocol in variables. */
	public static void storeProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Connection parameters
		MBCAPParam.MBCAPport = Integer.parseInt((String) panel.portTextField.getText());

		// Beaconing parameters
		MBCAPParam.beaconingPeriod = Integer.parseInt((String) panel.beaconingPeriodTextField.getText());
		MBCAPParam.numBeacons = Integer.parseInt((String) panel.numBeaconsTextField.getText());
		MBCAPParam.beaconExpirationTime = (long) (Double
				.parseDouble((String) panel.beaconExpirationTimeTextField.getText()) * 1000000000l);
		MBCAPParam.beaconFlyingTime = Double.parseDouble((String) panel.beaconFlyingTimeTextField.getText());

		MBCAPParam.hopTime = Double.parseDouble((String)panel.hopTimeTextField.getText());
		MBCAPParam.hopTimeNS = (long) (MBCAPParam.hopTime * 1000000000l);
		MBCAPParam.minSpeed = Double.parseDouble((String) panel.minSpeedTextField.getText());

		// Collision detection parameters
		MBCAPParam.collisionCheckPeriod = (long) (Double
				.parseDouble((String) panel.collisionCheckPeriodTextField.getText()) * 1000000000l);
		MBCAPParam.collisionDistance = Double.parseDouble((String) panel.collisionDistanceTextField.getText());

		// Collision avoidance protocol
		MBCAPParam.collisionRiskDistance = Double
				.parseDouble((String) panel.collisionRiskDistanceTextField.getText());
		MBCAPParam.collisionRiskAltitudeDifference = Double
				.parseDouble((String) panel.collisionRiskAltitudeDifferenceTextField.getText());
		MBCAPParam.maxTime = (long) (Double.parseDouble((String) panel.maxTimeTextField.getText()) * 1000000000l);
		MBCAPParam.reactionDistance = Double.parseDouble((String) panel.reactionDistanceTextField.getText());
		MBCAPParam.riskCheckPeriod = (long) (Double.parseDouble((String) panel.riskCheckPeriodTextField.getText())
				* 1000000000l);
		MBCAPParam.safePlaceDistance = Double.parseDouble((String) panel.safePlaceDistanceTextField.getText());
		MBCAPParam.standStillTimeout = (long) (Double.parseDouble((String) panel.standStillTimeTextField.getText()) * 1000000000l);
		MBCAPParam.passingTimeout = (long) (Double.parseDouble((String) panel.passingTimeTextField.getText()) * 1000000000l);
		MBCAPParam.solvedTimeout = (long) (Double.parseDouble((String) panel.solvedTimeTextField.getText()) * 1000000000l);
		MBCAPParam.deadlockTimeout = Integer.parseInt((String) panel.deadlockTimeoutTextField.getText())
				* 1000000000l;
	}

	/** Loads the default protocol configuration from variables. */
	public static void loadDefaultProtocolConfiguration(MBCAPConfigDialogPanel panel) {
		// Connection parameters
		panel.portTextField.setText("" + MBCAPParam.MBCAPport);

		// Beaconing parameters
		panel.beaconingPeriodTextField.setText("" + MBCAPParam.beaconingPeriod);
		panel.numBeaconsTextField.setText("" + MBCAPParam.numBeacons);
		panel.beaconExpirationTimeTextField.setText("" + ((double) MBCAPParam.beaconExpirationTime) / 1000000000l);
		panel.beaconFlyingTimeTextField.setText("" + MBCAPParam.beaconFlyingTime);
		panel.hopTimeTextField.setText("" + MBCAPParam.hopTime);
		panel.minSpeedTextField.setText("" + MBCAPParam.minSpeed);

		// Collision detection parameters
		panel.collisionCheckPeriodTextField.setText("" + ((double) MBCAPParam.collisionCheckPeriod) / 1000000000l);
		panel.collisionDistanceTextField.setText("" + MBCAPParam.collisionDistance);

		// Collision avoidance protocol parameters
		panel.collisionRiskDistanceTextField.setText("" + MBCAPParam.collisionRiskDistance);
		panel.collisionRiskAltitudeDifferenceTextField.setText("" + MBCAPParam.collisionRiskAltitudeDifference);
		panel.maxTimeTextField.setText("" + ((double) MBCAPParam.maxTime) / 1000000000l);
		panel.reactionDistanceTextField.setText("" + MBCAPParam.reactionDistance);
		panel.riskCheckPeriodTextField.setText("" + ((double) MBCAPParam.riskCheckPeriod) / 1000000000l);
		panel.safePlaceDistanceTextField.setText("" + MBCAPParam.safePlaceDistance);
		panel.standStillTimeTextField.setText("" + ((double) MBCAPParam.standStillTimeout) / 1000000000l);
		panel.passingTimeTextField.setText("" + ((double) MBCAPParam.passingTimeout) / 1000000000l);
		panel.solvedTimeTextField.setText("" + ((double) MBCAPParam.solvedTimeout) / 1000000000l);
		panel.deadlockTimeoutTextField.setText("" + (int) (((double) MBCAPParam.deadlockTimeout) / 1000000000l));
	}

	/** Loads the file of the image that represents a risk of collision. */
	public static void loadRiskImage() {
		URL url = MainWindow.class.getResource(MBCAPGUIParam.EXCLAMATION_IMAGE_PATH);
		try {
			MBCAPGUIParam.exclamationImage = ImageIO.read(url);
			MBCAPGUIParam.exclamationDrawScale = MBCAPGUIParam.EXCLAMATION_PX_SIZE
					/ MBCAPGUIParam.exclamationImage.getWidth();
		} catch (IOException e) {
			GUIHelper.exit(MBCAPText.WARN_IMAGE_LOAD_ERROR);
		}
	}

	/** Draws the circles that represent the future locations of the UAV. */
	public static void drawPredictedLocations(Graphics2D g) {
		Point3D predictedUTMLocation;
		List<Point3D> locationsUTM;
		Point2D.Double predictedPXLocation;
		Ellipse2D.Double ellipse;
		for (int i = 0; i < Param.numUAVs; i++) {
			locationsUTM = MBCAPGUIParam.predictedLocation.get(i);
			if (locationsUTM != null && locationsUTM.size() > 0) {
				g.setColor(SimParam.COLOR[i % SimParam.COLOR.length]);
				for (int j = 0; j < locationsUTM.size(); j++) {
					predictedUTMLocation = locationsUTM.get(j);
					predictedPXLocation = GUIHelper.locatePoint(predictedUTMLocation.x, predictedUTMLocation.y);
					ellipse = new Ellipse2D.Double(predictedPXLocation.x - MBCAPParam.collisionRiskScreenDistance,
							predictedPXLocation.y - MBCAPParam.collisionRiskScreenDistance, 2 * MBCAPParam.collisionRiskScreenDistance,
							2 * MBCAPParam.collisionRiskScreenDistance);
					g.draw(ellipse);
				}
			}
		}
	}

	/** Draws the circle that represents the collision distance around a UAV. */
	public static void drawCollisionCircle(Graphics2D g) {
		LogPoint currentPXLocation;
		Ellipse2D.Double ellipse;
		for (int i=0; i<Param.numUAVs; i++) {
			// Only drawn when there is known current position
			currentPXLocation = BoardParam.uavCurrentPXLocation[i];
			if (currentPXLocation != null) {
				if ((Param.simStatus == SimulatorState.TEST_IN_PROGRESS
						|| Param.simStatus == SimulatorState.TEST_FINISHED)
						&& UAVParam.flightMode.get(i).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
					g.setColor(Color.RED);
					ellipse = new Ellipse2D.Double(
							currentPXLocation.x-MBCAPParam.collisionScreenDistance, currentPXLocation.y-MBCAPParam.collisionScreenDistance,
							2*MBCAPParam.collisionScreenDistance, 2*MBCAPParam.collisionScreenDistance
							);
					g.draw(ellipse);
				}
			}
		}
	}
	
	/** Rescale the safety circles. */
	public static void rescaleSafetyCircles() {
		Point2D.Double locationUTM = null;
		boolean found = false;
		for (int i=0; i<Param.numUAVs && !found; i++) {
			locationUTM = UAVParam.uavCurrentData[i].getUTMLocation();
			if (locationUTM != null) {
				found = true;
			}
		}
		Point2D.Double a = GUIHelper.locatePoint(locationUTM.x, locationUTM.y);
		Point2D.Double b = GUIHelper.locatePoint(locationUTM.x + MBCAPParam.collisionRiskDistance, locationUTM.y);
		MBCAPParam.collisionRiskScreenDistance =b.x - a.x;
		b = GUIHelper.locatePoint(locationUTM.x + MBCAPParam.collisionDistance, locationUTM.y);
		MBCAPParam.collisionScreenDistance = b.x - a.x;
	}
	
	/** Stores (or removes when p==null) the collision risk location that is drawn. */
	public static void locateImpactRiskMark(Point3D riskUTMLocation, int numUAV, int beaconId) {
		MBCAPGUIParam.impactLocationUTM[numUAV][beaconId] = riskUTMLocation; // uavId==numUAV in the simulator
		if (riskUTMLocation == null) {
			MBCAPGUIParam.impactLocationPX[numUAV][beaconId] = null;
		} else {
			Point2D.Double riskGeoLocation = GUIHelper.locatePoint(riskUTMLocation.x, riskUTMLocation.y);
			MBCAPGUIParam.impactLocationPX[numUAV][beaconId] = new Point3D(riskGeoLocation.x, riskGeoLocation.y, riskUTMLocation.z);
		}
	}
	
	/** Removes all the collision risk locations used for drawing. */
	public static void removeImpactRiskMarks() {
		for (int i=0; i<MBCAPGUIParam.impactLocationUTM.length; i++) {
			for (int j=0; j<MBCAPGUIParam.impactLocationUTM[0].length; j++) {
				MBCAPGUIParam.impactLocationUTM[i][j] = null;
				MBCAPGUIParam.impactLocationPX[i][j] = null;
			}
		}
	}

	/** Draws the image that represents a collision risk, when needed. */
	public static void drawImpactRiskMarks(Graphics2D g2, BoardPanel p) {
		for (int i = 0; i < Param.numUAVs; i++) {
			for (int j = 0; j < Param.numUAVs; j++) {
				if (MBCAPGUIParam.impactLocationPX[i][j] != null) {
					// AffineTransform is applied in reverse order
					AffineTransform trans = new AffineTransform();
					synchronized (MBCAPGUIParam.impactLocationPX[i][j]) {
						trans.translate(MBCAPGUIParam.impactLocationPX[i][j].x,
								MBCAPGUIParam.impactLocationPX[i][j].y);
					}
					trans.scale(MBCAPGUIParam.exclamationDrawScale, MBCAPGUIParam.exclamationDrawScale);
					trans.translate(-MBCAPGUIParam.exclamationImage.getWidth() / 2,
							-MBCAPGUIParam.exclamationImage.getHeight() / 2);
					g2.drawImage(MBCAPGUIParam.exclamationImage, trans, p);
				}
			}
		}
	}

	/** Calculates the screen position of the points where the collision risk was detected. */
	public static void rescaleImpactRiskMarkPoints() {
		Point3D riskLocationUTM;
		Point2D.Double riskLocationGeo;
		for (int i = 0; i < Param.numUAVs; i++) {
			for (int j = 0; j < Param.numUAVs; j++) {
				if (MBCAPGUIParam.impactLocationUTM[i][j] != null) {
					synchronized (MBCAPGUIParam.impactLocationUTM[i][j]) {
						riskLocationUTM = MBCAPGUIParam.impactLocationUTM[i][j];
						riskLocationGeo = GUIHelper.locatePoint(riskLocationUTM.x, riskLocationUTM.y);
						MBCAPGUIParam.impactLocationPX[i][j] = new Point3D( riskLocationGeo.x, riskLocationGeo.y, riskLocationUTM.z);
					}
				}
			}
		}
	}

	/** Adds the protocol states times to the results String. */
	public static String getMBCAPResults() {
		// 1. Calculus of the experiment length and protocol times
		long[] uavsTotalTime = new long[Param.numUAVs];
		for (int i = 0; i < Param.numUAVs; i++) {
			uavsTotalTime[i] = Param.testEndTime[i] - Param.startTime;
		}
		StringBuilder sb = new StringBuilder(2000);
		long[] uavNormalTime = new long[Param.numUAVs];
		long[] uavStandStillTime = new long[Param.numUAVs];
		long[] uavMovingTime = new long[Param.numUAVs];
		long[] uavGoOnPleaseTime = new long[Param.numUAVs];
		long[] uavPassingTime = new long[Param.numUAVs];
		long[] uavEmergencyLandTime = new long[Param.numUAVs];
		for (int i = 0; i < Param.numUAVs; i++) {
			sb.append(Text.UAV_ID + " " + Param.id[i] + "\n");
			if (MBCAPParam.progress[i].size() == 0) {
				// In this case, only the global time is available
				uavNormalTime[i] = Param.testEndTime[i] - Param.startTime;
			} else {
				// Different steps are available, and calculated
				ProgressState[] progress = MBCAPParam.progress[i].toArray(new ProgressState[MBCAPParam.progress[i].size()]);

				MBCAPState iniState = MBCAPState.NORMAL;
				long iniTime = Param.startTime;
				MBCAPState curState;
				long curTime;
				for (int j = 0; j < progress.length; j++) {
					curState = progress[j].state;
					curTime = progress[j].time;
					switch (iniState) {
					case NORMAL:
						uavNormalTime[i] = uavNormalTime[i] + curTime - iniTime;
						break;
					case STAND_STILL:
						uavStandStillTime[i] = uavStandStillTime[i] + curTime - iniTime;
						break;
					case MOVING_ASIDE:
						uavMovingTime[i] = uavMovingTime[i] + curTime - iniTime;
						break;
					case GO_ON_PLEASE:
						uavGoOnPleaseTime[i] = uavGoOnPleaseTime[i] + curTime - iniTime;
						break;
					case OVERTAKING:
						uavPassingTime[i] = uavPassingTime[i] + curTime - iniTime;
						break;
					case EMERGENCY_LAND:
						uavEmergencyLandTime[i] = uavEmergencyLandTime[i] + curTime - iniTime;
						break;
					}
					iniState = curState;
					iniTime = curTime;
				}
				// From the last to the testEndTime
				switch (progress[progress.length - 1].state) {
				case NORMAL:
					uavNormalTime[i] = uavNormalTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				case STAND_STILL:
					uavStandStillTime[i] = uavStandStillTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				case MOVING_ASIDE:
					uavMovingTime[i] = uavMovingTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				case GO_ON_PLEASE:
					uavGoOnPleaseTime[i] = uavGoOnPleaseTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				case OVERTAKING:
					uavPassingTime[i] = uavPassingTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				case EMERGENCY_LAND:
					uavEmergencyLandTime[i] = uavEmergencyLandTime[i] + Param.testEndTime[i] - progress[progress.length - 1].time;
					break;
				}
			}
			sb.append(MBCAPState.NORMAL.getName() + " = " + GUIHelper.timeToString(0, uavNormalTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavNormalTime[i] / (double) uavsTotalTime[i]) + ")\n"
					+ MBCAPState.STAND_STILL.getName() + " = " + GUIHelper.timeToString(0, uavStandStillTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavStandStillTime[i] / (double) uavsTotalTime[i]) + ")\n"
					+ MBCAPState.MOVING_ASIDE.getName() + " = " + GUIHelper.timeToString(0, uavMovingTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavMovingTime[i] / (double) uavsTotalTime[i]) + ")\n"
					+ MBCAPState.GO_ON_PLEASE.getName() + " = " + GUIHelper.timeToString(0, uavGoOnPleaseTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavGoOnPleaseTime[i] / (double) uavsTotalTime[i]) + ")\n"
					+ MBCAPState.OVERTAKING.getName() + " = " + GUIHelper.timeToString(0, uavPassingTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavPassingTime[i] / (double) uavsTotalTime[i]) + ")\n"
					+ MBCAPState.EMERGENCY_LAND.getName() + " = " + GUIHelper.timeToString(0, uavEmergencyLandTime[i]) + " ("
					+ String.format("%.2f%%", 100 * uavEmergencyLandTime[i] / (double) uavsTotalTime[i]) + ")\n");
		}
		return sb.toString();
	}

	/** Adds the protocol configuration to the results String. */
	public static String getMBCAPConfiguration() {
		StringBuilder sb = new StringBuilder(2000);
		if (Param.IS_REAL_UAV) {
			sb.append(MBCAPText.BROADCAST_PORT);
		} else {
			sb.append(MBCAPText.BROADCAST_SIM_PORT);
		}
		sb.append(" " + MBCAPParam.MBCAPport
				+ "\n" + MBCAPText.BEACONING_PARAM + "\n\t" + MBCAPText.BEACON_INTERVAL + " " + MBCAPParam.beaconingPeriod
				+ " " + Text.MILLISECONDS + "\n\t" + MBCAPText.BEACON_REFRESH + " " + MBCAPParam.numBeacons
				+ "\n\t" + MBCAPText.BEACON_EXPIRATION + " "
				+ String.format( "%.2f", MBCAPParam.beaconExpirationTime*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.TIME_WINDOW + " " + MBCAPParam.beaconFlyingTime
				+ " " + Text.MILLISECONDS + "\n\t" + MBCAPText.INTERSAMPLE + " " + MBCAPParam.hopTime
				+ " " + Text.MILLISECONDS + "\n\t" + MBCAPText.MIN_ADV_SPEED + " " + MBCAPParam.minSpeed
				+ " " + Text.METERS_PER_SECOND + "\n" + MBCAPText.COLLISION_PARAM + "\n\t"
				+ MBCAPText.COLLISION_PERIOD + " "
				+ String.format( "%.2f", MBCAPParam.collisionCheckPeriod*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.COLLISION_DET_THRESHOLD + " " + MBCAPParam.collisionDistance
				+ " " + Text.METERS + "\n" + MBCAPText.AVOID_PARAM + "\n\t" + MBCAPText.WARN_DISTANCE + " "
				+ MBCAPParam.collisionRiskDistance + " " + Text.METERS + "\n\t" + MBCAPText.WARN_ALTITUDE + " "
				+ MBCAPParam.collisionRiskAltitudeDifference + " " + Text.METERS + "\n\t"
				+ MBCAPText.WARN_TIME + " "
				+ String.format( "%.2f", MBCAPParam.maxTime*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.CHECK_THRESHOLD + " " + MBCAPParam.reactionDistance
				+ " " + Text.METERS + "\n\t" + MBCAPText.CHECK_PERIOD + " "
				+ String.format( "%.2f", MBCAPParam.riskCheckPeriod*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.SAFE_DISTANCE + " " + MBCAPParam.safePlaceDistance
				+ " " + Text.METERS + "\n\t" + MBCAPText.HOVERING_TIMEOUT + " "
				+ String.format( "%.2f", MBCAPParam.standStillTimeout*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.OVERTAKE_TIMEOUT + " "
				+ String.format( "%.2f", MBCAPParam.passingTimeout*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.RESUME_MODE_DELAY + " "
				+ String.format( "%.2f", MBCAPParam.solvedTimeout*0.000000001 )
				+ " " + Text.SECONDS + "\n\t" + MBCAPText.DEADLOCK_TIMEOUT + " "
				+ String.format( "%.2f", MBCAPParam.deadlockTimeout*0.000000001 ) + " " + Text.SECONDS);
		return sb.toString();
	}

	/** Update the protocol state. */
	public static void updateState(int numUAV, MBCAPState state) {
		// Update the protocol state
		MBCAPParam.state[numUAV] = state;
		// Update the record of states used
		MBCAPParam.progress[numUAV].add(new ProgressState(state, System.currentTimeMillis()));

		// Update the log in the main window
		MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.CAP + " = " + state.getName());
		// Update the progress dialog
		MissionHelper.setMissionState(numUAV, state.getName());
	}

}
