package mbcap.logic;

import api.API;
import api.GUIHelper;
import main.Param;
import main.Param.SimulatorState;
import mbcap.gui.MBCAPGUIParam;
import mbcap.pojo.Beacon;
import sim.logic.SimParam;
import uavController.UAVParam;
import uavController.WaypointReachedListener;

/** This class sends data packets to other UAVs, by real or simulated broadcast, so others can detect risk of collision. */

public class BeaconingThread extends Thread implements WaypointReachedListener {

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
	public void run() {
		while (Param.simStatus == Param.SimulatorState.CONFIGURING
				|| Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL
				|| Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST
				|| (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS
					&& UAVParam.flightMode.get(numUAV).getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING)) {
			GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
		}
		Beacon selfBeacon = null;
		byte[] sendBuffer = null;
		int waitingTime;

		// The protocol is stopped when two UAVs collide
		long cicleTime = System.currentTimeMillis();
		while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
				&& UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
				&& !UAVParam.collisionDetected) {
			SimulatorState state = Param.simStatus;
			// Send beacons while the UAV is flying during the experiment
			if (state == SimulatorState.TEST_IN_PROGRESS) {
				// Each beacon is sent a number of times before renewing the predicted positions
				for (int i = 0; i < MBCAPParam.numBeacons; i++) {
					// The first time it is needed to calculate the predicted positions
					if (i == 0) {
						selfBeacon = Beacon.buildToSend(numUAV);
						MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
						sendBuffer = selfBeacon.getBuffer();

						// Beacon store for logging purposes
						if (Param.VERBOSE_STORE
								&& state == SimulatorState.TEST_IN_PROGRESS
								&& Param.testEndTime[numUAV] == 0) {
							MBCAPParam.beaconsStored[numUAV].add(selfBeacon.clone());
						}
					} else {
						// In any other case, only time, state and idAvoiding are updated
						sendBuffer = selfBeacon.getBufferUpdated();
					}
					if (!selfBeacon.points.isEmpty()) {
						API.sendBroadcastMessage(numUAV, sendBuffer);
					}

					cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						GUIHelper.waiting(waitingTime);
					}
				}
			}
		}
		
		// Stop sending and drawing future positions when the UAV lands
		MBCAPGUIParam.predictedLocation.set(numUAV, null);
	}

}
