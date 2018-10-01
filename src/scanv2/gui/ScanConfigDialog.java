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
import scanv2.logic.ScanParam;
import scanv2.logic.ScanText;

public class ScanConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField missionsTextField;
	JSpinner spinnerGround;
	JSpinner spinnerFlight;

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
					// In this protocol, the number of UAVs running on this machine (n) is not affected by the number of missions loaded (1)
					//   , so the function Tools.setNumUAVs() is not used
					Double groud = (Double) spinnerGround.getValue();
					Double air = (Double) spinnerFlight.getValue();
					ScanParam.initialDistanceBetweenUAV = groud.intValue();
					ScanParam.initialDistanceBetweenUAVreal = air.intValue();
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