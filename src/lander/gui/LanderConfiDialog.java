package lander.gui;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.TitledBorder;

import api.GUI;
import api.Tools;
//import api.Tools;
import api.pojo.GeoCoordinates;
import lander.logic.LanderParam;


public class LanderConfiDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	/**
	 * Launch the application.
	 */
	
	private JTextField txtLatStart;
	private JTextField txtLatEnd;
	private JTextField txtLongStart;
	private JTextField txtLongEnd;
	private JTextField txtDistMax;
	private JTextField txtAltitude;
	private JTextField txtDataFilePath;
	
	/*public static void main(String[] args) {
		try {
			LanderConfiDialog dialog = new LanderConfiDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Create the dialog.
	 */
	public LanderConfiDialog() {
		
		this.setModal(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		setBounds(100, 100, 516, 300);
		getContentPane().setLayout(null);
		JPanel cpLocation = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) cpLocation.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		cpLocation.setBorder(new TitledBorder(null, "Location", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
		cpLocation.setBounds(12, 12, 475, 80);
		getContentPane().add(cpLocation);
		
		JLabel lblLatStart = new JLabel("Start latitude   ");
		cpLocation.add(lblLatStart);
		
		txtLatStart = new JTextField("39.481554",10);
		cpLocation.add(txtLatStart);
		
		JLabel lblLatEnd = new JLabel("End latitude   ");
		cpLocation.add(lblLatEnd);
		
		txtLatEnd = new JTextField("39.479019",10);
		cpLocation.add(txtLatEnd);
		
		JLabel lblLongStart = new JLabel("Start longitude");
		cpLocation.add(lblLongStart);
		
		txtLongStart = new JTextField("-0.351266",10);
		cpLocation.add(txtLongStart);
		
		JLabel lblLongEnd = new JLabel("End longitude");
		cpLocation.add(lblLongEnd);
		
		txtLongEnd = new JTextField("-0.342125",10);
		cpLocation.add(txtLongEnd);
		
		JPanel cpRecognition = new JPanel();
		cpRecognition.setBorder(new TitledBorder(null, "Landing recognition", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		cpRecognition.setBounds(12, 104, 475, 80);
		getContentPane().add(cpRecognition);
		cpRecognition.setLayout(null);
		
		JLabel lblDistanceMax = new JLabel("Distance max.  (m) ");
		lblDistanceMax.setBounds(10, 24, 146, 15);
		lblDistanceMax.setVerticalAlignment(SwingConstants.TOP);
		cpRecognition.add(lblDistanceMax);
		
		txtDistMax = new JTextField("100",10);
		txtDistMax.setBounds(153, 22, 83, 19);
		cpRecognition.add(txtDistMax);
		
		JLabel lblOther = new JLabel("Height about ground");
		lblOther.setBounds(241, 24, 148, 15);
		cpRecognition.add(lblOther);
		
		txtAltitude = new JTextField("10",2);
		txtAltitude.setBounds(407, 22, 49, 19);
		cpRecognition.add(txtAltitude);
		
		txtDataFilePath = new JTextField();
		txtDataFilePath.setEditable(false);
		txtDataFilePath.setBounds(122, 53, 334, 19);
		txtDataFilePath.setEnabled(false);
		
		cpRecognition.add(txtDataFilePath);
		
		JButton btnRecognitionData = new JButton("...");
		btnRecognitionData.setBounds(10, 48, 49, 25);
		btnRecognitionData.setHorizontalAlignment(SwingConstants.RIGHT);
		cpRecognition.add(btnRecognitionData);
		
		
		btnRecognitionData.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				//chooser.setCurrentDirectory(Tools.getCurrentFolder());
				chooser.setDialogTitle("Select pollution data file"); // TODO Add text helper file
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);
				chooser.setAcceptAllFileFilterUsed(false);
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					txtDataFilePath.setText(chooser.getSelectedFile().getPath());
				}
				
			}
		});
		
		JPanel cpButton = new JPanel();
		cpButton.setBounds(22, 204, 462, 35);
		getContentPane().add(cpButton);
		cpButton.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton BtnOk = new JButton("OK");
		BtnOk.setActionCommand("OK");
		cpButton.add(BtnOk);
		
		BtnOk.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
			
		});

		
		
		JButton BtnCancel = new JButton("Cancel");
		BtnCancel.setActionCommand("Cancel");
		cpButton.add(BtnCancel);
		
		
		BtnCancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				CancelAction();
			}
			
		});

		
		
		
		
	}
	
	private void okAction() {
		try {
			
			LanderParam.LocationStart = new GeoCoordinates(Double.parseDouble(txtLatStart.getText()), 
					Double.parseDouble(txtLongStart.getText()));
			LanderParam.LocationEnd = new GeoCoordinates(Double.parseDouble(txtLatEnd.getText()), 
					Double.parseDouble(txtLongEnd.getText()));
			
			
			//LanderParam.distMax = Integer.parseInt(txtDistMax.getText());
			//LanderParam.altitude = Double.parseDouble(altitudeField.getText());
			LanderParam.distMax = Double.parseDouble(txtDistMax.getText());
			
			LanderParam.altitude = Integer.parseInt(txtAltitude.getText());
			
			LanderParam.LanderDataFile = txtDataFilePath.getText();
			
			//LanderParam.isSimulation = isSimulationCheckBox.isSelected();
			
			Tools.setProtocolConfigured();
			
			GUI.log("Coordenada final :"+ LanderParam.LocationEnd.toString());
			
			
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Longitude and latitude must be in decimal format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void CancelAction() {
		try {
						
			
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Longitude and latitude must be in decimal format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}
}