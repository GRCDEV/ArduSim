package com.api.communications;

import com.api.API;
import com.api.ArduSim;
import com.api.ArduSimTools;
import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.setup.Param;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

/** 
 * Thread used to periodically check if two UAVs can communicate.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class RangeCalculusThread extends Thread {
	
	private final ArduSim ardusim;
	public static final long RANGE_CHECK_PERIOD = 1000; // (ms) Time between UAVs range check
	
	public RangeCalculusThread() {
		this.ardusim = API.getArduSim();
	}
	
	@Override
	public void run() {
		long checkTime = System.currentTimeMillis();
		long waitingTime;
		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST
				|| Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
			boolean isInRange;
			if (UAVParam.distanceCalculusIsOnline) {
				SimParam.communicationsOnline = true;
				for (int i = 0; i < Param.numUAVs - 1; i++) {
					for (int j = i + 1; j < Param.numUAVs; j++) {
						isInRange = isInRange(UAVParam.distances[i][j].get());
						CommLinkObjectSimulation.isInRange[i][j].set(isInRange);
						CommLinkObjectSimulation.isInRange[j][i].set(isInRange);
					}
				}
			}
			checkTime = checkTime + RANGE_CHECK_PERIOD;
			waitingTime = checkTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
	}

	/** Checks if the data packet must arrive to the destination depending on distance and the wireless model used (only used on simulation). */
	private boolean isInRange(double distance) {
		switch (Param.selectedWirelessModel) {
			case NONE:
				return true;
			case FIXED_RANGE:
				return distance <= Param.fixedRange;
			case DISTANCE_5GHZ:
				return Math.random() >= (5.335*Math.pow(10, -7)*distance*distance + 3.395*Math.pow(10, -5)*distance);
			default:
				ArduSimTools.logGlobal("Selected WirelessModel does not exist");
				return false;
		}
	}

}
