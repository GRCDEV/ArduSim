package mbcap.logic;

public class MBCAPv4Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocolString = MBCAPText.MBCAP_V4;
	}
	
}
