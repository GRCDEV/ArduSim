package com.api.copter;

import com.api.API;
import com.api.ArduSim;
import com.api.GUI;
import com.setup.Text;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location3D;

/**
 * Thread used to move a UAV to a target location and detect when it arrives there.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MoveTo extends Thread {

	private static final double TARGET_THRESHOLD = 1.0;	// (m) Distance to target location to assert that the UAV has reached it.
	
	private int numUAV;
	private Location3D targetLocation;
	private double altitudeThreshold;
	private MoveToListener listener;
	
	@SuppressWarnings("unused")
	private MoveTo() {}
	
	public MoveTo(int numUAV, Location3D targetLocation, MoveToListener listener) {
		this.numUAV = numUAV;
		this.targetLocation = targetLocation;
		this.altitudeThreshold = Copter.getAltitudeGPSError(targetLocation.getAltitude());
		this.listener = listener;
	}

	@Override
	public void run() {
		
		ArduSim ardusim = API.getArduSim();
		GUI gui = API.getGUI(numUAV);
		
		double relAltitude = targetLocation.getAltitude();
		UAVParam.newLocation[numUAV][0] = (float)targetLocation.getLatitude();
		UAVParam.newLocation[numUAV][1] = (float)targetLocation.getLongitude();
		UAVParam.newLocation[numUAV][2] = (float)relAltitude;
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			gui.logUAV(Text.MOVE_TO_ERROR);
			listener.onFailure();
			return;
		}
		
		while (UAVParam.uavCurrentData[numUAV].getUTMLocation().distance(targetLocation) > MoveTo.TARGET_THRESHOLD
				|| Math.abs(UAVParam.uavCurrentData[numUAV].getZRelative() - relAltitude) > altitudeThreshold) {
			ardusim.sleep(UAVParam.STABILIZATION_WAIT_TIME);
		}
		
		listener.onCompleteActionPerformed();
	}
	
}
