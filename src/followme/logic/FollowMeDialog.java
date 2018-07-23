package followme.logic;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JComboBox;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class FollowMeDialog extends JDialog implements ActionListener {

	private final JPanel contentPanel = new JPanel();

	/**
	 * Launch the application.
	 */
	String[] mFollow = new String[] {"Lineal", "Matriz",
            "Circle"};
	private JTextField TxtRadioDistance;
	
	JComboBox mSelectComboBox = new JComboBox(mFollow);
	private JTextField TxtUpdateTime;
	private JTextField TxtOtherOption;

	
	
	public static void main(String[] args) {
		try {
			FollowMeDialog dialog = new FollowMeDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public FollowMeDialog() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);;
		
		JPanel Cpanel1 = new JPanel();
		Cpanel1.setBorder(new TitledBorder(null, "FollowMe Options", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
		Cpanel1.setBounds(12, 33, 426, 80);
		contentPanel.add(Cpanel1);
		Cpanel1.setLayout(null);
		
		JLabel lblOptions = new JLabel("Select options");
		lblOptions.setBounds(12, 22, 108, 15);
		Cpanel1.add(lblOptions);
		mSelectComboBox.setBounds(166, 17, 248, 24);
		
		
		mSelectComboBox.setSelectedIndex(1);
		Cpanel1.add(mSelectComboBox);
		
		JLabel LblOptionSelect = new JLabel("Get into radio/distance according to selection (m): ");
		LblOptionSelect.setBounds(12, 49, 359, 15);
		Cpanel1.add(LblOptionSelect);
		
		TxtRadioDistance = new JTextField("75", 3);
		TxtRadioDistance.setHorizontalAlignment(SwingConstants.RIGHT);
		TxtRadioDistance.setBounds(366, 53, 48, 19);
		Cpanel1.add(TxtRadioDistance);
		
		JPanel Cpanel2 = new JPanel();
		Cpanel2.setLayout(null);
		Cpanel2.setBorder(new TitledBorder(null, "Other configurations", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		Cpanel2.setBounds(12, 115, 426, 80);
		contentPanel.add(Cpanel2);
		
		JLabel Lbl = new JLabel("Network refresh period (ms)");
		Lbl.setVerticalAlignment(SwingConstants.TOP);
		Lbl.setBounds(12, 24, 246, 15);
		Cpanel2.add(Lbl);
		
		TxtUpdateTime = new JTextField("1000", 10);
		TxtUpdateTime.setBounds(331, 22, 83, 19);
		Cpanel2.add(TxtUpdateTime);
		
		TxtOtherOption = new JTextField("10", 2);
		TxtOtherOption.setBounds(331, 49, 83, 19);
		Cpanel2.add(TxtOtherOption);
		
		JLabel lblOtherOption = new JLabel("Other option");
		lblOtherOption.setVerticalAlignment(SwingConstants.TOP);
		lblOtherOption.setBounds(12, 51, 246, 15);
		Cpanel2.add(lblOtherOption);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == mSelectComboBox) {
			JComboBox cb = (JComboBox)e.getSource();
			String mOption = (String)cb.getSelectedItem();
			switch (mOption) {
			case "Lineal":
					System.out.println("Lineal");
				break;
			case "Matriz":
				System.out.println("Matriz");
				break;
			case "Circle":
				System.out.println("Circle");
				break;
				
			
			}
			
		}
	}
}
