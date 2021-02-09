package main.api.communications;

import api.API;
import main.Param;
import main.Text;
import main.api.ArduSim;

/**
 * UAV-to-UAV communications link.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CommLink {
	
	/**
	 * TCP parameter: maximum size of the byte array used on messages.
	 * It is based on the Ethernet MTU, and assumes that IP and UDP protocols are used. */
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	private int numUAV;
	private static volatile CommLinkObject publicCommLink = null;
	private static final Object PUBLIC_LOCK = new Object();
	
	private volatile boolean advertised = false;
	
	@SuppressWarnings("unused")
	private CommLink() {}
	
	public CommLink(int numUAV, int numUAVs, int port) {
		this.numUAV = numUAV;
		
		synchronized(PUBLIC_LOCK) {
			if (CommLink.publicCommLink == null) {
				CommLink.publicCommLink = new CommLinkObject(numUAVs, port);
			}
		}
		
	}
	
	/**
	 * Send a message to other UAVs. This function is only allowed for real or virtual multicopters. In other words, the PC Companion cannot inject messages during the experiment, but it can listen to messages from the real multicopters.
	 * <p>Blocking method. Depending on the system implementation, the message can also be received by the sender UAV. Please, avoid sending messages from more than one thread at a time.</p>
	 * @param message Message to be sent, and encoded by the protocol.
	 */
	public void sendBroadcastMessage(byte[] message) {
		
		if (Param.role == ArduSim.PCCOMPANION) {
			if (!advertised) {
				API.getGUI(0).log(Text.SEND_NOT_PERMITTED);
				advertised = true;
			}
		} else {
			CommLink.publicCommLink.sendBroadcastMessage(numUAV, message);
		}
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @return The message received, or null if a fatal error with the socket happens.
	 */
	public byte[] receiveMessage() {
		
		return CommLink.publicCommLink.receiveMessage(numUAV, 0);
		
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method until the socketTimeout is reached. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @param socketTimeout (ms) Duration of the timeout. A zero or negative value blocks until a message is received.
	 * @return The message received, or null if a fatal error with the socket happens or the timeout is reached.
	 */
	public byte[] receiveMessage(int socketTimeout) {
		
		return CommLink.publicCommLink.receiveMessage(numUAV, socketTimeout);
		
	}

	/** 
	 * Generates a String representation of the communication statistics, once the experiment has finished.
	 */
	@Override
	public String toString() {
		
		return CommLink.publicCommLink.toString();
		
	}
}
