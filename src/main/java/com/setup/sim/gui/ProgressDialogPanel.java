package com.setup.sim.gui;

import com.setup.Text;

import javax.swing.*;
import java.awt.*;

/** This class generates a panel with real time information gathered from the UAV on real time.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ProgressDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JLabel numUAVLabel;
	public JLabel xLabel;
	public JLabel yLabel;
	public JLabel zLabel;
	public JLabel speedLabel;
	public JLabel MAVModeLabel;
	public JLabel lblProtState;
	public JLabel protStateLabel;

	public ProgressDialogPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		JLabel lblnumUAV = new JLabel(Text.UAV_ID + ":");
		GridBagConstraints gbc_lblUAV = new GridBagConstraints();
		gbc_lblUAV.anchor = GridBagConstraints.WEST;
		gbc_lblUAV.insets = new Insets(5, 5, 5, 5);
		gbc_lblUAV.gridx = 0;
		gbc_lblUAV.gridy = 0;
		add(lblnumUAV, gbc_lblUAV);

		numUAVLabel = new JLabel();
		numUAVLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_numUAVLabel = new GridBagConstraints();
		gbc_numUAVLabel.anchor = GridBagConstraints.WEST;
		gbc_numUAVLabel.insets = new Insets(5, 5, 5, 5);
		gbc_numUAVLabel.gridx = 1;
		gbc_numUAVLabel.gridy = 0;
		add(numUAVLabel, gbc_numUAVLabel);

		JLabel lblX = new JLabel(Text.X_COORDINATE);
		GridBagConstraints gbc_lblX = new GridBagConstraints();
		gbc_lblX.anchor = GridBagConstraints.EAST;
		gbc_lblX.insets = new Insets(0, 0, 5, 5);
		gbc_lblX.gridx = 0;
		gbc_lblX.gridy = 1;
		add(lblX, gbc_lblX);

		xLabel = new JLabel(Text.NULL_TEXT);
		GridBagConstraints gbc_xLabel = new GridBagConstraints();
		gbc_xLabel.anchor = GridBagConstraints.EAST;
		gbc_xLabel.insets = new Insets(0, 0, 5, 5);
		gbc_xLabel.gridx = 1;
		gbc_xLabel.gridy = 1;
		add(xLabel, gbc_xLabel);

		JLabel lblM = new JLabel(Text.METERS);
		GridBagConstraints gbc_lblM = new GridBagConstraints();
		gbc_lblM.anchor = GridBagConstraints.WEST;
		gbc_lblM.insets = new Insets(0, 0, 5, 0);
		gbc_lblM.gridx = 2;
		gbc_lblM.gridy = 1;
		add(lblM, gbc_lblM);

		JLabel lblY = new JLabel(Text.Y_COORDINATE);
		GridBagConstraints gbc_lblY = new GridBagConstraints();
		gbc_lblY.anchor = GridBagConstraints.EAST;
		gbc_lblY.insets = new Insets(0, 0, 5, 5);
		gbc_lblY.gridx = 0;
		gbc_lblY.gridy = 2;
		add(lblY, gbc_lblY);

		yLabel = new JLabel(Text.NULL_TEXT);
		GridBagConstraints gbc_yLabel = new GridBagConstraints();
		gbc_yLabel.anchor = GridBagConstraints.EAST;
		gbc_yLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yLabel.gridx = 1;
		gbc_yLabel.gridy = 2;
		add(yLabel, gbc_yLabel);

		JLabel lblM_1 = new JLabel(Text.METERS);
		GridBagConstraints gbc_lblM_1 = new GridBagConstraints();
		gbc_lblM_1.anchor = GridBagConstraints.WEST;
		gbc_lblM_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblM_1.gridx = 2;
		gbc_lblM_1.gridy = 2;
		add(lblM_1, gbc_lblM_1);

		JLabel lblZ = new JLabel(Text.Z_COORDINATE);
		GridBagConstraints gbc_lblZ = new GridBagConstraints();
		gbc_lblZ.anchor = GridBagConstraints.EAST;
		gbc_lblZ.insets = new Insets(0, 0, 5, 5);
		gbc_lblZ.gridx = 0;
		gbc_lblZ.gridy = 3;
		add(lblZ, gbc_lblZ);

		zLabel = new JLabel(Text.NULL_TEXT);
		GridBagConstraints gbc_zLabel = new GridBagConstraints();
		gbc_zLabel.anchor = GridBagConstraints.EAST;
		gbc_zLabel.insets = new Insets(0, 0, 5, 5);
		gbc_zLabel.gridx = 1;
		gbc_zLabel.gridy = 3;
		add(zLabel, gbc_zLabel);

		JLabel lblM_2 = new JLabel(Text.METERS);
		GridBagConstraints gbc_lblM_2 = new GridBagConstraints();
		gbc_lblM_2.anchor = GridBagConstraints.WEST;
		gbc_lblM_2.insets = new Insets(0, 0, 5, 0);
		gbc_lblM_2.gridx = 2;
		gbc_lblM_2.gridy = 3;
		add(lblM_2, gbc_lblM_2);

		JLabel lblSpeed = new JLabel(Text.SPEED_1);
		GridBagConstraints gbc_lblSpeed = new GridBagConstraints();
		gbc_lblSpeed.anchor = GridBagConstraints.EAST;
		gbc_lblSpeed.insets = new Insets(0, 0, 5, 5);
		gbc_lblSpeed.gridx = 0;
		gbc_lblSpeed.gridy = 4;
		add(lblSpeed, gbc_lblSpeed);

		speedLabel = new JLabel(Text.NULL_TEXT);
		GridBagConstraints gbc_speedLabel = new GridBagConstraints();
		gbc_speedLabel.anchor = GridBagConstraints.EAST;
		gbc_speedLabel.insets = new Insets(0, 0, 5, 5);
		gbc_speedLabel.gridx = 1;
		gbc_speedLabel.gridy = 4;
		add(speedLabel, gbc_speedLabel);

		JLabel lblMs = new JLabel(Text.METERS_PER_SECOND);
		GridBagConstraints gbc_lblMs = new GridBagConstraints();
		gbc_lblMs.anchor = GridBagConstraints.WEST;
		gbc_lblMs.insets = new Insets(0, 0, 5, 0);
		gbc_lblMs.gridx = 2;
		gbc_lblMs.gridy = 4;
		add(lblMs, gbc_lblMs);

		JLabel lblMavMode = new JLabel(Text.MAV_MODE);
		GridBagConstraints gbc_lblMavStatus = new GridBagConstraints();
		gbc_lblMavStatus.anchor = GridBagConstraints.EAST;
		gbc_lblMavStatus.insets = new Insets(0, 0, 5, 5);
		gbc_lblMavStatus.gridx = 0;
		gbc_lblMavStatus.gridy = 5;
		add(lblMavMode, gbc_lblMavStatus);

		MAVModeLabel = new JLabel(Text.NULL_TEXT);
		GridBagConstraints gbc_MAVModeLabel = new GridBagConstraints();
		gbc_MAVModeLabel.gridwidth = 2;
		gbc_MAVModeLabel.anchor = GridBagConstraints.WEST;
		gbc_MAVModeLabel.insets = new Insets(0, 5, 5, 5);
		gbc_MAVModeLabel.gridx = 1;
		gbc_MAVModeLabel.gridy = 5;
		add(MAVModeLabel, gbc_MAVModeLabel);

		lblProtState = new JLabel();
		GridBagConstraints gbc_lblCapStatus = new GridBagConstraints();
		gbc_lblCapStatus.anchor = GridBagConstraints.EAST;
		gbc_lblCapStatus.insets = new Insets(0, 5, 5, 5);
		gbc_lblCapStatus.gridx = 0;
		gbc_lblCapStatus.gridy = 6;
		add(lblProtState, gbc_lblCapStatus);

		protStateLabel = new JLabel();
		GridBagConstraints gbc_CAPStatusLabel = new GridBagConstraints();
		gbc_CAPStatusLabel.gridwidth = 2;
		gbc_CAPStatusLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_CAPStatusLabel.insets = new Insets(0, 5, 5, 5);
		gbc_CAPStatusLabel.gridx = 1;
		gbc_CAPStatusLabel.gridy = 6;
		add(protStateLabel, gbc_CAPStatusLabel);

	}

}
