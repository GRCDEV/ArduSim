package scanv2.logic;

import static scanv2.pojo.State.*;

import java.awt.geom.Point2D;
import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.formations.FlightFormation;
import scanv2.pojo.Message;

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
		this.isMaster = ScanHelper.isMaster(numUAV);
		
		this.outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.output = new Output(outBuffer);
		
		this.cicleTime = 0;
	}

	@Override
	public void run() {
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
			while (ScanParam.state.get(numUAV) == START) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.SLAVE_START_TALKER);
			output.clear();
			output.writeShort(Message.HELLO);
			output.writeLong(selfId);
			Point2D.Double initialPos = Copter.getUTMLocation(numUAV);
			output.writeDouble(initialPos.x);
			output.writeDouble(initialPos.y);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (ScanParam.state.get(numUAV) == START) {
				Copter.sendBroadcastMessage(numUAV, message);

				// Timer
				cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (ScanParam.state.get(numUAV) < SETUP) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.MASTER_SEND_DATA);
			int length = ScanParam.data.length();//requiere identificador y un Map
			
			cicleTime = System.currentTimeMillis();
			while (ScanParam.state.get(numUAV) == SETUP) {
				for (int i = 0; i < length; i++) {
					message = ScanParam.data.get(i);
					if (message != null) {
						Copter.sendBroadcastMessage(numUAV, message);
					}
				}
				
				// Timer
				cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
			while (ScanParam.state.get(numUAV) == SETUP) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (ScanParam.state.get(numUAV) < SETUP_FINISHED) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
			while (ScanParam.state.get(numUAV) == SETUP_FINISHED) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
			
		} else {
			GUI.logVerbose(numUAV, ScanText.SLAVE_WAIT_LIST_TALKER);
			output.clear();
			output.writeShort(Message.DATA_ACK);
			output.writeLong(selfId);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (ScanParam.state.get(numUAV) == SETUP_FINISHED) {
				Copter.sendBroadcastMessage(numUAV, message);

				// Timer
				cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		while (ScanParam.state.get(numUAV) < WAIT_TAKE_OFF) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** WAIT TAKE OFF PHASE */
		if (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
			while (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (ScanParam.state.get(numUAV) < TAKING_OFF) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** TAKING OFF PHASE */
		GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
		while (ScanParam.state.get(numUAV) == TAKING_OFF) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		while (ScanParam.state.get(numUAV) < FOLLOWING_MISSION) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		int currentWP = 0;
		boolean amICenter = ScanParam.amICenter[numUAV].get();
		while (ScanParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** MOVE_TO_WP PHASE */
			if (currentWP == 0) {
				if (ScanParam.idNext.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
					GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
					while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
						Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
					}
				} else {
					GUI.logVerbose(numUAV, ScanText.TAKE_OFF_COMMAND);
					long next = ScanParam.idNext.get(numUAV);
					output.clear();
					output.writeShort(Message.TAKE_OFF_NOW);
					output.writeLong(next);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			} else {
				if (amICenter) {
					GUI.logVerbose(numUAV, ScanText.CENTER_SEND_MOVE);
					output.clear();
					output.writeShort(Message.MOVE_NOW);
					output.writeInt(currentWP);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				} else {
					GUI.logVerbose(numUAV, ScanText.SLAVE_SEND_WP_REACHED_ACK);
					output.clear();
					output.writeShort(Message.WP_REACHED_ACK);
					output.writeLong(selfId);
					output.writeInt(currentWP - 1);
					output.flush();
					message = Arrays.copyOf(outBuffer, output.position());
					
					cicleTime = System.currentTimeMillis();
					while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
						Copter.sendBroadcastMessage(numUAV, message);
						
						// Timer
						cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
						waitingTime = (int) (cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
			
			/** WP_REACHED PHASE */
			if (amICenter) {
				GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
				while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
				}
			} else {
				GUI.logVerbose(numUAV, ScanText.SLAVE_SEND_WP_REACHED_ACK);
				output.clear();
				output.writeShort(Message.WP_REACHED_ACK);
				output.writeLong(selfId);
				output.writeInt(currentWP);
				output.flush();
				message = Arrays.copyOf(outBuffer, output.position());
				
				cicleTime = System.currentTimeMillis();
				while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					Copter.sendBroadcastMessage(numUAV, message);
					
					// Timer
					cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
			currentWP++;
		}
		while (ScanParam.state.get(numUAV) < LANDING) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** LANDING PHASE */
		if (amICenter) {
			GUI.logVerbose(numUAV, ScanText.CENTER_SEND_LAND);
			output.clear();
			output.writeShort(Message.LAND_NOW);
			output.flush();
			message = Arrays.copyOf(outBuffer, output.position());
			
			cicleTime = System.currentTimeMillis();
			while (ScanParam.state.get(numUAV) == LANDING) {
				Copter.sendBroadcastMessage(numUAV, message);
				
				// Timer
				cicleTime = cicleTime + ScanParam.SENDING_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.TALKER_WAITING);
			while (ScanParam.state.get(numUAV) == LANDING) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		}
		while (ScanParam.state.get(numUAV) < FINISH) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** FINISH PHASE */
		GUI.logVerbose(numUAV, ScanText.TALKER_FINISHED);
	}
}
