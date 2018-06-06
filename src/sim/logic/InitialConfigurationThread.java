package sim.logic;

import java.util.concurrent.atomic.AtomicInteger;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.pojo.FlightMode;
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
		paramValue = Copter.getParameter(numUAV, ControllerParam.SINGLE_GCS);
		if (paramValue == null) {
			return;
		}
		if ((int)Math.round(paramValue) == 1) {
			if (!Copter.setParameter(numUAV, ControllerParam.SINGLE_GCS, 0)) {
				return;
			}
		}
		paramValue = Copter.getParameter(numUAV, ControllerParam.GCS_ID);
		if (paramValue == null) {
			return;
		}
		UAVParam.gcsId.set(numUAV, (int)Math.round(paramValue));
		
		// Ask the flight controller for information about battery usage (1Hz)
		if (!Copter.setParameter(numUAV, ControllerParam.STATISTICS, 1)) {
			return;
		}
		
		if (!Param.isRealUAV) {
			// Disable logging to speed up simulation
			if (!SimParam.arducopterLoggingEnabled && !Copter.setParameter(numUAV, ControllerParam.LOGGING, 0)) {
				return;
			}
			// Set simulated battery capacity
			if (!Copter.setParameter(numUAV, ControllerParam.BATTERY_CAPACITY, UAVParam.batteryCapacity)) {
				return;
			}
			if (UAVParam.batteryCapacity != UAVParam.MAX_BATTERY_CAPACITY
					&& (!Copter.setParameter(numUAV, ControllerParam.BATTERY_CURRENT_DEPLETION_THRESHOLD, UAVParam.batteryLowLevel)
							|| !Copter.setParameter(numUAV, ControllerParam.BATTERY_FAILSAFE_ACTION, UAVParam.BATTERY_DEPLETED_ACTION))) {
				// The parameter ControllerParam.BATTERY_VOLTAGE_DEPLETION_THRESHOLD cannot be set on simulation (voltages does not changes)
				return;
			}
			// Set simulated wind parameters
			if (!Copter.setParameter(numUAV, ControllerParam.WIND_DIRECTION, Param.windDirection)
					|| !Copter.setParameter(numUAV, ControllerParam.WIND_SPEED, Param.windSpeed)) {
				return;
			}
		} else {
			paramValue = Copter.getParameter(numUAV, ControllerParam.BATTERY_CAPACITY);
			if (paramValue == null) {
				return;
			}
			UAVParam.batteryCapacity = (int)Math.round(paramValue);
			if (UAVParam.batteryCapacity == 0) {
				paramValue = Copter.getParameter(numUAV, ControllerParam.BATTERY_CAPACITY2);
				if (paramValue == null) {
					return;
				}
				UAVParam.batteryCapacity = (int)Math.round(paramValue);
			}
			GUI.log(Text.BATTERY_SIZE + " " + UAVParam.batteryCapacity);
			UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
			if (UAVParam.batteryLowLevel % 50 != 0) {
				UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
			}
			GUI.log(Text.BATTERY_THRESHOLD + " " + UAVParam.batteryLowLevel + " (" + (UAVParam.BATTERY_DEPLETED_THRESHOLD * 100) + " %)");
		}
		
		// Get flight controller configuration
		// RC mapping
		paramValue = Copter.getParameter(numUAV, ControllerParam.RCMAP_ROLL);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapRoll.set(numUAV, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.RCMAP_PITCH);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapPitch.set(numUAV, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.RCMAP_THROTTLE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapThrottle.set(numUAV, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.RCMAP_YAW);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapYaw.set(numUAV, (int)Math.round(paramValue));
		// RC levels
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(0, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(0, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(0, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(1, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(1, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(1, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(2, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(2, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(2, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(3, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(3, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(3, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(4, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(4, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(4, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(5, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(5, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(5, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(6, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(6, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(6, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MIN_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV].set(7, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.TRIM_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV].set(7, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.MAX_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV].set(7, (int)Math.round(paramValue));
		// Stablish the middle throttle stick position
		int throttlePos = UAVParam.RCmapThrottle.get(numUAV);
		UAVParam.stabilizationThrottle[numUAV] = (UAVParam.RCmaxValue[numUAV].get(throttlePos - 1)
				+ UAVParam.RCminValue[numUAV].get(throttlePos - 1)) / 2;
		// Flight modes mapping
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE1);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(0, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE2);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(1, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE3);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(2, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE4);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(3, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE5);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(4, (int)Math.round(paramValue));
		paramValue = Copter.getParameter(numUAV, ControllerParam.FLTMODE6);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV].set(5, (int)Math.round(paramValue));
		// Mapping of custom flight mode to FLTMODE location in array
		int mode;
		// For each custom mode mapped in the remote control...
		for (int i = 0; i < UAVParam.flightModeMap[numUAV].length(); i++) {
			mode = UAVParam.flightModeMap[numUAV].get(i);
			// Store the FLTMODE it is mapped to
			UAVParam.customModeToFlightModeMap[numUAV][mode] = i + 1;
		}
		// Get the altitude used for RTL, and when the UAV reaches launch when using RTL
		paramValue = Copter.getParameter(numUAV, ControllerParam.RTL_ALTITUDE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RTLAltitude[numUAV] = paramValue*0.01;			// Data received in centimeters
		paramValue = Copter.getParameter(numUAV, ControllerParam.RTL_ALTITUDE_FINAL);
		if (paramValue == null) {
			return;
		}
		UAVParam.RTLAltitudeFinal[numUAV] = paramValue*0.01;	// Data received in centimeters
		
		// Set the speed when following a mission
		if (UAVParam.initialSpeeds[numUAV] != 0.0 && !Copter.setSpeed(numUAV, UAVParam.initialSpeeds[numUAV])) {
			return;
		}
		
		// Set STABILIZE flight mode if needed
		if (Copter.getFlightMode(numUAV) != FlightMode.STABILIZE) {
			Copter.setFlightMode(numUAV, FlightMode.STABILIZE);
		}
		
		if (!ProtocolHelper.selectedProtocolInstance.sendInitialConfiguration(numUAV)) {
			return;
		}
		
		// Configuration successful
		InitialConfigurationThread.UAVS_CONFIGURED.incrementAndGet();
	}
}
