package main.communications;

import uavController.UAVParam;

public class CommLink {
	
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
	 * Send a message to the other UAVs.
	 * <p>Blocking method. Depending on the system implementation, the message can also be received by the sender UAV. Please, avoid sending messages from more than one thread.</p>
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
	 * @param socketTimeout (milliseconds) Duration of the timeout. a zero or negative value blocks until a message is received.
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
