package mission.gui;

import api.API;
import api.pojo.location.Waypoint;
import main.api.MissionHelper;
import mbcap.logic.MBCAPText;
import org.javatuples.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/** This class generates the panel to input the MBCAP protocol configuration in the corresponding dialog.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionConfigDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JTextField missionsTextField;
	public JComboBox<String> UAVsComboBox;

	public MissionConfigDialogPanel() {
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0, 0, 5 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 31 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);
		
		JLabel lblSimpam = new JLabel(MBCAPText.SIMULATION_PARAMETERS);
		GridBagConstraints gbc_lblSimpam = new GridBagConstraints();
		gbc_lblSimpam.gridwidth = 2;
		gbc_lblSimpam.anchor = GridBagConstraints.WEST;
		gbc_lblSimpam.insets = new Insets(0, 0, 5, 5);
		gbc_lblSimpam.gridx = 0;
		gbc_lblSimpam.gridy = 0;
		add(lblSimpam, gbc_lblSimpam);
		
		JLabel lblmissions = new JLabel(MBCAPText.MISSIONS_SELECTION);
		lblmissions.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblmissions = new GridBagConstraints();
		gbc_lblmissions.gridwidth = 2;
		gbc_lblmissions.insets = new Insets(0, 0, 5, 5);
		gbc_lblmissions.anchor = GridBagConstraints.EAST;
		gbc_lblmissions.gridx = 0;
		gbc_lblmissions.gridy = 1;
		add(lblmissions, gbc_lblmissions);
		
		missionsTextField = new JTextField();
		missionsTextField.setEditable(false);
		GridBagConstraints gbc_missionsTextField = new GridBagConstraints();
		gbc_missionsTextField.gridwidth = 2;
		gbc_missionsTextField.insets = new Insets(0, 0, 5, 5);
		gbc_missionsTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_missionsTextField.gridx = 2;
		gbc_missionsTextField.gridy = 1;
		add(missionsTextField, gbc_missionsTextField);
		missionsTextField.setColumns(25);
		
		JButton missionsButton = new JButton(MBCAPText.BUTTON_SELECT);
		missionsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File[] fileArray = API.getGUI(0).searchMissionFiles();
				final Pair<String, List<Waypoint>[]> missions = API.getGUI(0).loadMissions(fileArray);
				MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
				if (missions == null) {
					missionHelper.setMissionsLoaded(null);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							missionsTextField.setText("");
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}
				
				// Missions are stored
				missionHelper.setMissionsLoaded(missions.getValue1());
				// The number of UAVs is updated
				final int numUAVs = Math.min(missions.getValue1().length, API.getArduSim().getNumUAVs());
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						missionsTextField.setText(missions.getValue0());
						UAVsComboBox.removeAllItems();
						for (int i = 0; i < numUAVs; i++) {
							UAVsComboBox.addItem("" + (i + 1));
						}
						UAVsComboBox.setSelectedIndex(UAVsComboBox.getItemCount() - 1);
					}
				});
			}
		});
		GridBagConstraints gbc_missionsButton = new GridBagConstraints();
		gbc_missionsButton.insets = new Insets(0, 0, 5, 0);
		gbc_missionsButton.gridx = 4;
		gbc_missionsButton.gridy = 1;
		add(missionsButton, gbc_missionsButton);
		
		JLabel lblNumberOfUAVs = new JLabel(MBCAPText.UAV_NUMBER);
		lblNumberOfUAVs.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblNumberOfUAVs = new GridBagConstraints();
		gbc_lblNumberOfUAVs.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfUAVs.gridwidth = 2;
		gbc_lblNumberOfUAVs.insets = new Insets(0, 0, 0, 5);
		gbc_lblNumberOfUAVs.gridx = 0;
		gbc_lblNumberOfUAVs.gridy = 2;
		add(lblNumberOfUAVs, gbc_lblNumberOfUAVs);
		
		UAVsComboBox = new JComboBox<String>();
		GridBagConstraints gbc_UAVsComboBox = new GridBagConstraints();
		gbc_UAVsComboBox.gridwidth = 2;
		gbc_UAVsComboBox.insets = new Insets(0, 0, 0, 5);
		gbc_UAVsComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_UAVsComboBox.gridx = 2;
		gbc_UAVsComboBox.gridy = 2;
		add(UAVsComboBox, gbc_UAVsComboBox);
	}

}
