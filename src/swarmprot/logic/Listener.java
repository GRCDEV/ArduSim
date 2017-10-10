package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;

import com.esotericsoftware.kryo.io.Input;

import main.Param;

public class Listener extends Thread {

	private int numUAV; // UAV identifier, beginning from 0
	private DatagramSocket socketListener;
	private byte[] inBuffer;
	private DatagramPacket inPacket;
	Input input; // Kryo paquet

	public Listener(int numUAV) throws SocketException {
		this.numUAV = numUAV;
		try {
			socketListener = new DatagramSocket();
			socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/** Real UAV */
		if (Param.IS_REAL_UAV) {
			this.socketListener = new DatagramSocket(SwarmProtParam.port);
			this.socketListener.setBroadcast(true);

			/** Simulated UAV */
		} else {
			this.socketListener = new DatagramSocket(
					new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.port));

		}

		inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
		inPacket = new DatagramPacket(inBuffer, inBuffer.length);

	}

	@Override
	public void run() {
		/**
		 * If you are master do: (If it is real, it compares if the mac of the numUAV is
		 * equal to the mac of the master that is hardcoded. If it's simulated compares
		 * if uav = 0 which is master UAV )
		 */
		// Starting time
		long startTime = System.currentTimeMillis();
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {
			// This HashSet contains the UAVs detected in START phase
			HashSet<Long> UAVsDetected = new HashSet<Long>();

			// Master whait slave UAVs for 20 seconds
			while ((System.currentTimeMillis() - startTime) < 20000) {
				try {
					socketListener.receive(inPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				input = new Input(inBuffer);
				input.setPosition(0);
				short tipo = input.readShort();
				switch (tipo) {
				case 1:
					// Read id of UAVs and write it into HashSet if is not duplicated
					UAVsDetected.add(input.readLong());
				}
			}

			// PARA VER CUANTOS DETECTA
			System.out.println("HAN SIDO DETECTADOS " + UAVsDetected.size() + " UAVs");

		} else {

		}

	}

}
