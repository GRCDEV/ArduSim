package api.pojo;

import api.Tools;

/** This class generates a point for each of the UAV locations during its movement, including time, heading, etc.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class LogPoint extends Point3D implements Cloneable {
	
	private static final long serialVersionUID = 1L;
	private double heading;
	private long nanoTime;
	private java.lang.Double elapsedTime = null;
	private double speed;
	private int simulatorState;
	
	@SuppressWarnings("unused")
	private LogPoint() {}
	
	public LogPoint(long nanoTime, double x, double y, double z, double heading, double speed, int simulatorState) {
		super(x, y, z);
		this.heading = heading;
		this.nanoTime = nanoTime;
		this.speed = speed;
		this.simulatorState = simulatorState;
	}

	public double getHeading() {
		return heading;
	}

	/** Get arbitrary time (Java VM time) in nanoseconds, when the data was generated. */
	public long getNanoTime() {
		return nanoTime;
	}
	
	/** Subtract starting time to get the elapsed time from the beginning of the setup phase, or the experiment.
	 * Method internally used by ArduSim. */
	public void setTime(long startingTime) {
		this.nanoTime = this.nanoTime - startingTime;
		this.elapsedTime = Tools.round(((double) this.nanoTime) / 1000000000l, 9);
	}
	
	/** Get the time when the data was generated.
	 * <p>Returns null if the data was collected out from the Setup and Test states, or if the method <i>setTime(long)</i> was not previously used by ArduSim.</p> */
	public java.lang.Double getTime() {
		return this.elapsedTime;
	}

	public double getSpeed() {
		return speed;
	}

	public int getSimulatorState() {
		return simulatorState;
	}

	@Override
	public LogPoint clone() {
		return new LogPoint(this.nanoTime, this.x, this.y, this.z, this.heading, this.speed, this.simulatorState);
	}
	
	
}
