package com.protocols.followme.logic;

import com.api.API;
import com.protocols.followme.pojo.RemoteInput;
import com.setup.Param;
import com.api.ArduSim;
import com.api.Copter;
import com.api.GUI;
import com.api.hiddenFunctions.HiddenFunctions;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.protocols.followme.pojo.State.LANDING;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class RemoteThread extends Thread {
	
	private AtomicInteger currentState;
	
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	private int numUAV;
	
	@SuppressWarnings("unused")
	private RemoteThread() {}
	
	public RemoteThread(int numUAV) {
		this.currentState = FollowMeParam.state[numUAV];
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
		this.ardusim = API.getArduSim();
		this.numUAV = numUAV;
		gui.logUAV("I am the master");
	}
	

	@Override
	public void run() {
		double startingAltitude = FollowMeParam.slavesStartingAltitude;
		double finalAltitude = startingAltitude * 0.5;
		double altitude;
		
		int role = API.getArduSim().getArduSimRole();
		if (role == ArduSim.MULTICOPTER) {
			
			while (copter.getAltitudeRelative() < startingAltitude) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			FollowMeTalkerThread.protocolStarted = true;
			while (copter.getAltitudeRelative() >= finalAltitude) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			currentState.set(LANDING);
		}
		
		if (role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			Queue<RemoteInput> path = FollowMeParam.masterData;
			RemoteInput data;
			
			boolean landing = false;
			long start = System.nanoTime();
			long wait;
			
			do {
				data = path.poll();
				if (data != null) {
					wait = (data.time - (System.nanoTime() - start)) / 1000000L;
					if (wait > 0) {
						ardusim.sleep(wait);
					}
					altitude = copter.getAltitudeRelative();
					if (!FollowMeTalkerThread.protocolStarted && altitude >= startingAltitude) {
						FollowMeTalkerThread.protocolStarted = true;
					}else{
						System.out.println(altitude + ":" + startingAltitude);
					}

					if (!FollowMeTalkerThread.protocolStarted || altitude >= finalAltitude) {
						HiddenFunctions.channelsOverride(numUAV, data.roll, data.pitch, data.throttle, data.yaw);
					} else {
						currentState.set(LANDING);
						landing = true;
					}
				}
			} while (data != null && !landing);
			
			// In case the path depletes before reaching that altitude
			if (!landing) {
				currentState.set(LANDING);
			}
		}

	}

}
