package sim.logic;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
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

import api.GUIHelper;
import api.pojo.LogPoint;
import main.Param;
import main.Text;
import main.Param.Protocol;
import main.Param.SimulatorState;
import main.Param.WirelessModel;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPParam;
import mission.MissionText;
import sim.board.BoardParam;
import sim.gui.ConfigDialogPanel;
import sim.gui.MainWindow;
import sim.gui.ProgressDialogPanel;
import sim.logic.SimParam.RenderQuality;
import uavController.UAVParam;

/** This class contains method used internally by the application for its own profit. */

@SuppressWarnings("unused")
public class SimTools {
	
	/** Sends information to the main window log and console. */
	public static void println(String text) {
		String res;
		if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			res = GUIHelper.timeToString(Param.startTime, System.currentTimeMillis())
					+ " " + text;
		} else {
			res = text;
		}
		System.out.println(res);
		// Update GUI only when using simulator and the main window is already loaded
		if (!Param.IS_REAL_UAV && MainWindow.buttonsPanel != null && MainWindow.buttonsPanel.logArea != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.logArea.append(res + "\n");
					int pos = MainWindow.buttonsPanel.logArea.getText().length();
					MainWindow.buttonsPanel.logArea.setCaretPosition( pos );
				}
			});
		}
	}
	
	/** Sends information to the main window upper-right corner label when a protocol needs it. */
	public static void updateGlobalInformation(String text) {
		// Update GUI only when using simulator
		if (!Param.IS_REAL_UAV) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(text);
				}
			});
		}
	}

	/** Updates the MAVLink flight mode on the progress dialog. */
	public static void updateUAVMAVMode(int numUAV, String mode) {
		if (MainWindow.progressDialog!=null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.progressDialog.panels[numUAV].MAVModeLabel.setText(mode);
				}
			});
		}
		SimTools.println(SimParam.prefix[numUAV] + Text.FLIGHT_MODE + " = " + mode);
	}
	
	/** Updates the protocol state on the progress dialog. */
	public static void updateprotocolState(int numUAV, String state) {
		if (MainWindow.progressDialog != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.progressDialog.panels[numUAV].protStateLabel.setText(state);
				}
			});
		}
	}

	/** Loads initial speed of UAVs from CSV file without header.
	 * <p>One value (m/s) per line.
	 * <p>Returns null if the file is not valid or it is empty. */
	public static double[] loadSpeedsFile(String txtFile) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(txtFile))) {
			lines = br.lines().collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			SimTools.println(Text.SPEEDS_PARSING_ERROR_1);
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
					SimTools.println(Text.SPEEDS_PARSING_ERROR_2 + " " + (i+1));
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
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.SITL_ERROR_3);
			return false;
		}
		if (Param.simulationIsMissionBased) {
			validating = panel.missionsTextField.getText();
			if (validating==null || validating.length()==0) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
				return false;
			}
		}
		validating = panel.speedsTextField.getText();
		if (validating==null || validating.length()==0) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.SPEEDS_ERROR_2);
			return false;
		}
		validating = (String)panel.UAVsComboBox.getSelectedItem();
		if (validating==null || validating.length()==0) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.UAVS_NUMBER_ERROR);
			return false;
		}

		//  Visualization parameters
		validating = (String)panel.screenDelayTextField.getText();
		if (!GUIHelper.isValidInteger(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_1);
			return false;
		}
		int intValue = Integer.parseInt(validating);
		if (intValue < BoardParam.MIN_SCREEN_DELAY || intValue > BoardParam.MAX_SCREEN_DELAY) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_2);
			return false;
		}

		validating = (String)panel.minScreenMovementTextField.getText();
		if (!GUIHelper.isValidPositiveDouble(validating)) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_1);
			return false;
		}
		double doubleValue = Double.parseDouble(validating);
		if (doubleValue >= BoardParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD) {
			GUIHelper.warn(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_2);
			return false;
		}

		//  Wireless model parameters
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			validating = (String)panel.fixedRangeTextField.getText();
			if (!GUIHelper.isValidPositiveDouble(validating)) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_1);
				return false;
			}
			doubleValue = Double.parseDouble(validating);
			if (doubleValue >= Param.FIXED_MAX_RANGE) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_2);
				return false;
			}
		}

		//  Wind parameters
		if (panel.useWindButton.isSelected()) {
			validating = (String)panel.windDirTextField.getText();
			if (!GUIHelper.isValidDouble(validating)) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.WIND_DIRECTION_ERROR);
				return false;
			}
			validating = (String)panel.windSpeedTextField.getText();
			if (!GUIHelper.isValidPositiveDouble(validating)) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_1);
				return false;
			}
			if (Double.parseDouble(validating) < UAVParam.WIND_THRESHOLD) {
				GUIHelper.warn(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_2);
				return false;
			}
		}
		return true;
	}

	/** Stores the configuration of the experiment in variables */
	public static void storeConfiguration(ConfigDialogPanel panel) {
		//  Simulation parameters
		Param.numUAVs = Integer.parseInt((String)panel.UAVsComboBox.getSelectedItem());

		//  Performance parameters
		BoardParam.screenDelay = Integer.parseInt((String)panel.screenDelayTextField.getText());
		BoardParam.minScreenMovement = Double.parseDouble((String)panel.minScreenMovementTextField.getText());
		String loggingEnabled = (String)panel.loggingEnabledButton.getText();
		if (loggingEnabled.equals(Text.YES_OPTION)) {
			SimParam.arducopterLoggingEnabled = true;
		} else {
			SimParam.arducopterLoggingEnabled = false;
		}

		//  CAP protocol parameters
		String protocol = (String)panel.protocolComboBox.getSelectedItem();
		Param.selectedProtocol = Protocol.getProtocolByName(protocol);

		//  Wireless model parameters
		if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
			Param.fixedRange = Double.parseDouble((String)panel.fixedRangeTextField.getText());
		}

		//  Wind parameters
		if (panel.useWindButton.isSelected()) {
			Param.windDirection = Double.parseDouble((String)panel.windDirTextField.getText());
			Param.windSpeed = Double.parseDouble((String)panel.windSpeedTextField.getText());
		} else {
			Param.windDirection = Param.DEFAULT_WIND_DIRECTION;
			Param.windSpeed = Param.DEFAULT_WIND_SPEED;
		}
	}

	/** Loads the default experiment configuration from variables */
	public static void loadDefaultConfiguration(ConfigDialogPanel panel) {
		//  Performance parameters
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.screenDelayTextField.setText("" + BoardParam.screenDelay);
				panel.minScreenMovementTextField.setText("" + BoardParam.minScreenMovement);
				if (SimParam.arducopterLoggingEnabled) {
					panel.loggingEnabledButton.setText(Text.YES_OPTION);
				} else {
					panel.loggingEnabledButton.setText(Text.NO_OPTION);
				}
				panel.renderQualityComboBox.setSelectedIndex(RenderQuality.Q3.getId());
			}
		});
		SimParam.renderQuality = RenderQuality.Q3;

		//  CAP protocol parameters
		Param.selectedProtocol = Protocol.getHighestIdProtocol();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i=0; i<panel.protocolComboBox.getItemCount(); i++) {
					if (((String)panel.protocolComboBox.getItemAt(i)).equals(Param.selectedProtocol.getName())) {
						panel.protocolComboBox.setSelectedIndex(i);
					}
				}
			}
		});

		//  Wireless model parameters
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.wirelessModelComboBox.setSelectedIndex(0);
			}
		});
		Param.selectedWirelessModel = WirelessModel.NONE;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.fixedRangeTextField.setText("" + Param.fixedRange);
				panel.fixedRangeTextField.setEnabled(false);
			}
		});

		//  Wind parameters
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.windDirTextField.setText("" + Param.DEFAULT_WIND_DIRECTION);
				panel.windSpeedTextField.setText("" + Param.DEFAULT_WIND_SPEED);
				panel.lblDegrees.setText(Text.DEGREE_SYMBOL);
				panel.dontUseWindButton.setSelected(true);
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
			GUIHelper.exit(Text.LOADING_UAV_IMAGE_ERROR);
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

}
