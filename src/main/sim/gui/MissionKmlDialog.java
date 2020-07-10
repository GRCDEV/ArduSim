package main.sim.gui;

import api.API;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.api.ArduSim;
import main.api.ValidationTools;
import main.sim.logic.SimTools;
import main.uavController.UAVParam;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** 
 * Dialog used to input options while loading missions.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionKmlDialog extends JDialog {
	
	/** Used to leave the mission as it is loaded from a Google Earth .kml file. */
	public static final String MISSION_END_UNMODIFIED = "unmodified";// If you change this text, also do it in ardusim.ini
	/** Used to end the mission with a LAND command when it is loaded from a Google Earth .kml file. */
	public static final String MISSION_END_LAND = "land";
	/** Used to end the mission with a RTL command when it is loaded from a Google Earth .kml file. */
	public static final String MISSION_END_RTL = "RTL";
	/** Last waypoint behavior for a mission loaded from a Google Earth .kml file. Please, set the same default value in <i>ardusim.ini</i> file. */
	public static volatile String missionEnd = MISSION_END_UNMODIFIED;
	/** (m) Final altitude when performing RTL. */
	public static volatile double rtlFinalAltitude;
	/**
	 * (s) Hovering time over each waypoint before going on to the next waypoint.
	 * <p>Please, modify WPNAV_RADIUS [10-1000 cm] parameter if needed to change where the mission waypoint is reached.</p> */
	public static volatile int waypointDelay = 0;
	/**
	 * (cm) WPNAV_RADIUS [10-1000] Distance to a waypoint to assert that it has been reached.
	 * This parameter is only used when the delay is greater than 0. */
	public static volatile int waypointDistance = 200;

	private static final long serialVersionUID = 1L;
	private final JDialog thisDialog;
	private final JPanel contentPanel = new JPanel();
	private JComboBox<String> missionEndComboBox;
	private final JSpinner delaySpinner;
	private JTextField distanceTextField;
	private JCheckBox altitudeCheckBox;
	private JTextField altitudeTextField;
	private JTextField minAltitudeTextField;
	private JCheckBox yawCheckBox;
	private JComboBox<String> yawComboBox;
	
	public static volatile boolean success = false;	//To check if the dialog was closed correctly
	private JTextField rtlAltitudeTextField;

	@SuppressWarnings("unused")
	private MissionKmlDialog() {
		this.thisDialog = null;
		this.delaySpinner = null;
	}

	public MissionKmlDialog(String title) {
		if(Param.role == ArduSim.SIMULATOR_CLI){
			this.thisDialog = null;
			this.delaySpinner = null;
			setDefaultParameters();
			return;
		}
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.WEST);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel = new JLabel(Text.EXTEND_MISSION);
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 2;
			gbc_lblNewLabel.gridy = 0;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			missionEndComboBox = new JComboBox<String>();
			missionEndComboBox.addItem(MissionKmlDialog.MISSION_END_UNMODIFIED);
			missionEndComboBox.addItem(MissionKmlDialog.MISSION_END_LAND);
			missionEndComboBox.addItem(MissionKmlDialog.MISSION_END_RTL);
			GridBagConstraints gbc_missionEndComboBox = new GridBagConstraints();
			gbc_missionEndComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_missionEndComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_missionEndComboBox.gridx = 3;
			gbc_missionEndComboBox.gridy = 0;
			contentPanel.add(missionEndComboBox, gbc_missionEndComboBox);
		}
		{
			JLabel lblAsg = new JLabel(Text.RTL_ALTITUDE);
			lblAsg.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblAsg = new GridBagConstraints();
			gbc_lblAsg.anchor = GridBagConstraints.EAST;
			gbc_lblAsg.gridwidth = 3;
			gbc_lblAsg.insets = new Insets(0, 0, 5, 5);
			gbc_lblAsg.gridx = 0;
			gbc_lblAsg.gridy = 1;
			contentPanel.add(lblAsg, gbc_lblAsg);
		}
		{
			rtlAltitudeTextField = new JTextField();
			rtlAltitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			rtlAltitudeTextField.setText("" + MissionKmlDialog.rtlFinalAltitude);
			GridBagConstraints gbc_rtlAltitudeTextField = new GridBagConstraints();
			gbc_rtlAltitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_rtlAltitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_rtlAltitudeTextField.gridx = 3;
			gbc_rtlAltitudeTextField.gridy = 1;
			contentPanel.add(rtlAltitudeTextField, gbc_rtlAltitudeTextField);
			rtlAltitudeTextField.setColumns(10);
			if (MissionKmlDialog.missionEnd.equals(MissionKmlDialog.MISSION_END_UNMODIFIED)) {
				missionEndComboBox.setSelectedIndex(0);
				rtlAltitudeTextField.setEnabled(false);
			}
			if (MissionKmlDialog.missionEnd.equals(MissionKmlDialog.MISSION_END_LAND)) {
				missionEndComboBox.setSelectedIndex(1);
				rtlAltitudeTextField.setEnabled(false);
			}
			if (MissionKmlDialog.missionEnd.equals(MissionKmlDialog.MISSION_END_RTL)) {
				missionEndComboBox.setSelectedIndex(2);
				rtlAltitudeTextField.setEnabled(true);
			}
			missionEndComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(((String)missionEndComboBox.getSelectedItem()).equals(MissionKmlDialog.MISSION_END_RTL)) {
						rtlAltitudeTextField.setEnabled(true);
					} else {
						rtlAltitudeTextField.setEnabled(false);
					}
				}
			});
		}
		{
			JLabel lblFdgfh = new JLabel(Text.METERS);
			lblFdgfh.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFdgfh = new GridBagConstraints();
			gbc_lblFdgfh.anchor = GridBagConstraints.WEST;
			gbc_lblFdgfh.insets = new Insets(0, 0, 5, 0);
			gbc_lblFdgfh.gridx = 4;
			gbc_lblFdgfh.gridy = 1;
			contentPanel.add(lblFdgfh, gbc_lblFdgfh);
		}
		
		{
			JLabel label = new JLabel(Text.MIN_TARGET_ALTITUDE);
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.gridwidth = 3;
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 2;
			contentPanel.add(label, gbc_label);
		}
		{
			minAltitudeTextField = new JTextField();
			minAltitudeTextField.setText("" + UAVParam.minAltitude);
			minAltitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			minAltitudeTextField.setColumns(10);
			GridBagConstraints gbc_minAltitudeTextField = new GridBagConstraints();
			gbc_minAltitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_minAltitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_minAltitudeTextField.gridx = 3;
			gbc_minAltitudeTextField.gridy = 2;
			contentPanel.add(minAltitudeTextField, gbc_minAltitudeTextField);
		}
		{
			JLabel label = new JLabel("m");
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.WEST;
			gbc_label.insets = new Insets(0, 0, 5, 0);
			gbc_label.gridx = 4;
			gbc_label.gridy = 2;
			contentPanel.add(label, gbc_label);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel(Text.XML_DELAY);
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.gridwidth = 3;
			gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 3;
			contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			SpinnerNumberModel sModel = new SpinnerNumberModel(MissionKmlDialog.waypointDelay, 0, 65535, 1);
			delaySpinner = new JSpinner(sModel);
			delaySpinner.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if ((int)delaySpinner.getValue() == 0) {
						distanceTextField.setEnabled(false);
					} else {
						distanceTextField.setEnabled(true);
					}
				}
			});
			GridBagConstraints gbc_delaySpinner = new GridBagConstraints();
			gbc_delaySpinner.fill = GridBagConstraints.HORIZONTAL;
			gbc_delaySpinner.anchor = GridBagConstraints.EAST;
			gbc_delaySpinner.insets = new Insets(0, 0, 5, 5);
			gbc_delaySpinner.gridx = 3;
			gbc_delaySpinner.gridy = 3;
			contentPanel.add(delaySpinner, gbc_delaySpinner);
		}
		{
			JLabel lblNewLabel_2 = new JLabel(Text.SECONDS);
			lblNewLabel_2.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 0);
			gbc_lblNewLabel_2.gridx = 4;
			gbc_lblNewLabel_2.gridy = 3;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		{
			JLabel lblNewLabel_3 = new JLabel(Text.TARGET_DISTANCE);
			lblNewLabel_3.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
			gbc_lblNewLabel_3.gridwidth = 3;
			gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_3.gridx = 0;
			gbc_lblNewLabel_3.gridy = 4;
			contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		}
		{
			distanceTextField = new JTextField();
			if ((int)delaySpinner.getValue() == 0) {
				distanceTextField.setEnabled(false);
			}
			distanceTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_distanceTextField = new GridBagConstraints();
			gbc_distanceTextField.insets = new Insets(0, 0, 5, 5);
			gbc_distanceTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_distanceTextField.gridx = 3;
			gbc_distanceTextField.gridy = 4;
			contentPanel.add(distanceTextField, gbc_distanceTextField);
			distanceTextField.setColumns(10);
		}
		
		distanceTextField.setText("" + MissionKmlDialog.waypointDistance);
		
		{
			JLabel lblNewLabel_4 = new JLabel(Text.CENTIMETERS);
			lblNewLabel_4.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
			gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 0);
			gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_4.gridx = 4;
			gbc_lblNewLabel_4.gridy = 4;
			contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		}
		{
			JLabel label = new JLabel(Text.ALTITUDE_OVERRIDE);
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.gridwidth = 3;
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 5;
			contentPanel.add(label, gbc_label);
		}
		{
			altitudeCheckBox = new JCheckBox();
			altitudeCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (altitudeCheckBox.isSelected()) {
						altitudeTextField.setEnabled(true);
					} else {
						altitudeTextField.setEnabled(false);
					}
				}
			});
			altitudeCheckBox.setSelected(UAVParam.overrideAltitude);
			GridBagConstraints gbc_altitudeCheckBox = new GridBagConstraints();
			gbc_altitudeCheckBox.anchor = GridBagConstraints.WEST;
			gbc_altitudeCheckBox.insets = new Insets(0, 0, 5, 5);
			gbc_altitudeCheckBox.gridx = 3;
			gbc_altitudeCheckBox.gridy = 5;
			contentPanel.add(altitudeCheckBox, gbc_altitudeCheckBox);
		}
		{
			JLabel label = new JLabel(Text.TARGET_ALTITUDE);
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.gridwidth = 3;
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 6;
			contentPanel.add(label, gbc_label);
		}
		{
			altitudeTextField = new JTextField();
			altitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			altitudeTextField.setText("" + UAVParam.minFlyingAltitude);
			GridBagConstraints gbc_altitudeTextField = new GridBagConstraints();
			gbc_altitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_altitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_altitudeTextField.gridx = 3;
			gbc_altitudeTextField.gridy = 6;
			contentPanel.add(altitudeTextField, gbc_altitudeTextField);
			altitudeTextField.setColumns(10);
		}
		if (!UAVParam.overrideAltitude) {
			altitudeTextField.setEnabled(false);
		}
		{
			JLabel label = new JLabel(Text.METERS);
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.insets = new Insets(0, 0, 5, 0);
			gbc_label.anchor = GridBagConstraints.WEST;
			gbc_label.gridx = 4;
			gbc_label.gridy = 6;
			contentPanel.add(label, gbc_label);
		}
		{
			JLabel lblOverrideWaypointYaw = new JLabel(Text.WAYPOINT_YAW_TITLE);
			GridBagConstraints gbc_lblOverrideWaypointYaw = new GridBagConstraints();
			gbc_lblOverrideWaypointYaw.anchor = GridBagConstraints.EAST;
			gbc_lblOverrideWaypointYaw.gridwidth = 3;
			gbc_lblOverrideWaypointYaw.insets = new Insets(0, 0, 5, 5);
			gbc_lblOverrideWaypointYaw.gridx = 0;
			gbc_lblOverrideWaypointYaw.gridy = 7;
			contentPanel.add(lblOverrideWaypointYaw, gbc_lblOverrideWaypointYaw);
		}
		{
			yawCheckBox = new JCheckBox();
			yawCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (yawCheckBox.isSelected()) {
						yawComboBox.setEnabled(true);
					} else {
						yawComboBox.setEnabled(false);
					}
				}
			});
			yawCheckBox.setSelected(UAVParam.overrideYaw);
			GridBagConstraints gbc_yawCheckBox = new GridBagConstraints();
			gbc_yawCheckBox.anchor = GridBagConstraints.WEST;
			gbc_yawCheckBox.insets = new Insets(0, 0, 5, 5);
			gbc_yawCheckBox.gridx = 3;
			gbc_yawCheckBox.gridy = 7;
			contentPanel.add(yawCheckBox, gbc_yawCheckBox);
		}
		{
			JLabel lblValue = new JLabel("Value:");
			lblValue.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblValue = new GridBagConstraints();
			gbc_lblValue.anchor = GridBagConstraints.EAST;
			gbc_lblValue.insets = new Insets(0, 0, 0, 5);
			gbc_lblValue.gridx = 2;
			gbc_lblValue.gridy = 8;
			contentPanel.add(lblValue, gbc_lblValue);
		}
		{
			yawComboBox = new JComboBox<String>();
			for (int i = 0; i < UAVParam.YAW_VALUES.length; i++) {
				yawComboBox.addItem(UAVParam.YAW_VALUES[i]);
			}
			yawComboBox.setSelectedIndex(UAVParam.yawBehavior);
			GridBagConstraints gbc_yawComboBox = new GridBagConstraints();
			gbc_yawComboBox.insets = new Insets(0, 0, 0, 5);
			gbc_yawComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_yawComboBox.gridx = 3;
			gbc_yawComboBox.gridy = 8;
			contentPanel.add(yawComboBox, gbc_yawComboBox);
		}
		if (!UAVParam.overrideYaw) {
			yawComboBox.setEnabled(false);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Text.OK);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(Param.role == ArduSim.SIMULATOR_GUI) {
							ValidationTools validationTools = API.getValidationTools();
							if (((String) missionEndComboBox.getSelectedItem()).equals(MissionKmlDialog.MISSION_END_RTL)) {
								String rtlAltitude = rtlAltitudeTextField.getText();
								if (!validationTools.isValidNonNegativeDouble(rtlAltitude)) {
									ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.RTL_ALTITUDE_ERROR);
									return;
								}
							}

							String altitudeString = minAltitudeTextField.getText();
							if (!validationTools.isValidPositiveDouble(altitudeString)) {
								ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.TARGET_MIN_ALTITUDE_ERROR);
								return;
							}
							double minAltitude = Double.parseDouble(minAltitudeTextField.getText());

							int delay;
							int distance = MissionKmlDialog.waypointDistance;
							double altitude;
							delay = (Integer) delaySpinner.getValue();
							if (delay != 0) {
								String distanceString = distanceTextField.getText();
								if (!validationTools.isValidPositiveInteger(distanceString)) {
									ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.TARGET_DISTANCE_ERROR_1);
									return;
								}
								distance = Integer.parseInt(distanceString);
								if (distance < 10 || distance > 1000) {
									ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.TARGET_DISTANCE_ERROR_2);
									return;
								}
							}

							if (altitudeCheckBox.isSelected()) {
								altitudeString = altitudeTextField.getText();
								if (!validationTools.isValidPositiveDouble(altitudeString)) {
									ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.TARGET_ALTITUDE_ERROR_1);
									return;
								}
								altitude = Double.parseDouble(altitudeString);
								if (altitude < minAltitude) {
									ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.TARGET_ALTITUDE_ERROR_2 + " " + minAltitude);
									return;
								}
								UAVParam.overrideAltitude = true;
								UAVParam.minFlyingAltitude = altitude;
							} else {
								UAVParam.overrideAltitude = false;
							}

							MissionKmlDialog.missionEnd = (String) missionEndComboBox.getSelectedItem();
							if (MissionKmlDialog.missionEnd.equals(MissionKmlDialog.MISSION_END_RTL)) {
								MissionKmlDialog.rtlFinalAltitude = Double.parseDouble(rtlAltitudeTextField.getText());
							}

							UAVParam.minAltitude = minAltitude;

							MissionKmlDialog.waypointDelay = delay;
							MissionKmlDialog.waypointDistance = distance;

							if (yawCheckBox.isSelected()) {
								UAVParam.overrideYaw = true;
								UAVParam.yawBehavior = yawComboBox.getSelectedIndex();

							} else {
								UAVParam.overrideYaw = false;
							}

							MissionKmlDialog.success = true;

							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									thisDialog.dispose();
								}
							});
						}
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
		
		this.thisDialog = this;
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setTitle(title);
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		
		SimTools.addEscListener(this, false);
		
		this.setVisible(true);
	}

	private void setDefaultParameters(){
		UAVParam.overrideAltitude = false;
		UAVParam.minAltitude = 5.0;
		MissionKmlDialog.waypointDelay = 0;
		UAVParam.overrideYaw = false;
		MissionKmlDialog.success = true;
	}


}
