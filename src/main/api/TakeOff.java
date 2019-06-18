package main.api;

import api.API;
import api.pojo.FlightMode;
import main.Param;
import main.Text;
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
		super("TakeOff thread");
		this.numUAV = numUAV;
		this.relAltitude = relAltitude;
		this.minAltitude = relAltitude - Copter.getAltitudeGPSError(relAltitude);
		this.listener = listener;
	}

	@Override
	public void run() {
		
		int a = 0;
		if (a == 0) {
			this.versionBuena();//TODO probar ambas versiones y quitar la peor
		} else {
			this.versionMala();
		}
		
	}
	
	private void versionBuena() {
		
		ArduSim ardusim = API.getArduSim();
		Copter copter = API.getCopter(numUAV);
		GUI gui = API.getGUI(numUAV);
		
		if (UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
				|| !copter.setFlightMode(FlightMode.LOITER)
				|| !copter.stabilize()) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			System.out.println("Error fase 1");//TODO limpiar debugging
			listener.onFailureListener();
			return;
		}
		
		// We need to wait the stabilize method to take effect
		ardusim.sleep(TakeOff.HOVERING_TIMEOUT);
		
		if (!copter.setFlightMode(FlightMode.GUIDED)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			System.out.println("Error para guided");
			listener.onFailureListener();
			return;
		}
		
		if (!copter.armEngines()) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			System.out.println("Error para armar");
			listener.onFailureListener();
			return;
		}
		
		if (!copter.takeOffGuided(relAltitude)) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			System.out.println("Error al iniciar despegue");
			listener.onFailureListener();
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
		
		listener.onCompletedListener();
		
	}
	
	private void versionMala() {
		
		ArduSim ardusim = API.getArduSim();
		Copter copter = API.getCopter(numUAV);
		GUI gui = API.getGUI(numUAV);
		
		if (UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
				|| !copter.setFlightMode(FlightMode.GUIDED)
				|| !copter.armEngines()
				|| !copter.takeOffGuided(relAltitude)
				) {
			gui.logUAV(Text.TAKE_OFF_ERROR + " " + Param.id[numUAV]);
			System.out.println("Error fase 1");//TODO limpiar debugging
			listener.onFailureListener();
			return;
		}
		
		long cicleTime = System.currentTimeMillis();
		long logTime = cicleTime;
		long waitTime;
		boolean goOn = true;
		while (goOn) {
			if (System.currentTimeMillis() - cicleTime > TakeOff.CHECK_PERIOD) {
				
				if (UAVParam.uavCurrentData[numUAV].getZRelative() >= minAltitude) {
					goOn = false;
				} else {
					cicleTime = cicleTime + TakeOff.CHECK_PERIOD;
					
					if (System.currentTimeMillis() - logTime > TakeOff.LOG_PERIOD) {
						gui.logVerboseUAV(Text.ALTITUDE_TEXT
								+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ())
								+ " " + Text.METERS);
						logTime = logTime + TakeOff.LOG_PERIOD;
					}
				}
			}
			
			if (goOn) {
				waitTime = cicleTime - System.currentTimeMillis();
				if (waitTime > 0) {
					ardusim.sleep(waitTime);
				}
			}
		}
		
		if (!copter.setFlightMode(FlightMode.LOITER) || !copter.stabilize())  {
			gui.logUAV(Text.TAKE_OFF_ERROR);
			System.out.println("Error fase 2");//TODO limpiar debugging
			listener.onFailureListener();
			return;
		}
		
		// We need to wait the stabilize metho to take effect
		ardusim.sleep(TakeOff.HOVERING_TIMEOUT);
		
		if (!copter.setFlightMode(FlightMode.GUIDED)) {
			gui.logUAV(Text.TAKE_OFF_ERROR);
			System.out.println("Error fase 3");//TODO limpiar debugging
			listener.onFailureListener();
			return;
		}
		
		listener.onCompletedListener();
		
	}
	
	
	
}
