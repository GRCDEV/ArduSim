package main.api.masterslavepattern.discovery;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public interface DiscoveryProgressListener {
	
	
	/**
	 * Periodic check to analyze if the master UAV has detected all the UAVs that will be running the protocol. For example, the process could finish when the setup button is pressed,, when it detects a specific number of slaves, or when a timeout expires. As this method is periodically executed, please avoid performing heavy calculations.
	 * @param numUAVs Number of UAVs detected until now.
	 * @return You must return true to finish the UAV discovery process.
	 */
	abstract boolean onProgressCheckActionPerformed(int numUAVs);
	
}
