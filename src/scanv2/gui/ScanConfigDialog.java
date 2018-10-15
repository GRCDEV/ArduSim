package scanv2.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.javatuples.Pair;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;
import api.pojo.formations.FlightFormation;
import api.pojo.formations.FlightFormation.Formation;
import main.Text;
import scanv2.logic.ScanParam;
import scanv2.logic.ScanText;

public class ScanConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField missionsTextField;
	private JComboBox<String> groundComboBox;
	private JComboBox<String> airComboBox;
	private JTextField groundTextField;
	private JTextField flyingTextField;
	private JTextField landingTextField;

	/**
	 * Create the dialog.
	 */
	public ScanConfigDialog() {
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
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblMapaMisin = new JLabel(ScanText.MISSION_SELECT);
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
			JButton btnMap = new JButton(ScanText.BUTTON_SELECT);
			btnMap.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					final Pair<String, List<Waypoint>[]> missions = GUI.loadMissions();
					if (missions != null) {
						int numUAVs = Tools.getNumUAVs();
						/** The master is assigned the first mission in the list */
						List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
						missionsFinal[ScanParam.MASTER_POSITION] = missions.getValue1()[0];
						Tools.setLoadedMissionsFromFile(missionsFinal);
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
			JLabel groundFormationLabel = new JLabel(ScanText.GROUND_TEXT);
			GridBagConstraints gbc_groundFormationLabel = new GridBagConstraints();
			gbc_groundFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_groundFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationLabel.gridx = 0;
			gbc_groundFormationLabel.gridy = 2;
			contentPanel.add(groundFormationLabel, gbc_groundFormationLabel);
		}
		{
			JLabel groundFormationFormationLabel = new JLabel(ScanText.FORMATION_TEXT);
			groundFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationFormationLabel = new GridBagConstraints();
			gbc_groundFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationFormationLabel.gridx = 0;
			gbc_groundFormationFormationLabel.gridy = 3;
			contentPanel.add(groundFormationFormationLabel, gbc_groundFormationFormationLabel);
		}
		Formation[] formations = Formation.values();
		{
			groundComboBox = new JComboBox<String>();
			GridBagConstraints gbc_groundComboBox = new GridBagConstraints();
			gbc_groundComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_groundComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_groundComboBox.gridx = 1;
			gbc_groundComboBox.gridy = 3;
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
			JLabel groundFormationDistanceLabel = new JLabel(ScanText.DISTANCE_TEXT);
			groundFormationDistanceLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_groundFormationDistanceLabel = new GridBagConstraints();
			gbc_groundFormationDistanceLabel.anchor = GridBagConstraints.EAST;
			gbc_groundFormationDistanceLabel.insets = new Insets(0, 0, 5, 5);
			gbc_groundFormationDistanceLabel.gridx = 0;
			gbc_groundFormationDistanceLabel.gridy = 4;
			contentPanel.add(groundFormationDistanceLabel, gbc_groundFormationDistanceLabel);
		}
		{
			groundTextField = new JTextField("" + Tools.round(FlightFormation.getGroundFormationDistance(), 6));
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
			JLabel airFormationLabel = new JLabel(ScanText.AIR_TEXT);
			GridBagConstraints gbc_airFormationLabel = new GridBagConstraints();
			gbc_airFormationLabel.anchor = GridBagConstraints.WEST;
			gbc_airFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationLabel.gridx = 0;
			gbc_airFormationLabel.gridy = 6;
			contentPanel.add(airFormationLabel, gbc_airFormationLabel);
		}
		{
			JLabel airFormationFormationLabel = new JLabel(ScanText.FORMATION_TEXT);
			airFormationFormationLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_airFormationFormationLabel = new GridBagConstraints();
			gbc_airFormationFormationLabel.anchor = GridBagConstraints.EAST;
			gbc_airFormationFormationLabel.insets = new Insets(0, 0, 5, 5);
			gbc_airFormationFormationLabel.gridx = 0;
			gbc_airFormationFormationLabel.gridy = 7;
			contentPanel.add(airFormationFormationLabel, gbc_airFormationFormationLabel);
		}
		{
			airComboBox = new JComboBox<String>();
			GridBagConstraints gbc_airComboBox = new GridBagConstraints();
			gbc_airComboBox.insets = new Insets(0, 0, 5, 5);
			gbc_airComboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_airComboBox.gridx = 1;
			gbc_airComboBox.gridy = 7;
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
			JLabel lblFlightDistance = new JLabel(ScanText.DISTANCE_TEXT);
			lblFlightDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFlightDistance = new GridBagConstraints();
			gbc_lblFlightDistance.anchor = GridBagConstraints.EAST;
			gbc_lblFlightDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightDistance.gridx = 0;
			gbc_lblFlightDistance.gridy = 8;
			contentPanel.add(lblFlightDistance, gbc_lblFlightDistance);
		}
		{
			flyingTextField = new JTextField("" + Tools.round(FlightFormation.getFlyingFormationDistance(), 6));
			flyingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_flyingTextField = new GridBagConstraints();
			gbc_flyingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_flyingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_flyingTextField.gridx = 1;
			gbc_flyingTextField.gridy = 8;
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
			gbc_lblNewLabel_1.gridy = 8;
			contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			JLabel lblNewLabel_2 = new JLabel(ScanText.LANDING_TEXT);
			GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
			gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_2.gridx = 0;
			gbc_lblNewLabel_2.gridy = 10;
			contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		}
		{
			JLabel lblLandDistance = new JLabel(ScanText.DISTANCE_TEXT);
			lblLandDistance.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblLandDistance = new GridBagConstraints();
			gbc_lblLandDistance.anchor = GridBagConstraints.EAST;
			gbc_lblLandDistance.insets = new Insets(0, 0, 5, 5);
			gbc_lblLandDistance.gridx = 0;
			gbc_lblLandDistance.gridy = 11;
			contentPanel.add(lblLandDistance, gbc_lblLandDistance);
		}
		{
			landingTextField = new JTextField("" + Tools.round(FlightFormation.getLandingFormationDistance(), 6));
			landingTextField.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_landingTextField = new GridBagConstraints();
			gbc_landingTextField.insets = new Insets(0, 0, 5, 5);
			gbc_landingTextField.fill = GridBagConstraints.HORIZONTAL;
			gbc_landingTextField.gridx = 1;
			gbc_landingTextField.gridy = 11;
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
			gbc_lblNewLabel_3.gridy = 11;
			contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		}
		{
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = 12;
			contentPanel.add(separator, gbc_separator);
		}
		JButton okButton = new JButton("OK");
		okButton.setForeground(Color.BLACK);
		GridBagConstraints gbc_okButton = new GridBagConstraints();
		gbc_okButton.insets = new Insets(0, 0, 0, 5);
		gbc_okButton.gridx = 2;
		gbc_okButton.gridy = 13;
		contentPanel.add(okButton, gbc_okButton);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<Waypoint>[] missions = Tools.getLoadedMissions();
				if (missions == null) {
					JOptionPane.showMessageDialog(null, ScanText.BAD_INPUT);
					return;
				}
				int count = 0;
				for (int i = 0; i < missions.length; i++) {
					if (missions[i] != null) {
						count++;
					}
				}
				if (count == 0) {
					JOptionPane.showMessageDialog(null, ScanText.BAD_INPUT);
					return;
				}
				
				try {
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					double ground = Double.parseDouble(groundTextField.getText());
					double flying = Double.parseDouble(flyingTextField.getText());
					double landing = Double.parseDouble(landingTextField.getText());
					FlightFormation.setGroundFormation(Formation.getFormation((String)groundComboBox.getSelectedItem()));
					FlightFormation.setGroundFormationDistance(ground);
					FlightFormation.setFlyingFormation(Formation.getFormation((String)airComboBox.getSelectedItem()));
					FlightFormation.setFlyingFormationDistance(flying);
					FlightFormation.setLandingFormationDistance(landing);
					// State change
					Tools.setProtocolConfigured();
					
					dispose();
				} catch (NumberFormatException e2) {
					JOptionPane.showMessageDialog(null, ScanText.BAD_INPUT);
					return;
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
		
		GUI.addEscapeListener(this);
		
		this.setTitle(ScanText.CONFIGURATION_DIALOG_TITLE_SWARM);
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);


	}

}
