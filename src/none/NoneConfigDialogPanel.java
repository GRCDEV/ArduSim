package none;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;
import main.Param;
import main.Text;
import mbcap.logic.MBCAPText;

/** This class generates the panel to input the MBCAP protocol configuration in the corresponding dialog. */

public class NoneConfigDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JTextField missionsTextField;
	public JComboBox<String> UAVsComboBox;

	public NoneConfigDialogPanel() {
		
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
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				// Select kml file or waypoints files
				final File[] selection;
				final JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(Tools.getCurrentFolder());
				chooser.setDialogTitle(Text.MISSIONS_DIALOG_TITLE);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, Text.FILE_EXTENSION_KML);
				chooser.addChoosableFileFilter(filter1);
				FileNameExtensionFilter filter2 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_2, Text.FILE_EXTENSION_WAYPOINTS);
				chooser.addChoosableFileFilter(filter2);
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.setMultiSelectionEnabled(true);
				if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
					return;
				}
				
				selection = chooser.getSelectedFiles();
				if (selection.length == 0) {
					return;
				}
				
				String extension = Tools.getFileExtension(selection[0]);
				// Only one "kml" file is accepted
				if (extension.toUpperCase().equals(Text.FILE_EXTENSION_KML.toUpperCase()) && selection.length > 1) {
					GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_1);
					Tools.setLoadedMissionsFromFile(null);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							missionsTextField.setText("");
							UAVsComboBox.removeAllItems();
						}
					});
					return;
				}
				// waypoints files can not be mixed with kml files
				if (extension.toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
					for (int i = 1; i < selection.length; i++) {
						if (!Tools.getFileExtension(selection[i]).toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
							GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_2);
							Tools.setLoadedMissionsFromFile(null);
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									missionsTextField.setText("");
									UAVsComboBox.removeAllItems();
								}
							});
							return;
						}
					}
				}

				List<Waypoint>[] lists;
				// kml file selected
				if (extension.toUpperCase().equals(Text.FILE_EXTENSION_KML.toUpperCase())) {
					// All missions are loaded from one single file
					lists = Tools.loadXMLMissionsFile(selection[0]);
					if (lists == null) {
						GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_3);
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
					Tools.setLoadedMissionsFromFile(lists);
					// The number of UAVs is updated
					final int numUAVs = Math.min(Tools.getLoadedMissions().length, Param.numUAVs);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							missionsTextField.setText(selection[0].getAbsolutePath());
							UAVsComboBox.removeAllItems();
							for (int i = 0; i < numUAVs; i++) {
								UAVsComboBox.addItem("" + (i + 1));
							}
							UAVsComboBox.setSelectedIndex(UAVsComboBox.getItemCount() - 1);
						}
					});
				}

				// One or more waypoints files selected
				if (extension.toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {

					lists = new ArrayList[selection.length];
					// Load each mission from one file
					int j = 0;
					for (int i = 0; i < selection.length; i++) {
						List<Waypoint> current = Tools.loadMissionFile(selection[i].getAbsolutePath());
						if (current != null) {
							lists[j] = current;
							j++;
						}
					}
					// If no valid missions were found, just ignore the action
					if (j == 0) {
						GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_4);
						Tools.setLoadedMissionsFromFile(null);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								missionsTextField.setText("");
								UAVsComboBox.removeAllItems();
							}
						});
						return;
					}
					// The array must be resized if some file was incorrect
					if (j != selection.length) {
						List<Waypoint>[] aux = lists;
						lists = new ArrayList[j];
						int m = 0;
						for (int k = 0; k < selection.length; k++) {
							if (aux[k] != null) {
								lists[m] = aux[k];
								m++;
							}
						}
					}
					// Missions are stored
					Tools.setLoadedMissionsFromFile(lists);
					// The number of UAVs is updated
					final int numUAVs = Math.min(Tools.getLoadedMissions().length, Param.numUAVs);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							missionsTextField.setText(chooser.getCurrentDirectory().getAbsolutePath());
							UAVsComboBox.removeAllItems();
							for (int i = 0; i < numUAVs; i++) {
								UAVsComboBox.addItem("" + (i + 1));
							}
							UAVsComboBox.setSelectedIndex(UAVsComboBox.getItemCount() - 1);
						}
					});
				}
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
