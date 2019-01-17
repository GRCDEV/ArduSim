package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import api.GUI;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.logic.SimTools;

/** This class generates the dialog to input the application general configuration.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private ConfigDialogPanel panel;

	public ConfigDialog() {
		getContentPane().setLayout(new BorderLayout());
		{
			panel = new ConfigDialogPanel();
			panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		}
		
		{
			JScrollPane scrollPane = new JScrollPane(panel);
			scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			getContentPane().add(scrollPane, BorderLayout.CENTER);
			
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
		
		GUI.addEscapeListener(this, true);

		this.setTitle(Text.CONFIGURATION_DIALOG_TITLE);
		SimTools.loadDefaultConfiguration(panel);
		
		this.pack();
		this.setSize(this.getWidth() + ((Integer)UIManager.get(Text.SCROLLBAR_WIDTH)).intValue(),
				this.getHeight() + ((Integer)UIManager.get(Text.SCROLLBAR_WIDTH)).intValue());
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration config = gd.getDefaultConfiguration();
		int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
		int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
		int upperBar = this.getHeight() - this.getContentPane().getHeight();
		int height = config.getBounds().height - top - bottom - upperBar;
		
		if (this.getHeight() > height) {
			this.setSize(this.getWidth(), height);
		} else  {
			this.setResizable(false);
		}
		this.setModal(true);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

}
