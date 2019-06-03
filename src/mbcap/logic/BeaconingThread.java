package mbcap.logic;

import api.API;
import main.api.ArduSim;
import main.api.Copter;
import main.api.WaypointReachedListener;
import main.api.communications.CommLink;
import mbcap.gui.MBCAPGUIParam;
import mbcap.pojo.Beacon;

/** This class sends data packets to other UAVs, by real or simulated broadcast, so others can detect risk of collision.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class BeaconingThread extends Thread implements WaypointReachedListener {
	
	boolean finished = false;
	double distance = Double.MAX_VALUE;
	
	private int numUAV; // UAV identifier, beginning from 0
	private CommLink link;
	private Copter copter;
	private ArduSim ardusim;

	@SuppressWarnings("unused")
	private BeaconingThread() {}

	public BeaconingThread(int numUAV) {
		this.numUAV = numUAV;
		this.link = API.getCommLink(numUAV);
		this.copter = API.getCopter(numUAV);
		this.ardusim = API.getArduSim();
	}
	
	@Override
	public void onWaypointReached(int numUAV, int numSeq) {
		// Project the predicted path over the planned mission
		if (this.numUAV == numUAV) {
			MBCAPParam.projectPath.set(numUAV, 1);
		}
	}

	@Override
	public void run() {
		while (!ardusim.isExperimentInProgress() || !copter.isFlying()) {
			ardusim.sleep(MBCAPParam.SHORT_WAITING_TIME);
		}
		
		Beacon selfBeacon = null;
		byte[] sendBuffer = null;
		long waitingTime;
		short prevState = 0;

		// Send beacons while the UAV is flying during the experiment
		// The protocol is stopped when two UAVs collide
		long cicleTime = System.currentTimeMillis();
		ArduSim ardusim = API.getArduSim();
		while (ardusim.isExperimentInProgress() && copter.isFlying()
				&& !ardusim.collisionIsDetected()) {
			// Each beacon is sent a number of times before renewing the predicted positions
			for (long i = 0; i < MBCAPParam.numBeacons; i++) {
				
				// The first time it is needed to calculate the predicted positions
				if (i == 0) {
					selfBeacon = Beacon.buildToSend(numUAV);
					MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
					sendBuffer = selfBeacon.getBuffer();
					prevState = selfBeacon.state;

					// Beacon store for logging purposes
					if (ardusim.isVerboseStorageEnabled()
							&& ardusim.getExperimentEndTime()[numUAV] == 0) {
						MBCAPParam.beaconsStored[numUAV].add(new Beacon(selfBeacon));
					}
				} else {
					// In any other case, only time, state and idAvoiding are updated
					sendBuffer = selfBeacon.getBufferUpdated();
					// Create a new beacon if the state is changed
					if (selfBeacon.state != prevState) {
						selfBeacon = Beacon.buildToSend(numUAV);
						MBCAPParam.selfBeacon.set(numUAV, selfBeacon);
						sendBuffer = selfBeacon.getBuffer();
						prevState = selfBeacon.state;

						// Beacon store for logging purposes
						if (ardusim.isVerboseStorageEnabled()
								&& ardusim.getExperimentEndTime()[numUAV] == 0) {
							MBCAPParam.beaconsStored[numUAV].add(new Beacon(selfBeacon));
						}
						i = 0;	// Reset value to start again
					}
				}
				if (!selfBeacon.points.isEmpty()) {
					link.sendBroadcastMessage(sendBuffer);
				}

				cicleTime = cicleTime + MBCAPParam.beaconingPeriod;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		
		// Stop sending and drawing future positions when the UAV lands
		MBCAPGUIParam.predictedLocation.set(numUAV, null);
	}

}
