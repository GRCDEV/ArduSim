package com.setup.sim.logic;

import com.api.API;
import com.setup.Param;
import com.api.ArduSim;
import com.uavController.UAVParam;

/** 
 * Thread used to calculate the distance between UAVs on real-time. We use this results to check if there is a collision between two UAVs, and to check if a UAV in in range and can receive a message form another UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DistanceCalculusThread extends Thread {

	@Override
	public void run() {
		ArduSim ardusim = API.getArduSim();
		long checkTime = System.currentTimeMillis();
		long waitingTime;
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
			waitingTime = checkTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		UAVParam.distanceCalculusIsOnline = false;

	}

}
