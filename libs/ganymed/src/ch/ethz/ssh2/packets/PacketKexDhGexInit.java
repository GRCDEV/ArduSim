package ch.ethz.ssh2.packets;

import java.math.BigInteger;

/**
 * PacketKexDhGexInit.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketKexDhGexInit.java,v 1.2 2005/08/24 17:54:09 cplattne Exp $
 */
public class PacketKexDhGexInit
{
	byte[] payload;

	BigInteger e;

	public PacketKexDhGexInit(BigInteger e)
	{
		this.e = e;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEX_DH_GEX_INIT);
			tw.writeMPInt(e);
			payload = tw.getBytes();
		}
		return payload;
	}
}
