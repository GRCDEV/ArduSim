package com.protocols.mbcap.pojo;

/** This class generates objects with the MBCAP protocol state and the instant when the data was retrieved.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ProgressState {
	public MBCAPState state;
	public Long time;

	@SuppressWarnings("unused")
	private ProgressState() {}
	
	public ProgressState(MBCAPState state, Long time) {
		this.state = state;
		this.time = time;
	}
}
