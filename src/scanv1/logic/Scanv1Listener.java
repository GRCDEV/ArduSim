package scanv1.logic;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import main.Param;
import scanv1.logic.Scanv1ProtParam.SwarmProtState;

public class Scanv1Listener extends Thread {

	private int numUAV;
	private DatagramSocket socketListener;
	private byte[] inBuffer;
	private DatagramPacket inPacket;
	Input input;

	// This ConcurrentHashMap contains the UAVs detected in START phase
	public static Map<Long, Scanv1uavPosition> UAVsDetected = new ConcurrentHashMap<Long, Scanv1uavPosition>();

	// ACK2 received from slaves UAVs
	public static Map<Long, Long> uavsACK2 = new ConcurrentHashMap<Long, Long>();

	// ACK3 received from slaves UAVs
	public static Map<Long, Long> uavsACK3 = new ConcurrentHashMap<Long, Long>();

	// ACK4 received from slaves UAVs
	public static Map<Long, Long> uavsACK4 = new ConcurrentHashMap<Long, Long>();
	
	// ACK5 received from slaves UAVs
	public static Map<Long, Long> uavsACK5 = new ConcurrentHashMap<Long, Long>();
	

	// Altitude of take off algorithm first step "calculated"
	public static double takeOffAltitudeStepOne;

	// Semaphore for threads
	public static volatile boolean semaphore = false;

	// Array with position and ID of all UAVs
	public static Scanv1uavPosition[] positionsAndIDs;

	// Id order take off
	public long idPrev;
	public long idNext;
	public static long idPrevMaster;
	public static long idNextMaster;

	// Identifier that indicates the last WP
	public static boolean WPLast = false;

	public Scanv1Listener(int numUAV) throws SocketException {
		this.numUAV = numUAV;
		try {
			socketListener = new DatagramSocket();
		} catch (SocketException e) {
			GUI.log(Scanv1ProtText.MASTER_SOCKET_ERROR);
			e.printStackTrace();
		}
		/** Real UAV */
		if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
			this.socketListener = new DatagramSocket(Scanv1ProtParam.port);
			this.socketListener.setBroadcast(true);

		/** Simulated UAV */
		} else {
			// Add the UAV number to calculate the available port
			this.socketListener = new DatagramSocket(new InetSocketAddress(Scanv1ProtParam.BROADCAST_IP_LOCAL, Scanv1ProtParam.port + numUAV));
		}

		inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
		inPacket = new DatagramPacket(inBuffer, inBuffer.length);

	}

	@Override
	public void run() {
		/**
		 * If you are master do: (If it is real, it compares if the MAC of the numUAV is
		 * equal to the MAC of the master that is hardcoded. If it's simulated compares
		 * if UAV = 0 (which is the master's identifier in simulation))
		 */
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(Scanv1ProtParam.waitState);
		}
		
		// Starting time
		long startTime = System.currentTimeMillis();
		
		long selfId = Tools.getIdFromPos(numUAV);
		
		/** Master actions ----------------------------------------------------------------------------------- */
		if (selfId == Scanv1ProtParam.idMaster) {
			/** PHASE START */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_START_LISTENER);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				
				while(Tools.areUAVsReadyForSetup()) {
					try {
						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						// The socket is unlocked after a given time
						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();

						if (tipo == 1) {
							// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
							Long idSlave = input.readLong();
							Scanv1uavPosition posiciones = new Scanv1uavPosition(input.readDouble(), input.readDouble(), idSlave,
									input.readDouble());
							// ADD to Map the new UAV detected
							UAVsDetected.put(idSlave, posiciones);
						}
					} catch (IOException e) {
						GUI.log(Scanv1ProtText.MASTER_SOCKET_READ_ERROR);
						e.printStackTrace();
					}
				}
				
				
				// Master wait slave UAVs for 20 seconds
