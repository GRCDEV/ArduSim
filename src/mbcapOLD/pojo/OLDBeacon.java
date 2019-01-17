package mbcapOLD.pojo;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.javatuples.Quintet;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.Tools;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import main.Param;
import mbcap.logic.MBCAPParam;
import mbcapOLD.gui.OLDMBCAPGUIParam;
import mbcapOLD.logic.OLDMBCAPHelper;
import mbcapOLD.logic.OLDMBCAPParam;
import uavController.UAVParam;

/** This class generates and updates the beacons sent by MBCAP protocol to detect risks of collision.
 * <p>It also allows to convert the object to MAVLink message and viceversa. */

public class OLDBeacon implements Comparable<OLDBeacon> {

	public boolean processed;
	private int numUAV;			// UAV position on arrays
	public long uavId;			// Sending UAV identifier
	public short event;			// Number of collision risk situations already solved
	public short state;			// Current protocol state
	private int statePos;		// Current protocol state position in the buffer so it could be updated
	public long idAvoiding;	// Identifier of the UAV with which this UAV is avoiding a collision, if any
	private int idAvoidingPos;// Avoiding UAV position in the buffer so it could be updated
	public double speed;		// Current speed
	public long time;			// (ns) On a sending beacon, it is the data capture local time,
								//      On a receiving beacon, it means the data capture time corrected to local time
	private int timePos;		// Time position in the buffer so it could be updated
	public List<Point3D> points;// Predicted positions

	private int dataSize;		// Buffer data size
	private byte[] sendBuffer;
	private Input in;
	private Output out;

	/** Constructor as auxiliary method used by buildToSend(int) method. */
	private OLDBeacon() {
	}

	/** Creates a beacon to send data. Don't use to receive data.
	 * <p>This method should be immediately followed by the getBuffer() method. */
	public static OLDBeacon buildToSend(int numUAV) {
		// 1. Getting the needed information
		long uavId = Param.id[numUAV];
		Quintet<Long, UTMCoordinates, Double, Double, Double> uavcurrentData = Copter.getData(numUAV);
		Long time = uavcurrentData.getValue0();
		Point2D.Double currentLocation = uavcurrentData.getValue1();
		double currentZ = uavcurrentData.getValue2();
		double speed = uavcurrentData.getValue3();
		double acceleration = uavcurrentData.getValue4();
		List<Point3D> points = OLDMBCAPHelper.getPredictedPath(numUAV, speed, acceleration, currentLocation, currentZ);
		OLDMBCAPGUIParam.predictedLocation.set(numUAV, points);

		// 2. Beacon building
		OLDBeacon res = new OLDBeacon();
		res.numUAV = numUAV;
		res.time = time;
		res.uavId = uavId;
		res.event = (short)OLDMBCAPParam.event.get(numUAV);
		res.state = MBCAPParam.state[numUAV].getId();
		res.idAvoiding = OLDMBCAPParam.idAvoiding.get(numUAV);
		res.speed = speed;
		res.points = points;

		// 3. Buffer building
		res.sendBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		res.out = new Output(res.sendBuffer);
		res.out.clear();
		res.out.writeLong(res.uavId);
		res.out.writeShort(res.event);
		res.statePos = res.out.position();
		res.out.writeShort(res.state);
		res.idAvoidingPos = res.out.position();
		res.out.writeLong(res.idAvoiding);
		res.out.writeFloat((float)res.speed);
		res.timePos = res.out.position();
		res.out.writeLong(System.nanoTime() - res.time);
		if (res.points == null) {
			res.out.writeShort(0);
		} else {
			res.out.writeShort(res.points.size());
			for (int i = 0; i < res.points.size(); i++) {
				res.out.writeFloat((float)res.points.get(i).x);
				res.out.writeFloat((float)res.points.get(i).y);
				res.out.writeFloat((float)res.points.get(i).z);
			}
		}
		res.out.flush();
		res.dataSize = res.out.position();

		return res;
	}

	/** Retrieves the buffer build by the method buildToSend(int).
	 * <p>This method must be called after the mentioned method. */
	public byte[] getBuffer() {
		return Arrays.copyOf(sendBuffer, dataSize);

	}

	/** Updates (time, state and avoidingId) and returns the buffer of the beacon. */
	public byte[] getBufferUpdated() {
		this.state = MBCAPParam.state[this.numUAV].getId();
		out.setPosition(statePos);
		out.writeShort(this.state);
		this.idAvoiding = OLDMBCAPParam.idAvoiding.get(this.numUAV);
		out.setPosition(idAvoidingPos);
		out.writeLong(this.idAvoiding);
		out.setPosition(timePos);
		out.writeLong(System.nanoTime() - this.time);
		out.setPosition(dataSize);
		out.flush();
		return Arrays.copyOf(sendBuffer, dataSize);
	}

	/** Creates a beacon from a received buffer.
	 * <p>It requires later analysis of which UAV has sent the beacon, based on the uavId parameter.
	 * <p>Returns null if the buffer is null. */
	public static OLDBeacon getBeacon(byte[] buffer) {
		if (buffer == null) {
			return null;
		}
		OLDBeacon res = new OLDBeacon();
		res.processed = false;
		res.in = new Input(buffer);
		// res.numUAV can not be identified at this point
		res.uavId = res.in.readLong();
		res.event = res.in.readShort();
		res.state = res.in.readShort();
		res.idAvoiding = res.in.readLong();
		res.speed = res.in.readFloat();
		res.time = System.nanoTime() - res.in.readLong();
		int size = res.in.readShort();
		res.points = new ArrayList<Point3D>(size);
		for (int i = 0; i < size; i++) {
			res.points.add(new Point3D(res.in.readFloat(), res.in.readFloat(), res.in.readFloat()));
		}
		return res;
	}

	/** Two beacons are equals if they came from the same UAV. */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof OLDBeacon)) {
			return false;
		}
		if (this.uavId == ((OLDBeacon) obj).uavId) {
			return true;
		} else {
			return false;
		}
	}

	/** Compares if two beacons came from the same UAV. */
	@Override
	public int compareTo(OLDBeacon o) {
		if (this.uavId < o.uavId)
			return -1;
		if (this.uavId == o.uavId)
			return 0;
		return 1;
	}

	@Override
	public String toString() {
		String res = "[numUAV=" + this.numUAV + ",id=" + this.uavId + ",ev=" + this.event + ",state=" + this.state
				+ ",idav=" + this.idAvoiding + ",speed=" + this.speed + ",time=" + this.time + ",size=" + this.points.size();
		if (this.points.size()>0) {
			Point3D p;
			for (int i=0; i<this.points.size(); i++) {
				p = this.points.get(i);
				res += ",(" + p.x + "," + p.y + "," + p.z + ")";
			}
		}
		res += "]";
		return res;
	}

	/** Creates an INCOMPLETE copy of a beacon, only useful to log the predicted points list on disk. It includes: id, time and points */
	@Override
	public OLDBeacon clone() {
		OLDBeacon res = new OLDBeacon();
		res.numUAV = this.numUAV;
		res.uavId = this.uavId;
		res.time = this.time;
		res.points = this.points;
		return res;
	}
}
