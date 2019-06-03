package mbcap.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import api.API;
import main.Text;
import main.api.communications.CommLink;
import mbcap.gui.MBCAPPCCompanionDialog;
import mbcap.pojo.Beacon;

/** 
 * Thread used in the PC Companion to receive messages from the UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

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
			DatagramSocket s = new DatagramSocket(API.getArduSim().getUDPBroadcastPort());
			s.setBroadcast(true);
			DatagramPacket p = new DatagramPacket(new byte[CommLink.DATAGRAM_MAX_LENGTH], CommLink.DATAGRAM_MAX_LENGTH);
			while (true) {
				try {
					s.receive(p);
					array = p.getData();
					b = Beacon.getBeacon(array);
					this.dialog.updateRow(b);
				} catch (IOException e) {}
				p.setData(new byte[CommLink.DATAGRAM_MAX_LENGTH], 0, CommLink.DATAGRAM_MAX_LENGTH);
			}
//			s.close();
		} catch (SocketException e) {
			API.getGUI(0).exit(Text.THREAD_START_ERROR);
		}
	}

}
