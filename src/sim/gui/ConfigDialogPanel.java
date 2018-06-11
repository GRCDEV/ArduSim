package sim.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import main.Param;
import main.Param.WirelessModel;
import main.Text;
import sim.board.BoardParam;
import sim.logic.SimParam;
import sim.logic.SimParam.RenderQuality;
import sim.logic.SimTools;
import uavController.UAVParam;

/** This class generates the panel with the general configuration parameters shown in the configuration dialog. */

public class ConfigDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JTextField screenDelayTextField;
	public JTextField minScreenMovementTextField;
	public JCheckBox loggingEnabledCheckBox;
	public JComboBox<String> renderQualityComboBox;
	public JTextField arducopterPathTextField;
	public JTextField speedsTextField;
	public JTextField fixedRangeTextField;
	public JComboBox<String> UAVsComboBox;
	public JCheckBox batteryCheckBox;
	public JTextField batteryTextField;
	public JCheckBox cpuCheckBox;
	public JComboBox<String> protocolComboBox;
	public JComboBox<String> wirelessModelComboBox;
	public JCheckBox windCheckBox;
	public JTextField windSpeedTextField;
	public JTextField windDirTextField;
	public JLabel lblDegrees;
	public JTextField receivingBufferSizeTextField;
	public JCheckBox collisionDetectionCheckBox;
	public JTextField collisionCheckPeriodTextField;
	public JTextField collisionDistanceTextField;
	public JTextField collisionAltitudeTextField;
	public JCheckBox carrierSensingCheckBox;
	public JCheckBox pCollisionDetectionCheckBox;

	public ConfigDialogPanel() {
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 5 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		setLayout(gridBagLayout);

		JLabel lblSimulationParameters = new JLabel(Text.SIMULATION_PARAMETERS);
		GridBagConstraints gbc_lblSimulationParameters = new GridBagConstraints();
		gbc_lblSimulationParameters.anchor = GridBagConstraints.WEST;
		gbc_lblSimulationParameters.gridwidth = 4;
		gbc_lblSimulationParameters.insets = new Insets(0, 0, 5, 5);
		gbc_lblSimulationParameters.gridx = 0;
		gbc_lblSimulationParameters.gridy = 0;
		add(lblSimulationParameters, gbc_lblSimulationParameters);

		JLabel lblArducopterPath = new JLabel(Text.ARDUCOPTER_PATH);
		lblArducopterPath.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblVmsBasePath = new GridBagConstraints();
		gbc_lblVmsBasePath.gridwidth = 3;
		gbc_lblVmsBasePath.anchor = GridBagConstraints.EAST;
		gbc_lblVmsBasePath.insets = new Insets(0, 0, 5, 5);
		gbc_lblVmsBasePath.gridx = 1;
		gbc_lblVmsBasePath.gridy = 1;
		add(lblArducopterPath, gbc_lblVmsBasePath);

		arducopterPathTextField = new JTextField();
		arducopterPathTextField.setEditable(false);
		GridBagConstraints gbc_pathTextField = new GridBagConstraints();
		gbc_pathTextField.gridwidth = 3;
		gbc_pathTextField.insets = new Insets(0, 0, 5, 5);
		gbc_pathTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_pathTextField.gridx = 4;
		gbc_pathTextField.gridy = 1;
		add(arducopterPathTextField, gbc_pathTextField);
		arducopterPathTextField.setColumns(10);
		if (SimParam.sitlPath != null) {
			arducopterPathTextField.setText(SimParam.sitlPath);
		}

		JButton basePathButton = new JButton(Text.BUTTON_SELECT);
		basePathButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Choose the arducopter executable file
				JFileChooser chooser;
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(Tools.getCurrentFolder());
				chooser.setDialogTitle(Text.BASE_PATH_DIALOG_TITLE);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);
				chooser.setAcceptAllFileFilterUsed(false);
				if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
					chooser.setFileFilter(new FileNameExtensionFilter(Text.BASE_PATH_DIALOG_SELECTION, Text.BASE_PATH_DIALOG_EXTENSION));
				}
				if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
					return;
				}
				
				final File sitlPath = chooser.getSelectedFile();
				// Only accept executable file
				if (!sitlPath.canExecute()) {
					GUI.log(Text.SITL_ERROR_1);
					GUI.warn(Text.SITL_SELECTION_ERROR, Text.SITL_ERROR_1);
					SimParam.sitlPath = null;
					SimParam.paramPath = null;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							arducopterPathTextField.setText("");
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}
				// Copter param file must be in the same folder
				File paramPath = new File(sitlPath.getParent() + File.separator + SimParam.PARAM_FILE_NAME);
				if (!paramPath.exists()) {
					GUI.log(Text.SITL_ERROR_2 + "\n" + SimParam.PARAM_FILE_NAME);
					GUI.warn(Text.SITL_SELECTION_ERROR, Text.SITL_ERROR_2 + "\n" + SimParam.PARAM_FILE_NAME);
					SimParam.sitlPath = null;
					SimParam.paramPath = null;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							arducopterPathTextField.setText("");
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}

				SimParam.sitlPath = sitlPath.getAbsolutePath();
				SimParam.paramPath = paramPath.getAbsolutePath();
				
				int n = -1;
				if (UAVParam.initialSpeeds != null && UAVParam.initialSpeeds.length > 0) {
					n = Math.min(UAVParam.mavPort.length, UAVParam.initialSpeeds.length);
					
					
				}
				final int numUAVs = n;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						arducopterPathTextField.setText(sitlPath.getAbsolutePath());
						// Update UAVs combobox if speeds are already loaded
						if (numUAVs != -1) {
							UAVsComboBox.removeAllItems();
							for (int i = 0; i < numUAVs; i++) {
								UAVsComboBox.addItem("" + (i + 1));
							}
							UAVsComboBox.setSelectedIndex(UAVsComboBox.getItemCount() - 1);
						}
					}
				});
			}
		});
		GridBagConstraints gbc_pathButton = new GridBagConstraints();
		gbc_pathButton.insets = new Insets(0, 0, 5, 0);
		gbc_pathButton.gridx = 7;
		gbc_pathButton.gridy = 1;
		add(basePathButton, gbc_pathButton);

		JLabel lblSpeedsFile = new JLabel(Text.SPEEDS_FILE);
		lblSpeedsFile.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblSpeedsFile = new GridBagConstraints();
		gbc_lblSpeedsFile.anchor = GridBagConstraints.EAST;
		gbc_lblSpeedsFile.gridwidth = 3;
		gbc_lblSpeedsFile.insets = new Insets(0, 0, 5, 5);
		gbc_lblSpeedsFile.gridx = 1;
		gbc_lblSpeedsFile.gridy = 2;
		add(lblSpeedsFile, gbc_lblSpeedsFile);

		speedsTextField = new JTextField();
		speedsTextField.setEditable(false);
		GridBagConstraints gbc_speedsTextField = new GridBagConstraints();
		gbc_speedsTextField.gridwidth = 3;
		gbc_speedsTextField.insets = new Insets(0, 0, 5, 5);
		gbc_speedsTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_speedsTextField.gridx = 4;
		gbc_speedsTextField.gridy = 2;
		add(speedsTextField, gbc_speedsTextField);
		speedsTextField.setColumns(10);

		JButton speedsButton = new JButton(Text.BUTTON_SELECT);
		speedsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Select speeds file
				final File selection;
				JFileChooser chooser;
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(Tools.getCurrentFolder());
				chooser.setDialogTitle(Text.SPEEDS_DIALOG_TITLE);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				FileNameExtensionFilter filter = new FileNameExtensionFilter(Text.SPEEDS_DIALOG_SELECTION, Text.FILE_EXTENSION_CSV);
				chooser.addChoosableFileFilter(filter);
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.setMultiSelectionEnabled(false);
				if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
					return;
				}
				
				selection = chooser.getSelectedFile();
				UAVParam.initialSpeeds = SimTools.loadSpeedsFile(selection.getAbsolutePath());
				if (UAVParam.initialSpeeds == null) {
					GUI.warn(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_1);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}
				int n = -1;
				if (SimParam.sitlPath != null) {
					n = Math.min(UAVParam.initialSpeeds.length, UAVParam.mavPort.length);
				}
				final int numUAVs = n;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						speedsTextField.setText(selection.getName());
						// Update UAVs combobox if arducopter is already located
						if (numUAVs != -1) {
							UAVsComboBox.removeAllItems();
							for (int i = 0; i < numUAVs; i++) {
								UAVsComboBox.addItem("" + (i + 1));
							}
						}
					}
				});
			}
		});
		GridBagConstraints gbc_speedsButton = new GridBagConstraints();
		gbc_speedsButton.insets = new Insets(0, 0, 5, 0);
		gbc_speedsButton.gridx = 7;
		gbc_speedsButton.gridy = 2;
		add(speedsButton, gbc_speedsButton);

		JLabel lblNumberOfUAVs = new JLabel(Text.UAV_NUMBER);
		lblNumberOfUAVs.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblNumberOfUAVs = new GridBagConstraints();
		gbc_lblNumberOfUAVs.gridwidth = 3;
		gbc_lblNumberOfUAVs.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfUAVs.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfUAVs.gridx = 1;
		gbc_lblNumberOfUAVs.gridy = 3;
		add(lblNumberOfUAVs, gbc_lblNumberOfUAVs);

		UAVsComboBox = new JComboBox<String>();
		GridBagConstraints gbc_UAVsComboBox = new GridBagConstraints();
		gbc_UAVsComboBox.gridwidth = 3;
		gbc_UAVsComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_UAVsComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_UAVsComboBox.gridx = 4;
		gbc_UAVsComboBox.gridy = 3;
		add(UAVsComboBox, gbc_UAVsComboBox);

		JLabel lblVisualizationParameters = new JLabel(Text.PERFORMANCE_PARAMETERS);
		GridBagConstraints gbc_lblVisualizationParameters = new GridBagConstraints();
		gbc_lblVisualizationParameters.anchor = GridBagConstraints.WEST;
		gbc_lblVisualizationParameters.gridwidth = 4;
		gbc_lblVisualizationParameters.insets = new Insets(0, 0, 5, 5);
		gbc_lblVisualizationParameters.gridx = 0;
		gbc_lblVisualizationParameters.gridy = 4;
		add(lblVisualizationParameters, gbc_lblVisualizationParameters);

		JLabel lblTimeBetweenScreen = new JLabel(Text.SCREEN_REFRESH_RATE);
		lblTimeBetweenScreen.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblTimeBetweenScreen = new GridBagConstraints();
		gbc_lblTimeBetweenScreen.anchor = GridBagConstraints.EAST;
		gbc_lblTimeBetweenScreen.gridwidth = 4;
		gbc_lblTimeBetweenScreen.insets = new Insets(0, 0, 5, 5);
		gbc_lblTimeBetweenScreen.gridx = 1;
		gbc_lblTimeBetweenScreen.gridy = 5;
		add(lblTimeBetweenScreen, gbc_lblTimeBetweenScreen);

		screenDelayTextField = new JTextField();
		screenDelayTextField.setText("" + BoardParam.screenDelay);
		screenDelayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_screenDelayTextField = new GridBagConstraints();
		gbc_screenDelayTextField.insets = new Insets(0, 0, 5, 5);
		gbc_screenDelayTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_screenDelayTextField.gridx = 5;
		gbc_screenDelayTextField.gridy = 5;
		add(screenDelayTextField, gbc_screenDelayTextField);
		screenDelayTextField.setColumns(10);

		JLabel lblMs = new JLabel(Text.MILLISECONDS);
		lblMs.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblMs = new GridBagConstraints();
		gbc_lblMs.anchor = GridBagConstraints.WEST;
		gbc_lblMs.insets = new Insets(0, 0, 5, 5);
		gbc_lblMs.gridx = 6;
		gbc_lblMs.gridy = 5;
		add(lblMs, gbc_lblMs);

		JLabel lblMinimumMovementOf = new JLabel(Text.REDRAW_DISTANCE);
		lblMinimumMovementOf.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblMinimumMovementOf = new GridBagConstraints();
		gbc_lblMinimumMovementOf.anchor = GridBagConstraints.EAST;
		gbc_lblMinimumMovementOf.gridwidth = 4;
		gbc_lblMinimumMovementOf.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinimumMovementOf.gridx = 1;
		gbc_lblMinimumMovementOf.gridy = 6;
		add(lblMinimumMovementOf, gbc_lblMinimumMovementOf);

		minScreenMovementTextField = new JTextField();
		minScreenMovementTextField.setText("" + BoardParam.minScreenMovement);
		minScreenMovementTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_minScreenMovementTextField = new GridBagConstraints();
		gbc_minScreenMovementTextField.insets = new Insets(0, 0, 5, 5);
		gbc_minScreenMovementTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_minScreenMovementTextField.gridx = 5;
		gbc_minScreenMovementTextField.gridy = 6;
		add(minScreenMovementTextField, gbc_minScreenMovementTextField);
		minScreenMovementTextField.setColumns(10);

		JLabel lblPixels = new JLabel(Text.PIXELS);
		lblPixels.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblPixels = new GridBagConstraints();
		gbc_lblPixels.anchor = GridBagConstraints.WEST;
		gbc_lblPixels.insets = new Insets(0, 0, 5, 5);
		gbc_lblPixels.gridx = 6;
		gbc_lblPixels.gridy = 6;
		add(lblPixels, gbc_lblPixels);
		
		JLabel lblLoggingEnabled = new JLabel(Text.LOGGING);
		lblLoggingEnabled.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblLoggingEnabled = new GridBagConstraints();
		gbc_lblLoggingEnabled.gridwidth = 4;
		gbc_lblLoggingEnabled.anchor = GridBagConstraints.EAST;
		gbc_lblLoggingEnabled.insets = new Insets(0, 0, 5, 5);
		gbc_lblLoggingEnabled.gridx = 0;
		gbc_lblLoggingEnabled.gridy = 7;
		add(lblLoggingEnabled, gbc_lblLoggingEnabled);
		
		loggingEnabledCheckBox = new JCheckBox();
		GridBagConstraints gbc_loggingEnabledCheckBox = new GridBagConstraints();
		gbc_loggingEnabledCheckBox.anchor = GridBagConstraints.WEST;
		gbc_loggingEnabledCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_loggingEnabledCheckBox.gridx = 4;
		gbc_loggingEnabledCheckBox.gridy = 7;
		add(loggingEnabledCheckBox, gbc_loggingEnabledCheckBox);
		
		JLabel lblBattery = new JLabel(Text.BATTERY);
		lblBattery.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblBattery = new GridBagConstraints();
		gbc_lblBattery.gridwidth = 4;
		gbc_lblBattery.anchor = GridBagConstraints.EAST;
		gbc_lblBattery.insets = new Insets(0, 0, 5, 5);
		gbc_lblBattery.gridx = 0;
		gbc_lblBattery.gridy = 8;
		add(lblBattery, gbc_lblBattery);
		
		batteryCheckBox = new JCheckBox();
		batteryCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean isSelected = batteryCheckBox.isSelected();
				if (isSelected) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							batteryTextField.setEnabled(true);
						}
					});
				} else {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							batteryTextField.setEnabled(false);
						}
					});
				}
				
				
			}
		});
		GridBagConstraints gbc_batteryCheckBox = new GridBagConstraints();
		gbc_batteryCheckBox.anchor = GridBagConstraints.WEST;
		gbc_batteryCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_batteryCheckBox.gridx = 4;
		gbc_batteryCheckBox.gridy = 8;
		add(batteryCheckBox, gbc_batteryCheckBox);
		
		batteryTextField = new JTextField();
		batteryTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_batteryTextField = new GridBagConstraints();
		gbc_batteryTextField.insets = new Insets(0, 0, 5, 5);
		gbc_batteryTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_batteryTextField.gridx = 5;
		gbc_batteryTextField.gridy = 8;
		add(batteryTextField, gbc_batteryTextField);
		batteryTextField.setColumns(10);
		
		JLabel lblmah = new JLabel(Text.BATTERY_CAPACITY);
		lblmah.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblmah = new GridBagConstraints();
		gbc_lblmah.anchor = GridBagConstraints.WEST;
		gbc_lblmah.insets = new Insets(0, 0, 5, 5);
		gbc_lblmah.gridx = 6;
		gbc_lblmah.gridy = 8;
		add(lblmah, gbc_lblmah);
		
		JLabel lblGf = new JLabel(Text.CPU_MEASUREMENT_ENABLED);
		lblGf.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblGf = new GridBagConstraints();
		gbc_lblGf.anchor = GridBagConstraints.EAST;
		gbc_lblGf.gridwidth = 4;
		gbc_lblGf.insets = new Insets(0, 0, 5, 5);
		gbc_lblGf.gridx = 0;
		gbc_lblGf.gridy = 9;
		add(lblGf, gbc_lblGf);
		
		cpuCheckBox = new JCheckBox();
		GridBagConstraints gbc_cpuCheckBox = new GridBagConstraints();
		gbc_cpuCheckBox.anchor = GridBagConstraints.WEST;
		gbc_cpuCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_cpuCheckBox.gridx = 4;
		gbc_cpuCheckBox.gridy = 9;
		add(cpuCheckBox, gbc_cpuCheckBox);
		
		JLabel renderQualityLabel = new JLabel(Text.RENDER);
		renderQualityLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_renderQualityLabel = new GridBagConstraints();
		gbc_renderQualityLabel.anchor = GridBagConstraints.EAST;
		gbc_renderQualityLabel.gridwidth = 3;
		gbc_renderQualityLabel.insets = new Insets(0, 0, 5, 5);
		gbc_renderQualityLabel.gridx = 1;
		gbc_renderQualityLabel.gridy = 10;
		add(renderQualityLabel, gbc_renderQualityLabel);
		
		renderQualityComboBox = new JComboBox<String>();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i=RenderQuality.Q1.getId(); i <= RenderQuality.getHighestIdRenderQuality().getId(); i++) {
					renderQualityComboBox.addItem(RenderQuality.getRenderQualityNameById(i));
				}
				renderQualityComboBox.setSelectedIndex(SimParam.renderQuality.getId());
				
				renderQualityComboBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							String item = (String)renderQualityComboBox.getSelectedItem();
							SimParam.renderQuality = RenderQuality.getRenderQualityByName(item);
						}
					}
				});
			}
		});
		GridBagConstraints gbc_renderQualityComboBox = new GridBagConstraints();
		gbc_renderQualityComboBox.gridwidth = 3;
		gbc_renderQualityComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_renderQualityComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_renderQualityComboBox.gridx = 4;
		gbc_renderQualityComboBox.gridy = 10;
		add(renderQualityComboBox, gbc_renderQualityComboBox);

		JLabel lblProtocolParameters = new JLabel(Text.UAV_PROTOCOL_USED);
		GridBagConstraints gbc_lblProtocolParameters = new GridBagConstraints();
		gbc_lblProtocolParameters.anchor = GridBagConstraints.WEST;
		gbc_lblProtocolParameters.gridwidth = 4;
		gbc_lblProtocolParameters.insets = new Insets(0, 0, 5, 5);
		gbc_lblProtocolParameters.gridx = 0;
		gbc_lblProtocolParameters.gridy = 11;
		add(lblProtocolParameters, gbc_lblProtocolParameters);

		protocolComboBox = new JComboBox<String>();
		for (int i = 0; i < ProtocolHelper.ProtocolNames.length; i++) {
			protocolComboBox.addItem(ProtocolHelper.ProtocolNames[i]);
		}
		
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				protocolComboBox.setSelectedIndex(protocolComboBox.getItemCount() - 1);
//			}
//		});

		GridBagConstraints gbc_protocolComboBox = new GridBagConstraints();
		gbc_protocolComboBox.gridwidth = 3;
		gbc_protocolComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_protocolComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_protocolComboBox.gridx = 4;
		gbc_protocolComboBox.gridy = 11;
		add(protocolComboBox, gbc_protocolComboBox);
		
		JLabel lblUav = new JLabel(Text.COMMUNICATIONS);
		GridBagConstraints gbc_lblUav = new GridBagConstraints();
		gbc_lblUav.anchor = GridBagConstraints.WEST;
		gbc_lblUav.gridwidth = 6;
		gbc_lblUav.insets = new Insets(0, 0, 5, 5);
		gbc_lblUav.gridx = 0;
		gbc_lblUav.gridy = 12;
		add(lblUav, gbc_lblUav);
		
		JLabel lblP = new JLabel(Text.CARRIER_SENSING);
		lblP.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblP = new GridBagConstraints();
		gbc_lblP.anchor = GridBagConstraints.EAST;
		gbc_lblP.gridwidth = 4;
		gbc_lblP.insets = new Insets(0, 0, 5, 5);
		gbc_lblP.gridx = 0;
		gbc_lblP.gridy = 13;
		add(lblP, gbc_lblP);
		
		carrierSensingCheckBox = new JCheckBox();
		GridBagConstraints gbc_carrierSensingCheckBox = new GridBagConstraints();
		gbc_carrierSensingCheckBox.anchor = GridBagConstraints.WEST;
		gbc_carrierSensingCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_carrierSensingCheckBox.gridx = 4;
		gbc_carrierSensingCheckBox.gridy = 13;
		add(carrierSensingCheckBox, gbc_carrierSensingCheckBox);
		
		JLabel lblDgh = new JLabel(Text.PACKET_COLLISION_DETECTION);
		lblDgh.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblDgh = new GridBagConstraints();
		gbc_lblDgh.anchor = GridBagConstraints.EAST;
		gbc_lblDgh.gridwidth = 4;
		gbc_lblDgh.insets = new Insets(0, 0, 5, 5);
		gbc_lblDgh.gridx = 0;
		gbc_lblDgh.gridy = 14;
		add(lblDgh, gbc_lblDgh);
		
		pCollisionDetectionCheckBox = new JCheckBox();
		GridBagConstraints gbc_pCollisionDetectionCheckBox = new GridBagConstraints();
		gbc_pCollisionDetectionCheckBox.anchor = GridBagConstraints.WEST;
		gbc_pCollisionDetectionCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_pCollisionDetectionCheckBox.gridx = 4;
		gbc_pCollisionDetectionCheckBox.gridy = 14;
		add(pCollisionDetectionCheckBox, gbc_pCollisionDetectionCheckBox);
		
		JLabel lblJ = new JLabel(Text.BUFFER_SIZE);
		lblJ.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblJ = new GridBagConstraints();
		gbc_lblJ.anchor = GridBagConstraints.EAST;
		gbc_lblJ.gridwidth = 5;
		gbc_lblJ.insets = new Insets(0, 0, 5, 5);
		gbc_lblJ.gridx = 0;
		gbc_lblJ.gridy = 15;
		add(lblJ, gbc_lblJ);
		
		receivingBufferSizeTextField = new JTextField();
		receivingBufferSizeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_receivingBufferSizeTextField = new GridBagConstraints();
		gbc_receivingBufferSizeTextField.insets = new Insets(0, 0, 5, 5);
		gbc_receivingBufferSizeTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_receivingBufferSizeTextField.gridx = 5;
		gbc_receivingBufferSizeTextField.gridy = 15;
		add(receivingBufferSizeTextField, gbc_receivingBufferSizeTextField);
		receivingBufferSizeTextField.setColumns(10);
		
		JLabel lblBytes = new JLabel(Text.BYTES);
		lblBytes.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblBytes = new GridBagConstraints();
		gbc_lblBytes.anchor = GridBagConstraints.WEST;
		gbc_lblBytes.insets = new Insets(0, 0, 5, 5);
		gbc_lblBytes.gridx = 6;
		gbc_lblBytes.gridy = 15;
		add(lblBytes, gbc_lblBytes);

		JLabel lblWirelessModelParameters = new JLabel(Text.WIFI_MODEL);
		lblWirelessModelParameters.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblWirelessRangeParameters = new GridBagConstraints();
		gbc_lblWirelessRangeParameters.anchor = GridBagConstraints.EAST;
		gbc_lblWirelessRangeParameters.gridwidth = 4;
		gbc_lblWirelessRangeParameters.insets = new Insets(0, 0, 5, 5);
		gbc_lblWirelessRangeParameters.gridx = 0;
		gbc_lblWirelessRangeParameters.gridy = 16;
		add(lblWirelessModelParameters, gbc_lblWirelessRangeParameters);

		wirelessModelComboBox = new JComboBox<String>();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i=WirelessModel.NONE.getId(); i <= WirelessModel.getHighestIdModel().getId(); i++) {
					wirelessModelComboBox.addItem(WirelessModel.getModelNameById(i));
				}
				wirelessModelComboBox.setSelectedIndex(WirelessModel.NONE.getId());
				
				wirelessModelComboBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							String item = (String)wirelessModelComboBox.getSelectedItem();
							Param.selectedWirelessModel = WirelessModel.getModelByName(item);
							if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
								fixedRangeTextField.setEnabled(true);
							} else {
								fixedRangeTextField.setEnabled(false);
							}
						}
					}
				});
			}
		});

		GridBagConstraints gbc_wirelessModelComboBox = new GridBagConstraints();
		gbc_wirelessModelComboBox.gridwidth = 4;
		gbc_wirelessModelComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_wirelessModelComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_wirelessModelComboBox.gridx = 4;
		gbc_wirelessModelComboBox.gridy = 16;
		add(wirelessModelComboBox, gbc_wirelessModelComboBox);

		JLabel lblFixedRangeDistance = new JLabel(Text.FIXED_RANGE_DISTANCE);
		lblFixedRangeDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblFixedRangeDistance = new GridBagConstraints();
		gbc_lblFixedRangeDistance.anchor = GridBagConstraints.EAST;
		gbc_lblFixedRangeDistance.gridwidth = 4;
		gbc_lblFixedRangeDistance.insets = new Insets(0, 0, 5, 5);
		gbc_lblFixedRangeDistance.gridx = 1;
		gbc_lblFixedRangeDistance.gridy = 17;
		add(lblFixedRangeDistance, gbc_lblFixedRangeDistance);

		fixedRangeTextField = new JTextField();
		fixedRangeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		fixedRangeTextField.setText("" + Param.fixedRange);
		GridBagConstraints gbc_rangeTextField = new GridBagConstraints();
		gbc_rangeTextField.insets = new Insets(0, 0, 5, 5);
		gbc_rangeTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_rangeTextField.gridx = 5;
		gbc_rangeTextField.gridy = 17;
		add(fixedRangeTextField, gbc_rangeTextField);
		fixedRangeTextField.setColumns(10);

		JLabel lblM_5 = new JLabel(Text.METERS);
		lblM_5.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblM_5 = new GridBagConstraints();
		gbc_lblM_5.anchor = GridBagConstraints.WEST;
		gbc_lblM_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblM_5.gridx = 6;
		gbc_lblM_5.gridy = 17;
		add(lblM_5, gbc_lblM_5);
		
		JLabel lblJ_1 = new JLabel(Text.COLLISION_PARAMETERS);
		GridBagConstraints gbc_lblJ_1 = new GridBagConstraints();
		gbc_lblJ_1.anchor = GridBagConstraints.WEST;
		gbc_lblJ_1.gridwidth = 5;
		gbc_lblJ_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblJ_1.gridx = 0;
		gbc_lblJ_1.gridy = 18;
		add(lblJ_1, gbc_lblJ_1);
		
		JLabel lblJ_2 = new JLabel(Text.COLLISION_ENABLE);
		lblJ_2.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblJ_2 = new GridBagConstraints();
		gbc_lblJ_2.anchor = GridBagConstraints.EAST;
		gbc_lblJ_2.gridwidth = 4;
		gbc_lblJ_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblJ_2.gridx = 0;
		gbc_lblJ_2.gridy = 19;
		add(lblJ_2, gbc_lblJ_2);
		
		collisionDetectionCheckBox = new JCheckBox();
		collisionDetectionCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						boolean isSelected = collisionDetectionCheckBox.isSelected();
						if (isSelected) {
							collisionCheckPeriodTextField.setEnabled(true);
							collisionDistanceTextField.setEnabled(true);
							collisionAltitudeTextField.setEnabled(true);
						} else {
							collisionCheckPeriodTextField.setEnabled(false);
							collisionDistanceTextField.setEnabled(false);
							collisionAltitudeTextField.setEnabled(false);
						}
					}
				});
			}
		});
		GridBagConstraints gbc_collisionDetectionCheckBox = new GridBagConstraints();
		gbc_collisionDetectionCheckBox.anchor = GridBagConstraints.WEST;
		gbc_collisionDetectionCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_collisionDetectionCheckBox.gridx = 4;
		gbc_collisionDetectionCheckBox.gridy = 19;
		add(collisionDetectionCheckBox, gbc_collisionDetectionCheckBox);
		
		JLabel lblC = new JLabel(Text.COLLISION_PERIOD);
		lblC.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblC = new GridBagConstraints();
		gbc_lblC.anchor = GridBagConstraints.EAST;
		gbc_lblC.gridwidth = 4;
		gbc_lblC.insets = new Insets(0, 0, 5, 5);
		gbc_lblC.gridx = 1;
		gbc_lblC.gridy = 20;
		add(lblC, gbc_lblC);
		
		collisionCheckPeriodTextField = new JTextField();
		collisionCheckPeriodTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_collisionCheckPeriodTextField = new GridBagConstraints();
		gbc_collisionCheckPeriodTextField.insets = new Insets(0, 0, 5, 5);
		gbc_collisionCheckPeriodTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_collisionCheckPeriodTextField.gridx = 5;
		gbc_collisionCheckPeriodTextField.gridy = 20;
		add(collisionCheckPeriodTextField, gbc_collisionCheckPeriodTextField);
		collisionCheckPeriodTextField.setColumns(10);
		
		JLabel lblS = new JLabel(Text.SECONDS);
		lblS.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblS = new GridBagConstraints();
		gbc_lblS.anchor = GridBagConstraints.WEST;
		gbc_lblS.insets = new Insets(0, 0, 5, 5);
		gbc_lblS.gridx = 6;
		gbc_lblS.gridy = 20;
		add(lblS, gbc_lblS);
		
		JLabel lblC_1 = new JLabel(Text.COLLISION_DISTANCE);
		lblC_1.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblC_1 = new GridBagConstraints();
		gbc_lblC_1.anchor = GridBagConstraints.EAST;
		gbc_lblC_1.gridwidth = 5;
		gbc_lblC_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblC_1.gridx = 0;
		gbc_lblC_1.gridy = 21;
		add(lblC_1, gbc_lblC_1);
		
		collisionDistanceTextField = new JTextField();
		collisionDistanceTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_collisionDistanceTextField = new GridBagConstraints();
		gbc_collisionDistanceTextField.insets = new Insets(0, 0, 5, 5);
		gbc_collisionDistanceTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_collisionDistanceTextField.gridx = 5;
		gbc_collisionDistanceTextField.gridy = 21;
		add(collisionDistanceTextField, gbc_collisionDistanceTextField);
		collisionDistanceTextField.setColumns(10);
		
		JLabel lblM = new JLabel(Text.METERS);
		lblM.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblM = new GridBagConstraints();
		gbc_lblM.anchor = GridBagConstraints.WEST;
		gbc_lblM.insets = new Insets(0, 0, 5, 5);
		gbc_lblM.gridx = 6;
		gbc_lblM.gridy = 21;
		add(lblM, gbc_lblM);
		
		JLabel lblH = new JLabel(Text.COLLISION_ALTITUDE);
		lblH.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblH = new GridBagConstraints();
		gbc_lblH.anchor = GridBagConstraints.EAST;
		gbc_lblH.gridwidth = 5;
		gbc_lblH.insets = new Insets(0, 0, 5, 5);
		gbc_lblH.gridx = 0;
		gbc_lblH.gridy = 22;
		add(lblH, gbc_lblH);
		
		collisionAltitudeTextField = new JTextField();
		collisionAltitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_collisionAltitudeTextField = new GridBagConstraints();
		gbc_collisionAltitudeTextField.insets = new Insets(0, 0, 5, 5);
		gbc_collisionAltitudeTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_collisionAltitudeTextField.gridx = 5;
		gbc_collisionAltitudeTextField.gridy = 22;
		add(collisionAltitudeTextField, gbc_collisionAltitudeTextField);
		collisionAltitudeTextField.setColumns(10);
		
		JLabel lblM_1 = new JLabel(Text.METERS);
		lblM_1.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblM_1 = new GridBagConstraints();
		gbc_lblM_1.anchor = GridBagConstraints.WEST;
		gbc_lblM_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblM_1.gridx = 6;
		gbc_lblM_1.gridy = 22;
		add(lblM_1, gbc_lblM_1);

		JLabel lblNewLabel = new JLabel(Text.WIND_PARAMETERS);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 2;
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 23;
		add(lblNewLabel, gbc_lblNewLabel);
		
		windCheckBox = new JCheckBox("");
		windCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean isSelected = windCheckBox.isSelected();
				if (isSelected) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							windDirTextField.setEnabled(true);
							windSpeedTextField.setEnabled(true);
						}
					});
				} else {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							windDirTextField.setEnabled(false);
							windSpeedTextField.setEnabled(false);
						}
					});
				}
			}
		});
		
		JLabel lblF = new JLabel(Text.WIND_ENABLE);
		lblF.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblF = new GridBagConstraints();
		gbc_lblF.anchor = GridBagConstraints.EAST;
		gbc_lblF.gridwidth = 4;
		gbc_lblF.insets = new Insets(0, 0, 5, 5);
		gbc_lblF.gridx = 0;
		gbc_lblF.gridy = 24;
		add(lblF, gbc_lblF);
		GridBagConstraints gbc_windCheckBox = new GridBagConstraints();
		gbc_windCheckBox.anchor = GridBagConstraints.WEST;
		gbc_windCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_windCheckBox.gridx = 4;
		gbc_windCheckBox.gridy = 24;
		add(windCheckBox, gbc_windCheckBox);

		JLabel lblDirection = new JLabel(Text.WIND_DIRECTION);
		lblDirection.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblDirection = new GridBagConstraints();
		gbc_lblDirection.anchor = GridBagConstraints.EAST;
		gbc_lblDirection.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirection.gridx = 3;
		gbc_lblDirection.gridy = 25;
		add(lblDirection, gbc_lblDirection);

		final ConfigDialogWindPanel windDirPanel = new ConfigDialogWindPanel();
		windDirPanel.setSize(new Dimension(35, 35));
		GridBagConstraints gbc_windDirPanel = new GridBagConstraints();
		gbc_windDirPanel.insets = new Insets(0, 0, 5, 5);
		gbc_windDirPanel.gridx = 4;
		gbc_windDirPanel.gridy = 25;
		add(windDirPanel, gbc_windDirPanel);

		windDirTextField = new JTextField();
		windDirTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		windDirTextField.setEnabled(false);
		windDirTextField.setText("" + Param.windDirection);
		windDirTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				checkValue();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				checkValue();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				checkValue();
			}

			private void checkValue() {
				// Only update drawing if the value is correct
				//   ...adapting to [0,360.0[ interval
				String valueString = windDirTextField.getText();
				int value;
				if (valueString!=null && valueString.length()>0) {
					try {
						value = Integer.parseInt(valueString);
						if (value >= 360) {
							int p = value / 360;
							value = value - p*360;
						}

						if (value < 0) {
							int p = (-value) / 360 + 1;
							value = value + p*360;
						}

						Param.windDirection = value;
						windDirPanel.repaint();
					} catch (NumberFormatException e) {
					}
				}
			}
		});
		GridBagConstraints gbc_windDirTextField = new GridBagConstraints();
		gbc_windDirTextField.insets = new Insets(0, 0, 5, 5);
		gbc_windDirTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_windDirTextField.gridx = 5;
		gbc_windDirTextField.gridy = 25;
		add(windDirTextField, gbc_windDirTextField);
		windDirTextField.setColumns(10);

		lblDegrees = new JLabel();
		lblDegrees.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblDegrees = new GridBagConstraints();
		gbc_lblDegrees.anchor = GridBagConstraints.WEST;
		gbc_lblDegrees.insets = new Insets(0, 0, 5, 5);
		gbc_lblDegrees.gridx = 6;
		gbc_lblDegrees.gridy = 25;
		add(lblDegrees, gbc_lblDegrees);

		JLabel lblSpeedms = new JLabel(Text.WIND_SPEED);
		lblSpeedms.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblSpeedms = new GridBagConstraints();
		gbc_lblSpeedms.anchor = GridBagConstraints.EAST;
		gbc_lblSpeedms.insets = new Insets(0, 0, 0, 5);
		gbc_lblSpeedms.gridx = 3;
		gbc_lblSpeedms.gridy = 26;
		add(lblSpeedms, gbc_lblSpeedms);

		windSpeedTextField = new JTextField();
		windSpeedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		windSpeedTextField.setEnabled(false);
		windSpeedTextField.setText("" + Param.windSpeed);
		GridBagConstraints gbc_windSpeedTextField = new GridBagConstraints();
		gbc_windSpeedTextField.insets = new Insets(0, 0, 0, 5);
		gbc_windSpeedTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_windSpeedTextField.gridx = 5;
		gbc_windSpeedTextField.gridy = 26;
		add(windSpeedTextField, gbc_windSpeedTextField);
		windSpeedTextField.setColumns(10);

		JLabel lblMs_1 = new JLabel(Text.METERS_PER_SECOND);
		lblMs_1.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblMs_1 = new GridBagConstraints();
		gbc_lblMs_1.anchor = GridBagConstraints.WEST;
		gbc_lblMs_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblMs_1.gridx = 6;
		gbc_lblMs_1.gridy = 26;
		add(lblMs_1, gbc_lblMs_1);
		
		UAVsComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					int numUAVs = Integer.parseInt((String)e.getItem());
					if (numUAVs == 1) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								carrierSensingCheckBox.setEnabled(false);
								pCollisionDetectionCheckBox.setEnabled(false);
								receivingBufferSizeTextField.setEnabled(false);
								wirelessModelComboBox.setEnabled(false);
								fixedRangeTextField.setEnabled(false);
								collisionDetectionCheckBox.setEnabled(false);
								collisionCheckPeriodTextField.setEnabled(false);
								collisionDistanceTextField.setEnabled(false);
								collisionAltitudeTextField.setEnabled(false);
							}
						});
					} else {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								carrierSensingCheckBox.setEnabled(true);
								pCollisionDetectionCheckBox.setEnabled(true);
								receivingBufferSizeTextField.setEnabled(true);
								wirelessModelComboBox.setEnabled(true);
								fixedRangeTextField.setEnabled(true);
								collisionDetectionCheckBox.setEnabled(true);
								collisionCheckPeriodTextField.setEnabled(true);
								collisionDistanceTextField.setEnabled(true);
								collisionAltitudeTextField.setEnabled(true);
							}
						});
					}
				}
			}
		});
	}

}
