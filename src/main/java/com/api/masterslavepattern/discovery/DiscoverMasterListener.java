package com.api.masterslavepattern.discovery;

import com.api.communications.CommLink;
import com.api.masterslavepattern.MSMessageID;
import com.api.masterslavepattern.MSParam;
import com.esotericsoftware.kryo.io.Input;
import com.setup.Text;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DUTM;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread used by the master UAV to detect slaves.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DiscoverMasterListener extends Thread {

	private ConcurrentHashMap<Long, Location2DUTM> UAVsDetected;
	private DiscoveryProgressListener listener;
	private CommLink commLink;
	
	@SuppressWarnings("unused")
	private DiscoverMasterListener() {}
	
	public DiscoverMasterListener(int numUAV, ConcurrentHashMap<Long, Location2DUTM> UAVsDetected, DiscoveryProgressListener listener) {
		super(Text.DISCOVERY_MASTER_LISTENER + numUAV);
		this.UAVsDetected = UAVsDetected;
		this.listener = listener;
		this.commLink = CommLink.getCommLink(numUAV, UAVParam.internalBroadcastPort);
	}
	
	@Override
	public void run() {
		
		byte[] inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Input input = new Input(inBuffer);
		long idSlave;
		Location2DUTM slaveLocation;
		
		while (!listener.onProgressCheckActionPerformed(UAVsDetected.size())) {
			
			inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.HELLO) {
					// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
					idSlave = input.readLong();
					slaveLocation = new Location2DUTM(input.readDouble(), input.readDouble());
					// ADD to Map the new UAV detected
					UAVsDetected.put(idSlave, slaveLocation);
				}
			}
		}
		
		input.close();
	}

}
