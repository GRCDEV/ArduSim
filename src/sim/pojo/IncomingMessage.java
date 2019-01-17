package sim.pojo;

import java.util.concurrent.atomic.AtomicBoolean;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class IncomingMessage implements Comparable<IncomingMessage> {
	public long start, end;				// (ns) Starting and ending time of the transmission period
	public int senderPos;				// Position of the UAV on the Simulator internal arrays
	public byte[] message;				// Message to be sent
	public AtomicBoolean checked = new AtomicBoolean();	// Variables used to check if there are collisions on the receiving buffer
	public AtomicBoolean overlapped = new AtomicBoolean();
	public AtomicBoolean alreadyOverlapped = new AtomicBoolean();
	
	private IncomingMessage() {}
	
	/** Creates a just sent message from the UAV on senderPos, generated on startTime and with message content. */
	public IncomingMessage(int senderPos, long startTime, byte[] message) {
		this.senderPos = senderPos;
		this.message = message;
		this.start = startTime;
		this.end = this.start + 20000 + 4000 * ((message.length + 61) / 3);
	}
	
	/** Two incoming messages are equal if the came from the same UAV and started on the same instant. */
	@Override
	public boolean equals(Object obj) {
		// No more comparisons are needed, as the sender will never send two messages with the same starting time
		if (!(obj instanceof IncomingMessage)) {
			return false;
		}
		IncomingMessage m = (IncomingMessage)obj;
		if (this.senderPos == m.senderPos && this.start == m.start) {
			return true;
		}
		return false;
	}

	/** This incoming message is less than o if it starts before o. */
	@Override
	public int compareTo(IncomingMessage o) {
		int res = (int)(this.start - o.start);
		if (res == 0) {
			return this.senderPos - o.senderPos;
		} else {
			return res;
		}
	}

	@Override
	public IncomingMessage clone() {
		IncomingMessage res = new IncomingMessage();
		res.start = this.start;
		res.end = this.end;
		res.senderPos = this.senderPos;
		res.message = this.message;
		res.overlapped.set(this.overlapped.get());
		res.checked.set(this.checked.get());
		return res;
	}
	
	
}
