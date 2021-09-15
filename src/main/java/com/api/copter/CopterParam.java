package com.api.copter;

import io.dronefleet.mavlink.common.MavParamType;
import io.dronefleet.mavlink.util.EnumValue;

/** This enumerator includes relevant parameters from the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public enum CopterParam {
	/** Mask of logs enabled */
	LOGGING("LOG_BITMASK", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT32)),
	
	// Information automatically requested to the flight controller for simulations (telem2 is in SR2 in real multicopters)
	/** Times per second to receive the current location from the flight controller. */
	POSITION_FREQUENCY("SR0_POSITION", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** Times per second to receive statistics (CPU load, battery remaining,...) from the flight controller. */
	STATISTICS("SR0_EXT_STAT", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	
	// Battery configuration
	/** (mAh) Battery capacity. */
	BATTERY_CAPACITY("BATT_CAPACITY", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT32)),
	/** Battery monitoring: 0 (disabled), 3 (only voltage), 4 (voltage and current), or 5, 6, 7 (others). */
	BATTERY_MONITOR("BATT_MONITOR", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** (mAh) Second battery capacity. */
	BATTERY_CAPACITY2("BATT2_CAPACITY", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT32)),
	/** Battery failsafe action: 0 (disabled), 1 (Land), 2 (RTL). */
	BATTERY_FAILSAFE_ACTION("FS_BATT_ENABLE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** (V) Voltage limit to trigger failsafe action (0 means disabled). */
	BATTERY_VOLTAGE_DEPLETION_THRESHOLD("FS_BATT_VOLTAGE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** (mAh) Remaining current to trigger failsafe action (0 means disabled, in increments of 50 mAh). */
	BATTERY_CURRENT_DEPLETION_THRESHOLD("FS_BATT_MAH", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),

	// Wind options
	/** (degrees) Wind orientation (0 points north). */
	WIND_DIRECTION("SIM_WIND_DIR", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** (m/s) Wind speed. */
	WIND_SPEED("SIM_WIND_SPD", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** ([0, 0.16] * m/s) Random speed and direction wind applied each time step. */
	WIND_TURBULENCE("SIM_WIND_TURB", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	
	// Other failsafe types options
	/** GCS failsafe action when using RC override: 0 (disabled), 1 (RTL), 2 (complete main.java.com.protocols.mission if flight mode is auto). */
	GCS_LOST_FAILSAFE("FS_GCS_ENABLE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Remote control failsafe: 0 (disabled), 1 (RTL), 2 (complete main.java.com.protocols.mission if flight mode is auto), 3 (Land). */
	REMOTE_LOST_FAILSAFE("FS_THR_ENABLE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Throttle value adopted for failsafe when connection lost with remote control. */
	REMOTE_LOST_FAILSAFE_THROTTLE_VALUE("FS_THR_VALUE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** Crash failsafe. Disarm motors in case of crash: 1 (enable). */
	CRASH_FAILSAFE("FS_CRASH_CHECK", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** GPS failsafe action, used when GPS fix is lost: 1 (Land), 2 (AltHld), 3 (Land even in Stabilize flight mode). */
	GPS_FAILSAFE_ACTION("FS_EKF_ACTION", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** EKF error failsafe: 0.6 (strict), 0.8 (default), 1.0 (relaxed). */
	GPS_FAILSAFE_THRESHOLD("FS_EKF_THRESH", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	
	// Specific parameters requested to the flight controller
	/** Whether only one system id is accepted for commands (1) or not (0). */
	SINGLE_GCS("SYSID_ENFORCE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Identifier of the GCS allowed to send commands (typically 255). */
	GCS_ID("SYSID_MYGCS", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** RC channel used for roll. */
	RCMAP_ROLL("RCMAP_ROLL", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** RC channel used for pitch. */
	RCMAP_PITCH("RCMAP_PITCH", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** RC channel used for throttle. */
	RCMAP_THROTTLE("RCMAP_THROTTLE", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** (us) Dead zone above and below mid throttle for speed control in AltHold, Loiter or PosHold flight modes. */
	DZ_THROTTLE("THR_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** RC channel used for yaw. */
	RCMAP_YAW("RCMAP_YAW", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** (us) Maximum value for RC1 (typically, roll). */
	MAX_RC1("RC1_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC1 (typically, roll). */
	TRIM_RC1("RC1_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC1 (typically, roll). */
	MIN_RC1("RC1_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom (typically, roll). */
	DZ_RC1("RC1_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC2 (typically, pitch). */
	MAX_RC2("RC2_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC2 (typically, pitch). */
	TRIM_RC2("RC2_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC2 (typically, pitch). */
	MIN_RC2("RC2_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom (typically, pitch). */
	DZ_RC2("RC2_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC3 (typically, throttle). */
	MAX_RC3("RC3_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC3 (typically, throttle). */
	TRIM_RC3("RC3_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC3 (typically, throttle). */
	MIN_RC3("RC3_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom (typically, throttle). */
	DZ_RC3("RC3_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC4 (typically, yaw). */
	MAX_RC4("RC4_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC4 (typically, yaw). */
	TRIM_RC4("RC4_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC4 (typically, yaw). */
	MIN_RC4("RC4_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom (typically, yaw). */
	DZ_RC4("RC4_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC5 (always, flight mode). */
	MAX_RC5("RC5_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC5 (always, flight mode). */
	TRIM_RC5("RC5_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC5 (always, flight mode). */
	MIN_RC5("RC5_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom (always, flight mode). */
	DZ_RC5("RC5_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC6. */
	MAX_RC6("RC6_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC6. */
	TRIM_RC6("RC6_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC6. */
	MIN_RC6("RC6_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC6("RC6_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC7. */
	MAX_RC7("RC7_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC7. */
	TRIM_RC7("RC7_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC7. */
	MIN_RC7("RC7_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC7("RC7_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Maximum value for RC8. */
	MAX_RC8("RC8_MAX", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Trim value for RC8. */
	TRIM_RC8("RC8_TRIM", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Minimum value for RC8. */
	MIN_RC8("RC8_MIN", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC8("RC8_DZ", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** Flight custom mode for RC level 1 (x &#8804; 1230). */
	FLTMODE1("FLTMODE1", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Flight custom mode for RC level 2 (1230<x<=1360). */
	FLTMODE2("FLTMODE2", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Flight custom mode for RC level 3 (1360<x<=1490). */
	FLTMODE3("FLTMODE3", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Flight custom mode for RC level 4 (1490<x<=1620). */
	FLTMODE4("FLTMODE4", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Flight custom mode for RC level 5 (1620<x<=1749). */
	FLTMODE5("FLTMODE5", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** Flight custom mode for RC level 6 (x>1749). */
	FLTMODE6("FLTMODE6", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	
	/** (cm) Relative altitude over home location during return to launch. */
	RTL_ALTITUDE("RTL_ALT", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (cm) Loiter relative altitude over home location when not desired to land after return to launch. */
	RTL_ALTITUDE_FINAL("RTL_ALT_FINAL", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT16)),
	/** (cm) Distance to a waypoint to assert that it has been reached. */
	WPNAV_RADIUS("WPNAV_RADIUS", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** Yaw behavior while following a main.java.com.protocols.mission: 0 (Fixed), 1 (Face next waypoint), 2 (Face next waypoint except RTL), 3 (Face along GPS course). */
	WP_YAW_BEHAVIOR("WP_YAW_BEHAVIOR", EnumValue.of(MavParamType.MAV_PARAM_TYPE_INT8)),
	/** (cm/s) Maximum ground speed while in Loiter mode (valid for ArduCopter 3.5.7 or lower). */
	LOITER_SPEED_357("WPNAV_LOIT_SPEED", EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32)),
	/** (cm/s) Maximum ground speed while in Loiter mode (valid for ArduCopter 3.6.0 or higher). */
	LOITER_SPEED_36X("LOIT_SPEED",EnumValue.of(MavParamType.MAV_PARAM_TYPE_REAL32));

	private final String id;
	private final EnumValue<MavParamType> type;

	CopterParam(String id, EnumValue<MavParamType> type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public EnumValue<MavParamType> getType() {
		return type;
	}
}
