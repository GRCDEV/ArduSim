package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import com.esotericsoftware.kryo.io.Output;
import api.GUIHelper;
import api.SwarmHelper;
import main.Param;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class Talker extends Thread {

	private int numUAV;
	private DatagramSocket socket;
	private byte[] buffer;
	private DatagramPacket packet;
	Double missionAltitude;
	Double takeOffAltitudeStapeOne;
	
	//Timer
	private long cicleTime;

	public Talker(int numUAV) throws SocketException, UnknownHostException {
		this.numUAV = numUAV;
		this.cicleTime = 0;
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

		/**
		 * Calculate the take-off altitude, which uses the take-off algorithm (it is not
		 * the same as the altitude of the mission)
		 */
		missionAltitude = UAVParam.currentGeoMission[SwarmProtParam.posMaster].get(1).getAltitude();

		if (missionAltitude <= 5.0) {
			takeOffAltitudeStapeOne = 2.0;
		}

		if (missionAltitude > 5.0 && missionAltitude < 10.0) {
			takeOffAltitudeStapeOne = missionAltitude / 2;
		}

		if (missionAltitude >= 10.0) {
			takeOffAltitudeStapeOne = 5.0;

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
		 * If you are master do: (If it is real, it compares if the MAC of the numUAV is
		 * equal to the mac of the master that is hardcoded. If it's simulated compares
		 * if uav = 0 which is master UAV )
		 */
		
		//Timer
		int waitingTime;
		if (cicleTime == 0) {
			cicleTime = System.currentTimeMillis();
		}

		/** Master actions */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			} /** END PHASE START */

			/** PHASE SEND DATA */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				Long[] idSlaves = Listener.UAVsDetected.values().toArray(new Long[Listener.UAVsDetected.size()]);
				Arrays.sort(idSlaves);

				while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
					/** MSJ2 */
					output = new Output(buffer);
					for (int i = 0; i < Param.numUAVs; i++) {
						output.clear();
						output.writeShort(2);
						output.writeLong(Param.id[numUAV]);
						// If you are the first drone of the array
						if (i == 0) {
							// idPrev
							output.writeInt(Integer.MAX_VALUE);
							// idNext
							output.writeInt(i + 1);
						}
						// If you are the last drone of the array
						if (i == Param.numUAVs - 1) {
							// idPrev
							output.writeInt(i - 1);
							// idNext
							output.writeInt(Integer.MAX_VALUE);
						}
						if (i != 0 && i != Param.numUAVs - 1) {
							// idPrev
							output.writeInt(i - 1);
							// idNext
							output.writeInt(i + 1);
						}
						output.writeDouble(takeOffAltitudeStapeOne);
						output.flush();

						packet.setData(buffer, 0, output.position());

						try {
							// Information send only to Slaves
							SwarmProtHelper.sendDataToSlaves(packet, socket);
						} catch (IOException e) {
							SwarmHelper.log("Problem in Master START stage");
							e.printStackTrace();
						}
						
						//Timer
						cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
						waitingTime = (int)(cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							GUIHelper.waiting(waitingTime);
						}
					}
				}
			} /** END PHASE SEND DATA */
			
			
			/** PHASE SEND LIST TODO*/
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				
			
				
				
				//Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			}/** END PHASE SEND LIST */
			

			/** Slave actions */
		} else {

			/** PHASE START */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				/** MSJ1 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(1);
				output.writeLong(Param.id[numUAV]);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// If only information is sent to the master, it is used sendDataToMaster else
					// sendDataToSlaves
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Slave START stage");
					e.printStackTrace();
				}
				
				//Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} /** END PHASE START */

			/** PHASE WAIT LIST */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				
				// Send ACK2 continuously while this is in WAIT_LIST state, listener will change
				// status when receiving MSJ3
				
				/** ACK2 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(7);
				output.writeLong(Param.id[numUAV]);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Slave WAIT_LIST stage");
					e.printStackTrace();
				}
				
				//Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} /** ENF PHASE WAIT LIST */
			
			/** PHASE WAIT TAKE OFF */
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				/** ACK3 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(8);
				output.writeLong(Param.id[numUAV]);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Slave WAIT_TAKE_OFF stage");
					e.printStackTrace();
				}
				
				//Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int)(cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			}/** ENF PHASE WAIT TAKE OFF */
		}
	}
}
