package com.api.pojo;

import io.dronefleet.mavlink.common.MavMode;
import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.util.EnumValue;

import com.setup.Text;

/** UAV flight modes available.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public enum FlightMode {
	/** Self-levels the roll and pitch axis. */
	STABILIZE(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			0, Text.STABILIZE),
	/** Self-levels the roll and pitch axis. Engines are spinning. */
	STABILIZE_ARMED(EnumValue.of(MavMode.MAV_MODE_STABILIZE_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(), // only 209, not as the rest (217)
			0, Text.STABILIZE_ARMED),
	/** Navigates to single points commanded by GCS. */
	GUIDED(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			4, Text.GUIDED),
	/** Navigates to single points commanded by GCS. Engines are spinning. */
	GUIDED_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			4, Text.GUIDED_ARMED),
	/** Executes pre-defined mission. */
	AUTO(EnumValue.of(MavMode.MAV_MODE_AUTO_DISARMED).value() -
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_AUTO_ENABLED).value() +	// Not valid with Custom enabled
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			3, Text.AUTO),
	/** Executes pre-defined mission. Engines are spinning. */
	AUTO_ARMED(EnumValue.of(MavMode.MAV_MODE_AUTO_ARMED).value() -
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_AUTO_ENABLED).value() +	// Not valid with Custom enabled
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			3, Text.AUTO_ARMED),
	/** Holds altitude and position, uses GPS for movements. */
	LOITER(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			5, Text.LOITER),
	/** Holds altitude and position, uses GPS for movements. Engines are spinning. */
	LOITER_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			5, Text.LOITER_ARMED),
	/** Returns above takeoff location, may also include landing. */
	RTL(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			6, Text.RTL),
	/** Returns above takeoff location, may also include landing. Engines are spinning. */
	RTL_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			6, Text.RTL_ARMED),
	/** Automatically circles a point in front of the vehicle. */
	CIRCLE(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			7, Text.CIRCLE),
	/** Automatically circles a point in front of the vehicle. Engines are spinning. */
	CIRCLE_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			7, Text.CIRCLE_ARMED),
	/** Like loiter, but manual roll and pitch when sticks not centered. */
	POSHOLD(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			16, Text.POSHOLD),
	/** Like loiter, but manual roll and pitch when sticks not centered. Engines are spinning. */
	POSHOLD_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			16, Text.POSHOLD_ARMED),
	/** Brings copter to an immediate stop. */
	BRAKE(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			17, Text.BRAKE),
	/** Brings copter to an immediate stop. Engines are spinning. */
	BRAKE_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			17, Text.BRAKE_ARMED),
	/** Avoid collisions using ADS-B sensor, if properly configured. */
	AVOID_ADSB(EnumValue.of(MavMode.MAV_MODE_GUIDED_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			19, Text.AVOID_ADSB),
	/** Avoid collisions using ADS-B sensor, if properly configured. Engines are spinning. */
	AVOID_ADSB_ARMED(EnumValue.of(MavMode.MAV_MODE_GUIDED_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			19, Text.AVOID_ADSB_ARMED),
	/** Reduces altitude to ground level, attempts to go straight down. */
	LAND(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			9, Text.LAND),
	/** Reduces altitude to ground level, attempts to go straight down. Engines are spinning. */
	LAND_ARMED(EnumValue.of(MavMode.MAV_MODE_STABILIZE_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			9, Text.LAND_ARMED),
	/** Holds position after a throwing takeoff. */
	THROW(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			18, Text.THROW),
	/** Like stabilize, but coordinates yaw with roll like a plane. */
	DRIFT(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			11, Text.DRIFT),
	/** Holds altitude and self-levels the roll & pitch. */
	ALT_HOLD(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			2, Text.ALT_HOLD),
	/** Holds altitude and self-levels the roll & pitch. Engines are spinning. */
	ALT_HOLD_ARMED(EnumValue.of(MavMode.MAV_MODE_STABILIZE_ARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			2, Text.ALT_HOLD),
	/** Alt-hold, but holds pitch & roll when sticks centered. */
	SPORT(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			13, Text.SPORT),
	/** Holds attitude, no self-level. */
	ACRO(EnumValue.of(MavMode.MAV_MODE_STABILIZE_DISARMED).value() + 
			EnumValue.of(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).value(),
			1, Text.ACRO);

	private final int baseMode;
	private final long customMode;
	private final String modeName;

	FlightMode(int baseMode, long customMode, String modeName) {
		this.baseMode = baseMode;
		this.customMode = customMode;
		this.modeName = modeName;
	}

	public int getBaseMode() {
		return baseMode;
	}

	public long getCustomMode() {
		return customMode;
	}
	
	public String getMode() {
		return modeName;
	}

	/**
	 * Return the ardupilot flight mode corresponding to the base and custom values.
	 * <p>If no valid flight mode is found, it returns null.</p> */
	public static FlightMode getMode(int base, long custom) {
		for (FlightMode p : FlightMode.values()) {
			if (p.baseMode == base && p.customMode == custom) {
				return p;
			}
		}
		return null;
	}
}
