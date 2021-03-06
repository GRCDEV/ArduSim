package com.protocols.mbcap.logic;

import com.api.communications.CommLink;
import com.esotericsoftware.kryo.io.Input;
import com.protocols.mbcap.gui.MBCAPPCCompanionDialog;
import com.protocols.mbcap.pojo.Beacon;

/** 
 * Thread used in the PC Companion to receive messages from the UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPPCCompanionListener extends Thread {
	
	private MBCAPPCCompanionDialog dialog;
	
	private CommLink link;
	private byte[] inBuffer;
	private Input input;

	@SuppressWarnings("unused")
	private MBCAPPCCompanionListener() {}
	
	public MBCAPPCCompanionListener(MBCAPPCCompanionDialog dialog) {
		this.dialog = dialog;
		this.link = CommLink.getCommLink(0);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
	}
	
	@Override
	public void run() {
		Beacon b;
		while(true) {
			b = ReceiverThread.getBeacon(link.receiveMessage(), input);
			this.dialog.updateRow(b);
		}
	}

}