//				while ((System.currentTimeMillis() - startTime) < Scanv1ProtParam.waitTimeForSlaves) {
//					try {
//						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//						inPacket.setData(inBuffer);
//						// The socket is unlocked after a given time
//						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//						socketListener.receive(inPacket);
//						input = new Input(inPacket.getData());
//						input.setPosition(0);
//						short tipo = input.readShort();
//
//						if (tipo == 1) {
//							// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
//							Long idSlave = input.readLong();
//							Scanv1uavPosition posiciones = new Scanv1uavPosition(input.readDouble(), input.readDouble(), idSlave,
//									input.readDouble());
//							// ADD to Map the new UAV detected
//							UAVsDetected.put(idSlave, posiciones);
//						}
//					} catch (IOException e) {
//						System.err.println(Scanv1ProtText.MASTER_SOCKET_READ_ERROR);
//						e.printStackTrace();
//					}
//				}

				// Setup step starts
				
				// Convert HashMap to Scanv1uavPosition with IDs and initial position
				Iterator<Map.Entry<Long, Scanv1uavPosition>> entries;
				Map.Entry<Long, Scanv1uavPosition> entry;
				entries = UAVsDetected.entrySet().iterator();
				int i = 0;
				// +1 to include master ID
				positionsAndIDs = new Scanv1uavPosition[UAVsDetected.size() + 1];

				while (entries.hasNext()) {
					entry = entries.next();
					positionsAndIDs[i] = entry.getValue();
					i++;
				}
				// Set Master ID and initial position to the last position
				Point2D.Double UTMMaster = Copter.getUTMLocation(numUAV);
				Double headingMaster = Copter.getHeading(numUAV);
				Scanv1uavPosition masterPosition = new Scanv1uavPosition();
				masterPosition.id = Scanv1ProtParam.idMaster;
				masterPosition.x = UTMMaster.x;
				masterPosition.y = UTMMaster.y;
				masterPosition.heading = headingMaster;
				positionsAndIDs[positionsAndIDs.length - 1] = masterPosition;

				//For flow control
				semaphore = true;

				// How many drones have been detected?
				GUI.log(Scanv1ProtText.DETECTED + UAVsDetected.size() + " UAVs");

				// Change to next phase
				Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_DATA;

			} /** END PHASE START */

			/** PHASE SEND DATA */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_DATA_LISTENER);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
			
				/** Waiting ACK2 from all slaves */
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();

					if (tipo == 7) {
						long idSlave = input.readLong();
						uavsACK2.put(idSlave, idSlave);
					}

					if (UAVsDetected.size() == uavsACK2.size()) {
						// Change to next phase
						Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_LIST;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND DATA */

			/** PHASE SEND LIST */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_LIST_LISTENER);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
				
				/** Waiting ACK3 from all slaves */
				try {

					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();

					if (tipo == 8) {
						long idSlave = input.readLong();
						uavsACK3.put(idSlave, idSlave);
					}
					if (UAVsDetected.size() == uavsACK3.size()) {
						if (idPrevMaster == Scanv1ProtParam.broadcastMAC) {
							Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
						} else {
							Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_TAKE_OFF;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND LIST */
			
			//Sync time
			Tools.waiting(3000);
			
			Scanv1ProtParam.setupFinished.set(numUAV, 1);
			
			/** Slave actions---------------------------------------------------------------*/
		} else {
			/** PHASE_START */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_START_LISTENER);
			}
			
			while(Tools.areUAVsReadyForSetup()) {
				Tools.waiting(Scanv1ProtParam.waitState);
			}
			
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				
				/** Waiting for MSJ2 */
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 2 && myID == selfId) {
						takeOffAltitudeStepOne = input.readDouble();
						Scanv1ProtParam.state[numUAV] = SwarmProtState.WAIT_LIST;
					}

				} catch (IOException e) {
					// e.printStackTrace();
				}
			} /** END PHASE START */
			
			/** PHASE WAIT LIST */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_LIST_LISTENER);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();

					if (tipo == 3 && myID == selfId) {

						idPrev = input.readLong();
						idNext = input.readLong();
						// The matrix is ​​filled with the idPrev and idNext (for take off step) of each UAV
						Scanv1ProtParam.fightPrevNext[numUAV][0] = idPrev;
						Scanv1ProtParam.fightPrevNext[numUAV][1] = idNext;

						// The number of WP is read to know the loop length
						int numberOfWp = input.readInt();
						Scanv1ProtParam.flightListPersonalized[numUAV] = new Point3D[numberOfWp];
						Scanv1ProtParam.flightListPersonalizedAlt[numUAV] = new Double[numberOfWp];

						for (int i = 0; i < numberOfWp; i++) {
							Scanv1ProtParam.flightListPersonalized[numUAV][i] = new Point3D(input.readDouble(),
									input.readDouble(), input.readDouble());

						}

						// Initialize the geometric coordinates array and altitude array
						Scanv1ProtParam.flightListPersonalizedGeo[numUAV] = new GeoCoordinates[numberOfWp];
						Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV] = new Double[numberOfWp];

						for (int s = 0; s < Scanv1ProtParam.flightListPersonalized[numUAV].length; s++) {
							Scanv1ProtParam.flightListPersonalizedGeo[numUAV][s] = Tools.UTMToGeo(Scanv1ProtParam.flightListPersonalized[numUAV][s].x, Scanv1ProtParam.flightListPersonalized[numUAV][s].y);
							Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV][s] = Scanv1ProtParam.flightListPersonalized[numUAV][s].z;
						}

						Scanv1ProtParam.state[numUAV] = SwarmProtState.WAIT_TAKE_OFF;
					} else {

					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
			} /** END PHASE WAIT LIST */

			/** PHASE WAIT TAKE OFF */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_TAKE_OFF_LISTENER);
			}
			
			Scanv1ProtParam.setupFinished.set(numUAV, 1);
		}
		
		while(Tools.isSetupInProgress() || Tools.isSetupFinished()) {
			Tools.waiting(Scanv1ProtParam.waitState);
		}
		
		/** Master actions ----------------------------------------------------------------------------------- */
		if (selfId == Scanv1ProtParam.idMaster) {
			/** PHASE SEND TAKE OFF */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.SEND_TAKE_OFF + "--> Listening");
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {

				/** Waiting for TAKE_OFF MSJ */
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 4 && myID == selfId) {
						Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
			/** END PHASE SEND TAKE OFF */
			
			/** Slave actions---------------------------------------------------------------*/
		} else {
			// If I'm the first one, I take off directly
			if (idPrev == Scanv1ProtParam.broadcastMAC && Scanv1Talker.semaphoreSlaveACK3) {
				Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
			}else {
				while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
					try {
						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();
						long myID = input.readLong();

						// If MSJ4 is received, someone has given the order for you to take off
						if (tipo == 4 && myID == selfId) {
							Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;

						}

					} catch (IOException e) {
						// e.printStackTrace();
					}
				}
			}

			/** END PHASE WAIT TAKE OFF */
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
// TODO comprobar los cambios puestos por Francisco
//		/** Master actions ----------------------------------------------------------------------------------- */
//		if (Param.id[numUAV] == Scanv1ProtParam.idMaster) {
//
//			/** PHASE START */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
//				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_START_LISTENER);
//			}
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
//
//				while(Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED) {
//					try {
//						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//						inPacket.setData(inBuffer);
//						// The socket is unlocked after a given time
//						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//						socketListener.receive(inPacket);
//						input = new Input(inPacket.getData());
//						input.setPosition(0);
//						short tipo = input.readShort();
//
//						if (tipo == 1) {
//							// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
//							Long idSlave = input.readLong();
//							uavPosition posiciones = new uavPosition(input.readDouble(), input.readDouble(), idSlave,
//									input.readDouble());
//							// ADD to Map the new UAV detected
//							UAVsDetected.put(idSlave, posiciones);
//						}
//					} catch (IOException e) {
//						System.err.println(Scanv1ProtText.MASTER_SOCKET_READ_ERROR);
//						e.printStackTrace();
//					}
//				}
//
//
//				// Master wait slave UAVs for 20 seconds
//				//				while ((System.currentTimeMillis() - startTime) < Scanv1ProtParam.waitTimeForSlaves) {
//				//					try {
//				//						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//				//						inPacket.setData(inBuffer);
//				//						// The socket is unlocked after a given time
//				//						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//				//						socketListener.receive(inPacket);
//				//						input = new Input(inPacket.getData());
//				//						input.setPosition(0);
//				//						short tipo = input.readShort();
//				//
//				//						if (tipo == 1) {
//				//							// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
//				//							Long idSlave = input.readLong();
//				//							uavPosition posiciones = new uavPosition(input.readDouble(), input.readDouble(), idSlave,
//				//									input.readDouble());
//				//							// ADD to Map the new UAV detected
//				//							UAVsDetected.put(idSlave, posiciones);
//				//						}
//				//					} catch (IOException e) {
//				//						System.err.println(Scanv1ProtText.MASTER_SOCKET_READ_ERROR);
//				//						e.printStackTrace();
//				//					}
//				//				}
//
//				// Convert HashMap to uavPosition with IDs and initial position
//				Iterator<Map.Entry<Long, uavPosition>> entries;
//				Map.Entry<Long, uavPosition> entry;
//				entries = UAVsDetected.entrySet().iterator();
//				int i = 0;
//				// +1 to include master ID
//				positionsAndIDs = new uavPosition[UAVsDetected.size() + 1];
//
//				while (entries.hasNext()) {
//					entry = entries.next();
//					positionsAndIDs[i] = entry.getValue();
//					i++;
//				}
//				// Set Master ID and initial position to the last position
//				Point2D.Double UTMMaster = UAVParam.uavCurrentData[numUAV].getUTMLocation();
//				Double headingMaster = UAVParam.uavCurrentData[numUAV].getHeading();
//				uavPosition masterPosition = new uavPosition();
//				masterPosition.id = Scanv1ProtParam.idMaster;
//				masterPosition.x = UTMMaster.x;
//				masterPosition.y = UTMMaster.y;
//				masterPosition.heading = headingMaster;
//				positionsAndIDs[positionsAndIDs.length - 1] = masterPosition;
//
//				//For flow control
//				semaphore = true;
//
//				// How many drones have been detected?
//				GUI.log(Scanv1ProtText.DETECTED + UAVsDetected.size() + " UAVs");
//
//				// Change to next phase
//				Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_DATA;
//
//			} /** END PHASE START */
//
//			/** PHASE SEND DATA */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
//				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_DATA_LISTENER);
//			}
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
//
//				/** Waiting ACK2 from all slaves */
//				try {
//					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//					inPacket.setData(inBuffer);
//					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//					socketListener.receive(inPacket);
//					input = new Input(inPacket.getData());
//					input.setPosition(0);
//					short tipo = input.readShort();
//
//					if (tipo == 7) {
//						long idSlave = input.readLong();
//						uavsACK2.put(idSlave, idSlave);
//					}
//
//					if (UAVsDetected.size() == uavsACK2.size()) {
//						// Change to next phase
//						Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_LIST;
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//			} /** END PHASE SEND DATA */
//
//			/** PHASE SEND LIST */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
//				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_LIST_LISTENER);
//			}
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
//
//				/** Waiting ACK3 from all slaves */
//				try {
//
//					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//					inPacket.setData(inBuffer);
//					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//					socketListener.receive(inPacket);
//					input = new Input(inPacket.getData());
//					input.setPosition(0);
//					short tipo = input.readShort();
//
//					if (tipo == 8) {
//						long idSlave = input.readLong();
//						uavsACK3.put(idSlave, idSlave);
//					}
//					if (UAVsDetected.size() == uavsACK3.size()) {
//						if (idPrevMaster == Scanv1ProtParam.broadcastMAC) {
//							Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
//						} else {
//							Scanv1ProtParam.state[numUAV] = SwarmProtState.SEND_TAKE_OFF;
//						}
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//			} /** END PHASE SEND LIST */
//
//			//Sync time
//			Tools.waiting(3000);
//
//			Param.simStatus = SimulatorState.READY_FOR_TEST;//TODO modificar la espera por mis cambios
//			while (Param.simStatus == SimulatorState.READY_FOR_TEST) {
//				Tools.waiting(Scanv1ProtParam.waitState);
//			}
//
//			/** PHASE SEND TAKE OFF */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
//				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.SEND_TAKE_OFF + "--> Listening");
//			}
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
//
//				/** Waiting for TAKE_OFF MSJ */
//				try {
//					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//					inPacket.setData(inBuffer);
//					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//					socketListener.receive(inPacket);
//					input = new Input(inPacket.getData());
//					input.setPosition(0);
//					short tipo = input.readShort();
//					long myID = input.readLong();
//					if (tipo == 4 && myID == Param.id[numUAV]) {
//						Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
//					}
//				} catch (IOException e) {
//					// e.printStackTrace();
//				}
//			}
//			/** END PHASE SEND TAKE OFF */
//
//			/** Slave actions---------------------------------------------------------------*/
//		} else {
//			/** PHASE_START */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
//				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_START_LISTENER);
//			}
//			
//			while(Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED) {
//				Tools.waiting(Scanv1ProtParam.waitState);
//			}
//			
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
//				
//				/** Waiting for MSJ2 */
//				try {
//					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//					inPacket.setData(inBuffer);
//					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//					socketListener.receive(inPacket);
//					input = new Input(inPacket.getData());
//					input.setPosition(0);
//					short tipo = input.readShort();
//					long myID = input.readLong();
//					if (tipo == 2 && myID == Param.id[numUAV]) {
//						takeOffAltitudeStepOne = input.readDouble();
//						Scanv1ProtParam.state[numUAV] = SwarmProtState.WAIT_LIST;
//					}
//
//				} catch (IOException e) {
//					// e.printStackTrace();
//				}
//			} /** END PHASE START */
//			
//			/** PHASE WAIT LIST */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
//				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_LIST_LISTENER);
//			}
//			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
//				
//				try {
//					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//					inPacket.setData(inBuffer);
//					socketListener.receive(inPacket);
//					input = new Input(inPacket.getData());
//					input.setPosition(0);
//					short tipo = input.readShort();
//					long myID = input.readLong();
//
//					if (tipo == 3 && myID == Param.id[numUAV]) {
//
//						idPrev = input.readLong();
//						idNext = input.readLong();
//						// The matrix is ​​filled with the idPrev and idNext (for take off step) of each UAV
//						Scanv1ProtParam.fightPrevNext[numUAV][0] = idPrev;
//						Scanv1ProtParam.fightPrevNext[numUAV][1] = idNext;
//
//						// The number of WP is read to know the loop length
//						int numberOfWp = input.readInt();
//						Scanv1ProtParam.flightListPersonalized[numUAV] = new Point3D[numberOfWp];
//						Scanv1ProtParam.flightListPersonalizedAlt[numUAV] = new Double[numberOfWp];
//
//						for (int i = 0; i < numberOfWp; i++) {
//							Scanv1ProtParam.flightListPersonalized[numUAV][i] = new Point3D(input.readDouble(),
//									input.readDouble(), input.readDouble());
//
//						}
//
//						// Initialize the geometric coordinates array and altitude array
//						Scanv1ProtParam.flightListPersonalizedGeo[numUAV] = new GeoCoordinates[numberOfWp];
//						Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV] = new Double[numberOfWp];
//
//						for (int s = 0; s < Scanv1ProtParam.flightListPersonalized[numUAV].length; s++) {
//							Scanv1ProtParam.flightListPersonalizedGeo[numUAV][s] = Tools.UTMToGeo(Scanv1ProtParam.flightListPersonalized[numUAV][s].x, Scanv1ProtParam.flightListPersonalized[numUAV][s].y);
//							Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV][s] = Scanv1ProtParam.flightListPersonalized[numUAV][s].z;
//						}
//
//						Scanv1ProtParam.state[numUAV] = SwarmProtState.WAIT_TAKE_OFF;
//					} else {
//
//					}
//				} catch (IOException e) {
//					//e.printStackTrace();
//				}
//			} /** END PHASE WAIT LIST */
//
//			/** PHASE WAIT TAKE OFF */
//			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
//				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_TAKE_OFF_LISTENER);
//			}
//			
//			if(Param.IS_REAL_UAV) {
//				Param.simStatus = SimulatorState.READY_FOR_TEST;//TODO modificar la espera por mis cambios
//			}
			
