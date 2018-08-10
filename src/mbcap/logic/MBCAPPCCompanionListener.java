package mbcap.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import api.GUI;
import api.Tools;
import main.Text;
import mbcap.gui.MBCAPPCCompanionDialog;
import mbcap.pojo.Beacon;

public class MBCAPPCCompanionListener extends Thread {
	
	private MBCAPPCCompanionDialog dialog;

	@SuppressWarnings("unused")
	private MBCAPPCCompanionListener() {}
	
	public MBCAPPCCompanionListener(MBCAPPCCompanionDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public void run() {
		byte[] array;
		Beacon b;
		try {
			@SuppressWarnings("resource")
			DatagramSocket s = new DatagramSocket(Tools.getUDPBroadcastPort());
			s.setBroadcast(true);
			DatagramPacket p = new DatagramPacket(new byte[Tools.DATAGRAM_MAX_LENGTH], Tools.DATAGRAM_MAX_LENGTH);
			while (true) {
				try {
					s.receive(p);
					array = p.getData();
					b = Beacon.getBeacon(array);
					this.dialog.updateRow(b);
				} catch (IOException e) {}
				p.setData(new byte[Tools.DATAGRAM_MAX_LENGTH], 0, Tools.DATAGRAM_MAX_LENGTH);
			}
//			s.close();
		} catch (SocketException e) {
			GUI.exit(Text.THREAD_START_ERROR);
		}
	}

}
