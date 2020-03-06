package main.api.hiddenFunctions;

import api.API;
import main.ArduSimTools;
import main.Text;
import main.api.ArduSim;
import main.sim.logic.SimParam;
import main.uavController.UAVParam;

/**
 * Functions related to MAVLink communications that are hidden to the user because they are already used by higher logic level functions.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class HiddenFunctions {
	
	/**
	 * Arm the engines.
	 * <p>Previously, on a real UAV you have to press the hardware switch for safety arm, if available.
	 * The UAV must be in an armable flight mode (STABILIZE, LOITER, ALT_HOLD, GUIDED).</p>
	 * @param numUAV Position of the UAV in the stored arrays.
	 * @return true if the command was successful.
	 */
	public static boolean armEngines(int numUAV) {
		ArduSim ardusim = API.getArduSim();
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_ARM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_ARM) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_ARM) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.ARM_ENGINES_ERROR);
			return false;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.ARM_ENGINES);
			return true;
		}
	}
	
	/**
	 * Center the joysticks of the virtual remote control to stabilize the UAV.
	 * <p>Useful to stabilize altitude before taking off, or for starting AUTO flight while being on the ground.</p>
	 * @param numUAV Position of the UAV in the stored arrays.
	 * @return true if the command was successful.
	 */
	public static  boolean stabilize(int numUAV) {
		if (UAVParam.overrideOn.get(numUAV) == 1) {
			ArduSim ardusim = API.getArduSim();
			while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
				ardusim.sleep(UAVParam.COMMAND_WAIT);
			}
			UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_THROTTLE_ON);
			while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
					&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
				ardusim.sleep(UAVParam.COMMAND_WAIT);
			}
			if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
				ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE_ERROR);
				return false;
			} else {
				ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE);
				return true;
			}
		}
		ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.RC_CHANNELS_OVERRIDE_FORBIDEN_ERROR);
		return false;
	}
	
	/**
	 * Take off a previously armed UAV.
	 * <p>The UAV must be in GUIDED mode and armed.
	 * Previously, on a real UAV you have to press the hardware switch for safety arm, if available.</p>
	 * @param numUAV Position of the UAV in the stored arrays.
	 * @param altitude Target altitude over the home location.
	 * @return true if the command was successful.
	 */
	public static boolean takeOffGuided(int numUAV, double altitude) {
		ArduSim ardusim = API.getArduSim();
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.takeOffAltitude.set(numUAV, altitude);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_TAKE_OFF);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.TAKE_OFF_ERROR_2);
			return false;
		} else {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.TAKE_OFF);
			return true;
		}
	}
	
	/**
	 * Get the value of a flight controller parameter.
	 * @param numUAV Position of the UAV in the stored arrays.
	 * @param index Parameter index in the flight controller memory, starting in 0.
	 * @return The parameter value if the command was successful, or null in an error happens.
	 */
	public static Double getParameter(int numUAV, int index) {
		ArduSim ardusim = API.getArduSim();
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newParamIndex.set(numUAV, index);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_GET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_2 + " " + index + ".");
			return null;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_2 + " " + index + " = " + UAVParam.newParamValue.get(numUAV));
			return UAVParam.newParamValue.get(numUAV);
		}
	}
	
	
	
}
