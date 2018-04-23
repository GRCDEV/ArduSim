package pccompanion;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import api.GUIHelper;
import main.Param;
import main.Text;
import main.Param.Protocol;
import main.Param.SimulatorState;
import sim.gui.VerticalFlowLayout;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
	private JPanel panel_1;
	private JPanel panel_2;
	
	private AtomicInteger rowCount = new AtomicInteger();
	private JComboBox<String> protocolComboBox;
	private JLabel lblProtocol;
	
	private volatile int connected = 0;

	public PCCompanionGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		assistantFrame = new JFrame();
		assistantFrame.setBounds(100, 100, 650, 300);
		assistantFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel upperPanel = new JPanel();
		assistantFrame.getContentPane().add(upperPanel, BorderLayout.NORTH);
		GridBagLayout gbl_upperPanel = new GridBagLayout();
		gbl_upperPanel.columnWidths = new int[]{0, 0, 145, 0, 0, 0, 0, 0};
		gbl_upperPanel.rowHeights = new int[]{35, 0};
		gbl_upperPanel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_upperPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		upperPanel.setLayout(gbl_upperPanel);
		
		numUAVsLabel = new JLabel("" + this.connected);
		GridBagConstraints gbc_numUAVsLabel = new GridBagConstraints();
		gbc_numUAVsLabel.insets = new Insets(0, 5, 0, 5);
		gbc_numUAVsLabel.gridx = 0;
		gbc_numUAVsLabel.gridy = 0;
		upperPanel.add(numUAVsLabel, gbc_numUAVsLabel);
		
		JLabel textLabel = new JLabel(Text.NUM_UAVS_COUNTER);
		GridBagConstraints gbc_textLabel = new GridBagConstraints();
		gbc_textLabel.insets = new Insets(0, 0, 0, 5);
		gbc_textLabel.gridx = 1;
		gbc_textLabel.gridy = 0;
		upperPanel.add(textLabel, gbc_textLabel);
		
		lblProtocol = new JLabel(Text.PROTOCOL);
		GridBagConstraints gbc_lblProtocol = new GridBagConstraints();
		gbc_lblProtocol.insets = new Insets(0, 0, 0, 5);
		gbc_lblProtocol.anchor = GridBagConstraints.EAST;
		gbc_lblProtocol.gridx = 3;
		gbc_lblProtocol.gridy = 0;
		upperPanel.add(lblProtocol, gbc_lblProtocol);
		
		protocolComboBox = new JComboBox<String>();
		for (Protocol p : Protocol.values()) {
			protocolComboBox.addItem(p.getName());
		}
		GridBagConstraints gbc_protocolComboBox = new GridBagConstraints();
		gbc_protocolComboBox.insets = new Insets(0, 0, 0, 5);
		gbc_protocolComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_protocolComboBox.gridx = 4;
		gbc_protocolComboBox.gridy = 0;
		upperPanel.add(protocolComboBox, gbc_protocolComboBox);
		
		setupButton = new JButton(Text.SWARM_BASED_CONFIGURATION);
		setupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(PCCompanionGUI.companion.assistantFrame,
						Text.SETUP_WARNING,
						Text.DIALOG_TITLE,
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
					PCCompanionParam.SELECTED_PROTOCOL.set(Param.Protocol.getProtocolByName((String)protocolComboBox.getSelectedItem()));
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							setupButton.setEnabled(false);
							protocolComboBox.setEnabled(false);
						}
					});
				}
			}
		});
		GridBagConstraints gbc_setupButton = new GridBagConstraints();
		gbc_setupButton.insets = new Insets(0, 0, 0, 5);
		gbc_setupButton.gridx = 5;
		gbc_setupButton.gridy = 0;
		upperPanel.add(setupButton, gbc_setupButton);
		
		startButton = new JButton(Text.START_TEST);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Param.simStatus = SimulatorState.TEST_IN_PROGRESS;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						startButton.setEnabled(false);
					}
				});
				Timer timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
		            long count = 0;
		            public void run() {
		            	SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								progressTimeLabel.setText(GUIHelper.timeToString(0, count));
								count = count + 1000;
							}
						});
		            }
		        }, 0, 1000);	// for each second, without initial delay
			}
		});
		GridBagConstraints gbc_startButton = new GridBagConstraints();
		gbc_startButton.insets = new Insets(0, 0, 0, 5);
		gbc_startButton.gridx = 6;
		gbc_startButton.gridy = 0;
		upperPanel.add(startButton, gbc_startButton);
		tableModel = new DefaultTableModel(0, 0);
		String header[] = new String[] { Text.UAV_ID, Text.IDENTIFIER_HEADER, Text.MAC_HEADER, Text.IP_HEADER, Text.STATUS_HEADER };
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
		
		
		panel_1 = new JPanel();
		panel_1.add(table.getTableHeader());
		panel_1.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.TOP, 5, 0));
		panel_1.add(table);
		
		
		panel_2 = new JPanel();
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
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		assistantFrame.setLocation(dim.width/2-assistantFrame.getSize().width/2, 0);
		
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
		if (Param.IS_PC_COMPANION) {
			BigInteger bi = new BigInteger(Long.toString(id & ~(1L << 63)));
		    if (id < 0) bi = bi.setBit(64);
		    String m = bi.toString(16);
		    while (m.length() < 12) {
		    	m = "0" + m;
		    }
		    mac = m.replaceAll(".{2}(?=.)", "$0:");
		} else {
			mac = "-";
		}
		final String status2 = status;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				connected++;
				numUAVsLabel.setText("" + connected);
				tableModel.addRow(new Object[] { "" + (numUAV+1), idString, mac, ip, status2 });
				resizeColumnWidth();
			}
		});
		return numUAV;
	}
	
	/** Modifies the application state received from each UAV. */
	public void setState(int row, String state) {
		final int row2 = row;
		final String state2 = state;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tableModel.setValueAt(state2, row2, 4);
				resizeColumnWidth();
			}
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
	    
	    int prevWidth = assistantFrame.getWidth();
	    assistantFrame.pack();
	    if (assistantFrame.getWidth() < prevWidth) {
	    	assistantFrame.setSize(prevWidth, assistantFrame.getHeight());
	    }
	}

}
