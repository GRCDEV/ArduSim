package mbcap.logic;

import api.ProtocolHelper;

public class MBCAPv4Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocol = ProtocolHelper.Protocol.MBCAP_V4;
	}
	
}
