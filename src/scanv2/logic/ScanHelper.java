package scanv2.logic;

import static scanv2.pojo.State.SETUP_FINISHED;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.AtomicDoubleArray;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.formations.FlightFormation;
import scanv2.gui.ScanConfigDialog;
import sim.board.BoardPanel;

/** Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class ScanHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = ScanText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		// Only the master UAV has a mission (on real UAV, numUAV == 0)
		boolean isMaster = ScanHelper.isMaster(0);
		if (isMaster) {
			ScanParam.idMaster = Tools.getIdFromPos(ScanParam.MASTER_POSITION);
		}//	The slave in a real UAV has SwarmProtParam.idMaster == null
		return isMaster;
	}

	@Override
	public void openConfigurationDialog() {
		ScanParam.idMaster = ScanParam.SIMULATION_MASTER_ID;

		new ScanConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = Tools.getNumUAVs();
		
		ScanParam.iAmCenter = new AtomicBoolean[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			ScanParam.iAmCenter[i] = new AtomicBoolean();
		}
		ScanParam.idPrev = new AtomicLongArray(numUAVs);
		ScanParam.idNext = new AtomicLongArray(numUAVs);
		ScanParam.numUAVs = new AtomicIntegerArray(numUAVs);
		ScanParam.flyingFormation = new AtomicReferenceArray<>(numUAVs);
		ScanParam.flyingFormationPosition = new AtomicIntegerArray(numUAVs);
		ScanParam.flyingFormationHeading = new AtomicDoubleArray(numUAVs);
		ScanParam.takeoffAltitude = new AtomicDoubleArray(numUAVs);
		ScanParam.uavMissionReceivedUTM = new AtomicReferenceArray<>(numUAVs);
		ScanParam.uavMissionReceivedGeo = new AtomicReferenceArray<>(numUAVs);
		ScanParam.data = new AtomicReference<>();
		
		ScanParam.state = new AtomicIntegerArray(numUAVs);
		ScanParam.moveSemaphore = new AtomicIntegerArray(numUAVs);
		ScanParam.wpReachedSemaphore = new AtomicIntegerArray(numUAVs);
	}

	@Override
	public String setInitialState() {
		return ScanText.START;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		// 1. Get the heading of the master given the current mission
		//    We only can set a heading if at least two points with valid coordinates are found
		// 1.1. Check that master mission exists
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		List<Waypoint>[] missions = Tools.getLoadedMissions();	// Simplified mission not ready
		if (missions == null || missions[ScanParam.MASTER_POSITION] == null) {
			JOptionPane.showMessageDialog(null, ScanText.UAVS_START_ERROR_1 + " " + (ScanParam.MASTER_POSITION + 1) + ".",
					ScanText.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		// 1.2. Get the heading
		// Locate the first waypoint with coordinates
		List<Waypoint> masterMission = missions[ScanParam.MASTER_POSITION];
		waypointFound = false;
		for (int j = 0; j < masterMission.size() && !waypointFound; j++) {
			waypoint1 = masterMission.get(j);
			if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
				waypoint1pos = j;
				waypointFound = true;
			}
		}
		if (!waypointFound) {
			GUI.exit(ScanText.UAVS_START_ERROR_2 + " " + Tools.getIdFromPos(ScanParam.MASTER_POSITION));
		}
		// Locate the second waypoint with coordinates
		waypointFound = false;
		for (int j = waypoint1pos + 1; j < masterMission.size()
				&& !waypointFound; j++) {
			waypoint2 = masterMission.get(j);
			if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
				waypointFound = true;
			}
		}
		// Get the heading
		UTMCoordinates groundCenterUTMLocation = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		if (waypointFound) {
			UTMCoordinates wp2 = Tools.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
			ScanParam.formationHeading = ScanHelper.getHeading(groundCenterUTMLocation, wp2);
		} else {
			ScanParam.formationHeading = 0.0;
		}
		
		// 2. With the ground formation centered on the mission beginning, get ground coordinates
		//   The UAVs appear with the center UAV in the first waypoint of the initial mission
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = Tools.getNumUAVs();
		FlightFormation groundFormation = FlightFormation.getFormation(FlightFormation.getGroundFormation(),
				numUAVs, FlightFormation.getGroundFormationDistance());
		int groundCenterUAVPosition = groundFormation.getCenterUAVPosition();
		Map<Long, UTMCoordinates> groundLocations = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		UTMCoordinates locationUTM;
		for (int i = 0; i < numUAVs; i++) {
			if (i == groundCenterUAVPosition) {
				groundLocations.put((long)i, groundCenterUTMLocation);
			} else {
				locationUTM = groundFormation.getLocation(i, groundCenterUTMLocation, ScanParam.formationHeading);
				groundLocations.put((long)i, locationUTM);
			}
		}
		
		// 3. Get the ID of the UAV that will be center in the flight formation
		FlightFormation airFormation = FlightFormation.getFormation(FlightFormation.getFlyingFormation(),
				numUAVs, FlightFormation.getFlyingFormationDistance());
		Triplet<Integer, Long, UTMCoordinates>[] match = FlightFormation.matchIDs(groundLocations, ScanParam.formationHeading,
				true, null, airFormation);
		int airCenterUAVPosition = airFormation.getCenterUAVPosition();
		Integer airCenterUAVId = null;
		Triplet<Integer, Long, UTMCoordinates> triplet = null;
		for (int i = 0; i < numUAVs && airCenterUAVId == null; i++) {
			triplet = match[i];
			if (triplet.getValue0() == airCenterUAVPosition) {
				airCenterUAVId = triplet.getValue1().intValue();
			}
		}
		
		// 4. Copy the mission to the location where the UAV in the center of the flight formation will be
		List<Waypoint> centerMission = new ArrayList<>(masterMission.size());
		int p = -1;
		for (int i = 0; i < masterMission.size(); i++) {
			centerMission.add(i, masterMission.get(i).clone());
			if (masterMission.get(i).getNumSeq() == waypoint1.getNumSeq()) {
				p = i;
			}
		}
		Waypoint moved = centerMission.get(p);
		GeoCoordinates movedGeo = Tools.UTMToGeo(triplet.getValue2());
		moved.setLatitude(movedGeo.latitude);
		moved.setLongitude(movedGeo.longitude);
		missions[airCenterUAVId] = centerMission;
		Tools.setLoadedMissionsFromFile(missions);
		
		// 5. Using the ground center UAV location as reference, get the starting location of all the UAVs
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocation = new Pair[numUAVs];
		double heading = ScanParam.formationHeading * 180 / Math.PI;
		for (int i = 0; i < numUAVs; i++) {
			if (i == groundCenterUAVPosition) {
				startingLocation[i] = Pair.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
			} else {
				startingLocation[i] = Pair.with(Tools.UTMToGeo(groundLocations.get((long)i)), heading);
			}
		}

		return startingLocation;
	}

	/** Calculates the heading (rad) from p1 to p2. */
	private static double getHeading(UTMCoordinates p1, UTMCoordinates p2) {
		double incX, incY;
		double heading = 0.0;
		incX = p2.x - p1.x;
		incY = p2.y - p1.y;
		if (incX != 0 || incY != 0) {
			if (incX == 0) {
				if (incY > 0)
					heading = 0.0;
				else
					heading = Math.PI;			// 180º
			} else if (incY == 0) {
				if (incX > 0)
					heading = Math.PI / 2;// 90º
				else
					heading = 3 * Math.PI / 2;	// 270º
			} else {
				if (incX > 0)
					heading = Math.PI / 2 - Math.atan(incY / incX);
				else
					heading = 3 * Math.PI / 2 - Math.atan(incY / incX);
			}
		}
		return heading;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// No need of specific configuration, as the mission is automatically loaded
		return true;
	}

	@Override
	public void startThreads() {
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			(new ListenerThread(i)).start();
			(new TalkerThread(i)).start();
		}
		GUI.log(ScanText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		int numUAVs = Tools.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (ScanParam.state.get(i) < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		//TODO
	}
	
	/** Asserts if the UAV is master. */
	public static boolean isMaster(int numUAV) {
		boolean b = false;
		int role = Tools.getArduSimRole();
		if (role == Tools.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = Tools.getIdFromPos(ScanParam.MASTER_POSITION);
			for (int i = 0; i < ScanParam.MAC_ID.length; i++) {
				if (ScanParam.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == Tools.SIMULATOR) {
			if (numUAV == ScanParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}

}
