package followme.logic;

import static followme.pojo.State.LANDING;

import java.util.Queue;

import api.API;
import api.pojo.FlightMode;
import followme.pojo.RemoteInput;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class RemoteThread extends Thread {
	
	private int numUAV;
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private RemoteThread() {}
	
	public RemoteThread(int numUAV) {
		this.numUAV = numUAV;
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
			TalkerThread.protocolStarted = true;
			while (copter.getAltitudeRelative() >= finalAltitude) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			FollowMeParam.state.set(numUAV, LANDING);
		}
		
		if (role == ArduSim.SIMULATOR) {
			
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
					wait = (data.time - (System.nanoTime() - start)) / 1000000l;
					if (wait > 0) {
						ardusim.sleep(wait);
					}
					altitude = copter.getAltitudeRelative();
					if (!TalkerThread.protocolStarted && altitude >= startingAltitude) {
						TalkerThread.protocolStarted = true;
					}
					
					if (!TalkerThread.protocolStarted || altitude >= finalAltitude) {
						copter.channelsOverride(data.roll, data.pitch, data.throttle, data.yaw);
					} else {
						FollowMeParam.state.set(numUAV, LANDING);
						landing = true;
					}
				}
			} while (data != null && !landing);
			
			// In case the path depletes before reaching that altitude
			if (!landing) {
				FollowMeParam.state.set(numUAV, LANDING);
			}
		}

	}

}
