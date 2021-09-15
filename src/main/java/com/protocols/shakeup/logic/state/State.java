package com.protocols.shakeup.logic.state;

import com.api.API;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.GUI;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.protocols.shakeup.pojo.Param;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
		this.outBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
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
			}else if(type == stateNr+1) {
				send_ack = false; //TODO check if necessary
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
		State currentState = transit(transit);
		return currentState;	
	}

	public abstract void inspect(Input message);
	public abstract void executeOnce();
	public abstract void executeContinously();
	public abstract State transit(Boolean transit);
	
	// This method is sometimes overwritten as a message is more complex 
	public byte[][] createMessage(){
		if(isMaster) {
			// The master sends in which state he is
			byte[][] message = new byte[1][];
			output.reset();
			output.writeShort(stateNr);
			message[0] = Arrays.copyOf(outBuffer, output.position());
			return message;
		}else {
			// send_ack is set to true inside of the state when some condition is met 
			if(send_ack) {
				// The slaves reply with an ack if they have received the state of the master
				output.reset();
				output.writeShort(Param.MESSAGE_ACK);
				output.writeShort(stateNr);
				output.writeLong(selfId);
				output.flush();
				byte[][] message = new byte[1][];
				message[0] = Arrays.copyOf(outBuffer, output.position());
				return message;
			}
		}
		return null;
	}

	public int getStateNr() {return stateNr;}
}
