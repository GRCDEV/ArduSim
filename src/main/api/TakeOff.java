package main.api;

import api.API;
import api.pojo.FlightMode;
import main.Param;
import main.Text;
import main.api.hiddenFunctions.HiddenFunctions;
import main.uavController.UAVParam;

/**
 * Thread used to take off a UAV and detect when it reaches the target altitude.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOff extends Thread {
	
	private static final long CHECK_PERIOD = 250;		// (ms) Time between two checks
	private static final long LOG_PERIOD = 1000;		// (ms) Time between log lines
	private static final long HOVERING_TIMEOUT = 1000;	// (ms) Time to hover at the end of the take off before accepting new commands

	private int numUAV;
	private double relAltitude;
	private double minAltitude;
	private TakeOffListener listener;
	
	@SuppressWarnings("unused")
	private TakeOff() {}
	
	public TakeOff(int numUAV, double relAltitude, TakeOffListener listener) {
		super(Text.TAKEOFF_THREAD + numUAV);
		this.numUAV = numUAV;
		this.relAltitude = relAltitude;
		this.minAltitude = relAltitude - Copter.getAltitudeGPSError(relAltitude);
		this.listener = listener;
	}

	@Override
	public void run() {
		
		ArduSim ardusim = API.getArduSim();
		Copter copter = API.getCopter(numUAV);
		GUI gui = API.getGUI(numUAV);
		
		if (UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
				|| !copter.setFlightMode(FlightMode.LOITER)
				|| !HiddenFunctions.stabilize(numUAV)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			listener.onFailure();
			return;
		}
		
		// We need to wait the stabilize method to take effect
		ardusim.sleep(TakeOff.HOVERING_TIMEOUT);
		
		if (!copter.setFlightMode(FlightMode.GUIDED)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			listener.onFailure();
			return;
		}
		
		if (!HiddenFunctions.armEngines(numUAV)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			listener.onFailure();
			return;
		}
		
		if (!HiddenFunctions.takeOffGuided(numUAV, relAltitude)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			listener.onFailure();
			return;
		}
		
		long cicleTime = System.currentTimeMillis();
		long logTime = cicleTime;
		long waitTime;
		boolean goOn = true;
		while (goOn) {
			if (UAVParam.uavCurrentData[numUAV].getZRelative() >= minAltitude) {
				goOn = false;
			} else {
				if (System.currentTimeMillis() - logTime > TakeOff.LOG_PERIOD) {
					gui.logVerboseUAV(Text.ALTITUDE_TEXT
							+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ())
							+ " " + Text.METERS);
					logTime = logTime + TakeOff.LOG_PERIOD;
				}
				
				cicleTime = cicleTime + TakeOff.CHECK_PERIOD;
				waitTime = cicleTime - System.currentTimeMillis();
				if (waitTime > 0) {
					ardusim.sleep(waitTime);
				}
			}
		}
		
		listener.onCompleteActionPerformed();
		
	}
}
