package sim.logic;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.LogPoint;
import main.Param;
import main.Text;
import main.ArduSimTools;
import main.Param.SimulatorState;
import main.Param.WirelessModel;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPParam;
import sim.board.BoardParam;
import sim.gui.ConfigDialogPanel;
import sim.gui.MainWindow;
import sim.gui.ProgressDialogPanel;
import sim.logic.SimParam.RenderQuality;
import uavController.UAVParam;

/** This class contains method used internally by the application for its own profit. */

@SuppressWarnings("unused")
public class SimTools {
	
	/** Updates the MAVLink flight mode on the progress dialog. */
	public static void updateUAVMAVMode(final int numUAV, final String mode) {
		if (MainWindow.progressDialog!=null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.progressDialog.panels[numUAV].MAVModeLabel.setText(mode);
				}
			});
		}
		GUI.log(SimParam.prefix[numUAV] + Text.FLIGHT_MODE + " = " + mode);
	}
	
	/** Loads initial speed of UAVs from CSV file without header.
	 * <p>One value (m/s) per line.
	 * <p>Returns null if the file is not valid or it is empty. */
	public static double[] loadSpeedsFile(String txtFile) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(txtFile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			GUI.log(Text.SPEEDS_PARSING_ERROR_1);
			return null;
		}
		// Only one line per speed value
		List<Double> speedsList = new ArrayList<Double>(lines.size());
		String x;
		for (int i=0; i<lines.size(); i++) {
			x = lines.get(i).trim();
			if (x.length() != 0) {
				try {
					speedsList.add(Double.parseDouble(x));
				} catch (NumberFormatException e) {
					GUI.log(Text.SPEEDS_PARSING_ERROR_2 + " " + (i+1));
					return null;
				}
			}
		}

		double[] speeds = new double[speedsList.size()];
		for (int i=0; i<speeds.length; i++) {
			speeds[i] = speedsList.get(i);
		}
		return speeds;
	}

	/** Checks the validity of the configuration of the experiment */
	public static boolean isValidConfiguration(ConfigDialogPanel panel) {
		//  Simulation parameters
		String validating = panel.arducopterPathTextField.getText();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.SITL_ERROR_3);
			return false;
		}
		validating = panel.speedsTextField.getText();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.SPEEDS_ERROR_2);
			return false;
		}
		validating = (String)panel.UAVsComboBox.getSelectedItem();
		if (validating==null || validating.length()==0) {
			GUI.warn(Text.VALIDATION_WARNING, Text.UAVS_NUMBER_ERROR);
			return false;
		}

		//  Visualization parameters
		validating = (String)panel.screenDelayTextField.getText();
		if (!Tools.isValidInteger(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_1);
			return false;
		}
		int intValue = Integer.parseInt(validating);
		if (intValue < BoardParam.MIN_SCREEN_DELAY || intValue > BoardParam.MAX_SCREEN_DELAY) {
			GUI.warn(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_2);
			return false;
		}
		validating = (String)panel.minScreenMovementTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_1);
			return false;
		}
		double doubleValue = Double.parseDouble(validating);
		if (doubleValue >= BoardParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD) {
			GUI.warn(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_2);
			return false;
		}
		if (panel.batteryCheckBox.isSelected()) {
			validating = (String)panel.batteryTextField.getText();
			if (!Tools.isValidInteger(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_1);
				return false;
			}
			intValue = Integer.parseInt(validating);
			if (intValue > UAVParam.MAX_BATTERY_CAPACITY) {
				GUI.warn(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_2);
				return false;
			}
		}
		
		//  Protocol parameter. Is there a valid implementation?
		ProtocolHelper.selectedProtocol = (String)panel.protocolComboBox.getSelectedItem();
		ProtocolHelper protocolInstance = ArduSimTools.getSelectedProtocolInstance();
		if (protocolInstance == null) {
			GUI.warn(Text.VALIDATION_WARNING, Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + (String)panel.protocolComboBox.getSelectedItem());
			return false;
		}

		//  UAV to UAV communications parameters
		validating = (String)panel.receivingBufferSizeTextField.getText();
		if (!Tools.isValidInteger(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_1);
			return false;
		}
		intValue = Integer.parseInt(validating);
		if (intValue < Tools.DATAGRAM_MAX_LENGTH) {
			GUI.warn(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_2);
			return false;
		}
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			validating = (String)panel.fixedRangeTextField.getText();
			if (!Tools.isValidPositiveDouble(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_1);
				return false;
			}
			doubleValue = Double.parseDouble(validating);
			if (doubleValue >= Param.FIXED_MAX_RANGE) {
				GUI.warn(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_2);
				return false;
			}
		}
		
		// Collision detection parameters
		boolean checkCollision = panel.collisionDetectionCheckBox.isSelected();
		if (checkCollision) {
			validating = (String) panel.collisionCheckPeriodTextField.getText();
			if (!Tools.isValidPositiveDouble(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.COLLISION_PERIOD_ERROR);
				return false;
			}
			validating = (String) panel.collisionDistanceTextField.getText();
			if (!Tools.isValidPositiveDouble(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.COLLISION_DISTANCE_THRESHOLD_ERROR);
				return false;
			}
			validating = (String) panel.collisionAltitudeTextField.getText();
			if (!Tools.isValidPositiveDouble(validating)) {
				GUI.warn(Text.VALIDATION_WARNING,  Text.COLLISION_ALTITUDE_THRESHOLD_ERROR);
				return false;
			}
		}
		
		//  Wind parameters
		if (panel.windCheckBox.isSelected()) {
			validating = (String)panel.windDirTextField.getText();
			if (!Tools.isValidInteger(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.WIND_DIRECTION_ERROR);
				return false;
			}
			validating = (String)panel.windSpeedTextField.getText();
			if (!Tools.isValidPositiveDouble(validating)) {
				GUI.warn(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_1);
				return false;
			}
			if (Double.parseDouble(validating) < UAVParam.WIND_THRESHOLD) {
				GUI.warn(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_2);
				return false;
			}
		}
		return true;
	}

	/** Stores the configuration of the experiment in variables */
	public static void storeConfiguration(ConfigDialogPanel panel) {
		//  Simulation parameters
		Param.numUAVsTemp.set(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));

		//  Performance parameters
		BoardParam.screenDelay = Integer.parseInt((String)panel.screenDelayTextField.getText());
		BoardParam.minScreenMovement = Double.parseDouble((String)panel.minScreenMovementTextField.getText());
		if (panel.loggingEnabledCheckBox.isSelected()) {
			SimParam.arducopterLoggingEnabled = true;
		} else {
			SimParam.arducopterLoggingEnabled = false;
		}
		if (panel.batteryCheckBox.isSelected()) {
			UAVParam.batteryCapacity = Integer.parseInt(panel.batteryTextField.getText());
		} else {
			UAVParam.batteryCapacity = UAVParam.MAX_BATTERY_CAPACITY;
		}
		UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
		if (UAVParam.batteryLowLevel % 50 != 0) {
			UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
		}

		//  Protocol parameters
		ProtocolHelper.selectedProtocol = (String)panel.protocolComboBox.getSelectedItem();
		ProtocolHelper.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();

		//  UAV to UAV communications parameters
		UAVParam.carrierSensingEnabled = panel.carrierSensingCheckBox.isSelected();
		UAVParam.pCollisionEnabled = panel.pCollisionDetectionCheckBox.isSelected();
		UAVParam.receivingBufferSize = Integer.parseInt((String)panel.receivingBufferSizeTextField.getText());
		UAVParam.receivingvBufferSize = UAVParam.V_BUFFER_SIZE_FACTOR * UAVParam.receivingBufferSize;
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			Param.fixedRange = Double.parseDouble((String)panel.fixedRangeTextField.getText());
		}
		
		// Collision detection parameters
		boolean checkCollision = panel.collisionDetectionCheckBox.isSelected();
		UAVParam.collisionCheckEnabled = checkCollision;
		if (checkCollision) {
			UAVParam.collisionCheckPeriod = Double.parseDouble((String) panel.collisionCheckPeriodTextField.getText());
			UAVParam.appliedCollisionCheckPeriod = (int) Math.round(UAVParam.collisionCheckPeriod*1000);
			UAVParam.collisionDistance = Double.parseDouble((String) panel.collisionDistanceTextField.getText());
			UAVParam.collisionAltitudeDifference = Double.parseDouble((String) panel.collisionAltitudeTextField.getText());
			// Distance calculus slightly faster than the collision check frequency
			UAVParam.distanceCalculusPeriod = Math.min(UAVParam.RANGE_CHECK_PERIOD / 2, (int) Math.round(UAVParam.collisionCheckPeriod*950));
		} else {
			UAVParam.distanceCalculusPeriod = UAVParam.RANGE_CHECK_PERIOD / 2;
		}

		//  Wind parameters
		if (panel.windCheckBox.isSelected()) {
			Param.windDirection = Integer.parseInt((String)panel.windDirTextField.getText());
			Param.windSpeed = Double.parseDouble((String)panel.windSpeedTextField.getText());
		} else {
			Param.windDirection = Param.DEFAULT_WIND_DIRECTION;
			Param.windSpeed = Param.DEFAULT_WIND_SPEED;
		}
	}

	/** Loads the default experiment configuration from variables */
	public static void loadDefaultConfiguration(final ConfigDialogPanel panel) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//  Performance parameters
				panel.screenDelayTextField.setText("" + BoardParam.screenDelay);
				panel.minScreenMovementTextField.setText("" + BoardParam.minScreenMovement);
				if (SimParam.arducopterLoggingEnabled) {
					panel.loggingEnabledCheckBox.setSelected(true);
				} else {
					panel.loggingEnabledCheckBox.setSelected(false);
				}
				SimParam.renderQuality = RenderQuality.Q3;
				panel.renderQualityComboBox.setSelectedIndex(RenderQuality.Q3.getId());
				
				panel.batteryCheckBox.setSelected(false);
				panel.batteryTextField.setText("" + UAVParam.STANDARD_BATTERY_CAPACITY);
				panel.batteryTextField.setEnabled(false);
				
				//  Protocol parameters
				ProtocolHelper.selectedProtocol = ProtocolHelper.ProtocolNames[ProtocolHelper.ProtocolNames.length - 1];
				panel.protocolComboBox.setSelectedIndex(ProtocolHelper.ProtocolNames.length - 1);
				
				//  UAV to UAV communications parameters
				panel.carrierSensingCheckBox.setSelected(UAVParam.carrierSensingEnabled);
				panel.pCollisionDetectionCheckBox.setSelected(UAVParam.pCollisionEnabled);
				panel.receivingBufferSizeTextField.setText("" + UAVParam.receivingBufferSize);
				Param.selectedWirelessModel = WirelessModel.NONE;
				panel.wirelessModelComboBox.setSelectedIndex(WirelessModel.NONE.getId());
				panel.fixedRangeTextField.setText("" + Param.fixedRange);
				panel.fixedRangeTextField.setEnabled(false);
				
				// Collision detection parameters
				panel.collisionDetectionCheckBox.setSelected(false);
				panel.collisionCheckPeriodTextField.setText("" + UAVParam.collisionCheckPeriod);
				panel.collisionCheckPeriodTextField.setEnabled(false);
				panel.collisionDistanceTextField.setText("" + UAVParam.collisionDistance);
				panel.collisionDistanceTextField.setEnabled(false);
				panel.collisionAltitudeTextField.setText("" + UAVParam.collisionAltitudeDifference);
				panel.collisionAltitudeTextField.setEnabled(false);
				
				//  Wind parameters
				panel.windDirTextField.setText("" + Param.DEFAULT_WIND_DIRECTION);
				panel.windSpeedTextField.setText("" + Param.DEFAULT_WIND_SPEED);
				panel.lblDegrees.setText(Text.DEGREE_SYMBOL);
				panel.windCheckBox.setSelected(false);
			}
		});
	}
	
	/** Loads the UAV image */
	public static void loadUAVImage() {
		URL url = MainWindow.class.getResource("/" + SimParam.UAV_IMAGE_PATH);
		try {
			SimParam.uavImage = ImageIO.read(url);
			SimParam.uavImageScale = SimParam.UAV_PX_SIZE / SimParam.uavImage.getWidth();
		} catch (IOException e) {
			GUI.exit(Text.LOADING_UAV_IMAGE_ERROR);
		}
	}

	/** Draws the panel content periodically, or stores information for loggin purposes when using a real UAV. */
	public static void update() {
		// Clean the saturated queue if needed
		SimTools.cleanQueue();
		ActionListener taskPerformer = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (Param.IS_REAL_UAV) {
					SimTools.storePath();
				} else {
					MainWindow.boardPanel.repaint();
					SimTools.updateUAVInfo();
				}
			}
		};
		new Timer(BoardParam.screenDelay, taskPerformer).start();
	}
	
	/** Auxiliary method to clean periodically the received position of the UAV. */
	private static void cleanQueue() {
		for (int i=0; i<Param.numUAVs; i++) {
			while (SimParam.uavUTMPathReceiving[i].remainingCapacity() < SimParam.UAV_POS_QUEUE_FULL_THRESHOLD) {
				SimParam.uavUTMPathReceiving[i].poll();
			}
		}
	}
	
	/** Stores the UAV path for logging when using a real UAV. */
	private static void storePath() {
		LogPoint currentUTMLocation;
		for (int i=0; i<Param.numUAVs; i++) {
			while(!SimParam.uavUTMPathReceiving[i].isEmpty()) {
				currentUTMLocation = SimParam.uavUTMPathReceiving[i].poll();
				if (currentUTMLocation != null) {
					SimParam.uavUTMPath[i].add(currentUTMLocation);
				}
			}
		}
	}
	
	/** Updates UAV position and speed on the progress dialog. */
	public static void updateUAVInfo() {
		if (MainWindow.progressDialog!=null) {
			Point2D.Double locationUTM;
			for (int i=0; i<Param.numUAVs; i++) {
				locationUTM = UAVParam.uavCurrentData[i].getUTMLocation();
				if (locationUTM!=null) {
					SimParam.xUTM[i] = locationUTM.x;
					SimParam.yUTM[i] = locationUTM.y;
					SimParam.z[i] = UAVParam.uavCurrentData[i].getZ();
					SimParam.speed[i] = UAVParam.uavCurrentData[i].getSpeed();
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					for (int i=0; i<Param.numUAVs; i++) {
						MainWindow.progressDialog.panels[i].xLabel.setText(String.format("%.2f", SimParam.xUTM[i]));
						MainWindow.progressDialog.panels[i].yLabel.setText(String.format("%.2f", SimParam.yUTM[i]));
						MainWindow.progressDialog.panels[i].zLabel.setText(String.format("%.2f", SimParam.z[i]));
						MainWindow.progressDialog.panels[i].speedLabel.setText(String.format("%.2f", SimParam.speed[i]));
					}
				}
			});
		}
	}
	
	/** Checks if the data packet must arrive to the destination depending on distance and the wireless model used (only used on simulation). */
	public static boolean isInRange(double distance) {
		switch (Param.selectedWirelessModel) {
		case NONE:
			return true;
		case FIXED_RANGE:
			if (distance <= Param.fixedRange) {
				return true;
			} else {
				return false;
			}
		case DISTANCE_5GHZ:
			if (Math.random() <= 5.335*Math.pow(10, -7)*distance*distance + 3.395*Math.pow(10, -5)*distance) {
				return false;
			} else {
				return true;
			}
		}
		
		// Point never reached if the selection structure is enlarged when adding new wireless models
		GUI.log(Text.WIRELESS_ERROR);
		return false;
	}

}
