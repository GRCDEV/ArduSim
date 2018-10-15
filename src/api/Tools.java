package api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.List;

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

public class Tools {
	
	// TCP parameter: maximum size of the byte array used on messages
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	/** ArduSim runs in a real multicopter. */
	public static final int MULTICOPTER = 0;
	/** ArduSim runs a simulation. */
	public static final int SIMULATOR = 1;
	/** ArduSim runs as a PC Companion to control real multicopters. */
	public static final int PCCOMPANION = 2;
	
	/** Returns the role ArduSim is performing. It can be compared to one of the following values to make decissions:
	 * <p>Tools.MULTICOPTER
	 * <p>Tools.SIMULATOR
	 * <p>Tools.PCCOMPANION */
	public static int getArduSimRole() {
		return Param.role;
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
	public static void setProtocolConfigured() {
		Param.simStatus = SimulatorState.STARTING_UAVS;
	}
	
	/** Returns true if the UAVs are located (GPS fix) and prepared to receive commands. */
	public static boolean areUAVsAvailable() {
		return Param.simStatus != Param.SimulatorState.STARTING_UAVS
				&& Param.simStatus != Param.SimulatorState.CONFIGURING_PROTOCOL
				&& Param.simStatus != Param.SimulatorState.CONFIGURING;
	}
	
	/** Returns true if the UAVs are available and ready for the setup step, which has not been started jet. */
	public static boolean areUAVsReadyForSetup() {
		return Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED;
	}
	
	/** Returns true while the setup step is in progress. */
	public static boolean isSetupInProgress() {
		return Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS;
	}
	
	/** Returns true if the setup step has finished but the experiment has not started. */
	public static boolean isSetupFinished() {
		return Param.simStatus == Param.SimulatorState.READY_FOR_TEST;
	}
	
	/** Returns true while the experiment is in progress. */
	public static boolean isExperimentInProgress() {
		return Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS;
	}
	
	/** Returns true if the experiment is finished (all UAVs running in the same machine have landed). */
	public static boolean isExperimentFinished() {
		return Param.simStatus == Param.SimulatorState.TEST_FINISHED;
	}
	
	/** Sets the loaded missions from file/s for the UAVs, in geographic coordinates.
	 * <p>Missions must be loaded and set in the protocol configuration dialog when needed.
	 * <p>Please, check that the length of the array is the same as the number of running UAVs in the same machine.*/
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
	 * <p>Mission only available if it is previously sent to the drone with sendMission(int,List<Waypoint>) and retrieved with retrieveMission(int). This steps can be performed automatically with the function api.Copter.cleanAndSendMissionToUAV().*/
	public static List<Waypoint> getUAVMission(int numUAV) {
		return UAVParam.currentGeoMission[numUAV];
	}

	/** Provides the simplified mission shown in the screen in UTM coordinates.
	 * <p>Mission only available if previously is sent to the drone with sendMission(int,List<Waypoint>) and retrieved with retrieveMission(int).*/ 
	public static List<WaypointSimplified> getUAVMissionSimplified(int numUAV) {
		return UAVParam.missionUTMSimplified.get(numUAV);
	}
	
	/** Provides the UDP port used by real UAVs for communication. This is useful in the PC Companion dialog, to listen data packets from the protocol.*/
	public static int getUDPBroadcastPort() {
		return UAVParam.broadcastPort;
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
	 *  <p>It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function geoToUTM is previously used, in order to get the zone and the letter of the UTM projection. */
	public static GeoCoordinates UTMToGeo(double x, double y) {
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
	
	/** Transforms UTM coordinates to Geographic coordinates. 
	 *  <p>Example: Tools.UTMToGeo(312915.84, 4451481.33).
	 *  <p>It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function geoToUTM is previously used, in order to get the zone and the letter of the UTM projection. */
	public static GeoCoordinates UTMToGeo(UTMCoordinates location) {
		return Tools.UTMToGeo(location.x, location.y);
	}

	/** Transforms Geographic coordinates to UTM coordinates. */
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
	
		if (SimParam.zone < 0) {
			SimParam.zone = Zone;
			SimParam.letter = Letter;
		}
		
		return new UTMCoordinates(x, y);
	}
	
	/** Transforms Geographic coordinates to UTM coordinates. */
	public static UTMCoordinates geoToUTM(GeoCoordinates location) {
		return Tools.geoToUTM(location.latitude, location.longitude);
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

	/** Rounds a double number to "places" decimal digits. */
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
			if (x < 1024 || x > UAVParam.MAX_PORT) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/** Validates a boolean value. */
	public static boolean isValidBoolean(String validating) {
		if (validating == null) {
			return false;
		}
		if (!validating.equalsIgnoreCase("true") && !validating.equalsIgnoreCase("false")) {
			return false;
		}
		return true;
	}

	/** Validates a positive integer number. */
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
		return Param.verboseStore;
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
