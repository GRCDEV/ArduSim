package main;

import api.API;
import api.pojo.CopterParam;
import api.pojo.location.Waypoint;
import main.api.Copter;
import main.sim.gui.MissionKmlSimProperties;
import main.uavController.UAVParam;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** This class sends the initial configuration to all UAVs, asynchronously.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class InitialConfiguration2Thread extends Thread {
	
	public static final AtomicInteger UAVS_CONFIGURED = new AtomicInteger();
	
	private int numUAV;
	
	@SuppressWarnings("unused")
	private InitialConfiguration2Thread() {}
	
	public InitialConfiguration2Thread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		InitialConfiguration2Thread.sendBasicConfiguration(numUAV);
	}
	
	/** Sends the initial configuration: loads missions to a specific UAV, and launches the protocol initial configuration. */
	public static void sendBasicConfiguration(int numUAV) {
		Copter copter = API.getCopter(numUAV);
		
		// Load mission if needed
		if (UAVParam.missionGeoLoaded != null) {
			List<Waypoint> mission = UAVParam.missionGeoLoaded[numUAV];
			if (mission != null) {
				if (!copter.getMissionHelper().updateUAV(mission)) {
					return;
				}
				if (MissionKmlSimProperties.inputMissionDelay != 0 && !copter.setParameter(CopterParam.WPNAV_RADIUS, MissionKmlSimProperties.distanceToWaypointReached)) {
					return;
				}
				if (UAVParam.overrideYaw && !copter.setParameter(CopterParam.WP_YAW_BEHAVIOR, UAVParam.yawBehavior)) {
					return;
				}
			}
		}
		
		// Actions needed by the specific protocol
		if (!ArduSimTools.selectedProtocolInstance.sendInitialConfiguration(numUAV)) {
			return;
		}
		
		// Configuration successful
		InitialConfiguration2Thread.UAVS_CONFIGURED.incrementAndGet();
	}
}
