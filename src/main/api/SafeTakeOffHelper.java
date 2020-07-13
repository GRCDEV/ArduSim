package main.api;

import api.API;
import es.upv.grc.mapper.Location2DUTM;
import main.api.formations.FlightFormation;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.safeTakeOff.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Functions used to perform a coordinated and safe take off based on the master-slave strategy.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SafeTakeOffHelper {
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private SafeTakeOffHelper() {}
	
	public SafeTakeOffHelper(int numUAV) {
		this.numUAV = numUAV;
	}
	
	/**
	 * Get the name of all the available take-off algorithms. This method can be used in the protocol configuration dialog.
	 * @return An array with the name of the available algorithms.
	 */
	public String[] getAvailableTakeOffAlgorithms() {
		return TakeOffAlgorithm.getAvailableAlgorithms();
	}
	
	/**
	 * Based on the master-slave strategy. This blocking method allows the master UAV to coordinate with a group of slaves for a safe take off process. All the UAVs (master/slaves) must join to the process. If it is not needed to take off at all, it can be specified.
	 * @param groundLocations List of slaves detected by the master, including their ID, and their location on the ground.
	 * @param flightFormation Flight formation to use while flying.
	 * @param formationYaw (rad) Initial yaw of the flight formation.
	 * @param targetAltitude (m) Take off altitude over the home location.
	 * @param isCenterUAV true if the master UAV must be set in the center of the flight formation. Otherwise, the center UAV will be the one that minimizes the risk of collision among them.
	 * @param exclude true if the master UAV must NOT take off, and only is in charge of coordinating the process.
	 * @return Context with useful information related to the take off process, and that allows to perform the take off with the method <i>start</i>.
	 */
	public SafeTakeOffContext getMasterContext(Map<Long, Location2DUTM> groundLocations,
			FlightFormation flightFormation, double formationYaw, double targetAltitude, boolean isCenterUAV, boolean exclude) {

		AtomicReference<SafeTakeOffContext> result = new AtomicReference<>();

		TakeOffMasterDataListenerThread listener = new TakeOffMasterDataListenerThread(numUAV, groundLocations, flightFormation, formationYaw, targetAltitude, isCenterUAV, exclude, result);
		listener.start();
		try {
			listener.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		API.getArduSim().sleep(MSParam.TAKE_OFF_DATA_TIMEOUT);	//Wait slaves timeout
		return result.get();
	}

	/**
	 * Based on the master-slave strategy. This blocking method allows a slave UAV to coordinate with a master and a group of slave UAVs for a safe take off. All the UAVs (master/slaves) must join the process. If it is not needed to take off at all, it can be specified.
	 * @param exclude true if the slave UAV must NOT take off, and only wants to coordinate with the other UAVs.
	 * @return Context with useful information related to the take off process, and that allows to perform the take off with the method <i>start</i>.
	 */
	public SafeTakeOffContext getSlaveContext(boolean exclude) {

		AtomicReference<SafeTakeOffContext> result = new AtomicReference<>();

		TakeOffSlaveDataListenerThread listener = new TakeOffSlaveDataListenerThread(numUAV, exclude, result);
		listener.start();
		try {
			listener.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result.get();
	}
	
	/**
	 * Get the take-off algorithm to be used. This method can be used in the protocol configuration dialog.
	 * @return Name of the take-off algorithm strategy to be used.
	 */
	public String getTakeOffAlgorithm() {
		return TakeOffMasterDataListenerThread.selectedAlgorithm.getName();
	}
	
	/**
	 * Set the take-off algorithm to be used. This method can be used in the protocol configuration dialog.
	 * @param name Name of the take-off algorithm strategy to be used.
	 */
	public void setTakeOffAlgorithm(String name) {
		TakeOffMasterDataListenerThread.selectedAlgorithm = TakeOffAlgorithm.getAlgorithm(name);
	}

	/**
	 * Based on the master-slave strategy. This non blocking method allows to start a safe take off, given a provided take off context.
	 * @param safeTakeOffContext Context that contains the parameters needed for a safe take off.
	 * @param takeOffListener Listener that can be used to detect when the take off finishes.
	 */
	public void start(SafeTakeOffContext safeTakeOffContext, SafeTakeOffListener takeOffListener) {

		new SafeTakeOffListenerThread(numUAV, safeTakeOffContext, takeOffListener).start();

	}
	
}
