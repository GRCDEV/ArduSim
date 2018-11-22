package mbcap.pojo;

import mbcap.logic.MBCAPParam.MBCAPState;

/** This class generates objects with the MBCAP protocol state and the instant when the data was retrieved.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

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
