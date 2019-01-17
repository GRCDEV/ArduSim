package mbcap.logic;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class MBCAPv2Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V2;
	}

}
