package com.protocols.followme.logic;

import com.api.API;
import com.api.pojo.FlightMode;
import com.protocols.followme.pojo.RemoteInput;
import com.setup.Param;
import com.api.ArduSim;
import com.api.copter.Copter;
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
	double startingAltitude;
	
	@SuppressWarnings("unused")
	private RemoteThread() {}
	
	public RemoteThread(int numUAV) {
		this.currentState = FollowMeParam.state[numUAV];
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
		this.ardusim = API.getArduSim();
		this.numUAV = numUAV;
		startingAltitude = FollowMeParam.slavesStartingAltitude;
	}
	

	@Override
	public void run() {
		int role = API.getArduSim().getArduSimRole();
		if (role == ArduSim.MULTICOPTER) {
			remoteRealFlight(startingAltitude);
		}

		if (role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			Queue<RemoteInput> path = FollowMeParam.masterData;
			RemoteInput data;
			long start = System.nanoTime();
			while(copter.getAltitudeRelative() <= startingAltitude){
				ardusim.sleep(200);
			}
			FollowMeTalkerThread.protocolStarted = true;
			copter.setFlightMode(FlightMode.STABILIZE_ARMED);
			do {
				data = path.poll();
				if (data != null && FollowMeTalkerThread.protocolStarted) {
					wait(data.time, start);
					HiddenFunctions.channelsOverride(numUAV, data.roll, data.pitch, data.throttle, data.yaw);

					if(copter.getAltitudeRelative() <= 3){
						break;
					}
				}
			} while (data != null);
			currentState.set(LANDING);
		}

	}

	private void wait(long remoteTime, long start) {
		long wait;
		wait = (remoteTime - (System.nanoTime() - start)) / 1000000L;
		if (wait > 0) {
			ardusim.sleep(wait);
		}
	}

	private void remoteRealFlight(double startingAltitude) {
		while (copter.getAltitudeRelative() < startingAltitude) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		FollowMeTalkerThread.protocolStarted = true;
		while (copter.getAltitudeRelative() >= startingAltitude * 0.5) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		currentState.set(LANDING);
	}

}
