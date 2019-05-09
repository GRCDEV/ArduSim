package api;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Main;
import main.Param;
import main.Param.SimulatorState;
import sim.logic.SimParam;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class Tools {
	
	/**
	 * TCP parameter: maximum size of the byte array used on messages.
	 * It is based on the Ethernet MTU, and assumes that IP and UDP protocols are used. */
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	/** ArduSim runs in a real multicopter. */
	public static final int MULTICOPTER = 0;
	/** ArduSim runs a simulation. */
	public static final int SIMULATOR = 1;
	/** ArduSim runs as a PC Companion to control real multicopters. */
	public static final int PCCOMPANION = 2;
	
	/**
	 * Get the role performed by ArduSim.
	 * @return The role ArduSim is performing. It can be compared to one of the following values to make decisions:
	 * <p>Tools.MULTICOPTER, Tools.SIMULATOR, or Tools.PCCOMPANION</p>
	 */
	public static int getArduSimRole() {
		return Param.role;
	}
	
	/**
	 * Get the number of multicopters running on the same machine.
	 * @return The number of UAVs that are running on the same machine.
	 * <p> 1 When running on a real UAV (arrays are of size 1 and numUAV==0), or nunUAVs when running a simulation on a PC.</p>
	 */
	public static int getNumUAVs() {
		return Param.numUAVs;
	}
	
	/**
	 * Set the number of UAVs running on the same machine.
	 * <p>Only use this method in the protocol configuration dialog, and when a parameter limits the number of UAVs that must be simulated.</p>
	 * @param numUAVs The number of UAVs running on the same machine
	 */
	public static void setNumUAVs(int numUAVs) {
		Param.numUAVsTemp.set(numUAVs);
	}
	
	/**
	 * Get the ID of a multicopter.
	 * @param numUAV UAV position in arrays.
	 * @return The ID of a multicopter.
	 * On real UAV returns a value based on the MAC address.
	 * On virtual UAV it returns the position of the UAV in the arrays used by the simulator.
	 */
	public static long getIdFromPos(int numUAV) {
		return Param.id[numUAV];
	}
	
	/**
	 * Use this function to assert that the configuration of the protocol has finished when the corresponding dialog is closed.
	 * <p>In order the parameters of the protocol to work properly, please establish default values for all of them to be used automatically when ArduSim is loaded.</p> */
	public static void setProtocolConfigured() {
		Param.simStatus = SimulatorState.STARTING_UAVS;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) ready to receive commands.
	 * @return true if all UAVs are located (GPS fix) and prepared to receive commands.
	 */
	public static boolean areUAVsAvailable() {
		return Param.simStatus != Param.SimulatorState.STARTING_UAVS
				&& Param.simStatus != Param.SimulatorState.CONFIGURING_PROTOCOL
				&& Param.simStatus != Param.SimulatorState.CONFIGURING;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) ready to press the setup button.
	 * @return true if the UAVs are available and ready for the setup step, which has not been started jet.
	 */
	public static boolean areUAVsReadyForSetup() {
		return Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) performing the setup step.
	 * @return true while the setup step is in progress.
	 */
	public static boolean isSetupInProgress() {
		return Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) has(have) finished the setup step.
	 * @return true if the setup step has finished but the experiment has not started.
	 */
	public static boolean isSetupFinished() {
		return Param.simStatus == Param.SimulatorState.READY_FOR_TEST;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) performing the experiment.
	 * @return true while the experiment is in progress.
	 */
	public static boolean isExperimentInProgress() {
		return Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS;
	}
	
	/**
	 * Find out if the multicopter(s) has(have) finished the experiment.
	 * @return true if the experiment is finished (all UAVs running in the same machine have landed).
	 */
	public static boolean isExperimentFinished() {
		return Param.simStatus == Param.SimulatorState.TEST_FINISHED;
	}
	
	/**
	 * Set the loaded missions from file/s for the UAVs, in geographic coordinates.
	 * <p>Please, check that the length of the array is the same as the number of running UAVs in the same machine (method <i>getNumUAVs()</i>).</p>
	 * @param missions Missions that must be loaded and set with this method in the protocol configuration window, when needed.
	 */
	public static void setLoadedMissionsFromFile(List<Waypoint>[] missions) {
		UAVParam.missionGeoLoaded = missions;
	}
	
	/**
	 * Get the missions loaded from files in geographic coordinates.
	 * @return Array with the missions of the UAVs. They are only available when they are set with the method <i>setLoadedMissionsFromFile(List<Waypoint>[])</i>. If this method is used before, it returns null.
	 */
	public static List<Waypoint>[] getLoadedMissions() {
		return UAVParam.missionGeoLoaded;
	}

	/**
	 * Get the mission stored on the multicopter.
	 * <p>Mission only available if it is previously sent to the drone with <i>Copter.sendMission(int,List&lt;Waypoint&gt;)</i> and retrieved with <i>Copter.retrieveMission(int)</i>.
	 * This steps can be performed automatically with the function <i>Copter.cleanAndSendMissionToUAV(int,List&lt;Waypoint&gt;)</i>.</p>
	 * @param numUAV UAV position in arrays.
	 * @return The mission currently stored in the UAV in geographic coordinates.
	 */
	public static List<Waypoint> getUAVMission(int numUAV) {
		return UAVParam.currentGeoMission[numUAV];
	}

	/**
	 * Get the mission shown on screen.
	 * <p>Mission only available if it is previously sent to the drone with <i>Copter.sendMission(int,List&lt;Waypoint&gt;)</i> and retrieved with <i>Copter.retrieveMission(int)</i>.
	 * This steps can be performed automatically with the function <i>Copter.cleanAndSendMissionToUAV(int,List&lt;Waypoint&gt;)</i>.</p>
	 * @param numUAV UAV position in arrays.
	 * @return The simplified mission shown in the screen in UTM coordinates.
	 */
	public static List<WaypointSimplified> getUAVMissionSimplified(int numUAV) {
		return UAVParam.missionUTMSimplified.get(numUAV);
	}
	
	/**
	 * Get the UTM coordinates of the last waypoint of the current mission.
	 * @param numUAV UAV position in arrays.
	 * @return The UTM coordinates of the last waypoint of the current mission, or <i>null</i> if the UAV is not following a mission.
	 */
	public static UTMCoordinates getUAVLastWaypointUTM(int numUAV) {
		return UAVParam.lastWPUTM[numUAV];
	}
	
	/**
	 * Get the last waypoint of the current mission.
	 * @param numUAV UAV position in arrays.
	 * @return The last waypoint of the current mission, or <i>null</i> if the UAV is not following a mission.
	 */
	public static Waypoint getUAVLastWaypoint(int numUAV) {
		return UAVParam.lastWP[numUAV];
	}
	
	/**
	 * Get the UDP port used for communications.
	 * <p>This is useful in the PC Companion dialog, to listen data packets from the protocol.</p>
	 * @return the UDP port used by real UAVs for communication.
	 */
	public static int getUDPBroadcastPort() {
		return UAVParam.broadcastPort;
	}
	
	/**
	 * Find out if possible UAV collisions are being detected.
	 * @return true if the UAV collision check is enabled.
	 */
	public static boolean isCollisionCheckEnabled() {
		return UAVParam.collisionCheckEnabled;
	}
	
	/**
	 * Get the security distance used to assert that a collision has happened.
	 * @return The maximum ground distance between two UAVs to assert that a collision has happened.
	 */
	public static double getCollisionHorizontalDistance() {
		return UAVParam.collisionDistance;
	}
	
	/**
	 * Get the security vertical distance used to assert that a collision has happened.
	 * @return The maximum vertical distance between two UAVs to assert that a collision has happened.
	 */
	public static double getCollisionVerticalDistance() {
		return UAVParam.collisionAltitudeDifference;
	}
	
	/**
	 * Find out if at least a collision between multicopters has happened.
	 * @return true if a collision has happened.
	 */
	public static boolean isCollisionDetected() {
		return UAVParam.collisionDetected;
	}
	
	/**
	 * Transform UTM coordinates into Geographic coordinates.
	 * @param x East (meters).
	 * @param y North (meters).
	 * @return Coordinates in Geographic coordinate system.
	 * It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function <i>geoToUTM</i> is previously used (in order to get the zone and the letter of the UTM projection).
	 * Otherwise, it returns null.
	 */
	public static GeoCoordinates UTMToGeo(double x, double y) {
		if (SimParam.zone == -1) {
			return null;
		}
		
		double latitude;
		double longitude;
	
		double Hem;
		if (SimParam.letter > 'M')
			Hem = 'N';
		else
			Hem = 'S';
		double north;
		if (Hem == 'S')
			north = y - 10000000;
		else
			north = y;
		latitude = (north / 6366197.724 / 0.9996 + (1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) - 0.006739496742 * Math.sin(north / 6366197.724 / 0.9996) * Math.cos(north / 6366197.724 / 0.9996) * (Math.atan(Math.cos(Math.atan((Math.exp((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996) * 3 / 2) * (Math.atan(Math.cos(Math.atan((Math.exp((x - 500000) / (0.9996 * 6399593.625/ Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow( Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996)) * 180 / Math.PI;
		latitude = Math.round(latitude * 10000000);
		latitude = latitude / 10000000;
		longitude = Math.atan((Math.exp((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) * 180 / Math.PI + SimParam.zone * 6 - 183;
		longitude = Math.round(longitude * 10000000);
		longitude = longitude / 10000000;
	
		return new GeoCoordinates(latitude, longitude);
	}
	
	/**
	 * Transform UTM coordinates into Geographic coordinates.
	 * @param location (meters).
	 * @return Coordinates in Geographic coordinate system.
	 * It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function <i>geoToUTM</i> is previously used (in order to get the zone and the letter of the UTM projection).
	 * Otherwise, it returns null.
	 */
	public static GeoCoordinates UTMToGeo(UTMCoordinates location) {
		return Tools.UTMToGeo(location.x, location.y);
	}

	/**
	 * Transform Geographic coordinates into UTM coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @return Coordinates in UTM coordinate system.
	 */
	public static UTMCoordinates geoToUTM(double latitude, double longitude) {
		double x;
		double y;
		int Zone = (int) Math.floor(longitude / 6 + 31);
		char Letter;
		if (latitude < -72)		Letter = 'C';
		else if (latitude < -64)	Letter = 'D';
		else if (latitude < -56)	Letter = 'E';
		else if (latitude < -48)	Letter = 'F';
		else if (latitude < -40)	Letter = 'G';
		else if (latitude < -32)	Letter = 'H';
		else if (latitude < -24)	Letter = 'J';
		else if (latitude < -16)	Letter = 'K';
		else if (latitude < -8)	Letter = 'L';
		else if (latitude < 0)	Letter = 'M';
		else if (latitude < 8)	Letter = 'N';
		else if (latitude < 16)	Letter = 'P';
		else if (latitude < 24)	Letter = 'Q';
		else if (latitude < 32)	Letter = 'R';
		else if (latitude < 40)	Letter = 'S';
		else if (latitude < 48)	Letter = 'T';
		else if (latitude < 56)	Letter = 'U';
		else if (latitude < 64)	Letter = 'V';
		else if (latitude < 72)	Letter = 'W';
		else				Letter = 'X';
		x = 0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2) / 3) + 500000;
		x = Math.round(x * 100) * 0.01;
		y = (Math.atan(Math.tan(latitude * Math.PI / 180) / Math.cos((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) - latitude * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (latitude * Math.PI / 180 - 0.005054622556 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 4 + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 3);
		if (Letter < 'M')
			y = y + 10000000;
		y = Math.round(y * 100) * 0.01;
	
		SimParam.zone = Zone;
		SimParam.letter = Letter;
		
		return new UTMCoordinates(x, y);
	}
	
	/**
	 * Transform Geographic coordinates into UTM coordinates.
	 * @param location (geographic coordinates).
	 * @return Coordinates in UTM coordinate system.
	 */
	public static UTMCoordinates geoToUTM(GeoCoordinates location) {
		return Tools.geoToUTM(location.latitude, location.longitude);
	}

	/**
	 * Format a time range to h:mm.ss.
	 * @param start Initial value retrieved with the method <i>System.curentTimeMillis()</i>.
	 * @param end Final value retrieved with the method <i>System.curentTimeMillis()</i>.
	 * @return String representation in h:mm:ss format.
	 */
	public static String timeToString(long start, long end) {
		long time = Math.abs(end - start);
		long h = time/3600000;
		time = time - h*3600000;
		long m = time/60000;
		time = time - m*60000;
		long s = time/1000;
		return h + ":" + String.format("%02d", m) + ":" + String.format("%02d", s);
	}

	/**
	 * Round a double number to "places" decimal digits.
	 * @param value Value to be rounded.
	 * @param places Target decimal places.
	 * @return Rounded value.
	 */
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	/**
	 * Validate a TCP port.
	 * @param validating String representation of the TCP port.
	 * @return true if the String represents a valid TCP port.
	 */
	public static boolean isValidPort(String validating) {
		if (validating == null) {
			return false;
		}
	
		try {
			int x = Integer.parseInt(validating);
			if (x < 1024 || x > UAVParam.MAX_PORT) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a boolean String.
	 * @param validating String representation of the boolean value.
	 * @return true if the String represents a valid boolean.
	 */
	public static boolean isValidBoolean(String validating) {
		if (validating == null) {
			return false;
		}
		if (!validating.equalsIgnoreCase("true") && !validating.equalsIgnoreCase("false")) {
			return false;
		}
		return true;
	}

	/**
	 * Validate a positive integer number.
	 * @param validating String representation of a positive integer.
	 * @return true if the String represents a valid positive integer.
	 */
	public static boolean isValidPositiveInteger(String validating) {
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
	
	/**
	 * Validate a non negative integer number.
	 * @param validating String representation of a non negative integer.
	 * @return true if the String represents a valid non negative integer.
	 */
	public static boolean isValidNonNegativeInteger(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			int x = Integer.parseInt(validating);
			if (x < 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Validate a positive double number.
	 * @param validating String representation of a positive double.
	 * @return true if the String represents a valid positive double.
	 */
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

	/**
	 * Validate a double number.
	 * @param validating String representation of a double.
	 * @return true if the String represents a valid double.
	 */
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

	/**
	 * Get the folder where ArduSim is running.
	 * @return The folder where ArduSim is running (.jar folder, or project root if running in eclipse).
	 */
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
	
	/**
	 * Parse an ini file to retrieve parameters for a protocol.
	 * @return Map with parameters and their respective value. Empty map if the file has no parameters or it is invalid.
	 */
	public static Map<String, String> parseINIFile(File iniFile) {
		Map<String, String> parameters = new HashMap<>();
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(iniFile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			return parameters;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			return parameters;
		}
		List<String> checkedLines = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.length() > 0 && !line.startsWith("#") && (line.length() - line.replace("=", "").length() == 1)) {
				checkedLines.add(line);
			}
		}
		if (checkedLines.size() > 0) {
			String key, value;
			String[] pair;
			for (int i = 0; i < checkedLines.size(); i++) {
				pair = checkedLines.get(i).split("=");
				key = pair[0].trim().toUpperCase();
				value = pair[1].trim();
				parameters.put(key, value);
			}
		}
		return parameters;
	}
	
	/**
	 * Get a file extension.
	 * @param file The file to be checked.
	 * @return The file extension, or empty string if the file has not extension.
	 */
	public static String getFileExtension(File file) {
		String fileName = file.getName();
		if(fileName.lastIndexOf(".") > 0) {
			return fileName.substring(fileName.lastIndexOf(".")+1);
		} else {
			return "";
		}
	}

	/**
	 * Store text in a file.
	 * @param destination File to store the text.
	 * @param text Text to be stored in the file.
	 */
	public static void storeFile(File destination, String text) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(destination);
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
	
	/**
	 * Find out if the verbose store feature is enabled.
	 * <p>If set to true, the developer can store additional file(s) for non relevant information.</p>
	 * @return true if verbose store feature is enabled.
	 */
	public static boolean isVerboseStorageEnabled() {
		return Param.verboseStore;
	}

	/**
	 * Make this Thread wait.
	 * @param ms Amount of time to wait in milliseconds.
	 */
	public static void waiting(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Get the experiment starting time.
	 * @return The instant when the experiment was started in Java VM time.
	 * It also returns 0 if the experiment has not started.
	 */
	public static long getExperimentStartTime() {
		return Param.startTime;
	}

	/**
	 * Get the experiment end time.
	 * @param numUAV UAV position in arrays.
	 * @return The instant when a specific UAV has finished the experiment in Java VM time in milliseconds.
	 * Returns 0 if the experiment has not finished jet for the multicopter <i>numUAV</i>, or it has not even started.
	 */
	public static long getExperimentEndTime(int numUAV) {
		return Param.testEndTime[numUAV];
	}
	
	/**
	 * Get the path followed by the multicopter during the experiment.
	 * <p>Useful to log protocol data related to the path followed by the UAV.
	 * If needed, it is suggested to use this method once the experiment has finished and the UAV is on the ground.</p>
	 * @param numUAV UAV position in arrays.
	 * @return The path followed by the UAV during the experiment in UTM coordinates.
	 */
	public static List<LogPoint> getUTMPath(int numUAV) {
		return SimParam.uavUTMPath[numUAV];
	}

}
