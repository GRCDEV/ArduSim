package com.uavController;

import org.javatuples.Quintet;

import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;

/**
 * This class generates and object that contains the most recent information received from the UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class UAVCurrentData {

	private long time;					// (ns) Local time when the location was retrieved from the UAV
	private Location2D location;		// Location in UTM and Geographic coordinates
	private double z, zRelative;		// (m) Altitude
	private double[] speed;				// (m/s) Current speed in the three axes
	private double groundSpeed;			// (m/s) Currrent ground speed
	private double acceleration;		// (m/s^2) Current acceleration
	private double heading;				// (rad) Current heading

	/** Updates the UAV object data. */
	public synchronized void update(long time, Location2D location, double z,
			double zRelative, double[] speed, double groundSpeed, double heading) {
		this.location = location;
		this.z = z;
		this.zRelative = zRelative;

		double acceleration;
		if (this.time != 0) {
			acceleration = (groundSpeed - this.groundSpeed)/(time - this.time)*1000000000L;
		} else {
			acceleration = 0.0;
		}
		this.time = time;
		this.speed = speed;
		this.groundSpeed = groundSpeed;
		this.heading = heading;

		// Filtering the acceleration
		double abs = Math.abs(acceleration);
		if (abs <= UAVParam.MAX_ACCELERATION) { // Upper limit
			if (abs<UAVParam.MIN_ACCELERATION) { // White noise
				this.acceleration = 0;
			} else {
				// Filter
				this.acceleration = UAVParam.ACCELERATION_THRESHOLD * acceleration + (1-UAVParam.ACCELERATION_THRESHOLD) * this.acceleration;
			}
		} else {
			if (acceleration > 0) {
				this.acceleration = UAVParam.MAX_ACCELERATION;
			} else {
				this.acceleration = - UAVParam.MAX_ACCELERATION;
			}
		}
	}

	/** Returns the current value of the most relevant data:
	 * <p><b>Long</b>. time.</p>
	 * <p><b>UTMCoordinates</b> UTM coordinates.</p>
	 * <p><b>double</b>. Absolute altitude.</p>
	 * <p><b>double</b>. Speed.</p>
	 * <p><b>double</b>. Acceleration.</p> */
	public synchronized Quintet<Long, Location2DUTM, Double, Double, Double> getData() {
		Location2DUTM location = null;
		if (this.location != null) {
			location = this.location.getUTMLocation();
		}
		return Quintet.with(this.time, location, this.z, this.groundSpeed, this.acceleration);
	}

	/** Returns the current location in UTM coordinates. */
	public synchronized Location2DUTM getUTMLocation() {
		if (this.location != null) {
			return this.location.getUTMLocation();
		}
		return null;
	}
	
	/** Returns the current location in Geographic coordinates. */
	public synchronized Location2DGeo getGeoLocation() {
		if (this.location != null) {
			return this.location.getGeoLocation();
		}
		return null;
	}
	
	/** Return the current location in both UTM and Geographic coordinates. */
	public synchronized Location2D getLocation() {
		return new Location2D(this.location);
	}

	/** Returns the current relative altitude (m). */
	public synchronized double getZRelative() {
		return this.zRelative;
	}

	/** Returns the current absolute altitude (m). */
	public synchronized double getZ() {
		return this.z;
	}

	/** Returns the current ground speed (m/s). */
	public synchronized double getHorizontalSpeed() {
		return this.groundSpeed;
	}
	
	/** Returns array of speeds x,y,z */
	public synchronized double[] getSpeedComponents(){
		return this.speed;
	}

	/** Returns the current heading (rad). */
	public synchronized double getHeading() {
		return this.heading;
	}
}
