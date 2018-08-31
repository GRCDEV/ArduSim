package swarmprot.logic;

import static swarmprot.pojo.State.*;

import java.awt.geom.Point2D;
import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import swarmprot.pojo.Message;

public class Talker extends Thread {

	private int numUAV;
	private long selfId;
	private boolean isMaster;
	
	private byte[] outBuffer;
	private Output output;
	private byte[] message;
	
	private long cicleTime;		// Cicle time used for sending messages
	private int waitingTime;	// Time to wait between two sent messages
	
	@SuppressWarnings("unused")
	private Talker() {}

	public Talker(int numUAV) {
		this.numUAV = numUAV;
		this.selfId = Tools.getIdFromPos(numUAV);
		this.isMaster = SwarmProtHelper.isMaster(numUAV);
		
		this.outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
			while (SwarmProtParam.state.get(numUAV) == START) {
				Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, SwarmProtText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			Point2D.Double initialPos = Copter.getUTMLocation(numUAV);
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.writeDouble(Copter.getHeading(numUAV));
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (SwarmProtParam.state.get(numUAV) == START) {
				Copter.sendBroadcastMessage(numUAV, message);

				// Timer
				cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (SwarmProtParam.state.get(numUAV) < SETUP) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, SwarmProtText.MASTER_SEND_DATA);
			int length = SwarmProtParam.data.length();
			
			cicleTime = System.currentTimeMillis();
			while (SwarmProtParam.state.get(numUAV) == SETUP) {
				for (int i = 0; i < length; i++) {
					message = SwarmProtParam.data.get(i);
					if (message != null && i != SwarmProtParam.masterPosition) {
						Copter.sendBroadcastMessage(numUAV, message);
					}
				}

				// Timer
				cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
			while (SwarmProtParam.state.get(numUAV) == SETUP) {
				Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (SwarmProtParam.state.get(numUAV) < SETUP_FINISHED) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
			while (SwarmProtParam.state.get(numUAV) == SETUP_FINISHED) {
				Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
			}
			
		} else {
			GUI.logVerbose(numUAV, SwarmProtText.SLAVE_WAIT_LIST_TALKER);
			output.clear();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (SwarmProtParam.state.get(numUAV) == SETUP_FINISHED) {
				Copter.sendBroadcastMessage(numUAV, message);

				// Timer
				cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (SwarmProtParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** WAIT TAKE OFF PHASE */
		if (SwarmProtParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
			while (SwarmProtParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (SwarmProtParam.state.get(numUAV) < TAKING_OFF) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TAKING OFF PHASE */
		GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
		while (SwarmProtParam.state.get(numUAV) == TAKING_OFF) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		while (SwarmProtParam.state.get(numUAV) < FOLLOWING_MISSION) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		int currentWP = 0;
		while (SwarmProtParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** MOVE_TO_WP PHASE */
			if (currentWP == 0) {
				if (SwarmProtParam.idNext.get(numUAV) == SwarmProtParam.BROADCAST_MAC_ID) {
					GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
					while (SwarmProtParam.moveSemaphore.get(numUAV) == currentWP) {
						Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
					}
				} else {
					GUI.logVerbose(numUAV, SwarmProtText.TAKE_OFF_COMMAND);
					long next = SwarmProtParam.idNext.get(numUAV);
					output.clear();
					output.writeShort(Message.TAKE_OFF_NOW);
					output.writeLong(next);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (SwarmProtParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			} else {
				if (this.isMaster) {
					GUI.logVerbose(numUAV, SwarmProtText.MASTER_SEND_MOVE);
					output.clear();
					output.writeShort(Message.MOVE_NOW);
					output.writeInt(currentWP);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (SwarmProtParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				} else {
					GUI.logVerbose(numUAV, SwarmProtText.SLAVE_SEND_WP_REACHED_ACK);
					output.clear();
					output.writeShort(Message.WP_REACHED_ACK);
					output.writeLong(selfId);
					output.writeInt(currentWP - 1);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (SwarmProtParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
			
			/** WP_REACHED PHASE */
			if (this.isMaster) {
				GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
				while (SwarmProtParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				GUI.logVerbose(numUAV, SwarmProtText.SLAVE_SEND_WP_REACHED_ACK);
				output.clear();
				output.writeShort(Message.WP_REACHED_ACK);
				output.writeLong(selfId);
				output.writeInt(currentWP);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (SwarmProtParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Copter.sendBroadcastMessage(numUAV, message);
					
					// Timer
					cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
			currentWP++;
		}
		while (SwarmProtParam.state.get(numUAV) < LANDING) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** LANDING PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, SwarmProtText.MASTER_SEND_LAND);
			output.clear();
			output.writeShort(Message.LAND_NOW);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (SwarmProtParam.state.get(numUAV) == LANDING) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + SwarmProtParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, SwarmProtText.TALKER_WAITING);
			while (SwarmProtParam.state.get(numUAV) == LANDING) {
				Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (SwarmProtParam.state.get(numUAV) < FINISH) {
			Tools.waiting(SwarmProtParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		GUI.logVerbose(numUAV, SwarmProtText.TALKER_FINISHED);
	}
}
