package com.setup;

import com.api.API;
import com.api.ArduSimTools;
import com.api.copter.Copter;
import com.api.copter.CopterParam;
import com.api.pojo.location.Waypoint;
import com.setup.sim.gui.MissionKmlSimProperties;
import com.uavController.UAVParam;

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
		
		// Load main.java.com.protocols.mission if needed
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
