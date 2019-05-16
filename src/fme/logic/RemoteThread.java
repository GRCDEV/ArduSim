package fme.logic;

import static fme.pojo.State.LANDING;

import java.util.Queue;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import fme.pojo.RemoteInput;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class RemoteThread extends Thread {
	
	private int numUAV;
	
	
	@SuppressWarnings("unused")
	private RemoteThread() {}
	
	public RemoteThread(int numUAV) {
		this.numUAV = numUAV;
	}
	

	@Override
	public void run() {
		double startingAltitude = FMeParam.slavesStartingAltitude;
		double protocolStartAltitude = Copter.getMinTargetAltitude(startingAltitude);
		double finalAltitude = startingAltitude * 0.5;
		double altitude;
		
		if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
			
			while (Copter.getZRelative(numUAV) < protocolStartAltitude) {
				Tools.waiting(FMeParam.STATE_CHANGE_TIMEOUT);
			}
			TalkerThread.protocolStarted = true;
			while (Copter.getZRelative(numUAV) >= finalAltitude) {
				Tools.waiting(FMeParam.STATE_CHANGE_TIMEOUT);
			}
			FMeParam.state.set(numUAV, LANDING);
		}
		
		if (Tools.getArduSimRole() == Tools.SIMULATOR) {
			
			if (!Copter.setFlightMode(numUAV, FlightMode.LOITER)) {
				GUI.log(numUAV, FMeText.MASTER_LOITER_ERROR);
				return;
			}
			
			Queue<RemoteInput> path = FMeParam.masterData;
			RemoteInput data;
			
			boolean landing = false;
			long start = System.nanoTime();
			long wait;
			
			do {
				data = path.poll();
				if (data != null) {
					wait = (data.time - (System.nanoTime() - start)) / 1000000l;
					if (wait > 0) {
						Tools.waiting((int)wait);
					}
					altitude = Copter.getZRelative(numUAV);
					if (!TalkerThread.protocolStarted && altitude >= protocolStartAltitude) {
						TalkerThread.protocolStarted = true;
					}
					
					if (!TalkerThread.protocolStarted || altitude >= finalAltitude) {
						Copter.channelsOverride(numUAV, data.roll, data.pitch, data.throttle, data.yaw);
					} else {
						FMeParam.state.set(numUAV, LANDING);
						landing = true;
					}
				}
			} while (data != null && !landing);
			
			// In case the path depletes before reaching that altitude
			if (!landing) {
				FMeParam.state.set(numUAV, LANDING);
			}
		}

	}

}
