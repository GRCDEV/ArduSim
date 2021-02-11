package com.api.pojo.location;

import es.upv.grc.mapper.Location3DUTM;

/** This class generates simplified waypoints, including only UTM coordinates and the sequence number in the main.java.com.protocols.mission.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class WaypointSimplified extends Location3DUTM {
	private static final long serialVersionUID = 1L;
	/** Sequence number of this waypoint in the main.java.com.protocols.mission it belongs to. */
	public int numSeq;		// Waypoint position in the main.java.com.protocols.mission sequence

	/**
	 * Create a simplified version of a waypoint.
	 * @param numSeq Sequence number of this waypoint in the main.java.com.protocols.mission (it starts always on 0).
	 * @param x (m) UTM easting.
	 * @param y (m) UTM northing.
	 * @param z (m) Altitude
	 */
	public WaypointSimplified(int numSeq, double x, double y, double z) {
		super(x, y, z);
		this.numSeq = numSeq;
	}
	
	/**
	 * Make a deep copy of a simplified waypoint.
	 * @param wp Simplified waypoint to copy.
	 */
	public WaypointSimplified(WaypointSimplified wp) {
		this.x = wp.x;
		this.y = wp.y;
		this.z = wp.z;
		this.numSeq = wp.numSeq;
	}

	@Override
	public String toString() {
		return "WP" + this.numSeq + "(" + this.x + "," + this.y + "," + this.z + ")";
	}


}
