package com.setup.sim.gui;

import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;

import javax.swing.*;
import java.awt.*;

/** This class generates the panel used to interact with the application, which is inside the main window.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MainWindowButtonsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public JButton progressDialogButton;
	public JButton setupButton;
	public JButton startTestButton;
	public JButton exitButton;
	public JTextArea logArea;
	public JLabel statusLabel;

	public MainWindowButtonsPanel() {
		setMaximumSize(new Dimension(32767, 100));
		setPreferredSize(new Dimension(800, 100));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		logArea = new JTextArea();
		logArea.setMinimumSize(new Dimension(80, 100));
		logArea.setEditable(false);
		GridBagConstraints gbc_textArea = new GridBagConstraints();
		gbc_textArea.weightx = 1.0;
		gbc_textArea.insets = new Insets(0, 0, 0, 5);
		gbc_textArea.fill = GridBagConstraints.BOTH;
		gbc_textArea.gridx = 0;
		gbc_textArea.gridy = 0;
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(logArea);
		add(sp, gbc_textArea);

		JPanel rightPanel = new JPanel();
		rightPanel.setSize(new Dimension(340, 100));
		rightPanel.setMaximumSize(new Dimension(340, 100));
		rightPanel.setMinimumSize(new Dimension(340, 100));
		rightPanel.setPreferredSize(new Dimension(340, 100));
		GridBagConstraints gbc_rightPanel = new GridBagConstraints();
		gbc_rightPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_rightPanel.weightx = 0.5;
		gbc_rightPanel.gridx = 1;
		gbc_rightPanel.gridy = 0;
		add(rightPanel, gbc_rightPanel);
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		Component verticalStrut = Box.createVerticalStrut(20);
		verticalStrut.setSize(new Dimension(340, 10));
		verticalStrut.setMaximumSize(new Dimension(340, 10));
		verticalStrut.setMinimumSize(new Dimension(340, 10));
		verticalStrut.setPreferredSize(new Dimension(340, 10));
		rightPanel.add(verticalStrut);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setSize(new Dimension(340, 0));
		buttonsPanel.setMinimumSize(new Dimension(340, 10));
		buttonsPanel.setPreferredSize(new Dimension(340, 10));
		rightPanel.add(buttonsPanel);
		GridBagLayout gbl_buttonsPanel = new GridBagLayout();
		gbl_buttonsPanel.columnWidths = new int[] { 0, 103, 59, 0, 0 };
		gbl_buttonsPanel.rowHeights = new int[] { 25, 0 };
		gbl_buttonsPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_buttonsPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		buttonsPanel.setLayout(gbl_buttonsPanel);
		
		progressDialogButton = new JButton(Text.SHOW_PROGRESS);
		progressDialogButton.addActionListener(e -> ProgressDialog.progressDialog.toggleProgressShown());
		GridBagConstraints gbc_progressDialogButton = new GridBagConstraints();
		gbc_progressDialogButton.insets = new Insets(0, 0, 0, 5);
		gbc_progressDialogButton.gridx = 0;
		gbc_progressDialogButton.gridy = 0;
		buttonsPanel.add(progressDialogButton, gbc_progressDialogButton);
		
		setupButton = new JButton(Text.SETUP_TEST);
		setupButton.setEnabled(false);
		setupButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			MainWindow.buttonsPanel.setupButton.setEnabled(false);
			MainWindow.buttonsPanel.statusLabel.setText(Text.CONFIGURATION_IN_PROGRESS);
			Param.setupTime = System.currentTimeMillis();
			Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
		}));
		GridBagConstraints gbc_setupButton = new GridBagConstraints();
		gbc_setupButton.anchor = GridBagConstraints.NORTH;
		gbc_setupButton.insets = new Insets(0, 0, 0, 5);
		gbc_setupButton.gridx = 1;
		gbc_setupButton.gridy = 0;
		buttonsPanel.add(setupButton, gbc_setupButton);

		startTestButton = new JButton(Text.START_TEST);
		startTestButton.setEnabled(false);
		startTestButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			MainWindow.buttonsPanel.statusLabel.setText(Text.TEST_IN_PROGRESS);
			MainWindow.buttonsPanel.startTestButton.setEnabled(false);
			Param.startTime = System.currentTimeMillis();
			Param.simStatus = SimulatorState.TEST_IN_PROGRESS;
		}));
		GridBagConstraints gbc_startTestButton = new GridBagConstraints();
		gbc_startTestButton.anchor = GridBagConstraints.NORTHWEST;
		gbc_startTestButton.insets = new Insets(0, 0, 0, 5);
		gbc_startTestButton.gridx = 2;
		gbc_startTestButton.gridy = 0;
		buttonsPanel.add(startTestButton, gbc_startTestButton);

		exitButton = new JButton(Text.EXIT);
		exitButton.addActionListener(e -> (new Thread(new Runnable() {
			public void run() {
				synchronized(MainWindow.CLOSE_SEMAPHORE) {
					if (Param.simStatus != SimulatorState.SHUTTING_DOWN) {
						ArduSimTools.shutdown();
					}
				}
			}
		})).start());
		GridBagConstraints gbc_exitButton = new GridBagConstraints();
		gbc_exitButton.anchor = GridBagConstraints.NORTHWEST;
		gbc_exitButton.gridx = 3;
		gbc_exitButton.gridy = 0;
		buttonsPanel.add(exitButton, gbc_exitButton);

		Component verticalStrut_1 = Box.createVerticalStrut(20);
		verticalStrut_1.setSize(new Dimension(340, 10));
		verticalStrut_1.setMinimumSize(new Dimension(340, 10));
		verticalStrut_1.setMaximumSize(new Dimension(340, 10));
		verticalStrut_1.setPreferredSize(new Dimension(340, 10));
		rightPanel.add(verticalStrut_1);

		statusLabel = new JLabel();
		statusLabel.setSize(new Dimension(340, 15));
		statusLabel.setMaximumSize(new Dimension(340, 15));
		statusLabel.setMinimumSize(new Dimension(340, 15));
		statusLabel.setPreferredSize(new Dimension(340, 15));
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(statusLabel);

	}

}
