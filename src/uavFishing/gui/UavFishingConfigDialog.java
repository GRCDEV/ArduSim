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
	private JLabel lblAngle,lblRadius,lblPathBoat;
	private JTextField txtAngleDegrees,txtRadiusMeters,txtMisionFilePath;
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
		
	
		setTitle(ProtocolHelper.selectedProtocol +" configuration");
		Point screenCenter = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		this.setBounds(screenCenter.x, screenCenter.y, 516, 300);
		this.setModal(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);		
		getContentPane().setLayout(new GridLayout(3,1));
		
		JPanel cpFishers = new JPanel();
		cpFishers.setBorder(new TitledBorder(null, "Fishers configuration", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
		cpFishers.setLayout(null);
		getContentPane().add(cpFishers);
		
		lblAngle = new JLabel("Trajectory angle (degrees): ");
		lblAngle.setBounds(15, 23, 210, 20);
		cpFishers.add(lblAngle);
		
		txtAngleDegrees = new JTextField(null,3);
		txtAngleDegrees.setHorizontalAlignment(SwingConstants.CENTER);
		txtAngleDegrees.setBounds(220, 24, 50, 20);
		cpFishers.add(txtAngleDegrees);
		
		lblRadius = new JLabel("Distancia al barco (metros): ");
		lblRadius.setBounds(15, 60, 210, 20);
		cpFishers.add(lblRadius);
		
		txtRadiusMeters = new JTextField(null,10);
		txtRadiusMeters.setHorizontalAlignment(SwingConstants.RIGHT);
		txtRadiusMeters.setBounds(220, 61, 75, 20);
		cpFishers.add(txtRadiusMeters);

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
				GUI.log("Longitud del vector de listas waypoint: " + Integer.toString(fitxData.getValue1().length));
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
			
			UavFishingParam.angle = Double.parseDouble(txtAngleDegrees.getText());					
			UavFishingParam.clockwise = chkClockwise.isSelected();
			UavFishingParam.radius = Double.parseDouble(txtRadiusMeters.getText());
			//Creamos un vector del tamaño igual al numero de UAVs para evitar problemas en la función sendInitialConfiguration principal.
			UavFishingParam.boatMission = (List<Waypoint>[]) new List[Tools.getNumUAVs()];
			UavFishingParam.boatMission[0]= fitxData.getValue1()[0];
			UavFishingParam.distanceTreshold = 2*UavFishingParam.radius;
			Tools.setLoadedMissionsFromFile(UavFishingParam.boatMission);
			Tools.setNumUAVs(2);
			Tools.setProtocolConfigured();
			dispose();
			
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Angle and distance must be a number.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void CancelAction() {
				
		dispose();
	
	}

}
