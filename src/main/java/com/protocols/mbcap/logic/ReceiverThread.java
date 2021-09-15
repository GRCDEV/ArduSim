package com.protocols.mbcap.logic;

import com.api.API;
import com.esotericsoftware.kryo.io.Input;
import es.upv.grc.mapper.Location3DUTM;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.protocols.mbcap.pojo.Beacon;

import java.util.ArrayList;
import java.util.Map;

/** This class receives data packets and stores them for later analysis of risk of collision.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ReceiverThread extends Thread {
	
	private int numUAV; // UAV identifier, beginning from 0
	private LowLevelCommLink link;
	byte[] inBuffer;
	Input input;
	private Copter copter;
	private ArduSim ardusim;
	private Map<Long, Beacon> beacons;
	
	@SuppressWarnings("unused")
	private ReceiverThread() {}
	
	public ReceiverThread(int numUAV) {
		this.beacons = MBCAPParam.beacons[numUAV];
		this.numUAV = numUAV;
		this.link = LowLevelCommLink.getCommLink(numUAV);
		this.inBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.copter = API.getCopter(numUAV);
		this.ardusim = API.getArduSim();
	}

	@Override
	public void run() {
		while (!ardusim.isExperimentInProgress() || !copter.isFlying()) {
			ardusim.sleep(MBCAPParam.SHORT_WAITING_TIME);
		}
		CollisionDetectorThread detector = new CollisionDetectorThread(numUAV);
		copter.getMissionHelper().setWaypointReachedListener(detector);
		detector.start();
		new BeaconingThread(numUAV).start();
		
		long expirationCheckTime = System.currentTimeMillis();
		long selfId = copter.getID();
		// If two UAVs collide, the protocol stops (when using the simulator)
		ArduSim ardusim = API.getArduSim();
		while (ardusim.isExperimentInProgress()
				&& !ardusim.collisionIsDetected()) {
			// Receive message
			Beacon beacon = this.getBeacon(link.receiveMessage());
			
			// Periodic cleanup of obsolete beacons to take into account UAVs that have gone far enough
			long now = System.currentTimeMillis();
			if (now > expirationCheckTime + MBCAPParam.BEACON_EXPIRATION_CHECK_PERIOD) {
				beacons.entrySet().removeIf(entry -> System.nanoTime() - entry.getValue().time >= MBCAPParam.beaconExpirationTime);
				expirationCheckTime = now;
			}
			
			// Ignoring beacons without useful information
			if (beacon != null && beacon.points.size() > 0) {
				// On real UAVs, the broadcast can also be received by the sender
				if (beacon.uavId != selfId) {
					beacons.put(beacon.uavId, beacon);
				}
			}
		}
	}
	
	/** Creates a beacon from a received buffer.
	 * <p>It requires later analysis of which UAV has sent the beacon, based on the uavId parameter.
	 * Returns null if the buffer is null.</p> */
	private Beacon getBeacon(byte[] buffer) {
		return ReceiverThread.getBeacon(buffer, input);
	}
	
	/** Creates a beacon from a received buffer.
	 * <p>It requires later analysis of which UAV has sent the beacon, based on the uavId parameter.
	 * Returns null if the buffer is null.</p> */
	public static Beacon getBeacon(byte[] buffer, Input input) {
		if (buffer == null) {
			return null;
		}
		input.setBuffer(buffer);
		Beacon res = new Beacon();
		res.uavId = input.readLong();
		res.event = input.readShort();
		res.state = input.readShort();
		res.isLanding = input.readShort() == 1;
		res.idAvoiding = input.readLong();
		res.plannedSpeed = input.readFloat();
		res.speed = input.readFloat();
		res.time = System.nanoTime() - input.readLong();
		int size = input.readShort();
		res.points = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			res.points.add(new Location3DUTM(input.readDouble(), input.readDouble(), input.readDouble()));
		}
		return res;
	}
}
