package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;
import main.Text;
import uavController.UAVParam;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JRadioButton;
import java.awt.Insets;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JCheckBox;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class MissionDelayDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JDialog thisDialog;
	private final JPanel contentPanel = new JPanel();
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private final JSpinner delaySpinner;
	private JTextField distanceTextField;
	private JCheckBox overrideCheckBox;
	private JTextField altitudeTextField;
	private JTextField minAltitudeTextField;
	
	@SuppressWarnings("unused")
	private MissionDelayDialog() {
		this.thisDialog = null;
		this.delaySpinner = null;
	}

	public MissionDelayDialog(String title) {
		
		this.setSize(600, 400);
		
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.WEST);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel = new JLabel(Text.EXTEND_MISSION);
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 0;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			JRadioButton rdbtnUnmodified = new JRadioButton(Waypoint.MISSION_END_UNMODIFIED);
			rdbtnUnmodified.setFont(new Font("Dialog", Font.PLAIN, 12));
			buttonGroup.add(rdbtnUnmodified);
			GridBagConstraints gbc_rdbtnUnmodified = new GridBagConstraints();
			gbc_rdbtnUnmodified.insets = new Insets(0, 0, 5, 5);
			gbc_rdbtnUnmodified.gridx = 1;
			gbc_rdbtnUnmodified.gridy = 0;
			contentPanel.add(rdbtnUnmodified, gbc_rdbtnUnmodified);
		}
		{
			JRadioButton rdbtnLand = new JRadioButton(Waypoint.MISSION_END_LAND);
			rdbtnLand.setFont(new Font("Dialog", Font.PLAIN, 12));
			buttonGroup.add(rdbtnLand);
			GridBagConstraints gbc_rdbtnLand = new GridBagConstraints();
			gbc_rdbtnLand.insets = new Insets(0, 0, 5, 5);
			gbc_rdbtnLand.gridx = 2;
			gbc_rdbtnLand.gridy = 0;
			contentPanel.add(rdbtnLand, gbc_rdbtnLand);
		}
		{
			JRadioButton rdbtnRTL = new JRadioButton(Waypoint.MISSION_END_RTL);
			rdbtnRTL.setFont(new Font("Dialog", Font.PLAIN, 12));
			buttonGroup.add(rdbtnRTL);
			GridBagConstraints gbc_rdbtnRTL = new GridBagConstraints();
			gbc_rdbtnRTL.insets = new Insets(0, 0, 5, 5);
			gbc_rdbtnRTL.gridx = 3;
			gbc_rdbtnRTL.gridy = 0;
			contentPanel.add(rdbtnRTL, gbc_rdbtnRTL);
		}
		
		String selection = Waypoint.missionEnd;
		Enumeration<AbstractButton> buttons = buttonGroup.getElements();
		AbstractButton button;
		boolean found = false;
		while (buttons.hasMoreElements() && !found) {
			button = buttons.nextElement();
			if (selection.equals(button.getText())) {
				button.doClick();
				found = true;
			}
		}
		{
			JLabel label = new JLabel(Text.MIN_TARGET_ALTITUDE);
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.gridwidth = 3;
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 1;
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
			gbc_minAltitudeTextField.gridy = 1;
			contentPanel.add(minAltitudeTextField, gbc_minAltitudeTextField);
		}
		{
			JLabel label = new JLabel("m");
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.WEST;
			gbc_label.insets = new Insets(0, 0, 5, 0);
			gbc_label.gridx = 4;
			gbc_label.gridy = 1;
			contentPanel.add(label, gbc_label);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel(Text.XML_DELAY);
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.gridwidth = 3;
			gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 2;
			contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			SpinnerNumberModel sModel = new SpinnerNumberModel(Waypoint.waypointDelay, 0, 65535, 1);
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
			gbc_delaySpinner.gridy = 2;
			contentPanel.add(delaySpinner, gbc_delaySpinner);
		}
		{
			JLabel lblNewLabel_2 = new JLabel(Text.SECONDS);
			lblNewLabel_2.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 0);
			gbc_lblNewLabel_2.gridx = 4;
			gbc_lblNewLabel_2.gridy = 2;
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
			gbc_lblNewLabel_3.gridy = 3;
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
			gbc_distanceTextField.gridy = 3;
			contentPanel.add(distanceTextField, gbc_distanceTextField);
			distanceTextField.setColumns(10);
		}
		
		distanceTextField.setText("" + Waypoint.waypointDistance);
		
		{
			JLabel lblNewLabel_4 = new JLabel(Text.CENTIMETERS);
			lblNewLabel_4.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
			gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 0);
			gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_4.gridx = 4;
			gbc_lblNewLabel_4.gridy = 3;
			contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		}
		{
			JLabel label = new JLabel(Text.ALTITUDE_OVERRIDE);
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.gridwidth = 3;
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 4;
			contentPanel.add(label, gbc_label);
		}
		{
			overrideCheckBox = new JCheckBox();
			overrideCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (overrideCheckBox.isSelected()) {
						altitudeTextField.setEnabled(true);
					} else {
						altitudeTextField.setEnabled(false);
					}
				}
			});
			overrideCheckBox.setSelected(true);
			GridBagConstraints gbc_overrideCheckBox = new GridBagConstraints();
			gbc_overrideCheckBox.anchor = GridBagConstraints.WEST;
			gbc_overrideCheckBox.insets = new Insets(0, 0, 5, 5);
			gbc_overrideCheckBox.gridx = 3;
			gbc_overrideCheckBox.gridy = 4;
			contentPanel.add(overrideCheckBox, gbc_overrideCheckBox);
		}
		{
			JLabel label = new JLabel(Text.TARGET_ALTITUDE);
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.gridwidth = 3;
			gbc_label.insets = new Insets(0, 0, 0, 5);
			gbc_label.gridx = 0;
			gbc_label.gridy = 5;
			contentPanel.add(label, gbc_label);
		}
		{
			altitudeTextField = new JTextField();
			altitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			altitudeTextField.setText("" + UAVParam.minFlyingAltitude);
			GridBagConstraints gbc_altitudeTextField = new GridBagConstraints();
			gbc_altitudeTextField.insets = new Insets(0, 0, 0, 5);
			gbc_altitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_altitudeTextField.gridx = 3;
			gbc_altitudeTextField.gridy = 5;
			contentPanel.add(altitudeTextField, gbc_altitudeTextField);
			altitudeTextField.setColumns(10);
		}
		{
			JLabel label = new JLabel(Text.METERS);
			label.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.anchor = GridBagConstraints.WEST;
			gbc_label.gridx = 4;
			gbc_label.gridy = 5;
			contentPanel.add(label, gbc_label);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Text.OK);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Enumeration<AbstractButton> buttons = buttonGroup.getElements();
						AbstractButton button;
						String selection = null;
						boolean found = false;
						while (buttons.hasMoreElements() && !found) {
							button = buttons.nextElement();
							if (button.isSelected()) {
								selection = button.getText();
								found = true;
							}
						}
						
						String altitudeString = minAltitudeTextField.getText();
						if (!Tools.isValidPositiveDouble(altitudeString)) {
							GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_MIN_ALTITUDE_ERROR);
							return;
						}
						double minAltitude = Double.parseDouble(minAltitudeTextField.getText());
						
						int delay;
						int distance = Waypoint.waypointDistance;
						double altitude;
						delay = (Integer)delaySpinner.getValue();
						if (delay != 0) {
							String distanceString = distanceTextField.getText();
							if (!Tools.isValidPositiveInteger(distanceString)) {
								GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_DISTANCE_ERROR_1);
								return;
							}
							distance = Integer.parseInt(distanceString);
							if (distance < 10 || distance > 1000) {
								GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_DISTANCE_ERROR_2);
								return;
							}
						}

						if (overrideCheckBox.isSelected()) {
							altitudeString = altitudeTextField.getText();
							if (!Tools.isValidPositiveDouble(altitudeString)) {
								GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_ALTITUDE_ERROR_1);
								return;
							}
							altitude = Double.parseDouble(altitudeString);
							if (altitude < minAltitude) {
								GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_ALTITUDE_ERROR_2 + " " + minAltitude);
								return;
							}
							UAVParam.overrideAltitude = true;
							UAVParam.minFlyingAltitude = altitude;
						} else {
							UAVParam.overrideAltitude = false;
						}
						
						Waypoint.missionEnd = selection;
						
						UAVParam.minAltitude = minAltitude;
						
						Waypoint.waypointDelay = delay;
						Waypoint.waypointDistance = distance;
						
						thisDialog.dispose();
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
		this.setVisible(true);
	}

}
