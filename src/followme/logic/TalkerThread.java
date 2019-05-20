package followme.logic;

import static followme.pojo.State.*;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.UTMCoordinates;
import api.pojo.formations.FlightFormation;
import followme.pojo.Message;
import main.communications.CommLink;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class TalkerThread extends Thread {
	
	private int numUAV;
	private long selfId;
	private boolean isMaster;
	
	private CommLink link;
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	
	private long cicleTime;		// Cicle time used for sending messages
	private int waitingTime;	// Time to wait between two sent messages
	
	protected static volatile boolean protocolStarted = false;

	@SuppressWarnings("unused")
	private TalkerThread() {}
	
	public TalkerThread(int numUAV) {
		this.numUAV = numUAV;
		this.selfId = Tools.getIdFromPos(numUAV);
		this.isMaster = FollowMeHelper.isMaster(numUAV);
		
		this.link = API.getCommLink(numUAV);
		this.outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == START) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, FollowMeText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			UTMCoordinates initialPos = Copter.getUTMLocation(numUAV);
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == START) {
				link.sendBroadcastMessage(message);

				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < SETUP) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, FollowMeText.MASTER_DATA_TALKER);
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
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, FollowMeText.SLAVE_WAIT_LIST_TALKER);
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
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < READY_TO_FLY) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO FLY PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, FollowMeText.MASTER_READY_TO_FLY_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_FLY) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, FollowMeText.SLAVE_READY_TO_FLY_CONFIRM_TALKER);
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
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		if (this.isMaster) {
			/** WAIT SLAVES PHASE */
			GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == TARGET_REACHED) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			/** WAIT TAKE OFF PHASE */
			if (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
				while (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
					Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
				}
			}
			while (FollowMeParam.state.get(numUAV) < TAKING_OFF) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** TAKING OFF PHASE */
			GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == TAKING_OFF) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			while (FollowMeParam.state.get(numUAV) < MOVE_TO_TARGET) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** MOVE TO TARGET PHASE */
			long idNext = FollowMeParam.idNext.get(numUAV);
			if (idNext == FlightFormation.BROADCAST_MAC_ID) {
				GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
				while (FollowMeParam.state.get(numUAV) == MOVE_TO_TARGET) {
					Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				GUI.logVerbose(numUAV, FollowMeText.TALKER_TAKE_OFF_COMMAND);
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
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
			while (FollowMeParam.state.get(numUAV) < TARGET_REACHED) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
			
			/** TARGET REACHED PHASE */
			GUI.logVerbose(numUAV, FollowMeText.SLAVE_TARGET_REACHED_TALKER);
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
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < READY_TO_START) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO START */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, FollowMeText.MASTER_TAKEOFF_END_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (FollowMeParam.state.get(numUAV) == READY_TO_START) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, FollowMeText.SLAVE_TAKEOFF_END_ACK_TALKER);
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
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (FollowMeParam.state.get(numUAV) < SETUP_FINISHED) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, FollowMeText.TALKER_WAITING);
			while (FollowMeParam.state.get(numUAV) == SETUP_FINISHED) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			/** FINISH PHASE */
			GUI.logVerbose(numUAV, FollowMeText.TALKER_FINISHED);
			return;
		}
		// Only the master UAV reaches this point
		while (FollowMeParam.state.get(numUAV) < FOLLOWING) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FOLLOWING PHASE */
		GUI.logVerbose(numUAV, FollowMeText.MASTER_WAIT_ALTITUDE);
		while (!protocolStarted) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		UTMCoordinates here;
		double z, yaw;
		cicleTime = System.currentTimeMillis();
		while (FollowMeParam.state.get(numUAV) == FOLLOWING) {
			here = Copter.getUTMLocation(numUAV);
			z = Copter.getZRelative(numUAV);
			yaw = Copter.getHeading(numUAV);
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
			waitingTime = (int) (cicleTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				Tools.waiting(waitingTime);
			}
		}
		while (FollowMeParam.state.get(numUAV) < LANDING) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** LANDING PHASE */
		GUI.logVerbose(numUAV, FollowMeText.MASTER_SEND_LAND);
		output.clear();
		output.writeShort(Message.LAND);
		here = Copter.getUTMLocation(numUAV);
		output.writeDouble(here.x);
		output.writeDouble(here.y);
		output.writeDouble(Copter.getHeading(numUAV));
		output.writeDouble(FlightFormation.getLandingFormationDistance());
		output.flush();
		message = Arrays.copyOf(outBuffer, output.position());
		
		cicleTime = System.currentTimeMillis();
		while (FollowMeParam.state.get(numUAV) == LANDING) {
			link.sendBroadcastMessage(message);
			
			// Timer
			cicleTime = cicleTime + FollowMeParam.SENDING_TIMEOUT;
			waitingTime = (int) (cicleTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				Tools.waiting(waitingTime);
			}
		}
		while (FollowMeParam.state.get(numUAV) < FINISH) {
			Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		GUI.logVerbose(numUAV, FollowMeText.TALKER_FINISHED);
	}
	
	
	
}
