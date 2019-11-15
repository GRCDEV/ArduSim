package main.api;

import java.util.Map;

import org.javatuples.Pair;

import api.pojo.location.Location2DUTM;
import main.ArduSimTools;
import main.Text;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;
import main.api.masterslavepattern.safeTakeOff.TakeOffMasterDataListenerThread;
import main.uavController.UAVParam;

/**
 * Functions provided to define and use flight formations.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FlightFormationTools {
	
	/**
	 * Get the available formations to be able to show them in any configuration dialog.
	 * @return Array with the name of the formations available to build flight formations.
	 */
	public String[] getAvailableFormations() {
		Formation[] formations = Formation.values();
		String[] res =new  String[formations.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = formations[i].getName();
		}
		
		return res;
	}
	
	/**
	 * Get which will be the UAV in the center of the flight formation. Use this in the master UAV only, which will be located in the position 0 in the data arrays (see documentation). This function is provided to be used in <i>setStartingLocation</i> method of the protocol implementation. This could be useful to know which would be the center UAV while flying before performing the safe take off (see example in protocol <i>MUSCOP</i>).
	 * @param groundLocations Location and ID of each of all the UAVs, including the UAV that could be in the center of the flight formation.
	 * @param formationYaw (rad) Yaw that will be set for the flight formation.
	 * @param isCenterUAV Whether the master UAV will be in the center of the flight formation or not. In the first case, the flight formation will be built around the current location of the master UAV.
	 * @return ID and air location of the UAV that will be in the center of the flight formation.
	 */
	public Pair<Long, Location2DUTM> getCenterUAV(Map<Long, Location2DUTM> groundLocations,
			double formationYaw, boolean isCenterUAV) {
		
		ArduSimTools.logGlobal(Text.TAKEOFF_ALGORITHM_IN_USE + " " + TakeOffMasterDataListenerThread.selectedAlgorithm.getName());
		
		return TakeOffMasterDataListenerThread.getCenterUAV(groundLocations, formationYaw, isCenterUAV);
		
	}
	
	/**
	 * Get the formation that will be used during flight.
	 * @param numUAVs Number of UAVs that will take part in the formation (at minimum 1).
	 * @return The flight formation to be used during flight, or null if <i>numUAVs</i> < 1.
	 */
	public FlightFormation getFlyingFormation(int numUAVs) {
		if (numUAVs < 1) {
			return null;
		}
		
		Formation formation = UAVParam.airFormation.get();
		double distance = UAVParam.airDistanceBetweenUAV;
		return FlightFormation.getFormation(formation, numUAVs, distance);
		
	}
	
	/**
	 * Get the minimum distance between UAVs during flight.
	 * @return (m) the minimum distance between UAVs during flight.
	 */
	public double getFlyingFormationMinimumDistance() {
		return UAVParam.airDistanceBetweenUAV;
	}
	
	/**
	 * Get the name of the underlying Formation used during flight.
	 * @return The name of the Formation used during flight.
	 */
	public String getFlyingFormationName() {
		return UAVParam.airFormation.get().getName();
	}
	
	/**
	 * Get the formation that will be used to deploy the UAVs in simulations.
	 * @param numUAVs Number of UAVs that will take part in the formation (at minimum 1).
	 * @return The flight formation to be used to deploy the UAVs in simulations, or null if <i>numUAVs</i> < 1.
	 */
	public FlightFormation getGroundFormation(int numUAVs) {
		if (numUAVs < 1) {
			return null;
		}
		
		Formation formation = UAVParam.groundFormation.get();
		double distance = UAVParam.groundDistanceBetweenUAV;
		return FlightFormation.getFormation(formation, numUAVs, distance);
	}
	
	/**
	 * Get the minimum distance between UAVs in the deployment on the ground used for simulations.
	 * @return (m) the minimum distance between UAVs while they are on the ground in simulations.
	 */
	public double getGroundFormationMinimumDistance() {
		return UAVParam.groundDistanceBetweenUAV;
	}
	
	/**
	 * Get the name of the underlying Formation used to deploy the UAVs in simulations.
	 * @return The name of the Formation used to deploy the UAVs in simulations.
	 */
	public String getGroundFormationName() {
		return UAVParam.groundFormation.get().getName();
	}
	
	/**
	 * Get the minimum distance between UAVs when they get closer to land.
	 * @return (m) the minimum distance between UAVs while they get closer to land.
	 */
	public double getLandingFormationMinimumDistance() {
		return UAVParam.landDistanceBetweenUAV;
	}
	
	/**
	 * Set the flight formation that must be used during the flight. This must be used in the configuration dialog.
	 * @param name Name of the Formation.
	 * @param minDistance (m) minimum distance between the UAVs included in the formation.
	 */
	public void setFlyingFormation(String name, double minDistance) {
		UAVParam.airFormation.set(Formation.getFormation(name));
		UAVParam.airDistanceBetweenUAV = minDistance;
	}
	
	/**
	 * Set the flight formation that must be used to deploy virtual UAVs in a simulation. This must be used in the configuration dialog.
	 * @param name Name of the Formation.
	 * @param minDistance (m) minimum distance between the UAVs included in deployment.
	 */
	public void setGroundFormation(String name, double minDistance) {
		UAVParam.groundFormation.set(Formation.getFormation(name));
		UAVParam.groundDistanceBetweenUAV = minDistance;
	}
	
	/**
	 * Set the minimum distance between UAV for landing keeping the flight formation.
	 * @param minDistance (m) minimum distance between the UAVs included in the flight formation.
	 */
	public void setLandingFormationMinimumDistance(double minDistance) {
		UAVParam.landDistanceBetweenUAV = minDistance;
	}
	
	
	
}
