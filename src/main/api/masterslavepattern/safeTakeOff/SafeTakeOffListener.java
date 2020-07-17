package main.api.masterslavepattern.safeTakeOff;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public interface SafeTakeOffListener {

	/**
	 * Define here which action perform when the take off of this UAV finishes. For example, you can modify a shared variable to break a loop used to wait.
	 */
    void onCompleteActionPerformed();
	
}
