package mbcap.logic;

import java.util.Iterator;
import java.util.Map;

import api.Copter;
import api.Tools;
import mbcap.pojo.Beacon;

/** This class receives data packets and stores them for later analysis of risk of collision.
 * <p>It also simulates broadcast and checks if there is a collision when using the simulator. */

public class ReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	
	@SuppressWarnings("unused")
	private ReceiverThread() {}
	
	public ReceiverThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		while (!Tools.isExperimentInProgress() || !Copter.isFlying(numUAV)) {
			Tools.waiting(MBCAPParam.SHORT_WAITING_TIME);
		}
		
		
//		while (!Tools.areUAVsAvailable() || Tools.areUAVsReadyForSetup() || Tools.isSetupInProgress()	|| Tools.isSetupFinished()
//				|| (Tools.isExperimentInProgress() && !Copter.isFlying(numUAV))) {
//			Tools.waiting(MBCAPParam.SHORT_WAITING_TIME);
//		}
		
		long expirationCheckTime = System.currentTimeMillis();
		long selfId = Tools.getIdFromPos(numUAV);
		// If two UAVs collide, the protocol stops (when using the simulator)
		while (Tools.isExperimentInProgress()
				&& !Tools.isCollisionDetected()) {
			// Receive message
			Beacon beacon = Beacon.getBeacon(Copter.receiveMessage(numUAV)); // beacon.numUAV is already INVALID
			
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
