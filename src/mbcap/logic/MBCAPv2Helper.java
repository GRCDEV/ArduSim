package mbcap.logic;

import api.ProtocolHelper;

public class MBCAPv2Helper extends MBCAPv3Helper {

	@Override
	public void setProtocol() {
		this.protocol = ProtocolHelper.Protocol.MBCAP_V2;
	}

}
