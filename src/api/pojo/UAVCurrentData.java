package api.pojo;

import java.awt.geom.Point2D;

import org.javatuples.Quintet;

import uavLogic.UAVParam;

/** This class generates and object that contains the most recent information received from the UAV. */

public class UAVCurrentData {

	private long time;				// (ns) Local time when the location was retrieved from the UAV
	private Point2D.Double location;		// (m) X,Y UTM coordinates
	private double z, zRelative;	// (m) Altitude
	private double speed;			// (m/s) Currrent speed
	private double acceleration;	// (m/s^2) Current acceleration

	/** Updates the UAV object data. */
	public synchronized void update(long time, Point2D.Double location, double z,
			double zRelative, double speed) {
		this.location = location;
		this.z = z;
		this.zRelative = zRelative;

		double acceleration;
		if (this.time != 0) {
			acceleration = (speed - this.speed)/(time - this.time)*1000000000l;
		} else {
			acceleration = 0.0;
		}
		this.time = time;
		this.speed = speed;

		// Filtering the acceleration
		double abs = Math.abs(acceleration);
		if (abs <= UAVParam.MAX_ACCELERATION) { // Upper limit
			if (abs<UAVParam.MIN_ACCELERATION) { // White noise
				this.acceleration = 0;
			} else {
				// Filter
				this.acceleration = UAVParam.ACCELERATION_THRESHOLD * acceleration + (1-UAVParam.ACCELERATION_THRESHOLD) * this.acceleration;
			}
		}
	}

	/** Returns the current value of the most relevant data:
	 * <p>Long. time.
	 * <p>Point2D.Double. UTM coordinates.
	 * <p>double. Absolute altitude.
	 * <p>double. Speed.
	 * <p>double. Acceleration. */
	public synchronized Quintet<Long, java.awt.geom.Point2D.Double, Double, Double, Double> getData() {
		return Quintet.with(this.time, this.location, this.z, this.speed, this.acceleration);
	}

	/** Returns the current position (x,y). */
	public synchronized Point2D.Double getUTMLocation() {
		if (location == null) return location;
		else return new Point2D.Double(this.location.x, this.location.y);
	}

	/** Returns the current relative altitude (m). */
	public synchronized double getZRelative() {
		return this.zRelative;
	}

	/** Returns the current absolute altitude (m). */
	public synchronized double getZ() {
		return this.z;
	}

	/** Returns the current speed (m/s). */
	public synchronized double getSpeed() {
		return this.speed;
	}
}
