package sim.logic;

import api.GUI;
import api.Tools;
import main.Param;
import main.Text;
import uavController.UAVParam;

public class CollisionDetector extends Thread {

	@Override
	public void run() {
		
		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
			Tools.waiting(SimParam.LONG_WAITING_TIME);
		}
		
		long checkTime = System.currentTimeMillis();
		int waitingTime;
		while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS && !UAVParam.collisionDetected) {
			if (UAVParam.distanceCalculusIsOnline) {
				for (int i = 0; i < Param.numUAVs && !UAVParam.collisionDetected; i++) {
					for (int j = i + 1; j < Param.numUAVs && !UAVParam.collisionDetected; j++) {
						if (UAVParam.distances[i][j].get() < UAVParam.collisionDistance
								&& Math.abs(UAVParam.uavCurrentData[i].getZ()-UAVParam.uavCurrentData[j].getZ()) < UAVParam.collisionAltitudeDifference) {
							GUI.log(Text.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + ".");
							GUI.updateGlobalInformation(Text.COLLISION_DETECTED);
							// The protocols must be stopped
							UAVParam.collisionDetected = true;
							SimTools.landAllUAVs();
							// Advising the user
							GUI.warn(Text.COLLISION_TITLE, Text.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
						}
					}
				}
			}
			checkTime = checkTime + UAVParam.appliedCollisionCheckPeriod;
			waitingTime = (int)(checkTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				Tools.waiting(waitingTime);
			}
		}
	}
}
