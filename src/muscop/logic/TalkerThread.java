package muscop.logic;

import static muscop.pojo.State.*;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.UTMCoordinates;
import api.pojo.formations.FlightFormation;
import muscop.pojo.Message;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class TalkerThread extends Thread {

	private int numUAV;
	private long selfId;
	private boolean isMaster;
	
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	
	private long cicleTime;		// Cicle time used for sending messages
	private int waitingTime;	// Time to wait between two sent messages
	
	@SuppressWarnings("unused")
	private TalkerThread() {}

	public TalkerThread(int numUAV) {
		this.numUAV = numUAV;
		this.selfId = Tools.getIdFromPos(numUAV);
		this.isMaster = MUSCOPHelper.isMaster(numUAV);
		
		this.outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == START) {
				Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			UTMCoordinates initialPos = Copter.getUTMLocation(numUAV);
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == START) {
				Copter.sendBroadcastMessage(numUAV, message);

				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < SETUP) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, MUSCOPText.MASTER_DATA_TALKER);
			byte[][] messages;
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				messages = MUSCOPParam.data.get();
				if (messages != null) {
					int length = messages.length;
					for (int i = 0; i < length; i++) {
						Copter.sendBroadcastMessage(numUAV, messages[i]);
					}
				}
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.SLAVE_WAIT_LIST_TALKER);
			output.clear();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				if (MUSCOPParam.uavMissionReceivedGeo.get(numUAV) != null) {
					Copter.sendBroadcastMessage(numUAV, message);
				}
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < READY_TO_FLY) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO FLY PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, MUSCOPText.MASTER_READY_TO_FLY_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.SLAVE_READY_TO_FLY_CONFIRM_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** WAIT TAKE OFF PHASE */
		if (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (MUSCOPParam.state.get(numUAV) < TAKING_OFF) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TAKING OFF PHASE */
		GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == TAKING_OFF) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		while (MUSCOPParam.state.get(numUAV) < MOVE_TO_TARGET) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** MOVE TO TARGET PHASE */
		long idNext = MUSCOPParam.idNext.get(numUAV);
		if (idNext == FlightFormation.BROADCAST_MAC_ID) {
			GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
				Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.TALKER_TAKE_OFF_COMMAND);
			output.clear();
			output.writeShort(Message.TAKE_OFF_NOW);
			output.writeLong(idNext);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < TARGET_REACHED) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TARGET REACHED PHASE */
		boolean iAmCenter = MUSCOPParam.iAmCenter[numUAV].get();
		if (iAmCenter) {
			GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_TARGET_REACHED_TALKER);
			output.clear();
			output.writeShort(Message.TARGET_REACHED_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < READY_TO_START) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO START */
		if (iAmCenter) {
			GUI.logVerbose(numUAV, MUSCOPText.CENTER_TAKEOFF_END_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_TAKEOFF_END_ACK_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < SETUP_FINISHED) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == SETUP_FINISHED) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		while (MUSCOPParam.state.get(numUAV) < FOLLOWING_MISSION) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		int currentWP = 0;
		while (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			if (iAmCenter) {
				GUI.logVerbose(numUAV, MUSCOPText.TALKER_WAITING);
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
				output.clear();
				output.writeShort(Message.WAYPOINT_REACHED_ACK);
				output.writeLong(selfId);
				output.writeInt(currentWP);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Copter.sendBroadcastMessage(numUAV, message);
					
					// Timer
					cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
				currentWP++;
				if (iAmCenter) {
					GUI.logVerbose(numUAV, MUSCOPText.CENTER_SEND_MOVE);
					output.clear();
					output.writeShort(Message.MOVE_TO_WAYPOINT);
					output.writeInt(currentWP);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				} else {
					GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
					output.clear();
					output.writeShort(Message.WAYPOINT_REACHED_ACK);
					output.writeLong(selfId);
					output.writeInt(currentWP - 1);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < MOVE_TO_LAND) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** MOVE TO LAND PHASE */
		if (MUSCOPParam.state.get(numUAV) == MOVE_TO_LAND) {
			while (MUSCOPParam.state.get(numUAV) < LANDING) {
				Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		}
		
		/** LANDING PHASE */
		if (iAmCenter) {
			GUI.logVerbose(numUAV, MUSCOPText.CENTER_SEND_LAND);
			output.clear();
			output.writeShort(Message.LAND);
			UTMCoordinates currentLocation = Copter.getUTMLocation(numUAV);
			output.writeDouble(currentLocation.x);
			output.writeDouble(currentLocation.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == LANDING) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < FINISH) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		GUI.logVerbose(numUAV, MUSCOPText.TALKER_FINISHED);
	}
}
