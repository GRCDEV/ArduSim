package vision.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;



/** This class generates the dialog to input the configuration of the MBCAP protocol.
 * <p>Developed by: Jamie Wubben, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ConfigDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private final JPanel contentPanel = new JPanel();
  private ConfigDialogPanel panel;

  private ResourceBundle rs;
  public ConfigDialog() {
	  // skip everything just go 
	  Tools.setProtocolConfigured();
	  /*
	  try {
			rs = ResourceBundle.getBundle("vision.bundle.text", new Locale("en"));
	  }catch(java.util.MissingResourceException e){
			GUI.log("shutdown, no resource bundle found.");
	        System.gc(); // Needed to avoid the error: Exception while removing reference.
	        System.exit(0);
	  }
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
        JButton okButton = new JButton(rs.getString("ok"));
        okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if(visionHelper.isValidProtocolConfiguration(panel)) {
              visionHelper.storeProtocolConfiguration(panel);
              Tools.setProtocolConfigured();
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
    
    GUI.addEscapeListener(this, true);
    
    this.setTitle(ProtocolHelper.selectedProtocol + " " + rs.getString("configuration"));
    this.pack();
    this.setModal(true);
    this.setResizable(false);
    this.setLocationRelativeTo(null);
    this.setVisible(true);
    */
  }
  
}
