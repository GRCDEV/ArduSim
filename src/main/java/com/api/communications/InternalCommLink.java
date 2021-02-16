package com.api.communications;

import com.setup.Param;
import com.uavController.UAVParam;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * UAV-to-UAV communications link.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class InternalCommLink {
	
	private static volatile AtomicReferenceArray<InternalCommLink> privateCommLink = null;
	private static final Object lockComm = new Object();
	
	/**
	 * Get the communication link that ArduSim uses internally for UAV-to-UAV communications.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Communication link used by the UAV to communicate with the other UAVs.
	 */
	public static InternalCommLink getCommLink(int numUAV) {
		
		synchronized (lockComm) {
			if (InternalCommLink.privateCommLink == null) {
				InternalCommLink.privateCommLink = new AtomicReferenceArray<>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					InternalCommLink.privateCommLink.set(i, new InternalCommLink(i, Param.numUAVs, UAVParam.broadcastInternalPort));
				}
			}
		}
		
		return InternalCommLink.privateCommLink.get(numUAV);
	}
	
	private int numUAV;
	private static volatile CommLinkObject ardusimCommLink = null;
	private static final Object ARDUSIM_LOCK = new Object();
	
	
	@SuppressWarnings("unused")
	private InternalCommLink() {}
	
	public InternalCommLink(int numUAV, int numUAVs, int port) {
		this.numUAV = numUAV;
		
		synchronized(ARDUSIM_LOCK) {
			if (InternalCommLink.ardusimCommLink == null) {
				InternalCommLink.ardusimCommLink = new CommLinkObject(numUAVs, port);
			}
		}
		
	}
	
	/**
	 * Send a message to other UAVs from the talker thread.
	 * <p>Blocking method. Depending on the system implementation, the message can also be received by the sender UAV. Please, avoid sending messages from more than one thread at a time.</p>
	 * @param message Message to be sent, and encoded by the protocol.
	 */
	public void sendBroadcastMessage(byte[] message) {
		
		InternalCommLink.ardusimCommLink.sendBroadcastMessage(numUAV, message);
		
	}
	
	/**
	 * Receive a message from another UAV from the listener thread.
	 * <p>Blocking method. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @return The message received, or null if a fatal error with the socket happens.
	 */
	public byte[] receiveMessage() {
		
		return InternalCommLink.ardusimCommLink.receiveMessage(numUAV, 0);
		
	}
	
	/**
	 * Receive a message from another UAV from the listener thread.
	 * <p>Blocking method until the socketTimeout is reached. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @param socketTimeout (ms) Duration of the timeout. A zero or negative value blocks until a message is received.
	 * @return The message received, or null if a fatal error with the socket happens or the timeout is reached.
	 */
	public byte[] receiveMessage(int socketTimeout) {
		
		return InternalCommLink.ardusimCommLink.receiveMessage(numUAV, socketTimeout);
		
	}
	
	/** 
	 * Generates a String representation of the communication statistics, once the experiment has finished.
	 */
	@Override
	public String toString() {
		
		return InternalCommLink.ardusimCommLink.toString();
		
	}
	
}
