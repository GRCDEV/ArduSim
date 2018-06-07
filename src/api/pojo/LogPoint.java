package api.pojo;

/** This class generates a point for each of the UAV locations during its movement, including time, heading, etc. */

public class LogPoint extends Point3D {
	
	private static final long serialVersionUID = 1L;
	public double heading;
	public long time;
	public double speed;
	public boolean inTest;
	
	public LogPoint(long time, double x, double y, double z, double heading, double speed, boolean inTest) {
		super(x, y, z);
		this.heading = heading;
		this.time = time;
		this.speed = speed;
		this.inTest = inTest;
	}
}
