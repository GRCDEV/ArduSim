package com.protocols.mbcap.gui;

import com.api.API;
import com.api.ValidationTools;
import com.api.pojo.StatusPacket;
import com.protocols.mbcap.logic.MBCAPPCCompanionListener;
import com.protocols.mbcap.logic.MBCAPText;
import com.protocols.mbcap.pojo.Beacon;
import com.protocols.mbcap.pojo.MBCAPState;
import com.setup.Param.SimulatorState;
import com.setup.sim.gui.VerticalFlowLayout;
import es.upv.grc.mapper.Location3DUTM;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 
 * Dialog opened in the PC Companion to monitor the messages sent among the UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPPCCompanionDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public static volatile MBCAPPCCompanionDialog mbcap = null;

	private StatusPacket[] connected;
	private final List<StatusPacket> adding = new ArrayList<>();
	private volatile StatusPacket[] shown = null;

	private JTable table;
	private DefaultTableModel tableModel;
	private JPanel panel_1;
	private JPanel panel_2;
	
	@SuppressWarnings("unused")
	private MBCAPPCCompanionDialog() {}

	public MBCAPPCCompanionDialog(Frame owner) {
		super(owner);
		setTitle(API.getArduSim().getSelectedProtocolName());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		{
			tableModel = new DefaultTableModel(0, 0);
			String[] header = new String[] { MBCAPText.ID, MBCAPText.EVENT, MBCAPText.FLIGHT_MODE,
					MBCAPText.ID_AVOIDING, MBCAPText.SPEED,
					MBCAPText.X, MBCAPText.Y, MBCAPText.Z };
			tableModel.setColumnIdentifiers(header);
			DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
			centerRenderer.setHorizontalAlignment( SwingConstants.CENTER );
			table = new JTable();
			table.setModel(tableModel);
			table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
			table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
			table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setEnabled(false);

			panel_1 = new JPanel();
			panel_1.add(table.getTableHeader());
			panel_1.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.TOP, 5, 0));
			panel_1.add(table);

			panel_2 = new JPanel();
			panel_2.setLayout(new BorderLayout(0, 0));
			panel_2.add(panel_1);

			connected = API.getGUI(0).getDetectedUAVs();
			int count = 0;
			for (int i = 0; i < connected.length; i++) {
				if (connected[i].status == SimulatorState.READY_FOR_TEST
						|| connected[i].status == SimulatorState.TEST_IN_PROGRESS) {
					tableModel.addRow(new String[] {"" + connected[i].id, "-","-","-","-","-","-", "-"});
					adding.add(connected[i]);
					connected[i].row = count;	// Reuse of the row parameter from general table for MBCAP table
					count++;
				}
			}
			shown = adding.toArray(new StatusPacket[adding.size()]);

			JScrollPane scrollPane = new JScrollPane(panel_2);
			getContentPane().add(scrollPane, BorderLayout.CENTER);
			resizeColumnWidth();
		}
		
		this.setModalityType(ModalityType.MODELESS);
		this.setVisible(true);
		
		// Listen for protocol data packets
		new MBCAPPCCompanionListener(this).start();
	}

	/** Updates a UAV row with information received within the protocol. */
	public void updateRow(Beacon beacon) {
		int pos = -1;
		boolean found = false;
		for (int i = 0; i < shown.length && !found; i++) {
			if (shown[i].id == beacon.uavId) {
				pos = i;
				found = true;
			}
		}
		if (found) {
			final int row = shown[pos].row;
			final Beacon b = beacon;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ValidationTools validationTools = API.getValidationTools();
					tableModel.setValueAt(b.event, row, 1);
					tableModel.setValueAt(MBCAPState.getSatateById(b.state), row, 2);
					tableModel.setValueAt(b.idAvoiding, row, 3);
					tableModel.setValueAt(validationTools.roundDouble(b.speed, 3), row, 4);
					if (b.points.size() > 0) {
						Location3DUTM p = b.points.get(0);
						tableModel.setValueAt(validationTools.roundDouble(p.x, 3), row, 5);
						tableModel.setValueAt(validationTools.roundDouble(p.y, 3), row, 6);
						tableModel.setValueAt(validationTools.roundDouble(p.z, 2), row, 7);
					}
					resizeColumnWidth();
				}
			});
		}
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
	        if (width + 20 > currentWidth) {
	        	column.setPreferredWidth(width + 20);
	        }
	    }
	    int prevWidth = this.getWidth();
	    this.pack();
	    if (this.getWidth() < prevWidth) {
	    	this.setSize(prevWidth, this.getHeight());
	    }
	}

}
