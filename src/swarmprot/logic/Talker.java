package swarmprot.logic;

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
import api.API;
import api.GUIHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.WaypointSimplified;
import main.Param;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class Talker extends Thread {

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

		/**
		 * Master actions
		 * ------------------------------------------------------------------------------------------------------
		 */
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {

			/** PHASE START */
			// TODO en todos los estados setSwarmState(numuav,"");

			if (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_START_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTSTART);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				GUIHelper.waiting(SwarmProtParam.waitState);
			} /** END PHASE START */

			/** PHASE SEND DATA */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_SEND_DATA_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTSEND_DATA);
			}
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
				while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_DATA) {
					/** MSJ2 */
					output = new Output(buffer);

					if (Listener.positionsAndIDs != null) {
						for (int i = 0; i < Listener.positionsAndIDs.length; i++) {
							output.clear();
							output.writeShort(2);
							output.writeLong(Listener.positionsAndIDs[i].id);
							output.writeDouble(takeOffAltitudeStepOne);
							output.flush();

							packet.setData(buffer, 0, output.position());

							try {
								// Information send only to Slaves
								SwarmProtHelper.sendDataToSlaves(packet, socket);
							} catch (IOException e) {
								SwarmHelper.log("Problem in Master START stage");
								e.printStackTrace();
							}

							// Timer
							cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
							waitingTime = (int) (cicleTime - System.currentTimeMillis());
							if (waitingTime > 0) {
								GUIHelper.waiting(waitingTime);
							}
						}
					}
				}
			} /** END PHASE SEND DATA */

			/** PHASE SEND LIST */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.MASTER_SEND_LIST_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTSEND_LIST);

				while (!Listener.semaphore) {
					GUIHelper.waiting(SwarmProtParam.waitState);
				}

				uavPosition[] currentPositions = Listener.positionsAndIDs;

				// TODO INICIO (Quitar, solo sirve para medir algoritmo despegue)
				long inicio = System.currentTimeMillis();
				System.out.println("Tiempo de inicio: " + inicio);

				System.out.println("Iniciales: " + Arrays.toString(currentPositions));

				// Calculation of formation flying
				Triplet<Long, Integer, uavPosition[]> resultArray = getFormation(currentPositions);

				// TODO Para medir tiempo algoritmo de despegue
				long fin = System.currentTimeMillis();
				System.out.println("Tiempo de fin: " + fin + " resultado: " + (fin - inicio));
				// TODO FIN

				@SuppressWarnings("unused")
				int posMasterReo = resultArray.getValue1();
				long center = resultArray.getValue0();
				uavPosition[] takeoffLocations = resultArray.getValue2();

				System.out.println(" Id del centro: " + center + " Posición del maestro: " + posMasterReo);
				System.out.println(Arrays.toString(takeoffLocations));

				List<WaypointSimplified> prueba = UAVParam.missionUTMSimplified.get(SwarmProtParam.posMaster);
				if (prueba.size() > SwarmProtParam.maxWaypoints) {
					GUIHelper.exit(SwarmProtParam.maxWpMes);
				}
				flightList = new Point3D[Param.numUAVs][prueba.size()];
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
				uavPosition cen = takeoffLocations[centerPos];
				centerMission[0] = new Point3D(cen.x, cen.y, missionAltitude);
				for (int s = 1; s < prueba.size(); s++) {
					WaypointSimplified wp = prueba.get(s);
					centerMission[s] = new Point3D(wp.x, wp.y, wp.z);
				}

				// Copy the mission of the central drone to the neighbors
				personalizedMission[] allMissions = new personalizedMission[takeoffLocations.length];
				for (int i = 0; i < takeoffLocations.length; i++) {
					personalizedMission mission = new personalizedMission(takeoffLocations[i].id, centerMission.length);
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
					SwarmProtParam.flightListPersonalized[numUAV] = new Point3D[loopPos.length];
					SwarmProtParam.flightListPersonalizedAlt[numUAV] = new Double[loopPos.length];

					if (i == masterPosReo) {
						for (int j = 0; j < loopPos.length; j++) {
							// The mission is assigned to the Master point by point
							if (i == 0) {
								Listener.idPrevMaster = SwarmProtParam.broadcastMAC;
							} else {
								Listener.idPrevMaster = allMissions[i - 1].id;
							}
							if (j == loopPos.length - 1) {

								Listener.idNextMaster = SwarmProtParam.broadcastMAC;
							}
							SwarmProtParam.flightListPersonalized[numUAV][j] = new Point3D(loopPos[j].x, loopPos[j].y,
									loopPos[j].z);

						}
					} else {
						output.clear();
						output.writeShort(3);
						output.writeLong(allMissions[i].id);

						if (i == 0) {
							// idPrev
							output.writeLong(SwarmProtParam.broadcastMAC);
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
							output.writeLong(SwarmProtParam.broadcastMAC);

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

			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_LIST) {

				// Send each of the lists of points to its receiver
				for (int j = 0; j < sendList.length; j++) {
					if (j == masterPosReo) {
						for (int z = 0; z < SwarmProtParam.flightListPersonalized[numUAV].length; z++) {
							if (SwarmProtParam.flightListPersonalized[numUAV][z] == null) {
								GUIHelper.waiting(SwarmProtParam.waitState);
							}
						}

						// Initialize the two vectors, one for height and another for coordinates
						SwarmProtParam.flightListPersonalizedGeo[numUAV] = new GeoCoordinates[SwarmProtParam.flightListPersonalized[numUAV].length];
						SwarmProtParam.flightListPersonalizedAltGeo[numUAV] = new Double[SwarmProtParam.flightListPersonalized[numUAV].length];

						for (int r = 0; r < SwarmProtParam.flightListPersonalized[numUAV].length; r++) {
							// Save heights of each point in flightListIndividualGeoAlt
							SwarmProtParam.flightListPersonalizedAltGeo[numUAV][r] = SwarmProtParam.flightListPersonalized[numUAV][r].z;
						}
						for (int d = 0; d < SwarmProtParam.flightListPersonalized[numUAV].length; d++) {
							// Save each coordinates point in flightListGeo
							SwarmProtParam.flightListPersonalizedGeo[numUAV][d] = GUIHelper.UTMToGeo(
									SwarmProtParam.flightListPersonalized[numUAV][d].x,
									SwarmProtParam.flightListPersonalized[numUAV][d].y);
						}

					} else {
						packet.setData(sendList[j]);
						try {
							SwarmProtHelper.sendDataToSlaves(packet, socket);
						} catch (IOException e) {
							SwarmHelper.log("Problem in Master SEND LIST stage");
							e.printStackTrace();
						}
					}
				}

				// Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}

			} /** END PHASE SEND LIST */

			/** PHASE SEND TAKE OFF */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				SwarmHelper.log("Master " + numUAV + ": " + SwarmProtText.SEND_TAKE_OFF + "--> Talker");
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTSEND_TAKE_OFF);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.SEND_TAKE_OFF) {
				// It only listen for his MSJ4
				GUIHelper.waiting(SwarmProtParam.waitState);

			}
			/** END PHASE SEND TAKE OFF */

			/**
			 * Slave actions
			 * ------------------------------------------------------------------------------------------------------
			 */
		} else {

			/** PHASE START */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_START_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTSTART);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.START) {
				/** MSJ1 */
				output = new Output(buffer);
				output.clear();
				output.writeShort(1);
				output.writeLong(Param.id[numUAV]);
				Point2D.Double initialPos = UAVParam.uavCurrentData[numUAV].getUTMLocation();
				output.writeDouble(initialPos.x);
				output.writeDouble(initialPos.y);
				output.writeDouble(UAVParam.uavCurrentData[numUAV].getHeading());
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Slave START stage");
					e.printStackTrace();
				}

				// Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} /** END PHASE START */

			/** PHASE WAIT LIST */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_LIST) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_WAIT_LIST_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTWAIT_LIST);

			}
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

				// Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} /** ENF PHASE WAIT LIST */

			/** PHASE WAIT TAKE OFF */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WAIT_TAKE_OFF) {
				SwarmHelper.log("Slave " + numUAV + ": " + SwarmProtText.SLAVE_WAIT_TAKE_OFF_TALKER);
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTWAIT_TAKE_OFF);
			}
			int minimodeenvios = 0;
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
				minimodeenvios++;
				if (minimodeenvios == 10) {
					semaphoreSlaveACK3 = true;
				}
				// Timer
				cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} /** ENF PHASE WAIT TAKE OFF */
		}

		/**
		 * COMMON PHASES OF MASTER AND SLAVE
		 * ------------------------------------------------------------
		 */

		/** PHASE TAKING OFF */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.TAKING_OFF_COMMON + "--> Talker");
			SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTTAKING_OFF);
		}

		while (SwarmProtParam.state[numUAV] == SwarmProtState.TAKING_OFF) {
			GUIHelper.waiting(SwarmProtParam.waitState);
		} /** END PHASE TAKING OFF */

		/** PHASE moveToWP */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.MOVE_TO_WP + "-->Talker");
			SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTMOVE_TO_WP);

		}

		while (SwarmProtParam.WpLast[numUAV][0] != true) {

			while (SwarmProtParam.state[numUAV] == SwarmProtState.MOVE_TO_WP) {
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTMOVE_TO_WP);

				if (missionPointNumber == 1 && SwarmProtParam.fightPrevNext[numUAV][1] != SwarmProtParam.broadcastMAC) {

					long startTime = System.currentTimeMillis(); // fetch starting time
					while ((System.currentTimeMillis() - startTime) < SwarmProtParam.timeoutSendingShort) {
						output = new Output(buffer);
						output.clear();
						output.writeShort(4);
						output.writeLong(SwarmProtParam.fightPrevNext[numUAV][1]);
						output.flush();
						packet.setData(buffer, 0, output.position());
						try {
							// Data to Master and Slaves because we do not know which is the addressee
							SwarmProtHelper.sendDataToMaster(packet, socket);
							SwarmProtHelper.sendDataToSlaves(packet, socket);
						} catch (IOException e) {
							SwarmHelper.log("Problem in Slave or Master during MOVE_TO_WP stage");
							e.printStackTrace();
						}
						// Timer
						cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							GUIHelper.waiting(waitingTime);
						}

					}
					
					// Counter to know if it is the first mission point (takeoff)
					missionPointNumber++;

				}

				// If I'm the last one I directly do counter ++ and do not send any takeoff
				// order
				if (SwarmProtParam.fightPrevNext[numUAV][1] == SwarmProtParam.broadcastMAC) {
					missionPointNumber++;
				}
				if (missionPointNumber > 1) {
					API.moveUAV(numUAV, SwarmProtParam.flightListPersonalizedGeo[numUAV][actualWP],
							SwarmProtParam.flightListPersonalizedAltGeo[numUAV][actualWP].floatValue(),
							SwarmProtParam.distToAcceptPointReached, 0.05);

				}

				actualWP++;
				SwarmProtParam.state[numUAV] = SwarmProtState.WP_REACHED;

			}
			/** END PHASE moveToWP */

			/** PHASE WP_REACHED */
			if (SwarmProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.WP_REACHED + "-->Talker");
				SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTWP_REACHED);
			}
			while (SwarmProtParam.state[numUAV] == SwarmProtState.WP_REACHED) {
				// Master
				if (Param.id[numUAV] == SwarmProtParam.idMaster) {
					// If the master receives confirmation that all slaves have reached the WP, he
					// sends a message to continue the slaves
					if (Listener.uavsACK4.size() == Listener.UAVsDetected.size()) {
							for (int i = 0; i < Listener.positionsAndIDs.length; i++) {
								output = new Output(buffer);
								output.clear();
								output.writeShort(5);
								output.writeLong(Listener.positionsAndIDs[i].id);
								output.flush();

								packet.setData(buffer, 0, output.position());

								try {
									// Information send only to Slaves
									SwarmProtHelper.sendDataToSlaves(packet, socket);
								} catch (IOException e) {
									SwarmHelper.log("Problem in Master START stage");
									e.printStackTrace();
								}

								// Timer
								cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
								waitingTime = (int) (cicleTime - System.currentTimeMillis());
								if (waitingTime > 0) {
									GUIHelper.waiting(waitingTime);
								}

							}
							
						Listener.uavsACK4.clear();
						SwarmProtParam.state[numUAV] = SwarmProtState.MOVE_TO_WP;
						SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTMOVE_TO_WP);

					} else {
						GUIHelper.waiting(SwarmProtParam.waitState);
					}
				} else {// Slave
					/** ACK4 */
					output = new Output(buffer);
					output.clear();
					output.writeShort(9);
					output.writeLong(Param.id[numUAV]);
					output.flush();

					packet.setData(buffer, 0, output.position());
					try {
						// Data only for Master
						SwarmProtHelper.sendDataToMaster(packet, socket);

					} catch (IOException e) {
						SwarmHelper.log("Problem in Slave WP_REACHED stage");
						e.printStackTrace();
					}

					// Timer
					cicleTime = cicleTime + SwarmProtParam.swarmStateWait;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						GUIHelper.waiting(waitingTime);
					}
				}
			} /** END PHASE WP_REACHED */

			// Detect if it's the last WP
			if (actualWP == SwarmProtParam.flightListPersonalizedGeo[numUAV].length) {
				SwarmProtParam.WpLast[numUAV][0] = true;
				SwarmProtParam.state[numUAV] = SwarmProtState.LANDING;
			}

		} /** END WHILE DE LAS DOS FASES DE MOVIMENTO **/

		/** PHASE LANDING */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.LANDING) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.LANDING + "-->Talker");
			SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTLANDING_UAV);
		}

		while (SwarmProtParam.state[numUAV] == SwarmProtState.LANDING) {
			if (Param.id[numUAV] == SwarmProtParam.idMaster) {
				output = new Output(buffer);
				output.clear();
				output.writeShort(6);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Slaves
					SwarmProtHelper.sendDataToSlaves(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Master sending LANDING order");
					e.printStackTrace();
				}

			} else {
				output = new Output(buffer);
				output.clear();
				output.writeShort(10);
				output.writeLong(Param.id[numUAV]);
				output.flush();

				packet.setData(buffer, 0, output.position());

				try {
					// Data only for Master
					SwarmProtHelper.sendDataToMaster(packet, socket);
				} catch (IOException e) {
					SwarmHelper.log("Problem in Slave LANDING stage");
					e.printStackTrace();
				}

			}
		} /** END PHASE LANDING **/

		/** PHASE FINISH */
		if (SwarmProtParam.state[numUAV] == SwarmProtState.FINISH) {
			SwarmHelper.log("UAV " + numUAV + ": " + SwarmProtText.FINISH + "--> Talker");
			SwarmHelper.setSwarmState(numUAV, SwarmProtText.INTFINISH);
		}
		/** END PHASE FINISH */

	}

	/** Methods */

	// Obtain the initial flight positions
	private Triplet<Long, Integer, uavPosition[]> getFormation(uavPosition[] current) {

		uavPosition[] bestFormation = null;
		double distanceOfBestFormation = Double.MAX_VALUE;
		// long centerGlobal = Long.MAX_VALUE;
		// long currentCenter;

		// We look for the drone that behave like center drone
		for (int i = 0; i < current.length; i++) {
			// currentCenter = current[i].id;
			// Distance in flight between drones initialDistanceBetweenUAVreal

			// We obtain coordinates for the drones that surround the central.
			Point2D.Double[] formation = SwarmProtHelper.posSlaveFromMasterLinear(SwarmProtParam.masterHeading,
					current[i], current.length - 1, SwarmProtParam.initialDistanceBetweenUAVreal);
			Point2D.Double[] flightFormationFinal = Arrays.copyOf(formation, formation.length + 1);

			// Add to the end of the array the one that is being viewed if it is the center
			flightFormationFinal[flightFormationFinal.length - 1] = current[i];

			// Copy of Ids
			Long[] ids = new Long[flightFormationFinal.length];
			for (int j = 0; j < current.length; j++) {
				ids[j] = current[j].id;
			}

			// Calculate all possible combinations of ids
			Permutation<Long> prueba = new Permutation<Long>(ids);
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
					uavPosition c = current[k];
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
					System.out.println(Arrays.toString(mix));
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
				bestFormation = new uavPosition[bestForCurrentCenter.length];
				while (j < bestForCurrentCenter.length) {
					bestFormation[j] = new uavPosition(flightFormationFinal[j].x, flightFormationFinal[j].y,
							bestForCurrentCenter[j], SwarmProtParam.masterHeading);
					distanceOfBestFormation = dOfCurrentCenter;
					// centerGlobal = currentCenter;
					boolean found = false;
					for (int x = 0; x < bestForCurrentCenter.length; x++) {
						if (bestForCurrentCenter[j] == SwarmProtParam.idMaster) {
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
