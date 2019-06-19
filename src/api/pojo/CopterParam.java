package api.pojo;

import org.mavlink.messages.MAV_PARAM_TYPE;

/** This enumerator includes relevant parameters from the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public enum CopterParam {
	/** Mask of logs enabled */
	LOGGING("LOG_BITMASK", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),
	
	// Information automatically requested to the flight controller
	/** Times per second to receive the current location from the flight controller. */
	POSITION_FREQUENCY("SR0_POSITION", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** Times per second to receive statistics (CPU load, battery remaining,...) from the flight controller. */
	STATISTICS("SR0_EXT_STAT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	
	// Battery configuration
	/** (mAh) Battery capacity. */
	BATTERY_CAPACITY("BATT_CAPACITY", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),
	/** Battery monitoring: 0 (disabled), 3 (only voltage), 4 (voltage and current, or 5, 6, 7 (others). */
	BATTERY_MONITOR("BATT_MONITOR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	/** (mAh) Second battery capacity. */
	BATTERY_CAPACITY2("BATT2_CAPACITY", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32),
	/** Battery failsafe action: 0 (disabled), 1 (Land), 2 (RTL). */
	BATTERY_FAILSAFE_ACTION("FS_BATT_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** (V) Voltage limit to trigger failsafe action (0 means disabled). */
	BATTERY_VOLTAGE_DEPLETION_THRESHOLD("FS_BATT_VOLTAGE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	/** (mAh) Remaining current to trigger failsafe action (0 means disabled, in increments of 50 mAh). */
	BATTERY_CURRENT_DEPLETION_THRESHOLD("FS_BATT_MAH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),

	// Wind options
	/** (degrees) Wind orientation (0 points north). */
	WIND_DIRECTION("SIM_WIND_DIR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	/** (m/s) Wind speed. */
	WIND_SPEED("SIM_WIND_SPD", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	/** ([0, 0.16] * m/s) Random speed and direction wind applied each time step. */
	WIND_TURBULENCE("SIM_WIND_TURB", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	
	// Other failsafe types options
	/** GCS failsafe action when using RC override: 0 (disabled), 1 (RTL), 2 (complete mission if flight mode is auto). */
	GCS_LOST_FAILSAFE("FS_GCS_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Remote control failsafe: 0 (disabled), 1 (RTL), 2 (complete mission if flight mode is auto), 3 (Land). */
	REMOTE_LOST_FAILSAFE("FS_THR_ENABLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Throttle value adopted for failsafe when connection lost with remote control. */
	REMOTE_LOST_FAILSAFE_THROTTLE_VALUE("FS_THR_VALUE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** Crash failsafe. Disarm motors in case of crash: 1 (enable). */
	CRASH_FAILSAFE("FS_CRASH_CHECK", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** GPS failsafe action, used when GPS fix is lost: 1 (Land), 2 (AltHld), 3 (Land even in Stabilize flight mode). */
	GPS_FAILSAFE_ACTION("FS_EKF_ACTION", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** EKF error failsafe: 0.6 (strict), 0.8 (default), 1.0 (relaxed). */
	GPS_FAILSAFE_THRESHOLD("FS_EKF_THRESH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	
	// Specific parameters requested to the flight controller
	/** Whether only one system id is accepted for commands (1) or not (0). */
	SINGLE_GCS("SYSID_ENFORCE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Identifier of the GCS allowed to send commands (typically 255). */
	GCS_ID("SYSID_MYGCS", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** RC channel used for roll. */
	RCMAP_ROLL("RCMAP_ROLL", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** RC channel used for pitch. */
	RCMAP_PITCH("RCMAP_PITCH", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** RC channel used for throttle. */
	RCMAP_THROTTLE("RCMAP_THROTTLE", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** (us) Dead zone above and below mid throttle for speed control in AltHold, Loiter or PosHold flight modes. */
	DZ_THROTTLE("THR_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** RC channel used for yaw. */
	RCMAP_YAW("RCMAP_YAW", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** (us) Maximum value for RC1 (typically, roll). */
	MAX_RC1("RC1_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC1 (typically, roll). */
	TRIM_RC1("RC1_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC1 (typically, roll). */
	MIN_RC1("RC1_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom (typically, roll). */
	DZ_RC1("RC1_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC2 (typically, pitch). */
	MAX_RC2("RC2_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC2 (typically, pitch). */
	TRIM_RC2("RC2_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC2 (typically, pitch). */
	MIN_RC2("RC2_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom (typically, pitch). */
	DZ_RC2("RC2_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC3 (typically, throttle). */
	MAX_RC3("RC3_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC3 (typically, throttle). */
	TRIM_RC3("RC3_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC3 (typically, throttle). */
	MIN_RC3("RC3_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom (typically, throttle). */
	DZ_RC3("RC3_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC4 (typically, yaw). */
	MAX_RC4("RC4_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC4 (typically, yaw). */
	TRIM_RC4("RC4_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC4 (typically, yaw). */
	MIN_RC4("RC4_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom (typically, yaw). */
	DZ_RC4("RC4_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC5 (always, flight mode). */
	MAX_RC5("RC5_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC5 (always, flight mode). */
	TRIM_RC5("RC5_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC5 (always, flight mode). */
	MIN_RC5("RC5_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom (always, flight mode). */
	DZ_RC5("RC5_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC6. */
	MAX_RC6("RC6_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC6. */
	TRIM_RC6("RC6_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC6. */
	MIN_RC6("RC6_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC6("RC6_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC7. */
	MAX_RC7("RC7_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC7. */
	TRIM_RC7("RC7_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC7. */
	MIN_RC7("RC7_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC7("RC7_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Maximum value for RC8. */
	MAX_RC8("RC8_MAX", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Trim value for RC8. */
	TRIM_RC8("RC8_TRIM", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Minimum value for RC8. */
	MIN_RC8("RC8_MIN", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (us) Dead zone around trim or bottom. */
	DZ_RC8("RC8_DZ", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** Flight custom mode for RC level 1 (x<=1230). */
	FLTMODE1("FLTMODE1", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Flight custom mode for RC level 2 (1230<x<=1360). */
	FLTMODE2("FLTMODE2", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Flight custom mode for RC level 3 (1360<x<=1490). */
	FLTMODE3("FLTMODE3", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Flight custom mode for RC level 4 (1490<x<=1620). */
	FLTMODE4("FLTMODE4", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Flight custom mode for RC level 5 (1620<x<=1749). */
	FLTMODE5("FLTMODE5", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** Flight custom mode for RC level 6 (x>1749). */
	FLTMODE6("FLTMODE6", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	
	/** (cm) Relative altitude over home location during return to launch. */
	RTL_ALTITUDE("RTL_ALT", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (cm) Loiter relative altitude over home location when not desired to land after return to launch. */
	RTL_ALTITUDE_FINAL("RTL_ALT_FINAL", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT16),
	/** (cm) Distance to a waypoint to assert that it has been reached. */
	WPNAV_RADIUS("WPNAV_RADIUS", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32),
	/** Yaw behavior while following a mission: 0 (Fixed), 1 (Face next waypoint), 2 (Face next waypoint except RTL), 3 (Face along GPS course). */
	WP_YAW_BEHAVIOR("WP_YAW_BEHAVIOR", MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8),
	/** (cm/s) Maximum ground speed while in Loiter mode (valid for ArduCopter 3.5.7 or lower). */
	LOITER_SPEED_357("WPNAV_LOIT_SPEED", MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32);

	private final String id;
	private final int type;

	private CopterParam(String id, int type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public int getType() {
		return type;
	}
}
