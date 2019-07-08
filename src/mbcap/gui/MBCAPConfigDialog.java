package mbcap.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import api.API;
import main.api.ArduSim;
import mbcap.logic.MBCAPText;

/** This class generates the dialog to input the configuration of the MBCAP protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPConfigDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private final JPanel contentPanel = new JPanel();
  private MBCAPConfigDialogPanel panel;

  public MBCAPConfigDialog() {
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setLayout(new FlowLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      panel = new MBCAPConfigDialogPanel();
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
        	  if(MBCAPGUITools.isValidProtocolConfiguration(panel)) {
        		  MBCAPGUITools.storeProtocolConfiguration(panel);
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
      {
        JButton restoreDefaultsButton = new JButton(MBCAPText.RESTORE_DEFAULTS);
        restoreDefaultsButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            MBCAPGUITools.loadDefaultProtocolConfiguration(panel);
          }
        });
        buttonPane.add(restoreDefaultsButton);
      }
    }
    
    MBCAPGUITools.loadDefaultProtocolConfiguration(panel);
    this.setTitle(ardusim.getSelectedProtocolName() + " " + MBCAPText.CONFIGURATION);
  }
  
}
