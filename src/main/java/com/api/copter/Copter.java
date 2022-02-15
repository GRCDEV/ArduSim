package com.api.copter;

import com.api.*;
import com.api.hiddenFunctions.HiddenFunctions;
import com.setup.Param;
import com.setup.Text;
import org.javatuples.Quintet;
import com.api.pojo.FlightMode;
import com.api.pojo.RCValues;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.Location3DGeo;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

/** API to control the multicopter.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Copter {
	
	private ArduSim ardusim;
	private int numUAV;
	private MissionHelper missionHelper;
	
	@SuppressWarnings("unused")
	private Copter() {}
	
	public Copter(int numUAV) {
		this.numUAV = numUAV;
		this.ardusim = API.getArduSim();
		this.missionHelper = new MissionHelper(numUAV, this);
	}
	
	/**
	 * Cancel the overriding of the remote control output.
	 * <p>This function can be used only once, and since then the RC channels can not be overridden any more.
	 * This function is already used in the PCCompanion to return the control of any overridden RCs when an emergency button is pressed.</p>
	 * @return true if the command was successful.
	 */
	public boolean cancelRCOverride() {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_RECOVER_CONTROL);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_RECOVER_ERROR) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_RECOVER_ERROR) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.RETURN_RC_CONTROL_ERROR);
			return false;
		} else {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.RETURN_RC_CONTROL);
			return true;
		}
	}
		
	/**
	 * Override the remote control output.
	 * <p>Values range [-1, 1]</p>
	 * <p>By default, channels can be overridden on any flight mode different from GUIDED, but the functionality can be disabled by the command <i>cancelRCOverride()</i>.
	 * This method doesn't wait a response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param roll Turn on horizontal axis that goes from front to rear of the UAV (value<0 tilts the UAV to the left, and value >0 tilts to the right).
	 * @param pitch Turn on horizontal axis that goes from left to right of the UAV (value>0 raises the front part of the UAV and it goes back, and value<0  goes forward).
	 * @param throttle Engine power (value>0 raises the UAV, and value<0 descends the UAV).
	 * @param yaw Turn on vertical axis (value>0 turns right, and value<0 turns left).
	 */
	public void channelsOverride(double roll, double pitch, double throttle, double yaw) {
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
		int mode = (int)UAVParam.flightMode.get(numUAV).getCustomMode();
		if (mode == FlightMode.ALT_HOLD.getCustomMode()
				|| mode == FlightMode.LOITER.getCustomMode()
				|| mode == FlightMode.POSHOLD.getCustomMode()) {
			deadzone = UAVParam.throttleDZ[numUAV];
		} else {
			deadzone = 0;
		}
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
			return  (int)Math.rint(min + (max-min)*value);
		}else if(value <0 && value >=-1) {
			//map value between -1 and 0 to value between minValue and trim-deadzone
			max = trim - deadzone;
			min = minValue;
			return  (int)Math.rint(max + (max-min)*value);
		}else {
			//value is either 0 or invalid => return trim value
			return trim;
		}
	}
	
	/**
	 * Get the latest absolute altitude received from the flight controller.
	 * @return (m) Absolute altitude.
	 */
	public double getAltitude() {
		return UAVParam.uavCurrentData[numUAV].getZ();
	}
	
	/**
	 * Get the latest relative altitude received from the flight controller.
	 * @return (m) Relative altitude over the home location.
	 */
	public double getAltitudeRelative() {
		return UAVParam.uavCurrentData[numUAV].getZRelative();
	}

