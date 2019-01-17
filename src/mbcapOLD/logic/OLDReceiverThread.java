package mbcapOLD.logic;

import java.util.Iterator;
import java.util.Map;

import api.Copter;
import api.Tools;
import main.Param;
import main.Param.SimulatorState;
import mbcapOLD.pojo.OLDBeacon;
import sim.logic.SimParam;
import uavController.UAVParam;

/** This class receives data packets and stores them for later analysis of risk of collision.
 * <p>It also simulates broadcast and checks if there is a collision when using the simulator. */

public class OLDReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	
	@SuppressWarnings("unused")
	private OLDReceiverThread() {}
	
	public OLDReceiverThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		while (Param.simStatus == Param.SimulatorState.CONFIGURING
				|| Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL
				|| Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST
				|| (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS
					&& UAVParam.flightMode.get(numUAV).getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING)) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}
		
		long expirationCheckTime = System.currentTimeMillis();
		// If two UAVs collide, the protocol stops (when using the simulator)
		while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
				&& !UAVParam.collisionDetected) {
			// Receive message
			OLDBeacon beacon = OLDBeacon.getBeacon(Copter.receiveMessage(numUAV)); // beacon.numUAV is already INVALID
			
			// Periodic cleanup of obsolete beacons to take into account UAVs that have gone far enough
			long now = System.currentTimeMillis();
			if (now > expirationCheckTime + OLDMBCAPParam.BEACON_EXPIRATION_CHECK_PERIOD) {
				Iterator<Map.Entry<Long, OLDBeacon>> entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
				while (entries.hasNext()) {
					Map.Entry<Long, OLDBeacon> entry = entries.next();
					if (System.nanoTime() - entry.getValue().time >= OLDMBCAPParam.beaconExpirationTime) {
						entries.remove();
					}
				}
				expirationCheckTime = now;
			}
			
			// Ignoring beacons without useful information
			if (beacon != null && beacon.points.size() > 0) {
				OLDMBCAPParam.beacons[numUAV].put(beacon.uavId, beacon);
			}
		}
	}
}
