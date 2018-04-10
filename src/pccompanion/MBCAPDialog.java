package pccompanion;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import api.GUIHelper;
import api.pojo.Point3D;
import main.Param;
import main.Text;
import main.Param.SimulatorState;
import mbcap.logic.MBCAPParam;
import mbcap.pojo.Beacon;
import sim.board.BoardParam;
import sim.gui.VerticalFlowLayout;
import sim.logic.SimParam;
import uavController.UAVParam;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class MBCAPDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public static volatile MBCAPDialog mbcap = null;

	public static AtomicReference<StatusPacket[]> connectedUAVs = new AtomicReference<>();
	private StatusPacket[] connected;
	private List<StatusPacket> adding = new ArrayList<StatusPacket>();
	private volatile StatusPacket[] shown = null;

	private JTable table;
	private DefaultTableModel tableModel;
	private JPanel panel_1;
	private JPanel panel_2;

	/**
	 * Create the dialog.
	 */
	public MBCAPDialog() {
		setTitle(PCCompanionParam.SELECTED_PROTOCOL.get().getName());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		{

			tableModel = new DefaultTableModel(0, 0);
			String header[] = new String[] { "id", "event", "flight mode", "id avoiding", "speed", "x", "y", "z" };
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

			connected = MBCAPDialog.connectedUAVs.get();
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
		}
	}

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
					tableModel.setValueAt(b.event, row, 1);
					tableModel.setValueAt(MBCAPParam.MBCAPState.getSatateById(b.state), row, 2);
					tableModel.setValueAt(b.idAvoiding, row, 3);
					tableModel.setValueAt(GUIHelper.round(b.speed, 3), row, 4);
					Point3D p = b.points.get(0);
					tableModel.setValueAt(GUIHelper.round(p.x, 3), row, 5);
					tableModel.setValueAt(GUIHelper.round(p.y, 3), row, 6);
					tableModel.setValueAt(GUIHelper.round(p.z, 2), row, 7);
				}
			});
		}
	}

	/** Listens for data packets on this protocol. */
	public void listenForPackets() {
		byte[] array;
		Beacon b;
//		if (Param.IS_REAL_UAV) {
			try {
				@SuppressWarnings("resource")
				DatagramSocket s = new DatagramSocket(UAVParam.BROADCAST_PORT);
				s.setBroadcast(true);
				DatagramPacket p = new DatagramPacket(new byte[UAVParam.DATAGRAM_MAX_LENGTH], UAVParam.DATAGRAM_MAX_LENGTH);
				while (true) {
					p.setData(new byte[UAVParam.DATAGRAM_MAX_LENGTH], 0, UAVParam.DATAGRAM_MAX_LENGTH);
					try {
						s.receive(p);
						array = p.getData();
						b = Beacon.getBeacon(array);
						updateRow(b);
					} catch (IOException e) {}
				}
			} catch (SocketException e) {
				GUIHelper.exit(Text.THREAD_START_ERROR);
			}
//		} else {
//			// Wait the dialog to be built, if needed
//			while (MBCAPDialog.mbcap.shown == null) {
//				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
//			}
//			int[] shownOrder = new int[Param.numUAVs];
//			boolean found;
//			long id;
//			int pos = -1;
//			for (int i = 0; i < Param.numUAVs; i++) {
//				found = false;
//				id = Param.id[i];
//				for (int j = 0; j < shown.length && !found; j++) {
//					if (id == shown[j].id) {
//						pos = j;
//						found = true;
//					}
//				}
//				if (found) {
//					shownOrder[i] = pos;
//				} else {
//					shownOrder[i] = -1;
//				}
//			}
//			Arrays.toString(shownOrder);
//			long time = System.currentTimeMillis();
//			long sleep;
//			while (true) {
//				for (int i = 0; i < Param.numUAVs; i++) {
//					if (shownOrder[i] != -1 && UAVParam.prevSentMessage.get(i) != null) {
//						array = UAVParam.prevSentMessage.get(i).message;
//						b = Beacon.getBeacon(array);
//						updateRow(b);
//					}
//				}
//				sleep = BoardParam.screenDelay - (System.currentTimeMillis() - time);
//				if (sleep > 0) {
//					GUIHelper.waiting((int)sleep);
//				}
//				time = time + BoardParam.screenDelay;
//			}
//		}
//		s.close();
	}

}
