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
		if (!Param.IS_REAL_UAV) {
			// Disable logging to speed up simulation
			if (!SimParam.arducopterLoggingEnabled && !API.setParam(numUAV, ControllerParam.LOGGING, 0)) {
				return;
			}
			// Ask the flight controller for information about battery usage (1Hz)
			if (!API.setParam(numUAV, ControllerParam.STATISTICS, 1)) {
				return;
			}
			// Set simulated battery capacity
			if (!API.setParam(numUAV, ControllerParam.BATTERY_CAPACITY, UAVParam.batteryCapacity)) {
				return;
			}
			if (UAVParam.batteryCapacity != UAVParam.MAX_BATTERY_CAPACITY
					&& (!API.setParam(numUAV, ControllerParam.BATTERY_CURRENT_DEPLETION_THRESHOLD, UAVParam.batteryLowLevel)
							|| !API.setParam(numUAV, ControllerParam.BATTERY_FAILSAFE_ACTION, UAVParam.BATTERY_DEPLETED_ACTION))) {
				// The parameter ControllerParam.BATTERY_VOLTAGE_DEPLETION_THRESHOLD cannot be set on simulation (voltages does not changes)
				return;
			}
			// Set simulated wind parameters
			if (!API.setParam(numUAV, ControllerParam.WIND_DIRECTION, Param.windDirection)
					|| !API.setParam(numUAV, ControllerParam.WIND_SPEED, Param.windSpeed)) {
				return;
			}
		}
		
		// Get flight controller configuration
		// Stablish the middle throttle stick position 
		if (!API.getParam(numUAV, ControllerParam.MAX_THROTTLE)) {
			return;
		}
		UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue[numUAV]);
		if (!API.getParam(numUAV, ControllerParam.MIN_THROTTLE)) {
			return;
		}
		UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue[numUAV])) / 2;
		// Get the altitude used for RTL, and when the UAV reaches launch when using RTL
		if (!API.getParam(numUAV, ControllerParam.RTL_ALTITUDE)) {
			return;
		}
		UAVParam.RTLAltitude[numUAV] = UAVParam.newParamValue[numUAV]*0.01;			// Data received in centimeters
		if (!API.getParam(numUAV, ControllerParam.RTL_ALTITUDE_FINAL)) {
			return;
		}
		UAVParam.RTLAltitudeFinal[numUAV] = UAVParam.newParamValue[numUAV]*0.01;	// Data received in centimeters
		
		// Set the speed when following a mission
		if (UAVParam.initialSpeeds[numUAV] != 0.0 && !API.setSpeed(numUAV, UAVParam.initialSpeeds[numUAV])) {
			return;
		}
		
		if (Param.simulationIsMissionBased) {
			if (!MissionHelper.sendBasicMissionConfig(numUAV)) {
				return;
			}
		} else {
			if (!SwarmHelper.sendBasicSwarmConfig(numUAV)) {
				return;
			}
		}
		
		// Configuration successful
		InitialConfigurationThread.UAVS_CONFIGURED.incrementAndGet();
	}
}
