package muscop.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.javatuples.Pair;

import api.API;
import api.pojo.location.Waypoint;
import main.Text;
import main.api.FlightFormationTools;
import main.api.GUI;
import main.api.MissionHelper;
import main.api.SafeTakeOffHelper;
import main.api.ValidationTools;
import muscop.logic.MUSCOPText;

/** 
 * This dialog shows the configuration needed for the MUSCOP protocol during simulations.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPConfigDialog extends JDialog {
	
	private GUI gui;
	private ValidationTools validationTools;
	private FlightFormationTools formationTools;
	private SafeTakeOffHelper takeOffHelper;

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField missionsTextField;
	private JComboBox<String> groundComboBox;
	private JComboBox<String> takeOffStrategyComboBox;
	private JComboBox<String> airComboBox;
	private JTextField groundTextField;
	private JTextField flyingTextField;
	private JTextField landingTextField;

	public MUSCOPConfigDialog() {
		setBounds(100, 100, 450, 300);
		
		this.gui = API.getGUI(0);
		this.validationTools = API.getValidationTools();
		this.formationTools = API.getFlightFormationTools();
		this.takeOffHelper = API.getCopter(0).getSafeTakeOffHelper();
		
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
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblMapaMisin = new JLabel(MUSCOPText.MISSION_SELECT);
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
			missionsTextField.setEditable(false);
			GridBagConstraints gbc_missionsTextField = new GridBagConstraints();
			gbc_missionsTextField.insets = new Insets(0, 0, 5, 5);
			gbc_missionsTextField.fill = GridBagConstraints.BOTH;
			gbc_missionsTextField.gridx = 1;
			gbc_missionsTextField.gridy = 0;
			contentPanel.add(missionsTextField, gbc_missionsTextField);
			missionsTextField.setColumns(10);
		}
		{
			JButton btnMap = new JButton(MUSCOPText.BUTTON_SELECT);
			btnMap.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					final Pair<String, List<Waypoint>[]> missions = gui.loadMissions();
					MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
					if (missions == null) {
						missionHelper.setMissionsLoaded(null);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								missionsTextField.setText("");
							}
						});
					} else {
						int numUAVs = API.getArduSim().getNumUAVs();
						/** The master is assigned the first mission in the list */
						List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
						// The master UAV is always in the position 0 of arrays
						missionsFinal[0] = missions.getValue1()[0];
						missionHelper.setMissionsLoaded(missionsFinal);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								missionsTextField.setText(missions.getValue0());
							}
						});
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
			JLabel groundFormationLabel = new JLabel(MUSCOPText.GROUND_TEXT);
			GridBagConstraints gbc_groundFormationLabel = new GridBagConstraints();
			gbc_groundFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_groundFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationLabel.gridx = 0;
			gbc_groundFormationLabel.gridy = 2;
			contentPanel.add(groundFormationLabel, gbc_groundFormationLabel);
		}
		{
			JLabel groundFormationFormationLabel = new JLabel(MUSCOPText.FORMATION_TEXT);
			groundFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationFormationLabel = new GridBagConstraints();
			gbc_groundFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationFormationLabel.gridx = 0;
			gbc_groundFormationFormationLabel.gridy = 3;
			contentPanel.add(groundFormationFormationLabel, gbc_groundFormationFormationLabel);
		}
		String[] formations = formationTools.getAvailableFormations();
		{
			groundComboBox = new JComboBox<String>();
			GridBagConstraints gbc_groundComboBox = new GridBagConstraints();
			gbc_groundComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_groundComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundComboBox.gridx = 1;
			gbc_groundComboBox.gridy = 3;
			if (formations.length > 0) {
				int pos = -1;
				for (int i = 0; i < formations.length; i++) {
					groundComboBox.addItem(formations[i]);
					if (formationTools.getGroundFormationName().equalsIgnoreCase(formations[i])) {
						pos = i;
					}
				}
				groundComboBox.setSelectedIndex(pos);
			}
			contentPanel.add(groundComboBox, gbc_groundComboBox);
		}
		{
			JLabel groundFormationDistanceLabel = new JLabel(MUSCOPText.DISTANCE_TEXT);
			groundFormationDistanceLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationDistanceLabel = new GridBagConstraints();
			gbc_groundFormationDistanceLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationDistanceLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationDistanceLabel.gridx = 0;
			gbc_groundFormationDistanceLabel.gridy = 4;
			contentPanel.add(groundFormationDistanceLabel, gbc_groundFormationDistanceLabel);
		}
		{
			groundTextField = new JTextField("" + validationTools.roundDouble(formationTools.getGroundFormationMinimumDistance(), 6));
			groundTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_groundTextField = new GridBagConstraints();
			gbc_groundTextField.insets = new Insets(0, 0, 5, 5);
			gbc_groundTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundTextField.gridx = 1;
			gbc_groundTextField.gridy = 4;
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
			gbc_lblNewLabel.gridy = 4;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			JLabel lblDfg = new JLabel(MUSCOPText.TAKEOFF_STRATEGY);
			lblDfg.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblDfg = new GridBagConstraints();
			gbc_lblDfg.anchor = GridBagConstraints.EAST;
			gbc_lblDfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblDfg.gridx = 0;
			gbc_lblDfg.gridy = 5;
			contentPanel.add(lblDfg, gbc_lblDfg);
		}
		{
			takeOffStrategyComboBox = new JComboBox<String>();
			GridBagConstraints gbc_takeOffStrategyComboBox = new GridBagConstraints();
			gbc_takeOffStrategyComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_takeOffStrategyComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_takeOffStrategyComboBox.gridx = 1;
			gbc_takeOffStrategyComboBox.gridy = 5;
			String[] algorithms = takeOffHelper.getAvailableTakeOffAlgorithms();
			String selected = takeOffHelper.getTakeOffAlgorithm();
			int selectedPos = 0;
			for (int i = 0; i < algorithms.length; i++) {
				takeOffStrategyComboBox.addItem(algorithms[i]);
				if (algorithms[i].equalsIgnoreCase(selected)) {
					selectedPos = i;
				}
			}
			takeOffStrategyComboBox.setSelectedIndex(selectedPos);
			contentPanel.add(takeOffStrategyComboBox, gbc_takeOffStrategyComboBox);
		}
		{
			JLabel airFormationLabel = new JLabel(MUSCOPText.AIR_TEXT);
			GridBagConstraints gbc_airFormationLabel = new GridBagConstraints();
			gbc_airFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_airFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationLabel.gridx = 0;
			gbc_airFormationLabel.gridy = 7;
			contentPanel.add(airFormationLabel, gbc_airFormationLabel);
		}
		{
			JLabel airFormationFormationLabel = new JLabel(MUSCOPText.FORMATION_TEXT);
			airFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_airFormationFormationLabel = new GridBagConstraints();
			gbc_airFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_airFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationFormationLabel.gridx = 0;
			gbc_airFormationFormationLabel.gridy = 8;
			contentPanel.add(airFormationFormationLabel, gbc_airFormationFormationLabel);
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
				String formationString = formationTools.getFlyingFormationName();
				for (int i = 0; i < formations.length; i++) {
					airComboBox.addItem(formations[i]);
					if (formationString.equalsIgnoreCase(formations[i])) {
						pos = i;
					}
				}
				airComboBox.setSelectedIndex(pos);
			}
			contentPanel.add(airComboBox, gbc_airComboBox);
		}
		{
			JLabel lblFlightDistance = new JLabel(MUSCOPText.DISTANCE_TEXT);
			lblFlightDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFlightDistance = new GridBagConstraints();
			gbc_lblFlightDistance.anchor = GridBagConstraints.EAST;
			gbc_lblFlightDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightDistance.gridx = 0;
			gbc_lblFlightDistance.gridy = 9;
			contentPanel.add(lblFlightDistance, gbc_lblFlightDistance);
		}
		{
			flyingTextField = new JTextField("" + validationTools.roundDouble(formationTools.getFlyingFormationMinimumDistance(), 6));
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
			JLabel lblNewLabel_2 = new JLabel(MUSCOPText.LANDING_TEXT);
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_2.gridx = 0;
			gbc_lblNewLabel_2.gridy = 11;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		{
			JLabel lblLandDistance = new JLabel(MUSCOPText.DISTANCE_TEXT);
			lblLandDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblLandDistance = new GridBagConstraints();
			gbc_lblLandDistance.anchor = GridBagConstraints.EAST;
			gbc_lblLandDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblLandDistance.gridx = 0;
			gbc_lblLandDistance.gridy = 12;
			contentPanel.add(lblLandDistance, gbc_lblLandDistance);
		}
		{
			landingTextField = new JTextField("" + validationTools.roundDouble(formationTools.getLandingFormationMinimumDistance(), 6));
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
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = 13;
			contentPanel.add(separator, gbc_separator);
		}
		JButton okButton = new JButton("OK");
		okButton.setForeground(Color.BLACK);
		GridBagConstraints gbc_okButton = new GridBagConstraints();
		gbc_okButton.insets = new Insets(0, 0, 0, 5);
		gbc_okButton.gridx = 2;
		gbc_okButton.gridy = 14;
		contentPanel.add(okButton, gbc_okButton);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(isValidConfiguration()) {
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					storeConfiguration();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							dispose();
						}
					});
				} else {
					gui.warn(Text.VALIDATION_WARNING, MUSCOPText.BAD_INPUT);
				}
			}
		});
		okButton.setActionCommand(Text.OK);
		getRootPane().setDefaultButton(okButton);
		
		this.setTitle(MUSCOPText.CONFIGURATION_DIALOG_TITLE_SWARM);
	}
	
	private boolean isValidConfiguration() {
		String validating = missionsTextField.getText();
		if (validationTools.isEmpty(validating)) {
			gui.warn(Text.VALIDATION_WARNING, Text.MISSIONS_ERROR_5);
			return false;
		}
		
		validating = groundTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(Text.VALIDATION_WARNING, MUSCOPText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		validating = flyingTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(Text.VALIDATION_WARNING, MUSCOPText.DISTANCE_TEXT_ERROR);
			return false;
		}
		
		validating = landingTextField.getText();
		if (!validationTools.isValidPositiveDouble(validating)) {
			gui.warn(Text.VALIDATION_WARNING, MUSCOPText.DISTANCE_TEXT_ERROR);
			return false;
		}
		return true;
	}
	
	private void storeConfiguration() {
		// The missions are stored automatically when pressing the button to select the file
		double ground = Double.parseDouble(groundTextField.getText());
		double flying = Double.parseDouble(flyingTextField.getText());
		double landing = Double.parseDouble(landingTextField.getText());
		formationTools.setGroundFormation((String)groundComboBox.getSelectedItem(), ground);
		takeOffHelper.setTakeOffAlgorithm((String)takeOffStrategyComboBox.getSelectedItem());
		formationTools.setFlyingFormation((String)airComboBox.getSelectedItem(), flying);
		formationTools.setLandingFormationMinimumDistance(landing);
	}

}