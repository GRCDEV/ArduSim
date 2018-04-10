package api;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.javatuples.Pair;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_FRAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param;
import main.Param.Protocol;
import main.Text;
import mbcap.gui.MBCAPConfigDialog;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPHelper;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.logic.MBCAPText;
import mission.MissionText;
import mission.StartMissionTestThread;
import sim.board.BoardPanel;
import sim.board.BoardParam;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVParam;
import uavController.UAVParam.Mode;

/** This class consists exclusively of static methods that allow to launch threads, and set specific configuration for mission based protocols.
 *  <p>The methods are sorted as they are called by the application on its different stages.
 *  <p>Most of these methods have their equivalent on the class SwarmHelper, where example code can be analized.
 *  <p>When developing a new protocol, the developer has to include code in this methods, if not stated otherwise. */

public class MissionHelper {
	
	/** Asserts if it is needed to load a mission, when using protocol on a real UAV. */
	public static boolean loadMission() {
		// On mission based protocols we always load a mission file
		return true;
	}
	
	/** Loads missions from Google Earth kml file.
	 * <p>Returns null if the file is not valid or it is empty.
	 * <p>Do not modify this method.
	 * <p>Helper function already applied for real and simulated UAVs. */
	@SuppressWarnings("unchecked")
	public static List<Waypoint>[] loadXMLMissionsFile(File xmlFile) {
		List<Waypoint>[] missions;
		try {
			Waypoint wp;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName(Text.LINE_TAG);
			if (nList.getLength()==0) {
				MissionHelper.log(Text.XML_PARSING_ERROR_1);
				return null;
			}
	
			missions = new ArrayList[nList.getLength()];
			String[][] lines = new String[nList.getLength()][];
			// One UAV per line
			String[] aux;
			double lat, lon, z;
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					lines[i] = eElement.getElementsByTagName(Text.COORDINATES_TAG).item(0).getTextContent().trim().split(" ");
					// Check if there are at least two coordinates to define a mission
					if (lines[i].length<2) {
						MissionHelper.log(Text.XML_PARSING_ERROR_2 + " " + (i+1));
						return null;
					}
					missions[i] = new ArrayList<>(lines[i].length+2); // Adding fake home, and substitute first real waypoint with take off
					// Add a fake waypoint for the home position (essential but ignored by the flight controller)
					wp = new Waypoint(0, true, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT, MAV_CMD.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 0, 0, 0, 1);
					missions[i].add(wp);
					// Check the coordinate triplet format
					for (int j=0; j<lines[i].length; j++) {
						aux = lines[i][j].split(",");
						if (aux.length!=3) {
							MissionHelper.log(Text.XML_PARSING_ERROR_3);
							return null;
						}
						try {
							lon = Double.parseDouble(aux[0].trim());
							lat = Double.parseDouble(aux[1].trim());
							//  Usually, Google Earth sets z=0
							z = Double.parseDouble(aux[2].trim());
							if (z==0) {
								//  Default flying altitude
								z = UAVParam.MIN_FLYING_ALTITUDE;
							}
							// Waypoint 0 is home and current
							// Waypoint 1 is take off
							if (j==0) {
								if (Param.IS_REAL_UAV) {
									wp = new Waypoint(1, false, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											MAV_CMD.MAV_CMD_NAV_TAKEOFF, 0, 0, 0, 0, 0, 0, z, 1);
									missions[i].add(wp);
									wp = new Waypoint(2, false, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											MAV_CMD.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								} else {
									wp = new Waypoint(1, false, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											MAV_CMD.MAV_CMD_NAV_TAKEOFF, 0, 0, 0, 0, lat, lon, z, 1);
									missions[i].add(wp);
								}
							} else {
								if (Param.IS_REAL_UAV) {
									wp = new Waypoint(j+2, false, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											MAV_CMD.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								} else {
									wp = new Waypoint(j+1, false, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											MAV_CMD.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								}
							}
						} catch (NumberFormatException e) {
							MissionHelper.log(Text.XML_PARSING_ERROR_4 + " " + (i+1) + "/" + (j+1));
							return null;
						}
					}
				}
			}
			return missions;
		} catch (ParserConfigurationException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		}
		return null;
	}

	/** Loads mission from standard QGroundControl file.
	 *  Returns null if the file is not valid or it is empty.
	 *  <p>Do not modify this method.
	 *  <p>This helper function is already applied for real and simulated UAVs. */
	@SuppressWarnings("unused")
	public static List<Waypoint> loadMissionFile(String path) {
	
		List<String> list = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				list.add(line);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Check if there are at least take off and a waypoint to define a mission (first line is header, and wp0 is home)
		// During simulation an additional waypoint is needed in order to stablish the starting location
		if (list==null || (Param.IS_REAL_UAV && list.size()<4) || (!Param.IS_REAL_UAV && list.size()<5)) {
			MissionHelper.log(Text.FILE_PARSING_ERROR_1);
			return null;
		}
		// Check the header of the file
		if (!list.get(0).trim().toUpperCase().equals(Text.FILE_HEADER)) {
			MissionHelper.log(Text.FILE_PARSING_ERROR_2);
			return null;
		}
	
		List<Waypoint> mission = new ArrayList<Waypoint>(list.size()-1);
		Waypoint wp;
		String[] line;
		// One line per waypoint
		// The first waypoint is supposed to be a fake point that will be ignored by the flight controller, but it is needed
		wp = new Waypoint(0, true, MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT, MAV_CMD.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 0, 0, 0, 1);
		mission.add(wp);
		for (int i=2; i<list.size(); i++) { // After header and fake home lines
			// Tabular delimited line
			line = list.get(i).split("\t");
			if (line.length!=12) {
				MissionHelper.log(Text.FILE_PARSING_ERROR_3 + " " + i);
				return null;
			}
			int numSeq, frame, command;
			double param1, param2, param3, param4, param5, param6, param7;
			int autoContinue;
			try {
				numSeq = Integer.parseInt(line[0].trim());
				frame = Integer.parseInt(line[2].trim());
				command = Integer.parseInt(line[3].trim());
				param1 = Double.parseDouble(line[4].trim());
				param2 = Double.parseDouble(line[5].trim());
				param3 = Double.parseDouble(line[6].trim());
				param4 = Double.parseDouble(line[7].trim());
				param5 = Double.parseDouble(line[8].trim());
				param6 = Double.parseDouble(line[9].trim());
				param7 = Double.parseDouble(line[10].trim());
				autoContinue = Integer.parseInt(line[11].trim());
				
				if (numSeq + 1 != i) {
					MissionHelper.log(Text.FILE_PARSING_ERROR_6);
					return null;
				}
				
				if (numSeq == 1) {
					if ((frame != MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT
							|| command != MAV_CMD.MAV_CMD_NAV_TAKEOFF
							|| (Param.IS_REAL_UAV && param7 <= 0))) {
						MissionHelper.log(Text.FILE_PARSING_ERROR_5);
						return null;
					}
					wp = new Waypoint(numSeq, false, frame, command, param1, param2, param3, param4, param5, param6, param7, autoContinue);
					mission.add(wp);
				} else {
					if (Param.IS_REAL_UAV) {
						wp = new Waypoint(numSeq, false, frame, command, param1, param2, param3, param4, param5, param6, param7, autoContinue);
						mission.add(wp);
					} else {
						if (numSeq == 2) {
							// Set takeoff coordinates from the first wapoint that has coordinates
							wp = mission.get(mission.size()-1);
							wp.setLatitude(param5);
							wp.setLongitude(param6);
							wp.setAltitude(param7);
						} else {
							numSeq = numSeq - 1;
							wp = new Waypoint(numSeq, false, frame, command, param1, param2, param3, param4, param5, param6, param7, autoContinue);
							mission.add(wp);
						}
					}
				}
			} catch (NumberFormatException e1) {
				MissionHelper.log(Text.FILE_PARSING_ERROR_4 + " " + i);
				return null;
			}
		}
		return mission;
	}
	
	/** Opens the configuration dialog of mission based protocols. */
	public static void openMissionConfigurationDialog() {

		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					new MBCAPConfigDialog();
				}
			});
		}
		// Add here the configuration window of any other mission based protocol
		//  or set Param.sim_status = SimulatorState.STARTING_UAVS to go to the next step

	}
	
	/** Initializes data structures related to mission based protocols. */
	public static void initializeMissionDataStructures() {
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			MBCAPHelper.initializeProtocolDataStructures();
		}
		
		// Add here the initialization of data structures needed for any other mission based protocol
	}
	
	/** Sets the initial value of the protocol state in the progress window for mission based protocols. */
	public static void setMissionProtocolInitialState(JLabel label) {
		
		if ((Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4)) {
			label.setText(MBCAPState.NORMAL.getName());
		}
		
		// Add here the initial value of the protocol in the progress dialog
		
	}
	
	/** Updates the value of the protocol state in the progress window for mission based protocols.
	 * <p>Do not modify this method. */
	public static void setMissionState(int numUAV, String text) {
		SimTools.updateprotocolState(numUAV, text);
	}
	
	/** Updates the text shown in the upper-right corner when mission based protocols change the state.
	 * <p>Do not modify this method. */
	public static void setMissionGlobalInformation(String text) {
		SimTools.updateGlobalInformation(text);
	}
	
	/** Updates the log in the main window.
	 * <p>Do not modify this method. */
	public static void log(String text) {
		SimTools.println(text);
	}
	
	/** Rescales mission based protocols specific data structures when the visualization scale changes. */
	public static void rescaleMissionDataStructures() {
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			MBCAPGUITools.rescaleSafetyCircles();
		}
		
		// Add here a function to locate any shown information in the main window
		
	}
	
	/** Rescales for visualization the resources used in mission based protocols, each time the visualization scale changes. */
	public static void rescaleMissionResources() {
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			MBCAPGUITools.rescaleImpactRiskMarkPoints();
		}
		
		// Add here a function to redraw any resource used for any other mission based protocol
		
	}
	
	/** Draw resources used in mission based protocols. */
	public static void drawMissionResources(Graphics2D g2, BoardPanel p) {
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			if (!UAVParam.collisionDetected) {
				g2.setStroke(SimParam.STROKE_POINT);
				MBCAPGUITools.drawPredictedLocations(g2);
				MBCAPGUITools.drawCollisionCircle(g2);
				MBCAPGUITools.drawImpactRiskMarks(g2, p);
			}
		}
		
		// Add here a function to draw any resource related to any other mission based protocol
		
	}
	
	/** Loads needed resources for mission based protocols. */
	public static void loadMissionResources() {
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			MBCAPGUITools.loadRiskImage();
		}
		
		// Add here to load resources for any other mission based protocol
	}
	
	/** Gets the initial position of the UAVs from the missions loaded, when using the simulator.
	 * <p>Returns the current Geographic coordinates, and the heading towards the next waypoint, when the mission is retrieved from the UAV.
	 * <p>Do not modify this method. */
	@SuppressWarnings("unchecked")
	public static Pair<GeoCoordinates, Double>[] getMissionStartingLocation() {
		Pair<GeoCoordinates, Double>[] startingLocations = new Pair[Param.numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		for (int i = 0; i < Param.numUAVs; i++) {
			if (UAVParam.missionGeoLoaded != null && UAVParam.missionGeoLoaded[i] != null) {
				waypointFound = false;
				for (int j=0; j<UAVParam.missionGeoLoaded[i].size() && !waypointFound; j++) {
					waypoint1 = UAVParam.missionGeoLoaded[i].get(j);
					if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
						waypoint1pos = j;
						waypointFound = true;
					}
				}
				if (!waypointFound) {
					GUIHelper.exit(Text.UAVS_START_ERROR_6 + " " + Param.id[i]);
				}
				waypointFound = false;
				for (int j=waypoint1pos+1; j<UAVParam.missionGeoLoaded[i].size() && !waypointFound; j++) {
					waypoint2 = UAVParam.missionGeoLoaded[i].get(j);
					if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
						waypointFound = true;
					}
				}
				if (waypointFound) {
					// We only can set a heading if at least two points with valid coordinates are found
					p1UTM = GUIHelper.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
					p2UTM = GUIHelper.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
					incX = p2UTM.Easting - p1UTM.Easting;
					incY = p2UTM.Northing - p1UTM.Northing;
					if (incX != 0 || incY != 0) {
						if (incX == 0) {
							if (incY > 0)	heading = 0.0;
							else			heading = 180.0;
						} else if (incY == 0) {
							if (incX > 0)	heading = 89.9;	// SITL fails changing the flight mode if heading is close to 90 degrees
							else			heading = 270.0;
						} else {
							double gamma = Math.atan(incY/incX);
							if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
							else 			heading = 270.0 - gamma * 180 / Math.PI;
						}
						// Same correction due to the SITL error
						if (heading < 90 && heading > 89.9) {
							heading = 89.9;
						}
						if (heading > 90 && heading < 90.1) {
							heading = 90.1;
						}
					}
				}
			} else {
				// Assuming that all UAVs have a mission loaded
				GUIHelper.exit(Text.APP_NAME + ": " + Text.UAVS_START_ERROR_5 + " " + Param.id[i] + ".");
			}
			startingLocations[i] = Pair.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
		}
		return startingLocations;
	}
	
	/** Sends the initial configuration needed by the mission based protocol to a specific UAV.
	 * <p>Do not modify this method.
	 * <p>Returns true if the operation was successful. */
	public static boolean sendBasicMissionConfig(int numUAV) {
		boolean success = false;
		// Erases, sends and retrieves the planned mission. Blocking procedure
		if (API.clearMission(numUAV)
				&& API.sendMission(numUAV, UAVParam.missionGeoLoaded[numUAV])
				&& API.getMission(numUAV)
				&& API.setCurrentWaypoint(numUAV, 0)) {
			MissionHelper.simplifyMission(numUAV);
			Param.numMissionUAVs.incrementAndGet();
			if (!Param.IS_REAL_UAV) {
				BoardParam.rescaleQueries.incrementAndGet();
			}
			success = true;
		}
		return success;
	}
	
	/** Creates the simplified mission shown on screen, and forces view to rescale.
	 * <p>Do not modify this method. */
	public static void simplifyMission(int numUAV) {
		List<WaypointSimplified> missionUTMSimplified = new ArrayList<WaypointSimplified>();
	
		// Hypothesis:
		//   The first take off waypoint retrieved from the UAV is used as "home"
		//   The "home" coordinates are no modified along the mission
		WaypointSimplified first = null;
		boolean foundFirst = false;
		Waypoint wp;
		UTMCoordinates utm;
		for (int i=0; i<UAVParam.currentGeoMission[numUAV].size(); i++) {
			wp = UAVParam.currentGeoMission[numUAV].get(i);
			switch (wp.getCommand()) {
			case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
			case MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM:
			case MAV_CMD.MAV_CMD_NAV_LOITER_TURNS:
			case MAV_CMD.MAV_CMD_NAV_LOITER_TIME:
			case MAV_CMD.MAV_CMD_NAV_SPLINE_WAYPOINT:
			case MAV_CMD.MAV_CMD_PAYLOAD_PREPARE_DEPLOY:
			case MAV_CMD.MAV_CMD_NAV_LOITER_TO_ALT:
			case MAV_CMD.MAV_CMD_NAV_LAND:
				utm = GUIHelper.geoToUTM(wp.getLatitude(), wp.getLongitude());
				WaypointSimplified swp = new WaypointSimplified(wp.getNumSeq(), utm.Easting, utm.Northing, wp.getAltitude());
				missionUTMSimplified.add(swp);
				break;
			case MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH:
				if (!foundFirst) {
					MissionHelper.log(Text.SIMPLIFYING_WAYPOINT_LIST_ERROR + Param.id[numUAV]);
				} else {
					WaypointSimplified s = new WaypointSimplified(wp.getNumSeq(),
							first.x, first.y, UAVParam.RTLAltitude[numUAV]);
					missionUTMSimplified.add(s);
				}
				break;
			case MAV_CMD.MAV_CMD_NAV_TAKEOFF:
				// The geographic coordinates have been set by the flight controller
				utm = GUIHelper.geoToUTM(wp.getLatitude(), wp.getLongitude());
				WaypointSimplified twp = new WaypointSimplified(wp.getNumSeq(), utm.Easting, utm.Northing, wp.getAltitude());
				missionUTMSimplified.add(twp);
				if (!foundFirst) {
					utm = GUIHelper.geoToUTM(wp.getLatitude(), wp.getLongitude());
					first = new WaypointSimplified(wp.getNumSeq(), utm.Easting, utm.Northing, wp.getAltitude());
					foundFirst = true;
				}
				break;
			}
		}
		UAVParam.missionUTMSimplified.set(numUAV, missionUTMSimplified);
	}
	
	/** Launch threads related to mission based protocols. */
	public static void launchMissionThreads() {
		
		if ((Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4)) {
			MBCAPHelper.startMBCAPThreads();
		}
		
		// Add here to launch threads for any other mission based protocol
		
	}
	
	/** Starts the movement of each UAV by arming engines, setting auto mode, and throttle to stabilize altitude.
	 * <p>Do not modify this method. */
	public static void startMissionTestActionPerformed() {
		// All UAVs start the experiment in different threads, but the first
		StartMissionTestThread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new StartMissionTestThread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new StartMissionTestThread(i);
			}
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1].start();
			}
		}
		StartMissionTestThread.startMissionTest(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException e) {
				}
			}
		}
		if (StartMissionTestThread.UAVS_TESTING.get() < Param.numUAVs) {
			GUIHelper.exit(MissionText.START_MISSION_ERROR);
		}
	}
	
	/** Detects the end of the mission experiment (specifically, when all UAVs are near enough to the last waypoint).
	 * <p>It also lands the UAVs when they reach that point, if needed.
	 * <p>Do not modify this method. */
	public static void detectMissionEnd() {
		for (int i = 0; i < Param.numUAVs; i++) {
			List<WaypointSimplified> mission = UAVParam.missionUTMSimplified.get(i);
			Mode mode = UAVParam.flightMode.get(i);
			if (mission != null
					&& mode != UAVParam.Mode.LAND_ARMED
					&& mode != UAVParam.Mode.LAND
					&& UAVParam.currentWaypoint.get(i) > 0
					&& UAVParam.currentWaypoint.get(i) == mission.get(mission.size()-1).numSeq) {
				
				if (!UAVParam.lastWaypointReached[i]) {
					UAVParam.lastWaypointReached[i] = true;
					MissionHelper.log(SimParam.prefix[i] + MBCAPText.WAYPOINT_REACHED);
				}
				
				if (UAVParam.uavCurrentData[i].getUTMLocation().distance(mission.get(mission.size()-1)) < Param.LAST_WP_THRESHOLD) {
					int command = UAVParam.currentGeoMission[i].get(UAVParam.currentGeoMission[i].size() - 1).getCommand();
					// There is no need of landing when the last waypoint is the command LAND or when it is RTL under some circumstances
					if (command != MAV_CMD.MAV_CMD_NAV_LAND
							&& !(command == MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH && UAVParam.RTLAltitudeFinal[i] == 0)) {
						// Land the UAV when reaches the last waypoint and mark as finished
						if (API.setMode(i, UAVParam.Mode.LAND_ARMED)) {
						} else {
							MissionHelper.log(SimParam.prefix[i] + MissionText.LAND_ERROR);
						}
					}
				}
			}
		}
	}
	
	/** Logs the experiment main results. */
	public static String getMissionTestResults() {
		String results = "";
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			results = MBCAPGUITools.getMBCAPResults();
		}
		
		// Add here code to log the experiment results to the results dialog
		
		return results;
	}
	
	/** Logs the mission based protocols configuration. */
	public static String getMissionProtocolConfig() {
		String configuration = "";
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			configuration = MBCAPGUITools.getMBCAPConfiguration();
		}
		
		// Add here code to log the mission based protocols configuration to the results dialog
		
		return configuration;
	}

	/** Logging to file of specific information related to mission based protocols. */
	public static void logMissionData(String folder, String baseFileName) {
		MissionHelper.logMission(folder, baseFileName);
		
		if (Param.selectedProtocol == Protocol.MBCAP_V1
				|| Param.selectedProtocol == Protocol.MBCAP_V2
				|| Param.selectedProtocol == Protocol.MBCAP_V3
				|| Param.selectedProtocol == Protocol.MBCAP_V4) {
			MBCAPHelper.storeBeaconsError(folder, baseFileName);
		}
		
		// Add here to store any information generated by any other mission based protocols
		
	}
	
	/** Logging to file the UAVs mission, in AutoCAD format.
	 * <p>Do not modify this method. */
	private static void logMission(String folder, String baseFileName) {
		File file;
		StringBuilder sb;
		int j;
		for (int i=0; i<Param.numUAVs; i++) {
			file = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + MissionText.MISSION_SUFIX);
			sb = new StringBuilder(2000);
			sb.append("._PLINE\n");
			j = 0;
			List<WaypointSimplified> missionUTMSimplified = UAVParam.missionUTMSimplified.get(i);
			if (missionUTMSimplified != null) {
				WaypointSimplified prev = null;
				WaypointSimplified current;
				while (j<missionUTMSimplified.size()) {
					current = missionUTMSimplified.get(j);
					if (prev == null) {
						sb.append(GUIHelper.round(current.x, 3)).append(",").append(GUIHelper.round(current.y, 3)).append("\n");
						prev = current;
					} else if (!current.equals(prev)) {
						sb.append(GUIHelper.round(current.x, 3)).append(",").append(GUIHelper.round(current.y, 3)).append("\n");
						prev = current;
					}
					j++;
				}
				sb.append("\n");
				GUIHelper.storeFile(file, sb.toString());
			}
		}
	}
	
	/** Manually takes off all the UAVs: executes mode=guided, arm engines, and then the take off. Blocking method.
	 * <p>Usefull when starting on the ground, on stabilize flight mode
	 * <p>Do not modify this method. */
	public static void takeoff() {
		for (int i = 0; i < Param.numUAVs; i++) {
			if (!API.setMode(i, UAVParam.Mode.GUIDED) || !API.armEngines(i) || !API.doTakeOff(i)) {
				GUIHelper.exit(Text.TAKE_OFF_ERROR_1 + " " + Param.id[i]);
			}
		}
		// The application must wait until all UAVs reach the planned altitude
		for (int i = 0; i < Param.numUAVs; i++) {
			while (UAVParam.uavCurrentData[i].getZRelative() < 0.95 * UAVParam.takeOffAltitude[i]) {
				if (Param.VERBOSE_LOGGING) {
					MissionHelper.log(SimParam.prefix[i] + Text.ALTITUDE_TEXT
							+ " = " + String.format("%.2f", UAVParam.uavCurrentData[i].getZ())
							+ " " + Text.METERS);
				}
				GUIHelper.waiting(UAVParam.ALTITUDE_WAIT);
			}
		}
	}

}
