package swarmprot.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JOptionPane;

import org.javatuples.Pair;

import com.esotericsoftware.kryo.io.Input;

import api.API;
import api.GUIHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Text;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPParam.MBCAPState;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class SwarmProtHelper {
	/** Initialize Swarm Protocol Data Structures */
	public static void initializeProtocolDataStructures() {
		SwarmProtParam.masterHeading = 0.0;
		SwarmProtParam.state = new SwarmProtState[Param.numUAVs];

		/** The state machine starts with the status "START" */
		for (int i = 0; i < Param.numUAVs; i++) {
			SwarmProtParam.state[i] = SwarmProtState.START;
		}

	}

	/** Launch Listener and Talker threads of each UAV */
	public static void startSwarmThreads() throws SocketException, UnknownHostException {
		for (int i = 0; i < Param.numUAVs; i++) {
			(new Listener(i)).start();
			(new Talker(i)).start();
		}
		SwarmHelper.log(SwarmProtText.ENABLING);
	}

	/** Get the position on the map of the drones, in simulation mode */
	public static Pair<GeoCoordinates, Double>[] getSwarmStartingLocationV1() {
		/** Position the master on the map */
		Pair<GeoCoordinates, Double>[] startingLocations = new Pair[Param.numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;

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
				// We only can set a heading if at least two points with valid coordinates are
				// found
				p1UTM = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
				p2UTM = GUIHelper.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
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
			}
		} else {
			JOptionPane.showMessageDialog(null,
					Text.APP_NAME + " " + Text.UAVS_START_ERROR_5 + " " + (SwarmProtParam.posMaster + 1) + ".",
					Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		startingLocations[SwarmProtParam.posMaster] = Pair
				.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);

		/** Position the slaves on the map according to master position */
		SwarmProtParam.masterHeading = heading;
		UTMCoordinates initialPoint = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		UTMCoordinates destPointP = new UTMCoordinates(initialPoint.Easting, initialPoint.Northing, initialPoint.Zone,
				initialPoint.Letter);
		UTMCoordinates destPointI = new UTMCoordinates(initialPoint.Easting, initialPoint.Northing, initialPoint.Zone,
				initialPoint.Letter);
		GeoCoordinates destPointFinal;

		for (int i = 0; i < Param.numUAVs; i++) {
			if (i != SwarmProtParam.posMaster) {
				if (i % 2 == 0) {
					destPointP.Easting = SwarmProtParam.initialDistanceBetweenUAV
							* Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180) + destPointP.Easting;
					destPointP.Northing = 0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							- SwarmProtParam.initialDistanceBetweenUAV
									* Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ destPointP.Northing;

					destPointFinal = GUIHelper.UTMToGeo(destPointP.Easting, destPointP.Northing);
					startingLocations[i] = Pair.with(
							new GeoCoordinates(destPointFinal.latitude, destPointFinal.longitude),
							SwarmProtParam.masterHeading);
				} else {
					destPointI.Easting = destPointI.Easting - (SwarmProtParam.initialDistanceBetweenUAV
							* Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
							+ 0 * Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));
					destPointI.Northing = destPointI.Northing
							- (0 * Math.cos((SwarmProtParam.masterHeading * Math.PI) / 180)
									- SwarmProtParam.initialDistanceBetweenUAV
											* Math.sin((SwarmProtParam.masterHeading * Math.PI) / 180));

					destPointFinal = GUIHelper.UTMToGeo(destPointI.Easting, destPointI.Northing);
					startingLocations[i] = Pair.with(
							new GeoCoordinates(destPointFinal.latitude, destPointFinal.longitude),
							SwarmProtParam.masterHeading);
				}
			}
		}

		return startingLocations;
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

					API.moveUAV(SwarmProtParam.posMaster, pointMasterGEO, (float) pointMasterxy.z);
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

	/** Send a packete to all Slaves */
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

}
