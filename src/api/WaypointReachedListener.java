package api;

/** Implementing this interface enables a Class to execute code each time the UAV "numUAV" reaches a waypoint. */

public interface WaypointReachedListener {
	
	/** Code executed each time the UAV associated to this Class reaches a waypoint.
	 * <p>This method is NOT thread-safe and the included code must take this into account. */
	void onWaypointReached();
	
	/** Gets numUAV, or the position of this UAV in ArduSim arrays. */
	int getNumUAV();
}
