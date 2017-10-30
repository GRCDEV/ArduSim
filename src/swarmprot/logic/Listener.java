package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.esotericsoftware.kryo.io.Input;
import api.SwarmHelper;
import main.Param;
import swarmprot.logic.SwarmProtParam.SwarmProtState;

public class Listener extends Thread {

	private int numUAV;
	private DatagramSocket socketListener;
	private byte[] inBuffer;
	private DatagramPacket inPacket;
	Input input;
	
	// This ConcurrentHashMap contains the UAVs detected in START phase
	public static Map<Long, Long> UAVsDetected = new ConcurrentHashMap<Long, Long>();
	
	// ACK2 received from slaves UAVs
	HashSet<Long> uavsACK2 = new HashSet<Long>();
	
	// ACK3 received from slaves UAVs
	HashSet<Long> uavsACK3 = new HashSet<Long>();
	
	//Id of Previous and Next UAV on take off
	public int idPrev, idNext;
	
	//Altitude of take off algorithm first step
	public double takeOffAltitudeStepOne;

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
					new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.port + numUAV));

			// this.socketListener = new DatagramSocket(SwarmProtParam.port + numUAV);

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

						if (tipo == 1) {
							// Read id (MAC on real UAV) of UAVs and write it into HashSet if is not
							// duplicated
							System.out.println("Proceso paquete");
							Long idSlave = input.readLong();
							UAVsDetected.put(idSlave, idSlave);
						}
					} catch (IOException e) {
						System.err.println(SwarmProtText.MASTER_SOCKET_READ_ERROR);
						e.printStackTrace();
					}
				}
				// How many drones have been detected?
				SwarmHelper.log("Have been detected " + UAVsDetected.size() + " UAVs");

				// Change to next phase
				SwarmProtParam.state[numUAV] = SwarmProtState.SEND_DATA;

			} /** END PHASE START */

			/** PHASE SEND DATA */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				// bucle que itera esperando el ACK2, para ello los slaves deberan enviar
				// dicho ack y en este while cambiar el estado de la maquina de estados al
				// siguiente para el mestro

				/** Waiting ACK2 from all slaves */
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					// The socket is unlocked after a given time
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();

					if (tipo == 7) {
						long idSlave = input.readLong();
						uavsACK2.add(idSlave);
					}
					if (UAVsDetected.size() == uavsACK2.size()) {
						// Change to next phase
						SwarmProtParam.state[numUAV] = SwarmProtState.SEND_LIST;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND DATA */
			
			/** PHASE SEND LIST */
			
			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
			
				/** Waiting ACK3 from all slaves */
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();

					if (tipo == 8) {
						long idSlave = input.readLong();
						uavsACK3.add(idSlave);
					}
					if (UAVsDetected.size() == uavsACK3.size()) {
						SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND LIST */
			
			
			
			/** Slave actions */
		} else {
			/** PHASE_START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {

				inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
				inPacket.setData(inBuffer);
				try {
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 2 && myID == Param.id[numUAV]) {
						idPrev = input.readInt();
						idNext = input.readInt();
						takeOffAltitudeStepOne = input.readDouble();
						
						SwarmProtParam.state[numUAV] = SwarmProtState.WAIT_LIST;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} /** END PHASE START */

			/** PHASE WAIT LIST */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
				inPacket.setData(inBuffer);
				try {
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 3 && myID == Param.id[numUAV]) {
						// TODO proceso el msj3 y cambio a WAIT TAKE OFF que lo que hara es esperar el
						// MSJ 4 y enviar ACK2 sin parar mientras no reciba msj3 (en talker)
						SwarmProtParam.state[numUAV] = SwarmProtState.WAIT_TAKE_OFF;
					} else {

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} /** END PHASE WAIT LIST */
			
			/** PHASE WAIT TAKE OFF */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) { 
				inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
				inPacket.setData(inBuffer);
				try {
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 4 && myID == Param.id[numUAV]) {
						//TODO vendrá orden de despegue procesa
						SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
					} else {

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			/** END PHASE WAIT TAKE OFF */

		}

	}
}
