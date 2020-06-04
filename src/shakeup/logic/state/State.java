package shakeup.logic.state;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import shakeup.pojo.Param;

public abstract class State {
	
	protected int numUAVs;
	protected int selfId;
	protected boolean isMaster;
	protected GUI gui;
	protected Copter copter;
	protected ArduSim ardusim;
	
	protected short stateNr;
	protected boolean send_ack;
	protected Set<Long> acknowledged;
	
	private boolean transit;
	private boolean executedOnce = false;
	
	protected byte[] outBuffer;
	protected Output output;
	
	public State(int selfId, boolean isMaster) {
		this.selfId = selfId;
		this.isMaster = isMaster;
		this.gui = API.getGUI(selfId);
		this.copter = API.getCopter(selfId);
		this.ardusim = API.getArduSim(); 
		
		acknowledged = new HashSet<Long>();
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
	}
	
	public void processMessage(Input message) {
		// reset the flags
		transit = false;
		
		short type = message.readShort();
		
		if(isMaster) {
			if(type == Param.MESSAGE_ACK && message.readShort() == stateNr) {
				acknowledged.add(message.readLong());
				if(acknowledged.size() == (numUAVs -1)) {transit = true;}
			}
		}else {
			if(type == stateNr) {
				inspect(message);
				send_ack = true;
			}else if(type == stateNr+1) {
				send_ack = false;
				transit = true;
			}
		}
		
		
	}

	public State handle() {
		if(!executedOnce) {
			executedOnce = true;
			executeOnce();
		}
		executeContinously();
		
		// The flag transit does not mean that the state will always changes
		// some additional conditions must be met as well and those are implemented in the transit method
		State currentState = transit(transit);
		return currentState;	
	};
	
	public abstract void inspect(Input message);
	public abstract void executeOnce();
	public abstract void executeContinously();
	public abstract State transit(Boolean transit);
	
	// This method is sometimes overwritten as a message is more complex 
	public byte[][] createMessage(){
		if(isMaster) {
			// The master sends in which state he is
			byte message[][] = new byte[1][];
			output.reset();
			output.writeShort(stateNr);
			message[0] = Arrays.copyOf(outBuffer, output.position());
			return message;
		}else {
			if(send_ack) {
				// The slaves reply with an ack if they have received the state of the master
				output.reset();
				output.writeShort(Param.MESSAGE_ACK);
				output.writeShort(stateNr);
				output.writeLong(selfId);
				output.flush();
				byte message[][] = new byte[1][];
				message[0] = Arrays.copyOf(outBuffer, output.position());
				return message;
			}
		}
		return null;
	};
	
	public int getStateNr() {return stateNr;}
}
