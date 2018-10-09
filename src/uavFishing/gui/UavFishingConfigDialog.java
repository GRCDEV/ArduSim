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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import api.GUI;
import api.ProtocolHelper;



public class UavFishingConfigDialog extends JDialog{
	
	private static final long serialVersionUID = 1L;
	private JLabel lblAngle,lblRadius,lblPathBoat;
	private JTextField txtAngleDegrees,txtRadiusMeters,txtDataFilePath;
	private JCheckBox chkClockwise;
	
	
	public static void main(String[] args) {
		try {
			UavFishingConfigDialog dialog = new UavFishingConfigDialog();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
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
		//cpFishers.setBounds(10, 10, 490, 100);
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
		Float x = cpBoat.getAlignmentX();
		GUI.log(x.toString());
		
		
		lblPathBoat = new JLabel("Fichero misión para el barco: ");
		lblPathBoat.setBounds(0, 130, 210, 20);
		cpBoat.add(lblPathBoat);
		
		txtDataFilePath = new JTextField("/home/fran/git/Ardusim");
		txtDataFilePath.setEditable(true);
		txtDataFilePath.setBounds(0, 0, 334, 20);
		txtDataFilePath.setEnabled(true);
		cpBoat.add(txtDataFilePath);
		
		JButton btnBoatMision = new JButton("...");
		btnBoatMision.setSize(30,20);
		cpBoat.add(btnBoatMision);
		
		
		btnBoatMision.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				//chooser.setCurrentDirectory(Tools.getCurrentFolder());
				chooser.setDialogTitle("Select UAV mision data file"); // TODO Add text helper file
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);
				chooser.setAcceptAllFileFilterUsed(false);
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Keyhole Markup Language file", "kml");
				chooser.addChoosableFileFilter(filter);
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
		
		JButton BtnCancel = new JButton("Cancel");
		BtnCancel.setActionCommand("Cancel");
		cpButton.add(BtnCancel);
		
		this.setVisible(true);
	}

}
