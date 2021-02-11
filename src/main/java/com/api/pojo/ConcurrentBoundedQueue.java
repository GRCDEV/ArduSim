package com.api.pojo;

import java.lang.reflect.Array;

/** Concurrent, generic and bounded FIFO queue.
 * <p>When the queue is filled, the oldest element is removed. Elements cannot be removed. The list of elements stored is provided in the same order they were inserted.</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ConcurrentBoundedQueue<T> {
	
	Class<T> c;
	private int size, count, p, ini;
	private T[] positions;
	
	@SuppressWarnings("unused")
	private ConcurrentBoundedQueue() {}
	
	/**
	 * Creates a bounded FIFO queue of the provided size. Example with Integers:
	 * @param c Class of objects to include in the queue.
	 * @param size Size of the queue.
	 */
	// <p>LastElements&lt;Integer&gt; elements = new LastElements<>(Integer.class, 5);</p>
	@SuppressWarnings("unchecked")
	public ConcurrentBoundedQueue(Class<T> c, int size) {
		this.c = c;
		this.size = size;
		this.count = 0;
		this.p = 0;
		this.positions = (T[]) Array.newInstance(c, size);
	}
	
	/**
	 * Add a new element at the end of the queue.
	 * <p>If the queue is full, the oldest element is removed.</p>
	 * @param element Element to add to the queue.
	 */
	public synchronized void add(T element) {
		positions[p] = element;
		if (this.count < this.size) {
			this.count++;
		}
		
		this.p = (this.p + 1) % this.size;
		
		if (this.count < this.size) {
			this.ini = 0;
		} else {
			this.ini = this.p;
		}
	}
	
	/**
	 * Get an array containing all the current values included in the queue.
	 * @return Array with the elements of the queue.
	 */
	public synchronized T[] getLastValues() {
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(c, this.count);
		int p = this.ini;
		for (int i = 0; i < this.count; i++) {
			res[i] = this.positions[p];
			p = (p + 1) % this.size;
		}
		return res;
	}
}
