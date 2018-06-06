package api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_FRAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Main;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.logic.SimParam;
import uavController.UAVParam;

public class Tools {
	
	// TCP parameter: maximum size of the byte array used on messages
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	/** Returns true if the protocol is running on an real UAV or false if a simulation is being performed. */
	public static boolean isRealUAV() {
		return Param.IS_REAL_UAV;
	}
	
	/** Returns the number of UAVs that are running on the same machine.
	 * <p> 1 When running on a real UAV (arrays are of size 1 and numUAV==0).
	 * <p> n When running a simulation on a PC. */
	public static int getNumUAVs() {
		return Param.numUAVs;
	}
	
	/** Sets the number of UAVs that will be simulated.
	 * <p>Only use it in the protocol configuration dialog, and when a parameter limits the number of UAVs that must be simulated. */
	public static void setNumUAVs(int numUAVs) {
		Param.numUAVsTemp.set(numUAVs);
	}
	
	/** Returns the ID of a UAV.
	 * <p>On real UAV returns a value based on the MAC address.
	 * <p>On virtual UAV returns the position of the UAV in the arrays used by the simulator. */
	public static long getIdFromPos(int numUAV) {
		return Param.id[numUAV];
	}
	
	/** Use this function to assert that the configuration of the protocol has finished when the corresponding dialog is closed.
	 * <p>In order the parameters of the protocol to work properly, please establish default values for all of them to be used automatically when ArduSim is loaded. */
	public static void setProtocolConfigured(boolean isConfigured) {
		Param.simStatus = SimulatorState.STARTING_UAVS;
	}
	
	/** Returns true while the UAVs are starting and without valid coordinates. Phase 1. */
	public static boolean areUAVsNotAvailable() {
		return Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL
				|| Param.simStatus == Param.SimulatorState.CONFIGURING;
	}
	
	/** Returns true if the UAVs are available and ready for the setup step, which has not been started jet. Phase 2. */
	public static boolean areUAVsReadyForSetup() {
		return Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED;
	}
	
	/** Returns true while the setup step is in progress. Phase 3. */
	public static boolean isSetupInProgress() {
		return Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS;
	}
	
	/** Returns true if the setup step has finished but the experiment has not started. Phase 4. */
	public static boolean isSetupFinished() {
		return Param.simStatus == Param.SimulatorState.READY_FOR_TEST;
	}
	
	/** Returns true while the experiment is in progress. */
	public static boolean isExperimentInProgress() {
		return Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS;
	}
	
	/** Returns true if the experiment is finished. */
	public static boolean isExperimentFinished() {
		return Param.simStatus == Param.SimulatorState.TEST_FINISHED;
	}
	
	/** Loads missions from a Google Earth kml file.
	 * <p>Returns null if the file is not valid or it is empty. */
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
				GUI.log(Text.XML_PARSING_ERROR_1);
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
						GUI.log(Text.XML_PARSING_ERROR_2 + " " + (i+1));
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
							GUI.log(Text.XML_PARSING_ERROR_3);
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
							GUI.log(Text.XML_PARSING_ERROR_4 + " " + (i+1) + "/" + (j+1));
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

