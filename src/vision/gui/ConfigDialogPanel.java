package vision.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.javatuples.Pair;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;

/** This class generates the panel to input the MBCAP protocol configuration in the corresponding dialog.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ConfigDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JTextField missionsTextField;
	public JComboBox<String> UAVsComboBox;
	private ResourceBundle rs;

	public ConfigDialogPanel() {
		  try {
				rs = ResourceBundle.getBundle("vision.bundle.text", new Locale("en"));
		  }catch(java.util.MissingResourceException e){
				GUI.log("shutdown, no resource bundle found.");
		        System.gc(); // Needed to avoid the error: Exception while removing reference.
		        System.exit(0);
		  }
		  
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0, 0, 5 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 31 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);
		
		JLabel lblSimpam = new JLabel(rs.getString("simulation_parameters"));
		GridBagConstraints gbc_lblSimpam = new GridBagConstraints();
		gbc_lblSimpam.gridwidth = 2;
		gbc_lblSimpam.anchor = GridBagConstraints.WEST;
		gbc_lblSimpam.insets = new Insets(0, 0, 5, 5);
		gbc_lblSimpam.gridx = 0;
		gbc_lblSimpam.gridy = 0;
		add(lblSimpam, gbc_lblSimpam);
		
		JLabel lblmissions = new JLabel(rs.getString("mission_selection"));
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
		
		JButton missionsButton = new JButton(rs.getString("dotdotdot"));
		missionsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Pair<String, List<Waypoint>[]> missions = GUI.loadMissions();
				if (missions == null) {
					Tools.setLoadedMissionsFromFile(null);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							missionsTextField.setText("");
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}
				
				// Missions are stored
				Tools.setLoadedMissionsFromFile(missions.getValue1());
				// The number of UAVs is updated
				final int numUAVs = Math.min(missions.getValue1().length, Tools.getNumUAVs());
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
		
		JLabel lblNumberOfUAVs = new JLabel(rs.getString("uavNumber"));
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
