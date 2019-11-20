package fishing.gui;


import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.javatuples.Pair;
import java.util.List;

import api.API;
import api.pojo.location.Waypoint;
import fishing.logic.FishingParam;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;



public class FishingConfigDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private JTextField txtAngleDegrees;
	private JTextField txtRadiusMeters;
	private JTextField txtAltitude;
	private JCheckBox chkClockwise;
	private JTextField txtMissionFilePath;
	private Pair<String, List<Waypoint>[]> fitxData = null;
	
	public FishingConfigDialog () {
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblFishersConfiguration = new JLabel("Fishers configuration");
			GridBagConstraints gbc_lblFishersConfiguration = new GridBagConstraints();
			gbc_lblFishersConfiguration.anchor = GridBagConstraints.WEST;
			gbc_lblFishersConfiguration.insets = new Insets(0, 0, 5, 5);
			gbc_lblFishersConfiguration.gridx = 0;
			gbc_lblFishersConfiguration.gridy = 0;
			contentPanel.add(lblFishersConfiguration, gbc_lblFishersConfiguration);
		}
		{
			JLabel lblTrajectoryAngle = new JLabel("Trajectory angle:");
			lblTrajectoryAngle.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblTrajectoryAngle = new GridBagConstraints();
			gbc_lblTrajectoryAngle.anchor = GridBagConstraints.EAST;
			gbc_lblTrajectoryAngle.insets = new Insets(0, 0, 5, 5);
			gbc_lblTrajectoryAngle.gridx = 0;
			gbc_lblTrajectoryAngle.gridy = 1;
			contentPanel.add(lblTrajectoryAngle, gbc_lblTrajectoryAngle);
		}
		{
			txtAngleDegrees = new JTextField();
			txtAngleDegrees.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_txtAngleDegrees = new GridBagConstraints();
			gbc_txtAngleDegrees.insets = new Insets(0, 0, 5, 5);
			gbc_txtAngleDegrees.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtAngleDegrees.gridx = 1;
			gbc_txtAngleDegrees.gridy = 1;
			contentPanel.add(txtAngleDegrees, gbc_txtAngleDegrees);
			txtAngleDegrees.setColumns(10);
		}
		{
			JLabel lblDistanceToShip = new JLabel("Distance to ship:");
			lblDistanceToShip.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblDistanceToShip = new GridBagConstraints();
			gbc_lblDistanceToShip.anchor = GridBagConstraints.EAST;
			gbc_lblDistanceToShip.insets = new Insets(0, 0, 5, 5);
			gbc_lblDistanceToShip.gridx = 0;
			gbc_lblDistanceToShip.gridy = 2;
			contentPanel.add(lblDistanceToShip, gbc_lblDistanceToShip);
		}
		{
			txtRadiusMeters = new JTextField();
			txtRadiusMeters.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_txtRadiusMeters = new GridBagConstraints();
			gbc_txtRadiusMeters.insets = new Insets(0, 0, 5, 5);
			gbc_txtRadiusMeters.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtRadiusMeters.gridx = 1;
			gbc_txtRadiusMeters.gridy = 2;
			contentPanel.add(txtRadiusMeters, gbc_txtRadiusMeters);
			txtRadiusMeters.setColumns(10);
		}
		{
			JLabel lblFlightAltitude = new JLabel("Flight altitude:");
			lblFlightAltitude.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblFlightAltitude = new GridBagConstraints();
			gbc_lblFlightAltitude.anchor = GridBagConstraints.EAST;
			gbc_lblFlightAltitude.insets = new Insets(0, 0, 5, 5);
			gbc_lblFlightAltitude.gridx = 0;
			gbc_lblFlightAltitude.gridy = 3;
			contentPanel.add(lblFlightAltitude, gbc_lblFlightAltitude);
		}
		{
			txtAltitude = new JTextField();
			txtAltitude.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_txtAltitude = new GridBagConstraints();
			gbc_txtAltitude.insets = new Insets(0, 0, 5, 5);
			gbc_txtAltitude.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtAltitude.gridx = 1;
			gbc_txtAltitude.gridy = 3;
			contentPanel.add(txtAltitude, gbc_txtAltitude);
			txtAltitude.setColumns(10);
		}
		{
			JLabel lblClockwise = new JLabel("Clockwise:");
			lblClockwise.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblClockwise = new GridBagConstraints();
			gbc_lblClockwise.anchor = GridBagConstraints.EAST;
			gbc_lblClockwise.insets = new Insets(0, 0, 5, 5);
			gbc_lblClockwise.gridx = 0;
			gbc_lblClockwise.gridy = 4;
			contentPanel.add(lblClockwise, gbc_lblClockwise);
		}
		{
			chkClockwise = new JCheckBox("");
			GridBagConstraints gbc_chkClockwise = new GridBagConstraints();
			gbc_chkClockwise.anchor = GridBagConstraints.WEST;
			gbc_chkClockwise.insets = new Insets(0, 0, 5, 5);
			gbc_chkClockwise.gridx = 1;
			gbc_chkClockwise.gridy = 4;
			contentPanel.add(chkClockwise, gbc_chkClockwise);
		}
		{
			JLabel lblBoatConfiguration = new JLabel("Boat configuration");
			GridBagConstraints gbc_lblBoatConfiguration = new GridBagConstraints();
			gbc_lblBoatConfiguration.anchor = GridBagConstraints.WEST;
			gbc_lblBoatConfiguration.insets = new Insets(0, 0, 5, 5);
			gbc_lblBoatConfiguration.gridx = 0;
			gbc_lblBoatConfiguration.gridy = 5;
			contentPanel.add(lblBoatConfiguration, gbc_lblBoatConfiguration);
		}
		{
			JLabel lblShipMission = new JLabel("Ship mission:");
			lblShipMission.setFont(new Font("Dialog", Font.PLAIN, 12));
			GridBagConstraints gbc_lblShipMission = new GridBagConstraints();
			gbc_lblShipMission.anchor = GridBagConstraints.EAST;
			gbc_lblShipMission.insets = new Insets(0, 0, 0, 5);
			gbc_lblShipMission.gridx = 0;
			gbc_lblShipMission.gridy = 6;
			contentPanel.add(lblShipMission, gbc_lblShipMission);
		}
		{
			txtMissionFilePath = new JTextField();
			txtMissionFilePath.setEditable(false);
			txtMissionFilePath.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_txtMissionFilePath = new GridBagConstraints();
			gbc_txtMissionFilePath.insets = new Insets(0, 0, 0, 5);
			gbc_txtMissionFilePath.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtMissionFilePath.gridx = 1;
			gbc_txtMissionFilePath.gridy = 6;
			contentPanel.add(txtMissionFilePath, gbc_txtMissionFilePath);
			txtMissionFilePath.setColumns(10);
		}
		{
			JButton btnBoatMision = new JButton("...");
			btnBoatMision.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					fitxData = API.getGUI(0).loadMissionsKML();
					txtMissionFilePath.setText(fitxData.getValue0());
				}
			});
			GridBagConstraints gbc_btnBoatMision = new GridBagConstraints();
			gbc_btnBoatMision.insets = new Insets(0, 0, 0, 5);
			gbc_btnBoatMision.gridx = 2;
			gbc_btnBoatMision.gridy = 6;
			contentPanel.add(btnBoatMision, gbc_btnBoatMision);
		}
		
