package com.api.pojo;

/** This class generates objects to store the RC values that are received or sent to the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class RCValues {
	/** (degrees) Roll. */
	public int roll;
	/** (degrees) Pitch. */
	public int pitch;
	/** (degrees) Throttle. */
	public int throttle;
	/** (degrees) Yaw. */
	public int yaw;
	
	@SuppressWarnings("unused")
	private RCValues() {}
	
	public RCValues(int roll, int pitch, int throttle, int yaw) {
		this.roll = roll;
		this.pitch = pitch;
		this.throttle = throttle;
		this.yaw = yaw;
	}
}
