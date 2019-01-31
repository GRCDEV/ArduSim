package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import api.GUI;
import main.Text;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class MissionWaypointsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JDialog thisDialog;
	private final JPanel contentPanel = new JPanel();
	private JCheckBox yawCheckBox;
	private JComboBox<String> yawComboBox;
	
	public static volatile boolean success = false;	//To check if the dialog was closed correctly
	
	@SuppressWarnings("unused")
	private MissionWaypointsDialog() {
		this.thisDialog = null;
	}

	public MissionWaypointsDialog(String title) {
		getContentPane().setLayout(new BorderLayout());
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
			yawComboBox = new JComboBox<String>();
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
		
		GUI.addEscapeListener(this, false);
		
		this.setVisible(true);
	}

}
