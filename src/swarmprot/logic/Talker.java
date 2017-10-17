package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import com.esotericsoftware.kryo.io.Output;
import api.GUIHelper;
import main.Param;
import swarmprot.logic.SwarmProtParam.SwarmProtState;

public class Talker extends Thread {

	private int numUAV;
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
					SwarmProtParam.portTalker);
		} else {
			packet = new DatagramPacket(buffer, buffer.length,
					new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.portTalker));
		}

	}

	// Cuando quiera enviar en simulacion tendre que recorrer con un for el
	// SwarmProtParam.port y hacer un +1 (SwarmProtParam.port+1) porque en
	// simulación no se puede repetir el puerto, despues en real esto ya no importa
	// pero lo que deberemos recorrer es la IP si por ejemplo el UAV0 es
	// 192.168.1.2 y tenemos 5 drones la Ip del quinto sera 192.168.1.7 TODO

	@Override
	public void run() {

		// Kryo serialization
		Output output;

		/**
		 * If you are master do: (If it is real, it compares if the mac of the numUAV is
		 * equal to the mac of the master that is hardcoded. If it's simulated compares
		 * if uav = 0 which is master UAV )
		 */

		/** Master actions */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			} /** END PHASE START */

			/** Slave actions */
		} else {

			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				output = new Output(buffer);
				output.clear();
				// MSJType1
				output.writeShort(1);
				output.writeLong(Param.id[numUAV]);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// If only information is sent to the master, it is used sendDataToMaster else
					// sendDataToSlaves
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} /** END PHASE START */

		}

	}
}
