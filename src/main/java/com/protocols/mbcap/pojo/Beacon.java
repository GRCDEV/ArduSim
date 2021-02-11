package com.protocols.mbcap.pojo;

import es.upv.grc.mapper.Location3DUTM;

import java.util.List;

/** This class generates and updates the beacons sent by MBCAP protocol to detect risks of collision.
 * <p>It also allows to convert the object to message and viceversa.</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Beacon implements Comparable<Beacon> {
	
	public long uavId;			// Sending UAV identifier
	public short event;			// Number of collision risk situations already solved
	public short state;			// Current protocol state
	public int statePos;		// Current protocol state position in the buffer so it could be updated
	public boolean isLanding;	// Whether the UAV is landing and the protocol must be disabled
	public int isLandingPos;	// The position in the buffer so it could be updated
	public long idAvoiding;		// Identifier of the UAV with which this UAV is avoiding a collision, if any
	public int idAvoidingPos;	// Avoiding UAV position in the buffer so it could be updated
	public float plannedSpeed;	// (m/s) Planned speed
	public double speed;		// (m/s) Current speed
	public long time;			// (ns) On a sending beacon, it is the data capture local time,
								//      On a receiving beacon, it means the data capture time corrected to local time
	public int timePos;		// Time position in the buffer so it could be updated
	public List<Location3DUTM> points;// Predicted positions

	public int dataSize;		// Buffer data size
	public byte[] sendBuffer;
	
	/** Constructor as auxiliary method used by buildToSend(int) method. */
	public Beacon() {
	}
	
	/**
	 * Make a NOT FULL deep copy of a beacon. Only useful to log the predicted points list on disk, as the ID, time and predictions are no longer modified.
	 * @param beacon Beacon to copy
	 */
	public Beacon(Beacon beacon) {
		this.uavId = beacon.uavId;
		this.time = beacon.time;
		this.points = beacon.points;
	}

	/** Two beacons are equals if they came from the same UAV. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof Beacon)) {
			return false;
		}
		return this.uavId == ((Beacon) obj).uavId;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.uavId);
	}

	/** Compares if two beacons came from the same UAV. */
	@Override
	public int compareTo(Beacon o) {
		if (this.uavId < o.uavId)
			return -1;
		if (this.uavId == o.uavId)
			return 0;
		return 1;
	}

	@Override
	public String toString() {
		String res = "[id=" + this.uavId + ",ev=" + this.event + ",state=" + this.state + ",landing=" + this.isLanding
				+ ",idav=" + this.idAvoiding + ",plannedSpeed=" + this.plannedSpeed + ",speed=" + this.speed + ",time=" + this.time + ",size=" + this.points.size();
		if (this.points.size()>0) {
			Location3DUTM p;
			for (int i=0; i<this.points.size(); i++) {
				p = this.points.get(i);
				res += ",(" + p.x + "," + p.y + "," + p.z + ")";
			}
		}
		res += "]";
		return res;
	}

}
