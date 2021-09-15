package com.protocols.followme.logic;

import com.api.API;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.GUI;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.esotericsoftware.kryo.io.Output;
import com.protocols.followme.pojo.Message;
import es.upv.grc.mapper.Location2DUTM;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.protocols.followme.pojo.State.*;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class FollowMeTalkerThread extends Thread {
	
	private AtomicInteger currentState;
	
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	private LowLevelCommLink link;
	private byte[] outBuffer;
	private Output output;
	private long selfId;

	protected static volatile boolean protocolStarted = false;

	@SuppressWarnings("unused")
	private FollowMeTalkerThread() {}
	
	public FollowMeTalkerThread(int numUAV) {
		super(FollowMeText.TALKER_THREAD + numUAV);
		currentState = FollowMeParam.state[numUAV];
		this.ardusim = API.getArduSim();
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
		this.link = LowLevelCommLink.getCommLink(numUAV);
		this.outBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		this.selfId = numUAV;
	}

	@Override
	public void run() {
		waitUntilProtocolStarted();
		follow();
		waitForLanding();
		land();
		waitForFinish();
		finish();
	}

	private void waitUntilProtocolStarted() {
		gui.logVerboseUAV(FollowMeText.MASTER_WAIT_ALTITUDE);
		while (!protocolStarted) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
	}

	private void follow() {
		while (currentState.get() == FOLLOWING) {
			sendPosition();
			ardusim.sleep(FollowMeParam.sendPeriod);
		}
	}

	private void sendPosition() {
		Location2DUTM here = copter.getLocationUTM();
		double z = copter.getAltitudeRelative();
		double yaw = copter.getHeading();

		output.reset();
		output.writeShort(Message.I_AM_HERE);
		output.writeDouble(here.x);
		output.writeDouble(here.y);
		output.writeDouble(z);
		output.writeDouble(yaw);
		output.flush();
		byte[] message = Arrays.copyOf(outBuffer, output.position());
		link.sendBroadcastMessage(message);
	}

	private void waitForLanding() {
		while (currentState.get() < LANDING) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
	}

	private void land() {
		byte[] message = createMasterSendLandMessage();
		while (currentState.get() == LANDING) {
			link.sendBroadcastMessage(message);
			ardusim.sleep(FollowMeParam.SENDING_TIMEOUT);
		}
	}

	private byte[] createMasterSendLandMessage() {
		Location2DUTM here;
		gui.logVerboseUAV(FollowMeText.MASTER_SEND_LAND);
		output.reset();
		output.writeShort(Message.LAND);
		here = copter.getLocationUTM();
		output.writeDouble(here.x);
		output.writeDouble(here.y);
		output.writeDouble(copter.getHeading());
		output.flush();
		byte[] message = Arrays.copyOf(outBuffer, output.position());
		return message;
	}

	private void waitForFinish() {
		while (currentState.get() < FINISH) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
	}

	private void finish() {
		gui.logVerboseUAV(FollowMeText.TALKER_FINISHED);
	}



}
