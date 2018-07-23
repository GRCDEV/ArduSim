package swarmprot.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;
import swarmprot.logic.SwarmProtParam;
import swarmprot.logic.SwarmProtText;

public class SwarmConfig extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField missionsTextField;
	JSpinner spinnerGround;
	JSpinner spinnerFlight;

	/**
	 * Create the dialog.
	 */
	public SwarmConfig() {
		setBounds(100, 100, 450, 300);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{355, 0};
		gridBagLayout.rowHeights = new int[]{163, 0};
		gridBagLayout.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc_contentPanel = new GridBagConstraints();
		gbc_contentPanel.insets = new Insets(10, 10, 10, 10);
		gbc_contentPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_contentPanel.gridx = 0;
		gbc_contentPanel.gridy = 0;
		getContentPane().add(contentPanel, gbc_contentPanel);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{114, 114, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 19, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblMapaMisin = new JLabel("Flight route map");
			GridBagConstraints gbc_lblMapaMisin = new GridBagConstraints();
			gbc_lblMapaMisin.anchor = GridBagConstraints.WEST;
			gbc_lblMapaMisin.fill = GridBagConstraints.VERTICAL;
			gbc_lblMapaMisin.insets = new Insets(0, 0, 5, 5);
			gbc_lblMapaMisin.gridx = 0;
			gbc_lblMapaMisin.gridy = 0;
			contentPanel.add(lblMapaMisin, gbc_lblMapaMisin);
		}
		{
			missionsTextField = new JTextField();
			GridBagConstraints gbc_missionsTextField = new GridBagConstraints();
			gbc_missionsTextField.insets = new Insets(0, 0, 5, 5);
			gbc_missionsTextField.fill = GridBagConstraints.BOTH;
			gbc_missionsTextField.gridx = 1;
			gbc_missionsTextField.gridy = 0;
			contentPanel.add(missionsTextField, gbc_missionsTextField);
			missionsTextField.setColumns(10);
		}
		{
			JButton btnMap = new JButton(SwarmProtText.BUTTON_SELECT);
			btnMap.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					// Select kml file or waypoints files
					final File selection;
					final JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(Tools.getCurrentFolder());
					chooser.setDialogTitle(SwarmProtText.MISSIONS_DIALOG_TITLE);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					FileNameExtensionFilter filter1 = new FileNameExtensionFilter(SwarmProtText.MISSIONS_DIALOG_SELECTION_1, SwarmProtText.FILE_EXTENSION_KML);
					chooser.addChoosableFileFilter(filter1);
					FileNameExtensionFilter filter2 = new FileNameExtensionFilter(SwarmProtText.MISSIONS_DIALOG_SELECTION_2, SwarmProtText.FILE_EXTENSION_WAYPOINTS);
					chooser.addChoosableFileFilter(filter2);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setMultiSelectionEnabled(false);
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						selection = chooser.getSelectedFile();
					} else {
						selection = null;
					}
					int numUAVs = Tools.getNumUAVs();
					if (selection != null) {
						List<Waypoint>[] lists;
						List<Waypoint> mission;
						String extension = Tools.getFileExtension(selection);
						// kml file selected
						if (extension.toUpperCase().equals(SwarmProtText.FILE_EXTENSION_KML.toUpperCase())) {
							// All missions are loaded from one single file
							lists = Tools.loadXMLMissionsFile(selection);
							if (lists == null) {
								GUI.warn(SwarmProtText.MISSIONS_SELECTION_ERROR, SwarmProtText.MISSIONS_ERROR_3);
								return;
							}
							// Missions are stored
							mission = lists[0];
							
							if (mission != null) {
								/** The master is assigned the first mission in the list */
								List<Waypoint>[] missions = new ArrayList[numUAVs];
								missions[SwarmProtParam.posMaster] = mission;
								Tools.setLoadedMissionsFromFile(missions);

							} else {
								JOptionPane.showMessageDialog(null, SwarmProtText.MISSIONS_ERROR_3, SwarmProtText.MISSIONS_SELECTION_ERROR,
										JOptionPane.WARNING_MESSAGE);

								return;
							}
							
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									missionsTextField.setText(selection.getName());
								}
							});
						}

						// One or more waypoints files selected
						if (extension.toUpperCase().equals(SwarmProtText.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
							// Load one mission from one file
							List<Waypoint> current = Tools.loadMissionFile(selection.getAbsolutePath());
							
							// The mission is stored
							if (current != null) {
								/** The master is assigned the first mission in the list */
								List<Waypoint>[] missions = new ArrayList[numUAVs];
								missions[SwarmProtParam.posMaster] = current;
								Tools.setLoadedMissionsFromFile(missions);
							} else {
								JOptionPane.showMessageDialog(null, SwarmProtText.MISSIONS_ERROR_3, SwarmProtText.MISSIONS_SELECTION_ERROR,
										JOptionPane.WARNING_MESSAGE);

								return;
							}
							
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									missionsTextField.setText(selection.getName());
								}
							});
						}
					}
				}
			});
			GridBagConstraints gbc_btnMap = new GridBagConstraints();
			gbc_btnMap.insets = new Insets(0, 0, 5, 5);
			gbc_btnMap.gridx = 2;
			gbc_btnMap.gridy = 0;
			contentPanel.add(btnMap, gbc_btnMap);
			
			
			
			
			
		}
		{
			JLabel lblGroundFormationDistance = new JLabel("Ground formation distance (m)");
			GridBagConstraints gbc_lblGroundFormationDistance = new GridBagConstraints();
			gbc_lblGroundFormationDistance.anchor = GridBagConstraints.WEST;
			gbc_lblGroundFormationDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblGroundFormationDistance.gridx = 0;
			gbc_lblGroundFormationDistance.gridy = 2;
			contentPanel.add(lblGroundFormationDistance, gbc_lblGroundFormationDistance);
		}
		{
			SpinnerNumberModel model1 = new SpinnerNumberModel(1.0, 1.0, 100.0, 1.0);  
			spinnerGround = new JSpinner(model1);
			JFormattedTextField txt = ((JSpinner.NumberEditor) spinnerGround.getEditor()).getTextField();
			((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
			GridBagConstraints gbc_spinnerGround = new GridBagConstraints();
			gbc_spinnerGround.anchor = GridBagConstraints.EAST;
			gbc_spinnerGround.insets = new Insets(0, 0, 5, 5);
			gbc_spinnerGround.gridx = 1;
			gbc_spinnerGround.gridy = 2;
			contentPanel.add(spinnerGround, gbc_spinnerGround);
		}
		{
			JLabel lblFlightDistance = new JLabel("Flight formation distance (m)");
			GridBagConstraints gbc_lblFlightDistance = new GridBagConstraints();
			gbc_lblFlightDistance.anchor = GridBagConstraints.WEST;
			gbc_lblFlightDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightDistance.gridx = 0;
			gbc_lblFlightDistance.gridy = 4;
			contentPanel.add(lblFlightDistance, gbc_lblFlightDistance);
		}
		{
			SpinnerNumberModel model2 = new SpinnerNumberModel(5.0, 5.0, 100.0, 1.0); 
			spinnerFlight = new JSpinner(model2);
			JFormattedTextField txt = ((JSpinner.NumberEditor) spinnerFlight.getEditor()).getTextField();
			((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
			GridBagConstraints gbc_spinnerFlight = new GridBagConstraints();
			gbc_spinnerFlight.anchor = GridBagConstraints.EAST;
			gbc_spinnerFlight.insets = new Insets(0, 0, 5, 5);
			gbc_spinnerFlight.gridx = 1;
			gbc_spinnerFlight.gridy = 4;
			contentPanel.add(spinnerFlight, gbc_spinnerFlight);
		}
		{
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = 6;
			contentPanel.add(separator, gbc_separator);
		}
		JButton okButton = new JButton("OK");
		okButton.setForeground(Color.BLACK);
		GridBagConstraints gbc_okButton = new GridBagConstraints();
		gbc_okButton.insets = new Insets(0, 0, 0, 5);
		gbc_okButton.gridx = 2;
		gbc_okButton.gridy = 7;
		contentPanel.add(okButton, gbc_okButton);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(Tools.getLoadedMissions() != null ) {
					//Acepta la configuración y cambia el estado
					Tools.setProtocolConfigured();
					Double groud = (Double) spinnerGround.getValue();
					Double air = (Double) spinnerFlight.getValue();
					SwarmProtParam.initialDistanceBetweenUAV = groud.intValue();
					SwarmProtParam.initialDistanceBetweenUAVreal = air.intValue();

					dispose();
				}else {
					JOptionPane.showMessageDialog(null, SwarmProtText.BAD_INPUT);
				}					
				}
		});
		okButton.setActionCommand("OK");
		getRootPane().setDefaultButton(okButton);
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				dispose();
				System.gc();
				System.exit(0);
			}
		});
		
		this.setTitle(SwarmProtText.CONFIGURATION_DIALOG_TITLE_SWARM);
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);


	}

}
