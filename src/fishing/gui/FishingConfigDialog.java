package fishing.gui;


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
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.javatuples.Pair;
import java.util.List;

import main.api.GUI;
import api.API;
import api.pojo.location.Waypoint;
import fishing.logic.FishingParam;



public class FishingConfigDialog extends JDialog{
	
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
	
	public FishingConfigDialog () {
		
		final GUI gui = API.getGUI(0);
	
		int Panelwith=516;
		int Panelheitgth=420;
		setTitle(API.getArduSim().getSelectedProtocolName() +" configuration");
		Point screenCenter = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		this.setBounds(screenCenter.x-(Panelwith/2), screenCenter.y-(Panelheitgth/2), Panelwith, Panelheitgth);
		gui.log("Posicion ventana :" + screenCenter.x + ", " + screenCenter.y);	
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
				
				fitxData=gui.loadMissionsKML();
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
	}
	
	@SuppressWarnings("unchecked")
	private void okAction() {
		
		try {
			int numUAVs = 2;
			API.getArduSim().setNumUAVs(numUAVs);
			FishingParam.rotationAngle = Double.parseDouble(txtAngleDegrees.getText());					
			FishingParam.clockwise = chkClockwise.isSelected();
			FishingParam.radius = Double.parseDouble(txtRadiusMeters.getText());
			FishingParam.UavAltitude = Double.parseDouble(txtAltitude.getText());
			//Creamos un vector del tamaño igual al numero de UAVs para evitar problemas en la función sendInitialConfiguration principal.
			FishingParam.boatMission = (List<Waypoint>[]) new List[numUAVs];
			FishingParam.boatMission[0]= fitxData.getValue1()[0];
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
	
	private void CancelAction() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				dispose();
			}
		});
	}

}
