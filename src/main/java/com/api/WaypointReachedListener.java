package com.api;

/** Implementing this interface enables a Class to execute code each time the UAV reaches a waypoint.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public interface WaypointReachedListener {
	
	/**
	 * This method is executed each time a UAV reaches a waypoint. You must check if the information corresponds to the current UAV.
	 * <p>This method is NOT thread-safe and the included code must take this into account.</p>
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @param numSeq Sequence number of the waypoint reached.
	 */
	void onWaypointReachedActionPerformed(int numUAV, int numSeq);
	
}
