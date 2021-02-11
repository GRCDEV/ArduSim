package com.api.pojo.location;

import com.api.API;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import io.dronefleet.mavlink.annotations.MavlinkEntryInfo;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import io.dronefleet.mavlink.common.MissionItem;
import io.dronefleet.mavlink.util.EnumValue;
import com.setup.Text;

import java.util.Objects;

/** This class generates waypoints used in missions.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Waypoint {

	private int numSeq; // Waypoint sequence number
	private MavFrame frame;	// Coordinate frame used
	private int isCurrent; // 1 When this is the current waypoint
	private int autoContinue; // 1 If the UAV must go on to the next when arriving to this waypoint
	private EnumValue<MavCmd> command; // What to do when arriving to this waypoint as specified in MAV_CMD enumerator
	private double param1;
	private double param2;
	private double param3;
	private double param4;
	private double param5;
	private double param6;
	private double param7;

	/** Not needed. */
	@SuppressWarnings("unused")
	private Waypoint() {
	}

	/** Waypoint constructor using all of its attributes. */
	public Waypoint(int numSeq, boolean isCurrent, MavFrame frame, EnumValue<MavCmd> command, double param1, double param2, double param3,
			double param4, double param5, double param6, double param7, int autoContinue) {
		this.numSeq = numSeq;
		if (isCurrent) {
			this.isCurrent = 1;
		} else {
			this.isCurrent = 0;
		}
		this.frame = frame;
		this.command = command;
		this.param1 = param1;
		this.param2 = param2;
		this.param3 = param3;
		this.param4 = param4;
		this.param5 = param5;
		this.param6 = param6;
		this.param7 = param7;
		this.autoContinue = autoContinue;
	}
	
	/**
	 * Make a deep copy of a waypoint.
	 * @param wp Waypoint to copy.
	 */
	public Waypoint(Waypoint wp) {
		this.numSeq = wp.numSeq;
		this.isCurrent = wp.isCurrent;
		this.frame = wp.frame;
		this.command = wp.command;
		this.param1 = wp.param1;
		this.param2 = wp.param2;
		this.param3 = wp.param3;
		this.param4 = wp.param4;
		this.param5 = wp.param5;
		this.param6 = wp.param6;
		this.param7 = wp.param7;
		this.autoContinue = wp.autoContinue;
	}

	/** Returns the sequence number of this waypoint in the main.java.com.protocols.mission, beginning in 0. */
	public int getNumSeq() {
		return this.numSeq;
	}

	/** Returns this waypoint command. */
	public EnumValue<MavCmd> getCommand() {
		return this.command;
	}

	/** Returns this waypoint latitude (function only compatible with waypoints with latitude and longitude values in parameters 5 and 6). */
	public double getLatitude() {
		return this.param5;
	}
	
	/** Sets this waypoint latitude (function only compatible with waypoints with latitude and longitude values in parameters 5 and 6). */
	public void setLatitude(double latitude) {
		this.param5 = latitude;
	}

	/** Returns this waypoint longitude (function only compatible with waypoints with latitude and longitude values in parameters 5 and 6). */
	public double getLongitude() {
		return this.param6;
	}
	
	/** Sets this waypoint longitude (function only compatible with waypoints with latitude and longitude values in parameters 5 and 6). */
	public void setLongitude(double longitude) {
		this.param6 = longitude;
	}
	
	/**
	 * Get the UTM coordinates (function only compatible with waypoints with latitude and longitude values in parameters 5 and 6).
	 * @return Location in UTM coordinates.
	 */
	public Location2DUTM getUTM() {
		return Location2DGeo.getUTM(this.param5, this.param6);
	}

	/** Returns this waypoint altitude (function only compatible with waypoints with altitude values in parameter 7). */
	public double getAltitude() {
		return this.param7;
	}
	
	/** Sets this waypoint altitude (function only compatible with waypoints with altitude values in parameter 7). */
	public void setAltitude(double altitude) {
		this.param7 = altitude;
	}

	/** Returns whether this waypoint is the current one. */
	public boolean isCurrent() {
		return this.isCurrent == 1;
	}

	/** Generates a MAVLink message with this waypoint data. */
	public MissionItem.Builder getMessage() {
		return MissionItem.builder()
			.seq(this.numSeq)
			.frame(this.frame)
			.current(this.isCurrent)
			.autocontinue(this.autoContinue)
			.command(this.command)
			.param1((float)this.param1)
			.param2((float)this.param2)
			.param3((float)this.param3)
			.param4((float)this.param4)
			.x((float)this.param5)
			.y((float)this.param6)
			.z((float)this.param7);
	}

	@Override
	/* String representation of a waypoint. */
	public String toString() {
		String res = this.numSeq + "\t" + this.isCurrent + "\t";
		try {
			res += MavFrame.class.getField(this.frame.name()).getAnnotation(MavlinkEntryInfo.class).value();
		} catch (NoSuchFieldException | SecurityException e) {
			res += 3;	// MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT
			API.getGUI(0).logVerbose(Text.WAYPOINT_PRINT_ERROR);
		}
		res += "\t" + this.command + "\t" + this.param1 + "\t"
				+ this.param2 + "\t" + this.param3 + "\t" + this.param4 + "\t" + this.param5 + "\t" + this.param6 + "\t"
				+ this.param7 + "\t" + this.autoContinue;
		return res;
	}

	/** Two waypoints are equals even when one of them is the current and the other not. */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		
		if (!(obj instanceof Waypoint)) {
			return false;
		}
		Waypoint b = (Waypoint) obj;
		return this.numSeq == b.numSeq && this.autoContinue == b.autoContinue
				&& this.command == b.command && Double.compare(this.param1, b.param1) == 0
				&& Double.compare(this.param2, b.param2) == 0 && Double.compare(this.param3, b.param3) == 0
				&& Double.compare(this.param4, b.param4) == 0 && Double.compare(this.param5, b.param5) == 0
				&& Double.compare(this.param6, b.param6) == 0 && Double.compare(this.param7, b.param7) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.numSeq, this.autoContinue, this.command, this.param1, this.param2,
				this.param3, this.param4, this.param5, this.param6, this.param7);
	}
}
