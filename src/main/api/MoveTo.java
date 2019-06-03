package main.api;

import api.API;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2DUTM;
import main.Text;
import main.uavController.UAVParam;

/**
 * Thread used to move a UAV to a target location and detect when it arrives there.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MoveTo extends Thread {

	private static final double TARGET_THRESHOLD = 1.0;	// (m) Distance to target location to assert that the UAV has reached it.
	
	private int numUAV;
	private Location2DGeo targetLocationGeo;
	private Location2DUTM targetLocationUTM;
	private double relAltitude;
	private double altitudeThreshold;
	private MoveToListener listener;
	
	@SuppressWarnings("unused")
	private MoveTo() {}
	
	public MoveTo(int numUAV, Location2DGeo targetLocation, double relAltitude, MoveToListener listener) {
		this.numUAV = numUAV;
		this.targetLocationGeo = targetLocation;
		this.targetLocationUTM = targetLocation.getUTM();
		this.relAltitude = relAltitude;
		this.altitudeThreshold = Copter.getAltitudeGPSError(relAltitude);
		this.listener = listener;
	}

	@Override
	public void run() {
		
		ArduSim ardusim = API.getArduSim();
		GUI gui = API.getGUI(numUAV);
		
		UAVParam.newLocation[numUAV][0] = (float)targetLocationGeo.latitude;
		UAVParam.newLocation[numUAV][1] = (float)targetLocationGeo.longitude;
		UAVParam.newLocation[numUAV][2] = (float)relAltitude;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			gui.logUAV(Text.MOVE_TO_ERROR);
			listener.onFailureListener();
			return;
		}
		
		while (UAVParam.uavCurrentData[numUAV].getUTMLocation().distance(targetLocationUTM) > MoveTo.TARGET_THRESHOLD
				|| Math.abs(UAVParam.uavCurrentData[numUAV].getZRelative() - relAltitude) > altitudeThreshold) {
			ardusim.sleep(UAVParam.STABILIZATION_WAIT_TIME);
		}
		
		listener.onCompletedListener();
	}
	
}
