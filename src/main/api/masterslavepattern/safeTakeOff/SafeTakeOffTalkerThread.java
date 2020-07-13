package main.api.masterslavepattern.safeTakeOff;

import api.API;
import com.esotericsoftware.kryo.io.Output;
import main.Text;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread used to perform a safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SafeTakeOffTalkerThread extends Thread {
	
	private long selfID;
	private long nextID;
	private boolean excluded;
	private boolean isCenter;
	private GUI gui;
	private InternalCommLink commLink;
	private byte[] outBuffer;
	private Output output;
	private ArduSim ardusim;
	private AtomicInteger state;
	
	@SuppressWarnings("unused")
	private SafeTakeOffTalkerThread() {}
	
	public SafeTakeOffTalkerThread(int numUAV, SafeTakeOffContext safeTakeOffInstance, AtomicInteger state) {
		super(Text.SAFE_TAKE_OFF_TAKER + numUAV);
		this.nextID = safeTakeOffInstance.nextID;
		this.excluded = safeTakeOffInstance.excluded;
		this.gui = API.getGUI(numUAV);
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		Copter copter = API.getCopter(numUAV);
		this.selfID = copter.getID();
		this.isCenter = safeTakeOffInstance.masterOrder[0] == this.selfID;
		this.ardusim = API.getArduSim();
		this.state = state;
	}

	@Override
	public void run() {
		
		// WAIT TAKE OFF PHASE
		if (state.get() == MSParam.STATE_WAIT_TAKE_OFF) {
			gui.logVerboseUAV(MSText.TALKER_WAITING);
		}
		while (state.get() < MSParam.STATE_TAKE_OFF_STEP_1) {
			ardusim.sleep(MSParam.SENDING_PERIOD);
		}
		
		// TAKE OFF STEP 1 PHASE */
		if (state.get() == MSParam.STATE_TAKE_OFF_STEP_1) {
			gui.logVerboseUAV(MSText.TALKER_TAKING_OFF_1);
		}
		while (state.get() < MSParam.STATE_TAKE_OFF_STEP_2) {
			ardusim.sleep(MSParam.SENDING_PERIOD);
		}
		
		// TAKE OFF STEP 2 PHASE
		long waitingTime, cicleTime;
		byte[] message;
		if (state.get() == MSParam.STATE_TAKE_OFF_STEP_2) {
			if (nextID == SafeTakeOffContext.BROADCAST_MAC_ID) {
				gui.logVerboseUAV(MSText.TALKER_WAITING_TAKING_OFF_2);
				while (state.get() == MSParam.STATE_TAKE_OFF_STEP_2) {
					ardusim.sleep(MSParam.SENDING_PERIOD);
				}
			} else {
				gui.logVerboseUAV(MSText.TALKER_TAKING_OFF_2);
				output.reset();
				output.writeShort(MSMessageID.TAKE_OFF_NOW);
				output.writeLong(selfID);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (state.get() == MSParam.STATE_TAKE_OFF_STEP_2) {
					commLink.sendBroadcastMessage(message);
					
					cicleTime = cicleTime + MSParam.SENDING_PERIOD;
					waitingTime = cicleTime - System.currentTimeMillis();
					if (waitingTime > 0) {
						ardusim.sleep(waitingTime);
					}
				}
			}
		}
		while (state.get() < MSParam.STATE_TARGET_REACHED) {
			ardusim.sleep(MSParam.SENDING_PERIOD);
		}
		
		// TARGET REACHED PHASE
		if (!isCenter || (excluded && nextID != SafeTakeOffContext.BROADCAST_MAC_ID)) {
			gui.logVerboseUAV(MSText.TALKER_TARGET_REACHED);
			output.reset();
			output.writeShort(MSMessageID.REACHED_ACK);
			output.writeLong(selfID);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (state.get() == MSParam.STATE_TARGET_REACHED) {
				commLink.sendBroadcastMessage(message);
				
				cicleTime = cicleTime + MSParam.SENDING_PERIOD;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MSText.TALKER_WAITING_ALL_REACHED);
			while (state.get() == MSParam.STATE_TARGET_REACHED) {
				ardusim.sleep(MSParam.SENDING_PERIOD);
			}
		}
		while (state.get() < MSParam.STATE_TAKE_OFF_CHECK) {
			ardusim.sleep(MSParam.SENDING_PERIOD);
		}
		
		// TAKE OFF CHECK PHASE
		if (isCenter) {
			gui.logVerboseUAV(MSText.TALKER_CENTER_WAITING_END);
			output.reset();
			output.writeShort(MSMessageID.TAKE_OFF_END);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (state.get() == MSParam.STATE_TAKE_OFF_CHECK) {
				commLink.sendBroadcastMessage(message);
				
				cicleTime = cicleTime + MSParam.SENDING_PERIOD;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MSText.TALKER_NO_CENTER_WAITING_END);
			output.reset();
			output.writeShort(MSMessageID.TAKE_OFF_END_ACK);
			output.writeLong(selfID);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (state.get() == MSParam.STATE_TAKE_OFF_CHECK) {
				commLink.sendBroadcastMessage(message);
				
				cicleTime = cicleTime + MSParam.SENDING_PERIOD;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
	}

}
