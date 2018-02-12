package sim.logic;

import api.GUIHelper;
import main.Param;
import mbcap.logic.MBCAPHelper;
import mbcap.logic.MBCAPText;
import uavController.UAVParam;

public class CollisionDetector extends Thread {

	@Override
	public void run() {
		
		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
			GUIHelper.waiting(SimParam.LONG_WAITING_TIME);
		}
		
		long checkTime = System.currentTimeMillis();
		int waitingTime;
		while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS && !UAVParam.collisionDetected) {
			if (UAVParam.distanceCalculusIsOnline) {
				for (int i = 0; i < Param.numUAVs && !UAVParam.collisionDetected; i++) {
					for (int j = i + 1; j < Param.numUAVs && !UAVParam.collisionDetected; j++) {
						if (UAVParam.distances[i][j].get() < UAVParam.collisionDistance
								&& Math.abs(UAVParam.uavCurrentData[i].getZ()-UAVParam.uavCurrentData[j].getZ()) < UAVParam.collisionAltitudeDifference) {
							SimTools.println(MBCAPText.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + ".");
							SimTools.updateGlobalInformation(MBCAPText.COLLISION_DETECTED);
							// The protocols must be stopped
							UAVParam.collisionDetected = true;
							MBCAPHelper.landAllUAVs();
							// Advising the user
							GUIHelper.warn(MBCAPText.COLLISION_TITLE, MBCAPText.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
						}
					}
				}
			}
			checkTime = checkTime + UAVParam.appliedCollisionCheckPeriod;
			waitingTime = (int)(checkTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				GUIHelper.waiting(waitingTime);
			}
		}
	}
}
