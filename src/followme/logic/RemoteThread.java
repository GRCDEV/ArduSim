package followme.logic;

import api.API;
import api.pojo.FlightMode;
import followme.pojo.RemoteInput;
import main.Param;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static followme.pojo.State.LANDING;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class RemoteThread extends Thread {
	
	private AtomicInteger currentState;
	
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private RemoteThread() {}
	
	public RemoteThread(int numUAV) {
		this.currentState = FollowMeParam.state[numUAV];
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
		this.ardusim = API.getArduSim();
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
			if (!copter.setFlightMode(FlightMode.LOITER)) {
				gui.logUAV(FollowMeText.MASTER_LOITER_ERROR);
				return;
			}
			
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
					}
					
					if (!FollowMeTalkerThread.protocolStarted || altitude >= finalAltitude) {
						copter.channelsOverride(data.roll, data.pitch, data.throttle, data.yaw);
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