//			while(Param.simStatus == SimulatorState.SETUP_IN_PROGRESS || Param.simStatus == SimulatorState.READY_FOR_TEST) {
//				Tools.waiting(Scanv1ProtParam.waitState);
//			}
//
//			// If I'm the first one, I take off directly
//			if (idPrev == Scanv1ProtParam.broadcastMAC && Talker.semaphoreSlaveACK3) {
//				Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
//			}else {
//				while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
//					try {
//						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
//						inPacket.setData(inBuffer);
//						socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
//						socketListener.receive(inPacket);
//						input = new Input(inPacket.getData());
//						input.setPosition(0);
//						short tipo = input.readShort();
//						long myID = input.readLong();
//
//						// If MSJ4 is received, someone has given the order for you to take off
//						if (tipo == 4 && myID == Param.id[numUAV]) {
//							Scanv1ProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
//
//						}
//
//					} catch (IOException e) {
//						// e.printStackTrace();
//					}
//				}
//			}
//			
//			
//			
//
//			/** END PHASE WAIT TAKE OFF */
//
//		}

		/** COMMON PHASES OF MASTER AND SLAVE--------------------------------------------------------*/

		/** PHASE TAKING OFF */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.TAKING_OFF_COMMON + "--> Listening");

			//Taking Off to first altitude step
			if (!Copter.takeOff(numUAV, Scanv1Listener.takeOffAltitudeStepOne)) {
				//TODO tratar el error
			}
		}
		// Waiting to reach the altitude of the first point step
		boolean txtPrint = true;
		while (Scanv1ProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			if (Copter.getZRelative(numUAV) < takeOffAltitudeStepOne) {
				if (txtPrint) {
					GUI.log("The UAV " + numUAV + " is going to the safety altitude");
				}
				txtPrint = false;
			} else {
					Scanv1ProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;		
			}

		}
		/** END PHASE TAKING OFF */
		
		
		while (Scanv1ProtParam.WpLast[numUAV][0] != true) {
			/** PHASE MOVE_TO_WP */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.MOVE_TO_WP + "--> Listening");
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				// In this phase only the takeoff is done and nobody's MSJ is expected
				Tools.waiting(Scanv1ProtParam.waitState);
			}
			/** END PHASE MOVE_TO_WP */

			/** PHASE WP_REACHED */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.WP_REACHED + "--> Listening");
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				
				/** If I am the master, I must wait for the confirmation messages from the slaves when they reach the 
				 * point correctly (that is when they reach their WP_REACHED phase) 
				 */
				if (selfId == Scanv1ProtParam.idMaster) {
					try {
						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();

						if (tipo == 9) {
							long idSlave = input.readLong();
							uavsACK4.put(idSlave, idSlave);
						}
					} catch (IOException e) {
						// e.printStackTrace();
					}
				} else {
					//Slaves wait for order to go to the next point if it is not the last one, if it is the last one they make a land 
					// (or could make everyone wait and then land)
					try {
						inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();
						long myID = input.readLong();
						
						if (tipo == 5 && myID == selfId) {
							Scanv1ProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;
						}
					} catch (IOException e) {
						// e.printStackTrace();
					}
				}

			} /** END PHASE WP_REACHED */
			
		} /** END WHILE DE LAS DOS FASES DE MOVIMENTO */

		/** PHASE LANDING */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.LANDING) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.LANDING + "--> Listening");
		}

		while (Scanv1ProtParam.state[numUAV] == SwarmProtState.LANDING) {
			if (selfId == Scanv1ProtParam.idMaster) {
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					
					if (tipo == 10) {
						//Land
						long idSlave = input.readLong();
						uavsACK5.put(idSlave, idSlave);
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}
				if(uavsACK5.size() == UAVsDetected.size() ) {
					Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);
				}
			} else {
				try {
					inBuffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(Scanv1ProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					
					//I do not put the id because it does not matter, if that order is given it is for all
					if (tipo == 6) {
						//Land command
						Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}				
			}
			
			//If the flight mode is LAND it means that it has landed and is disarmed
			if(!Copter.isFlying(numUAV)) {
				Scanv1ProtParam.state[numUAV] = SwarmProtState.FINISH;
			}
			
		}/** END PHASE LANDING **/
		
		/** PHASE FINISH */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.FINISH) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.FINISH + "--> Listening");
		}
		/** END PHASE FINISH */
		
		
		
		

	}

}