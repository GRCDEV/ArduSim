package swarmprot.logic;

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
import api.API;
import api.GUIHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import main.Param;
import main.Param.SimulatorState;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class Listener extends Thread {

	private int numUAV;
	private DatagramSocket socketListener;
	private byte[] inBuffer;
	private DatagramPacket inPacket;
	Input input;

	// This ConcurrentHashMap contains the UAVs detected in START phase
	public static Map<Long, uavPosition> UAVsDetected = new ConcurrentHashMap<Long, uavPosition>();

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
	public static uavPosition[] positionsAndIDs;

	// Id order take off
	public long idPrev;
	public long idNext;
	public static long idPrevMaster;
	public static long idNextMaster;

	// Identifier that indicates the last WP
	public static boolean WPLast = false;

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
			// Add the UAV number to calculate the available port
			this.socketListener = new DatagramSocket(new InetSocketAddress(SwarmProtParam.BROADCAST_IP_LOCAL, SwarmProtParam.port + numUAV));
		}

		inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
		inPacket = new DatagramPacket(inBuffer, inBuffer.length);

	}

	@Override
	public void run() {
		/**
		 * If you are master do: (If it is real, it compares if the MAC of the numUAV is
		 * equal to the MAC of the master that is hardcoded. If it's simulated compares
		 * if UAV = 0 (which is the master's identifier in simulation))
		 */
		
		while (Param.simStatus == Param.SimulatorState.CONFIGURING
				|| Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL
				|| Param.simStatus == Param.SimulatorState.STARTING_UAVS) {
			GUIHelper.waiting(SwarmProtParam.waitState);
		}
		
		// Starting time
		long startTime = System.currentTimeMillis();

		/** Master actions ----------------------------------------------------------------------------------- */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

			/** PHASE START */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_START_LISTENER);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				
				while(Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED) {
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
							// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
							Long idSlave = input.readLong();
							uavPosition posiciones = new uavPosition(input.readDouble(), input.readDouble(), idSlave,
									input.readDouble());
							// ADD to Map the new UAV detected
							UAVsDetected.put(idSlave, posiciones);
						}
					} catch (IOException e) {
						System.err.println(SwarmProtText.MASTER_SOCKET_READ_ERROR);
						e.printStackTrace();
					}
				}
				
				
				// Master wait slave UAVs for 20 seconds
