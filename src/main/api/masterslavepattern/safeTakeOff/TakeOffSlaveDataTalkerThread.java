package main.api.masterslavepattern.safeTakeOff;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import main.Text;
import main.api.ArduSim;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

/**
 * Thread used by a slave to coordinate the safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOffSlaveDataTalkerThread extends Thread {
	
	private long selfID;
	private boolean exclude;
	private AtomicInteger state;
	private InternalCommLink commLink;
	private byte[] outBuffer;
	private Output output;
	private GUI gui;
	private ArduSim ardusim;

	@SuppressWarnings("unused")
	private TakeOffSlaveDataTalkerThread() {}
	
	public TakeOffSlaveDataTalkerThread(int numUAV, boolean exclude, AtomicInteger state) {
		super(Text.SAFE_TAKE_OFF_SLAVE_CONTEXT_TALKER + numUAV);
		this.selfID = API.getCopter(numUAV).getID();
		this.exclude = exclude;
		this.state = state;
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		this.gui = API.getGUI(numUAV);
		this.ardusim = API.getArduSim();
	}
	
	@Override
	public void run() {
		
		gui.logVerboseUAV(MSText.SLAVE_TALKER_SENDING_EXCLUDE);
		output.clear();
		output.writeShort(MSMessageID.EXCLUDE);
		output.writeLong(selfID);
		output.writeBoolean(exclude);
		output.flush();
		byte[] message = Arrays.copyOf(outBuffer, output.position());
		long waitingTime;
		long cicleTime = System.currentTimeMillis();
		while (state.get() == MSParam.STATE_EXCLUDE) {
			commLink.sendBroadcastMessage(message);
			
			cicleTime = cicleTime + MSParam.SENDING_PERIOD;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		
		gui.logVerboseUAV(MSText.SLAVE_TALKER_SENDING_TAKE_OFF_DATA_ACK);
		output.clear();
		output.writeShort(MSMessageID.TAKE_OFF_DATA_ACK);
		output.writeLong(selfID);
		output.flush();
		message = Arrays.copyOf(outBuffer, output.position());
		cicleTime = System.currentTimeMillis();
		while (state.get() == MSParam.STATE_SENDING_DATA) {
			commLink.sendBroadcastMessage(message);
			
			cicleTime = cicleTime + MSParam.SENDING_PERIOD;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		
		gui.logVerboseUAV(MSText.SLAVE_TALKER_TAKE_OFF_DATA_END);
	}

}
