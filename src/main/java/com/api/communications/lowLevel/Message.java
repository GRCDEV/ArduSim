package com.api.communications.lowLevel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 
 * Virtual communications message.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

class Message implements Comparable<Message> {
	/**
	 * Start time of the transmission period (ns)
	 */
	public long start;
	/**
	 * End time of the transmission Period (ns)
	 */
	public long	end;
	/**
	 * Position of the UAV on the Simulator internal arrays
	 */
	public int senderPos;
	/**
	 * Message to be sent (in bytes)
	 */
	public byte[] message;

	public AtomicBoolean checked = new AtomicBoolean();	// Variables used to check if there are collisions on the receiving buffer
	public AtomicBoolean overlapped = new AtomicBoolean();
	public AtomicBoolean alreadyOverlapped = new AtomicBoolean();

	/** Creates a just sent message from the UAV on senderPos, generated on startTime and with message content. */
	public Message(int senderPos, long startTime, byte[] message) {
		this.senderPos = senderPos;
		this.message = message;
		this.start = startTime;
		this.end = this.start + 20000 + 4000 * ((message.length + 61) / 3);
	}
	
	/**
	 * Not full deep copy of a message. This function is only used to send the same message to different UAVs, and the message content is not copied, as it is no modified later.
	 * @param message Message to copy
	 */
	public Message(Message message) {
		this.start = message.start;
		this.end = message.end;
		this.senderPos = message.senderPos;
		this.message = message.message;	// Alert! It is not a deep copy, as the same message is used on all the UAVs
		this.checked.set(message.checked.get());
		this.overlapped.set(message.overlapped.get());
		this.alreadyOverlapped.set(message.alreadyOverlapped.get()); //Not necessary
	}
	
	/** Two incoming messages are equal if the came from the same UAV and started on the same instant. */
	@Override
	public boolean equals(Object obj) {
		// No more comparisons are needed, as the sender will never send two messages with the same starting time
		if (this == obj) {
			return true;
		}
		
		if (!(obj instanceof Message)) {
			return false;
		}
		Message m = (Message)obj;
		return this.senderPos == m.senderPos && this.start == m.start;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.senderPos, this.start);
	}

	/** This incoming message is less than o if it starts before o. */
	@Override
	public int compareTo(Message o) {
		int res = (int)(this.start - o.start);
		if (res == 0) {
			return this.senderPos - o.senderPos;
		} else {
			return res;
		}
	}
}
