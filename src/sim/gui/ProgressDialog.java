package sim.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import api.ProtocolHelper;
import main.Param;
import main.Text;
import sim.logic.SimParam;

/** This class generates the dialog that shows UAV information on real time, over the main window. */

public class ProgressDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	public ProgressDialogPanel[] panels;

	public ProgressDialog(JFrame mainWindow) {
		JScrollPane scrollPane;
		panels = new ProgressDialogPanel[Param.numUAVs];

		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		{
			scrollPane = new JScrollPane();
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			JPanel aux = new JPanel();
			for (int i = 0; i < Param.numUAVs; i++) {
				panels[i] = new ProgressDialogPanel();
				panels[i].numUAVLabel.setText("" + Param.id[i]);
				if (ProtocolHelper.selectedProtocol.equals(ProtocolHelper.noneProtocolName)) {
					panels[i].lblProtState.setText("");
					panels[i].protStateLabel.setText("");
				} else {
					panels[i].lblProtState.setText(Text.PROTOCOL_STATUS);
					String initialState = ProtocolHelper.selectedProtocolInstance.setInitialState();
					if (initialState != null) {
						panels[i].protStateLabel.setText(initialState);
					}
				}
				aux.add(panels[i]);
			}
			scrollPane.setViewportView(aux);
			aux.setLayout(new BoxLayout(aux, BoxLayout.Y_AXIS));
			contentPanel.add(scrollPane);
		}

		// Adapting and moving the dialog towards the bottom-right corner
		this.pack();
		
		GraphicsDevice gd = mainWindow.getGraphicsConfiguration().getDevice();
		GraphicsConfiguration config = gd.getDefaultConfiguration();

		int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
		int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
		int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;

		Container c = this.getContentPane();
		Point pt = c.getLocation();
		pt = SwingUtilities.convertPoint(c,  pt, this);
		int maxHeight = config.getBounds().height - top - bottom - pt.y;
		
		// The width may need more room for the side bar
		int dialogWidth = SimParam.DIALOG_WIDTH;
		if (maxHeight < this.getHeight()) {
			dialogWidth = dialogWidth + ((Integer)UIManager.get(Text.SCROLLBAR_WIDTH)).intValue();
		}
		int dialogHeight = Math.min(this.getHeight(), maxHeight);
		setSize(dialogWidth, dialogHeight);
		setLocation(config.getBounds().x + config.getBounds().width - right - dialogWidth, config.getBounds().y + config.getBounds().height - this.getHeight() - top - bottom);
		
		setResizable(false);
		setAlwaysOnTop(true);

		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				SimParam.progressShowing = false;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setVisible(false);
						MainWindow.buttonsPanel.progressDialogButton.setEnabled(true);
					}
				});
			}
		});

		this.setTitle(Text.PROGRESS_DIALOG_TITLE);
		SimParam.progressShowing = true;
	}

}
