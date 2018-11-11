package uavFishing.gui;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
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
import javax.swing.border.TitledBorder;

import org.javatuples.Pair;
import java.util.List;

import api.GUI;
import api.pojo.Waypoint;
import api.ProtocolHelper;
import api.Tools;
import uavFishing.logic.UavFishingParam;



public class UavFishingConfigDialog extends JDialog{
	
	private static final long serialVersionUID = 1L;
	private JLabel lblAngle,lblRadius,lbAltitude,lblPathBoat;
	private JTextField txtAngleDegrees,txtRadiusMeters,txtAltitude,txtMisionFilePath;
	private JCheckBox chkClockwise;
	private Pair<String, List<Waypoint>[]> fitxData;
	
	/*
	public static void main(String[] args) {
		try {
			UavFishingConfigDialog dialog = new UavFishingConfigDialog();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
	
	public UavFishingConfigDialog () {
		
	
		int Panelwith=516;
		int Panelheitgth=420;
		setTitle(ProtocolHelper.selectedProtocol +" configuration");
		Point screenCenter = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		this.setBounds(screenCenter.x-(Panelwith/2), screenCenter.y-(Panelheitgth/2), Panelwith, Panelheitgth);
		GUI.log("Posicion ventana :" + screenCenter.x + ", " + screenCenter.y);
		this.setModal(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);		
		getContentPane().setLayout(new GridLayout(3,1));
		
		JPanel cpFishers = new JPanel();
		cpFishers.setBorder(new TitledBorder(null, "Fishers configuration", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
		cpFishers.setLayout(null);
		getContentPane().add(cpFishers);
		
		lblAngle = new JLabel("Angulo trayectoria (degrees): ");
		lblAngle.setBounds(15, 23, 210, 20);
		cpFishers.add(lblAngle);
		
		txtAngleDegrees = new JTextField(null,3);
		txtAngleDegrees.setHorizontalAlignment(SwingConstants.RIGHT);
		txtAngleDegrees.setBounds(220, 24, 50, 20);
		cpFishers.add(txtAngleDegrees);
		
		lblRadius = new JLabel("Distancia al barco (metros): ");
		lblRadius.setBounds(15, 60, 210, 20);
		cpFishers.add(lblRadius);
		
		txtRadiusMeters = new JTextField(null,10);
		txtRadiusMeters.setHorizontalAlignment(SwingConstants.RIGHT);
		txtRadiusMeters.setBounds(220, 61, 50, 20);
		cpFishers.add(txtRadiusMeters);
		
		lbAltitude = new JLabel("Altitud del dron (metros): ");
		lbAltitude.setBounds(15, 98, 210, 20);
		cpFishers.add(lbAltitude);
		
		txtAltitude = new JTextField(null,4);
		txtAltitude.setHorizontalAlignment(SwingConstants.RIGHT);
		txtAltitude.setBounds(220, 99, 50, 20);
		cpFishers.add(txtAltitude);
		
		chkClockwise = new JCheckBox("Sentido horario");
		chkClockwise.setBounds(320,50,150, 20);
		cpFishers.add(chkClockwise);
		
		JPanel cpBoat = new JPanel();
		cpBoat.setBorder(new TitledBorder(null, "Boat configuration", TitledBorder.LEFT, TitledBorder.TOP, null, null));
		FlowLayout flowLayout_1 = (FlowLayout) cpBoat.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		getContentPane().add(cpBoat);
		
		
		lblPathBoat = new JLabel("Fichero misión para el barco: ");
		lblPathBoat.setBounds(0, 130, 210, 20);
		cpBoat.add(lblPathBoat);
		
		txtMisionFilePath = new JTextField("");
		txtMisionFilePath.setEditable(true);
		txtMisionFilePath.setBounds(0, 0, 334, 20);
		txtMisionFilePath.setEnabled(true);
		cpBoat.add(txtMisionFilePath);
		
		JButton btnBoatMision = new JButton("...");
		btnBoatMision.setSize(30,20);
		cpBoat.add(btnBoatMision);
		
		
		btnBoatMision.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				fitxData=api.GUI.loadKMLMissions();
				txtMisionFilePath.setText(fitxData.getValue0());
			}
			
			
		});
		
		
		JPanel cpButton = new JPanel();
		cpButton.setBounds(22, 204, 462, 35);
		getContentPane().add(cpButton);
		cpButton.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton btnOk = new JButton("OK");
		btnOk.setActionCommand("OK");
		cpButton.add(btnOk);
		
		
		btnOk.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
			
		});
		
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("Cancel");
		cpButton.add(btnCancel);
		
		btnCancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				CancelAction();
			}
			
		});
		this.setVisible(true);
	}
	
	@SuppressWarnings("unchecked")
	private void okAction() {
		
		try {
			Tools.setNumUAVs(2);
			UavFishingParam.rotationAngle = Double.parseDouble(txtAngleDegrees.getText());					
			UavFishingParam.clockwise = chkClockwise.isSelected();
			UavFishingParam.radius = Double.parseDouble(txtRadiusMeters.getText());
			UavFishingParam.UavAltitude = Double.parseDouble(txtAltitude.getText());
			//Creamos un vector del tamaño igual al numero de UAVs para evitar problemas en la función sendInitialConfiguration principal.
			UavFishingParam.boatMission = (List<Waypoint>[]) new List[Tools.getNumUAVs()];
			UavFishingParam.boatMission[0]= fitxData.getValue1()[0];
			UavFishingParam.distanceTreshold = 2*UavFishingParam.radius;
			Tools.setLoadedMissionsFromFile(UavFishingParam.boatMission);
			Tools.setProtocolConfigured();
			dispose();
			
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Angle, altitude and distance must be a number.", "Format error", JOptionPane.ERROR_MESSAGE);
		} catch (NullPointerException e) {
			JOptionPane.showMessageDialog(this, "A file mission for Boat must be selected.", "Mission File", JOptionPane.ERROR_MESSAGE);
		}	
		
	}
	
	private void CancelAction() {
				
		dispose();
	
	}

}
