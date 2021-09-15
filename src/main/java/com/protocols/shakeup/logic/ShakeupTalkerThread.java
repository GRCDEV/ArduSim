package com.protocols.shakeup.logic;

import com.api.API;
import com.api.ArduSim;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.protocols.shakeup.pojo.Param;

public class ShakeupTalkerThread extends Thread{
	
	private final ArduSim ardusim;
	private final LowLevelCommLink link;
	private volatile com.protocols.shakeup.logic.state.State currentState;
	private final int selfId;
	
	public ShakeupTalkerThread(int numUAV) {
		this.ardusim = API.getArduSim();
		this.link = LowLevelCommLink.getCommLink(numUAV);
		this.selfId = numUAV;
	}
	
	public void run() {
		while (!ardusim.isAvailable()) {ardusim.sleep(Param.TIMEOUT);}
		while(API.getCopter(selfId).isFlying()) {
			long before = System.currentTimeMillis();
			
			byte[][] message = currentState.createMessage();
			try {
				for (byte[] bytes : message) {
					link.sendBroadcastMessage(bytes);
				}
			}catch(NullPointerException e) {/* do nothing the message was just empty */ }
			
			long after = System.currentTimeMillis();
			long elapsedTime = after - before;
			
			long waitingTime = Param.SENDING_TIMEOUT - elapsedTime;
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
	}
	
	public synchronized void setState(com.protocols.shakeup.logic.state.State s) {
		// TODO only write when they are not the same but that issues nullpointerexception
		currentState = s;
	}
}
