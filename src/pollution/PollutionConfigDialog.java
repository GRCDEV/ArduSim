package pollution;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import api.GUIHelper;
import api.pojo.GeoCoordinates;

public class PollutionConfigDialog extends JDialog {
	JTextField startLatField;
	JTextField startLongField;
	JTextField altitudeField;
	JTextField widthField;
	JTextField lengthField;
	JTextField densityField;
	JTextField pollutionDataField;
	JCheckBox isSimulationCheckBox;
	JLabel pollutionDataLabel;
	JButton pollutionDataButton;
	
	void setSimulation(boolean isSimulation) {
		pollutionDataLabel.setEnabled(isSimulation);
		pollutionDataField.setEnabled(isSimulation);
		pollutionDataButton.setEnabled(isSimulation);
	}

	public PollutionConfigDialog() {
		super();
		
		/** Set up dialog and main container **/
		this.setModal(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container contentPane = this.getContentPane();
		BoxLayout mainLayout = new BoxLayout(contentPane, BoxLayout.PAGE_AXIS);
		contentPane.setLayout(mainLayout);
		((JComponent)this.getContentPane()).setBorder( 
		        BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		/** Create components **/
		// Location components
		JLabel startLatLabel = new JLabel("Start latitude: "); // TODO Add text helper file
		JLabel startLongLabel = new JLabel("Start longitude: "); // TODO Add text helper file
		JLabel altitudeLabel = new JLabel("Altitude: "); // TODO Add text helper file
		startLatField = new JTextField("0", 10);
		startLongField = new JTextField("0", 10);
		altitudeField = new JTextField("0", 10);
		
		// Size components
		JLabel widthLabel = new JLabel("Width (m): "); // TODO Add text helper file
		JLabel lengthLabel = new JLabel("Length (m): "); // TODO Add text helper file
		JLabel densityLabel = new JLabel("Density (mpm): "); // TODO Add text helper file
		densityLabel.setToolTipText("Metres per measurement"); // TODO Add text helper file
		widthField = new JTextField("0", 5);
		lengthField = new JTextField("0", 5);
		densityField = new JTextField("0", 5);
		densityField.setToolTipText("Metres per measurement"); // TODO Add text helper file
		
		// Simulation components
		pollutionDataLabel = new JLabel("Pollution data: "); // TODO Add text helper file
		isSimulationCheckBox = new JCheckBox("Simulation", true); // TODO Add text helper file
		pollutionDataField = new JTextField();
		pollutionDataButton = new JButton(main.Text.BUTTON_SELECT);
		pollutionDataButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(GUIHelper.getCurrentFolder());
				chooser.setDialogTitle("Select pollution data file"); // TODO Add text helper file
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);
				chooser.setAcceptAllFileFilterUsed(false);
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					pollutionDataField.setText(chooser.getSelectedFile().getPath());
				}
				
			}
		});
		
		// Ok button
		JButton okButton = new JButton("Ok");
		
		/** Layout components **/
		
		// Location panel
		JPanel startLocationPanel = new JPanel();
		TitledBorder startLocationBorder = BorderFactory.createTitledBorder("Location");
		startLocationBorder.setTitleJustification(TitledBorder.LEFT);
		startLocationBorder.setTitlePosition(TitledBorder.TOP);
		startLocationPanel.setBorder(startLocationBorder); 
		startLocationPanel.setLayout(new BoxLayout(startLocationPanel, BoxLayout.PAGE_AXIS));
		contentPane.add(startLocationPanel);
		
		// -- Location latitude panel
		JPanel startLatPanel = new JPanel(new BorderLayout());
		startLatPanel.add(startLatLabel, BorderLayout.WEST);
		startLatPanel.add(startLatField, BorderLayout.EAST);
		startLocationPanel.add(startLatPanel);
		
		// -- Location longitude panel
		JPanel startLongPanel = new JPanel(new BorderLayout());
		startLongPanel.add(startLongLabel, BorderLayout.WEST);
		startLongPanel.add(startLongField, BorderLayout.EAST);
		startLocationPanel.add(startLongPanel);
		
		// -- Location altitude panel
		JPanel altitudePanel = new JPanel(new BorderLayout());
		altitudePanel.add(altitudeLabel, BorderLayout.WEST);
		altitudePanel.add(altitudeField, BorderLayout.EAST);
		startLocationPanel.add(altitudePanel);
		
		
		// Size panel
		JPanel sizePanel = new JPanel();
		TitledBorder sizeBorder = BorderFactory.createTitledBorder("Size");
		sizeBorder.setTitleJustification(TitledBorder.LEFT);
		sizeBorder.setTitlePosition(TitledBorder.TOP);
		sizePanel.setBorder(sizeBorder);
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.PAGE_AXIS));
		contentPane.add(sizePanel);
		
		// -- Width panel
		JPanel widthPanel = new JPanel(new BorderLayout());
		widthPanel.add(widthLabel, BorderLayout.WEST);
		widthPanel.add(widthField, BorderLayout.EAST);
		sizePanel.add(widthPanel);
		
		// -- Length panel
		JPanel lengthPanel = new JPanel(new BorderLayout());
		lengthPanel.add(lengthLabel, BorderLayout.WEST);
		lengthPanel.add(lengthField, BorderLayout.EAST);
		sizePanel.add(lengthPanel);
		
		// -- Density panel
		JPanel densityPanel = new JPanel(new BorderLayout());
		densityPanel.add(densityLabel, BorderLayout.WEST);
		densityPanel.add(densityField, BorderLayout.EAST);
		sizePanel.add(densityPanel);
		
		// Simulation panel
		JPanel simulationPanel = new JPanel();
		simulationPanel.setLayout(new BoxLayout(simulationPanel, BoxLayout.PAGE_AXIS));
		JPanel checkBoxPanel = new JPanel(new BorderLayout());
		checkBoxPanel.add(isSimulationCheckBox, BorderLayout.WEST);
		simulationPanel.add(checkBoxPanel);
		setSimulation(isSimulationCheckBox.isSelected());
		isSimulationCheckBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				setSimulation(cb.isSelected());
				
			}
		});
		contentPane.add(simulationPanel);
		
		// -- Pollution data panel
		JPanel pollutionDataPanel = new JPanel(new BorderLayout());
		pollutionDataPanel.add(pollutionDataLabel, BorderLayout.WEST);
		pollutionDataPanel.add(pollutionDataField, BorderLayout.CENTER);
		pollutionDataPanel.add(pollutionDataButton, BorderLayout.EAST);
		simulationPanel.add(pollutionDataPanel);
		
		// Ok button panel
		JPanel okButtonPanel = new JPanel(new BorderLayout());
		okButtonPanel.add(okButton, BorderLayout.CENTER);
		contentPane.add(okButtonPanel);
		
		
		/** Logic **/
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
		});
		startLatField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
		});
		startLongField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
		});
		
		
		
		this.pack();
	}
	
	private void okAction() {
		try {
			PollutionParam.startLocation = new GeoCoordinates(Double.parseDouble(startLatField.getText()), 
					Double.parseDouble(startLongField.getText()));
			PollutionParam.width = Integer.parseInt(widthField.getText());
			PollutionParam.length = Integer.parseInt(lengthField.getText());
			PollutionParam.density = Double.parseDouble(densityField.getText());
			PollutionParam.altitude = Double.parseDouble(altitudeField.getText());
			PollutionParam.pollutionDataFile = pollutionDataField.getText();
			PollutionParam.isSimulation = isSimulationCheckBox.isSelected();
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Longitude and latitude must be in decimal format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
