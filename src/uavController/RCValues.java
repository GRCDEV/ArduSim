package uavController;

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
