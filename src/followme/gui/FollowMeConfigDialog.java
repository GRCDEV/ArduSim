package followme.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import api.GUI;
import api.Tools;
import api.pojo.formations.FlightFormation;
import api.pojo.formations.FlightFormation.Formation;
import followme.logic.FollowMeParam;
import followme.logic.FollowMeText;
import followme.pojo.RemoteInput;
import main.Text;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FollowMeConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JComboBox<String> groundComboBox;
	private JComboBox<String> airComboBox;
	private JTextField groundTextField;
	private JTextField flyingTextField;
	private JTextField landingTextField;
	private JTextField dataTextField;
	private JTextField messagePeriodTextField;
	private JTextField latitudeTextField;
	private JTextField longitudeTextField;
	private JTextField yawTextField;
	private JTextField relAltitudeTextField;
	private JTextField masterSpeedTextField;

	public FollowMeConfigDialog() {
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
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblAdfg = new JLabel(FollowMeText.GROUND_LOCATION);
			GridBagConstraints gbc_lblAdfg = new GridBagConstraints();
			gbc_lblAdfg.anchor = GridBagConstraints.WEST;
			gbc_lblAdfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblAdfg.gridx = 0;
			gbc_lblAdfg.gridy = 0;
			contentPanel.add(lblAdfg, gbc_lblAdfg);
		}
		{
			JLabel lblSdfh = new JLabel(FollowMeText.LATITUDE);
			lblSdfh.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblSdfh = new GridBagConstraints();
			gbc_lblSdfh.anchor = GridBagConstraints.EAST;
			gbc_lblSdfh.insets = new Insets(0, 0, 5, 5);
			gbc_lblSdfh.gridx = 0;
			gbc_lblSdfh.gridy = 1;
			contentPanel.add(lblSdfh, gbc_lblSdfh);
		}
		{
			latitudeTextField = new JTextField();
			latitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			latitudeTextField.setText("" + FollowMeParam.masterInitialLatitude);
			GridBagConstraints gbc_latitudeTextField = new GridBagConstraints();
			gbc_latitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_latitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_latitudeTextField.gridx = 1;
			gbc_latitudeTextField.gridy = 1;
			contentPanel.add(latitudeTextField, gbc_latitudeTextField);
			latitudeTextField.setColumns(10);
		}
		{
			JLabel lblMm = new JLabel(Text.DEGREE_SYMBOL);
			lblMm.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblMm = new GridBagConstraints();
			gbc_lblMm.anchor = GridBagConstraints.WEST;
			gbc_lblMm.insets = new Insets(0, 0, 5, 5);
			gbc_lblMm.gridx = 2;
			gbc_lblMm.gridy = 1;
			contentPanel.add(lblMm, gbc_lblMm);
		}
		{
			JLabel lblAsdfg = new JLabel(FollowMeText.LONGITUDE);
			lblAsdfg.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblAsdfg = new GridBagConstraints();
			gbc_lblAsdfg.anchor = GridBagConstraints.EAST;
			gbc_lblAsdfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblAsdfg.gridx = 0;
			gbc_lblAsdfg.gridy = 2;
			contentPanel.add(lblAsdfg, gbc_lblAsdfg);
		}
		{
			longitudeTextField = new JTextField();
			longitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			longitudeTextField.setText("" + FollowMeParam.masterInitialLongitude);
			GridBagConstraints gbc_longitudeTextField = new GridBagConstraints();
			gbc_longitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_longitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_longitudeTextField.gridx = 1;
			gbc_longitudeTextField.gridy = 2;
			contentPanel.add(longitudeTextField, gbc_longitudeTextField);
			longitudeTextField.setColumns(10);
		}
		{
			JLabel lblMm_1 = new JLabel(Text.DEGREE_SYMBOL);
			lblMm_1.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblMm_1 = new GridBagConstraints();
			gbc_lblMm_1.anchor = GridBagConstraints.WEST;
			gbc_lblMm_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblMm_1.gridx = 2;
			gbc_lblMm_1.gridy = 2;
			contentPanel.add(lblMm_1, gbc_lblMm_1);
		}
		{
			JLabel lblAdsfg = new JLabel(FollowMeText.YAW);
			lblAdsfg.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblAdsfg = new GridBagConstraints();
			gbc_lblAdsfg.anchor = GridBagConstraints.EAST;
			gbc_lblAdsfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblAdsfg.gridx = 0;
			gbc_lblAdsfg.gridy = 3;
			contentPanel.add(lblAdsfg, gbc_lblAdsfg);
		}
		{
			yawTextField = new JTextField();
			yawTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			yawTextField.setText("" + FollowMeParam.masterInitialYaw);
			GridBagConstraints gbc_yawTextField = new GridBagConstraints();
			gbc_yawTextField.insets = new Insets(0, 0, 5, 5);
			gbc_yawTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_yawTextField.gridx = 1;
			gbc_yawTextField.gridy = 3;
			contentPanel.add(yawTextField, gbc_yawTextField);
			yawTextField.setColumns(10);
		}
		{
			JLabel lblMm_2 = new JLabel(Text.DEGREE_SYMBOL);
			lblMm_2.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblMm_2 = new GridBagConstraints();
			gbc_lblMm_2.anchor = GridBagConstraints.WEST;
			gbc_lblMm_2.insets = new Insets(0, 0, 5, 5);
			gbc_lblMm_2.gridx = 2;
			gbc_lblMm_2.gridy = 3;
			contentPanel.add(lblMm_2, gbc_lblMm_2);
		}
		{
			JLabel groundFormationLabel = new JLabel(FollowMeText.GROUND_TEXT);
			GridBagConstraints gbc_groundFormationLabel = new GridBagConstraints();
			gbc_groundFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_groundFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationLabel.gridx = 0;
			gbc_groundFormationLabel.gridy = 4;
			contentPanel.add(groundFormationLabel, gbc_groundFormationLabel);
		}
		Formation[] formations = Formation.values();
		{
			groundComboBox = new JComboBox<String>();
			GridBagConstraints gbc_groundComboBox = new GridBagConstraints();
			gbc_groundComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_groundComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundComboBox.gridx = 1;
			gbc_groundComboBox.gridy = 5;
			if (formations.length > 0) {
				int pos = -1;
				String formation = FlightFormation.getGroundFormation().getName();
				for (int i = 0; i < formations.length; i++) {
					groundComboBox.addItem(formations[i].getName());
					if (formation.equalsIgnoreCase(formations[i].getName())) {
						pos = i;
					}
				}
				groundComboBox.setSelectedIndex(pos);
			}
			
			contentPanel.add(groundComboBox, gbc_groundComboBox);
		}
		{
			JLabel groundFormationFormationLabel = new JLabel(FollowMeText.FORMATION_TEXT);
			groundFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationFormationLabel = new GridBagConstraints();
			gbc_groundFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationFormationLabel.gridx = 0;
			gbc_groundFormationFormationLabel.gridy = 5;
			contentPanel.add(groundFormationFormationLabel, gbc_groundFormationFormationLabel);
		}
		{
			JLabel groundFormationDistanceLabel = new JLabel(FollowMeText.DISTANCE_TEXT);
			groundFormationDistanceLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationDistanceLabel = new GridBagConstraints();
			gbc_groundFormationDistanceLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationDistanceLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationDistanceLabel.gridx = 0;
			gbc_groundFormationDistanceLabel.gridy = 6;
			contentPanel.add(groundFormationDistanceLabel, gbc_groundFormationDistanceLabel);
		}
		{
			groundTextField = new JTextField("" + Tools.round(FlightFormation.getGroundFormationDistance(), 6));
			groundTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_groundTextField = new GridBagConstraints();
			gbc_groundTextField.insets = new Insets(0, 0, 5, 5);
			gbc_groundTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundTextField.gridx = 1;
			gbc_groundTextField.gridy = 6;
			contentPanel.add(groundTextField, gbc_groundTextField);
			groundTextField.setColumns(10);
		}
		{
			JLabel lblNewLabel = new JLabel(Text.METERS);
			lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 2;
			gbc_lblNewLabel.gridy = 6;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			JLabel airFormationLabel = new JLabel(FollowMeText.AIR_TEXT);
			GridBagConstraints gbc_airFormationLabel = new GridBagConstraints();
			gbc_airFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_airFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationLabel.gridx = 0;
			gbc_airFormationLabel.gridy = 7;
			contentPanel.add(airFormationLabel, gbc_airFormationLabel);
		}
		{
			airComboBox = new JComboBox<String>();
			GridBagConstraints gbc_airComboBox = new GridBagConstraints();
			gbc_airComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_airComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_airComboBox.gridx = 1;
			gbc_airComboBox.gridy = 8;
			if (formations.length > 0) {
				int pos = -1;
				String formation = FlightFormation.getFlyingFormation().getName();
				for (int i = 0; i < formations.length; i++) {
					airComboBox.addItem(formations[i].getName());
					if (formation.equalsIgnoreCase(formations[i].getName())) {
						pos = i;
					}
				}
				airComboBox.setSelectedIndex(pos);
			}
			contentPanel.add(airComboBox, gbc_airComboBox);
		}
		{
			JLabel airFormationFormationLabel = new JLabel(FollowMeText.FORMATION_TEXT);
			airFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_airFormationFormationLabel = new GridBagConstraints();
			gbc_airFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_airFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationFormationLabel.gridx = 0;
			gbc_airFormationFormationLabel.gridy = 8;
			contentPanel.add(airFormationFormationLabel, gbc_airFormationFormationLabel);
		}
		{
			JLabel lblFlightDistance = new JLabel(FollowMeText.DISTANCE_TEXT);
			lblFlightDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFlightDistance = new GridBagConstraints();
			gbc_lblFlightDistance.anchor = GridBagConstraints.EAST;
			gbc_lblFlightDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightDistance.gridx = 0;
			gbc_lblFlightDistance.gridy = 9;
			contentPanel.add(lblFlightDistance, gbc_lblFlightDistance);
		}
		{
			flyingTextField = new JTextField("" + Tools.round(FlightFormation.getFlyingFormationDistance(), 6));
			flyingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_flyingTextField = new GridBagConstraints();
			gbc_flyingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_flyingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_flyingTextField.gridx = 1;
			gbc_flyingTextField.gridy = 9;
			contentPanel.add(flyingTextField, gbc_flyingTextField);
			flyingTextField.setColumns(10);
		}
		{
			JLabel lblNewLabel_1 = new JLabel(Text.METERS);
			lblNewLabel_1.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 2;
			gbc_lblNewLabel_1.gridy = 9;
			contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			JLabel lblNewLabel_6 = new JLabel(FollowMeText.INITIAL_RELATIVE_ALTITUDE);
			lblNewLabel_6.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
			gbc_lblNewLabel_6.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_6.gridx = 0;
			gbc_lblNewLabel_6.gridy = 10;
			contentPanel.add(lblNewLabel_6, gbc_lblNewLabel_6);
		}
		{
			relAltitudeTextField = new JTextField();
			relAltitudeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			relAltitudeTextField.setText("" + FollowMeParam.slavesStartingAltitude);
			GridBagConstraints gbc_relAltitudeTextField = new GridBagConstraints();
			gbc_relAltitudeTextField.insets = new Insets(0, 0, 5, 5);
			gbc_relAltitudeTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_relAltitudeTextField.gridx = 1;
			gbc_relAltitudeTextField.gridy = 10;
			contentPanel.add(relAltitudeTextField, gbc_relAltitudeTextField);
			relAltitudeTextField.setColumns(10);
		}
		{
			JLabel lblNewLabel_7 = new JLabel(Text.METERS);
			lblNewLabel_7.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
			gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_7.gridx = 2;
			gbc_lblNewLabel_7.gridy = 10;
			contentPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);
		}
		{
			JLabel lblNewLabel_2 = new JLabel(FollowMeText.LANDING_TEXT);
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_2.gridx = 0;
			gbc_lblNewLabel_2.gridy = 11;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		{
			JLabel lblLandDistance = new JLabel(FollowMeText.DISTANCE_TEXT);
			lblLandDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblLandDistance = new GridBagConstraints();
			gbc_lblLandDistance.anchor = GridBagConstraints.EAST;
			gbc_lblLandDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblLandDistance.gridx = 0;
			gbc_lblLandDistance.gridy = 12;
			contentPanel.add(lblLandDistance, gbc_lblLandDistance);
		}
		{
			landingTextField = new JTextField("" + Tools.round(FlightFormation.getLandingFormationDistance(), 6));
			landingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_landingTextField = new GridBagConstraints();
			gbc_landingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_landingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_landingTextField.gridx = 1;
			gbc_landingTextField.gridy = 12;
			contentPanel.add(landingTextField, gbc_landingTextField);
			landingTextField.setColumns(10);
		}
		{
			JLabel lblNewLabel_3 = new JLabel(Text.METERS);
			lblNewLabel_3.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
			gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_3.gridx = 2;
			gbc_lblNewLabel_3.gridy = 12;
			contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		}
		{
			JLabel lblDfg = new JLabel(FollowMeText.SIMULATED_DATA);
			GridBagConstraints gbc_lblDfg = new GridBagConstraints();
			gbc_lblDfg.anchor = GridBagConstraints.WEST;
			gbc_lblDfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblDfg.gridx = 0;
			gbc_lblDfg.gridy = 13;
			contentPanel.add(lblDfg, gbc_lblDfg);
		}
		{
			dataTextField = new JTextField();
			dataTextField.setHorizontalAlignment(SwingConstants.LEFT);
			dataTextField.setEditable(false);
			GridBagConstraints gbc_dataTextField = new GridBagConstraints();
			gbc_dataTextField.gridwidth = 2;
			gbc_dataTextField.insets = new Insets(0, 0, 5, 5);
			gbc_dataTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_dataTextField.gridx = 0;
			gbc_dataTextField.gridy = 14;
			contentPanel.add(dataTextField, gbc_dataTextField);
			dataTextField.setColumns(10);
		}
		{
			JButton loadDataButton = new JButton(Text.BUTTON_SELECT);
			loadDataButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Load a data file and store values
					final File selection;
					JFileChooser chooser;
					chooser = new JFileChooser();
					chooser.setCurrentDirectory(Tools.getCurrentFolder());
					chooser.setDialogTitle(FollowMeText.SIMULATED_DATA_DIALOG_TITLE);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					FileNameExtensionFilter filter = new FileNameExtensionFilter(FollowMeText.DATA_TXT_FILE, Text.FILE_EXTENSION_TXT);
					chooser.addChoosableFileFilter(filter);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setMultiSelectionEnabled(false);
					if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
						FollowMeParam.masterData = null;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								dataTextField.setText("");
							}
						});
						return;
					}
					
					selection = chooser.getSelectedFile();
					Queue<RemoteInput> data = getData(selection);
					if (data != null && !data.isEmpty()) {
						FollowMeParam.masterData = data;data.toString();
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								dataTextField.setText(selection.getName());
							}
						});
					} else {
						FollowMeParam.masterData = null;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								dataTextField.setText("");
							}
						});
						GUI.warn(Text.VALIDATION_WARNING, FollowMeText.SIMULATED_DATA_ERROR);
					}
				}
			});
			GridBagConstraints gbc_loadDataButton = new GridBagConstraints();
			gbc_loadDataButton.insets = new Insets(0, 0, 5, 5);
			gbc_loadDataButton.gridx = 2;
			gbc_loadDataButton.gridy = 14;
			contentPanel.add(loadDataButton, gbc_loadDataButton);
		}
		{
			JLabel lblNewLabel_8 = new JLabel(FollowMeText.MASTER_UAV_SPEED);
			lblNewLabel_8.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
			gbc_lblNewLabel_8.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_8.gridx = 0;
			gbc_lblNewLabel_8.gridy = 15;
			contentPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);
		}
		{
			masterSpeedTextField = new JTextField();
			masterSpeedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			masterSpeedTextField.setText("" + FollowMeParam.masterSpeed);
			GridBagConstraints gbc_masterSpeedTextField = new GridBagConstraints();
			gbc_masterSpeedTextField.insets = new Insets(0, 0, 5, 5);
			gbc_masterSpeedTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_masterSpeedTextField.gridx = 1;
			gbc_masterSpeedTextField.gridy = 15;
			contentPanel.add(masterSpeedTextField, gbc_masterSpeedTextField);
			masterSpeedTextField.setColumns(10);
		}
		{
			JLabel lblNewLabel_9 = new JLabel(Text.CENTIMETERS_PER_SECOND);
			lblNewLabel_9.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
			gbc_lblNewLabel_9.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_9.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_9.gridx = 2;
			gbc_lblNewLabel_9.gridy = 15;
			contentPanel.add(lblNewLabel_9, gbc_lblNewLabel_9);
		}
		{
			JLabel lblNewLabel_4 = new JLabel(Text.COMMUNICATIONS);
			GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
			gbc_lblNewLabel_4.gridwidth = 2;
			gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_4.gridx = 0;
			gbc_lblNewLabel_4.gridy = 16;
			contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		}
		{
			JLabel lblNewLabel_5 = new JLabel(FollowMeText.COMM_PERIOD);
			lblNewLabel_5.setHorizontalAlignment(SwingConstants.CENTER);
			lblNewLabel_5.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
			gbc_lblNewLabel_5.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_5.gridx = 0;
			gbc_lblNewLabel_5.gridy = 17;
			contentPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		}
		{
			messagePeriodTextField = new JTextField("" + FollowMeParam.sendPeriod);
			messagePeriodTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_messagePeriodTextField = new GridBagConstraints();
			gbc_messagePeriodTextField.insets = new Insets(0, 0, 5, 5);
			gbc_messagePeriodTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_messagePeriodTextField.gridx = 1;
			gbc_messagePeriodTextField.gridy = 17;
			contentPanel.add(messagePeriodTextField, gbc_messagePeriodTextField);
			messagePeriodTextField.setColumns(10);
		}
		{
			JLabel lblDfg_1 = new JLabel(Text.MILLISECONDS);
			lblDfg_1.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblDfg_1 = new GridBagConstraints();
			gbc_lblDfg_1.anchor = GridBagConstraints.WEST;
			gbc_lblDfg_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblDfg_1.gridx = 2;
			gbc_lblDfg_1.gridy = 17;
			contentPanel.add(lblDfg_1, gbc_lblDfg_1);
		}
		{
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = 18;
			contentPanel.add(separator, gbc_separator);
		}
		JButton okButton = new JButton("OK");
		okButton.setForeground(Color.BLACK);
		GridBagConstraints gbc_okButton = new GridBagConstraints();
		gbc_okButton.insets = new Insets(0, 0, 0, 5);
		gbc_okButton.gridx = 2;
		gbc_okButton.gridy = 19;
		contentPanel.add(okButton, gbc_okButton);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(isValidConfiguration()) {
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					storeConfiguration();
					// State change
					Tools.setProtocolConfigured();
					
					dispose();
				} else {
					GUI.warn(Text.VALIDATION_WARNING, FollowMeText.BAD_INPUT);
				}		
			}
		});
		okButton.setActionCommand(Text.OK);
		getRootPane().setDefaultButton(okButton);
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				dispose();
				System.gc();
				System.exit(0);
			}
		});
		
		GUI.addEscapeListener(this, true);
		
		this.setTitle(FollowMeText.CONFIGURATION_DIALOG_TITLE_SWARM);
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);
	}
	
	private Queue<RemoteInput> getData(File file) {
		List<RemoteInput> content = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line = null;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			return null;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			return null;
		}
		List<String> checkedLines = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.length() > 0 && line.startsWith("1")) {// type=1 for RC override
				checkedLines.add(line);
			}
		}
		if (checkedLines.size() == 0) {
			return null;
		}
		String[] tokens;
		RemoteInput value;
		for (int i = 0; i < checkedLines.size(); i++) {
			tokens = checkedLines.get(i).split(",");
			if (tokens.length != 6) {
				return null;
			}
			try {
				value = new RemoteInput(Long.parseLong(tokens[1]),
						Integer.parseInt(tokens[2]),
						Integer.parseInt(tokens[3]),
						Integer.parseInt(tokens[4]),
						Integer.parseInt(tokens[5]));
			} catch (NumberFormatException e) {
				return null;
			}
			content.add(value);
		}
		
		if (content.size() == 0) {
			return null;
		}
		
		// Sort by date
		Collections.sort(content);
		// Reset initial time to zero
		long startingTime = content.get(0).time;
		RemoteInput current;
		for (int i = 0; i < content.size(); i++) {
			current = content.get(i);
			current.time = current.time - startingTime;
		}
		
		return new ArrayDeque<>(content);
	}

	
	private boolean isValidConfiguration() {
		String validating = latitudeTextField.getText();
		if (!Tools.isValidDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.LATITUDE_ERROR);
			return false;
		}
		
		validating = longitudeTextField.getText();
		if (!Tools.isValidDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.LONGITUDE_ERROR);
			return false;
		}
		
		validating = yawTextField.getText();
		if (!Tools.isValidDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.YAW_ERROR);
			return false;
		}
		
		validating = groundTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		validating = flyingTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		validating = relAltitudeTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.INITIAL_RELATIVE_ALTITUDE_ERROR);
			return false;
		}
		
		validating = landingTextField.getText();
		if (!Tools.isValidPositiveDouble(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		validating = dataTextField.getText();
		if (validating == null || validating.length() == 0) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.SIMULATED_DATA_ERROR);
			return false;
		}
		
		validating = masterSpeedTextField.getText();
		if (!Tools.isValidPositiveInteger(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.MASTER_UAV_SPEED_ERROR);
			return false;
		}
		
		validating = messagePeriodTextField.getText();
		if (!Tools.isValidPositiveInteger(validating)) {
			GUI.warn(Text.VALIDATION_WARNING, FollowMeText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		return true;
	}
	
	private void storeConfiguration() {
		FollowMeParam.masterInitialLatitude = Double.parseDouble(latitudeTextField.getText());
		FollowMeParam.masterInitialLongitude = Double.parseDouble(longitudeTextField.getText());
		FollowMeParam.masterInitialYaw = Double.parseDouble(yawTextField.getText()) * Math.PI / 180.0;
		double ground = Double.parseDouble(groundTextField.getText());
		double flying = Double.parseDouble(flyingTextField.getText());
		double landing = Double.parseDouble(landingTextField.getText());
		FlightFormation.setGroundFormation(Formation.getFormation((String)groundComboBox.getSelectedItem()));
		FlightFormation.setGroundFormationDistance(ground);
		FlightFormation.setFlyingFormation(Formation.getFormation((String)airComboBox.getSelectedItem()));
		FlightFormation.setFlyingFormationDistance(flying);
		FollowMeParam.slavesStartingAltitude = Double.parseDouble(relAltitudeTextField.getText());
		FlightFormation.setLandingFormationDistance(landing);
		FollowMeParam.masterSpeed = Integer.parseInt(masterSpeedTextField.getText());
		FollowMeParam.sendPeriod = Integer.parseInt(messagePeriodTextField.getText());
		
	}
	
	
}
