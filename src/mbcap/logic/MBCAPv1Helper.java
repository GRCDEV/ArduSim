package mbcap.logic;

public class MBCAPv1Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V1;
	}
	
}
