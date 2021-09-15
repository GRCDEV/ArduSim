package com.api.copter;

/**
 * Interface that must be implemented in order to apply actions when a UAV reaches a target location.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public interface MoveToListener {

	/**
	 * Actions to perform when the UAV reaches the target location.
	 */
    void onCompleteActionPerformed();
	
	/**
	 * Actions to perform if some error happens during the take off.
	 */
    void onFailure();
}
