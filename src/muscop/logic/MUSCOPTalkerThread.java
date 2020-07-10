package muscop.logic;

import api.API;
import com.esotericsoftware.kryo.io.Output;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import muscop.pojo.Message;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static muscop.pojo.State.*;

/** 
 * Thread used to send messages to other UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPTalkerThread extends Thread {
	
	
	private AtomicInteger currentState;
	
	private long selfId;
	private boolean isMaster;
	private boolean isCenter;
	private ArduSim ardusim;
	private Copter copter;
	private GUI gui;
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	private CommLink link;
	// Concurrency variables:
	private AtomicBoolean missionReceived;
	private AtomicBoolean running;
	private AtomicInteger wpReachedSemaphore, moveSemaphore;
	
	private long cicleTime;		// Cicle time used for sending messages
	private long waitingTime;	// (ms) Time to wait between two sent messages
	
	@SuppressWarnings("unused")
	private MUSCOPTalkerThread() {}

	public MUSCOPTalkerThread(int numUAV, boolean isMaster, boolean isCenter,
			AtomicBoolean missionReceived, AtomicInteger wpReachedSemaphore, AtomicInteger moveSemaphore) {
		
		this.running = new AtomicBoolean(true);
		this.currentState = MuscopSimProperties.state[numUAV];
		this.selfId = API.getCopter(numUAV).getID();
		this.isMaster = isMaster;
		this.isCenter = isCenter;
		this.ardusim = API.getArduSim();
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		this.link = API.getCommLink(numUAV);
		this.missionReceived = missionReceived;
		this.wpReachedSemaphore = wpReachedSemaphore;
		this.moveSemaphore = moveSemaphore;
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		if(!running.get()) {return;}
		shareMission();
		takingOff();
		fly();
		land();
		/** FINISH PHASE */
		gui.logVerboseUAV(MUSCOPText.TALKER_FINISHED);
	}

	private void fly() {
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		int currentWP = 0;
		while (currentState.get() == FOLLOWING_MISSION && running.get()) {
			wpReachedPhase(currentWP);
			currentWP = moveToWP(currentWP);
		}
		
		while (currentState.get() < MOVE_TO_LAND) {ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);}
		
		if (!isCenter) {
			gui.logVerboseUAV(MUSCOPText.TALKER_FINISHED);
			return;
		}
	}

	private int moveToWP(int currentWP) {
		/** MOVE_TO_WP PHASE */
		if (currentState.get() == FOLLOWING_MISSION) {
			currentWP++;
			
			output.reset();
			if(isCenter) {
				gui.logVerboseUAV(MUSCOPText.CENTER_SEND_MOVE);
				output.writeShort(Message.MOVE_TO_WAYPOINT);
				output.writeLong(selfId);
				output.writeInt(currentWP);
			}else {
				gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
				output.writeShort(Message.WAYPOINT_REACHED_ACK);
				output.writeLong(selfId);
				output.writeInt(currentWP - 1);
			}
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (moveSemaphore.get() == currentWP && running.get()) {
				link.sendBroadcastMessage(message);
				// Timer
				cicleTime = cicleTime + MuscopSimProperties.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		return currentWP;
	}

	private void wpReachedPhase(int currentWP) {
		output.reset();
		output.writeShort(Message.WAYPOINT_REACHED_ACK);
		output.writeLong(selfId);
		output.writeInt(currentWP);
		output.flush();
		message = Arrays.copyOf(outBuffer, output.position());
		
		cicleTime = System.currentTimeMillis();
		while (wpReachedSemaphore.get() == currentWP && running.get()) {
			link.sendBroadcastMessage(message);
			
			// Timer
			cicleTime = cicleTime + MuscopSimProperties.SENDING_TIMEOUT;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		/** WP_REACHED PHASE */
		/*
		if (isCenter) {
			gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
			while (wpReachedSemaphore.get() == currentWP && running.get()) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
			output.reset();
			output.writeShort(Message.WAYPOINT_REACHED_ACK);
			output.writeLong(selfId);
			output.writeInt(currentWP);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (wpReachedSemaphore.get() == currentWP && running.get()) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		*/
	}

	private void land() {
		/** LANDING PHASE */
		// Only the center UAV gets here
		gui.logVerboseUAV(MUSCOPText.CENTER_SEND_LAND);
		output.reset();
		output.writeShort(Message.LAND);
		Location2DUTM currentLocation = copter.getLocationUTM();
		output.writeDouble(currentLocation.x);
		output.writeDouble(currentLocation.y);
		output.flush();
		message = Arrays.copyOf(outBuffer, output.position());
		
		cicleTime = System.currentTimeMillis();
		while (currentState.get() == LANDING) {
			link.sendBroadcastMessage(message);
			
			// Timer
			cicleTime = cicleTime + MuscopSimProperties.SENDING_TIMEOUT;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		while (currentState.get() < FINISH) {
			ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);
		}
	}

	private void takingOff() {
		/** TAKING OFF and SETUP FINISHED PHASES */
		gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
		while (currentState.get() < FOLLOWING_MISSION && running.get()) {
			ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);
		}
	}

	private void shareMission() {
		/** SHARE MISSION PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_DATA_TALKER);
			Location3DUTM[] mission;
			while ((mission = MuscopSimProperties.missionSent.get()) == null && running.get()) {
				ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);
			}
			output.reset();
			output.writeShort(Message.DATA);
			output.writeShort(mission.length);
			Location3DUTM waypoint;
			for (int i = 0; i < mission.length; i++) {
				waypoint = mission[i];
				output.writeDouble(waypoint.x);
				output.writeDouble(waypoint.y);
				output.writeDouble(waypoint.z);
			}
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (currentState.get() == SHARE_MISSION && running.get()) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MuscopSimProperties.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_WAIT_LIST_TALKER);
			output.reset();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (currentState.get() == SHARE_MISSION && running.get()) {
				if (missionReceived.get()) {
					link.sendBroadcastMessage(message);
				}
				
				// Timer
				cicleTime = cicleTime + MuscopSimProperties.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (currentState.get() < TAKING_OFF && running.get()) {
			ardusim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);
		}
	}
	
	public boolean isRunning() {
		return running.get();
	}

	public void setRunning(boolean running) {
		this.running.set(running);
	}

	public boolean isCenter() {
		return isCenter;
	}

	public void setCenter(boolean isCenter) {
		this.isCenter = isCenter;
	}
}
