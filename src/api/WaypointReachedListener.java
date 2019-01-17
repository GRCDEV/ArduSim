package api;

/** Implementing this interface enables a Class to execute code each time the UAV "numUAV" reaches a waypoint.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public interface WaypointReachedListener {
	
	/**
	 * This method is executed each time the UAV associated to this Class reaches a waypoint.
	 * <p>This method is NOT thread-safe and the included code must take this into account.</p>
	 * @param numSeq Sequence number of the waypoint reached.
	 */
	void onWaypointReached(int numSeq);
	
	/**
	 * Get the position of the multicopter in the Ardusim arrays (<i>numUAV</i> in many methods of the API).
	 * @return The position of the UAV in the arrays.
	 */
	int getNumUAV();
}
