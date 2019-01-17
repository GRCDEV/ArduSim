package mbcap.logic;

/** This version of the protocol has shown to be worst than v3.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class MBCAPv4Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V4;
	}
	
}
