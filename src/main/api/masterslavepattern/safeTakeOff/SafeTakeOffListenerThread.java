package main.api.masterslavepattern.safeTakeOff;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.io.Input;

import api.API;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3D;
import main.Text;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.TakeOffListener;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

/**
 * Thread used to perform a safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SafeTakeOffListenerThread extends Thread {

	private int numUAV;
	private long selfID;
	private long prevID;
	private Location2DGeo targetLocation;
	private double altitudeStep1, altitudeStep2;
	private boolean excluded;
	private boolean isCenter;
	private int numUAVs;
	private SafeTakeOffContext safeTakeOffInstance;
	private SafeTakeOffListener listener;
	private GUI gui;
	private InternalCommLink commLink;
	private byte[] inBuffer;
	private Input input;
	private Copter copter;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private SafeTakeOffListenerThread() {}
	
	public SafeTakeOffListenerThread(int numUAV, SafeTakeOffContext safeTakeOffInstance, SafeTakeOffListener listener) {
		super(Text.SAFE_TAKE_OFF_LISTENER + numUAV);
		this.numUAV = numUAV;
		this.safeTakeOffInstance = safeTakeOffInstance;
		this.prevID = safeTakeOffInstance.prevID;
		this.targetLocation = safeTakeOffInstance.targetLocation;
		this.altitudeStep1 = safeTakeOffInstance.altitudeStep1;
		this.altitudeStep2 = safeTakeOffInstance.altitudeStep2;
		this.excluded = safeTakeOffInstance.excluded;
		this.isCenter = safeTakeOffInstance.isCenter;
		this.numUAVs = safeTakeOffInstance.numUAVs;
		this.listener = listener;
		this.gui = API.getGUI(numUAV);
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.copter = API.getCopter(numUAV);
		this.selfID = this.copter.getID();
		this.ardusim = API.getArduSim();
	}
	
	@Override
	public void run() {
		
		final AtomicInteger state = new AtomicInteger(MSParam.STATE_WAIT_TAKE_OFF);
		
		if (prevID == SafeTakeOffContext.BROADCAST_MAC_ID) {
			if (excluded) {
				state.set(MSParam.STATE_TARGET_REACHED);
			} else {
				state.set(MSParam.STATE_TAKE_OFF_STEP_1);
			}
		}
		
		new SafeTakeOffTalkerThread(numUAV, safeTakeOffInstance, state).start();
		
		/** WAIT TAKE OFF PHASE */
		if (state.get() == MSParam.STATE_WAIT_TAKE_OFF) {
			gui.logVerboseUAV(MSText.LISTENER_WAITING_TAKE_OFF);
			while (state.get() == MSParam.STATE_WAIT_TAKE_OFF) {
				inBuffer = commLink.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == MSMessageID.TAKE_OFF_NOW || type == MSMessageID.REACHED_ACK) {
						long id = input.readLong();
						if (id == prevID) {
							if (excluded) {
								state.set(MSParam.STATE_TARGET_REACHED);
							} else {
								state.set(MSParam.STATE_TAKE_OFF_STEP_1);
							}
						}
					}
				}
			}
		}
		
		/** TAKE OFF STEP 1 PHASE */
		if (state.get() == MSParam.STATE_TAKE_OFF_STEP_1) {
			gui.logVerboseUAV(MSText.LISTENER_TAKING_OFF_1);
			copter.takeOff(altitudeStep1, new TakeOffListener() {
				
				@Override
				public void onFailure() {
					gui.exit(MSText.TAKE_OFF_ERROR + " " + selfID);
				}
				
				@Override
				public void onCompleteActionPerformed() {
					state.set(MSParam.STATE_TAKE_OFF_STEP_2);
				}
			}).start();
			while (state.get() == MSParam.STATE_TAKE_OFF_STEP_1) {
				// Discard message
				commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
			}
			
			/** TAKE OFF STEP 2 PHASE */
			gui.logVerboseUAV(MSText.LISTENER_TAKING_OFF_2);
			copter.moveTo(new Location3D(targetLocation, altitudeStep2),
					new MoveToListener() {
						
						@Override
						public void onFailure() {
							gui.exit(MSText.MOVE_ERROR + selfID);
						}
						
						@Override
						public void onCompleteActionPerformed() {
							state.set(MSParam.STATE_TARGET_REACHED);
						}
					}).start();
			
			
		}
		while (state.get() < MSParam.STATE_TARGET_REACHED) {
			// Discard message
			commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
		}
		
		/** TARGET REACHED PHASE */
		if (excluded) {
			gui.logVerboseUAV(MSText.LISTENER_WAITING_ON_GROUND);
		} else {
			gui.logVerboseUAV(MSText.LISTENER_TARGET_REACHED);
		}
		Map<Long, Long> acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
		long lastReceivedTakeoffEnd = 0;
		if (isCenter) {
			gui.logVerboseUAV(MSText.LISTENER_CENTER_WAITING_SLAVES);
			while (state.get() == MSParam.STATE_TARGET_REACHED) {
				inBuffer = commLink.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == MSMessageID.REACHED_ACK) {
						long id = input.readLong();
						if (id != selfID) {
							acks.put(id, id);
							if (acks.size() == numUAVs - 1) {
								state.set(MSParam.STATE_TAKE_OFF_CHECK);
							}
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(MSText.LISTENER_NO_CENTER_WAITING_END);
			while (state.get() == MSParam.STATE_TARGET_REACHED) {
				inBuffer = commLink.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == MSMessageID.TAKE_OFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
						state.set(MSParam.STATE_TAKE_OFF_CHECK);
					}
				}
			}
		}
		
		/** TAKE OFF CHECK PHASE */
		gui.logVerboseUAV(MSText.LISTENER_SYNCHRONIZING);
		if (isCenter) {
			gui.logVerboseUAV(MSText.CENTER_TAKEOFF_END_ACK_LISTENER);
			acks.clear();
			while (state.get() == MSParam.STATE_TAKE_OFF_CHECK) {
				inBuffer = commLink.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == MSMessageID.TAKE_OFF_END_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							state.set(MSParam.STATE_TAKE_OFF_FINISHED);
						}
					}
					
				}
			}
			ardusim.sleep(MSParam.TAKE_OFF_TIMEOUT);	// Wait to slaves timeout
		} else {
			gui.logVerboseUAV(MSText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (state.get() == MSParam.STATE_TAKE_OFF_CHECK) {
				inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == MSMessageID.TAKE_OFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedTakeoffEnd > MSParam.TAKE_OFF_TIMEOUT) {
					state.set(MSParam.STATE_TAKE_OFF_FINISHED);
				}
			}
		}
		
		if (listener != null) {
			listener.onCompleteActionPerformed();
		}
	}

}
