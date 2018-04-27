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
		// Determining the GCS identifier that must be used
		if (!API.getParam(numUAV, ControllerParam.SINGLE_GCS)) {
			return;
		}
		if ((int)Math.round(UAVParam.newParamValue.get(numUAV)) == 1) {
			if (!API.setParam(numUAV, ControllerParam.SINGLE_GCS, 0)) {
				return;
			}
		}
		if (!API.getParam(numUAV, ControllerParam.GCS_ID)) {
			return;
		}
		UAVParam.gcsId.set(numUAV, (int)Math.round(UAVParam.newParamValue.get(numUAV)));
		
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
			if (!API.getParam(numUAV, ControllerParam.BATTERY_CAPACITY)) {
				return;
			}
			UAVParam.batteryCapacity = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (UAVParam.batteryCapacity == 0 && !API.getParam(numUAV, ControllerParam.BATTERY_CAPACITY2)) {
				return;
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
		if (!API.getParam(numUAV, ControllerParam.RCMAP_THROTTLE)) {
			return;
		}
		UAVParam.RCmapThrottle.set(numUAV, (int)Math.round(UAVParam.newParamValue.get(numUAV)));
		switch (UAVParam.RCmapThrottle.get(numUAV)) {
		case 1:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC1)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC1)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 2:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC2)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC2)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 3:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC3)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC3)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 4:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC4)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC4)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 5:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC5)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC5)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 6:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC6)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC6)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 7:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC7)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC7)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		case 8:
			if (!API.getParam(numUAV, ControllerParam.MAX_RC8)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (int)Math.round(UAVParam.newParamValue.get(numUAV));
			if (!API.getParam(numUAV, ControllerParam.MIN_RC8)) {
				return;
			}
			UAVParam.stabilizationThrottle[numUAV] = (UAVParam.stabilizationThrottle[numUAV] + (int)Math.round(UAVParam.newParamValue.get(numUAV))) / 2;
			break;
		}
		// Get the altitude used for RTL, and when the UAV reaches launch when using RTL
		if (!API.getParam(numUAV, ControllerParam.RTL_ALTITUDE)) {
			return;
		}
		UAVParam.RTLAltitude[numUAV] = UAVParam.newParamValue.get(numUAV)*0.01;			// Data received in centimeters
		if (!API.getParam(numUAV, ControllerParam.RTL_ALTITUDE_FINAL)) {
			return;
		}
		UAVParam.RTLAltitudeFinal[numUAV] = UAVParam.newParamValue.get(numUAV)*0.01;	// Data received in centimeters
		
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
