package swarmprot.logic;

import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import org.javatuples.Pair;

import api.GUIHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import main.Param;
import main.Text;
import mbcap.logic.MBCAPParam;
import mbcap.logic.MBCAPParam.MBCAPState;
import swarmprot.logic.SwarmProtParam.SwarmProtState;
import uavController.UAVParam;

public class SwarmProtHelper {
	/**Initialize Swarm Protocol Data Structures*/
	public static void initializeProtocolDataStructures() {
		SwarmProtParam.masterHeading = 0.0;
		UAVParam.newLocation = new float[Param.numUAVs][2];
		SwarmProtParam.state = new SwarmProtState[Param.numUAVs];
		
		for(int i = 0; i<Param.numUAVs;i++) {
			SwarmProtParam.state[i] = SwarmProtState.START;
		}
		

	}

	/**Launch Listener and Talker threads of each UAV*/
	public static void startSwarmThreads() throws SocketException, UnknownHostException {
		// cuando sea real se tendra que hacer un hilo de lectura y escritura en cada
		// dron
		for (int i = 0; i < Param.numUAVs; i++) {
			(new Listener(i)).start();
			(new Talker(i)).start();
		}
		SwarmHelper.log(SwarmProtText.ENABLING);
	}

	
	/**Get the position on the map of the drones, in simulation mode*/
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
}
