package mbcap.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import api.GUIHelper;
import api.MissionHelper;
import main.Param;
import main.Param.SimulatorState;
import mbcap.gui.MBCAPGUIParam;
import mbcap.pojo.Beacon;
import sim.logic.SimParam;
import uavController.UAVParam;
import uavController.WaypointReachedListener;

/** This class sends data packets to other UAVs, by real or simulated broadcast, so others can detect risk of collision. */

public class BeaconingThread extends Thread implements WaypointReachedListener {

	private int numUAV; // UAV identifier, beginning from 0
	private DatagramSocket socket;
	private byte[] buffer;
	private DatagramPacket packet;
	private long cicleTime;

	@SuppressWarnings("unused")
	private BeaconingThread() {
	}

	public BeaconingThread(int numUAV) throws SocketException, UnknownHostException {
		this.numUAV = numUAV;
		socket = new DatagramSocket();
		buffer = new byte[MBCAPParam.DATAGRAM_MAX_LENGTH];
		if (Param.IS_REAL_UAV) {
			socket.setBroadcast(true);
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MBCAPParam.BROADCAST_IP), MBCAPParam.MBCAPport);
		} else {
			packet = new DatagramPacket(buffer, buffer.length,
					new InetSocketAddress(MBCAPParam.BROKER_IP, MBCAPParam.MBCAPport));
		}
		
		this.cicleTime = 0;
	}
	
	@Override
	public void onWaypointReached() {
		// Project the predicted path over the planned mission
		MBCAPParam.projectPath.set(numUAV, 1);
		// Use the UAV acceleration for calculating the future locations
		MBCAPParam.useAcceleration.set(numUAV, 1);
	}

	@Override
	public void run() {
		Beacon selfBeacon = null;
		byte[] sendBuffer = null;
		if (cicleTime == 0) {
			cicleTime = System.currentTimeMillis();
		}
		int waitingTime;
		boolean experimentStarted = false;

		// The protocol is stopped when two UAVs collide
		while (!MBCAPParam.stopProtocol) {
			SimulatorState state = Param.simStatus;
			// Send beacons while the UAV is flying during the experiment
			if (state == SimulatorState.TEST_IN_PROGRESS
					&& UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
				experimentStarted = true;
				
				// Each beacon is sent a number of times before renewing the predicted positions
				for (int i = 0; i < MBCAPParam.numBeacons; i++) {
					// The first time it is needed to calculate the predicted positions
					if (i == 0) {
						selfBeacon = Beacon.buildToSend(numUAV);
						MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
						sendBuffer = selfBeacon.getBuffer();

						// Beacon store for logging purposes
						if (Param.VERBOSE_STORE
								&& state == SimulatorState.TEST_IN_PROGRESS
								&& Param.testEndTime[numUAV] == 0) {
							MBCAPParam.beaconsStored[numUAV].add(selfBeacon.clone());
						}
					} else {
						// In any other case, only time, state and idAvoiding are updated
						sendBuffer = selfBeacon.getBufferUpdated();
					}
					if (!selfBeacon.points.isEmpty()) {
						packet.setData(sendBuffer);
						try {
							socket.send(packet);
						} catch (IOException e) {
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.ERROR_BEACON);
						}
					}

					cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						GUIHelper.waiting(waitingTime);
					}
				}
			} else if ((state == SimulatorState.TEST_IN_PROGRESS && !experimentStarted) // Beginning test but without flying jet
					|| state == SimulatorState.CONFIGURING
					|| state == SimulatorState.CONFIGURING_PROTOCOL
					|| state == SimulatorState.STARTING_UAVS
					|| state == SimulatorState.UAVS_CONFIGURED
					|| state == SimulatorState.SETUP_IN_PROGRESS
					|| state == SimulatorState.READY_FOR_TEST) {
				// Passive waiting until the experiment starts
				cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} else if ( state == SimulatorState.TEST_IN_PROGRESS // Test finishing but already landed
					||state == SimulatorState.TEST_FINISHED
					|| state == SimulatorState.SHUTTING_DOWN) {
				// Stop sending and drawing future positions when the UAV lands
				MBCAPGUIParam.predictedLocation.set(numUAV, null);
				return;
			}
		}
	}

}
