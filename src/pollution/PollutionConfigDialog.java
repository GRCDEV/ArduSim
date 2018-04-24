package pollution;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import api.pojo.GeoCoordinates;

public class PollutionConfigDialog extends JDialog {
	JTextField startLatField;
	JTextField startLongField;

	public PollutionConfigDialog() {
		super();
		
		// Set up dialog and main container
		this.setModal(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container contentPane = this.getContentPane();
		BoxLayout mainLayout = new BoxLayout(contentPane, BoxLayout.PAGE_AXIS);
		contentPane.setLayout(mainLayout);
		
		// Create components
		JLabel startLatLabel = new JLabel("Start latitude: "); // TODO Add text helper file
		JLabel startLongLabel = new JLabel("Start longitude: "); // TODO Add text helper file
		startLatField = new JTextField("0", 10);
		startLongField = new JTextField("0", 10);
		JButton okButton = new JButton("Ok");
		
		// Layout components
		
		/** Starting location panel **/
		JPanel startLocationPanel = new JPanel();
		TitledBorder startLocationBorder = BorderFactory.createTitledBorder("Starting location");
		startLocationBorder.setTitleJustification(TitledBorder.LEFT);
		startLocationBorder.setTitlePosition(TitledBorder.TOP);
		//startLocationBorder.setBorder(border);
		startLocationPanel.setBorder(startLocationBorder); 
		startLocationPanel.setLayout(new BoxLayout(startLocationPanel, BoxLayout.PAGE_AXIS));
		contentPane.add(startLocationPanel);
		
		// -- -- Starting location latitude panel
		JPanel startLatPanel = new JPanel(new BorderLayout());
		startLatPanel.add(startLatLabel, BorderLayout.WEST);
		startLatPanel.add(startLatField, BorderLayout.EAST);
		startLocationPanel.add(startLatPanel);
		
		// -- -- Starting location longitude panel
		JPanel startLongPanel = new JPanel(new BorderLayout());
		startLongPanel.add(startLongLabel, BorderLayout.WEST);
		startLongPanel.add(startLongField, BorderLayout.EAST);
		startLocationPanel.add(startLongPanel);
		
		/** Ok button panel **/
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
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Longitude and latitude must be in decimal format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
