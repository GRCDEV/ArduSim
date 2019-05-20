package api;

import java.util.ArrayList;
import java.util.List;

import org.javatuples.Quintet;
import org.mavlink.messages.MAV_CMD;

import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.Location2D;
import api.pojo.Point3D;
import api.pojo.RCValues;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.ArduSimTools;
import main.Param;
import main.StartExperimentThread;
import main.Text;
import sim.board.BoardParam;
import sim.logic.SimParam;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;

/** This class contains exclusively static methods to control the UAV "numUAV" in the arrays included in the application.
 * <p>Methods that start with "API" are single commands, while other methods are complex commands made of several of the previous commands.</p>
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class Copter {
	
	/**
	 * API: Set a new value for a flight controller parameter.
	 * <p>Parameters that start with "SIM_" are only available on simulation.</p>
	 * @param numUAV UAV position in arrays.
	 * @param parameter Parameter to modify.
	 * @param value New value.
	 * @return true if the command was successful.
	 */
	public static boolean setParameter(int numUAV, ControllerParam parameter, double value) {
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.newParamValue.set(numUAV, value);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			GUI.log(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_1 + " " + parameter.getId() + ".");
			return false;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.PARAMETER_1 + " " + parameter.getId() + " = " + value);
			return true;
		}
	}
	
	/**
	 * API: Get the value of a flight controller parameter.
	 * <p>Parameters that start with "SIM_" are only available on simulation.</p>
	 * @param numUAV UAV position in arrays.
	 * @param parameter Parameter to retrieve.
	 * @return The parameter value if the command was successful, or null in an error happens.
	 */
	public static Double getParameter(int numUAV, ControllerParam parameter) {
		if (UAVParam.loadedParams[numUAV].containsKey(parameter.getId())) {
			return (double) UAVParam.loadedParams[numUAV].get(parameter.getId()).getValue();
		}
		
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_GET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			GUI.log(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_2 + " " + parameter.getId() + ".");
			return null;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.PARAMETER_2 + " " + parameter.getId() + " = " + UAVParam.newParamValue.get(numUAV));
			return UAVParam.newParamValue.get(numUAV);
		}
	}

	/**
	 * API: Change a UAV flight mode.
	 * @param numUAV UAV position in arrays.
	 * @param mode Flight mode to set.
	 * @return true if the command was successful.
	 */
	public static boolean setFlightMode(int numUAV, FlightMode mode) {
		UAVParam.newFlightMode[numUAV] = mode;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_MODE);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_MODE) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_MODE) {
			GUI.log(SimParam.prefix[numUAV] + Text.FLIGHT_MODE_ERROR_1);
			return false;
		} else {
			long start = System.currentTimeMillis();
			boolean changed = false;
			FlightMode currentMode;
			while (!changed) {
				currentMode = UAVParam.flightMode.get(numUAV);
				if (currentMode.getCustomMode() == mode.getCustomMode()) {
					changed = true;
				} else {
					if (System.currentTimeMillis() - start > UAVParam.MODE_CHANGE_TIMEOUT) {
						return false;
					} else {
						Tools.waiting(UAVParam.COMMAND_WAIT);
					}
				}
			}
			return true;
		}
	}

	/**
	 * API: Get the last flight mode value provided by a flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return The flight mode of the multicopter.
	 */
	public static FlightMode getFlightMode(int numUAV) {
		return UAVParam.flightMode.get(numUAV);
	}
	
	/**
	 * API: Check if the UAV is flying.
	 * @param numUAV UAV position in arrays.
	 * @return Whether the UAV is flying or not.
	 */
	public static boolean isFlying(int numUAV) {
		return UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING;
	}
	
	/**
	 * API: Arm the engines.
	 * <p>Previously, on a real UAV you have to press the hardware switch for safety arm, if available.
	 * The UAV must be in an armable flight mode (STABILIZE, LOITER, ALT_HOLD, GUIDED).</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful.
	 */
	public static boolean armEngines(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_ARM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_ARM) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_ARM) {
			GUI.log(SimParam.prefix[numUAV] + Text.ARM_ENGINES_ERROR);
			return false;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.ARM_ENGINES);
			return true;
		}
	}

	/**
	 * API: Take off a UAV previously armed.
	 * <p>The UAV must be in GUIDED mode and armed.
	 * Previously, on a real UAV you have to press the hardware switch for safety arm, if available.</p>
	 * @param numUAV UAV position in arrays.
	 * @param altitude Target altitude over the home location.
	 * @return true if the command was successful.
	 */
	public static boolean guidedTakeOff(int numUAV, double altitude) {
		UAVParam.takeOffAltitude.set(numUAV, altitude);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_TAKE_OFF);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			GUI.log(SimParam.prefix[numUAV] + Text.TAKE_OFF_ERROR_2);
			return false;
		} else {
			GUI.log(SimParam.prefix[numUAV] + Text.TAKE_OFF);
			return true;
		}
	}
	
	/**
	 * Take off all the UAVs running on the same machine and start the planned missions.
	 * <p>Concurrent method (all UAVs start the mission at the same time).
	 * Previously, on a real UAV you have to press the hardware switch for safety arm, if available.
	 * The UAVs must be on the ground and in an armable flight mode (STABILIZE, LOITER, ALT_HOLD, GUIDED).</p>
	 * @return true if all the commands were successful.
	 */
	public static boolean startMissionsFromGround() {
		// Starts the movement of each UAV by arming engines, setting auto mode, and throttle to stabilize altitude.
		// All UAVs start the experiment in different threads, but the first
		StartExperimentThread[] threads = null;
		int numUAVs = Tools.getNumUAVs();
		if (numUAVs > 1) {
			threads = new StartExperimentThread[numUAVs - 1];
			for (int i=1; i<numUAVs; i++) {
				threads[i-1] = new StartExperimentThread(i, null);
			}
			for (int i=1; i<numUAVs; i++) {
				threads[i-1].start();
			}
		}
		if (Copter.startMissionFromGround(0)) {
			StartExperimentThread.UAVS_TESTING.incrementAndGet();
		}
		if (numUAVs > 1) {
			for (int i=1; i<numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException e) {
				}
			}
		}
		if (StartExperimentThread.UAVS_TESTING.get() < numUAVs) {
			return false;
		}
		return true;
	}
	
	/**
	 * Take off a UAV and start the planned mission.
	 * <p>Issues three commands: armEngines, setFlightMode --> AUTO, and setHalfThrottle.
	 * Previously, on a real UAV you have to press the hardware switch for safety arm, if available.
	 * The UAV must be on the ground and in an armable flight mode (STABILIZE, LOITER, ALT_HOLD, GUIDED).</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if all the commands were successful.
	 */
	public static boolean startMissionFromGround(int numUAV) {
		// Documentation says: While on the ground, 1st arm, 2nd auto mode, 3rd some throttle, and the mission begins
		//    If the copter is flying, the take off waypoint will be considered to be completed, and the UAV goes to the next waypoint
		if (armEngines(numUAV)
				&& setFlightMode(numUAV, FlightMode.AUTO)
				&& setHalfThrottle(numUAV)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Take off all the UAVs: change mode to guided, arm engines, and then perform the guided take off.
	 * <p>Non blocking method.
	 * Please, consider that the take-off of all the UAVs should not end before reaching the following altitude: altitude-0.25 for altitude<=10m, altitude*0.95+0.25 for 10<altitude<=50m, and altitude-1.35 otherwise.</p>
	 * @param altitudes (meters) Array with the target relative altitude for all the UAVs (be sure that <i>altitudes.length == Tools.getNumUAVs()</i>).
	 * @return true if all the commands were successful.
	 */
	public static boolean takeOffAllUAVsNonBlocking(double[] altitudes) {
		int numUAVs = Tools.getNumUAVs();
		if (altitudes.length != numUAVs) {
			return false;
		}
		// Starts the movement of each UAV by setting guided mode, arming engines, and throttle to stabilize altitude.
		// All UAVs start the experiment in different threads, but the first
		StartExperimentThread[] threads = null;
		if (numUAVs > 1) {
			threads = new StartExperimentThread[numUAVs - 1];
			for (int i=1; i<numUAVs; i++) {
				threads[i-1] = new StartExperimentThread(i, altitudes[i]);
			}
			for (int i=1; i<numUAVs; i++) {
				threads[i-1].start();
			}
		}
		if (Copter.takeOffNonBlocking(0, altitudes[0])) {
			StartExperimentThread.UAVS_TESTING.incrementAndGet();
		}
		if (numUAVs > 1) {
			for (int i=1; i<numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException e) {
				}
			}
		}
		if (StartExperimentThread.UAVS_TESTING.get() < numUAVs) {
			return false;
		}
		return true;
	}
	
	/**
	 * Take off all the UAVs one by one: change mode to guided, arm engines, and then perform the guided take off.
	 * <p>Blocking method.
	 * Please, consider that this method ends when the rules of the <i>Copter.takeOffNonBlocking</i> command are satisfied for all the UAVs.</p>
	 * @param altitudes (meters) Array with the target relative altitude for all the UAVs (be sure that <i>altitudes.length == Tools.getNumUAVs()</i>).
	 * @return true if all the commands were successful.
	 */
	public static boolean takeOffAllUAVs(double[] altitudes) {
		if (altitudes.length != Tools.getNumUAVs()) {
			return false;
		}
		if (!Copter.takeOffAllUAVsNonBlocking(altitudes)) {
			return false;
		}
		
		double minAltitude;
		for (int i = 0; i < Param.numUAVs; i++) {
			minAltitude = Copter.getMinTargetAltitude(altitudes[i]);
			while (UAVParam.uavCurrentData[i].getZRelative() < minAltitude) {
				GUI.logVerbose(SimParam.prefix[i] + Text.ALTITUDE_TEXT
						+ " = " + String.format("%.2f", UAVParam.uavCurrentData[i].getZ())
						+ " " + Text.METERS);
				Tools.waiting(UAVParam.ALTITUDE_WAIT);
			}
		}
		return true;
	}
	
	/**
	 *  Take off until the target relative altitude: change mode to guided, arm engines, and then perform the guided take off.
	 * <p>Non blocking method.
	 * Please, consider that the take-off should not end before reaching the following altitude: altitude-0.25 for altitude<=10m, altitude*0.95+0.25 for 10<altitude<=50m, and altitude-1.35 otherwise.</p>
	 * @param numUAV UAV position in arrays.
	 * @param altitude (meters) Target relative altitude for the UAV.
	 * @return true if the command was successful.
	 */
	public static boolean takeOffNonBlocking(int numUAV, double altitude) {
		if (!setFlightMode(numUAV, FlightMode.GUIDED) || !armEngines(numUAV) || !guidedTakeOff(numUAV, altitude)) {
			GUI.log(Text.TAKE_OFF_ERROR_1 + " " + Param.id[numUAV]);
			return false;
		}
		return true;
	}
	
	/**
	 * Take off until the target relative altitude: change mode to guided, arm engines, and then perform the guided take off.
	 * <p>Blocking method.
	 * Please, consider that this method ends when the rules of the <i>Copter.takeOffNonBlocking</i> command are satisfied.</p>
	 * @param numUAV UAV position in arrays.
	 * @param altitude (meters) Target relative altitude for the UAV.
	 * @return true if the command was successful.
	 */
	public static boolean takeOff(int numUAV, double altitude) {
		if (!takeOffNonBlocking(numUAV, altitude)) {
			return false;
		}

		double minAltitude = Copter.getMinTargetAltitude(altitude);
		while (UAVParam.uavCurrentData[numUAV].getZRelative() < minAltitude) {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.ALTITUDE_TEXT
					+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ())
					+ " " + Text.METERS);
			Tools.waiting(UAVParam.ALTITUDE_WAIT);
		}
		return true;
	}
	
	/**
	 * API: Change the planned flight speed.
	 * @param numUAV UAV position in arrays.
	 * @param speed (m/s) Target speed.
	 * @return true if the command was successful.
	 */
	public static boolean setSpeed(int numUAV, double speed) {
		UAVParam.newSpeed[numUAV] = speed;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_SPEED);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			GUI.log(SimParam.prefix[numUAV] + Text.SPEED_2_ERROR);
			return false;
		} else {
			GUI.log(SimParam.prefix[numUAV] + Text.SPEED_2 + " = " + speed);
			return true;
		}
	}

	/**
	 * API: Modify the current waypoint of the mission stored on the UAV.
	 * @param numUAV UAV position in arrays.
	 * @param currentWP New value, starting from 0.
	 * @return true if the command was successful.
	 */
	public static boolean setCurrentWaypoint(int numUAV, int currentWP) {
		UAVParam.newCurrentWaypoint[numUAV] = currentWP;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_CURRENT_WP);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			GUI.log(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT_ERROR);
			return false;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT + " = " + currentWP);
			return true;
		}
	}

	/**
	 * Suspend temporarily a mission in AUTO mode, entering on loiter flight mode to force a fast stop.
	 * <p>Blocking method until the UAV is almost stopped.</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if all the commands were successful.
	 */
	public static boolean stopUAV(int numUAV) {
		if (Copter.setHalfThrottle(numUAV) && setFlightMode(numUAV, FlightMode.LOITER)) {
			long time = System.nanoTime();
			while (UAVParam.uavCurrentData[numUAV].getSpeed() > UAVParam.STABILIZATION_SPEED) {
				Tools.waiting(UAVParam.STABILIZATION_WAIT_TIME);
				if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
					GUI.log(SimParam.prefix[numUAV] + Text.STOP_ERROR_1);
					return true;
				}
			}

			GUI.logVerbose(SimParam.prefix[numUAV] + Text.STOP);
			return true;
		}
		GUI.log(SimParam.prefix[numUAV] + Text.STOP_ERROR_2);
		return false;
	}

	/**
	 * API: Move the throttle stick to half power using the corresponding RC channel.
	 * <p>Useful for starting auto flight while being on the ground, or to stabilize altitude when going out of auto mode.</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful.
	 */
	public static boolean setHalfThrottle(int numUAV) {
		if (UAVParam.overrideOn.get(numUAV) == 1) {
			UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_THROTTLE_ON);
			while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
					&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
				Tools.waiting(UAVParam.COMMAND_WAIT);
			}
			if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
				GUI.log(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE_ERROR);
				return false;
			} else {
				GUI.logVerbose(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE);
				return true;
			}
		}
		GUI.log(SimParam.prefix[numUAV] + Text.RC_CHANNELS_OVERRIDE_FORBIDEN_ERROR);
		return false;
	}
	
	/**
	 * API: Cancel the overriding of the remote control output.
	 * <p>This function can be used only once, and since then the RC channels can not be overridden any more.
	 * Function already used in the PCCompanion to return the control of any overridden RCs when an emergency button is pressed.</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful.
	 */
	public static boolean returnRCControl(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_RECOVER_CONTROL);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_RECOVER_ERROR) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_RECOVER_ERROR) {
			GUI.log(SimParam.prefix[numUAV] + Text.RETURN_RC_CONTROL_ERROR);
			return false;
		} else {
			GUI.log(SimParam.prefix[numUAV] + Text.RETURN_RC_CONTROL);
			return true;
		}
	}
	
	/**
	 * API: Override the remote control output.
	 * <p>Channel values in microseconds. Typically chan1=roll, chan2=pitch, chan3=throttle, chan4=yaw.
	 * Standard modulation: 1000 (0%) - 2000 (100%).
	 * Value 0 means that the control of that channel must be returned to the RC radio.
	 * Value UINT16_MAX means to ignore this field.</p>
	 * <p>By default, channels can be overridden on any flight mode different from GUIDED, but the functionality can be disabled by the command "returnRCControl".
	 * This method doesn't wait a response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param numUAV UAV position in arrays.
	 * @param roll Turn on horizontal axes that goes from front to rear of the UAV (tilt the UAV to the left or to the right).
	 * @param pitch Turn on horizontal axes that goes from left to right of the UAV (raise or turn down the front part of the UAV).
	 * @param throttle Engine power (raise or descend the UAV).
	 * @param yaw Turn on vertical axes (pointing north, east...).
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
	
	/**
	 * API: Override the remote control output.
	 * <p>Values range [-1, 1]</p>
	 * <p>By default, channels can be overridden on any flight mode different from GUIDED, but the functionality can be disabled by the command "returnRCControl".
	 * This method doesn't wait a response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param numUAV UAV position in arrays.
	 * @param roll Turn on horizontal axes that goes from front to rear of the UAV (value<0 tilts the UAV to the left, and value >0 tilts to the right).
	 * @param pitch Turn on horizontal axes that goes from left to right of the UAV (value>0 raises the front part of the UAV and it goes back, and value<0  goes forward).
	 * @param throttle Engine power (value>0 raises the UAV, and value<0 descends the UAV).
	 * @param yaw Turn on vertical axes (value>0 turns right, and value<0 turns left).
	 */
	public static void channelsOverride(int numUAV, double roll, double pitch, double throttle, double yaw) {
		int channelPos = UAVParam.RCmapRoll[numUAV]-1;
		int minValue = UAVParam.RCminValue[numUAV][channelPos];// TODO implement reverse parameter
		int deadzone = UAVParam.RCDZValue[numUAV][channelPos];
		int trim = UAVParam.RCtrimValue[numUAV][channelPos];
		int maxValue = UAVParam.RCmaxValue[numUAV][channelPos];
		int intRoll = Copter.mapValues(minValue, deadzone, trim, maxValue, roll);
		
		channelPos = UAVParam.RCmapPitch[numUAV]-1;
		minValue = UAVParam.RCminValue[numUAV][channelPos];
		deadzone = UAVParam.RCDZValue[numUAV][channelPos];
		trim = UAVParam.RCtrimValue[numUAV][channelPos];
		maxValue = UAVParam.RCmaxValue[numUAV][channelPos];
		int intPitch = Copter.mapValues(minValue, deadzone, trim, maxValue, pitch);
		
		channelPos = UAVParam.RCmapThrottle[numUAV]-1;
		minValue = UAVParam.RCminValue[numUAV][channelPos];
		deadzone = UAVParam.throttleDZ[numUAV];
		trim = UAVParam.stabilizationThrottle[numUAV];
		maxValue = UAVParam.RCmaxValue[numUAV][channelPos];
		int intThrottle = Copter.mapValues(minValue, deadzone, trim, maxValue, throttle);
		
		channelPos = UAVParam.RCmapYaw[numUAV]-1;
		minValue = UAVParam.RCminValue[numUAV][channelPos];
		deadzone = UAVParam.RCDZValue[numUAV][channelPos];
		trim = UAVParam.RCtrimValue[numUAV][channelPos];
		maxValue = UAVParam.RCmaxValue[numUAV][channelPos];
		int intYaw = Copter.mapValues(minValue, deadzone, trim, maxValue, yaw);
		
		if (UAVParam.overrideOn.get(numUAV) == 1) {
			UAVParam.rcs[numUAV].set(new RCValues(intRoll, intPitch, intThrottle, intYaw));
		}
	}
	
	private static int mapValues(int minValue,int deadzone, int trim, int maxValue,double value) {
		double max, min;
		if(value >0 && value <= 1) {
			//map value between 0 and 1  to value between trim+deadzone and maxValue
			max = maxValue;
			min = trim + deadzone;
			return  (int)Math.round(min + (max-min)*value);
		}else if(value <0 && value >=-1) {
			//map value between -1 and 0 to value between minValue and trim-deadzone
			max = trim - deadzone;
			min = minValue;
			return  (int)Math.round(max + (max-min)*value);
		}else {
			//value is either 0 or invalid => return trim value
			return trim;
		}
	}
	
	/**
	 * API: Move the UAV to a new location.
	 * <p>The UAV must be in GUIDED flight mode.</p>
	 * <p>This method uses the message SET_POSITION_TARGET_GLOBAL_INT, and doesn't wait response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param numUAV UAV position in arrays.
	 * @param geo Geographic coordinates the UAV has to move to.
	 * @param relAltitude (meters) Relative altitude the UAV has to move to.
	 */
	public static void moveUAV(int numUAV, GeoCoordinates geo, float relAltitude) {
		UAVParam.target[numUAV].set(new Point3D(geo.longitude, geo.latitude, relAltitude));
	}

	/**
	 * API: Move the UAV to a new location.
	 * <p>The UAV must be in GUIDED flight mode.</p>
	 * <p>Please, consider that the UAV could stop somewhere in the following ranges: relAltitude±0.25 for relAltitude<=10m, [relAltitude*0.95+0.25,relAltitude*1.05-0.25] for 10<relAltitude<=50m, and relAltitude±1.35 otherwise.</p>
	 * <p>This method uses the message MISSION_ITEM, and waits response from the flight controller.
	 * The method may return control immediately or in more than 200 ms depending on the reaction of the flight controller.</p>
	 * @param numUAV UAV position in arrays.
	 * @param geo Geographic coordinates the UAV has to move to.
	 * @param relAltitude (meters) Relative altitude the UAV has to move to.
	 * @return true if the command was successful.
	 */
	public static boolean moveUAVNonBlocking(int numUAV, GeoCoordinates geo, float relAltitude) {
		UAVParam.newLocation[numUAV][0] = (float)geo.latitude;
		UAVParam.newLocation[numUAV][1] = (float)geo.longitude;
		UAVParam.newLocation[numUAV][2] = relAltitude;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			GUI.log(SimParam.prefix[numUAV] + Text.MOVING_ERROR_1);
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * API: Move the UAV to a new location.
	 * <p>The UAV must be in GUIDED flight mode.
	 * Blocking method. </p>
	 * <p>This method uses the message MISSION_ITEM, and waits response from the flight controller.
	 * It also waits until the UAV is close enough of the target location.</p>
	 * @param numUAV UAV position in arrays.
	 * @param geo Geographic coordinates the UAV has to move to.
	 * @param relAltitude (meters) Relative altitude the UAV has to move to.
	 * @param destThreshold (meters) Horizontal distance from the destination to assert that the UAV has reached there.
	 * @param altThreshold (meters) Vertical distance from the destination to assert that the UAV has reached there. Please, consider that altThreshold will be forced to satisfy the rules of the <i>Copter.moveUAVNonBlocking</i> command.
	 * @return true if the command was successful.
	 */
	public static boolean moveUAV(int numUAV, GeoCoordinates geo, float relAltitude, double destThreshold, double altThreshold) {
		if (!Copter.moveUAVNonBlocking(numUAV, geo, relAltitude)) {
			return false;
		}
		
		UTMCoordinates utm = Tools.geoToUTM(geo.latitude, geo.longitude);
		double min = Math.min(Copter.getMinTargetAltitude(relAltitude), relAltitude - altThreshold);
		double max = Math.max(Copter.getMaxTargetAltitude(relAltitude), relAltitude + altThreshold);
		double altitude;
		// Once the command is issued, we have to wait until the UAV approaches to destination.
		// No timeout is defined to reach the destination, as it would depend on speed and distance
		boolean goOn = true;
		while (goOn) {
			if (UAVParam.uavCurrentData[numUAV].getUTMLocation().distance(utm) <= destThreshold) {
				altitude = UAVParam.uavCurrentData[numUAV].getZRelative();
				if (altitude >= min && altitude <= max) {
					goOn = false;
				}
			}
			if (goOn) {
				Tools.waiting(UAVParam.STABILIZATION_WAIT_TIME);
			}
		}
		return true;
	}
	
	/**
	 * When moving towards a target location with a <i>moveUAV</i> function, it gets the minimum relative altitude to assert that the UAV is close enough to that location.
	 * @param relAltitude Relative altitude (m) over home location.
	 * @return The minimum altitude where a UAV could stop for a target altitude, when the moveUAV command is used. The rules for that command are applied.
	 */
	public static double getMinTargetAltitude(double relAltitude) {
		double res;
		if (relAltitude <= 20) {
			res = relAltitude - 1;
		} else if (relAltitude <= 50) {
			res = relAltitude * 0.95;
		} else {
			res = relAltitude - 2.5;
		}
		return res;
	}
	
	/**
	 * When moving towards a target location with a <i>moveUAV</i> function, it gets the maximum relative altitude to assert that the UAV is close enough to that location.
	 * @param relAltitude Relative altitude (m) over home location.
	 * @return The maximum altitude where a UAV could stop for a target altitude, when the moveUAV command is used. The rules for that command are applied.
	 */
	public static double getMaxTargetAltitude(double relAltitude) {
		double res;
		if (relAltitude <= 20) {
			res = relAltitude + 1;
		} else if (relAltitude <= 50) {
			res = relAltitude * 1.05;
		} else {
			res = relAltitude + 2.5;
		}
		return res;
	}

	/**
	 * API: Remove the current mission from the UAV.
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful.
	 */
	public static boolean clearMission(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_CLEAR_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			GUI.log(SimParam.prefix[numUAV] + Text.MISSION_DELETE_ERROR);
			return false;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.MISSION_DELETE);
			return true;
		}
	}

	/**
	 * API: Send a new mission to the UAV.
	 * <p>The waypoint 0 of the mission should be the current coordinates retrieved from the controller, but it is ignored anyway.
	 * The waypoint 1 must be take off.
	 * The last waypoint can be land or RTL.</p>
	 * @param numUAV UAV position in arrays.
	 * @param list Mission to be sent to the flight controller.
	 * @return true if the command was successful.
	 */
	public static boolean sendMission(int numUAV, List<Waypoint> list) {
		if (list == null || list.size() < 2) {
			GUI.log(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_1);
			return false;
		}
		// There is a minimum altitude to fly (waypoint 0 is home, and waypoint 1 is takeoff)
		if (list.get(1).getAltitude() < UAVParam.minAltitude) {
			GUI.log(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_2 + "(" + UAVParam.minAltitude + " " + Text.METERS+ ").");
			return false;
		}
		int current = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isCurrent()) {
				current = i;
			}
		}
		// Specify the current waypoint
		UAVParam.currentWaypoint.set(numUAV, current);
		UAVParam.newCurrentWaypoint[numUAV] = current;
		// Relative altitude calculus to take off
		UAVParam.takeOffAltitude.set(numUAV, list.get(1).getAltitude());

		UAVParam.currentGeoMission[numUAV].clear();
		for (int i = 0; i < list.size(); i++) {
			UAVParam.currentGeoMission[numUAV].add(list.get(i).clone());
		}
		GeoCoordinates geo = UAVParam.uavCurrentData[numUAV].getGeoLocation();
		// The take off waypoint must be modified to include current coordinates
		UAVParam.currentGeoMission[numUAV].get(1).setLatitude(geo.latitude);
		UAVParam.currentGeoMission[numUAV].get(1).setLongitude(geo.longitude);
		
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SEND_WPS);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			GUI.log(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_3);
			return false;
		} else {
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.MISSION_SENT);
			return true;
		}
	}
	
	/**
	 * API: Retrieve the mission stored on the UAV.
	 * <p>The new value available on <i>api.Tools.getUAVMission(numUAV)</i>.
	 * The simplified version of the mission in UTM coordinates available on <i>api.Tools.getUAVMissionSimplified(numUAV)</i>.</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful.
	 */
	public static boolean retrieveMission(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST) {
			Tools.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			GUI.log(SimParam.prefix[numUAV] + Text.MISSION_GET_ERROR);
			return false;
		} else {
			Copter.simplifyMission(numUAV);
			GUI.logVerbose(SimParam.prefix[numUAV] + Text.MISSION_GET);
			return true;
		}
	}
	
	/**
	 * Create the simplified mission shown on screen, and forces view to re-scale.
	 * @param numUAV UAV position in arrays.
	 */
	private static void simplifyMission(int numUAV) {
		List<WaypointSimplified> missionUTMSimplified = new ArrayList<WaypointSimplified>();
	
		// Hypothesis:
		//   The first take off waypoint retrieved from the UAV is used as "home"
		//   The "home" coordinates are no modified along the mission
		WaypointSimplified first = null;
		boolean foundFirst = false;
		Waypoint wp;
		UTMCoordinates utm;
		for (int i=1; i<UAVParam.currentGeoMission[numUAV].size(); i++) {
			wp = UAVParam.currentGeoMission[numUAV].get(i);
			switch (wp.getCommand()) {
			case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
			case MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM:// Currently, only WAYPOINT, SPLINE_WAYPOINT, TAKEOFF, LAND, and RETURN_TO_LAUNCH waypoints are accepted when loading
			case MAV_CMD.MAV_CMD_NAV_LOITER_TURNS:
			case MAV_CMD.MAV_CMD_NAV_LOITER_TIME:
			case MAV_CMD.MAV_CMD_NAV_SPLINE_WAYPOINT:
			case MAV_CMD.MAV_CMD_PAYLOAD_PREPARE_DEPLOY:
			case MAV_CMD.MAV_CMD_NAV_LOITER_TO_ALT:
			case MAV_CMD.MAV_CMD_NAV_LAND:
				if (wp.getLatitude() != 0.0 || wp.getLongitude() != 0.0) {
					utm = Tools.geoToUTM(wp.getLatitude(), wp.getLongitude());
					WaypointSimplified swp = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
					missionUTMSimplified.add(swp);
				}
				break;
			case MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH:
				if (!foundFirst) {
					GUI.log(Text.SIMPLIFYING_WAYPOINT_LIST_ERROR + Param.id[numUAV]);
				} else {
					WaypointSimplified s = new WaypointSimplified(wp.getNumSeq(),
							first.x, first.y, UAVParam.RTLAltitude[numUAV]);
					missionUTMSimplified.add(s);
				}
				break;
			case MAV_CMD.MAV_CMD_NAV_TAKEOFF:
				// The geographic coordinates have been set by the flight controller
				utm = Tools.geoToUTM(wp.getLatitude(), wp.getLongitude());
				WaypointSimplified twp = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
				missionUTMSimplified.add(twp);
				if (!foundFirst) {
					utm = Tools.geoToUTM(wp.getLatitude(), wp.getLongitude());
					first = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
					foundFirst = true;
				}
				break;
			}
		}
		UAVParam.missionUTMSimplified.set(numUAV, missionUTMSimplified);
		Waypoint lastWP = UAVParam.currentGeoMission[numUAV].get(UAVParam.currentGeoMission[numUAV].size() - 1);
		UAVParam.lastWP[numUAV] = lastWP;
		if (lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH) {
			Waypoint home = UAVParam.currentGeoMission[numUAV].get(0);
			UAVParam.lastWPUTM[numUAV] = Tools.geoToUTM(home.getLatitude(), home.getLongitude());
		} else if (lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND) {
			if (lastWP.getLatitude() == 0 && lastWP.getLongitude() == 0) {
				Waypoint prevLastWP = UAVParam.currentGeoMission[numUAV].get(UAVParam.currentGeoMission[numUAV].size() - 2);
				UAVParam.lastWPUTM[numUAV] = Tools.geoToUTM(prevLastWP.getLatitude(), prevLastWP.getLongitude());
				// Be careful, usually, the UAV lands many meters before reaching that waypoint if the planned speed is high
			} else {
				UAVParam.lastWPUTM[numUAV] = Tools.geoToUTM(lastWP.getLatitude(), lastWP.getLongitude());
			}
		} else {
			UAVParam.lastWPUTM[numUAV] = Tools.geoToUTM(lastWP.getLatitude(), lastWP.getLongitude());
		}
	}
	
	/**
	 * Delete the current mission of the UAV, sends a new one, and gets it to be shown on the GUI.
	 * <p>Blocking method.</p>
	 * <p>Method automatically used by ArduSim on start to send available missions to the UAVs.
	 * Must be used only if the UAV must follow a mission.</p>
	 * @param numUAV UAV position in arrays.
	 * @param mission Mission to be sent to the flight controller.
	 * @return true if all the commands were successful.
	 */
	public static boolean cleanAndSendMissionToUAV(int numUAV, List<Waypoint> mission) {
		boolean success = false;
		if (Copter.clearMission(numUAV)
				&& Copter.sendMission(numUAV, mission)
				&& Copter.retrieveMission(numUAV)
				&& Copter.setCurrentWaypoint(numUAV, 0)) {
			Param.numMissionUAVs.incrementAndGet();
			if (Param.role == Tools.SIMULATOR) {
				BoardParam.rescaleQueries.incrementAndGet();
			}
			success = true;
		}
		return success;
	}
	
	/**
	 * Add a Class as listener for the event: waypoint reached (more than one can be set).
	 * @param listener Class to be added as listener for the event.
	 */
	public static void setWaypointReachedListener(WaypointReachedListener listener) {
		ArduSimTools.listeners.add(listener);
	}
	
	/**
	 * API: Get the current waypoint of the mission for a UAV.
	 * <p>Use only when the UAV is performing a planned mission.</p>
	 * @param numUAV UAV position in arrays.
	 * @return The current waypoint of the mission, starting from 0.
	 */
	public static int getCurrentWaypoint(int numUAV) {
		return UAVParam.currentWaypoint.get(numUAV);
	}
	
	/**
	 * API: Find out if the UAV has reached the last waypoint of the mission.
	 * <p>Use only when the UAV is performing a planned mission.</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if the last waypoint of the mission has been reached.
	 */
	public static boolean isLastWaypointReached(int numUAV) {
		return UAVParam.lastWaypointReached[numUAV];
	}
	
	/**
	 * Land the UAV if it is close enough to the last waypoint.
	 * <p>This method can be launched periodically, it only informs that the last waypoint is reached once, and it only lands the UAV if it close enough to the last waypoint and not already landing or on the ground.
	 * Use only when the UAV is performing a planned mission.</p>
	 * @param numUAV UAV position in arrays.
	 * @param distanceThreshold (meters) Horizontal distance from the last waypoint of the mission to assert that the UAV has to land.
	 */
	public static void landIfMissionEnded(int numUAV, double distanceThreshold) {
		int currentWaypoint = Copter.getCurrentWaypoint(numUAV);
		Waypoint lastWP = UAVParam.lastWP[numUAV];
		// Only check when the last waypoint is reached
		if (lastWP != null && currentWaypoint > 0 && currentWaypoint == lastWP.getNumSeq()) {
			// Inform only once that the last waypoint has been reached
			if (!UAVParam.lastWaypointReached[numUAV]) {
				UAVParam.lastWaypointReached[numUAV] = true;
				GUI.log(numUAV, Text.LAST_WAYPOINT_REACHED);// never shown when RTL is used (workaround in ArduSimTools.isTestFinished())
			}
			// Do nothing if the mission plans to land autonomously
			if (lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND
					|| (lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH && UAVParam.RTLAltitudeFinal[numUAV] == 0)) {
				return;
			}
			// Do nothing if the UAV is already landing
			FlightMode mode = Copter.getFlightMode(numUAV);
			if (mode == FlightMode.LAND_ARMED
					&& mode == FlightMode.LAND) {
				return;
			}
			// Land only if the UAV is really close to the last waypoint force it to land
			List<WaypointSimplified> mission = Tools.getUAVMissionSimplified(numUAV);
			if (mission != null
					&& Copter.getUTMLocation(numUAV).distance(mission.get(mission.size()-1)) < distanceThreshold) {
				if (!Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED)) {
					GUI.log(numUAV, Text.LAND_ERROR);
				}
			}
		}
	}
	
	/**
	 * Land a UAV if it is flying.
	 * @param numUAV UAV position in arrays.
	 * @return true if the command was successful, or even when it is not needed because the UAV was not flying.
	 */
	public static boolean landUAV(int numUAV) {
		if (Copter.isFlying(numUAV)) {
			if (!setFlightMode(numUAV, FlightMode.LAND_ARMED)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Land all the UAVs that are flying.
	 * @return true if all the commands were successful, or even when they are not needed because one or more UAVs were not flying.
	 */
	public static boolean landAllUAVs() {
		for (int i=0; i<Param.numUAVs; i++) {
			if (Copter.isFlying(i)) {
				if (!setFlightMode(i, FlightMode.LAND_ARMED)) {
					return false;
				}
			}
		}
		return true;
	}
	
//	/** Method that provides the controller thread of an specific UAV.
//	 * <p>Temporary usage for debugging purposes.
//	 * <p>HANDLE WITH CARE.*/
//	public static UAVControllerThread getController(int numUAV) {
//		return Param.controllers[numUAV];
//	}
	
	/**
	 * API: Get the planned speed.
	 * @param numUAV UAV position in arrays.
	 * @return (meters) The planned speed.
	 */
	public static double getPlannedSpeed(int numUAV) {
		return UAVParam.initialSpeeds[numUAV];
	}

	/**
	 * API: Get the latest received data from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return A set of 5 values: time (ns) when the data was received from the flight controller, UTM coordinates,
	 * absolute altitude (meters), speed (m/s), and acceleration (m/s^2).
	 */
	public static Quintet<Long, UTMCoordinates, Double, Double, Double> getData(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getData();
	}

	/**
	 * API: Get the latest location in UTM coordinates (meters) received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return The latest known location.
	 */
	public static UTMCoordinates getUTMLocation(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getUTMLocation();
	}

	/**
	 * API: Get the latest location in Geographic coordinates (degrees) received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return The latest known location.
	 */
	public static GeoCoordinates getGeoLocation(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getGeoLocation();
	}
	
	/**
	 * API: Get the latest location in both UTM (meters) and Geographic coordinates (degrees) received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return The latest known location.
	 */
	public static Location2D getLocation(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getLocation();
	}
	
	/**
	 * API: Get the last available known locations (x,y) of the UAV in UTM coordinates (meters).
	 * @param numUAV UAV position in arrays.
	 * @return An array with the last received coordinates where time increases with the position in the array.
	 */
	public static UTMCoordinates[] getLastKnownUTMLocations(int numUAV) {
		return UAVParam.lastUTMLocations[numUAV].getLastValues();
	}

	/**
	 * API: Get the latest relative altitude received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return (meters) Relative altitude over the home location.
	 */
	public static double getZRelative(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getZRelative();
	}

	/**
	 * API: Get the latest altitude received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return (meters) Absolute altitude.
	 */
	public static double getZ(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getZ();
	}

	/**
	 * API: Get the latest ground speed received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return (m/s) Ground speed.
	 */
	public static double getSpeed(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getSpeed();
	}

	/**
	 * API: Get the latest three axes components of the speed received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return (m/s) Ground speed on the three axes [x, y, z].
	 */
	public static double[] getSpeeds(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getSpeeds();
	}

	/**
	 * API: Get the latest heading received from the flight controller.
	 * @param numUAV UAV position in arrays.
	 * @return (rad) Heading
	 */
	public static double getHeading(int numUAV) {
		return UAVParam.uavCurrentData[numUAV].getHeading();
	}

	
}
