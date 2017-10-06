package ch.ethz.ssh2.packets;

import ch.ethz.ssh2.DHGexParameters;

/**
 * PacketKexDhGexRequest.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketKexDhGexRequest.java,v 1.4 2005/08/24 17:54:09 cplattne Exp $
 */
public class PacketKexDhGexRequest
{
	byte[] payload;

	int min;
	int n;
	int max;

	public PacketKexDhGexRequest(DHGexParameters para)
	{
		this.min = para.getMin_group_len();
		this.n = para.getPref_group_len();
		this.max = para.getMax_group_len();
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEX_DH_GEX_REQUEST);
			tw.writeUINT32(min);
			tw.writeUINT32(n);
			tw.writeUINT32(max);
			payload = tw.getBytes();
		}
		return payload;
	}
}