//		final GUI gui = API.getGUI(0);
//	
//		int Panelwith=600;
//		int Panelheitgth=600;
//		
//		
//		this.setTitle(API.getArduSim().getSelectedProtocolName() +" configuration");
//		Point screenCenter = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
////		this.setBounds(screenCenter.x-(Panelwith/2), screenCenter.y-(Panelheitgth/2), Panelwith, Panelheitgth);
//		gui.log("Posicion ventana :" + screenCenter.x + ", " + screenCenter.y);	
//		this.getContentPane().setLayout(new GridLayout(3,1));
//		
//		JPanel cpFishers = new JPanel();
//		cpFishers.setBorder(new TitledBorder(null, "Fishers configuration", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
//		cpFishers.setLayout(null);
//		this.getContentPane().add(cpFishers);
//		
//		lblAngle = new JLabel("Angulo trayectoria (degrees): ");
////		lblAngle.setBounds(15, 23, 210, 20);
//		cpFishers.add(lblAngle);
//		
//		txtAngleDegrees = new JTextField(null,3);
//		txtAngleDegrees.setHorizontalAlignment(SwingConstants.RIGHT);
////		txtAngleDegrees.setBounds(220, 24, 50, 20);
//		cpFishers.add(txtAngleDegrees);
//		
//		lblRadius = new JLabel("Distancia al barco (metros): ");
////		lblRadius.setBounds(15, 60, 210, 20);
//		cpFishers.add(lblRadius);
//		
//		txtRadiusMeters = new JTextField(null,10);
//		txtRadiusMeters.setHorizontalAlignment(SwingConstants.RIGHT);
////		txtRadiusMeters.setBounds(220, 61, 50, 20);
//		cpFishers.add(txtRadiusMeters);
//		
//		lbAltitude = new JLabel("Altitud del dron (metros): ");
////		lbAltitude.setBounds(15, 98, 210, 20);
//		cpFishers.add(lbAltitude);
//		
//		txtAltitude = new JTextField(null,4);
//		txtAltitude.setHorizontalAlignment(SwingConstants.RIGHT);
////		txtAltitude.setBounds(220, 99, 50, 20);
//		cpFishers.add(txtAltitude);
//		
//		chkClockwise = new JCheckBox("Sentido horario");
////		chkClockwise.setBounds(320,50,150, 20);
//		cpFishers.add(chkClockwise);
//		
//		JPanel cpBoat = new JPanel();
//		cpBoat.setBorder(new TitledBorder(null, "Boat configuration", TitledBorder.LEFT, TitledBorder.TOP, null, null));
//		FlowLayout flowLayout_1 = (FlowLayout) cpBoat.getLayout();
//		flowLayout_1.setAlignment(FlowLayout.LEFT);
//		this.getContentPane().add(cpBoat);
//		
//		
//		lblPathBoat = new JLabel("Fichero misión para el barco: ");
////		lblPathBoat.setBounds(0, 130, 210, 20);
//		cpBoat.add(lblPathBoat);
//		
//		txtMisionFilePath = new JTextField("");
//		txtMisionFilePath.setEditable(true);
////		txtMisionFilePath.setBounds(0, 0, 334, 20);
//		txtMisionFilePath.setEnabled(true);
//		cpBoat.add(txtMisionFilePath);
//		
//		JButton btnBoatMision = new JButton("...");
//		btnBoatMision.setSize(30,20);
//		cpBoat.add(btnBoatMision);
//		
//		
//		btnBoatMision.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				
//				fitxData=gui.loadMissionsKML();
//				txtMisionFilePath.setText(fitxData.getValue0());
//			}
//			
//			
//		});
//		
//		
//		JPanel cpButton = new JPanel();
////		cpButton.setBounds(22, 204, 462, 35);
//		this.getContentPane().add(cpButton);
//		cpButton.setLayout(new FlowLayout(FlowLayout.RIGHT));
//		
//		JButton btnOk = new JButton("OK");
//		btnOk.setActionCommand("OK");
//		cpButton.add(btnOk);
//		
//		
//		btnOk.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				okAction();
//			}
//			
//		});
//		
//		
//		JButton btnCancel = new JButton("Cancel");
//		btnCancel.setActionCommand("Cancel");
//		cpButton.add(btnCancel);
//		
//		btnCancel.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				CancelAction();
//			}
//			
//		});
//		this.setResizable(true);
//		this.pack();
		
	    {
	      JPanel buttonPane = new JPanel();
	      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	      getContentPane().add(buttonPane, BorderLayout.SOUTH);
	      {
	        JButton okButton = new JButton("OK");
	        okButton.addActionListener(new ActionListener() {
	          public void actionPerformed(ActionEvent e) {
	        	  okAction();
	          }
	        });
	        buttonPane.add(okButton);
	        getRootPane().setDefaultButton(okButton);
	      }
	    }
	    
	    this.setTitle(API.getArduSim().getSelectedProtocolName() + " configuration");
	}
	
	@SuppressWarnings("unchecked")
	private void okAction() {
		
		try {
			int numUAVs = 2;
			API.getArduSim().setNumUAVs(numUAVs);
			FishingParam.rotationAngle = Double.parseDouble(txtAngleDegrees.getText());
			FishingParam.radius = Double.parseDouble(txtRadiusMeters.getText());
			FishingParam.UavAltitude = Double.parseDouble(txtAltitude.getText());
			FishingParam.clockwise = chkClockwise.isSelected();
			//Creamos un vector del tamaño igual al numero de UAVs para evitar problemas en la función sendInitialConfiguration principal.
			FishingParam.boatMission = (List<Waypoint>[]) new List[numUAVs];
			FishingParam.boatMission[0] = fitxData.getValue1()[0];
			FishingParam.distanceTreshold = 0.1*FishingParam.radius;
			API.getCopter(0).getMissionHelper().setMissionsLoaded(FishingParam.boatMission);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					dispose();
				}
			});
			
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Angle, altitude and distance must be a number.", "Format error", JOptionPane.ERROR_MESSAGE);
		} catch (NullPointerException e) {
			JOptionPane.showMessageDialog(this, "A file mission for Boat must be selected.", "Mission File", JOptionPane.ERROR_MESSAGE);
		}	
		
	}

}
