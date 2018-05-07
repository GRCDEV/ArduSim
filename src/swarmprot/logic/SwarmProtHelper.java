package swarmprot.logic;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.javatuples.Pair;
import api.API;
import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Text;
import main.Tools;
import sim.board.BoardParam;
import sim.logic.SimParam;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class SwarmProtHelper {
	/** Initialize Swarm Protocol Data Structures */
	public static void initializeProtocolDataStructures() {
		SwarmProtParam.masterHeading = 0.0;
		SwarmProtParam.state = new SwarmProtState[Param.numUAVs];
		//UTM
		SwarmProtParam.flightListPersonalized = new Point3D[Param.numUAVs][];
		SwarmProtParam.flightListPersonalizedAlt = new Double[Param.numUAVs][];
		//GEO
		SwarmProtParam.flightListPersonalizedGeo = new GeoCoordinates[Param.numUAVs][];
		SwarmProtParam.flightListPersonalizedAltGeo = new Double[Param.numUAVs][];
		
		//Drones with his prev and next for takeoff
		SwarmProtParam.fightPrevNext = new long[Param.numUAVs][2];
		
		//Inicialización vector detección ultimo WP
		SwarmProtParam.WpLast = new boolean[Param.numUAVs][1];



		/** The state machine starts with the status "START" */
		for (int i = 0; i < Param.numUAVs; i++) {
			SwarmProtParam.state[i] = SwarmProtState.START;
		}

	}

	/** Launch Listener and Talker threads of each UAV */
	public static void startSwarmThreads() {
		try {
		for (int i = 0; i < Param.numUAVs; i++) {
			(new Listener(i)).start();
			(new Talker(i)).start();
		}
		SwarmHelper.log(SwarmProtText.ENABLING);
		}catch(SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
		
	}

	/** Get the position on the map of the drones, in simulation mode */
	@SuppressWarnings("unchecked")
	public static Pair<GeoCoordinates, Double>[] getSwarmStartingLocationV1() {
		/** Position the master on the map */
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;

		if (UAVParam.missionGeoLoaded != null && UAVParam.missionGeoLoaded[SwarmProtParam.posMaster] != null) {
			waypointFound = false;
			for (int j = 0; j < UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].size() && !waypointFound; j++) {
				waypoint1 = UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].get(j);
				if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				GUIHelper.exit(Text.UAVS_START_ERROR_6 + " " + Param.id[SwarmProtParam.posMaster]);
			}
			waypointFound = false;
			for (int j = waypoint1pos + 1; j < UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].size()
					&& !waypointFound; j++) {
				waypoint2 = UAVParam.missionGeoLoaded[SwarmProtParam.posMaster].get(j);
				if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				heading = masterHeading(waypoint1, waypoint2);
				SwarmProtParam.missionHeading = heading;
			}
		} else {
			JOptionPane.showMessageDialog(null,
					Text.APP_NAME + " " + Text.UAVS_START_ERROR_5 + " " + (SwarmProtParam.posMaster + 1) + ".",
					Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		GeoCoordinates[] startingLocations = posSlaveFromMaster(heading, waypoint1, Param.numUAVs - 1,
				SwarmProtParam.initialDistanceBetweenUAV);
		Pair<GeoCoordinates, Double>[] startingLocationsFinal = new Pair[Param.numUAVs];
		int aux = 0;
		int destAux = 0;
		while (aux < SwarmProtParam.posMaster) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], heading);
			aux++;
			destAux++;
		}

		startingLocationsFinal[destAux] = Pair
				.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
		destAux++;
		while (aux < startingLocations.length) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], heading);
			aux++;
			destAux++;
		}

		return startingLocationsFinal;
	}

	public static void startSwarmTestActionPerformedV1() {
		for (int i = 0; i < Param.numUAVs; i++) {
			/** If you drone is master */
			if (Param.id[i] == SwarmProtParam.idMaster) {

				/** We create a list with the points of the mission */
				List<WaypointSimplified> listOfPoints = UAVParam.missionUTMSimplified.get(SwarmProtParam.posMaster);

				for (int j = 1; j < listOfPoints.size(); j++) {
					/** We obtain each of those points that are given in X and Y */
					WaypointSimplified pointMasterxy = listOfPoints.get(j);

					/** They are converted to Geometrical coordinates */
					GeoCoordinates pointMasterGEO = GUIHelper.UTMToGeo(pointMasterxy.x, pointMasterxy.y);

					API.moveUAV(SwarmProtParam.posMaster, pointMasterGEO, (float) pointMasterxy.z, SwarmProtParam.distToAcceptPointReached, 0.05);
				}

			}

		}
	}

	/** Send a packet to Master */
	public static void sendDataToMaster(DatagramPacket packet, DatagramSocket socket) throws IOException {

		/** The real drone has different IP and sends without problems */
		if (Param.IS_REAL_UAV) {
			socket.send(packet);
		}
		/**
		 * For simulation we need to set port of master
		 */
		else {
			packet.setPort(SwarmProtParam.port);
			socket.send(packet);
		}
	}

	/** Send a packet to all Slaves */
	public static void sendDataToSlaves(DatagramPacket packet, DatagramSocket socket) throws IOException {
		/** The real drone has different IP and sends without problems */
		if (Param.IS_REAL_UAV) {
			socket.send(packet);
		}
		/**
		 * For simulation we need to set port of each slave
		 */
		else {
			for (int i = 0; i < Param.numUAVs; i++) {
				if (i != SwarmProtParam.posMaster) {
					packet.setPort(SwarmProtParam.port + i);
					socket.send(packet);
				}
			}
		}

	}
	
	/** Manually takes off all the UAVs: executes mode=guided, arm engines, and then the take off. Blocking method.
	 * <p>Usefull when starting on the ground, on stabilize flight mode
	 * <p>Do not modify this method. */
	public static void takeOffIndividual(int numUAV) {
			//Taking Off to first altitude step
			UAVParam.takeOffAltitude[numUAV] = Listener.takeOffAltitudeStepOne;
			if (!API.setMode(numUAV, UAVParam.Mode.GUIDED) || !API.armEngines(numUAV) || !API.doTakeOff(numUAV)) {
				GUIHelper.exit(Text.TAKE_OFF_ERROR_1 + " " + Param.id[numUAV]);
			}

		// The application must wait until all UAVs reach the planned altitude
			while (UAVParam.uavCurrentData[numUAV].getZRelative() < 0.95 * UAVParam.takeOffAltitude[numUAV]) {
				if (Param.VERBOSE_LOGGING) {
					MissionHelper.log(SimParam.prefix[numUAV] + Text.ALTITUDE_TEXT
							+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ())
							+ " " + Text.METERS);
				}
				GUIHelper.waiting(UAVParam.ALTITUDE_WAIT);
			}
		}
	
	
	/** Calculates the master heading */
	public static double masterHeading(Waypoint wp1, Waypoint wp2) {
		// We only can set a heading if at least two points with valid coordinates are
		// found
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		double heading = 0.0;
		p1UTM = GUIHelper.geoToUTM(wp1.getLatitude(), wp1.getLongitude());
		p2UTM = GUIHelper.geoToUTM(wp2.getLatitude(), wp2.getLongitude());
		incX = p2UTM.Easting - p1UTM.Easting;
		incY = p2UTM.Northing - p1UTM.Northing;
		if (incX != 0 || incY != 0) {
			if (incX == 0) {
				if (incY > 0)
					heading = 0.0;
				else
					heading = 180.0;
			} else if (incY == 0) {
				if (incX > 0)
					heading = 89.9;
				else
					heading = 270.0;
			} else {
				double gamma = Math.atan(incY / incX);
				if (incX > 0)
					heading = 90 - gamma * 180 / Math.PI;
				else
					heading = 270.0 - gamma * 180 / Math.PI;
			}
		}
		return heading;
	}

	/** Position the slaves on the map according to master position */
	public static Point2D.Double[] posSlaveFromMasterUTM(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		UTMCoordinates initialPoint = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		return posSlaveFromMasterLinear(heading, new Point2D.Double(initialPoint.Easting, initialPoint.Northing),
				numOfUAVs, initialDistanceBetweenUAV);

	}

	// Var initialDistanceBetweenUAV simulation mode distance between UAV
	/**
	 * Position the slaves on the map according to master position forming a
	 * straight line perpendicular to the master heading.
	 */
	public static Point2D.Double[] posSlaveFromMasterLinear(double heading, Point2D.Double initialPoint, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] startingLocations = new Point2D.Double[numOfUAVs];

		SwarmProtParam.masterHeading = heading;
		Point2D.Double destPointP = new Point2D.Double(initialPoint.x, initialPoint.y);
		Point2D.Double destPointI = new Point2D.Double(initialPoint.x, initialPoint.y);

		for (int i = 0; i < numOfUAVs; i++) {
			if (i % 2 == 0) {
				destPointP.x = destPointP.x
						+ initialDistanceBetweenUAV * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
						+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180);
				destPointP.y = destPointP.y + 0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180);
				startingLocations[i] = new Point2D.Double(destPointP.x, destPointP.y);

			} else {
				destPointI.x = destPointI.x
						- (initialDistanceBetweenUAV * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
								+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));
				destPointI.y = destPointI.y - (0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));
				startingLocations[i] = new Point2D.Double(destPointI.x, destPointI.y);
			}

		}
		return startingLocations;
	}

	// Return position of UAVs from master with GEO
	public static GeoCoordinates[] posSlaveFromMaster(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] aux = posSlaveFromMasterUTM(heading, waypoint1, numOfUAVs, initialDistanceBetweenUAV);
		GeoCoordinates[] result = new GeoCoordinates[aux.length];
		for (int i = 0; i < aux.length; i++) {
			result[i] = GUIHelper.UTMToGeo(aux[i].x, aux[i].y);
		}
		return result;
	}

	
	public static void openSwarmConfigurationDialog() {
		/** Si pongo una pantalla de preconfiguración, se hace aqui TODO*/

		/** We load the file directly with the mission. In a future add menu */
		List<Waypoint> mission = Tools.loadMission(GUIHelper.getCurrentFolder());

		if (mission != null) {
			/** The master is assigned the first mission in the list */
			UAVParam.missionGeoLoaded = new ArrayList[Param.numUAVs];
			UAVParam.missionGeoLoaded[SwarmProtParam.posMaster] = mission;

		} else {
			JOptionPane.showMessageDialog(null, Text.MISSIONS_ERROR_3, Text.MISSIONS_SELECTION_ERROR,
					JOptionPane.WARNING_MESSAGE);

			return;
		}
		SwarmProtParam.idMaster = SwarmProtParam.idMasterSimulation;

	}
	
	public static boolean loadMission() {
		// Only the master UAV has a mission
		boolean b = false;
		/** You get the id = MAC for real drone */
		for (int i = 0; i < SwarmProtParam.MACId.length; i++) {
			if (SwarmProtParam.MACId[i] == Param.id[SwarmProtParam.posMaster]) {
				SwarmProtParam.idMaster = Param.id[SwarmProtParam.posMaster];
				return b = true;
			}
		}
		return b;
	}
	
	public static boolean sendBasicSwarmConfig(int numUAV) {
		boolean success = true;
		if (Param.id[numUAV] == SwarmProtParam.idMaster) {
			success = false;
			// Erases, sends and retrieves the planned mission. Blocking procedure
			if (API.clearMission(numUAV) && API.sendMission(numUAV, UAVParam.missionGeoLoaded[numUAV])
					&& API.getMission(numUAV) && API.setCurrentWaypoint(numUAV, 0)) {
				MissionHelper.simplifyMission(numUAV);
				Param.numMissionUAVs.incrementAndGet();
				if (!Param.IS_REAL_UAV) {
					BoardParam.rescaleQueries.incrementAndGet();
				}
				success = true;
			}
		}

		return success;
	}

}
