package com.api.hiddenFunctions;

import com.api.API;
import com.api.pojo.RCValues;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.ArduSim;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

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
	 * Starting from ArduPilot version 3.5.8 and higher ArduCopter expects
	 * continuous RC override or no override at all. However RC overriding is necessary once,
	 * in order to set the remote control values in the middle.
	 * Therefore in the function stabilize we cancel RCOverride. 
	 * @param numUAV number of UAVs
	 * @return true if stabilized false if error occurred
	 */
	public static boolean stabilize(int numUAV) {
		Boolean answer = stabilize_intern(numUAV);
		if (answer) {
			API.getCopter(numUAV).cancelRCOverride();
			UAVParam.overrideOn.set(numUAV, 1);
			return true;
		}
		return false;
	}

	
	/**
	 * Center the joysticks of the virtual remote control to stabilize the UAV.
	 * <p>Useful to stabilize altitude before taking off, or for starting AUTO flight while being on the ground.</p>
	 * @param numUAV Position of the UAV in the stored arrays.
	 * @return true if the command was successful.
	 */
	private static  boolean stabilize_intern(int numUAV) {
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
	
	public static boolean requestForMessage(int messageId, int numUAV, boolean ignoreError) {
		ArduSim ardusim = API.getArduSim();
		//mavlink2 messageId are max 3 bytes
		if(messageId < 0 || messageId > 16777215) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + "Requesting for unvalid messageId");
			return false;
		}
		// Wait until finite state machine is ready
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		// Set finite state machine
		if(UAVParam.messageId == null) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + "UAVParam.messageId is not definied");
			return false;
		}
		UAVParam.messageId.set(messageId);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_MESSAGE);
		// Wait until finite state machine is done executing (or until it gives an error)
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_REQUEST_MESSAGE) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		// Check if everything went correctly
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_REQUEST_MESSAGE) {
			if(ignoreError) {
				UAVParam.MAVStatus.set(numUAV,UAVParam.MAV_STATUS_OK);
				while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
					ardusim.sleep(UAVParam.COMMAND_WAIT);
				}
				return true;
			}else {
				ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.REQUEST_MESSAGE_ERROR + messageId);
				return false;
			}
		} else {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.REQUEST_MESSAGE + messageId);
			return true;
		}
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

	/**
	 * Override the remote control output.
	 * <p>Input channel values in microseconds (standard modulation ranges from 1000 us (0%) to 2000 us (100%)).</p>
	 * <p>Input value 0 means that the control of that channel must be returned to the RC radio.
	 * Value UINT16_MAX means to ignore this field.</p>
	 * <p>By default, channels can be overridden on any flight mode different from GUIDED, but this functionality can be disabled by the command <i>cancelRCOverride()</i>.
	 * This method doesn't wait a response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param roll (us) Turn on horizontal axis that goes from front to rear of the UAV (tilt the UAV to the left or to the right).
	 * @param pitch (us) Turn on horizontal axis that goes from left to right of the UAV (raise or turn down the front part of the UAV).
	 * @param throttle (us) Engine power (raise or descend the UAV).
	 * @param yaw (us) Turn on vertical axis (pointing north, east...).
	 */
	public static void channelsOverride(int numUAV, int roll, int pitch, int throttle, int yaw) {
		if (UAVParam.overrideOn.get(numUAV) == 1) {
			UAVParam.rcs[numUAV].set(new RCValues(roll, pitch, throttle, yaw));
		}
		//TODO analizar la frecuencia de recepción de mensajes
		// y analizar si vale la pena hacer la lectura no bloqueante para enviar esto con más frecuencia y de otra forma
		// ¿con qué frecuencia aceptaremos el channels override? Hay experimentos que indican que algunos valores se ignoran
		// si se envian en intervalos menores a 0.393 segundos (quizá una cola fifo con timeout facilite resolver el problema)
	}

}
