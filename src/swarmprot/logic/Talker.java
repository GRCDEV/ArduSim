package swarmprot.logic;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import main.Param;

public class Talker extends Thread {

	private int numUAV; // UAV identifier, beginning from 0
	private DatagramSocket socket;
	private byte[] buffer;
	private DatagramPacket packet;

	public Talker(int numUAV) throws SocketException, UnknownHostException {
		this.numUAV = numUAV;
		socket = new DatagramSocket();
		buffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
		if (Param.IS_REAL_UAV) {
			socket.setBroadcast(true);
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SwarmProtParam.BROADCAST_IP_REAL),
					SwarmProtParam.port);
		} else {
			packet = new DatagramPacket(buffer, buffer.length,
					new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.port));
		}
	}

	@Override
	public void run() {
		/**
		 * If you are master do: (If it is real, it compares if the mac of the numUAV is
		 * equal to the mac of the master that is hardcoded. If it's simulated compares
		 * if uav = 0 which is master UAV )
		 */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

		} else {

		}

	}
}
