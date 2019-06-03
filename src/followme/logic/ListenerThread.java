package followme.logic;

import static followme.pojo.State.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2D;
import api.pojo.location.Location2DUTM;
import followme.pojo.Message;
import main.api.ArduSim;
import main.api.ArduSimNotReadyException;
import main.api.Copter;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.MoveTo;
import main.api.TakeOffListener;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class ListenerThread extends Thread {
	
	private int numUAV;
	private long selfId;
	private boolean isMaster;
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	
	private CommLink link;
	byte[] inBuffer;
	Input input;

	@SuppressWarnings("unused")
	private ListenerThread() {}
	
	public ListenerThread(int numUAV) {
		this.numUAV = numUAV;
		
		this.isMaster = FollowMeHelper.isMaster(numUAV);
		this.copter = API.getCopter(numUAV);
		this.selfId = this.copter.getID();
		this.gui = API.getGUI(numUAV);
		this.link = API.getCommLink(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.ardusim = API.getArduSim();
	}

	@Override
	public void run() {
		while (!ardusim.isAvailable()) {
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		Map<Long, Location2DUTM> UAVsDetected = null;
		gui.logUAV(FollowMeText.START);
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_START_LISTENER);
			UAVsDetected = new HashMap<Long, Location2DUTM>();	// Detecting UAVs
			int numUAVsDetected = 0;
			Long idSlave;
			Location2DUTM slaveLocation;
			while (FollowMeParam.state.get(numUAV) == START) {
				inBuffer = link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.HELLO) {
						// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
						idSlave = input.readLong();
						slaveLocation = new Location2DUTM(input.readDouble(), input.readDouble());
						// ADD to Map the new UAV detected
						UAVsDetected.put(idSlave, slaveLocation);
						if (UAVsDetected.size() > numUAVsDetected) {
							numUAVsDetected = UAVsDetected.size();
							gui.log(FollowMeText.MASTER_DETECTED_UAVS + numUAVsDetected);
						}
					}
				}
				// Coordination with ArduSim
				if (ardusim.isSetupInProgress() || ardusim.isSetupFinished()) {// The setup could be too fast
					FollowMeParam.state.set(numUAV, SETUP);
				}
			}
		} else {
			gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
			while (FollowMeParam.state.get(numUAV) == START) {
				// Discard message
				link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
				// Coordination with ArduSim
				if (ardusim.isSetupInProgress() || ardusim.isSetupFinished()) {
					FollowMeParam.state.set(numUAV, SETUP);
				}
			}
		}
		
		/** SETUP PHASE */
		gui.logUAV(FollowMeText.SETUP);
		gui.updateProtocolState(FollowMeText.SETUP);
		long lastReceivedReadyToFly = 0;	// Used in slaves to detect the end of this phase
		Map<Long, Long> acks = null;
		int numSlaves = 0;
		FlightFormation flightFormation = null;
		Location2D targetLocation = null;
		double takeoffAltitudePhase1 = 0;
		double takeoffAltitudePhase2 = 0;
		long idPrev = 0;					// Identifier of the previous UAV in the takeoff sequence
		int numUAVs = 0;					// Number of UAVs in the formation, including the master UAV
		int flightFormationPosition = 0;	// Position of the master UAV in the flight formation
		if (this.isMaster) {
			numSlaves = UAVsDetected.size();
			Map<Long, Location2DUTM> groundLocations = new HashMap<Long, Location2DUTM>();
			groundLocations.putAll(UAVsDetected);
			groundLocations.put(this.selfId, copter.getLocationUTM());	// Adding master UAV
			
			// 1. Get the location and yaw of the master UAV, and the take off altitude
			Location2D masterLocation;
			double masterYaw;
			if (ardusim.getArduSimRole() == ArduSim.MULTICOPTER) {
				masterLocation = copter.getLocation();
				masterYaw = copter.getHeading();
			} else {
				masterLocation = new Location2D(FollowMeParam.masterInitialLatitude, FollowMeParam.masterInitialLongitude);
				masterYaw = FollowMeParam.masterInitialYaw;
			}
			double takeoffAltitude = FollowMeParam.slavesStartingAltitude;
			double takeOffAltitudeStepOne;
			if (takeoffAltitude <= 5.0) {
				takeOffAltitudeStepOne = 2.0;
			} else if (takeoffAltitude >= 10.0) {
				takeOffAltitudeStepOne = 5.0;
			} else {
				takeOffAltitudeStepOne = takeoffAltitude / 2;
			}
			
			// 2. Calculus of the slaves distribution that better fit the current layout on the ground
			Formation flyingFormation = FlightFormation.getFlyingFormation();
			double minFlyingFormationDistance = FlightFormation.getFlyingFormationDistance();
			FlightFormation airFormation = FlightFormation.getFormation(flyingFormation, numSlaves + 1, minFlyingFormationDistance);
			Triplet<Integer, Long, Location2DUTM>[] match =
					FlightFormation.matchIDs(groundLocations, masterYaw, false, this.selfId, airFormation);
			
			// 3. Define the takeoff sequence and store data
			byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
			Output output = new Output(outBuffer);
			Pair<Integer, Long>[] sequence = FlightFormation.getTakeoffSequence(UAVsDetected, match);
			Map<Long, byte[]> messages = new HashMap<>((int)Math.ceil(numSlaves / 0.75) + 1);
			long prevId, nextId;
			Pair<Integer, Long> current;
			// i represents the position in the takeoff sequence
			for (int i = 0; i < numSlaves; i++) {
				current = sequence[i];
				
				if (i == 0) {
					prevId = FlightFormation.BROADCAST_MAC_ID;
				} else {
					prevId = sequence[i - 1].getValue1();
				}
				if (i == numSlaves - 1) {
					nextId = FlightFormation.BROADCAST_MAC_ID;
				} else {
					nextId = sequence[i + 1].getValue1();
				}
				
				output.clear();
				output.writeShort(Message.DATA);
				output.writeLong(current.getValue1());
				output.writeLong(prevId);
				output.writeLong(nextId);
				output.writeShort(flyingFormation.getFormationId());
				output.writeInt(numSlaves + 1);
				output.writeDouble(minFlyingFormationDistance);
				output.writeInt(current.getValue0());
				Location2DUTM masterLoc = masterLocation.getUTMLocation();
				output.writeDouble(masterLoc.x);
				output.writeDouble(masterLoc.y);
				output.writeDouble(masterYaw);
				output.writeDouble(takeOffAltitudeStepOne);
				output.writeDouble(takeoffAltitude);
				output.flush();
				messages.put(current.getValue1(), Arrays.copyOf(outBuffer, output.position()));
			}
			FollowMeParam.data.set(messages.values().toArray(new byte[messages.size()][]));
			
			try {
				output.close();
			} catch (KryoException e) {}
			
			
			// 4. Wait for data ack from all the slaves
			gui.logVerboseUAV(FollowMeText.MASTER_DATA_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numSlaves / 0.75) + 1);
			while (FollowMeParam.state.get(numUAV) == SETUP) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numSlaves) {
							FollowMeParam.state.set(numUAV, READY_TO_FLY);
						}
					}
				}
			}
		} else {
			// I am a slave
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_DATA_LISTENER);
			while (FollowMeParam.state.get(numUAV) == SETUP) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.DATA && FollowMeParam.flyingFormation.get(numUAV) == null) {
						long id = input.readLong();
						if (id == selfId) {
							idPrev = input.readLong();
							FollowMeParam.idNext.set(numUAV, input.readLong());
							Formation receivedFormation = Formation.getFormation(input.readShort());
							numUAVs = input.readInt();
							double minDistance = input.readDouble();
							flightFormationPosition = input.readInt();
							Location2DUTM masterLocation = new Location2DUTM(input.readDouble(), input.readDouble());
							double masterYaw = input.readDouble();
							takeoffAltitudePhase1 = input.readDouble();
							takeoffAltitudePhase2 = input.readDouble();
							FollowMeParam.flyingFormation.set(numUAV, receivedFormation);
							flightFormation = FlightFormation.getFormation(receivedFormation, numUAVs, minDistance);
							
							Location2DUTM targetLocationUTM = flightFormation.getLocation(flightFormationPosition, masterLocation, masterYaw);
							try {
								targetLocation = new Location2D(targetLocationUTM);
							} catch (ArduSimNotReadyException e) {
								e.printStackTrace();
								gui.exit(e.getMessage());
							}
						}
					}
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
						FollowMeParam.state.set(numUAV, READY_TO_FLY);
					}
				}
			}
		}
		
		/** READY TO FLY PHASE */
		gui.logUAV(FollowMeText.READY_TO_FLY);
		gui.updateProtocolState(FollowMeText.READY_TO_FLY);
		long lastReceivedTakeoffEnd = 0;	// For the timeout at the end of the takeoff process
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_READY_TO_FLY_ACK_LISTENER);
			acks.clear();
			while (FollowMeParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.READY_TO_FLY_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numSlaves) {
							FollowMeParam.state.set(numUAV, WAIT_SLAVES);
						}
					}
				}
			}
			
			/** WAIT SLAVES PHASE */
			gui.logUAV(FollowMeText.WAIT_SLAVES);
			gui.updateProtocolState(FollowMeText.WAIT_SLAVES);
			gui.logVerboseUAV(FollowMeText.MASTER_TARGET_REACHED_ACK_LISTENER);
			acks.clear();
			while (FollowMeParam.state.get(numUAV) == WAIT_SLAVES) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TARGET_REACHED_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numSlaves) {
							FollowMeParam.state.set(numUAV, READY_TO_START);
						}
					}
				}
			}
			
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_READY_TO_FLY_LISTENER);
			while (FollowMeParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedReadyToFly > FollowMeParam.SETUP_TIMEOUT) {
					if (idPrev == FlightFormation.BROADCAST_MAC_ID) {
						FollowMeParam.state.set(numUAV, TAKING_OFF);
					} else {
						FollowMeParam.state.set(numUAV, WAIT_TAKE_OFF);
					}
				}
			}
			
			/** WAIT TAKE OFF PHASE */
			if (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				gui.logUAV(FollowMeText.WAIT_TAKE_OFF);
				gui.updateProtocolState(FollowMeText.WAIT_TAKE_OFF);
				gui.logVerboseUAV(FollowMeText.LISTENER_WAITING_TAKE_OFF);
				while (FollowMeParam.state.get(numUAV) == WAIT_TAKE_OFF) {
					inBuffer = link.receiveMessage();
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();
						
						if (type == Message.TAKE_OFF_NOW || type == Message.TARGET_REACHED_ACK) {
							long id = input.readLong();
							if (id == idPrev) {
								FollowMeParam.state.set(numUAV, TAKING_OFF);
							}
						}
					}
				}
			}
			
			/** TAKING OFF PHASE */
			gui.logUAV(FollowMeText.TAKING_OFF);
			gui.updateProtocolState(FollowMeText.TAKING_OFF);
			
			copter.takeOff(takeoffAltitudePhase1, new TakeOffListener() {
				
				@Override
				public void onFailureListener() {
					gui.exit(FollowMeText.TAKE_OFF_ERROR + " " + selfId);
				}
				
				@Override
				public void onCompletedListener() {
					FollowMeParam.state.set(numUAV, MOVE_TO_TARGET);
				}
			}).start();
			gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
			while (FollowMeParam.state.get(numUAV) == TAKING_OFF) {
				// Discard message
				link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
			}
			
			/** MOVE TO TARGET PHASE */
			gui.logUAV(FollowMeText.MOVE_TO_TARGET);
			gui.updateProtocolState(FollowMeText.MOVE_TO_TARGET);
			gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
			copter.moveTo(targetLocation.getGeoLocation(), takeoffAltitudePhase2,
					new MoveToListener() {
						
						@Override
						public void onFailureListener() {
							gui.exit(FollowMeText.MOVE_ERROR + " " + selfId);
						}
						
						@Override
						public void onCompletedListener() {
							FollowMeParam.state.set(numUAV, TARGET_REACHED);
						}
					}).start();
			
			while (FollowMeParam.state.get(numUAV) == MOVE_TO_TARGET) {
				// Discard message
				link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
			}
			
			/** TARGET REACHED PHASE */
			gui.logUAV(FollowMeText.TARGET_REACHED);
			gui.updateProtocolState(FollowMeText.TARGET_REACHED);
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_TAKEOFF_END_ACK);
			while (FollowMeParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
						FollowMeParam.state.set(numUAV, READY_TO_START);
					}
				}
			}
			
		}
		
		/** READY TO START PHASE */
		gui.logUAV(FollowMeText.READY_TO_START);
		gui.updateProtocolState(FollowMeText.READY_TO_START);
		if (this.isMaster) {
			gui.logVerboseUAV(FollowMeText.MASTER_TAKEOFF_END_ACK_LISTENER);
			acks.clear();
			while (FollowMeParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TAKEOFF_END_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numSlaves) {
							FollowMeParam.state.set(numUAV, SETUP_FINISHED);
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_TAKEOFF_END_LISTENER);
			while (FollowMeParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedTakeoffEnd > FollowMeParam.TAKEOFF_TIMEOUT) {
					FollowMeParam.state.set(numUAV, SETUP_FINISHED);
				}
			}
		}
		
		/** SETUP FINISHED PHASE */
		gui.logUAV(FollowMeText.SETUP_FINISHED);
		gui.updateProtocolState(FollowMeText.SETUP_FINISHED);
		gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
		while (FollowMeParam.state.get(numUAV) == SETUP_FINISHED) {
			// Discard message
			link.receiveMessage(FollowMeParam.RECEIVING_TIMEOUT);
			// Coordination with ArduSim
			if (ardusim.isExperimentInProgress()) {
				FollowMeParam.state.set(numUAV, FOLLOWING);
			}
		}
		
		/** FOLLOWING PHASE */
		gui.logUAV(FollowMeText.FOLLOWING);
		gui.updateProtocolState(FollowMeText.FOLLOWING);
		long waitingTime;
		if (this.isMaster) {
			// Wait until the master UAV descends below a threshold (in the remote thread)
			while (FollowMeParam.state.get(numUAV) == FOLLOWING) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_ORDER_LISTENER);
			Location2DUTM masterLocation;
			while (FollowMeParam.state.get(numUAV) == FOLLOWING) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.I_AM_HERE){
						masterLocation = new Location2DUTM(input.readDouble(), input.readDouble());
						double relAltitude = input.readDouble();
						double yaw = input.readDouble();
						try {
							targetLocation = new Location2D(flightFormation.getLocation(flightFormationPosition, masterLocation, yaw));
							copter.moveTo(targetLocation.getGeoLocation(), relAltitude);
						} catch (ArduSimNotReadyException e) {
							gui.log(e.getMessage());
							e.printStackTrace();
							// Fatal error. It lands
							FollowMeParam.state.set(numUAV, LANDING);
						}
						
					}
					
					if (type == Message.LAND) {
						Location2DUTM centerUAVFinalLocation = new Location2DUTM(input.readDouble(), input.readDouble());
						double yaw = input.readDouble();
						double minLandDistance = input.readDouble();
						FlightFormation landingFormation = FlightFormation.getFormation(FollowMeParam.flyingFormation.get(numUAV),
								numUAVs, minLandDistance);
						Location2DUTM landingLocationUTM = landingFormation.getLocation(flightFormationPosition, centerUAVFinalLocation, yaw);
						try {
							targetLocation = new Location2D(landingLocationUTM);
							FollowMeParam.state.set(numUAV, MOVE_TO_LAND);
						} catch (ArduSimNotReadyException e) {
							gui.log(e.getMessage());
							e.printStackTrace();
							// Fatal error. It lands
							FollowMeParam.state.set(numUAV, LANDING);
						}
					}
				}
			}
			
			/** MOVE TO LAND PHASE */
			if (FollowMeParam.state.get(numUAV) == MOVE_TO_LAND) {
				gui.logUAV(FollowMeText.MOVE_TO_LAND);
				gui.updateProtocolState(FollowMeText.MOVE_TO_LAND);
				gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
				MoveTo moveTo = copter.moveTo(targetLocation.getGeoLocation(), copter.getAltitudeRelative(),
						new MoveToListener() {
							
							@Override
							public void onFailureListener() {
								gui.exit(FollowMeText.MOVE_ERROR + " " + selfId);
							}
							
							@Override
							public void onCompletedListener() {
								// Nothing to do, as we wait until the target location is reached with Thread.join()
							}
						});
				moveTo.start();
				try {
					moveTo.join();
				} catch (InterruptedException e) {}
				FollowMeParam.state.set(numUAV, LANDING);
			}
		}
		
		/** LANDING PHASE */
		if (!copter.land()) {
			gui.exit(FollowMeText.LAND_ERROR + " " + selfId);
		}
		gui.logUAV(FollowMeText.LANDING);
		gui.updateProtocolState(FollowMeText.LANDING);
		gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		while (FollowMeParam.state.get(numUAV) == LANDING) {
			if(!copter.isFlying()) {
				FollowMeParam.state.set(numUAV, FINISH);
			} else {
				cicleTime = cicleTime + FollowMeParam.LAND_CHECK_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		
		/** FINISH PHASE */
		gui.logUAV(FollowMeText.FINISH);
		gui.updateProtocolState(FollowMeText.FINISH);
		gui.logVerboseUAV(FollowMeText.LISTENER_FINISHED);
	}
	
	
}
