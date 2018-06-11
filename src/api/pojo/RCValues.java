package api.pojo;

/** This class generates objects to store the RC values that are received or sent to the flight controller. */

public class RCValues {
	public int roll, pitch, throttle, yaw;
	
	@SuppressWarnings("unused")
	private RCValues() {}
	
	public RCValues(int roll, int pitch, int throttle, int yaw) {
		this.roll = roll;
		this.pitch = pitch;
		this.throttle = throttle;
		this.yaw = yaw;
	}
}