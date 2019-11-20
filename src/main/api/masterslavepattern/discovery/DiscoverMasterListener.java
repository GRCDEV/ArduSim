package main.api.masterslavepattern.discovery;

import java.util.concurrent.ConcurrentHashMap;
import com.esotericsoftware.kryo.io.Input;

import es.upv.grc.mapper.Location2DUTM;
import main.Text;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;

/**
 * Thread used by the master UAV to detect slaves.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DiscoverMasterListener extends Thread {

	private ConcurrentHashMap<Long, Location2DUTM> UAVsDetected;
	private DiscoveryProgressListener listener;
	
	private InternalCommLink commLink;
	
	@SuppressWarnings("unused")
	private DiscoverMasterListener() {}
	
	public DiscoverMasterListener(int numUAV, ConcurrentHashMap<Long, Location2DUTM> UAVsDetected, DiscoveryProgressListener listener) {
		super(Text.DISCOVERY_MASTER_LISTENER + numUAV);
		this.UAVsDetected = UAVsDetected;
		this.listener = listener;
		this.commLink = InternalCommLink.getCommLink(numUAV);
	}
	
	@Override
	public void run() {
		
		byte[] inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Input input = new Input(inBuffer);
		Long idSlave;
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
