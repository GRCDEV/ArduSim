package followme.logic;

import static followme.pojo.State.*;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2DUTM;
import followme.pojo.Message;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class TalkerThread extends Thread {
	
	private int numUAV;
	private long selfId;
	private boolean isMaster;
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	private CommLink link;
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	
	private long cicleTime;		// Cicle time used for sending messages
	private long waitingTime;	// (ms) Time to wait between two sent messages
	
	protected static volatile boolean protocolStarted = false;

	@SuppressWarnings("unused")
	private TalkerThread() {}
	
	public TalkerThread(int numUAV) {
		this.numUAV = numUAV;
		
		this.ardusim = API.getArduSim();
		this.isMaster = FollowMeHelper.isMaster(numUAV);
		this.copter = API.getCopter(numUAV);
		this.selfId = this.copter.getID();
		this.gui = API.getGUI(numUAV);
		this.link = API.getCommLink(numUAV);
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		while (!API.getArduSim().isAvailable()) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == START) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			Location2DUTM initialPos = copter.getLocationUTM();
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == START) {
				link.sendBroadcastMessage(message);

				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < SETUP) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_DATA_TALKER);
			byte[][] messages;
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == SETUP) {
				messages = FollowMeParam.data.get();
				if (messages != null) {
					int length = messages.length;
					for (int i = 0; i < length; i++) {
						link.sendBroadcastMessage(messages[i]);
					}
				}
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_LIST_TALKER);
			output.clear();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == SETUP) {
				if (FollowMeParam.flyingFormation.get(numUAV) != null) {
					link.sendBroadcastMessage(message);
				}
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < READY_TO_FLY) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO FLY PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_READY_TO_FLY_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_FLY) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_READY_TO_FLY_CONFIRM_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_FLY) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		if (this.isMaster) {
			/** WAIT SLAVES PHASE */
			gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == TARGET_REACHED) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			/** WAIT TAKE OFF PHASE */
			if (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
				while (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
					ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
				}
			}
			while (FollowMeParam.state.get(numUAV) < TAKING_OFF) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** TAKING OFF PHASE */
			gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == TAKING_OFF) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			while (FollowMeParam.state.get(numUAV) < MOVE_TO_TARGET) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** MOVE TO TARGET PHASE */
			long idNext = FollowMeParam.idNext.get(numUAV);
			if (idNext == FlightFormation.BROADCAST_MAC_ID) {
				gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
				while (FollowMeParam.state.get(numUAV) == MOVE_TO_TARGET) {
					ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				gui.logVerboseUAV(FollowMeText.TALKER_TAKE_OFF_COMMAND);
				output.clear();
				output.writeShort(Message.TAKE_OFF_NOW);
				output.writeLong(selfId);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (FollowMeParam.state.get(numUAV) == MOVE_TO_TARGET) {
					link.sendBroadcastMessage(message);
					
					// Timer
					cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
					waitingTime = cicleTime - System.currentTimeMillis();
					if (waitingTime > 0) {
						ardusim.sleep(waitingTime);
					}
				}
			}
			while (FollowMeParam.state.get(numUAV) < TARGET_REACHED) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** TARGET REACHED PHASE */
			gui.logVerboseUAV(FollowMeText.SLAVE_TARGET_REACHED_TALKER);
			output.clear();
			output.writeShort(Message.TARGET_REACHED_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == TARGET_REACHED) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < READY_TO_START) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO START */
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_TAKEOFF_END_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_START) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_TAKEOFF_END_ACK_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_START) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < SETUP_FINISHED) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == SETUP_FINISHED) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			/** FINISH PHASE */
			gui.logVerboseUAV(FollowMeText.TALKER_FINISHED);
			return;
		}
		// Only the master UAV reaches this point
		while (FollowMeParam.state.get(numUAV) < FOLLOWING) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FOLLOWING PHASE */
		gui.logVerboseUAV(FollowMeText.MASTER_WAIT_ALTITUDE);
		while (!protocolStarted) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		Location2DUTM here;
		double z, yaw;
		cicleTime = System.currentTimeMillis();
		while (FollowMeParam.state.get(numUAV) == FOLLOWING) {
			here = copter.getLocationUTM();
			z = copter.getAltitudeRelative();
			yaw = copter.getHeading();
			output.clear();
			output.writeShort(Message.I_AM_HERE);
//			output.writeLong(selfId);
			output.writeDouble(here.x);
			output.writeDouble(here.y);
			output.writeDouble(z);
			output.writeDouble(yaw);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			link.sendBroadcastMessage(message);
			
			// Timer
			cicleTime = cicleTime + FollowMeParam.sendPeriod;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		while (FollowMeParam.state.get(numUAV) < LANDING) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** LANDING PHASE */
		gui.logVerboseUAV(FollowMeText.MASTER_SEND_LAND);
		output.clear();
		output.writeShort(Message.LAND);
		here = copter.getLocationUTM();
		output.writeDouble(here.x);
		output.writeDouble(here.y);
		output.writeDouble(copter.getHeading());
		output.writeDouble(FlightFormation.getLandingFormationDistance());
		output.flush();
		message = Arrays.copyOf(outBuffer, output.position());
		
		cicleTime = System.currentTimeMillis();
		while (FollowMeParam.state.get(numUAV) == LANDING) {
			link.sendBroadcastMessage(message);
			
			// Timer
			cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
		while (FollowMeParam.state.get(numUAV) < FINISH) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		gui.logVerboseUAV(FollowMeText.TALKER_FINISHED);
	}
	
	
	
}
