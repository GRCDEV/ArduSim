package scanv1.logic;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.javatuples.Pair;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import scanv1.gui.Scanv1ConfigDialog;
//import scanprotocolv1.gui.Scanv1ConfigDialog;
import scanv1.logic.Scanv1ProtParam.SwarmProtState;
import sim.board.BoardPanel;

public class Scanv1ProtHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = Scanv1ProtText.PROTOCOL_TEXT;
	}
	
	@Override
	public boolean loadMission() {
		// Only the master UAV has a mission
		boolean b = false;
		/** You get the id = MAC for real drone */
		long idMaster;
		for (int i = 0; i < Scanv1ProtParam.MACId.length; i++) {
			idMaster = Tools.getIdFromPos(Scanv1ProtParam.posMaster);
			if (Scanv1ProtParam.MACId[i] == idMaster) {
				Scanv1ProtParam.idMaster = idMaster;
				return b = true;
			}
		}
		return b;
	}

	@Override
	public void openConfigurationDialog() {
		Scanv1ProtParam.idMaster = Scanv1ProtParam.idMasterSimulation;
		Scanv1ConfigDialog dialog = new Scanv1ConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		Scanv1ProtParam.masterHeading = 0.0;
		int numUAVs = Tools.getNumUAVs();
		Scanv1ProtParam.state = new SwarmProtState[numUAVs];
		//UTM
		Scanv1ProtParam.flightListPersonalized = new Point3D[numUAVs][];
		Scanv1ProtParam.flightListPersonalizedAlt = new Double[numUAVs][];
		//GEO
		Scanv1ProtParam.flightListPersonalizedGeo = new GeoCoordinates[numUAVs][];
		Scanv1ProtParam.flightListPersonalizedAltGeo = new Double[numUAVs][];
		
		//Drones with his prev and next for takeoff
		Scanv1ProtParam.fightPrevNext = new long[numUAVs][2];
		
		//Inicialización vector detección ultimo WP
		Scanv1ProtParam.WpLast = new boolean[numUAVs][1];

		Scanv1ProtParam.setupFinished = new AtomicIntegerArray(numUAVs);

		/** The state machine starts with the status "START" */
		for (int i = 0; i < numUAVs; i++) {
			Scanv1ProtParam.state[i] = SwarmProtState.START;
		}
	}

	@Override
	public String setInitialState() {
		return "INICIO";
	}

	@Override
	public void rescaleDataStructures() {
		// TODO 
	}

	@Override
	public void loadResources() {
		// TODO 
	}
	
	@Override
	public void rescaleShownResources() {
		// TODO 
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		// TODO 
	}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		/** Position the master on the map */
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		List<Waypoint>[] missions = Tools.getLoadedMissions();
		if (missions != null && missions[Scanv1ProtParam.posMaster] != null) {
			waypointFound = false;
			for (int j = 0; j < missions[Scanv1ProtParam.posMaster].size() && !waypointFound; j++) {
				waypoint1 = missions[Scanv1ProtParam.posMaster].get(j);
				if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				GUI.exit(Scanv1ProtText.UAVS_START_ERROR_2 + " " + Tools.getIdFromPos(Scanv1ProtParam.posMaster));
			}
			waypointFound = false;
			for (int j = waypoint1pos + 1; j < missions[Scanv1ProtParam.posMaster].size()
					&& !waypointFound; j++) {
				waypoint2 = missions[Scanv1ProtParam.posMaster].get(j);
				if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				heading = masterHeading(waypoint1, waypoint2);
				Scanv1ProtParam.missionHeading = heading;
			}
		} else {
			JOptionPane.showMessageDialog(null,
					Scanv1ProtText.APP_NAME + " " + Scanv1ProtText.UAVS_START_ERROR_1 + " " + (Scanv1ProtParam.posMaster + 1) + ".",
					Scanv1ProtText.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		int numUAVs = Tools.getNumUAVs();
		GeoCoordinates[] startingLocations = posSlaveFromMaster(heading, waypoint1, numUAVs - 1,
				Scanv1ProtParam.initialDistanceBetweenUAV);
		Pair<GeoCoordinates, Double>[] startingLocationsFinal = new Pair[numUAVs];
		int aux = 0;
		int destAux = 0;
		while (aux < Scanv1ProtParam.posMaster) {
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

	/** Calculates the master heading */
	private static double masterHeading(Waypoint wp1, Waypoint wp2) {
		// We only can set a heading if at least two points with valid coordinates are
		// found
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		double heading = 0.0;
		p1UTM = Tools.geoToUTM(wp1.getLatitude(), wp1.getLongitude());
		p2UTM = Tools.geoToUTM(wp2.getLatitude(), wp2.getLongitude());
		incX = p2UTM.x - p1UTM.x;
		incY = p2UTM.y - p1UTM.y;
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
	
	// Return position of UAVs from master with GEO
	private static GeoCoordinates[] posSlaveFromMaster(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] aux = posSlaveFromMasterUTM(heading, waypoint1, numOfUAVs, initialDistanceBetweenUAV);
		GeoCoordinates[] result = new GeoCoordinates[aux.length];
		for (int i = 0; i < aux.length; i++) {
			result[i] = Tools.UTMToGeo(aux[i].x, aux[i].y);
		}
		return result;
	}
	
	/** Position the slaves on the map according to master position */
	private static Point2D.Double[] posSlaveFromMasterUTM(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		UTMCoordinates initialPoint = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		return posSlaveFromMasterLinear(heading, new Point2D.Double(initialPoint.x, initialPoint.y),
				numOfUAVs, initialDistanceBetweenUAV);

	}
	
	/**
	 * Position the slaves on the map according to master position forming a
	 * straight line perpendicular to the master heading.
	 */
	protected static Point2D.Double[] posSlaveFromMasterLinear(double heading, Point2D.Double initialPoint, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] startingLocations = new Point2D.Double[numOfUAVs];

		Scanv1ProtParam.masterHeading = heading;
		Point2D.Double destPointP = new Point2D.Double(initialPoint.x, initialPoint.y);
		Point2D.Double destPointI = new Point2D.Double(initialPoint.x, initialPoint.y);

		for (int i = 0; i < numOfUAVs; i++) {
			if (i % 2 == 0) {
				destPointP.x = destPointP.x
						+ initialDistanceBetweenUAV * Math.cos((Scanv1ProtParam.masterHeading * Math.PI) / 180)
						+ 0 * Math.sin((Scanv1ProtParam.masterHeading * Math.PI) / 180);
				destPointP.y = destPointP.y + 0 * Math.cos((Scanv1ProtParam.masterHeading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((Scanv1ProtParam.masterHeading * Math.PI) / 180);
				startingLocations[i] = new Point2D.Double(destPointP.x, destPointP.y);

			} else {
				destPointI.x = destPointI.x
						- (initialDistanceBetweenUAV * Math.cos((Scanv1ProtParam.masterHeading * Math.PI) / 180)
								+ 0 * Math.sin((Scanv1ProtParam.masterHeading * Math.PI) / 180));
				destPointI.y = destPointI.y - (0 * Math.cos((Scanv1ProtParam.masterHeading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((Scanv1ProtParam.masterHeading * Math.PI) / 180));
				startingLocations[i] = new Point2D.Double(destPointI.x, destPointI.y);
			}

		}
		return startingLocations;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// No special action required (master mission is automatically loaded)
		return true;
	}

	@Override
	public void startThreads() {
		try {
			int numUAVs = Tools.getNumUAVs();
			for (int i = 0; i < numUAVs; i++) {
				(new Scanv1Listener(i)).start();
				(new Scanv1Talker(i)).start();
			}
			GUI.log(Scanv1ProtText.ENABLING);
		}catch(SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setupActionPerformed() {
		boolean finish;
		int numUAVs = Tools.getNumUAVs();
		while(true) {
			finish = true;
			for (int i = 0; i < numUAVs && finish; i++) {
				if (Scanv1ProtParam.setupFinished.get(i) == 0) {
					finish = false;
				}
			}
			if (finish) {
				break;
			} else {
				Tools.waiting(Scanv1ProtParam.waitState);
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {
//		for (int i = 0; i < Param.numUAVs; i++) {//TODO integrar con una variable a la que se esperen los hilos que sea necesario
//			/** If you drone is master */
//			if (Param.id[i] == Scanv1ProtParam.idMaster) {
//
//				/** We create a list with the points of the mission */
//				List<WaypointSimplified> listOfPoints = Tools.getUAVMissionSimplified(Scanv1ProtParam.posMaster);
//
//				for (int j = 1; j < listOfPoints.size(); j++) {
//					/** We obtain each of those points that are given in X and Y */
//					WaypointSimplified pointMasterxy = listOfPoints.get(j);
//
//					/** They are converted to Geometrical coordinates */
//					GeoCoordinates pointMasterGEO = Tools.UTMToGeo(pointMasterxy.x, pointMasterxy.y);
//
//					Copter.moveUAV(Scanv1ProtParam.posMaster, pointMasterGEO, (float) pointMasterxy.z, Scanv1ProtParam.distToAcceptPointReached, 0.05);
//				}
//
//			}
//
//		}
	}

	@Override
	public void forceExperimentEnd() {
		
	}

	@Override
	public String getExperimentResults() {
		// TODO 
		return null;
	}
	
	@Override
	public String getExperimentConfiguration() {
		// TODO 
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO 
	}
	
	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO 
	}

	/** Send a packet to all Slaves */
	public static void sendDataToSlaves(DatagramPacket packet, DatagramSocket socket) throws IOException {
		/** The real drone has different IP and sends without problems */
		if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
			socket.send(packet);
		}
		/**
		 * For simulation we need to set port of each slave
		 */
		else {
			int numUAVs = Tools.getNumUAVs();
			for (int i = 0; i < numUAVs; i++) {
				if (i != Scanv1ProtParam.posMaster) {
					packet.setPort(Scanv1ProtParam.port + i);
					socket.send(packet);
				}
			}
		}
	
	}

	/** Send a packet to Master */
	public static void sendDataToMaster(DatagramPacket packet, DatagramSocket socket) throws IOException {
	
		/** The real drone has different IP and sends without problems */
		if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
			socket.send(packet);
		}
		/**
		 * For simulation we need to set port of master
		 */
		else {
			packet.setPort(Scanv1ProtParam.port);
			socket.send(packet);
		}
	}

	
	

}
