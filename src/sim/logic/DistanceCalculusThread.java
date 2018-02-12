package sim.logic;

import api.GUIHelper;
import main.Param;
import uavController.UAVParam;

public class DistanceCalculusThread extends Thread {

	@Override
	public void run() {
		long checkTime = System.currentTimeMillis();
		int waitingTime;
		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST
				|| Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			double distance;
			for (int i = 0; i < Param.numUAVs - 1; i++) {
				for (int j = i + 1; j < Param.numUAVs; j++) {
					distance = UAVParam.uavCurrentData[i].getUTMLocation().distance(UAVParam.uavCurrentData[j].getUTMLocation());
					UAVParam.distances[i][j].set(distance);
					UAVParam.distances[j][i].set(distance);
				}
			}
			UAVParam.distanceCalculusIsOnline = true;
			checkTime = checkTime + UAVParam.distanceCalculusPeriod;
			waitingTime = (int)(checkTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				GUIHelper.waiting(waitingTime);
			}
		}
		UAVParam.distanceCalculusIsOnline = false;
		
		
		
		
		
		
	}

}
