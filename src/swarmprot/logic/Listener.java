package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import com.esotericsoftware.kryo.io.Input;
import api.GUIHelper;
import api.SwarmHelper;
import main.Param;
import swarmprot.logic.SwarmProtParam.SwarmProtState;

public class Listener extends Thread {

	private int numUAV;
	private DatagramSocket socketListener;
	private byte[] inBuffer;
	private DatagramPacket inPacket;
	Input input;

	public Listener(int numUAV) throws SocketException {
		this.numUAV = numUAV;
		try {
			socketListener = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println(SwarmProtText.MASTER_SOCKET_ERROR);
			e.printStackTrace();
		}
		/** Real UAV */
		if (Param.IS_REAL_UAV) {
			this.socketListener = new DatagramSocket(SwarmProtParam.port);
			this.socketListener.setBroadcast(true);

		/** Simulated UAV */
		} else {
			 this.socketListener = new DatagramSocket(
			 // Add the UAV number to calculate the available port
			 new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.port
			 + numUAV));

			//this.socketListener = new DatagramSocket(SwarmProtParam.port + numUAV);

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

		/** Master actions */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {

				// This HashSet contains the UAVs detected in START phase
				HashSet<Long> UAVsDetected = new HashSet<Long>();

				// Master wait slave UAVs for 20 seconds
				while ((System.currentTimeMillis() - startTime) < 20000) {
					try {
						inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						// The socket is unlocked after a given time
						socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();

						switch (tipo) {
						case 1:
							// Read id (MAC on real UAV) of UAVs and write it into HashSet if is not
							// duplicated
							System.out.println("Proceso paquete");

							UAVsDetected.add(input.readLong());
						}
					} catch (IOException e) {
						System.err.println(SwarmProtText.MASTER_SOCKET_READ_ERROR);
						e.printStackTrace();
					}
				}
				SwarmProtParam.state[numUAV] = SwarmProtState.SEND_DATA;

				// How many drones have been detected?
				SwarmHelper.log("Have been detected " + UAVsDetected.size() + " UAVs");
			}/** END PHASE START */

			// 2 fase protocolo master TODO

			/** Slave actions */
		} else {
			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			}/** END PHASE START */

			// 2 fase protocolo slave TODO

		}

	}
}
