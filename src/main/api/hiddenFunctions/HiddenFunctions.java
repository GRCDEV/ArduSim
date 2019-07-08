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
	 * <p>Useful to stabilize altitude when the take off process finishes, or for starting AUTO flight while being on the ground.</p>
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
	
	
	
}
