package muscop.logic;

import static muscop.pojo.State.*;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2DUTM;
import main.api.ArduSim;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;
import muscop.pojo.Message;

/** 
 * Thread used to send messages to other UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TalkerThread extends Thread {

	private int numUAV;
	private long selfId;
	private boolean isMaster;
	private ArduSim ardusim;
	private GUI gui;
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	private CommLink link;
	
	private long cicleTime;		// Cicle time used for sending messages
	private long waitingTime;	// (ms) Time to wait between two sent messages
	
	@SuppressWarnings("unused")
	private TalkerThread() {}

	public TalkerThread(int numUAV) {
		this.numUAV = numUAV;
		this.selfId = API.getCopter(numUAV).getID();
		this.isMaster = MUSCOPHelper.isMaster(numUAV);
		this.ardusim = API.getArduSim();
		this.gui = API.getGUI(numUAV);
		this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		this.link = API.getCommLink(numUAV);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		
		while (!API.getArduSim().isAvailable()) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == START) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			Location2DUTM initialPos = API.getCopter(numUAV).getLocationUTM();
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == START) {
				link.sendBroadcastMessage(message);

				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < SETUP) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_DATA_TALKER);
			byte[][] messages;
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				messages = MUSCOPParam.data.get();
				if (messages != null) {
					int length = messages.length;
					for (int i = 0; i < length; i++) {
						link.sendBroadcastMessage(messages[i]);
					}
				}
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_WAIT_LIST_TALKER);
			output.clear();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				if (MUSCOPParam.uavMissionReceivedGeo.get(numUAV) != null) {
					link.sendBroadcastMessage(message);
				}
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < READY_TO_FLY) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO FLY PHASE */
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_READY_TO_FLY_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_READY_TO_FLY_CONFIRM_TALKER);
			output.clear();
			output.writeShort(Message.READY_TO_FLY_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** WAIT TAKE OFF PHASE */
		if (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (MUSCOPParam.state.get(numUAV) < TAKING_OFF) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TAKING OFF PHASE */
		gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == TAKING_OFF) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		while (MUSCOPParam.state.get(numUAV) < MOVE_TO_TARGET) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** MOVE TO TARGET PHASE */
		long idNext = MUSCOPParam.idNext.get(numUAV);
		if (idNext == FlightFormation.BROADCAST_MAC_ID) {
			gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.TALKER_TAKE_OFF_COMMAND);
			output.clear();
			output.writeShort(Message.TAKE_OFF_NOW);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < TARGET_REACHED) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TARGET REACHED PHASE */
		boolean iAmCenter = MUSCOPParam.iAmCenter[numUAV].get();
		if (iAmCenter) {
			gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.NO_CENTER_TARGET_REACHED_TALKER);
			output.clear();
			output.writeShort(Message.TARGET_REACHED_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < READY_TO_START) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** READY TO START */
		if (iAmCenter) {
			gui.logVerboseUAV(MUSCOPText.CENTER_TAKEOFF_END_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.NO_CENTER_TAKEOFF_END_ACK_TALKER);
			output.clear();
			output.writeShort(Message.TAKEOFF_END_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < SETUP_FINISHED) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == SETUP_FINISHED) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		while (MUSCOPParam.state.get(numUAV) < FOLLOWING_MISSION) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		int currentWP = 0;
		while (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			if (iAmCenter) {
				gui.logVerboseUAV(MUSCOPText.TALKER_WAITING);
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
				output.clear();
				output.writeShort(Message.WAYPOINT_REACHED_ACK);
				output.writeLong(selfId);
				output.writeInt(currentWP);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					link.sendBroadcastMessage(message);
					
					// Timer
					cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
					waitingTime = cicleTime - System.currentTimeMillis();
					if (waitingTime > 0) {
						ardusim.sleep(waitingTime);
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
				currentWP++;
				if (iAmCenter) {
					gui.logVerboseUAV(MUSCOPText.CENTER_SEND_MOVE);
					output.clear();
					output.writeShort(Message.MOVE_TO_WAYPOINT);
					output.writeInt(currentWP);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
						link.sendBroadcastMessage(message);
						
						// Timer
						cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
						waitingTime = cicleTime - System.currentTimeMillis();
						if (waitingTime > 0) {
							ardusim.sleep(waitingTime);
						}
					}
				} else {
					gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAYPOINT_REACHED_ACK_TALKER);
					output.clear();
					output.writeShort(Message.WAYPOINT_REACHED_ACK);
					output.writeLong(selfId);
					output.writeInt(currentWP - 1);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
						link.sendBroadcastMessage(message);
						
						// Timer
						cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
						waitingTime = cicleTime - System.currentTimeMillis();
						if (waitingTime > 0) {
							ardusim.sleep(waitingTime);
						}
					}
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < MOVE_TO_LAND) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** MOVE TO LAND PHASE */
		if (MUSCOPParam.state.get(numUAV) == MOVE_TO_LAND) {
			while (MUSCOPParam.state.get(numUAV) < LANDING) {
				ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
			}
		}
		
		/** LANDING PHASE */
		if (iAmCenter) {
			gui.logVerboseUAV(MUSCOPText.CENTER_SEND_LAND);
			output.clear();
			output.writeShort(Message.LAND);
			Location2DUTM currentLocation = API.getCopter(numUAV).getLocationUTM();
			output.writeDouble(currentLocation.x);
			output.writeDouble(currentLocation.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == LANDING) {
				link.sendBroadcastMessage(message);
				
				// Timer
				cicleTime = cicleTime + MUSCOPParam.SENDING_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		while (MUSCOPParam.state.get(numUAV) < FINISH) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		gui.logVerboseUAV(MUSCOPText.TALKER_FINISHED);
	}
}
