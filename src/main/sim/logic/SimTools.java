package main.sim.logic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import api.API;
import api.ProtocolHelper;
import api.pojo.location.LogPoint;
import api.pojo.location.Location2DUTM;
import main.ArduSimTools;
import main.Param;
import main.api.ArduSim;
import main.api.ValidationTools;
import main.api.communications.CommLink;
import main.api.communications.CommLinkObject;
import main.api.communications.WirelessModel;
import main.Text;
import main.cpuHelper.CPUUsageThread;
import main.sim.board.BoardParam;
import main.sim.gui.ConfigDialogPanel;
import main.sim.gui.MainWindow;
import main.sim.gui.ProgressDialog;
import main.sim.logic.SimParam.RenderQuality;
import main.uavController.UAVParam;

/** This class contains method used internally by the application for its own profit.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SimTools {
	
	/** Updates the MAVLink flight mode on the progress dialog. */
	public static void updateUAVMAVMode(final int numUAV, final String mode) {
		if (ProgressDialog.progressDialog != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ProgressDialog.progressDialog.panels[numUAV].MAVModeLabel.setText(mode);
				}
			});
		}
		ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.FLIGHT_MODE + " = " + mode);
	}
	
	/** Loads initial speed of UAVs from CSV file without header.
	 * <p>One value (m/s) per line. Returns null if the file is not valid or it is empty.</p> */
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
			ArduSimTools.logGlobal(Text.SPEEDS_PARSING_ERROR_1);
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
					ArduSimTools.logGlobal(Text.SPEEDS_PARSING_ERROR_2 + " " + (i+1));
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
	
	/**
	 * For ArduSim internal purposes only.
	 * @param dialog Dialog where this method enables the escape key.
	 * @param closeArduSim Whether to close ArduSim or not when the key is pressed.
	 */
	public static void addEscListener(final JDialog dialog, final boolean closeArduSim) {
	    ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	dialog.dispose();
				System.gc();
				if (closeArduSim) {
					System.exit(0);
				}
	        }
	    };

	    dialog.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

	}

	/** Checks the validity of the configuration of the experiment */
	public static boolean isValidConfiguration(ConfigDialogPanel panel) {
		ValidationTools validationTools = API.getValidationTools();
		
		//  Simulation parameters
		String validating = panel.arducopterPathTextField.getText();
		if (validationTools.isEmpty(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SITL_ERROR_3);
			return false;
		}
		validating = panel.speedsTextField.getText();
		if (validationTools.isEmpty(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SPEEDS_ERROR_2);
			return false;
		}
		validating = panel.iniAltitudeTextField.getText();
		if (!validationTools.isValidDouble(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.INITIAL_ALTITUDE_ERROR);
			return false;
		}
		validating = (String)panel.UAVsComboBox.getSelectedItem();
		if (validationTools.isEmpty(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.UAVS_NUMBER_ERROR);
			return false;
		}

		//  Visualization parameters
		validating = panel.screenDelayTextField.getText();
		if (!validationTools.isValidPositiveInteger(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_1);
			return false;
		}
		int intValue = Integer.parseInt(validating);
		if (intValue < BoardParam.MIN_SCREEN_DELAY || intValue > BoardParam.MAX_SCREEN_DELAY) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_2);
			return false;
		}
		validating = panel.minScreenMovementTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_1);
			return false;
		}
		double doubleValue = Double.parseDouble(validating);
		if (doubleValue >= BoardParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_2);
			return false;
		}
		if (panel.batteryCheckBox.isSelected()) {
			validating = panel.batteryTextField.getText();
			if (!validationTools.isValidPositiveInteger(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_1);
				return false;
			}
			intValue = Integer.parseInt(validating);
			if (intValue > UAVParam.VIRT_BATTERY_MAX_CAPACITY) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_2);
				return false;
			}
		}
		
		//  Protocol parameter. Is there a valid implementation?
		ArduSimTools.selectedProtocol = (String)panel.protocolComboBox.getSelectedItem();
		ProtocolHelper protocolInstance = ArduSimTools.getSelectedProtocolInstance();
		if (protocolInstance == null) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + (String)panel.protocolComboBox.getSelectedItem());
			return false;
		}

		//  UAV to UAV communications parameters
		validating = panel.receivingBufferSizeTextField.getText();
		if (!validationTools.isValidPositiveInteger(validating)) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_1);
			return false;
		}
		intValue = Integer.parseInt(validating);
		if (intValue < CommLink.DATAGRAM_MAX_LENGTH) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_2);
			return false;
		}
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			validating = panel.fixedRangeTextField.getText();
			if (!validationTools.isValidPositiveDouble(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_1);
				return false;
			}
			doubleValue = Double.parseDouble(validating);
			if (doubleValue >= Param.FIXED_MAX_RANGE) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_2);
				return false;
			}
		}
		
		// Collision detection parameters
		boolean checkCollision = panel.collisionDetectionCheckBox.isSelected();
		if (checkCollision) {
			validating = panel.collisionCheckPeriodTextField.getText();
			if (!validationTools.isValidPositiveDouble(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.COLLISION_PERIOD_ERROR);
				return false;
			}
			validating = panel.collisionDistanceTextField.getText();
			if (!validationTools.isValidPositiveDouble(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.COLLISION_DISTANCE_THRESHOLD_ERROR);
				return false;
			}
			validating = panel.collisionAltitudeTextField.getText();
			if (!validationTools.isValidPositiveDouble(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING,  Text.COLLISION_ALTITUDE_THRESHOLD_ERROR);
				return false;
			}
		}
		
		//  Wind parameters
		if (panel.windCheckBox.isSelected()) {
			validating = panel.windDirTextField.getText();
			if (!validationTools.isValidNonNegativeInteger(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_DIRECTION_ERROR);
				return false;
			}
			validating = panel.windSpeedTextField.getText();
			if (!validationTools.isValidPositiveDouble(validating)) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_1);
				return false;
			}
			if (Double.parseDouble(validating) < UAVParam.WIND_THRESHOLD) {
				ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_2);
				return false;
			}
		}
		return true;
	}

	/** Stores the configuration of the experiment in variables */
	public static void storeConfiguration(ConfigDialogPanel panel) {
		//  Simulation parameters
		UAVParam.initialAltitude = Double.parseDouble(panel.iniAltitudeTextField.getText());
		Param.numUAVsTemp.set(Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem()));

		//  Performance parameters
		BoardParam.screenDelay = Integer.parseInt(panel.screenDelayTextField.getText());
		BoardParam.minScreenMovement = Double.parseDouble(panel.minScreenMovementTextField.getText());
		SimParam.arducopterLoggingEnabled = panel.loggingEnabledCheckBox.isSelected();
		if (panel.batteryCheckBox.isSelected()) {
			UAVParam.batteryCapacity = Integer.parseInt(panel.batteryTextField.getText());
		} else {
			UAVParam.batteryCapacity = UAVParam.VIRT_BATTERY_MAX_CAPACITY;
		}
		UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
		if (UAVParam.batteryLowLevel % 50 != 0) {
			UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
		}
		if (panel.cpuCheckBox.isSelected()) {
			Param.measureCPUEnabled = true;
			new CPUUsageThread().start();
		} else {
			Param.measureCPUEnabled = false;
		}
		BoardParam.downloadBackground = panel.mapCheckBox.isSelected();
		
		// General parameters
		Param.verboseLogging = panel.chckbxLogging.isSelected();
		Param.verboseStore = panel.chckbxStorage.isSelected();
		
		//  Protocol parameters
		ArduSimTools.selectedProtocol = (String)panel.protocolComboBox.getSelectedItem();
		ArduSimTools.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();

		//  UAV to UAV communications parameters
		CommLinkObject.carrierSensingEnabled = panel.carrierSensingCheckBox.isSelected();
		CommLinkObject.pCollisionEnabled = panel.pCollisionDetectionCheckBox.isSelected();
		CommLinkObject.receivingBufferSize = Integer.parseInt(panel.receivingBufferSizeTextField.getText());
		CommLinkObject.receivingvBufferSize = CommLinkObject.V_BUFFER_SIZE_FACTOR * CommLinkObject.receivingBufferSize;
		CommLinkObject.receivingvBufferTrigger = (int)Math.rint(CommLinkObject.BUFFER_FULL_THRESHOLD * CommLinkObject.receivingvBufferSize);
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			Param.fixedRange = Double.parseDouble(panel.fixedRangeTextField.getText());
		}
		
		// Collision detection parameters
		boolean checkCollision = panel.collisionDetectionCheckBox.isSelected();
		UAVParam.collisionCheckEnabled = checkCollision;
		if (checkCollision) {
			UAVParam.collisionCheckPeriod = Double.parseDouble(panel.collisionCheckPeriodTextField.getText());
			UAVParam.appliedCollisionCheckPeriod = Math.round(UAVParam.collisionCheckPeriod * 1000);
			UAVParam.collisionDistance = Double.parseDouble(panel.collisionDistanceTextField.getText());
			UAVParam.collisionAltitudeDifference = Double.parseDouble(panel.collisionAltitudeTextField.getText());
			// Distance calculus slightly faster than the collision check frequency
			UAVParam.distanceCalculusPeriod = Math.min(CommLinkObject.RANGE_CHECK_PERIOD / 2, Math.round(UAVParam.collisionCheckPeriod * 950));
		} else {
			UAVParam.distanceCalculusPeriod = CommLinkObject.RANGE_CHECK_PERIOD / 2;
		}

		//  Wind parameters
		if (panel.windCheckBox.isSelected()) {
			Param.windDirection = Integer.parseInt(panel.windDirTextField.getText());
			Param.windSpeed = Double.parseDouble(panel.windSpeedTextField.getText());
		} else {
			Param.windDirection = Param.DEFAULT_WIND_DIRECTION;
			Param.windSpeed = Param.DEFAULT_WIND_SPEED;
		}
	}

	/** Loads the default experiment configuration from variables */
	public static void loadDefaultConfiguration(final ConfigDialogPanel panel) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Simulation parameters
				panel.iniAltitudeTextField.setText("" + UAVParam.initialAltitude);
				
				//  Performance parameters
				panel.screenDelayTextField.setText("" + BoardParam.screenDelay);
				panel.minScreenMovementTextField.setText("" + BoardParam.minScreenMovement);
				panel.loggingEnabledCheckBox.setSelected(SimParam.arducopterLoggingEnabled);
				panel.mapCheckBox.setSelected(BoardParam.downloadBackground);
				SimParam.renderQuality = RenderQuality.Q3;
				panel.renderQualityComboBox.setSelectedIndex(RenderQuality.Q3.getId());
				
				panel.batteryCheckBox.setSelected(false);
				panel.batteryTextField.setText("" + UAVParam.lipoBatteryCapacity);
				panel.batteryTextField.setEnabled(false);
				
				//  Protocol parameters
				ArduSimTools.selectedProtocol = ArduSimTools.ProtocolNames[ArduSimTools.ProtocolNames.length - 1];
				panel.protocolComboBox.setSelectedIndex(ArduSimTools.ProtocolNames.length - 1);
				
				//  UAV to UAV communications parameters
				panel.carrierSensingCheckBox.setSelected(CommLinkObject.carrierSensingEnabled);
				panel.pCollisionDetectionCheckBox.setSelected(CommLinkObject.pCollisionEnabled);
				panel.receivingBufferSizeTextField.setText("" + CommLinkObject.receivingBufferSize);
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
		URL url = MainWindow.class.getResource(SimParam.UAV_IMAGE_PATH);
		try {
			SimParam.uavImage = ImageIO.read(url);
			SimParam.uavImageScale = SimParam.UAV_PX_SIZE / SimParam.uavImage.getWidth();
		} catch (IOException e) {
			ArduSimTools.closeAll(Text.LOADING_UAV_IMAGE_ERROR);
		}
	}

	/** Draws the panel content periodically, or stores information for logging purposes when using a real UAV. */
	public static void update() {
		// Clean the saturated queue if needed
		SimTools.cleanQueue();
		ActionListener taskPerformer = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (Param.role == ArduSim.MULTICOPTER) {
					SimTools.storePath();
				} else if (Param.role == ArduSim.SIMULATOR) {
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
		if (ProgressDialog.progressDialog != null) {
			Location2DUTM locationUTM;
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
						ProgressDialog.progressDialog.panels[i].xLabel.setText(String.format("%.2f", SimParam.xUTM[i]));
						ProgressDialog.progressDialog.panels[i].yLabel.setText(String.format("%.2f", SimParam.yUTM[i]));
						ProgressDialog.progressDialog.panels[i].zLabel.setText(String.format("%.2f", SimParam.z[i]));
						ProgressDialog.progressDialog.panels[i].speedLabel.setText(String.format("%.2f", SimParam.speed[i]));
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
		ArduSimTools.logGlobal(Text.WIRELESS_ERROR);
		return false;
	}

}
