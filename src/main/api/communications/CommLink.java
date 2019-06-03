package main.api.communications;

import main.uavController.UAVParam;

/**
 * UAV-to-UAV communications link.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CommLink {
	
	/**
	 * TCP parameter: maximum size of the byte array used on messages.
	 * It is based on the Ethernet MTU, and assumes that IP and UDP protocols are used. */
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	private int numUAV;
	private int port;
	private static volatile CommLinkObject publicCommLink = null;
	private static final Object PUBLIC_LOCK = new Object();
	
	@SuppressWarnings("unused")
	private CommLink() {}
	
	public CommLink(int numUAV, int numUAVs, int port) {
		this.numUAV = numUAV;
		this.port = port;
		
		if (port == UAVParam.broadcastPort) {
			synchronized(PUBLIC_LOCK) {
				if (CommLink.publicCommLink == null) {
					CommLink.publicCommLink = new CommLinkObject(numUAVs, port);
				}
			}
		}//TODO lo mismo con las comunicaciones internas
		
	}
	
	/**
	 * Send a message to other UAVs.
	 * <p>Blocking method. Depending on the system implementation, the message can also be received by the sender UAV. Please, avoid sending messages from more than one thread at a time.</p>
	 * @param message Message to be sent, and encoded by the protocol.
	 */
	public void sendBroadcastMessage(byte[] message) {
		
		if (port == UAVParam.broadcastPort) {
			CommLink.publicCommLink.sendBroadcastMessage(numUAV, message);
		}
		
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @return The message received, or null if a fatal error with the socket happens.
	 */
	public byte[] receiveMessage() {
		
		if (port == UAVParam.broadcastPort) {
			return CommLink.publicCommLink.receiveMessage(numUAV, 0);
		}
		return null;
		
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method until the socketTimeout is reached. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @param socketTimeout (ms) Duration of the timeout. A zero or negative value blocks until a message is received.
	 * @return The message received, or null if a fatal error with the socket happens or the timeout is reached.
	 */
	public byte[] receiveMessage(int socketTimeout) {
		
		if (port == UAVParam.broadcastPort) {
			return CommLink.publicCommLink.receiveMessage(numUAV, socketTimeout);
		}
		return null;
		
	}

	/** 
	 * Generates a String representation of the communication statistics, once the experiment has finished.
	 */
	@Override
	public String toString() {
		return CommLink.publicCommLink.toString();
	}
	
}
