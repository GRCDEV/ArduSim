package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketServiceAccept.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketServiceAccept.java,v 1.2 2005/08/24 17:54:09 cplattne Exp $
 */
public class PacketServiceAccept
{
	byte[] payload;

	String serviceName;
	
	public PacketServiceAccept(String serviceName)
	{
		this.serviceName = serviceName;
	}

	public PacketServiceAccept(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_SERVICE_ACCEPT)
			throw new IOException("This is not a SSH_MSG_SERVICE_ACCEPT! ("
					+ packet_type + ")");

		serviceName = tr.readString();
		
		if (tr.remain() != 0)
			throw new IOException("Padding in SSH_MSG_SERVICE_ACCEPT packet!");
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_SERVICE_ACCEPT);
			tw.writeString(serviceName);
			payload = tw.getBytes();
		}
		return payload;
	}
}
