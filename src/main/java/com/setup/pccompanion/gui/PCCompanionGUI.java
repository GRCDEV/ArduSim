package com.setup.pccompanion.gui;

import com.api.API;
import com.api.pojo.FlightMode;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;
import com.api.ArduSim;
import com.setup.pccompanion.logic.PCCompanionParam;
import com.setup.pccompanion.logic.PCCompanionTalker;
import com.setup.sim.gui.VerticalFlowLayout;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/** Main GUI for the PC Companion.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class PCCompanionGUI {
	
	public static PCCompanionGUI companion;
	public JFrame assistantFrame;
	
	public JLabel numUAVsLabel;
	public JButton setupButton;
	public JButton startButton;
	private JTable table;
	private DefaultTableModel tableModel;
	public JLabel progressTextLabel;
	public JLabel progressTimeLabel;

	private final AtomicInteger rowCount = new AtomicInteger();
	private JComboBox<String> protocolComboBox;

	private volatile int connected = 0;
	private JButton buttonRecoverControl;
	private JButton buttonRTL;
	private JButton buttonLand;
	
	private Timer timer;
	
	public static final Object semaphore = new Object();
	public static volatile boolean setupPressed = false;

	public PCCompanionGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration config = gd.getDefaultConfiguration();
		assistantFrame = new JFrame(config);
//		assistantFrame.setBounds(100, 100, 700, 300);
		
		JPanel upperPanel = new JPanel();
		assistantFrame.getContentPane().add(upperPanel, BorderLayout.NORTH);
		GridBagLayout gbl_upperPanel = new GridBagLayout();
		gbl_upperPanel.columnWidths = new int[]{0, 0, 0, 0, 145, 0, 0, 0, 0, 0};
		gbl_upperPanel.rowHeights = new int[]{35, 0, 0};
		gbl_upperPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_upperPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		upperPanel.setLayout(gbl_upperPanel);
		
		numUAVsLabel = new JLabel("" + this.connected);
		GridBagConstraints gbc_numUAVsLabel = new GridBagConstraints();
		gbc_numUAVsLabel.insets = new Insets(0, 5, 5, 5);
		gbc_numUAVsLabel.gridx = 0;
		gbc_numUAVsLabel.gridy = 0;
		upperPanel.add(numUAVsLabel, gbc_numUAVsLabel);
		
		JLabel textLabel = new JLabel(Text.NUM_UAVS_COUNTER);
		GridBagConstraints gbc_textLabel = new GridBagConstraints();
		gbc_textLabel.gridwidth = 3;
		gbc_textLabel.anchor = GridBagConstraints.WEST;
		gbc_textLabel.insets = new Insets(0, 0, 5, 5);
		gbc_textLabel.gridx = 1;
		gbc_textLabel.gridy = 0;
		upperPanel.add(textLabel, gbc_textLabel);

		JLabel lblProtocol = new JLabel(Text.PROTOCOL);
		lblProtocol.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblProtocol = new GridBagConstraints();
		gbc_lblProtocol.insets = new Insets(0, 0, 5, 5);
		gbc_lblProtocol.anchor = GridBagConstraints.EAST;
		gbc_lblProtocol.gridx = 5;
		gbc_lblProtocol.gridy = 0;
		upperPanel.add(lblProtocol, gbc_lblProtocol);
		
		protocolComboBox = new JComboBox<>();
		for (int i = 0; i < ArduSimTools.ProtocolNames.length; i++) {
			protocolComboBox.addItem(ArduSimTools.ProtocolNames[i]);
		}
		GridBagConstraints gbc_protocolComboBox = new GridBagConstraints();
		gbc_protocolComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_protocolComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_protocolComboBox.gridx = 6;
		gbc_protocolComboBox.gridy = 0;
		upperPanel.add(protocolComboBox, gbc_protocolComboBox);
		
		setupButton = new JButton(Text.SETUP_TEST);
		setupButton.addActionListener(e -> {
			synchronized(semaphore) {
				if (!setupPressed) {
					int result = JOptionPane.showConfirmDialog(PCCompanionGUI.companion.assistantFrame,
							Text.SETUP_WARNING,
							Text.DIALOG_TITLE,
							JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION) {
						Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
						ArduSimTools.selectedProtocol = (String)protocolComboBox.getSelectedItem();
						ArduSimTools.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();
						if (ArduSimTools.selectedProtocolInstance == null) {
							ArduSimTools.closeAll(Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR);
						}
						SwingUtilities.invokeLater(() -> {
							setupButton.setEnabled(false);
							protocolComboBox.setEnabled(false);
							buttonRecoverControl.setEnabled(true);
							buttonRTL.setEnabled(true);
							buttonLand.setEnabled(true);
						});
					}
					setupPressed = true;
				}
			}
		});
		GridBagConstraints gbc_setupButton = new GridBagConstraints();
		gbc_setupButton.insets = new Insets(0, 0, 5, 5);
		gbc_setupButton.gridx = 7;
		gbc_setupButton.gridy = 0;
		upperPanel.add(setupButton, gbc_setupButton);
		
		startButton = new JButton(Text.START_TEST);
		startButton.addActionListener(e -> {
			Param.simStatus = SimulatorState.TEST_IN_PROGRESS;
			SwingUtilities.invokeLater(() -> startButton.setEnabled(false));
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				long count = 0;
				public void run() {
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						final String timeString = API.getValidationTools().timeToString(0, count);
						count = count + 1000;
						SwingUtilities.invokeLater(() -> progressTimeLabel.setText(timeString));
					} else {
						timer.cancel();
					}
				}
			}, 0, 1000);	// for each second, without initial delay
		});
		GridBagConstraints gbc_startButton = new GridBagConstraints();
		gbc_startButton.insets = new Insets(0, 0, 5, 0);
		gbc_startButton.gridx = 8;
		gbc_startButton.gridy = 0;
		upperPanel.add(startButton, gbc_startButton);
		tableModel = new DefaultTableModel(0, 0);
		String[] header = new String[] { Text.UAV_ID, Text.IDENTIFIER_HEADER, Text.MAC_HEADER, Text.IP_HEADER, Text.STATUS_HEADER };
		tableModel.setColumnIdentifiers(header);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( SwingConstants.CENTER );
		
		table = new JTable();
		table.setModel(tableModel);
		table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
		table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setEnabled(false);


		JPanel panel_1 = new JPanel();
		panel_1.add(table.getTableHeader());
		panel_1.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.TOP, 5, 0));
		panel_1.add(table);


		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BorderLayout(0, 0));
		panel_2.add(panel_1);
		
		JScrollPane scrollPane = new JScrollPane(panel_2);
		assistantFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JPanel lowerPanel = new JPanel();
		assistantFrame.getContentPane().add(lowerPanel, BorderLayout.SOUTH);
		
		progressTextLabel = new JLabel(Text.PROGRESS_DIALOG_TITLE + ":");
		lowerPanel.add(progressTextLabel);
		
		progressTimeLabel = new JLabel();
		lowerPanel.add(progressTimeLabel);
		
		setupButton.setEnabled(false);
		startButton.setEnabled(false);
		
		buttonRecoverControl = new JButton(Text.RECOVER_CONTROL);
		buttonRecoverControl.setEnabled(false);
		buttonRecoverControl.addActionListener(e -> {
			PCCompanionParam.action.set(PCCompanionParam.ACTION_RECOVER_CONTROL);
			if (timer != null) {
				timer.cancel();
			}
			SwingUtilities.invokeLater(() -> {
				buttonRecoverControl.setEnabled(false);
				buttonRTL.setEnabled(false);
				buttonLand.setEnabled(false);
			});
		});
		GridBagConstraints gbc_buttonRecoverControl = new GridBagConstraints();
		gbc_buttonRecoverControl.insets = new Insets(0, 0, 0, 5);
		gbc_buttonRecoverControl.gridx = 1;
		gbc_buttonRecoverControl.gridy = 1;
		upperPanel.add(buttonRecoverControl, gbc_buttonRecoverControl);
		
		buttonRTL = new JButton(FlightMode.RTL.getMode());
		buttonRTL.setEnabled(false);
		buttonRTL.addActionListener(e -> {
			PCCompanionParam.action.set(PCCompanionParam.ACTION_RTL);
			if (timer != null) {
				timer.cancel();
			}
			SwingUtilities.invokeLater(() -> {
				buttonRecoverControl.setEnabled(false);
				buttonRTL.setEnabled(false);
				buttonLand.setEnabled(false);
			});
		});
		GridBagConstraints gbc_buttonRTL = new GridBagConstraints();
		gbc_buttonRTL.fill = GridBagConstraints.HORIZONTAL;
		gbc_buttonRTL.insets = new Insets(0, 0, 0, 5);
		gbc_buttonRTL.gridx = 2;
		gbc_buttonRTL.gridy = 1;
		upperPanel.add(buttonRTL, gbc_buttonRTL);
		
		buttonLand = new JButton(FlightMode.LAND.getMode());
		buttonLand.setEnabled(false);
		buttonLand.addActionListener(e -> {
			PCCompanionParam.action.set(PCCompanionParam.ACTION_LAND);
			if (timer != null) {
				timer.cancel();
			}
			SwingUtilities.invokeLater(() -> {
				buttonRecoverControl.setEnabled(false);
				buttonRTL.setEnabled(false);
				buttonLand.setEnabled(false);
			});
		});
		GridBagConstraints gbc_buttonLand = new GridBagConstraints();
		gbc_buttonLand.insets = new Insets(0, 0, 0, 5);
		gbc_buttonLand.fill = GridBagConstraints.HORIZONTAL;
		gbc_buttonLand.gridx = 3;
		gbc_buttonLand.gridy = 1;
		upperPanel.add(buttonLand, gbc_buttonLand);

		JLabel lblEm = new JLabel(Text.EMERGENCY_ACTIONS);
		lblEm.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblEm = new GridBagConstraints();
		gbc_lblEm.gridwidth = 5;
		gbc_lblEm.anchor = GridBagConstraints.WEST;
		gbc_lblEm.insets = new Insets(0, 0, 0, 5);
		gbc_lblEm.gridx = 4;
		gbc_lblEm.gridy = 1;
		upperPanel.add(lblEm, gbc_lblEm);
		
//		assistantFrame.setLocation(config.getBounds().width/2 - assistantFrame.getSize().width/2, 0);
		
		if (Param.runningOperatingSystem != Param.OS_WINDOWS) {
			assistantFrame.setUndecorated(true);
		}
		assistantFrame.pack();
		
	//  Adapting the window to the screen size
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		int left = insets.left;
		int right = insets.right;
		int top = insets.top;
		int bottom = insets.bottom;

		int width = config.getBounds().width - left - right;
		int height = config.getBounds().height - top - bottom;
		
		assistantFrame.setSize(width, height);
		assistantFrame.setResizable(false);
		assistantFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		assistantFrame.setTitle(Text.COMPANION_NAME);
		ActionListener escListener = e -> (new Thread(() -> {
			assistantFrame.dispose();
			System.exit(0);
		})).start();
	    assistantFrame.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		assistantFrame.setVisible(true);
		Param.simStatus = SimulatorState.STARTING_UAVS;
		(new PCCompanionTalker()).start();
	}


	/** Inserts a UAV in the table.
	 * <p>Returns the row number, starting in 0. */
	public int insertRow(long id, String IP, String status) {
		final int  numUAV = rowCount.getAndIncrement();
		final String idString = "" + id;
		final String mac;
		final String ip = IP;
		if (Param.role == ArduSim.PCCOMPANION) {
			BigInteger bi = new BigInteger(Long.toString(id & ~(1L << 63)));
		    if (id < 0) bi = bi.setBit(64);
		    StringBuilder m = new StringBuilder(bi.toString(16));
		    while (m.length() < 12) {
		    	m.insert(0, "0");
		    }
		    mac = m.toString().replaceAll(".{2}(?=.)", "$0:");
		} else {
			mac = "-";
		}
		final String status2 = status;
		
		SwingUtilities.invokeLater(() -> {
			connected++;
			numUAVsLabel.setText("" + connected);
			tableModel.addRow(new Object[] { "" + (numUAV+1), idString, mac, ip, status2 });
			resizeColumnWidth();
		});
		return numUAV;
	}
	
	/** Modifies the application state received from each UAV. */
	public void setState(int row, String state) {
		final int row2 = row;
		final String state2 = state;
		SwingUtilities.invokeLater(() -> {
			tableModel.setValueAt(state2, row2, 4);
			resizeColumnWidth();
		});
	}
	
	
	private void resizeColumnWidth() {
	    final TableColumnModel columnModel = table.getColumnModel();
	    TableCellRenderer renderer;
	    Component comp;
	    
	    for (int col = 0; col < table.getColumnCount(); col++) {
	    	TableColumn column = columnModel.getColumn(col);
	    	int currentWidth = column.getPreferredWidth();
	        int width = 20; // Min width
	        for (int row = 0; row < table.getRowCount(); row++) {
	            renderer = table.getCellRenderer(row, col);
	            comp = table.prepareRenderer(renderer, row, col);
	            width = Math.max(comp.getPreferredSize().width +1 , width);
	        }
	        
	        renderer = column.getHeaderRenderer();
	        if (renderer == null) {
	        	renderer = table.getTableHeader().getDefaultRenderer();
	        }
	        comp = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, col);
	        width = Math.max(comp.getPreferredSize().width +1,  width);
	        // Upper row width limit
	        if(width > 300) {
	        	width=300;
	        }
	        if (width + 20 != currentWidth) {
	        	column.setPreferredWidth(width + 20);
	        }
	    }
	    
//	    int prevWidth = assistantFrame.getWidth();
//	    assistantFrame.pack();
//	    if (assistantFrame.getWidth() < prevWidth) {
//	    	assistantFrame.setSize(prevWidth, assistantFrame.getHeight());
//	    }
	}

}
