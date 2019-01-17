package mbcapOLD.pojo;

import mbcap.logic.MBCAPParam.MBCAPState;

/** This class generates objects with the MBCAP protocol state and the instant when the data was retrieved. */

public class OLDProgressState {
	public MBCAPState state;
	public Long time;

	@SuppressWarnings("unused")
	private OLDProgressState() {}
	
	public OLDProgressState(MBCAPState state, Long time) {
		this.state = state;
		this.time = time;
	}
}
