package followme.logic.comunication;

import java.util.LinkedList;

public class MessageStorage {
	
	public LinkedList<Message> listMsg;
	
	public MessageStorage() {
		this.listMsg = new LinkedList<Message>();
	}
	
	public synchronized Message pop() {
		return listMsg.pop();
	}
	
	public synchronized void put(Message msg) {
		this.listMsg.add(msg);
	}
	
	public synchronized int size() {
		return listMsg.size();
	}

}
