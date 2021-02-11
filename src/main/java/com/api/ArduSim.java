package com.api;

import com.api.pojo.location.LogPoint;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;
import com.setup.Param;

import java.util.List;

/**
 * API to interact with ArduSim.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ArduSim {

	/** ArduSim runs in a real multicopter. */
	public static final int MULTICOPTER = 0;
	/** ArduSim runs a simulation with a gui interface. */
	public static final int SIMULATOR_GUI = 1;
	/** ArduSim runs as a PC Companion to control real multicopters. */
	public static final int PCCOMPANION = 2;
	/** ArduSim runs a simulation without a gui interface. */
	public static final int SIMULATOR_CLI = 3;

	/**
	 * Find out if at least a collision between multicopters has happened.
	 * @return true if a collision has happened.
	 */
	public boolean collisionIsDetected() {
		return UAVParam.collisionDetected;
	}
	
	/**
	 * Get the role performed by ArduSim.
	 * @return The role ArduSim is performing. It can be compared to one of the following values to make decisions:
	 * <p>ArduSim.MULTICOPTER (running in a real multicopter), ArduSim.SIMULATOR (running as a simulator), or ArduSim.PCCOMPANION (running as a PC Companion to coordinate real UAVs).</p>
	 */
	public int getArduSimRole() {
		return Param.role;
	}
	
	/**
	 * Get the experiment starting time.
	 * <p>This is useful if you plan to analyze at the end of the experiment data gathered from your protocol.</p>
	 * @return (ms) The instant when the experiment was started in Java VM time. It also returns 0 if the experiment has not started.
	 */
	public long getExperimentStartTime() {
		return Param.startTime;
	}

	/**
	 * Get the experiment end time for all the UAVs running in the same machine.
	 * <p>This is useful if you plan to analyze at the end of the experiment data gathered from your protocol.</p>
	 * @return (ms) The instant when a specific UAV has finished the experiment in Java VM time in milliseconds. Returns 0 if the experiment has not finished jet for a specific multicopter, or it has not even started.
	 */
	public long[] getExperimentEndTime() {
		return Param.testEndTime;
	}
	
	/**
	 * Get the number of multicopters running on the same machine.
	 * @return The number of UAVs that are running on the same machine.
	 * <p> 1 When running on a real UAV, or several when running a simulation on a PC.</p>
	 */
	public int getNumUAVs() {
		return Param.numUAVs;
	}
	
	/**
	 * Get the name of the protocol running in ArduSim.
	 * @return The protocol name.
	 */
	public String getSelectedProtocolName() {
		return ArduSimTools.selectedProtocol;
	}
	
	/**
	 * Get the path followed during the experiment by all the multicopters running on the same machine.
	 * <p>Useful to log protocol data related to the path followed by the UAV.
	 * If you need this, it is suggested to use this method once the experiment has finished and the UAV is on the ground.</p>
	 * @return The path followed by the UAV during the experiment in UTM coordinates.
	 */
	public List<LogPoint>[] getUTMPath() {
		return SimParam.uavUTMPath;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) ready to receive commands.
	 * @return true if all UAVs running in this machine are located (GPS fix) and prepared to receive commands.
	 */
	public boolean isAvailable() {
		return Param.simStatus != Param.SimulatorState.STARTING_UAVS
				&& Param.simStatus != Param.SimulatorState.CONFIGURING_PROTOCOL
				&& Param.simStatus != Param.SimulatorState.CONFIGURING;
	}

	/**
	 * Find out if the multicopter(s) is(are) performing the setup step.
	 * @return true while the setup step is in progress.
	 */
	public boolean isSetupInProgress() {
		return Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) has(have) finished the setup step.
	 * @return true if the setup step has finished but the experiment has not started.
	 */
	public boolean isSetupFinished() {
		return Param.simStatus == Param.SimulatorState.READY_FOR_TEST;
	}
	
	/**
	 * Find out if the multicopter(s) is(are) performing the experiment.
	 * @return true while the experiment is in progress.
	 */
	public boolean isExperimentInProgress() {
		return Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS;
	}

	/**
	 * Find out if the verbose store feature is enabled.
	 * <p>If set to true, the developer can store additional file(s) for non relevant information.</p>
	 * @return true if verbose store feature is enabled.
	 */
	public boolean isStoreDataEnabled() {
		return Param.storeData;
	}
	
	/**
	 * Set the number of UAVs running on the same machine.
	 * <p>Only use this method in the protocol configuration dialog, and when you are using a parameter that limits the number of UAVs that must be simulated.</p>
	 * @param numUAVs The number of UAVs running on the same machine.
	 */
	public void setNumUAVs(int numUAVs) {
		Param.numUAVsTemp.set(numUAVs);
	}
	
	/**
	 * Make this Thread wait.
	 * @param ms Amount of time to wait in milliseconds.
	 */
	public void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {}
	}
	
}
