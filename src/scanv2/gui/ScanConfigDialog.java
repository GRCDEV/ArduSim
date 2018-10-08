package scanv2.gui;

import java.awt.Color;
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
import javax.swing.JDialog;
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
import javax.swing.text.NumberFormatter;

import org.javatuples.Pair;

import api.GUI;
import api.Tools;
import api.pojo.Waypoint;
import api.pojo.formations.FlightFormation.Formation;
import main.Text;
import scanv2.logic.ScanParam;
import scanv2.logic.ScanText;
import uavController.UAVParam;

import java.awt.Font;
import javax.swing.JComboBox;

public class ScanConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField missionsTextField;
	private JComboBox<String> groundComboBox;
	private JSpinner spinnerGround;
	private JComboBox<String> airComboBox;
	private JSpinner spinnerFlight;

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
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
				for (int i = 0; i < formations.length; i++) {
					groundComboBox.addItem(formations[i].getName());
					if (UAVParam.groundFormation.get().getName().equalsIgnoreCase(formations[i].getName())) {
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
			SpinnerNumberModel model1 = new SpinnerNumberModel(UAVParam.groundDistanceBetweenUAV, 1, 100, 1);  
			spinnerGround = new JSpinner(model1);
			JFormattedTextField txt = ((JSpinner.NumberEditor) spinnerGround.getEditor()).getTextField();
			((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
			GridBagConstraints gbc_spinnerGround = new GridBagConstraints();
			gbc_spinnerGround.anchor = GridBagConstraints.EAST;
			gbc_spinnerGround.insets = new Insets(0, 0, 5, 5);
			gbc_spinnerGround.gridx = 1;
			gbc_spinnerGround.gridy = 4;
			contentPanel.add(spinnerGround, gbc_spinnerGround);
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
				for (int i = 0; i < formations.length; i++) {
					airComboBox.addItem(formations[i].getName());
					if (UAVParam.airFormation.get().getName().equalsIgnoreCase(formations[i].getName())) {
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
			SpinnerNumberModel model2 = new SpinnerNumberModel(UAVParam.airDistanceBetweenUAV, 5, 100, 1); 
			spinnerFlight = new JSpinner(model2);
			JFormattedTextField txt = ((JSpinner.NumberEditor) spinnerFlight.getEditor()).getTextField();
			((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
			GridBagConstraints gbc_spinnerFlight = new GridBagConstraints();
			gbc_spinnerFlight.anchor = GridBagConstraints.EAST;
			gbc_spinnerFlight.insets = new Insets(0, 0, 5, 5);
			gbc_spinnerFlight.gridx = 1;
			gbc_spinnerFlight.gridy = 8;
			contentPanel.add(spinnerFlight, gbc_spinnerFlight);
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
			JSeparator separator = new JSeparator();
			GridBagConstraints gbc_separator = new GridBagConstraints();
			gbc_separator.fill = GridBagConstraints.HORIZONTAL;
			gbc_separator.gridwidth = 4;
			gbc_separator.insets = new Insets(0, 0, 5, 0);
			gbc_separator.gridx = 0;
			gbc_separator.gridy = 10;
			contentPanel.add(separator, gbc_separator);
		}
		JButton okButton = new JButton("OK");
		okButton.setForeground(Color.BLACK);
		GridBagConstraints gbc_okButton = new GridBagConstraints();
		gbc_okButton.insets = new Insets(0, 0, 0, 5);
		gbc_okButton.gridx = 2;
		gbc_okButton.gridy = 11;
		contentPanel.add(okButton, gbc_okButton);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(Tools.getLoadedMissions() != null ) {
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					UAVParam.groundFormation.set(Formation.getFormation((String)groundComboBox.getSelectedItem()));
					UAVParam.groundDistanceBetweenUAV = (Integer)spinnerGround.getValue();
					UAVParam.airFormation.set(Formation.getFormation((String)airComboBox.getSelectedItem()));
					UAVParam.airDistanceBetweenUAV = (Integer)spinnerFlight.getValue();
					// State change
					Tools.setProtocolConfigured();
					
					dispose();
				}else {
					JOptionPane.showMessageDialog(null, ScanText.BAD_INPUT);
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
		
		this.setTitle(ScanText.CONFIGURATION_DIALOG_TITLE_SWARM);
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);


	}

}
