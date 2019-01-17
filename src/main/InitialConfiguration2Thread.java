package main;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import api.Copter;
import api.ProtocolHelper;
import api.pojo.Waypoint;
import uavController.UAVParam;

/** This class sends the initial configuration to all UAVs, asynchronously.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

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
		// Load mission if needed
		if (UAVParam.missionGeoLoaded != null) {
			List<Waypoint> mission = UAVParam.missionGeoLoaded[numUAV];
			if (mission != null) {
				if (!Copter.cleanAndSendMissionToUAV(numUAV, mission)) {
					return;
				}
				if (Waypoint.waypointDelay != 0 && !Copter.setParameter(numUAV, UAVParam.ControllerParam.WPNAV_RADIUS, Waypoint.waypointDistance)) {
					return;
				}
			}
		}
		
		// Actions needed by the specific protocol
		if (!ProtocolHelper.selectedProtocolInstance.sendInitialConfiguration(numUAV)) {
			return;
		}
		
		// Configuration successful
		InitialConfiguration2Thread.UAVS_CONFIGURED.incrementAndGet();
	}
}
