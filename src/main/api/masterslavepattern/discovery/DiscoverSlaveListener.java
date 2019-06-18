package main.api.masterslavepattern.discovery;

import com.esotericsoftware.kryo.io.Input;

import main.Text;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;

/**
 * Thread used by slave UAVs to detect the master UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DiscoverSlaveListener extends Thread {
	
	private InternalCommLink commLink;
	
	@SuppressWarnings("unused")
	private DiscoverSlaveListener() {}
	
	public DiscoverSlaveListener(int numUAV) {
		super(Text.DISCOVERY_SLAVE_LISTENER + numUAV);
		this.commLink = InternalCommLink.getCommLink(numUAV);
	}
	
	@Override
	public void run() {
		
		byte[] inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Input input = new Input(inBuffer);
		
		boolean masterFound = false;
		long lastReceivedTime = 0;
		
		while (!masterFound || System.currentTimeMillis() - lastReceivedTime <= MSParam.DISCOVERY_TIMEOUT) {
			inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.DISCOVERING) {
					masterFound = true;
					lastReceivedTime = System.currentTimeMillis();
				}
			}
		}
		
		input.close();
	}

}
