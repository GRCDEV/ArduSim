package mission.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import api.API;
import main.api.ArduSim;
import mbcap.logic.MBCAPText;
import mission.logic.MissionHelper;

/** This class generates the dialog to input the configuration of the MBCAP protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionConfigDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private final JPanel contentPanel = new JPanel();
  private MissionConfigDialogPanel panel;

  public MissionConfigDialog() {
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setLayout(new FlowLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      panel = new MissionConfigDialogPanel();
      contentPanel.add(panel);
    }
    final ArduSim ardusim = API.getArduSim();
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton(MBCAPText.OK);
        okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if(MissionHelper.isValidProtocolConfiguration(panel)) {
              MissionHelper.storeProtocolConfiguration(panel);
              ardusim.setProtocolConfigured();
              SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					dispose();
				}
			});
              
            }
          }
        });
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
    }
    
    //  If the dialog is closed, then it closes the whole program
    this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        dispose();
        System.gc(); // Needed to avoid the error: Exception while removing reference.
        System.exit(0);
      }
    });
    
    API.getGUI(0).addEscapeListener(this, true);
    
    this.setTitle(ardusim.getSelectedProtocolName() + " " + MBCAPText.CONFIGURATION);
    this.pack();
    this.setModal(true);
    this.setResizable(false);
    this.setLocationRelativeTo(null);
    this.setVisible(true);
  }
  
}
