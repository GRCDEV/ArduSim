package main.api.communications;

import java.util.ArrayDeque;

/** This implementation is used for the virtual wireless link, and it is thread-safe.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class IncomingMessageQueue {
	
	private ArrayDeque<IncomingMessage> queue;
	private int byteSize;
	
	/** Inserts a new message in the queue.
	 * <p>If the queue is full, it ignores the message and return false.</p> */
	public synchronized boolean offerLast(IncomingMessage m) {
		if (this.byteSize + m.message.length <= CommLinkObject.receivingBufferSize) {
			this.queue.addLast(m);
			this.byteSize = this.byteSize + m.message.length;
			return true;
		}
		return false;
	}
	
	public IncomingMessageQueue() {
		this.queue = new ArrayDeque<>(CommLinkObject.RECEIVING_BUFFER_PACKET_SIZE);
		this.byteSize = 0;
	}
	
	/** Returns but not removes the next element in the queue.
	 * <p>Remember to check if the queue is empty before retrieving the element.</p> */
	public synchronized IncomingMessage peekFirst() {
		return this.queue.peekFirst();
	}
	
	/** Removes and returns the next element in the queue.
	 * <p>Remember to check if the queue is empty before removing the element.</p> */
	public synchronized IncomingMessage pollFirst() {
		IncomingMessage m = this.queue.pollFirst();
		this.byteSize = this.byteSize - m.message.length;
		return m;
	}
	
	public synchronized boolean isEmpty() {
		return this.byteSize == 0;
	}
	
	/** Returns the byte size of this queue. */
	public synchronized int size() {
		return this.byteSize;
	}
	
}
