package com.setup.sim.gui;

import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimParam;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/** This class generates the dialog that shows UAV information on real time, over the main window.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ProgressDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	public static ProgressDialog progressDialog;
	// Whether the progress dialog is showing or not
	public static volatile boolean progressShowing = false;

	public ProgressDialogPanel[] panels;
	
	private static volatile int x, y;
	private static final Object SEMAPHORE = new Object();

	public ProgressDialog(JFrame mainWindow) {
		JScrollPane scrollPane;
		panels = new ProgressDialogPanel[Param.numUAVs];

		getContentPane().setLayout(new BorderLayout());
		JPanel contentPanel = new JPanel();
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
				if (ArduSimTools.selectedProtocol.equals(ArduSimTools.noneProtocolName)) {
					panels[i].lblProtState.setText("");
					panels[i].protStateLabel.setText("");
				} else {
					panels[i].lblProtState.setText(Text.PROTOCOL_STATUS);
					String initialState = ArduSimTools.selectedProtocolInstance.setInitialState();
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
			dialogWidth = dialogWidth + (Integer) UIManager.get(Text.SCROLLBAR_WIDTH);
		}
		int dialogHeight = Math.min(this.getHeight(), maxHeight);
		setSize(dialogWidth, dialogHeight);
		ProgressDialog.x = config.getBounds().x + config.getBounds().width - right - dialogWidth;
		ProgressDialog.y = config.getBounds().y + config.getBounds().height - this.getHeight() - top - bottom;
		setLocation(x, y);
		
		setResizable(false);
		setAlwaysOnTop(true);

		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				ProgressDialog.progressDialog.toggleProgressShown();
			}
		});

		this.setTitle(Text.PROGRESS_DIALOG_TITLE);
		
		ActionListener escListener = e -> ProgressDialog.progressDialog.toggleProgressShown();
	    this.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
	    
	    ProgressDialog.progressDialog = this;
	}
	
	/** Request to show or hide the progress dialog. */
	public void toggleProgressShown() {
		synchronized(SEMAPHORE) {
			if (ProgressDialog.progressShowing) {
				ProgressDialog.progressShowing = false;
				SwingUtilities.invokeLater(() -> {
					ProgressDialog.progressDialog.setVisible(false);
					MainWindow.buttonsPanel.progressDialogButton.setText(Text.SHOW_PROGRESS);
				});
			} else {
				ProgressDialog.progressShowing = true;
				SwingUtilities.invokeLater(() -> {
					ProgressDialog.progressDialog.setLocation(ProgressDialog.x, ProgressDialog.y);
					ProgressDialog.progressDialog.setVisible(true);
					MainWindow.buttonsPanel.progressDialogButton.setText(Text.HIDE_PROGRESS);
				});
			}
		}
	}

}