//	/** Method that provides the controller thread of an specific UAV.
//	 * <p>Temporary usage for debugging purposes.
//	 * <p>HANDLE WITH CARE.*/
//	public com.api.uavController.UAVControllerThread getController(int numUAV) {
//		return Param.controllers[numUAV];
//	}
	
	/**
	 * Get the current battery level.
	 * @return (%) the battery remaining level or -1 if unknown.
	 * <p>100% is the level when started, even if the battery was not fully charged!.</p>
	 */
	public int getBattery() {
		return UAVParam.uavCurrentStatus[numUAV].getRemainingBattery();
	}
	
	/**
	 * Get the latest received data from the flight controller.
	 * @return A set of 5 values: time (ns) when the data was received from the flight controller, UTM coordinates,
	 * absolute altitude (meters), speed (m/s), and acceleration (m/s&sup2;).
	 */
	public Quintet<Long, Location2DUTM, Double, Double, Double> getData() {
		return UAVParam.uavCurrentData[numUAV].getData();
	}
	
	/**
	 * Get the last flight mode value provided by the flight controller.
	 * @return The current flight mode of the multicopter.
	 */
	public FlightMode getFlightMode() {
		return UAVParam.flightMode.get(numUAV);
	}
	
	/**
	 * Get the latest heading received from the flight controller.
	 * @return (rad) Heading
	 */
	public double getHeading() {
		return UAVParam.uavCurrentData[numUAV].getHeading();
	}
	
	/**
	 * Get the ID of a multicopter.
	 * @return The ID of a multicopter.
	 * On real UAV it returns a value based on the MAC address.
	 * On virtual UAV it returns the position of the UAV in the arrays used by the simulator.
	 */
	public long getID() {
		return Param.id[numUAV];
	}
	
	/**
	 * Get the latest location in both UTM (meters) and Geographic coordinates (degrees) received from the flight controller.
	 * @return The latest known location.
	 */
	public Location2D getLocation() {
		return UAVParam.uavCurrentData[numUAV].getLocation();
	}
	
	/**
	 * Get the latest location in Geographic coordinates (degrees) received from the flight controller.
	 * @return The latest known location.
	 */
	public Location2DGeo getLocationGeo() {
		return UAVParam.uavCurrentData[numUAV].getGeoLocation();
	}

	/**
	 * Get the latest location in UTM coordinates (meters) received from the flight controller.
	 * @return The latest known location.
	 */
	public Location2DUTM getLocationUTM() {
		return UAVParam.uavCurrentData[numUAV].getUTMLocation();
	}

	/**
	 * Get the latest available known locations (x,y) of the UAV in UTM coordinates (meters).
	 * @return An array with the last received coordinates where time increases with the position in the array.
	 */
	public Location2DUTM[] getLocationUTMLastKnown() {
		return UAVParam.lastUTMLocations[numUAV].getLastValues();
	}
	
	/**
	 * Get the altitude GPS expected error depending on the altitude over the home location.
	 * @param relAltitude (m) Relative altitude over the home location
	 * @return (m) GPS error
	 */
	protected static double getAltitudeGPSError(double relAltitude) {
		if (relAltitude <= 20) {
			return 1;
		} else if (relAltitude <= 50) {
			return relAltitude * 0.05;
		} else {
			return 2.5;
		}
	}

	/**
	 * Get additional functions to interact with the UAV to follow planned missions.
	 * @return Context needed to use commands related to missions.
	 */
	public MissionHelper getMissionHelper() {
		return this.missionHelper;
	}
	
	/**
	 * Get the value of a flight controller parameter.
	 * <p>Parameters that start with "SIM_" are only available on simulation.</p>
	 * @param parameter Parameter to retrieve.
	 * @return The parameter value if the command was successful, or null in an error happens.
	 */
	public Double getParameter(CopterParam parameter) {
		if (UAVParam.loadedParams[numUAV].containsKey(parameter.getId())) {
			return (double) UAVParam.loadedParams[numUAV].get(parameter.getId()).getValue();
		}
		
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_GET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_2 + " " + parameter.getId() + ".");
			return null;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_2 + " " + parameter.getId() + " = " + UAVParam.newParamValue.get(numUAV));
			return UAVParam.newParamValue.get(numUAV);
		}
	}
	
	/**
	 * Get the planned ground speed.
	 * @return (m/s) The planned ground speed.
	 */
	public double getPlannedSpeed() {
		return UAVParam.initialSpeeds[numUAV];
	}

	/**
	 * Get the latest ground speed received from the flight controller.
	 * @return (m/s) Current ground speed.
	 */
	public double getHorizontalSpeed() {
		return UAVParam.uavCurrentData[numUAV].getHorizontalSpeed();
	}

	/**
	 * Get the latest speed, as an array x,y,z speeds
	 * @return (m/s) speed for every axis
	 */
	public double[] getSpeedComponents(){
		return UAVParam.uavCurrentData[numUAV].getSpeedComponents();
	}

	/**
	 * Check if the UAV is flying.
	 * @return Whether the UAV is flying or not.
	 */
	public boolean isFlying() {
		return UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING;
	}
	
	/**
	 * Land a UAV if it is flying, and not already landing.
	 * @return true if the command was successful, or even when it is not needed because the UAV was not flying.
	 */
	public boolean land() {
		FlightMode current = UAVParam.flightMode.get(numUAV);
		if (current.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
				&& current != FlightMode.LAND_ARMED) {
			return this.setFlightMode(FlightMode.LAND);
		}
		return true;
	}
	
	/**
	 * Move the UAV to a new location.
	 * <p>Method designed to update continuously the target location of the UAV.</p>
	 * <p>The UAV must be in GUIDED flight mode.</p>
	 * <p>This method uses the message SET_POSITION_TARGET_GLOBAL_INT, and doesn't wait response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param location Geographic coordinates and relative altitude the UAV has to move to.
	 */
	public void moveTo(Location3DGeo location) {
		UAVParam.target[numUAV].set(location);
	}
	
	/**
	 * Move the UAV to a new location using Threads.
	 * <p>Method designed to send sporadically a new target location for the UAV. Please, do not send this command too often (less than one each 5 seconds), as it has a high cost using threads.</p>
	 * <p>The UAV must be in GUIDED flight mode.</p>
	 * <p>This method uses the message MISSION_ITEM, and waits response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * <p>The Thread sends the command and allows to check when the UAV has reached the target location, or to wait to this event.</p>
	 * @param location Geographic coordinates and relative altitude the UAV has to move to.
	 * @param listener Please, create an anonymous inner element to implement the provided methods.
	 * @return MoveTo A thread that moves the UAV, and continuously checks if it has reached the target location.
	 */
	public MoveTo moveTo(Location3D location, MoveToListener listener) {
		
		return new MoveTo(numUAV, location, listener);
		
	}
	
	/**
	 * Move the UAV in an specific direction, given a speed vector.
	 * <p>Method designed to update continuously the target location of the UAV.</p>
	 * <p>The UAV must be in GUIDED flight mode.</p>
	 * <p>This method uses the message SET_POSITION_TARGET_GLOBAL_INT, and doesn't wait response from the flight controller.
	 * Values are not applied immediately, but each time a message is received from the flight controller.</p>
	 * @param vx (m/s) target speed pointing north.
	 * @param vy (m/s) target speed pointing east.
	 * @param vz (m/s) target speed pointing down.
	 */
	public void moveTo(double vx, double vy, double vz) {
		UAVParam.targetSpeed[numUAV].set(new float[]{(float)vx, (float)vy, (float)vz});
	}
	
	/**
	 * Change the UAV flight mode.
	 * @param mode Flight mode to set.
	 * @return true if the command was successful.
	 */
	public boolean setFlightMode(FlightMode mode) {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newFlightMode[numUAV] = mode;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_MODE);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_MODE) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_MODE) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.FLIGHT_MODE_ERROR_1);
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
						ardusim.sleep(UAVParam.COMMAND_WAIT);
					}
				}
			}
			return true;
		}
	}
	
	/**
	 * Set a new value for a flight controller parameter.
	 * <p>Parameters that start with "SIM_" are only available on simulation.</p>
	 * @param parameter Parameter to modify.
	 * @param value New value.
	 * @return true if the command was successful.
	 */
	public boolean setParameter(CopterParam parameter, double value) {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.newParamValue.set(numUAV, value);
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_1 + " " + parameter.getId() + ".");
			return false;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.PARAMETER_1 + " " + parameter.getId() + " = " + value);
			return true;
		}
	}
	
	/**
	 * Send a command to the flightcontroller to ask for a specific (continuous) message e.g Global_position_int
	 * @param messageId: the message Id defined by Mavlink https://mavlink.io/en/messages/common.html
	 * @return boolean if procedure went correctly or not
	 */
	public boolean requestForMessage(int messageId) {
		return HiddenFunctions.requestForMessage(messageId,numUAV,false);
	}
	
	/**
	 * Change the planned flight speed.
	 * @param speed (m/s) Target speed.
	 * @return true if the command was successful.
	 */
	public boolean setPlannedSpeed(double speed) {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newSpeed[numUAV] = speed;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_SPEED);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.SPEED_2_ERROR);
			return false;
		} else {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.SPEED_2 + " = " + speed);
			return true;
		}
	}
	
	/**
	 * Take off until the target relative altitude: it changes the flight mode to guided, arms engines, and then performs the guided take off. Before that, it changes the flight mode to loiter and overrides the controls of the remote control to avoid a crash that happens with real multicopters.
	 * <p>Please, start the thread returned by this function. Then, you can use <i>Thread.join()</i> to wait until the take off ends, or you can wait in a loop until the listener <i>onCompleteActionPerformed()</i> method updates some shared variable when the take off finishes.
	 * @param altitude (m) Relative altitude to go to.
	 * @param listener Please, create an anonymous inner element to implement the provided methods. If <i>null</i>, you should wait with <i>Thread.join()</i> for the take off process to complete, as <i>onCompleteActionPerformed()</i> method will not be available, and applying actions to the multicopter before finishing the take off is a bad practice, from the safety point of view.
	 * @return TakeOffThread A thread that starts the take off, and continuously checks if it has finished.
	 */
	public TakeOff takeOff(double altitude, TakeOffListener listener) {
		
		return new TakeOff(numUAV, altitude, listener);
		
	}
	
}
