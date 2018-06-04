package api;

/** Implementing this interface enables a Class to execute code each time the UAV reaches a waypoint. */

public interface WaypointReachedListener {
	
	/** Code executed each time the UAV associated to this Class reaches a waypoint. */
	void onWaypointReached();
	
	/** Gets numUAV, or the position of this UAV in ArduSim arrays. */
	int getNumUAV();
}
