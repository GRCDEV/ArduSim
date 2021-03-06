package com.protocols.muscop.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.formations.Formation;
import com.api.pojo.location.Waypoint;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.GUI;
import com.api.MissionHelper;
import com.setup.sim.logic.SimParam;
import com.protocols.muscop.gui.MuscopConfigDialogApp;
import com.protocols.muscop.gui.MuscopSimProperties;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class MUSCOPHelper extends ProtocolHelper {

	private final List<MUSCOPListenerThread> threads = new ArrayList<>();

	@Override
	public void setProtocol() {
		this.protocolString = MUSCOPText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		// In a real multicopter, the UAV is always in position 0. The master UAV is which has a mission to load.
		return API.getCopter(0).getMasterSlaveHelper().isMaster();
	}

	@Override
	public JDialog openConfigurationDialog() {return null;}

	@Override
	public void openConfigurationDialogFX() {
		Platform.runLater(()->new MuscopConfigDialogApp().start(new Stage()));
	}

	@Override
	public void configurationCLI() {
		MuscopSimProperties properties = new MuscopSimProperties();
		ResourceBundle resources;
		try {
			FileInputStream fis = new FileInputStream(SimParam.protocolParamFile);
			resources = new PropertyResourceBundle(fis);
			fis.close();
			Properties p = new Properties();
			for(String key: resources.keySet()){
				p.setProperty(key,resources.getString(key));
			}
			properties.storeParameters(p,resources);
		} catch (IOException e) {
			e.printStackTrace();
			ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND );
			System.exit(0);
		}

	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = API.getArduSim().getNumUAVs();
		AtomicInteger[] state = new AtomicInteger[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			state[i] = new AtomicInteger();	// Implicit value State.START, as it is equals to 0
		}
		MuscopSimProperties.state = state;
		for(int i =0;i<numUAVs;i++){
			threads.add(new MUSCOPListenerThread(i));
		}
	}

	@Override
	public String setInitialState() {
		return MUSCOPText.START;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		// As a design decision:
		// The ground formation will appear with the UAV in the center in the first waypoint of the mission from the master
		// The ission to be followed will start in the location where the flight center UAV will be after taking off
		// We overlap the original master mission with the center UAV mission for visualization
		
		// 1. Get the yaw of the master given the current mission
		//    We only can set yaw if at least two points with valid coordinates are found
		// 1.1. Check if master mission exists
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		GUI gui = API.getGUI(0);
		MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
		List<Waypoint>[] missions = missionHelper.getMissionsLoaded();	// Simplified mission would be better, but it is not ready jet
		// The master UAV is always in the position 0 of arrays
		List<Waypoint> masterMission = null;
		if (missions == null || missions[0] == null) {
			gui.exit(MUSCOPText.UAVS_START_ERROR_1);
		} else {
			// 1.2. Get the heading
			// Locate the first waypoint with coordinates
			masterMission = missions[0];
			waypointFound = false;
			for (int i = 0; i < masterMission.size() && !waypointFound; i++) {
				waypoint1 = masterMission.get(i);
				if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
					waypoint1pos = i;
					waypointFound = true;
				}
			}
			if (waypointFound) {
				// Locate the second waypoint with coordinates
				waypointFound = false;
				for (int i = waypoint1pos + 1; i < masterMission.size()
						&& !waypointFound; i++) {
					waypoint2 = masterMission.get(i);
					if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
						waypointFound = true;
					}
				}
				// Get the heading
				if (waypointFound) {
					MuscopSimProperties.formationYaw = MUSCOPHelper.getYaw(waypoint1.getUTM(), waypoint2.getUTM());
				} else {
					MuscopSimProperties.formationYaw = 0.0;
				}
			} else {
				gui.exit(MUSCOPText.UAVS_START_ERROR_2);
			}
		}

		// 2. With the ground formation centered on the mission beginning, get ground coordinates.
		//   The UAVs appear with the center UAV in the first waypoint of the initial mission.
		//   As this is simulation, ID and position on the ground are the same for all the UAVs.
		int numUAVs = API.getArduSim().getNumUAVs();

		Formation groundFormation = UAVParam.groundFormation.get();
		System.out.println(groundFormation.getLayout().toString());
		int groundCenterUAVPosition = groundFormation.getCenterIndex(); //groundFormation.getCenterUAVPosition();
		Map<Long, Location2DUTM> groundLocations = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		Location2DUTM locationUTM;
		Location2DUTM groundCenterUTMLocation = waypoint1.getUTM();
		for (int i = 0; i < numUAVs; i++) {
			if (i == groundCenterUAVPosition) {
				groundLocations.put((long)i, groundCenterUTMLocation);
			} else {
				locationUTM = groundFormation.get2DUTMLocation(groundCenterUTMLocation,i);
				System.out.println(locationUTM.toString());
				groundLocations.put((long)i, locationUTM);
			}
		}
		
		// 3. Get the ID of the UAV that will be center in the flight formation
		Pair<Long, Location2DUTM> flightCenterUAV = new Pair<>((long)groundFormation.getCenterIndex(),groundCenterUTMLocation);
		
		// 4. Copy the mission to the location where the UAV in the center of the flight formation will be
		//   We do this to show the real path followed by the swarm as a mission for the flight center UAV, as it will follow this mission coordinating the remaining UAVs.
		//   First, we copy the mission as is
		List<Waypoint> centerMission = new ArrayList<>(masterMission.size());
		int p = -1;
		for (int i = 0; i < masterMission.size(); i++) {
			centerMission.add(i, new Waypoint(masterMission.get(i)));
			if (masterMission.get(i).getNumSeq() == waypoint1.getNumSeq()) {
				p = i;
			}
		}
		//   Now we modify the first waypoint with coordinates to match the air location of the UAV that will be in the center of the formation.
		Waypoint moved = centerMission.get(p);
		Location2DGeo movedGeo;
		try {
			movedGeo = flightCenterUAV.getValue1().getGeo();
			moved.setLatitude(movedGeo.latitude);
			moved.setLongitude(movedGeo.longitude);
			// Finally, we set the mission in the center UAV. As this is simulation, position and ID always match.
			missions[flightCenterUAV.getValue0().intValue()] = centerMission;
			missionHelper.setMissionsLoaded(missions);
			
			// 5. Using the ground center UAV location as reference, get the starting location of all the UAVs
			@SuppressWarnings("unchecked")
			Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
			for (int i = 0; i < numUAVs; i++) {
				if (i == groundCenterUAVPosition) {
					startingLocation[i] = Pair.with(new Location2DGeo(waypoint1.getLatitude(), waypoint1.getLongitude()), MuscopSimProperties.formationYaw);
				} else {
					startingLocation[i] = Pair.with(groundLocations.get((long)i).getGeo(), MuscopSimProperties.formationYaw);
				}
			}
			return startingLocation;
		} catch (LocationNotReadyException e) {
			e.printStackTrace();
			gui.exit(e.getMessage());
		}
		return null;
	}

	/** Calculates the yaw (rad) from p1 to p2. */
	private static double getYaw(Location2DUTM p1, Location2DUTM p2) {
		double incX, incY;
		double heading = 0.0;
		incX = p2.x - p1.x;
		incY = p2.y - p1.y;
		if (incX != 0 || incY != 0) {
			if (incX == 0) {
				if (incY > 0)
					heading = 0.0;				// Pointing north
				else
					heading = Math.PI;			// Pointing south
			} else if (incY == 0) {
				if (incX > 0)
					heading = Math.PI / 2;		// Pointing east
				else
					heading = 3 * Math.PI / 2;	// Pointing west
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
			threads.get(i).start();
		}
		API.getGUI(0).log(MUSCOPText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		int numUAVs = API.getArduSim().getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			// start the setup in the tread
			threads.get(i).startSetup = true;
		}
		boolean setupFinished = false;
		while(!setupFinished){
			API.getArduSim().sleep(200);
			for (int i = 0; i < numUAVs; i++) {
				// start the setup in the tread
				setupFinished = threads.get(i).setupFinished;
			}
		}

		/*
		while (API.getArduSim().isSetupFinished()) {
			API.getArduSim().sleep(200);
		}*/
		/*
		ArduSim ardusim = API.getArduSim();
		int numUAVs = ardusim.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (MuscopSimProperties.state[i].get() < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);
			}
		}
		*/
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
	}

}
