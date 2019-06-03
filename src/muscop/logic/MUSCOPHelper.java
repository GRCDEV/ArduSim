package muscop.logic;

import static muscop.pojo.State.SETUP_FINISHED;

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

import api.API;
import api.ProtocolHelper;
import api.pojo.AtomicDoubleArray;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Waypoint;
import main.api.ArduSim;
import main.api.ArduSimNotReadyException;
import main.api.MissionHelper;
import main.api.formations.FlightFormation;
import main.sim.board.BoardPanel;
import muscop.gui.MUSCOPConfigDialog;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class MUSCOPHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = MUSCOPText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		return MUSCOPHelper.isMaster(0);
	}

	@Override
	public void openConfigurationDialog() {
		new MUSCOPConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = API.getArduSim().getNumUAVs();
		
		MUSCOPParam.iAmCenter = new AtomicBoolean[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			MUSCOPParam.iAmCenter[i] = new AtomicBoolean();
		}
		MUSCOPParam.idPrev = new AtomicLongArray(numUAVs);
		MUSCOPParam.idNext = new AtomicLongArray(numUAVs);
		MUSCOPParam.numUAVs = new AtomicIntegerArray(numUAVs);
		MUSCOPParam.flyingFormation = new AtomicReferenceArray<>(numUAVs);
		MUSCOPParam.flyingFormationPosition = new AtomicIntegerArray(numUAVs);
		MUSCOPParam.flyingFormationHeading = new AtomicDoubleArray(numUAVs);
		MUSCOPParam.takeoffAltitude = new AtomicDoubleArray(numUAVs);
		MUSCOPParam.uavMissionReceivedUTM = new AtomicReferenceArray<>(numUAVs);
		MUSCOPParam.uavMissionReceivedGeo = new AtomicReferenceArray<>(numUAVs);
		MUSCOPParam.data = new AtomicReference<>();
		
		MUSCOPParam.state = new AtomicIntegerArray(numUAVs);	// Implicit value State.START, as it is equals to 0
		MUSCOPParam.moveSemaphore = new AtomicIntegerArray(numUAVs);
		MUSCOPParam.wpReachedSemaphore = new AtomicIntegerArray(numUAVs);
	}

	@Override
	public String setInitialState() {
		return MUSCOPText.START;
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
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		// 1. Get the heading of the master given the current mission
		//    We only can set a heading if at least two points with valid coordinates are found
		// 1.1. Check that master mission exists
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
		List<Waypoint>[] missions = missionHelper.getMissionsLoaded();	// Simplified mission not ready
		if (missions == null || missions[MUSCOPParam.MASTER_POSITION] == null) {
			JOptionPane.showMessageDialog(null, MUSCOPText.UAVS_START_ERROR_1 + " " + (MUSCOPParam.MASTER_POSITION + 1) + ".",
					MUSCOPText.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		// 1.2. Get the heading
		// Locate the first waypoint with coordinates
		List<Waypoint> masterMission = missions[MUSCOPParam.MASTER_POSITION];
		waypointFound = false;
		for (int j = 0; j < masterMission.size() && !waypointFound; j++) {
			waypoint1 = masterMission.get(j);
			if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
				waypoint1pos = j;
				waypointFound = true;
			}
		}
		if (!waypointFound) {
			API.getGUI(0).exit(MUSCOPText.UAVS_START_ERROR_2 + " " + API.getCopter(MUSCOPParam.MASTER_POSITION).getID());
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
		Location2DUTM groundCenterUTMLocation = waypoint1.getUTM();
		if (waypointFound) {
			Location2DUTM wp2 = waypoint2.getUTM();
			MUSCOPParam.formationHeading = MUSCOPHelper.getHeading(groundCenterUTMLocation, wp2);
		} else {
			MUSCOPParam.formationHeading = 0.0;
		}
		
		// 2. With the ground formation centered on the mission beginning, get ground coordinates
		//   The UAVs appear with the center UAV in the first waypoint of the initial mission
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = API.getArduSim().getNumUAVs();
		FlightFormation groundFormation = FlightFormation.getFormation(FlightFormation.getGroundFormation(),
				numUAVs, FlightFormation.getGroundFormationDistance());
		int groundCenterUAVPosition = groundFormation.getCenterUAVPosition();
		Map<Long, Location2DUTM> groundLocations = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		Location2DUTM locationUTM;
		for (int i = 0; i < numUAVs; i++) {
			if (i == groundCenterUAVPosition) {
				groundLocations.put((long)i, groundCenterUTMLocation);
			} else {
				locationUTM = groundFormation.getLocation(i, groundCenterUTMLocation, MUSCOPParam.formationHeading);
				groundLocations.put((long)i, locationUTM);
			}
		}
		
		// 3. Get the ID of the UAV that will be center in the flight formation
		FlightFormation airFormation = FlightFormation.getFormation(FlightFormation.getFlyingFormation(),
				numUAVs, FlightFormation.getFlyingFormationDistance());
		Triplet<Integer, Long, Location2DUTM>[] match = FlightFormation.matchIDs(groundLocations, MUSCOPParam.formationHeading,
				true, null, airFormation);
		int airCenterUAVPosition = airFormation.getCenterUAVPosition();
		Integer airCenterUAVId = null;
		Triplet<Integer, Long, Location2DUTM> triplet = null;
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
			centerMission.add(i, new Waypoint(masterMission.get(i)));
			if (masterMission.get(i).getNumSeq() == waypoint1.getNumSeq()) {
				p = i;
			}
		}
		Waypoint moved = centerMission.get(p);
		Location2DGeo movedGeo;
		try {
			movedGeo = triplet.getValue2().getGeo();
			moved.setLatitude(movedGeo.latitude);
			moved.setLongitude(movedGeo.longitude);
			missions[airCenterUAVId] = centerMission;
			missionHelper.setMissionsLoaded(missions);
			
			// 5. Using the ground center UAV location as reference, get the starting location of all the UAVs
			@SuppressWarnings("unchecked")
			Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
			double heading = MUSCOPParam.formationHeading * 180 / Math.PI;
			for (int i = 0; i < numUAVs; i++) {
				if (i == groundCenterUAVPosition) {
					startingLocation[i] = Pair.with(new Location2DGeo(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
				} else {
					startingLocation[i] = Pair.with(groundLocations.get((long)i).getGeo(), heading);
				}
			}
			return startingLocation;
		} catch (ArduSimNotReadyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			API.getGUI(0).exit(e.getMessage());
		}
		return null;
	}

	/** Calculates the heading (rad) from p1 to p2. */
	private static double getHeading(Location2DUTM p1, Location2DUTM p2) {
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
		int numUAVs = API.getArduSim().getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			(new ListenerThread(i)).start();
			(new TalkerThread(i)).start();
		}
		API.getGUI(0).log(MUSCOPText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		ArduSim ardusim = API.getArduSim();
		int numUAVs = ardusim.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (MUSCOPParam.state.get(i) < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {
		// All the tasks are performed in the protocol threads
	}

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
		int role = API.getArduSim().getArduSimRole();
		if (role == ArduSim.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = API.getCopter(MUSCOPParam.MASTER_POSITION).getID();
			for (int i = 0; i < MUSCOPParam.MAC_ID.length; i++) {
				if (MUSCOPParam.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == ArduSim.SIMULATOR) {
			if (numUAV == MUSCOPParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}

}
