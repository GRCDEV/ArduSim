package main.api;

/**
 * Interface that must be implemented to apply actions when the UAV reaches the target altitude. */

public interface TakeOffListener {
	
	/**
	 * Actions to perform when the take off has finished.
	 */
	abstract void onCompletedListener();
	
	/**
	 * Actions to perform if some error happens during the take off.
	 */
	abstract void onFailureListener();
	
}
