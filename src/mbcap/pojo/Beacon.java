package mbcap.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.javatuples.Quintet;
import org.mavlink.messages.MAV_CMD;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.FlightMode;
import api.pojo.location.Location3DUTM;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Waypoint;
import main.api.Copter;
import main.api.communications.CommLink;
import main.uavController.UAVParam;
import mbcap.gui.MBCAPGUIParam;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPHelper;

/** This class generates and updates the beacons sent by MBCAP protocol to detect risks of collision.
 * <p>It also allows to convert the object to message and viceversa.</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Beacon implements Comparable<Beacon> {

	private int numUAV;			// UAV position on arrays
	
	public long uavId;			// Sending UAV identifier
	public short event;			// Number of collision risk situations already solved
	public short state;			// Current protocol state
	private int statePos;		// Current protocol state position in the buffer so it could be updated
	public boolean isLanding;	// Whether the UAV is landing and the protocol must be disabled
	private int isLandingPos;	// The position in the buffer so it could be updated
	public long idAvoiding;		// Identifier of the UAV with which this UAV is avoiding a collision, if any
	private int idAvoidingPos;	// Avoiding UAV position in the buffer so it could be updated
	public float plannedSpeed;	// (m/s) Planned speed
	public double speed;		// (m/s) Current speed
	public long time;			// (ns) On a sending beacon, it is the data capture local time,
								//      On a receiving beacon, it means the data capture time corrected to local time
	private int timePos;		// Time position in the buffer so it could be updated
	public List<Location3DUTM> points;// Predicted positions

	private int dataSize;		// Buffer data size
	private byte[] sendBuffer;
	private Input in;
	private Output out;
	
	private Waypoint lastWP;
	private Location2DUTM lastWPUTM;

	/** Constructor as auxiliary method used by buildToSend(int) method. */
	private Beacon() {
	}
	
	/**
	 * Make a NOT FULL deep copy of a beacon. Only useful to log the predicted points list on disk, as the ID, time and predictions are no longer modified.
	 * @param beacon Beacon to copy
	 */
	public Beacon(Beacon beacon) {
		this.numUAV = beacon.numUAV;
		this.uavId = beacon.uavId;
		this.time = beacon.time;
		this.points = beacon.points;
	}

	/** Creates a beacon to send data. Don't use to receive data.
	 * <p>This method always should be followed by the getBuffer() method.</p> */
	public static Beacon buildToSend(int numUAV) {
		Copter copter = API.getCopter(numUAV);
		// 1. Getting the needed information
		long uavId = API.getCopter(numUAV).getID();
		Quintet<Long, Location2DUTM, Double, Double, Double> uavcurrentData = copter.getData();
		Long time = uavcurrentData.getValue0();
		Location2DUTM currentLocation = uavcurrentData.getValue1();
		double currentZ = uavcurrentData.getValue2();
		float plannedSpeed = (float)copter.getPlannedSpeed();
		double speed = uavcurrentData.getValue3();
		double acceleration = uavcurrentData.getValue4();
		List<Location3DUTM> points = MBCAPHelper.getPredictedPath(numUAV, speed, acceleration, currentLocation, currentZ);
		MBCAPGUIParam.predictedLocation.set(numUAV, points);

		// 2. Beacon building
		Beacon res = new Beacon();
		res.numUAV = numUAV;
		res.time = time;
		res.uavId = uavId;
		res.event = (short)MBCAPParam.event.get(numUAV);
		res.state = MBCAPParam.state[numUAV].getId();
		res.idAvoiding = MBCAPParam.idAvoiding.get(numUAV);
		res.plannedSpeed = plannedSpeed;
		res.speed = speed;
		res.points = points;

		// 3. Buffer building
		res.sendBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		res.out = new Output(res.sendBuffer);
		res.out.clear();
		res.out.writeLong(res.uavId);
		res.out.writeShort(res.event);
		res.statePos = res.out.position();
		res.out.writeShort(res.state);
		res.isLanding = res.isLanding();
		res.isLandingPos = res.out.position();
		if (res.isLanding) {
			res.out.writeShort(1);
		} else {
			res.out.writeShort(0);
		}
		res.idAvoidingPos = res.out.position();
		res.out.writeLong(res.idAvoiding);
		res.out.writeFloat(res.plannedSpeed);
		res.out.writeFloat((float)res.speed);
		res.timePos = res.out.position();
		res.out.writeLong(System.nanoTime() - res.time);
		if (res.points == null) {
			res.out.writeShort(0);
		} else {
			res.out.writeShort(res.points.size());
			for (int i = 0; i < res.points.size(); i++) {
				res.out.writeDouble(res.points.get(i).x);
				res.out.writeDouble(res.points.get(i).y);
				res.out.writeDouble(res.points.get(i).z);
			}
		}
		res.out.flush();
		res.dataSize = res.out.position();

		return res;
	}

	/** Retrieves the buffer build by the method buildToSend(int).
	 * <p>This method must always be called after the mentioned method.</p> */
	public byte[] getBuffer() {
		return Arrays.copyOf(sendBuffer, dataSize);

	}

	/** Updates (time, state, landing, and avoidingId) and returns the buffer of the beacon. */
	public byte[] getBufferUpdated() {
		this.state = MBCAPParam.state[this.numUAV].getId();
		out.setPosition(statePos);
		out.writeShort(this.state);
		out.setPosition(this.isLandingPos);
		if (this.isLanding) {
			out.writeShort(1);
		} else {
			out.writeShort(0);
		}
		this.idAvoiding = MBCAPParam.idAvoiding.get(this.numUAV);
		out.setPosition(idAvoidingPos);
		out.writeLong(this.idAvoiding);
		out.setPosition(timePos);
		out.writeLong(System.nanoTime() - this.time);
		out.setPosition(dataSize);
		out.flush();
		return Arrays.copyOf(sendBuffer, dataSize);
	}
	
	/** Analyzes if the UAV is landing and the protocol must be disabled. */
	private boolean isLanding() {
		Copter copter = API.getCopter(numUAV);
		FlightMode mode = copter.getFlightMode();
		if (mode.getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING) {
			return true;
		}
		if (this.lastWP == null) {
			this.lastWP = copter.getMissionHelper().getLastWaypoint();
			this.lastWPUTM = copter.getMissionHelper().getLastWaypointUTM();
		}
		int currentWP = copter.getMissionHelper().getCurrentWaypoint();
		if (this.lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND) {
			if (currentWP >= this.lastWP.getNumSeq() - 1) {
				return true;
			}
		} else if (this.lastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND) {
			if (currentWP >= this.lastWP.getNumSeq() - 1 && copter.getLocationUTM().distance(lastWPUTM) < UAVParam.LAST_WP_THRESHOLD) {
				return true;
			}
		} else if (mode.getCustomMode() == 9){
			return true;
		}
		return false;
	}

	/** Creates a beacon from a received buffer.
	 * <p>It requires later analysis of which UAV has sent the beacon, based on the uavId parameter.
	 * Returns null if the buffer is null.</p> */
	public static Beacon getBeacon(byte[] buffer) {
		if (buffer == null) {
			return null;
		}
		Beacon res = new Beacon();
		res.in = new Input(buffer);
		// res.numUAV can not be identified at this point
		res.uavId = res.in.readLong();
		res.event = res.in.readShort();
		res.state = res.in.readShort();
		res.isLanding = res.in.readShort() == 1;
		res.idAvoiding = res.in.readLong();
		res.plannedSpeed = res.in.readFloat();
		res.speed = res.in.readFloat();
		res.time = System.nanoTime() - res.in.readLong();
		int size = res.in.readShort();
		res.points = new ArrayList<Location3DUTM>(size);
		for (int i = 0; i < size; i++) {
			res.points.add(new Location3DUTM(res.in.readDouble(), res.in.readDouble(), res.in.readDouble()));
		}
		return res;
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
		if (this.uavId == ((Beacon) obj).uavId) {
			return true;
		} else {
			return false;
		}
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
		String res = "[numUAV=" + this.numUAV + ",id=" + this.uavId + ",ev=" + this.event + ",state=" + this.state + ",landing=" + this.isLanding
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
