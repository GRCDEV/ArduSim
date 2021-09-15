package com.api.communications.lowLevel;

import java.util.ArrayDeque;

/** This implementation is used for the virtual wireless link, and it is thread-safe.
 * @author Francisco Jos&eacute; fabra Collado
*/

class MessageQueue{
	/**
	 * Queue with messages
	 */
	private final ArrayDeque<Message> queue;
	/**
	 * Size of queue expressed in bytes
	 */
	private int byteSize = 0;

	public MessageQueue() {
		this.queue = new ArrayDeque<>(CommLinkObjectSimulation.RECEIVING_BUFFER_PACKET_SIZE);
	}

	/**
	 * Inserts a new message in the queue.
	 * If the queue is full, it ignores the message and return false.
	 */
	public synchronized boolean offerLast(Message m) {
		if (byteSize + m.message.length <= CommLinkObjectSimulation.receivingBufferSize) {
			queue.addLast(m);
			byteSize = byteSize + m.message.length;
			return true;
		}
		return false;
	}

	/**
	 * Returns but not removes the next element in the queue.
	 * Returns null in case queue is empty.
	 */
	public synchronized Message peekFirst() {
		return this.queue.peekFirst();
	}
	
	/**
	 * Removes and returns the next element in the queue.
	 * Returns null in case queue is empty
	 */
	public synchronized Message pollFirst() {
		Message m = this.queue.pollFirst();
		if(m != null) {
			this.byteSize = this.byteSize - m.message.length;
		}
		return m;
	}

	/**
	 * @return true is queue is empty
	 */
	public synchronized boolean isEmpty() {
		return this.queue.isEmpty();
	}
	
}
