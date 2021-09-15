package com.setup;

import com.api.*;
import com.api.API;
import com.api.copter.CopterParam;
import com.api.pojo.FlightMode;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.hiddenFunctions.HiddenFunctions;
import com.setup.sim.gui.MissionKmlSimProperties;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.util.concurrent.atomic.AtomicInteger;

/** This class sends the initial configuration to all UAVs, asynchronously.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class InitialConfiguration1Thread extends Thread {
	
	public static final AtomicInteger UAVS_CONFIGURED = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private InitialConfiguration1Thread() {}
	
	public InitialConfiguration1Thread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		InitialConfiguration1Thread.sendBasicConfiguration(numUAV);
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, loads missions..., to a specific UAV. */
	public static void sendBasicConfiguration(int numUAV) {
		Copter copter = API.getCopter(numUAV);
		
		// Load all parameters and get the ArduCopter compilation version
		ArduSimTools.getArduCopterParameters(numUAV);
		
		if (UAVParam.totParams[numUAV] != UAVParam.loadedParams[numUAV].size()) {
			for (int i = 0; i < UAVParam.totParams[numUAV]; i++) {
				if (!UAVParam.paramLoaded[numUAV][i]) {
					if (HiddenFunctions.getParameter(numUAV, i) == null) return;
				}
			}
		}
		// Follows a redundant check
		if (UAVParam.totParams[numUAV] != UAVParam.loadedParams[numUAV].size()) {
			return;
		}
		
		/*
		CopterParamLoaded[] values = UAVParam.loadedParams[numUAV].values().toArray(new CopterParamLoaded[0]);
		Arrays.sort(values);
		for (int i = 0; i < values.length; i++) {
			System.out.println(values[i]);
		}
		*/
		
		// Determining the GCS identifier that must be used
		Double paramValue;
		paramValue = copter.getParameter(CopterParam.SINGLE_GCS);
		if (paramValue == null) {
			return;
		}
		if ((int)Math.rint(paramValue) == 1) {
			if (!copter.setParameter(CopterParam.SINGLE_GCS, 0)) {
				return;
			}
		}
		paramValue = copter.getParameter(CopterParam.GCS_ID);
		if (paramValue == null) {
			return;
		}
		UAVParam.gcsId.set(numUAV, (int)Math.rint(paramValue));
		
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			// Ask the flight controller for information about battery usage (1Hz)
			if (!copter.setParameter(CopterParam.STATISTICS, 1)) {
				return;
			}
			// Disable logging to speed up simulation
			if (!SimParam.arducopterLoggingEnabled && !copter.setParameter(CopterParam.LOGGING, 0)) {
				return;
			}
			// Set simulated battery capacity
			if (!copter.setParameter(CopterParam.BATTERY_CAPACITY, UAVParam.batteryCapacity)) {
				return;
			}
			if (UAVParam.batteryCapacity != UAVParam.VIRT_BATTERY_MAX_CAPACITY
					&& (!copter.setParameter(CopterParam.BATTERY_CURRENT_DEPLETION_THRESHOLD, UAVParam.batteryLowLevel)
							|| !copter.setParameter(CopterParam.BATTERY_FAILSAFE_ACTION, UAVParam.BATTERY_DEPLETED_ACTION))) {
				// The parameter CopterParam.BATTERY_VOLTAGE_DEPLETION_THRESHOLD cannot be set on simulation (voltages does not changes)
				return;
			}
			// Set simulated wind parameters
			if (!copter.setParameter(CopterParam.WIND_DIRECTION, Param.windDirection)
					|| !copter.setParameter(CopterParam.WIND_SPEED, Param.windSpeed)) {
				return;
			}
		} else if (Param.role == ArduSim.MULTICOPTER) {
			paramValue = copter.getParameter(CopterParam.BATTERY_CAPACITY);
			if (paramValue == null) {
				return;
			}
			UAVParam.batteryCapacity = (int)Math.rint(paramValue);
			if (UAVParam.batteryCapacity == 0) {
				paramValue = copter.getParameter(CopterParam.BATTERY_CAPACITY2);
				if (paramValue == null) {
					return;
				}
				UAVParam.batteryCapacity = (int)Math.rint(paramValue);
			}
			ArduSimTools.logVerboseGlobal(Text.BATTERY_SIZE + " " + UAVParam.batteryCapacity);
			UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
			if (UAVParam.batteryLowLevel % 50 != 0) {
				UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
			}
			ArduSimTools.logVerboseGlobal(Text.BATTERY_THRESHOLD + " " + UAVParam.batteryLowLevel + " (" + (UAVParam.BATTERY_DEPLETED_THRESHOLD * 100) + " %)");
		}
		
		// Get flight controller configuration
		// RC mapping
		paramValue = copter.getParameter(CopterParam.RCMAP_ROLL);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapRoll[numUAV] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.RCMAP_PITCH);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapPitch[numUAV] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.RCMAP_THROTTLE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapThrottle[numUAV] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.RCMAP_YAW);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmapYaw[numUAV] = (int)Math.rint(paramValue);
		// RC levels
		paramValue = copter.getParameter(CopterParam.MIN_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][0] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][0] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][0] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC1);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][0] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][1] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][1] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][1] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC2);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][1] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][2] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][2] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][2] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC3);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][2] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][3] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][3] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][3] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC4);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][3] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][4] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][4] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][4] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC5);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][4] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][5] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][5] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][5] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC6);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][5] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][6] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][6] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][6] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC7);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][6] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MIN_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCminValue[numUAV][7] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.TRIM_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCtrimValue[numUAV][7] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.MAX_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCmaxValue[numUAV][7] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.DZ_RC8);
		if (paramValue == null) {
			return;
		}
		UAVParam.RCDZValue[numUAV][7] = (int)Math.rint(paramValue);
		// Stablish the middle throttle stick position
		int throttlePos = UAVParam.RCmapThrottle[numUAV];
		UAVParam.stabilizationThrottle[numUAV] = (UAVParam.RCmaxValue[numUAV][throttlePos - 1]
				+ UAVParam.RCminValue[numUAV][throttlePos - 1]) / 2;
		// Throttle dead zone
		paramValue = copter.getParameter(CopterParam.DZ_THROTTLE);
		if (paramValue == null) {
			return;
		}
		UAVParam.throttleDZ[numUAV] = (int)Math.rint(paramValue);
		// Flight modes mapping
		paramValue = copter.getParameter(CopterParam.FLTMODE1);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][0] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.FLTMODE2);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][1] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.FLTMODE3);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][2] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.FLTMODE4);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][3] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.FLTMODE5);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][4] = (int)Math.rint(paramValue);
		paramValue = copter.getParameter(CopterParam.FLTMODE6);
		if (paramValue == null) {
			return;
		}
		UAVParam.flightModeMap[numUAV][5] = (int)Math.rint(paramValue);
		// Mapping of custom flight mode to FLTMODE location in array
		int mode;
		// For each custom mode mapped in the remote control...
		for (int i = 0; i < UAVParam.flightModeMap[numUAV].length; i++) {
			mode = UAVParam.flightModeMap[numUAV][i];
			// Store the FLTMODE it is mapped to
			UAVParam.customModeToFlightModeMap[numUAV][mode] = i + 1;
		}
		// Get the altitude used for RTL, and when the UAV reaches launch when using RTL
		paramValue = copter.getParameter(CopterParam.RTL_ALTITUDE);
		if (paramValue == null) {
			return;
		}
		UAVParam.RTLAltitude[numUAV] = paramValue*0.01;			// Data received in centimeters
		
		if (MissionKmlSimProperties.missionEnd.equals(MissionKmlSimProperties.MISSION_END_RTL)) {
			if (!copter.setParameter(CopterParam.RTL_ALTITUDE_FINAL, MissionKmlSimProperties.finalAltitudeForRTL * 100)) {
				return;
			}
			UAVParam.RTLAltitudeFinal[numUAV] = MissionKmlSimProperties.finalAltitudeForRTL;
		} else {
			paramValue = copter.getParameter(CopterParam.RTL_ALTITUDE_FINAL);
			if (paramValue == null) {
				return;
			}
			UAVParam.RTLAltitudeFinal[numUAV] = paramValue * 0.01;	// Data received in centimeters
		}
		
		// Set the speed when following a main.java.com.protocols.mission
		if (UAVParam.initialSpeeds[numUAV] != 0.0 && !copter.setPlannedSpeed(UAVParam.initialSpeeds[numUAV])) {
			return;
		}
		
		// Set STABILIZE flight mode if needed
		if (UAVParam.flightMode.get(numUAV) != FlightMode.STABILIZE) {
			if (!copter.setFlightMode(FlightMode.STABILIZE)) {
				return;
			}
		}
		// Configuration successful
		InitialConfiguration1Thread.UAVS_CONFIGURED.incrementAndGet();
	}
}