	/** Loads a mission from a standard QGroundControl file.
	 * <p>Returns null if the file is not valid or it is empty. */
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
			GUI.log(Text.FILE_PARSING_ERROR_1);
			return null;
		}
		// Check the header of the file
		if (!list.get(0).trim().toUpperCase().equals(Text.FILE_HEADER)) {
			GUI.log(Text.FILE_PARSING_ERROR_2);
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
				GUI.log(Text.FILE_PARSING_ERROR_3 + " " + i);
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
					GUI.log(Text.FILE_PARSING_ERROR_6);
					return null;
				}
				
				if (numSeq == 1) {
					if ((frame != MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT
							|| command != MAV_CMD.MAV_CMD_NAV_TAKEOFF
							|| (Param.IS_REAL_UAV && param7 <= 0))) {
						GUI.log(Text.FILE_PARSING_ERROR_5);
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
				GUI.log(Text.FILE_PARSING_ERROR_4 + " " + i);
				return null;
			}
		}
		return mission;
	}

	/** Loads the first mission found in a folder.
	 * <p>Priority loading: 1st xml file, 2nd QGRoundControl file, 3rd txt file.
	 * <p>Only the first valid file/mission found is used.
	 * <p>Returns null if no valid mission was found. */
	public static List<Waypoint> loadMission(File parentFolder) {
		// 1. kml file case
		File[] files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_KML) && pathname.isFile();
			}
		});
		if (files != null && files.length>0) {
			// Only one kml file must be on the current folder
			if (files.length>1) {
				GUI.exit(Text.MISSIONS_ERROR_6);
			}
			List<Waypoint>[] missions = loadXMLMissionsFile(files[0]);
			if (missions != null && missions.length>0) {
				GUI.log(Text.MISSION_XML_SELECTED + " " + files[0].getName());
				return missions[0];
			}
		}
		
		// 2. kml file not found or not valid. waypoints file case
		files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_WAYPOINTS) && pathname.isFile();
			}
		});
		List<Waypoint> mission;
		if (files != null && files.length>0) {
			// Only one waypoints file must be on the current folder
			if (files.length>1) {
				GUI.exit(Text.MISSIONS_ERROR_7);
			}
			mission = loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				GUI.log(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		
		// 3. waypoints file not found or not valid. txt file case
		files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_TXT) && pathname.isFile();
			}
		});
		if (files != null && files.length>0) {
			// Not checking single txt file existence, as other file types can use the same extension
			mission = loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				GUI.log(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		return null;
	}
	
	/** Sets the loaded missions from file/s for the UAVs, in geographic coordinates.
	 * <p>Missions must be loaded and set in the protocol configuration dialog when needed.*/
	public static void setLoadedMissionsFromFile(List<Waypoint>[] missions) {
		UAVParam.missionGeoLoaded = missions;
	}
	
	/** Provides the missions loaded from files in geographic coordinates.
	 * <p>Mission only available once they has been loaded.
	 * <p>Returns null if not available.*/
	public static List<Waypoint>[] getLoadedMissions() {
		return UAVParam.missionGeoLoaded;
	}

	/** Provides the mission currently stored in the UAV in geographic coordinates.
	 * <p>Mission only available if previously is sent to the drone with sendMission(int,List<Waypoint>) and retrieved with retrieveMission(int).*/
	public static List<Waypoint> getUAVMission(int numUAV) {
		return UAVParam.currentGeoMission[numUAV];
	}

	/** Provides the simplified mission shown in the screen in UTM coordinates.
	 * <p>Mission only available if previously is sent to the drone with sendMission(int,List<Waypoint>) and retrieved with retrieveMission(int).*/ 
	public static List<WaypointSimplified> getUAVMissionSimplified(int numUAV) {
		return UAVParam.missionUTMSimplified.get(numUAV);
	}
	
	/** Advises if the collision check is enabled or not. */
	public static boolean isCollisionCheckEnabled() {
		return UAVParam.collisionCheckEnabled;
	}
	
	/** Provides the maximum ground distance between two UAVs to assert that a collision has happened. */
	public static double getCollisionHorizontalDistance() {
		return UAVParam.collisionDistance;
	}
	
	/** Provides the maximum vertical distance between two UAVs to assert that a collision has happened. */
	public static double getCollisionVerticalDistance() {
		return UAVParam.collisionAltitudeDifference;
	}
	
	/** Advises when at least one collision between UAVs has happened. */
	public static boolean isCollisionDetected() {
		return UAVParam.collisionDetected;
	}
	
	/** Transforms UTM coordinates to Geographic coordinates. 
	 *  <p>Example: Tools.UTMToGeo(312915.84, 4451481.33).
	 *  <p>It is assumed that this function is used when at least one coordinate set is received from the UAV, in order to get the zone and the letter of the UTM projection, available on GUIParam.zone and GUIParam.letter. */
	public static GeoCoordinates UTMToGeo(double Easting, double Northing) {
		double latitude;
		double longitude;
	
		double Hem;
		if (SimParam.letter > 'M')
			Hem = 'N';
		else
			Hem = 'S';
		double north;
		if (Hem == 'S')
			north = Northing - 10000000;
		else
			north = Northing;
		latitude = (north / 6366197.724 / 0.9996 + (1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) - 0.006739496742 * Math.sin(north / 6366197.724 / 0.9996) * Math.cos(north / 6366197.724 / 0.9996) * (Math.atan(Math.cos(Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996) * 3 / 2) * (Math.atan(Math.cos(Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625/ Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow( Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996)) * 180 / Math.PI;
		latitude = Math.round(latitude * 10000000);
		latitude = latitude / 10000000;
		longitude = Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) * 180 / Math.PI + SimParam.zone * 6 - 183;
		longitude = Math.round(longitude * 10000000);
		longitude = longitude / 10000000;
	
		return new GeoCoordinates(latitude, longitude);
	}

	/** Transforms Geographic coordinates to UTM coordinates. */
	public static UTMCoordinates geoToUTM(double lat, double lon) {
		double Easting;
		double Northing;
		int Zone = (int) Math.floor(lon / 6 + 31);
		char Letter;
		if (lat < -72)		Letter = 'C';
		else if (lat < -64)	Letter = 'D';
		else if (lat < -56)	Letter = 'E';
		else if (lat < -48)	Letter = 'F';
		else if (lat < -40)	Letter = 'G';
		else if (lat < -32)	Letter = 'H';
		else if (lat < -24)	Letter = 'J';
		else if (lat < -16)	Letter = 'K';
		else if (lat < -8)	Letter = 'L';
		else if (lat < 0)	Letter = 'M';
		else if (lat < 8)	Letter = 'N';
		else if (lat < 16)	Letter = 'P';
		else if (lat < 24)	Letter = 'Q';
		else if (lat < 32)	Letter = 'R';
		else if (lat < 40)	Letter = 'S';
		else if (lat < 48)	Letter = 'T';
		else if (lat < 56)	Letter = 'U';
		else if (lat < 64)	Letter = 'V';
		else if (lat < 72)	Letter = 'W';
		else				Letter = 'X';
		Easting = 0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2) / 3) + 500000;
		Easting = Math.round(Easting * 100) * 0.01;
		Northing = (Math.atan(Math.tan(lat * Math.PI / 180) / Math.cos((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) - lat * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(lat * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (lat * Math.PI / 180 - 0.005054622556 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 3);
		if (Letter < 'M')
			Northing = Northing + 10000000;
		Northing = Math.round(Northing * 100) * 0.01;
	
		if (SimParam.zone < 0) {
			SimParam.zone = Zone;
			SimParam.letter = Letter;
		}
		
		return new UTMCoordinates(Easting, Northing, Zone, Letter);
	}

	/** Formats time retrieved by System.curentTimeMillis(), from initial to final time, to h:mm:ss. */
	public static String timeToString(long start, long end) {
		long time = Math.abs(end - start);
		long h = time/3600000;
		time = time - h*3600000;
		long m = time/60000;
		time = time - m*60000;
		long s = time/1000;
		return h + ":" + String.format("%02d", m) + ":" + String.format("%02d", s);
	}

	/** Round a double number to "places" decimal digits. */
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	/** Validates a TCP port. */
	public static boolean isValidPort(String validating) {
		if (validating == null) {
			return false;
		}
	
		try {
			int x = Integer.parseInt(validating);
			if (x < 1024 || x > 65535) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/** Validates a positive integer number. */
	public static boolean isValidInteger(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			int x = Integer.parseInt(validating);
			if (x <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/** Validates a positive double number. */
	public static boolean isValidPositiveDouble(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			double x = Double.parseDouble(validating);
			if (x <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/** Validates a double number. */
	public static boolean isValidDouble(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			Double.parseDouble(validating);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/** Gets the folder where the simulator is running (.jar folder or project root under eclipse). */
	public static File getCurrentFolder() {
		Class<Main> c = main.Main.class;
		CodeSource codeSource = c.getProtectionDomain().getCodeSource();
	
		File jarFile = null;
	
		if (codeSource != null && codeSource.getLocation() != null) {
			try {
				jarFile = new File(codeSource.getLocation().toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		else {
			String path = c.getResource(c.getSimpleName() + ".class").getPath();
			String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
			try {
				jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			jarFile = new File(jarFilePath);
		}
		return jarFile.getParentFile();
	}

	/** Gets the file extension
	 * <p>Returns empty String if there is not file extension */
	public static String getFileExtension(File file) {
		String fileName = file.getName();
		if(fileName.lastIndexOf(".") > 0) {
			return fileName.substring(fileName.lastIndexOf(".")+1);
		} else {
			return "";
		}
	}

	/** Stores a String in a file. */
	public static void storeFile(File destiny, String text) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(destiny);
			fw.write(text);
			fw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/** Returns true if verbose store feature is enabled.
	 * <p>If set to true, the developer can store additional file(s) for non relevant information. */
	public static boolean isVerboseStorageEnabled() {
		return Param.VERBOSE_STORE;
	}

	/** Makes the thread wait for ms milliseconds. */
	public static void waiting(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
	
	/** Returns the instant when the experiment started in local time.
	 * <p>Returns 0 if the experiment has not started.*/
	public static long getExperimentStartTime() {
		return Param.startTime;
	}

	/** Returns the instant when a specific UAV has finished the experiment in local time (starting time is not 0).
	 * <p>Returns 0 if the experiment has not finished jet, or it has not even started. */
	public static long getExperimentEndTime(int numUAV) {
		return Param.testEndTime[numUAV];
	}
	
	/** Returns the path followed by the UAV in screen in UTM coordinates.
	 * <p>Useful to log protocol data related to the path followed by the UAV, once the experiment has finished and the UAV is on the ground.*/
	public static List<LogPoint> getUTMPath(int numUAV) {
		return SimParam.uavUTMPath[numUAV];
	}
	
}
