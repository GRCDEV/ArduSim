package mbcap.logic;

import api.Copter;
import api.Tools;
import api.WaypointReachedListener;
import mbcap.gui.MBCAPGUIParam;
import mbcap.pojo.Beacon;

/** This class sends data packets to other UAVs, by real or simulated broadcast, so others can detect risk of collision.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class BeaconingThread extends Thread implements WaypointReachedListener {
	
	boolean finished = false;
	double distance = Double.MAX_VALUE;
	

	private int numUAV; // UAV identifier, beginning from 0

	@SuppressWarnings("unused")
	private BeaconingThread() {}

	public BeaconingThread(int numUAV) {
		this.numUAV = numUAV;
	}
	
	@Override
	public void onWaypointReached() {
		// Project the predicted path over the planned mission
		MBCAPParam.projectPath.set(numUAV, 1);
	}

	@Override
	public int getNumUAV() {
		return this.numUAV;
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
		Beacon selfBeacon = null;
		byte[] sendBuffer = null;
		int waitingTime;
		short prevState = 0;

		// Send beacons while the UAV is flying during the experiment
		// The protocol is stopped when two UAVs collide
		long cicleTime = System.currentTimeMillis();
		while (Tools.isExperimentInProgress() && Copter.isFlying(numUAV)
				&& !Tools.isCollisionDetected()) {
			// Each beacon is sent a number of times before renewing the predicted positions
			for (long i = 0; i < MBCAPParam.numBeacons; i++) {
				// The first time it is needed to calculate the predicted positions
				if (i == 0) {
					selfBeacon = Beacon.buildToSend(numUAV);
					MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
					sendBuffer = selfBeacon.getBuffer();
					prevState = selfBeacon.state;

					// Beacon store for logging purposes
					if (Tools.isVerboseStorageEnabled()
							&& Tools.getExperimentEndTime(numUAV) == 0) {
						MBCAPParam.beaconsStored[numUAV].add(selfBeacon.clone());
					}
				} else {
					// In any other case, only time, state and idAvoiding are updated
					sendBuffer = selfBeacon.getBufferUpdated();
					// Create a new beacon if the state is changed
					if (selfBeacon.state != prevState) {
						selfBeacon = Beacon.buildToSend(numUAV);
						MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
						sendBuffer = selfBeacon.getBuffer();
						prevState = selfBeacon.state;

						// Beacon store for logging purposes
						if (Tools.isVerboseStorageEnabled()
								&& Tools.getExperimentEndTime(numUAV) == 0) {
							MBCAPParam.beaconsStored[numUAV].add(selfBeacon.clone());
						}
						i = 0;	// Reset value to start again
					}
				}
				if (!selfBeacon.points.isEmpty()) {
					Copter.sendBroadcastMessage(numUAV, sendBuffer);
				}

				cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		
		// Stop sending and drawing future positions when the UAV lands
		MBCAPGUIParam.predictedLocation.set(numUAV, null);
	}

}
