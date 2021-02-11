package com.api.pojo.location;

import com.api.API;
import es.upv.grc.mapper.Location3DUTM;

/** This class generates points for each of the UAV locations during its movement, including time, heading, etc. We use it to log the flight path to file.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class LogPoint extends Location3DUTM {
	
	private static final long serialVersionUID = 1L;
	/** (m) Relative altitude over home location. */
	public double zRel;
	private double heading;
	private long nanoTime;
	private java.lang.Double elapsedTime = null;
	private double speed;
	private int simulatorState;
	
	protected LogPoint() {}
	
	
	/**
	 * Create a point to log data to file.
	 * @param nanoTime (ns) Arbitrary time (Java VM time) in nanoseconds, when the data was generated.
	 * @param x (m) UTM easting.
	 * @param y (m) UTM northing.
	 * @param z (m) Absolute altitude.
	 * @param zRel (m) Relative altitude.
	 * @param heading (rad) Yaw.
	 * @param speed (m/s) Ground speed.
	 * @param simulatorState Simulator state given the finite state machine that rules ArduSim.
	 */
	public LogPoint(long nanoTime, double x, double y, double z, double zRel, double heading, double speed, int simulatorState) {
		super(x, y, z);
		this.zRel = zRel;
		this.heading = heading;
		this.nanoTime = nanoTime;
		this.speed = speed;
		this.simulatorState = simulatorState;
	}
	
	/**
	 * Make deep copy of a point.
	 * @param point Point to copy.
	 */
	public LogPoint(LogPoint point) {
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		this.zRel = point.zRel;
		this.heading = point.heading;
		this.nanoTime = point.nanoTime;
		this.speed = point.speed;
		this.simulatorState = point.simulatorState;
		this.elapsedTime = point.elapsedTime;
	}

	/**
	 * Get the heading of the UAV.
	 * @return (rad) Yaw.
	 */
	public double getHeading() {
		return heading;
	}

	/**
	 * Get arbitrary time (Java VM time), when the data was generated.
	 * @return (ns) Time instant when the data was generated.
	 */
	public long getNanoTime() {
		return nanoTime;
	}
	
	/**
	 * Subtract starting time to get the elapsed time from the beginning of the setup phase, or the experiment.
	 * Method internally used by ArduSim.
	 * @param startingTime (ns) arbitrary time (Java VM time), when the setup or test phase was started.
	 */
	public void setTime(long startingTime) {
		this.nanoTime = this.nanoTime - startingTime;
		this.elapsedTime = API.getValidationTools().roundDouble(((double) this.nanoTime) / 1000000000L, 9);
	}
	
	/**
	 * Get the time when the data was generated.
	 * @return The instant when the data was generated, or null if the data was collected out from the Setup and Test states, or if the method <i>setTime(long)</i> was not previously used by ArduSim.</p>
	 */
	public java.lang.Double getTime() {
		return this.elapsedTime;
	}

	/**
	 * Get the ground speed.
	 * @return (m/s) Ground speed when the data was generated.
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Get the simulator state.
	 * @return The simulator state given the finite state machine that rules ArduSim.
	 */
	public int getSimulatorState() {
		return simulatorState;
	}
	
}
