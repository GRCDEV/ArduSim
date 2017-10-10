package sim.logic;

import java.util.concurrent.atomic.AtomicInteger;

import api.API;
import api.MissionHelper;
import api.SwarmHelper;
import main.Param;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;

/** This class sends the initial configuration to all UAVs, asynchronously. */

public class InitialConfigurationThread extends Thread {
	
	public static final AtomicInteger UAVS_CONFIGURED = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private InitialConfigurationThread() {}
	
	public InitialConfigurationThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		InitialConfigurationThread.sendBasicConfiguration(numUAV);
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, loads missions..., to a specific UAV. */
	public static void sendBasicConfiguration(int numUAV) {
		boolean success = false;
		if (!Param.IS_REAL_UAV
				&& API.setParam(numUAV, ControllerParam.BATTERY_CAPACITY, UAVParam.BATTERY_CHARGE)
				&& API.setParam(numUAV, ControllerParam.WIND_DIRECTION, Param.windDirection)
				&& API.setParam(numUAV, ControllerParam.WIND_SPEED, Param.windSpeed)
				&& API.setSpeed(numUAV, UAVParam.initialSpeeds[numUAV])
				&& API.getParam(numUAV, ControllerParam.MAX_THROTTLE)) {
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue[numUAV]);
			if (API.getParam(numUAV, ControllerParam.MIN_THROTTLE)) {
				UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue[numUAV])) / 2;
				if (API.getParam(numUAV, ControllerParam.RTL_ALTITUDE)) {
					UAVParam.RTLAltitude[numUAV] = UAVParam.newParamValue[numUAV]*0.01; // Data received in centimeters
					if (API.getParam(numUAV, ControllerParam.RTL_ALTITUDE_FINAL)) {
						UAVParam.RTLAltitudeFinal[numUAV] = UAVParam.newParamValue[numUAV];
						if (Param.simulationIsMissionBased) {
							success = MissionHelper.sendBasicMissionConfig(numUAV);
						} else {
							success = SwarmHelper.sendBasicSwarmConfig(numUAV);
						}
						if (success) {
							InitialConfigurationThread.UAVS_CONFIGURED.incrementAndGet();
						}
					}
				}
			}
		}
	}
}
