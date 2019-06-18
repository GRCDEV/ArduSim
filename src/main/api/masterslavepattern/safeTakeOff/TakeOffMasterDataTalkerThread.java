package main.api.masterslavepattern.safeTakeOff;

import java.util.concurrent.atomic.AtomicInteger;

import api.API;
import main.Text;
import main.api.ArduSim;
import main.api.GUI;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

/**
 * Thread used by the master UAV to coordinate the safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOffMasterDataTalkerThread extends Thread {
	
	private byte[][] messages;
	private AtomicInteger state;
	private InternalCommLink commLink;
	private GUI gui;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private TakeOffMasterDataTalkerThread() {}
	
	public TakeOffMasterDataTalkerThread(int numUAV, byte[][] messages, AtomicInteger state) {
		super(Text.SAFE_TAKE_OFF_MASTER_CONTEXT_TALKER + numUAV);
		this.messages = messages;
		this.state = state;
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.gui = API.getGUI(numUAV);
		this.ardusim = API.getArduSim();
	}

	@Override
	public void run() {
		
		gui.logVerboseUAV(MSText.MASTER_TALKER_SENDING_TAKE_OFF_DATA);
		long cicleTime = System.currentTimeMillis();
		long waitingTime;
		
		while (state.get() == MSParam.STATE_SENDING_DATA) {
			for (int i = 0; i < messages.length; i++) {
				commLink.sendBroadcastMessage(messages[i]);
			}
			
			cicleTime = cicleTime + MSParam.SENDING_PERIOD;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		
		gui.logVerboseUAV(MSText.MASTER_TALKER_TAKE_OFF_DATA_END);
	}

}
