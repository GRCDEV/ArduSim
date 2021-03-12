package com.api.masterslavepattern.discovery;

import com.api.communications.CommLink;
import com.api.masterslavepattern.MSMessageID;
import com.api.masterslavepattern.MSParam;
import com.esotericsoftware.kryo.io.Input;
import com.setup.Text;
import com.uavController.UAVParam;

/**
 * Thread used by slave UAVs to detect the master UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DiscoverSlaveListener extends Thread {

	private CommLink commLink;
	
	@SuppressWarnings("unused")
	private DiscoverSlaveListener() {}
	
	public DiscoverSlaveListener(int numUAV) {
		super(Text.DISCOVERY_SLAVE_LISTENER + numUAV);
		this.commLink = CommLink.getCommLink(numUAV, UAVParam.internalBroadcastPort);
	}
	
	@Override
	public void run() {
		
		byte[] inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Input input = new Input(inBuffer);
		
		long lastReceivedTime = 0;
		
		while (lastReceivedTime == 0 || System.currentTimeMillis() - lastReceivedTime <= MSParam.DISCOVERY_TIMEOUT) {
			inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.DISCOVERING) {
					lastReceivedTime = System.currentTimeMillis();
				}
			}
		}
		
		input.close();
	}

}
