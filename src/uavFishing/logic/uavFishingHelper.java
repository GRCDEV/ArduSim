package uavFishing.logic;

import org.javatuples.Pair;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;


public class uavFishingHelper {

	/** Opens the configuration dialog of the pollution protocol. */
	public static void openConfigurationDialog() {
		SwarmHelper.log("Cargar misión para el barco");
		
	}
	
	/** Initializes data structures related to the pollution protocol. */
	public static void initializeDataStructures() {} //TODO
	
	public static Pair<GeoCoordinates, Double>[] getStartingLocation() {
		
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = new Pair<GeoCoordinates, Double>(uavFishingParam.startLocationBoat,0.0);
		startCoordinatesArray[1] = new Pair<GeoCoordinates, Double>(uavFishingParam.startLocationUAV,0.0);
		
		return startCoordinatesArray;
	} 
	
	public static void launchThreads() {} //TODO
	
	public static boolean sendBasicConfig(int numUAV) {
		
		boolean success = false;
		//TODO
		success = true;
		
		return success;
	
	}
	
	public static void startSwarmTestActionPerformed() {}//TODO
	
	
}
