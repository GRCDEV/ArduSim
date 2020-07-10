package muscop.gui;

import api.API;
import api.pojo.location.Waypoint;
import main.Text;
import main.api.*;
import main.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;
import muscop.logic.MUSCOPText;
import muscop.logic.MuscopSimProperties;
import org.javatuples.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
	private JComboBox<Integer> nrOfClustersComboBox;
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
		int gridy = 0;
		{
			JLabel lblMapaMisin = new JLabel(MUSCOPText.MISSION_SELECT);
			GridBagConstraints gbc_lblMapaMisin = new GridBagConstraints();
			gbc_lblMapaMisin.anchor = GridBagConstraints.WEST;
			gbc_lblMapaMisin.fill = GridBagConstraints.VERTICAL;
			gbc_lblMapaMisin.insets = new Insets(0, 0, 5, 5);
			gbc_lblMapaMisin.gridx = 0;
			gbc_lblMapaMisin.gridy = gridy;
			contentPanel.add(lblMapaMisin, gbc_lblMapaMisin);
		}
		{
			missionsTextField = new JTextField();
			missionsTextField.setEditable(false);
			GridBagConstraints gbc_missionsTextField = new GridBagConstraints();
			gbc_missionsTextField.insets = new Insets(0, 0, 5, 5);
			gbc_missionsTextField.fill = GridBagConstraints.BOTH;
			gbc_missionsTextField.gridx = 1;
			gbc_missionsTextField.gridy = gridy;
			contentPanel.add(missionsTextField, gbc_missionsTextField);
			missionsTextField.setColumns(10);
		}
		{
			JButton btnMap = new JButton(MUSCOPText.BUTTON_SELECT);
			btnMap.addActionListener(e -> {
				final Pair<String, List<Waypoint>[]> missions = gui.loadMissions(gui.searchMissionFiles());
				MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
				if (missions == null) {
					missionHelper.setMissionsLoaded(null);
					SwingUtilities.invokeLater(() -> missionsTextField.setText(""));
				} else {
					int numUAVs = API.getArduSim().getNumUAVs();
					/* The master is assigned the first mission in the list */
					List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
					// The master UAV is always in the position 0 of arrays
					missionsFinal[0] = missions.getValue1()[0];
					missionHelper.setMissionsLoaded(missionsFinal);
					SwingUtilities.invokeLater(() -> missionsTextField.setText(missions.getValue0()));
				}
			});
			GridBagConstraints gbc_btnMap = new GridBagConstraints();
			gbc_btnMap.insets = new Insets(0, 0, 5, 5);
			gbc_btnMap.gridx = 2;
			gbc_btnMap.gridy = gridy;
			contentPanel.add(btnMap, gbc_btnMap);
		}
		gridy++;
		{
			JLabel groundFormationLabel = new JLabel(MUSCOPText.GROUND_TEXT);
			GridBagConstraints gbc_groundFormationLabel = new GridBagConstraints();
			gbc_groundFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_groundFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationLabel.gridx = 0;
			gbc_groundFormationLabel.gridy = gridy;
			contentPanel.add(groundFormationLabel, gbc_groundFormationLabel);
		}
		gridy++;
		{
			JLabel groundFormationFormationLabel = new JLabel(MUSCOPText.FORMATION_TEXT);
			groundFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationFormationLabel = new GridBagConstraints();
			gbc_groundFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationFormationLabel.gridx = 0;
			gbc_groundFormationFormationLabel.gridy = gridy;
			contentPanel.add(groundFormationFormationLabel, gbc_groundFormationFormationLabel);
		}
		String[] formations = formationTools.getAvailableFormations();
		{
			groundComboBox = new JComboBox<>();
			GridBagConstraints gbc_groundComboBox = new GridBagConstraints();
			gbc_groundComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_groundComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundComboBox.gridx = 1;
			gbc_groundComboBox.gridy = gridy;
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
		gridy++;
		{
			JLabel groundFormationDistanceLabel = new JLabel(MUSCOPText.DISTANCE_TEXT);
			groundFormationDistanceLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationDistanceLabel = new GridBagConstraints();
			gbc_groundFormationDistanceLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationDistanceLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationDistanceLabel.gridx = 0;
			gbc_groundFormationDistanceLabel.gridy = gridy;
			contentPanel.add(groundFormationDistanceLabel, gbc_groundFormationDistanceLabel);
		}
		{
			// input min distance uav
			groundTextField = new JTextField("" + validationTools.roundDouble(formationTools.getGroundFormationMinimumDistance(), 6));
			groundTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_groundTextField = new GridBagConstraints();
			gbc_groundTextField.insets = new Insets(0, 0, 5, 5);
			gbc_groundTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundTextField.gridx = 1;
			gbc_groundTextField.gridy = gridy;
			contentPanel.add(groundTextField, gbc_groundTextField);
			groundTextField.setColumns(10);
		}
		{
			// text m
			JLabel lblNewLabel = new JLabel(Text.METERS);
			lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 2;
			gbc_lblNewLabel.gridy = gridy;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		gridy++;
		{
			JLabel clusters = new JLabel(MUSCOPText.CLUSTERS);
			clusters.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_clusters = new GridBagConstraints();
			gbc_clusters.anchor = GridBagConstraints.EAST;
			gbc_clusters.insets = new Insets(0, 0, 5, 5);
			gbc_clusters.gridx = 0;
			gbc_clusters.gridy = gridy;
			contentPanel.add(clusters, gbc_clusters);
		}
		{
			nrOfClustersComboBox = new JComboBox<>();
			GridBagConstraints gbc_nrOfClustersComboBox = new GridBagConstraints();
			gbc_nrOfClustersComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_nrOfClustersComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_nrOfClustersComboBox.gridx = 1;
			gbc_nrOfClustersComboBox.gridy = gridy;
			for (int i = 1; i <=4 ; i++) {
				nrOfClustersComboBox.addItem(i);
			}
			nrOfClustersComboBox.setSelectedIndex(0);
			contentPanel.add(nrOfClustersComboBox, gbc_nrOfClustersComboBox);
		}
		gridy++;
		{
			// text take off strategy
			JLabel lblDfg = new JLabel(MUSCOPText.TAKEOFF_STRATEGY);
			lblDfg.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblDfg = new GridBagConstraints();
			gbc_lblDfg.anchor = GridBagConstraints.EAST;
			gbc_lblDfg.insets = new Insets(0, 0, 5, 5);
			gbc_lblDfg.gridx = 0;
			gbc_lblDfg.gridy = gridy;
			contentPanel.add(lblDfg, gbc_lblDfg);
		}
		{
			// take off strategy input
			takeOffStrategyComboBox = new JComboBox<>();
			GridBagConstraints gbc_takeOffStrategyComboBox = new GridBagConstraints();
			gbc_takeOffStrategyComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_takeOffStrategyComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_takeOffStrategyComboBox.gridx = 1;
			gbc_takeOffStrategyComboBox.gridy = gridy;
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
			
			for(int i = 0; i<= algorithms.length; i++) {
				if(algorithms[i].equals(TakeOffAlgorithm.SIMPLIFIED.getName())) {
					takeOffStrategyComboBox.setSelectedIndex(i);
					break;
				}
			}
			contentPanel.add(takeOffStrategyComboBox, gbc_takeOffStrategyComboBox);
		}
		gridy+=2;
		{
			//text flying formation
			JLabel airFormationLabel = new JLabel(MUSCOPText.AIR_TEXT);
			GridBagConstraints gbc_airFormationLabel = new GridBagConstraints();
			gbc_airFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_airFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationLabel.gridx = 0;
			gbc_airFormationLabel.gridy = gridy;
			contentPanel.add(airFormationLabel, gbc_airFormationLabel);
		}
		gridy++;
		{
			// text formation
			JLabel airFormationFormationLabel = new JLabel(MUSCOPText.FORMATION_TEXT);
			airFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_airFormationFormationLabel = new GridBagConstraints();
			gbc_airFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_airFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationFormationLabel.gridx = 0;
			gbc_airFormationFormationLabel.gridy = gridy;
			contentPanel.add(airFormationFormationLabel, gbc_airFormationFormationLabel);
		}
		{
			// flyFormation input
			airComboBox = new JComboBox<>();
			GridBagConstraints gbc_airComboBox = new GridBagConstraints();
			gbc_airComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_airComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_airComboBox.gridx = 1;
			gbc_airComboBox.gridy = gridy;
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
		gridy++;
		{
			// text min distance UAV
			JLabel lblFlightDistance = new JLabel(MUSCOPText.DISTANCE_TEXT);
			lblFlightDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFlightDistance = new GridBagConstraints();
			gbc_lblFlightDistance.anchor = GridBagConstraints.EAST;
			gbc_lblFlightDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightDistance.gridx = 0;
			gbc_lblFlightDistance.gridy = gridy;
			contentPanel.add(lblFlightDistance, gbc_lblFlightDistance);
		}
		{
			// min distance UAV input
			flyingTextField = new JTextField("" + validationTools.roundDouble(formationTools.getFlyingFormationMinimumDistance(), 6));
			flyingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_flyingTextField = new GridBagConstraints();
			gbc_flyingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_flyingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_flyingTextField.gridx = 1;
			gbc_flyingTextField.gridy = gridy;
			contentPanel.add(flyingTextField, gbc_flyingTextField);
			flyingTextField.setColumns(10);
		}
		{
			// text m
			JLabel lblNewLabel_1 = new JLabel(Text.METERS);
			lblNewLabel_1.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 2;
			gbc_lblNewLabel_1.gridy = gridy;
			contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		gridy+=2;
		{
			// text landing
			JLabel lblNewLabel_2 = new JLabel(MUSCOPText.LANDING_TEXT);
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_2.gridx = 0;
			gbc_lblNewLabel_2.gridy = gridy;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		gridy++;
		{
			// text distance
			JLabel lblLandDistance = new JLabel(MUSCOPText.DISTANCE_TEXT);
			lblLandDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblLandDistance = new GridBagConstraints();
			gbc_lblLandDistance.anchor = GridBagConstraints.EAST;
			gbc_lblLandDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblLandDistance.gridx = 0;
			gbc_lblLandDistance.gridy = gridy;
			contentPanel.add(lblLandDistance, gbc_lblLandDistance);
		}
		{
			// Landing distance field
			landingTextField = new JTextField("" + validationTools.roundDouble(formationTools.getLandingFormationMinimumDistance(), 6));
			landingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_landingTextField = new GridBagConstraints();
			gbc_landingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_landingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_landingTextField.gridx = 1;
			gbc_landingTextField.gridy = gridy;
			contentPanel.add(landingTextField, gbc_landingTextField);
			landingTextField.setColumns(10);
		}
		{
			// Text m
			JLabel lblNewLabel_3 = new JLabel(Text.METERS);
			lblNewLabel_3.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
			gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_3.gridx = 2;
			gbc_lblNewLabel_3.gridy = gridy;
			contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		}
		gridy++;
		{
			// LINE separator
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = gridy;
			contentPanel.add(separator, gbc_separator);
		}
		gridy++;
		{
			// OK button
			JButton okButton = new JButton("OK");
			okButton.setForeground(Color.BLACK);
			GridBagConstraints gbc_okButton = new GridBagConstraints();
			gbc_okButton.insets = new Insets(0, 0, 0, 5);
			gbc_okButton.gridx = 2;
			gbc_okButton.gridy = gridy;
			contentPanel.add(okButton, gbc_okButton);
			okButton.addActionListener(e -> {

				if(isValidConfiguration()) {
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					storeConfiguration();
					SwingUtilities.invokeLater(this::dispose);
				} else {
					gui.warn(Text.VALIDATION_WARNING, MUSCOPText.BAD_INPUT);
				}
			});
			okButton.setActionCommand(Text.OK);
			getRootPane().setDefaultButton(okButton);
		}
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
		MuscopSimProperties.numberOfClusters = (int)nrOfClustersComboBox.getSelectedItem();
	}

}