//				while ((System.currentTimeMillis() - startTime) < SwarmProtParam.waitTimeForSlaves) {
//					try {
//						inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
//						inPacket.setData(inBuffer);
//						// The socket is unlocked after a given time
//						socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
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
//						System.err.println(SwarmProtText.MASTER_SOCKET_READ_ERROR);
//						e.printStackTrace();
//					}
//				}

				// Convert HashMap to uavPosition with IDs and initial position
				Iterator<Map.Entry<Long, uavPosition>> entries;
				Map.Entry<Long, uavPosition> entry;
				entries = UAVsDetected.entrySet().iterator();
				int i = 0;
				// +1 to include master ID
				positionsAndIDs = new uavPosition[UAVsDetected.size() + 1];

				while (entries.hasNext()) {
					entry = entries.next();
					positionsAndIDs[i] = entry.getValue();
					i++;
				}
				// Set Master ID and initial position to the last position
				Point2D.Double UTMMaster = UAVParam.uavCurrentData[numUAV].getUTMLocation();
				Double headingMaster = UAVParam.uavCurrentData[numUAV].getHeading();
				uavPosition masterPosition = new uavPosition();
				masterPosition.id = SwarmProtParam.idMaster;
				masterPosition.x = UTMMaster.x;
				masterPosition.y = UTMMaster.y;
				masterPosition.heading = headingMaster;
				positionsAndIDs[positionsAndIDs.length - 1] = masterPosition;

				//For flow control
				semaphore = true;

				// How many drones have been detected?
				SwarmHelper.log(SwarmProtText.DETECTED + UAVsDetected.size() + " UAVs");

				// Change to next phase
				SwarmProtParam.state[numUAV] = SwarmProtState.SEND_DATA;

			} /** END PHASE START */

			/** PHASE SEND DATA */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_SEND_DATA_LISTENER);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
			
				/** Waiting ACK2 from all slaves */
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
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
						SwarmProtParam.state[numUAV] = SwarmProtState.SEND_LIST;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND DATA */

			/** PHASE SEND LIST */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_SEND_LIST_LISTENER);
			}
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
						uavsACK3.put(idSlave, idSlave);
					}
					if (UAVsDetected.size() == uavsACK3.size()) {
						if (idPrevMaster == SwarmProtParam.broadcastMAC) {
							SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
						} else {
							SwarmProtParam.state[numUAV] = SwarmProtState.SEND_TAKE_OFF;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} /** END PHASE SEND LIST */
			
			//Sync time
			GUIHelper.waiting(3000);
			
			Param.simStatus = SimulatorState.READY_FOR_TEST;
			while (Param.simStatus == SimulatorState.READY_FOR_TEST) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			}

			/** PHASE SEND TAKE OFF */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.SEND_TAKE_OFF + "--> Listening");
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {

				/** Waiting for TAKE_OFF MSJ */
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 4 && myID == Param.id[numUAV]) {
						SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
			/** END PHASE SEND TAKE OFF */

			/** Slave actions---------------------------------------------------------------*/
		} else {
			/** PHASE_START */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_START_LISTENER);
			}
			
			while(Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			}
			
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				
				/** Waiting for MSJ2 */
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();
					if (tipo == 2 && myID == Param.id[numUAV]) {
						takeOffAltitudeStepOne = input.readDouble();
						SwarmProtParam.state[numUAV] = SwarmProtState.WAIT_LIST;
					}

				} catch (IOException e) {
					// e.printStackTrace();
				}
			} /** END PHASE START */
			
			/** PHASE WAIT LIST */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_WAIT_LIST_LISTENER);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					long myID = input.readLong();

					if (tipo == 3 && myID == Param.id[numUAV]) {

						idPrev = input.readLong();
						idNext = input.readLong();
						// The matrix is ​​filled with the idPrev and idNext (for take off step) of each UAV
						SwarmProtParam.fightPrevNext[numUAV][0] = idPrev;
						SwarmProtParam.fightPrevNext[numUAV][1] = idNext;

						// The number of WP is read to know the loop length
						int numberOfWp = input.readInt();
						SwarmProtParam.flightListPersonalized[numUAV] = new Point3D[numberOfWp];
						SwarmProtParam.flightListPersonalizedAlt[numUAV] = new Double[numberOfWp];

						for (int i = 0; i < numberOfWp; i++) {
							SwarmProtParam.flightListPersonalized[numUAV][i] = new Point3D(input.readDouble(),
									input.readDouble(), input.readDouble());

						}

						// Initialize the geometric coordinates array and altitude array
						SwarmProtParam.flightListPersonalizedGeo[numUAV] = new GeoCoordinates[numberOfWp];
						SwarmProtParam.flightListPersonalizedAltGeo[numUAV] = new Double[numberOfWp];

						for (int s = 0; s < SwarmProtParam.flightListPersonalized[numUAV].length; s++) {
							SwarmProtParam.flightListPersonalizedGeo[numUAV][s] = GUIHelper.UTMToGeo(SwarmProtParam.flightListPersonalized[numUAV][s].x, SwarmProtParam.flightListPersonalized[numUAV][s].y);
							SwarmProtParam.flightListPersonalizedAltGeo[numUAV][s] = SwarmProtParam.flightListPersonalized[numUAV][s].z;
						}

						SwarmProtParam.state[numUAV] = SwarmProtState.WAIT_TAKE_OFF;
					} else {

					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
			} /** END PHASE WAIT LIST */

			/** PHASE WAIT TAKE OFF */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_WAIT_TAKE_OFF_LISTENER);
			}
			
			if(Param.IS_REAL_UAV) {
				Param.simStatus = SimulatorState.READY_FOR_TEST;
			}
			
			while(Param.simStatus == SimulatorState.SETUP_IN_PROGRESS || Param.simStatus == SimulatorState.READY_FOR_TEST) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			}

			// If I'm the first one, I take off directly
			if (idPrev == SwarmProtParam.broadcastMAC && Talker.semaphoreSlaveACK3) {
				SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;
			}else {
				while (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
					try {
						inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();
						long myID = input.readLong();

						// If MSJ4 is received, someone has given the order for you to take off
						if (tipo == 4 && myID == Param.id[numUAV]) {
							SwarmProtParam.state[numUAV] = SwarmProtState.TAKING_OFF;

						}

					} catch (IOException e) {
						// e.printStackTrace();
					}
				}
			}
			
			
			

			/** END PHASE WAIT TAKE OFF */

		}

		/** COMMON PHASES OF MASTER AND SLAVE--------------------------------------------------------*/

		/** PHASE TAKING OFF */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.TAKING_OFF_COMMON + "--> Listening");

			SwarmProtHelper.takeOffIndividual(numUAV);
		}
		// Waiting to reach the altitude of the first point step
		boolean txtPrint = true;
		while (SwarmProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			if (UAVParam.uavCurrentData[numUAV].getZRelative() < takeOffAltitudeStepOne) {
				if (txtPrint) {
					SwarmHelper.log("The UAV " + numUAV + " is going to the safety altitude");
				}
				txtPrint = false;
			} else {
					SwarmProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;		
			}

		}
		/** END PHASE TAKING OFF */
		
		
		while (SwarmProtParam.WpLast[numUAV][0] != true) {
			/** PHASE MOVE_TO_WP */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.MOVE_TO_WP + "--> Listening");
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				// In this phase only the takeoff is done and nobody's MSJ is expected
				GUIHelper.waiting(SwarmProtParam.waitState);
			}
			/** END PHASE MOVE_TO_WP */

			/** PHASE WP_REACHED */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.WP_REACHED + "--> Listening");
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				
				/** If I am the master, I must wait for the confirmation messages from the slaves when they reach the 
				 * point correctly (that is when they reach their WP_REACHED phase) 
				 */
				if (Param.id[numUAV] == SwarmProtParam.idMaster) {
					try {
						inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
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
						inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
						inPacket.setData(inBuffer);
						socketListener.receive(inPacket);
						input = new Input(inPacket.getData());
						input.setPosition(0);
						short tipo = input.readShort();
						long myID = input.readLong();
						
						if (tipo == 5 && myID == Param.id[numUAV]) {
							SwarmProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;
						}
					} catch (IOException e) {
						// e.printStackTrace();
					}
				}

			} /** END PHASE WP_REACHED */
			
		} /** END WHILE DE LAS DOS FASES DE MOVIMENTO */

		/** PHASE LANDING */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.LANDING) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.LANDING + "--> Listening");
		}

		while (SwarmProtParam.state[numUAV] == SwarmProtState.LANDING) {
			if (Param.id[numUAV] == SwarmProtParam.idMaster) {
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
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
					API.setMode(numUAV, UAVParam.Mode.LAND_ARMED);
				}
			} else {
				try {
					inBuffer = new byte[SwarmProtParam.DGRAM_MAX_LENGTH];
					inPacket.setData(inBuffer);
					socketListener.setSoTimeout(SwarmProtParam.recTimeOut);
					socketListener.receive(inPacket);
					input = new Input(inPacket.getData());
					input.setPosition(0);
					short tipo = input.readShort();
					
					//I do not put the id because it does not matter, if that order is given it is for all
					if (tipo == 6) {
						//Land command
						API.setMode(numUAV, UAVParam.Mode.LAND_ARMED);
					}
				} catch (IOException e) {
					// e.printStackTrace();
				}				
			}
			
			//If the flight mode is LAND it means that it has landed and is disarmed
			if(UAVParam.flightMode.get(numUAV).getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING) {
				SwarmProtParam.state[numUAV] = SwarmProtState.FINISH;
			}
			
		}/** END PHASE LANDING **/
		
		/** PHASE FINISH */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.FINISH) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.FINISH + "--> Listening");
		}
		/** END PHASE FINISH */
		
		
		
		

	}

}