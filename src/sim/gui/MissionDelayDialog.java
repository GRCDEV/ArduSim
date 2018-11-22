package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import api.GUI;
import api.pojo.Waypoint;
import main.Text;

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

/** Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class MissionDelayDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JDialog thisDialog;
	private final JPanel contentPanel = new JPanel();
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private final JSpinner delaySpinner;
	private JTextField distanceTextField;
	
	@SuppressWarnings("unused")
	private MissionDelayDialog() {
		this.thisDialog = null;
		this.delaySpinner = null;
	}

	public MissionDelayDialog(String title) {
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
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
			JLabel lblNewLabel_1 = new JLabel(Text.XML_DELAY);
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.gridwidth = 3;
			gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
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
			gbc_delaySpinner.gridy = 1;
			contentPanel.add(delaySpinner, gbc_delaySpinner);
		}
		{
			JLabel lblNewLabel_2 = new JLabel(Text.SECONDS);
			lblNewLabel_2.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 0);
			gbc_lblNewLabel_2.gridx = 4;
			gbc_lblNewLabel_2.gridy = 1;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		{
			JLabel lblNewLabel_3 = new JLabel(Text.TARGET_DISTANCE);
			GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
			gbc_lblNewLabel_3.gridwidth = 3;
			gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_3.insets = new Insets(0, 0, 0, 5);
			gbc_lblNewLabel_3.gridx = 0;
			gbc_lblNewLabel_3.gridy = 2;
			contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		}
		{
			distanceTextField = new JTextField();
			if ((int)delaySpinner.getValue() == 0) {
				distanceTextField.setEnabled(false);
			}
			distanceTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_distanceTextField = new GridBagConstraints();
			gbc_distanceTextField.insets = new Insets(0, 0, 0, 5);
			gbc_distanceTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_distanceTextField.gridx = 3;
			gbc_distanceTextField.gridy = 2;
			contentPanel.add(distanceTextField, gbc_distanceTextField);
			distanceTextField.setColumns(10);
		}
		
		distanceTextField.setText("" + Waypoint.waypointDistance);
		
		{
			JLabel lblNewLabel_4 = new JLabel(Text.CENTIMETERS);
			lblNewLabel_4.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
			gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_4.gridx = 4;
			gbc_lblNewLabel_4.gridy = 2;
			contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Text.OK);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int delay;
						int distance;
						try {
							delay = (Integer)delaySpinner.getValue();
							if (delay != 0) {
								distance = Integer.parseInt(distanceTextField.getText());
								if (distance < 10 || distance > 1000) {
									GUI.warn(Text.VALIDATION_WARNING, Text.TARGET_DISTANCE_ERROR);
									return;
								}
								Waypoint.waypointDistance = distance;
								Waypoint.waypointDelay = delay;
							}
							
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
							Waypoint.missionEnd = selection;
							
							thisDialog.dispose();
						} catch (NumberFormatException e1) {}
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
