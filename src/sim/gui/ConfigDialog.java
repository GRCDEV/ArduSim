package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.logic.SimTools;

/** This class generates de dialog to input the application general configuration. */

public class ConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private ConfigDialogPanel panel;

	public ConfigDialog() {
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			panel = new ConfigDialogPanel();
			contentPanel.add(panel);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Text.OK);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (SimTools.isValidConfiguration(panel)) {
							SimTools.storeConfiguration(panel);
							Param.simStatus = SimulatorState.CONFIGURING_PROTOCOL;
							dispose();
						}
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton restoreDefaultsButton = new JButton(Text.RESTORE_DEFAULTS);
				restoreDefaultsButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SimTools.loadDefaultConfiguration(panel);
					}
				});
				buttonPane.add(restoreDefaultsButton);
			}
		}

		// The application closes with the dialog
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				dispose();
				System.gc();
				System.exit(0);
			}
		});

		this.setTitle(Text.CONFIGURATION_DIALOG_TITLE);
		SimTools.loadDefaultConfiguration(panel);
		this.pack();
		this.setResizable(false);
		this.setModal(true);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

}
