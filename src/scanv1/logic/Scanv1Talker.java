package scanv1.logic;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.WaypointSimplified;
import main.Param;
import scanv1.logic.Scanv1ProtParam.SwarmProtState;

public class Scanv1Talker extends Thread {

	private int numUAV;
	private DatagramSocket socket;
	private byte[] buffer;
	private DatagramPacket packet;
	Double missionAltitude;
	Double takeOffAltitudeStepOne;

	// List with master edited flight routes for slaves
	byte[][] sendList;

	// The position of the best UAV to be center
	int centerPos;

	// Timer
	private long cicleTime;

	// Position of master in reorganized vector
	int masterPosReo;

	// Personalized list of Waypoints received from Master
	public static Point3D[][] flightList;

	// Stores the ACK3 of all the Slaves to continue with the protocol
	public static volatile boolean semaphoreSlaveACK3 = false;

	// Indicates in which point of the flight list the UAV is
	public int missionPointNumber = 1;

	// The first point of the mission, each time it moves, increases
	private int actualWP = 0;

	public Scanv1Talker(int numUAV) throws SocketException, UnknownHostException {
		this.numUAV = numUAV;
		this.cicleTime = 0;
		socket = new DatagramSocket();
		buffer = new byte[Scanv1ProtParam.DGRAM_MAX_LENGTH];
		if (Param.role == Tools.MULTICOPTER) {
			socket.setBroadcast(true);
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Scanv1ProtParam.BROADCAST_IP_REAL),
					Scanv1ProtParam.portTalker);
		} else {
			packet = new DatagramPacket(buffer, buffer.length,
					new InetSocketAddress(Scanv1ProtParam.BROADCAST_IP_LOCAL, Scanv1ProtParam.portTalker));
		}

		/**
		 * Calculate the take-off altitude, which uses the take-off algorithm (it is not
		 * the same as the altitude of the mission)
		 */
		missionAltitude = Tools.getUAVMission(Scanv1ProtParam.posMaster).get(1).getAltitude();

		if (missionAltitude <= 5.0) {
			takeOffAltitudeStepOne = 2.0;
		}

		if (missionAltitude > 5.0 && missionAltitude < 10.0) {
			takeOffAltitudeStepOne = missionAltitude / 2;
		}

		if (missionAltitude >= 10.0) {
			takeOffAltitudeStepOne = 5.0;

		}

	}

	@Override
	public void run() {

		// Kryo serialization
		Output output;

		/**
		 * If you are master do: (If it is real, it compares if the MAC of the numUAV is
		 * equal to the mac of the master that is hardcoded. If it's simulated compares
		 * if uav = 0 which is master UAV )
		 */

		// Timer
		int waitingTime;
		if (cicleTime == 0) {
			cicleTime = System.currentTimeMillis();
		}
		
		long selfId = Tools.getIdFromPos(numUAV);

		/**
		 * Master actions
		 * ------------------------------------------------------------------------------------------------------
		 */
		if (selfId == Scanv1ProtParam.idMaster) {

			/** PHASE START */
			// TODO en todos los estados setSwarmState(numuav,"");

			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_START_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTSTART);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				Tools.waiting(Scanv1ProtParam.waitState);
			} /** END PHASE START */

			/** PHASE SEND DATA */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_DATA_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTSEND_DATA);
			}
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
					/** MSJ2 */
					output = new Output(buffer);

					if (Scanv1Listener.positionsAndIDs != null) {
						for (int i = 0; i < Scanv1Listener.positionsAndIDs.length; i++) {
							output.clear();
							output.writeShort(2);
							output.writeLong(Scanv1Listener.positionsAndIDs[i].id);
							output.writeDouble(takeOffAltitudeStepOne);
							output.flush();

							packet.setData(buffer, 0, output.position());

							try {
								// Information send only to Slaves
								Scanv1ProtHelper.sendDataToSlaves(packet, socket);
							} catch (IOException e) {
								GUI.log("Problem in Master START stage");
								e.printStackTrace();
							}

							// Timer
							cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
							waitingTime = (int) (cicleTime - System.currentTimeMillis());
							if (waitingTime > 0) {
								Tools.waiting(waitingTime);
							}
						}
					}
				}
			} /** END PHASE SEND DATA */

			/** PHASE SEND LIST */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.MASTER_SEND_LIST_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTSEND_LIST);

				while (!Scanv1Listener.semaphore) {
					Tools.waiting(Scanv1ProtParam.waitState);
				}

				Scanv1uavPosition[] currentPositions = Scanv1Listener.positionsAndIDs;

				// TODO INICIO (Quitar, solo sirve para medir algoritmo despegue)
				long inicio = System.currentTimeMillis();
				GUI.log("Tiempo de inicio: " + inicio);

				GUI.log("Iniciales: " + Arrays.toString(currentPositions));

				// Calculation of formation flying
				Triplet<Long, Integer, Scanv1uavPosition[]> resultArray = getFormation(currentPositions);

				// TODO Para medir tiempo algoritmo de despegue
				long fin = System.currentTimeMillis();
				GUI.log("Tiempo de fin: " + fin + " resultado: " + (fin - inicio));
				// TODO FIN

				@SuppressWarnings("unused")
				int posMasterReo = resultArray.getValue1();
				long center = resultArray.getValue0();
				Scanv1uavPosition[] takeoffLocations = resultArray.getValue2();

				GUI.log(" Id del centro: " + center + " Posición del maestro: " + posMasterReo);
				GUI.log(Arrays.toString(takeoffLocations));

				List<WaypointSimplified> prueba = Tools.getUAVMissionSimplified(Scanv1ProtParam.posMaster);
				if (prueba.size() > Scanv1ProtParam.maxWaypoints) {
					GUI.exit(Scanv1ProtParam.maxWpMes);
				}
				flightList = new Point3D[Tools.getNumUAVs()][prueba.size()];
				Point3D[] centerMission = new Point3D[prueba.size()];

				boolean found = false;
				centerPos = 0;
				for (int r = 0; r < takeoffLocations.length && !found; r++) {
					if (takeoffLocations[r].id == center) {
						centerPos = r;
						found = true;
					}
				}
				// Mission of the central drone
				Scanv1uavPosition cen = takeoffLocations[centerPos];
				centerMission[0] = new Point3D(cen.x, cen.y, missionAltitude);
				for (int s = 1; s < prueba.size(); s++) {
					WaypointSimplified wp = prueba.get(s);
					centerMission[s] = new Point3D(wp.x, wp.y, wp.z);
				}

				// Copy the mission of the central drone to the neighbors
				Scanv1personalizedMission[] allMissions = new Scanv1personalizedMission[takeoffLocations.length];
				for (int i = 0; i < takeoffLocations.length; i++) {
					Scanv1personalizedMission mission = new Scanv1personalizedMission(takeoffLocations[i].id, centerMission.length);
					if (i == centerPos) {
						for (int j = 0; j < centerMission.length; j++) {
							mission.addPoint(centerMission[j]);
						}
					} else {
						double incX = takeoffLocations[i].x - takeoffLocations[centerPos].x;
						double incY = takeoffLocations[i].y - takeoffLocations[centerPos].y;
						for (int s = 0; s < centerMission.length; s++) {
							Point3D punto = new Point3D();
							Point3D origin = centerMission[s];
							punto.x = origin.x + incX;
							punto.y = origin.y + incY;
							punto.z = origin.z;
							mission.addPoint(punto);
						}
					}

					allMissions[i] = mission;
				}
				output = new Output(buffer);

				sendList = new byte[allMissions.length][];
				for (int i = 0; i < allMissions.length; i++) {
					Point3D[] loopPos = allMissions[i].posiciones;
					// Initialize the array of the mission points of the master
					Scanv1ProtParam.flightListPersonalized[numUAV] = new Point3D[loopPos.length];
					Scanv1ProtParam.flightListPersonalizedAlt[numUAV] = new Double[loopPos.length];

					if (i == masterPosReo) {
						for (int j = 0; j < loopPos.length; j++) {
							// The mission is assigned to the Master point by point
							if (i == 0) {
								Scanv1Listener.idPrevMaster = Scanv1ProtParam.broadcastMAC;
							} else {
								Scanv1Listener.idPrevMaster = allMissions[i - 1].id;
							}
							if (j == loopPos.length - 1) {

								Scanv1Listener.idNextMaster = Scanv1ProtParam.broadcastMAC;
							}
							Scanv1ProtParam.flightListPersonalized[numUAV][j] = new Point3D(loopPos[j].x, loopPos[j].y,
									loopPos[j].z);

						}
					} else {
						output.clear();
						output.writeShort(3);
						output.writeLong(allMissions[i].id);

						if (i == 0) {
							// idPrev
							output.writeLong(Scanv1ProtParam.broadcastMAC);
							// idNext
							output.writeLong(allMissions[i + 1].id);

						}
						if (i != 0 && i != allMissions.length - 1) {
							// idPrev
							output.writeLong(allMissions[i - 1].id);
							// idNext
							output.writeLong(allMissions[i + 1].id);

						}
						if (i == allMissions.length - 1) {
							// idPrev
							output.writeLong(allMissions[i - 1].id);
							// idNext
							output.writeLong(Scanv1ProtParam.broadcastMAC);

						}

						// Number of points (Necessary for the Talker to know how many to read)
						output.writeInt(centerMission.length);
						// Points to send
						for (int x = 0; x < loopPos.length; x++) {
							output.writeDouble(loopPos[x].x);
							output.writeDouble(loopPos[x].y);
							output.writeDouble(loopPos[x].z);
						}

						output.flush();
						sendList[i] = Arrays.copyOf(buffer, output.position());

					}

				}

			}

			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {

				// Send each of the lists of points to its receiver
				for (int j = 0; j < sendList.length; j++) {
					if (j == masterPosReo) {
						for (int z = 0; z < Scanv1ProtParam.flightListPersonalized[numUAV].length; z++) {
							if (Scanv1ProtParam.flightListPersonalized[numUAV][z] == null) {
								Tools.waiting(Scanv1ProtParam.waitState);
							}
						}

						// Initialize the two vectors, one for height and another for coordinates
						Scanv1ProtParam.flightListPersonalizedGeo[numUAV] = new GeoCoordinates[Scanv1ProtParam.flightListPersonalized[numUAV].length];
						Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV] = new Double[Scanv1ProtParam.flightListPersonalized[numUAV].length];

						for (int r = 0; r < Scanv1ProtParam.flightListPersonalized[numUAV].length; r++) {
							// Save heights of each point in flightListIndividualGeoAlt
							Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV][r] = Scanv1ProtParam.flightListPersonalized[numUAV][r].z;
						}
						for (int d = 0; d < Scanv1ProtParam.flightListPersonalized[numUAV].length; d++) {
							// Save each coordinates point in flightListGeo
							Scanv1ProtParam.flightListPersonalizedGeo[numUAV][d] = Tools.UTMToGeo(
									Scanv1ProtParam.flightListPersonalized[numUAV][d].x,
									Scanv1ProtParam.flightListPersonalized[numUAV][d].y);
						}

					} else {
						packet.setData(sendList[j]);
						try {
							Scanv1ProtHelper.sendDataToSlaves(packet, socket);
						} catch (IOException e) {
							GUI.log("Problem in Master SEND LIST stage");
							e.printStackTrace();
						}
					}
				}

				// Timer
				cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}

			} /** END PHASE SEND LIST */

			/** PHASE SEND TAKE OFF */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				GUI.log("Master " + numUAV + ": " + Scanv1ProtText.SEND_TAKE_OFF + "--> Talker");
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTSEND_TAKE_OFF);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				// It only listen for his MSJ4
				Tools.waiting(Scanv1ProtParam.waitState);

			}
			/** END PHASE SEND TAKE OFF */

			/**
			 * Slave actions
			 * ------------------------------------------------------------------------------------------------------
			 */
		} else {

			/** PHASE START */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_START_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTSTART);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.START) {
				/** MSJ1 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(1);
				output.writeLong(selfId);
				Point2D.Double initialPos = Copter.getUTMLocation(numUAV);
				output.writeDouble(initialPos.x);
				output.writeDouble(initialPos.y);
				output.writeDouble(Copter.getHeading(numUAV));
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					Scanv1ProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					GUI.log("Problem in Slave START stage");
					e.printStackTrace();
				}

				// Timer
				cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			} /** END PHASE START */

			/** PHASE WAIT LIST */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_LIST_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTWAIT_LIST);

			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				// Send ACK2 continuously while this is in WAIT_LIST state, listener will change
				// status when receiving MSJ3

				/** ACK2 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(7);
				output.writeLong(selfId);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					Scanv1ProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					GUI.log("Problem in Slave WAIT_LIST stage");
					e.printStackTrace();
				}

				// Timer
				cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			} /** ENF PHASE WAIT LIST */

			/** PHASE WAIT TAKE OFF */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				GUI.log("Slave " + numUAV + ": " + Scanv1ProtText.SLAVE_WAIT_TAKE_OFF_TALKER);
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTWAIT_TAKE_OFF);
			}
			int minimodeenvios = 0;
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				/** ACK3 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(8);
				output.writeLong(selfId);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					Scanv1ProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					GUI.log("Problem in Slave WAIT_TAKE_OFF stage");
					e.printStackTrace();
				}
				minimodeenvios++;
				if (minimodeenvios == 10) {
					semaphoreSlaveACK3 = true;
				}
				// Timer
				cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			} /** ENF PHASE WAIT TAKE OFF */
		}

		/**
		 * COMMON PHASES OF MASTER AND SLAVE
		 * ------------------------------------------------------------
		 */

		/** PHASE TAKING OFF */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.TAKING_OFF_COMMON + "--> Talker");
			GUI.updateProtocolState(numUAV, Scanv1ProtText.INTTAKING_OFF);
		}

		while (Scanv1ProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			Tools.waiting(Scanv1ProtParam.waitState);
		} /** END PHASE TAKING OFF */

		/** PHASE moveToWP */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.MOVE_TO_WP + "-->Talker");
			GUI.updateProtocolState(numUAV, Scanv1ProtText.INTMOVE_TO_WP);

		}

		while (Scanv1ProtParam.WpLast[numUAV][0] != true) {

			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTMOVE_TO_WP);

				if (missionPointNumber == 1 && Scanv1ProtParam.fightPrevNext[numUAV][1] != Scanv1ProtParam.broadcastMAC) {

					long startTime = System.currentTimeMillis(); // fetch starting time
					while ((System.currentTimeMillis() - startTime) < Scanv1ProtParam.timeoutSendingShort) {
						output = new Output(buffer);
						output.clear();
						output.writeShort(4);
						output.writeLong(Scanv1ProtParam.fightPrevNext[numUAV][1]);
						output.flush();
						packet.setData(buffer, 0, output.position());
						try {
							// Data to Master and Slaves because we do not know which is the addressee
							Scanv1ProtHelper.sendDataToMaster(packet, socket);
							Scanv1ProtHelper.sendDataToSlaves(packet, socket);
						} catch (IOException e) {
							GUI.log("Problem in Slave or Master during MOVE_TO_WP stage");
							e.printStackTrace();
						}
						// Timer
						cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}

					}
					
					// Counter to know if it is the first mission point (takeoff)
					missionPointNumber++;

				}

				// If I'm the last one I directly do counter ++ and do not send any takeoff
				// order
				if (Scanv1ProtParam.fightPrevNext[numUAV][1] == Scanv1ProtParam.broadcastMAC) {
					missionPointNumber++;
				}
				if (missionPointNumber > 1) {
					Copter.moveUAV(numUAV, Scanv1ProtParam.flightListPersonalizedGeo[numUAV][actualWP],
							Scanv1ProtParam.flightListPersonalizedAltGeo[numUAV][actualWP].floatValue(),
							Scanv1ProtParam.distToAcceptPointReached, 0.05);

				}

				actualWP++;
				Scanv1ProtParam.state[numUAV] = SwarmProtState.WP_REACHED;

			}
			/** END PHASE moveToWP */

			/** PHASE WP_REACHED */
			if (Scanv1ProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.WP_REACHED + "-->Talker");
				GUI.updateProtocolState(numUAV, Scanv1ProtText.INTWP_REACHED);
			}
			while (Scanv1ProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				// Master
				if (selfId == Scanv1ProtParam.idMaster) {
					// If the master receives confirmation that all slaves have reached the WP, he
					// sends a message to continue the slaves
					if (Scanv1Listener.uavsACK4.size() == Scanv1Listener.UAVsDetected.size()) {
							for (int i = 0; i < Scanv1Listener.positionsAndIDs.length; i++) {
								output = new Output(buffer);
								output.clear();
								output.writeShort(5);
								output.writeLong(Scanv1Listener.positionsAndIDs[i].id);
								output.flush();

								packet.setData(buffer, 0, output.position());

								try {
									// Information send only to Slaves
									Scanv1ProtHelper.sendDataToSlaves(packet, socket);
								} catch (IOException e) {
									GUI.log("Problem in Master START stage");
									e.printStackTrace();
								}

								// Timer
								cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
								waitingTime = (int) (cicleTime - System.currentTimeMillis());
								if (waitingTime > 0) {
									Tools.waiting(waitingTime);
								}

							}
							
						Scanv1Listener.uavsACK4.clear();
						Scanv1ProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;
						GUI.updateProtocolState(numUAV, Scanv1ProtText.INTMOVE_TO_WP);

					} else {
						Tools.waiting(Scanv1ProtParam.waitState);
					}
				} else {// Slave
					/** ACK4 */
					output = new Output(buffer);
					output.clear();
					output.writeShort(9);
					output.writeLong(selfId);
					output.flush();

					packet.setData(buffer, 0, output.position());
					try {
						// Data only for Master
						Scanv1ProtHelper.sendDataToMaster(packet, socket);

					} catch (IOException e) {
						GUI.log("Problem in Slave WP_REACHED stage");
						e.printStackTrace();
					}

					// Timer
					cicleTime = cicleTime + Scanv1ProtParam.swarmStateWait;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			} /** END PHASE WP_REACHED */

			// Detect if it's the last WP
			if (actualWP == Scanv1ProtParam.flightListPersonalizedGeo[numUAV].length) {
				Scanv1ProtParam.WpLast[numUAV][0] = true;
				Scanv1ProtParam.state[numUAV] = SwarmProtState.LANDING;
			}

		} /** END WHILE DE LAS DOS FASES DE MOVIMENTO **/

		/** PHASE LANDING */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.LANDING) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.LANDING + "-->Talker");
			GUI.updateProtocolState(numUAV, Scanv1ProtText.INTLANDING_UAV);
		}

		while (Scanv1ProtParam.state[numUAV] == SwarmProtState.LANDING) {
			if (selfId == Scanv1ProtParam.idMaster) {
				output = new Output(buffer);
				output.clear();
				output.writeShort(6);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Slaves
					Scanv1ProtHelper.sendDataToSlaves(packet, socket);
				} catch (IOException e) {
					GUI.log("Problem in Master sending LANDING order");
					e.printStackTrace();
				}

			} else {
				output = new Output(buffer);
				output.clear();
				output.writeShort(10);
				output.writeLong(selfId);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					Scanv1ProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					GUI.log("Problem in Slave LANDING stage");
					e.printStackTrace();
				}

			}
		} /** END PHASE LANDING **/

		/** PHASE FINISH */
		if (Scanv1ProtParam.state[numUAV] == SwarmProtState.FINISH) {
			GUI.log("UAV " + numUAV + ": " + Scanv1ProtText.FINISH + "--> Talker");
			GUI.updateProtocolState(numUAV, Scanv1ProtText.INTFINISH);
		}
		/** END PHASE FINISH */

	}

	/** Methods */

	// Obtain the initial flight positions
	private Triplet<Long, Integer, Scanv1uavPosition[]> getFormation(Scanv1uavPosition[] current) {

		Scanv1uavPosition[] bestFormation = null;
		double distanceOfBestFormation = Double.MAX_VALUE;
		// long centerGlobal = Long.MAX_VALUE;
		// long currentCenter;

		// We look for the drone that behave like center drone
		for (int i = 0; i < current.length; i++) {
			// currentCenter = current[i].id;
			// Distance in flight between drones initialDistanceBetweenUAVreal

			// We obtain coordinates for the drones that surround the central.
			Point2D.Double[] formation = Scanv1ProtHelper.posSlaveFromMasterLinear(Scanv1ProtParam.masterHeading,
					current[i], current.length - 1, Scanv1ProtParam.initialDistanceBetweenUAVreal);
			Point2D.Double[] flightFormationFinal = Arrays.copyOf(formation, formation.length + 1);

			// Add to the end of the array the one that is being viewed if it is the center
			flightFormationFinal[flightFormationFinal.length - 1] = current[i];

			// Copy of Ids
			Long[] ids = new Long[flightFormationFinal.length];
			for (int j = 0; j < current.length; j++) {
				ids[j] = current[j].id;
			}

			// Calculate all possible combinations of ids
			Scanv1Permutation<Long> prueba = new Scanv1Permutation<Long>(ids);
			long[] bestForCurrentCenter = null;
			double dOfCurrentCenter = Double.MAX_VALUE;
			int j = factorial(ids.length);
			while (j > 0) {
				// Test of a permutation
				Long[] mix = prueba.next();
				double acum = 0;
				// Calculation of the sum of distances to the square of each drone on the ground
				// to the position that would correspond
				// to it in the air according to the current combination of ids
				for (int k = 0; k < current.length; k++) {
					Scanv1uavPosition c = current[k];
					boolean found = false;
					int pos = -1;
					// Find the position "pos" in the air for current Ids combination
					for (int n = 0; n < mix.length && !found; n++) {

						if (mix[n] == c.id) {
							pos = n;
							found = true;
						}
					}
					// Adjustment of least squares. The distance is exaggerated to know which is
					// closest
					acum = acum + Math.pow(c.distance(flightFormationFinal[pos]), 2);
				}

				if (i == 1) {
					// System.out.println("Centro: " + currentCenter + " acum: " + acum + " en i: "
					// + i);
					GUI.log(Arrays.toString(mix));
				}

				if (bestForCurrentCenter == null || acum < dOfCurrentCenter) {
					bestForCurrentCenter = new long[mix.length];
					for (int t = 0; t < mix.length; t++) {
						bestForCurrentCenter[t] = mix[t];
					}
					dOfCurrentCenter = acum;

				}
				j--;
			}

			// System.out.println("Error acumulado: " + dOfCurrentCenter + " para centro " +
			// currentCenter);

			if (bestFormation == null || dOfCurrentCenter < distanceOfBestFormation) {
				j = 0;
				bestFormation = new Scanv1uavPosition[bestForCurrentCenter.length];
				while (j < bestForCurrentCenter.length) {
					bestFormation[j] = new Scanv1uavPosition(flightFormationFinal[j].x, flightFormationFinal[j].y,
							bestForCurrentCenter[j], Scanv1ProtParam.masterHeading);
					distanceOfBestFormation = dOfCurrentCenter;
					// centerGlobal = currentCenter;
					boolean found = false;
					for (int x = 0; x < bestForCurrentCenter.length; x++) {
						if (bestForCurrentCenter[j] == Scanv1ProtParam.idMaster) {
							found = true;
						}
					}
					if (found) {
						masterPosReo = j;
					}
					j++;
				}

			}
		}

		// Cálculo del centro
		long centerGlobal = Long.MAX_VALUE;
		double globalError = Double.MAX_VALUE;
		for (int i = 0; i < bestFormation.length; i++) {
			double acum = 0;
			for (int j = 0; j < bestFormation.length; j++) {
				if (i != j) {
					acum += Math.pow(bestFormation[i].distance(bestFormation[j]), 2);
				}
			}
			if (acum < globalError) {
				centerGlobal = bestFormation[i].id;
			}
		}

		return Triplet.with(centerGlobal, masterPosReo, bestFormation);
	}

	public static int factorial(int n) {
		int resultado = 1;
		for (int i = 1; i <= n; i++) {
			resultado *= i;
		}
		return resultado;
	}

}
