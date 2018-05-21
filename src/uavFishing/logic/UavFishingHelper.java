package uavFishing.logic;

import org.javatuples.Pair;

import api.API;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import main.Param;
import main.Param.SimulatorState;
import uavController.UAVParam.Mode;


public class UavFishingHelper {

	/** Opens the configuration dialog of the protocol UAVFISHING. */
	public static void openConfigurationDialog() {
		SwarmHelper.log("Cargar misión para el barco");
		
	}
	
	/** Initializes data structures related to the protocol UAVFISHING. */
	public static void initializeDataStructures() {} //TODO
	
	
	/**
	 * Gets the initial position of the UAVs from the mission of the master and
	 * other source for the other UAVs related to the protocol UAVFISHING.
	 */
	public static Pair<GeoCoordinates, Double>[] getStartingLocation() {
		
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[2];
		startCoordinatesArray[0] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationBoat,0.0);
		startCoordinatesArray[1] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationUAV,0.0);
		
		return startCoordinatesArray;
	} 
	
	//Lanzar los distintos hilos de ejecución
	public static void launchThreads() {
		
		new FisherControllerThread().run();
		new BoatThread().run();
		
		
		
	} //TODO
	
	
	public static boolean sendBasicConfig(int numUAV) {
		//Enviar, si procede, misión para el dron
		boolean success = false;
		//TODO
		success = true;
		
		return success;
	
	}
	
	public static void startTestActionPerformed() {
		
		Param.simStatus = SimulatorState.READY_FOR_TEST;
		//Barco
		API.setMode(0, Mode.STABILIZE);
		API.armEngines(0);
		API.setMode(0, Mode.AUTO);
		API.setThrottle(0);
		//Dron
		API.setMode(1, Mode.STABILIZE);
		API.armEngines(1);
		API.setMode(1, Mode.GUIDED);
		API.doTakeOff(1);
		
		
				
		
	}//TODO
	
	
}
