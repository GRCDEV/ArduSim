package mbcap.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;

import api.GUIHelper;
import api.MissionHelper;
import api.pojo.Point3D;
import main.Param;
import mbcap.pojo.Beacon;
import uavController.UAVParam;

/** This class receives data packets and stores them for later analysis of risk of collision.
 * <p>It also simulates broadcast and checks if there is a collision when using the simulator. */

public class BrokerThread extends Thread {

	private DatagramSocket socketListener;
	private DatagramPacket inPacket;
	private byte[] buffer;

	public BrokerThread() throws SocketException {
		if (Param.IS_REAL_UAV) {
			this.socketListener = new DatagramSocket(MBCAPParam.MBCAPport);
			this.socketListener.setBroadcast(true);
		} else {
			this.socketListener = new DatagramSocket(
					new InetSocketAddress(MBCAPParam.BROKER_IP, MBCAPParam.MBCAPport));
		}
		// this.socketListener.setSoTimeout(UAVParam.MESSAGE_WAITING_TIMEOUT);	Not needed. If another UAV is close enough to be
																				// a risk, it means that data packets are being received
		buffer = new byte[MBCAPParam.DATAGRAM_MAX_LENGTH];
		inPacket = new DatagramPacket(buffer, buffer.length);
	}

	@Override
	public void run() {
		long checkTime = System.nanoTime();
		// If two UAVs collide, the protocol stops (when using the simulator)
		while (!MBCAPParam.stopProtocol) {
			buffer = new byte[MBCAPParam.DATAGRAM_MAX_LENGTH];
			inPacket.setData(buffer, 0, buffer.length);
			try {
				socketListener.receive(inPacket);
				Beacon beacon = Beacon.getBeacon(inPacket.getData()); // beacon.numUAV is already INVALID

				long currentTime = System.nanoTime();
				if (!Param.IS_REAL_UAV) {
					// 1. Periodic cyclic collision check when using the simulator
					if (Param.numUAVs > 1 && currentTime - checkTime >= MBCAPParam.collisionCheckPeriod) {
						this.checkCollision();
						checkTime = checkTime + MBCAPParam.collisionCheckPeriod;
					}
					// If a collision happens, the protocol has to stop, so we finish the threads tasks
					if (MBCAPParam.stopProtocol) {
						break;
					}
				}

				// 2. Cleaning the obsolete beacons received on each UAV
				for (int i = 0; i < Param.numUAVs; i++) {
					Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[i].entrySet().iterator();
					while (entries.hasNext()) {
						Map.Entry<Long, Beacon> entry = entries.next();
						if (currentTime - entry.getValue().time > MBCAPParam.beaconExpirationTime) {
							entries.remove();
						}
					}
				}

				// 3. Sending the beacon to all UAVs
				//   Ignoring beacons without useful information
				if (beacon.points.size() > 0) {
					if (Param.IS_REAL_UAV) {
						// Send to all UAVs, as only this UAV is in the array
						for (int i=0; i<Param.numUAVs; i++) {
							MBCAPParam.beacons[i].put(beacon.uavId, beacon);
						}
					} else {
						// We need to decide which UAVs have to receive the message depending on the wireless model in use
						// All UAVs can receive the message but the sending one
						
						// First, we have to calculate where the sending UAV is in this moment
						Point3D beaconPredictedLocation;
						int beaconPredictedLocationPos = 0;		// Only one useful point case (we can not predict a more precise location)
						if (beacon.points.size() > 1) {	// General case
							// Locating the segment in the predicted path, where the UAV is in this moment
							while (beaconPredictedLocationPos < beacon.points.size()) {
								if (beacon.time+beaconPredictedLocationPos*MBCAPParam.hopTimeNS > currentTime) {
									break;
								}
								beaconPredictedLocationPos++;
							}
							beaconPredictedLocationPos--;	// As it has surpassed the real current UAV location
						}
						
						// Now sending to everybody but the sender
						beaconPredictedLocation = beacon.points.get(beaconPredictedLocationPos);
						for (int i = 0; i < Param.numUAVs; i++) {
							if (beacon.uavId != Param.id[i]) {
								// Taking into account the wireless model
								if (GUIHelper.isInRange(beaconPredictedLocation.distance(UAVParam.uavCurrentData[i].getUTMLocation()))) {
									MBCAPParam.beacons[i].put(beacon.uavId, beacon);
								}
							}
						}
					}
				}
			} catch (IOException e) {
			}
		}
	}

	private void checkCollision() {
		for (int i = 0; i < Param.numUAVs
				&& !MBCAPParam.stopProtocol; i++) {
			for (int j = i + 1; j < Param.numUAVs
					&& !MBCAPParam.stopProtocol; j++) {
				if (UAVParam.uavCurrentData[i].getUTMLocation()
						.distance(UAVParam.uavCurrentData[j].getUTMLocation()) < MBCAPParam.collisionDistance
						&& Math.abs(UAVParam.uavCurrentData[i].getZ()-UAVParam.uavCurrentData[j].getZ()) < MBCAPParam.COLLISION_ALTITUDE_DIFFERENCE) {
					MissionHelper.log(MBCAPText.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + ".");
					MissionHelper.setMissionGlobalInformation(MBCAPText.COLLISION_DETECTED);
					// The protocol is stopped
					MBCAPParam.stopProtocol = true;
					MBCAPHelper.landAllUAVs();
					// Advising the user
					GUIHelper.warn(MBCAPText.COLLISION_TITLE, MBCAPText.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
				}
			}
		}
	}
	}
