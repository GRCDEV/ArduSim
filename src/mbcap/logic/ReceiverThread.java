package mbcap.logic;

import java.util.Iterator;
import java.util.Map;

import api.API;
import main.api.ArduSim;
import main.api.Copter;
import main.api.communications.CommLink;
import mbcap.pojo.Beacon;

/** This class receives data packets and stores them for later analysis of risk of collision.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	private CommLink link;
	private Copter copter;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private ReceiverThread() {}
	
	public ReceiverThread(int numUAV) {
		this.numUAV = numUAV;
		this.link = API.getCommLink(numUAV);
		this.copter = API.getCopter(numUAV);
		this.ardusim = API.getArduSim();
	}

	@Override
	public void run() {
		while (!ardusim.isExperimentInProgress() || !copter.isFlying()) {
			ardusim.sleep(MBCAPParam.SHORT_WAITING_TIME);
		}
		
		long expirationCheckTime = System.currentTimeMillis();
		long selfId = API.getCopter(numUAV).getID();
		// If two UAVs collide, the protocol stops (when using the simulator)
		ArduSim ardusim = API.getArduSim();
		while (ardusim.isExperimentInProgress()
				&& !ardusim.collisionIsDetected()) {
			// Receive message
			Beacon beacon = Beacon.getBeacon(link.receiveMessage()); // beacon.numUAV is already INVALID
			
			// Periodic cleanup of obsolete beacons to take into account UAVs that have gone far enough
			long now = System.currentTimeMillis();
			if (now > expirationCheckTime + MBCAPParam.BEACON_EXPIRATION_CHECK_PERIOD) {
				Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
				while (entries.hasNext()) {
					Map.Entry<Long, Beacon> entry = entries.next();
					if (System.nanoTime() - entry.getValue().time >= MBCAPParam.beaconExpirationTime) {
						entries.remove();
					}
				}
				expirationCheckTime = now;
			}
			
			// Ignoring beacons without useful information
			if (beacon != null && beacon.points.size() > 0) {
				// On real UAVs, the broadcast is also received by the sender
				if (beacon.uavId != selfId) {
					MBCAPParam.beacons[numUAV].put(beacon.uavId, beacon);
				}
			}
		}
	}
}
