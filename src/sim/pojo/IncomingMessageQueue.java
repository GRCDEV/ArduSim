package sim.pojo;

import java.util.ArrayDeque;

import uavController.UAVParam;

/** This implementation is thread-safe.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class IncomingMessageQueue {
	
	private ArrayDeque<IncomingMessage> queue;
	private int byteSize;
	
	/** Inserts a new message in the queue.
	 * <p>If the queue is full, it ignores the message and return false.</p> */
	public synchronized boolean offerLast(IncomingMessage m) {
		if (this.byteSize + m.message.length <= UAVParam.receivingBufferSize) {
			this.queue.addLast(m);
			this.byteSize = this.byteSize + m.message.length;
			return true;
		}
		return false;
	}
	
	public IncomingMessageQueue() {
		this.queue = new ArrayDeque<>(UAVParam.RECEIVING_BUFFER_PACKET_SIZE);
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
