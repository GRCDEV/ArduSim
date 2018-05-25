package sim.logic;

import java.util.concurrent.atomic.AtomicInteger;

import api.API;
import api.MissionHelper;
import api.SwarmHelper;
import main.Param;
import main.Text;
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
		Double paramValue;
		// Determining the GCS identifier that must be used
		paramValue = API.getParam(numUAV, ControllerParam.SINGLE_GCS);
		if (paramValue == null) {
			return;
		}
		if ((int)Math.round(paramValue) == 1) {
			if (!API.setParam(numUAV, ControllerParam.SINGLE_GCS, 0)) {
				return;
			}
		}
		paramValue = API.getParam(numUAV, ControllerParam.GCS_ID);
		if (paramValue == null) {
			return;
		}
		UAVParam.gcsId.set(numUAV, (int)Math.round(paramValue));
		
		// Ask the flight controller for information about battery usage (1Hz)
		if (!API.setParam(numUAV, ControllerParam.STATISTICS, 1)) {
			return;
		}
		
		if (!Param.IS_REAL_UAV) {
			// Disable logging to speed up simulation
			if (!SimParam.arducopterLoggingEnabled && !API.setParam(numUAV, ControllerParam.LOGGING, 0)) {
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
		} else {
			paramValue = API.getParam(numUAV, ControllerParam.BATTERY_CAPACITY);
			if (paramValue == null) {
				return;
			}
			UAVParam.batteryCapacity = (int)Math.round(paramValue);
			if (UAVParam.batteryCapacity == 0) {
				paramValue = API.getParam(numUAV, ControllerParam.BATTERY_CAPACITY2);
				if (paramValue == null) {
					return;
				}
				UAVParam.batteryCapacity = (int)Math.round(paramValue);
			}
			SimTools.println(Text.BATTERY_SIZE + " " + UAVParam.batteryCapacity);
			UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
			if (UAVParam.batteryLowLevel % 50 != 0) {
				UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
			}
			SimTools.println(Text.BATTERY_THRESHOLD + " " + UAVParam.batteryLowLevel + " (" + (UAVParam.BATTERY_DEPLETED_THRESHOLD * 100) + " %)");
		}
		
		// Get flight controller configuration
		// Stablish the middle throttle stick position
		paramValue = API.getParam(numUAV, ControllerParam.RCMAP_THROTTLE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapThrottle.set(numUAV, (int)Math.round(paramValue));
		switch (UAVParam.RCmapThrottle.get(numUAV)) {
		case 1:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC1);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC1);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 2:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC2);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC2);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 3:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC3);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC3);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 4:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC4);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC4);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 5:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC5);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC5);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 6:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC6);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC6);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 7:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC7);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC7);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		case 8:
			paramValue = API.getParam(numUAV, ControllerParam.MAX_RC8);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(paramValue);
			paramValue = API.getParam(numUAV, ControllerParam.MIN_RC8);
			if (paramValue == null) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(paramValue)) / 2;
			break;
		}
		// Get the altitude used for RTL, and when the UAV reaches launch when using RTL
		paramValue = API.getParam(numUAV, ControllerParam.RTL_ALTITUDE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RTLAltitude[numUAV] = paramValue*0.01;			// Data received in centimeters
		paramValue = API.getParam(numUAV, ControllerParam.RTL_ALTITUDE_FINAL);
		if (paramValue == null) {
			return;
		}
		UAVParam.RTLAltitudeFinal[numUAV] = paramValue*0.01;	// Data received in centimeters
		
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
		}//TODO uncomment
		
		// Configuration successful
		InitialConfigurationThread.UAVS_CONFIGURED.incrementAndGet();
	}
}
