package com.setup.sim.gui;

import com.setup.Param;
import com.setup.Text;
import com.api.ArduSim;
import com.setup.sim.logic.SimTools;
import com.uavController.UAVParam;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** 
 * Dialog used to input options while loading missions.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionWaypointsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JDialog thisDialog;
	private JCheckBox yawCheckBox;
	private JComboBox<String> yawComboBox;
	
	public static volatile boolean success = false;	//To check if the dialog was closed correctly
	
	@SuppressWarnings("unused")
	private MissionWaypointsDialog() {
		this.thisDialog = null;
	}

	public MissionWaypointsDialog(String title) {
		if(Param.role == ArduSim.SIMULATOR_CLI){
			thisDialog = null;
			setDefaultParameters();
			return;
		}
		getContentPane().setLayout(new BorderLayout());
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.WEST);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblOverrideWaypointYaw = new JLabel(Text.WAYPOINT_YAW_TITLE);
			GridBagConstraints gbc_lblOverrideWaypointYaw = new GridBagConstraints();
			gbc_lblOverrideWaypointYaw.anchor = GridBagConstraints.EAST;
			gbc_lblOverrideWaypointYaw.gridwidth = 3;
			gbc_lblOverrideWaypointYaw.insets = new Insets(0, 0, 5, 5);
			gbc_lblOverrideWaypointYaw.gridx = 0;
			gbc_lblOverrideWaypointYaw.gridy = 0;
			contentPanel.add(lblOverrideWaypointYaw, gbc_lblOverrideWaypointYaw);
		}
		{
			yawCheckBox = new JCheckBox();
			yawCheckBox.addActionListener(e -> yawComboBox.setEnabled(yawCheckBox.isSelected()));
			yawCheckBox.setSelected(UAVParam.overrideYaw);
			GridBagConstraints gbc_yawCheckBox = new GridBagConstraints();
			gbc_yawCheckBox.anchor = GridBagConstraints.WEST;
			gbc_yawCheckBox.insets = new Insets(0, 0, 5, 0);
			gbc_yawCheckBox.gridx = 3;
			gbc_yawCheckBox.gridy = 0;
			contentPanel.add(yawCheckBox, gbc_yawCheckBox);
		}
		{
			JLabel lblValue = new JLabel("Value:");
			lblValue.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblValue = new GridBagConstraints();
			gbc_lblValue.anchor = GridBagConstraints.EAST;
			gbc_lblValue.insets = new Insets(0, 0, 0, 5);
			gbc_lblValue.gridx = 2;
			gbc_lblValue.gridy = 1;
			contentPanel.add(lblValue, gbc_lblValue);
		}
		{
			yawComboBox = new JComboBox<>();
			for (int i = 0; i < UAVParam.YAW_VALUES.length; i++) {
				yawComboBox.addItem(UAVParam.YAW_VALUES[i]);
			}
			yawComboBox.setSelectedIndex(UAVParam.yawBehavior);
			GridBagConstraints gbc_yawComboBox = new GridBagConstraints();
			gbc_yawComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_yawComboBox.gridx = 3;
			gbc_yawComboBox.gridy = 1;
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
						
						if (yawCheckBox.isSelected()) {
							UAVParam.overrideYaw = true;
							UAVParam.yawBehavior = yawComboBox.getSelectedIndex();
							
						} else {
							UAVParam.overrideYaw = false;
						}
						
						MissionWaypointsDialog.success = true;
						
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
		
		SimTools.addEscListener(this, false);
		
		this.setVisible(true);
	}

	private void setDefaultParameters() {
		UAVParam.overrideYaw = false;
		success = true;
	}

}
