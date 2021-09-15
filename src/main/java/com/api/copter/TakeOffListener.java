package com.api.copter;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public interface TakeOffListener {
	
	/**
	 * Actions to perform when the take off finishes.
	 */
	void onCompleteActionPerformed();
	
	/**
	 * Actions to perform if some error happens during the take off.
	 */
	void onFailure();
	
}
