package sim.pojo;

public class IncomingMessage implements Comparable<IncomingMessage> {
	public long start, end;				// (ns) Starting and ending time of the transmission period
	public int senderPos;				// Position of the UAV on the Simulator internal arrays
	public byte[] message;				// Message to be sent
	public boolean overlapped, checked;	// Variables used to check if there are collisions on the receiving buffer
	
	@SuppressWarnings("unused")
	private IncomingMessage() {}
	
	/** Creates a just sent message from the UAV on senderPos, generated on startTime and with message content. */
	public IncomingMessage(int senderPos, long startTime, byte[] message) {
		this.senderPos = senderPos;
		this.message = message;
		this.overlapped = false;
		this.checked = false;
		this.start = startTime;
		// integer roundUp: ru = (a + (b-1))/b ( in this case: (59+length + 2)/3 )
		// roundedUp bytes = ru * 3
//		long tTrans = 20000 + 4000 * ((message.length + 61) / 3);
//		this.end = this.start + tTrans;
//		System.out.println(this.start + " + " + tTrans + " = " + this.end);
		
		this.end = this.start + 20000 + 4000 * ((message.length + 61) / 3);
		
		//System.out.println((long)(this.start * 0.001) + " - " + (long)(this.end * 0.001));
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

	/** This incoming message is less than o if it starts before o.
	 * <p>This comparison does not take into account from which UAV the message was sent. */
	@Override
	public int compareTo(IncomingMessage o) {
		return (int)(this.start - o.start);
	}

	@Override
	public IncomingMessage clone() {
		IncomingMessage res = new IncomingMessage();
		res.start = this.start;
		res.end = this.end;
		res.senderPos = this.senderPos;
		res.message = this.message;
		res.overlapped = this.overlapped;
		res.checked = this.checked;
		return res;
	}
	
	
}
