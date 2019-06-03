package muscop.logic;

import static muscop.pojo.State.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location3DUTM;
import api.pojo.location.Location2DUTM;
import api.pojo.location.WaypointSimplified;
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
import muscop.pojo.Message;
import muscop.pojo.MovedMission;

/** 
 * Thread used to listen for messages sent by other UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ListenerThread extends Thread {

	private int numUAV;
	private long selfId;
	private boolean isMaster;
	private Copter copter;
	private GUI gui;
	private byte[] inBuffer;
	private Input input;
	private CommLink link;
	private ArduSim ardusim;

	@SuppressWarnings("unused")
	private ListenerThread() {}
	
	public ListenerThread(int numUAV) {
		this.numUAV = numUAV;
		this.isMaster = MUSCOPHelper.isMaster(numUAV);
		this.copter = API.getCopter(numUAV);
		this.selfId = this.copter.getID();
		this.gui = API.getGUI(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.link = API.getCommLink(numUAV);
		this.ardusim = API.getArduSim();
	}

	@Override
	public void run() {
		
		while (!ardusim.isAvailable()) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		Map<Long, Location2DUTM> UAVsDetected = null;
		gui.logUAV(MUSCOPText.START);
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_START_LISTENER);
			UAVsDetected = new HashMap<Long, Location2DUTM>();	// Detecting UAVs
			int numUAVsDetected = 0;
			while (MUSCOPParam.state.get(numUAV) == START) {
				inBuffer = link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.HELLO) {
						// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
						Long idSlave = input.readLong();
						Location2DUTM location = new Location2DUTM(input.readDouble(), input.readDouble());
						// ADD to Map the new UAV detected
						UAVsDetected.put(idSlave, location);
						if (UAVsDetected.size() > numUAVsDetected) {
							numUAVsDetected = UAVsDetected.size();
							gui.log(MUSCOPText.MASTER_DETECTED_UAVS + numUAVsDetected);
						}
					}
				}
				// Coordination with ArduSim
				if (ardusim.isSetupInProgress() || ardusim.isSetupFinished()) {// The setup could be too fast
					MUSCOPParam.state.set(numUAV, SETUP);
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == START) {
				// Discard message
				link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				// Coordination with ArduSim
				if (ardusim.isSetupInProgress() || ardusim.isSetupFinished()) {
					MUSCOPParam.state.set(numUAV, SETUP);
				}
			}
		}
		
		/** SETUP PHASE */
		gui.logUAV(MUSCOPText.SETUP);
		gui.updateProtocolState(MUSCOPText.SETUP);
		long idPrev = 0;					// Identifier of the previous UAV in the takeoff sequence
		long lastReceivedReadyToFly = 0;	// Used in slaves to detect the end of this phase
		Map<Long, Long> acks = null;
		if (this.isMaster) {
			// 1. Make a permanent list of UAVs detected, including master
			// 1.1. Add master to the Map
			Location2DUTM masterLocation = copter.getLocationUTM();
			UAVsDetected.put(this.selfId, new Location2DUTM(masterLocation.x, masterLocation.y));
			// 1.2. Set the number of UAVs running
			int numUAVs = UAVsDetected.size();
			
			// 2. Set the heading used to orient the formation
			if (ardusim.getArduSimRole() == ArduSim.MULTICOPTER) {
				// We use the master heading
				MUSCOPParam.formationHeading = copter.getHeading();
			}// In simulation, centerHeading is assigned when MUSCOPHelper.setStartingLocation() is called,
			 //   using the first mission segment to orient the formation
			
			// 3. Calculus of the UAVs distribution that better fit the current layout on the ground
			Formation flyingFormation = FlightFormation.getFlyingFormation();
			FlightFormation airFormation = FlightFormation.getFormation(flyingFormation, numUAVs, FlightFormation.getFlyingFormationDistance());
			Triplet<Integer, Long, Location2DUTM>[] match =
					FlightFormation.matchIDs(UAVsDetected, MUSCOPParam.formationHeading, true, null, airFormation);
			
			// 4. Get the target coordinates for the central UAV
			int centerUAVAirPos = airFormation.getCenterUAVPosition();	// Position in the formation
			Triplet<Integer, Long, Location2DUTM> centerUAVAir = null;
			for (int i = 0; i < numUAVs; i++) {
				if (match[i].getValue0() == centerUAVAirPos) {
					centerUAVAir = match[i];
				}
			}
			if (centerUAVAir == null) {
				gui.exit(MUSCOPText.CENTER_ID_NOT_FOUND);
			}
			long centerUAVId = centerUAVAir.getValue1();
			
			// 5. Load the mission
			List<WaypointSimplified> screenMission = copter.getMissionHelper().getSimplified();
			if (screenMission == null || screenMission.size() > MUSCOPParam.MAX_WAYPOINTS) {
				gui.exit(MUSCOPText.MAX_WP_REACHED);
			}
			
			// 6. Calculate the takeoff altitude
			double takeoffAltitude = screenMission.get(0).z;
			double takeOffAltitudeStepOne;
			if (takeoffAltitude <= 5.0) {
				takeOffAltitudeStepOne = 2.0;
			} else if (takeoffAltitude >= 10.0) {
				takeOffAltitudeStepOne = 5.0;
			} else {
				takeOffAltitudeStepOne = takeoffAltitude / 2;
			}
			
			// 7. Calculus of the mission of all the UAVs
			// 7.1. Center UAV mission
			Location3DUTM[] centerMission = new Location3DUTM[screenMission.size()];
			centerMission[0] = new Location3DUTM(centerUAVAir.getValue2().x, centerUAVAir.getValue2().y, takeoffAltitude);
			for (int i = 1; i < screenMission.size(); i++) {
				WaypointSimplified wp = screenMission.get(i);
				centerMission[i] = new Location3DUTM(wp.x, wp.y, wp.z);
			}
			// 7.2. Copying the mission of the central UAV to the others
			Map<Integer, MovedMission> movedMission = new HashMap<Integer, MovedMission>((int)Math.ceil(numUAVs / 0.75) + 1);
			int formationPosition;
			long id;
			Location3DUTM[] locations;
			Location3DUTM location, reference;
			for (int i = 0; i < numUAVs; i++) {
				formationPosition = match[i].getValue0();
				id = match[i].getValue1();
				locations = new Location3DUTM[centerMission.length];
				if (id == centerUAVId) {
					for (int j = 0; j < centerMission.length; j++) {
						locations[j] = centerMission[j];
					}
				} else {
					Location2DUTM offset = airFormation.getOffset(formationPosition, MUSCOPParam.formationHeading);
					for (int j = 0; j < centerMission.length; j++) {
						reference = centerMission[j];
						location = new Location3DUTM(reference);
						location.x = location.x + offset.x;
						location.y = location.y + offset.y;
						//location.z = location.z;	// Not necessary
						locations[j] = location;
					}
				}
				movedMission.put(formationPosition, new MovedMission(id, formationPosition, locations));
			}
			
			// 7.3 Define the takeoff sequence and store data
			byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
			Output output = new Output(outBuffer);
			Pair<Integer, Long>[] sequence = FlightFormation.getTakeoffSequence(UAVsDetected, match);
			Map<Long, byte[]> messages = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
			long prevId, nextId;
			MovedMission currentMission;
			Location3DUTM[] points;
			// i represents the position in the takeoff sequence
			for (int i = 0; i < numUAVs; i++) {
				if (i == 0) {
					prevId = FlightFormation.BROADCAST_MAC_ID;
				} else {
					prevId = sequence[i - 1].getValue1();
				}
				if (i == numUAVs - 1) {
					nextId = FlightFormation.BROADCAST_MAC_ID;
				} else {
					nextId = sequence[i + 1].getValue1();
				}
				currentMission = movedMission.get(sequence[i].getValue0());
				points = currentMission.locations;
				formationPosition = currentMission.formationPosition;
				id = currentMission.id;
				if (this.selfId == id) {
					if (this.selfId == centerUAVId) {
						MUSCOPParam.iAmCenter[numUAV].set(true);
					}
					MUSCOPParam.idPrev.set(numUAV, prevId);
					MUSCOPParam.idNext.set(numUAV, nextId);
					idPrev = prevId;
					MUSCOPParam.numUAVs.set(numUAV, numUAVs);
					MUSCOPParam.flyingFormation.set(numUAV, flyingFormation);
					MUSCOPParam.flyingFormationPosition.set(numUAV, formationPosition);
					MUSCOPParam.flyingFormationHeading.set(numUAV, MUSCOPParam.formationHeading);
					MUSCOPParam.takeoffAltitude.set(numUAV, takeOffAltitudeStepOne);
					MUSCOPParam.uavMissionReceivedUTM.set(numUAV, points);
					Location2DGeo[] missionMasterGeo = new Location2DGeo[points.length];
					for (int j = 0; j < points.length; j++) {
						try {
							missionMasterGeo[j] = points[j].getGeo();
						} catch (ArduSimNotReadyException e) {
							e.printStackTrace();
							gui.exit(e.getMessage());
						}
					}
					MUSCOPParam.uavMissionReceivedGeo.set(numUAV, missionMasterGeo);
					// Data for the masterPosition remain null
				} else {
					output.clear();
					output.writeShort(Message.DATA);
					output.writeLong(id);
					output.writeLong(centerUAVId);
					output.writeLong(prevId);
					output.writeLong(nextId);
					output.writeInt(numUAVs);
					output.writeShort(flyingFormation.getFormationId());
					output.writeInt(formationPosition);
					output.writeDouble(MUSCOPParam.formationHeading);
					output.writeDouble(takeOffAltitudeStepOne);
					// Number of points (Necessary for the Talker to know how many to read)
					output.writeInt(points.length);
					// Points to send
					for (int j = 0; j < points.length; j++) {
						output.writeDouble(points[j].x);
						output.writeDouble(points[j].y);
						output.writeDouble(points[j].z);
					}
					output.flush();
					messages.put(id, Arrays.copyOf(outBuffer, output.position()));
				}
			}
			MUSCOPParam.data.set(messages.values().toArray(new byte[messages.size()][]));
			
			try {
				output.close();
			} catch (KryoException e) {}
			
			
			// 4. Wait for data ack from all the slaves
			gui.logVerboseUAV(MUSCOPText.MASTER_DATA_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							MUSCOPParam.state.set(numUAV, READY_TO_FLY);
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_WAIT_DATA_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.DATA && MUSCOPParam.uavMissionReceivedGeo.get(numUAV) == null) {
						long id = input.readLong();
						if (id == selfId) {
							if (this.selfId == input.readLong()) {
								MUSCOPParam.iAmCenter[numUAV].set(true);
							}
							idPrev = input.readLong();
							MUSCOPParam.idPrev.set(numUAV, idPrev);
							MUSCOPParam.idNext.set(numUAV, input.readLong());
							MUSCOPParam.numUAVs.set(numUAV, input.readInt());
							MUSCOPParam.flyingFormation.set(numUAV, Formation.getFormation(input.readShort()));
							MUSCOPParam.flyingFormationPosition.set(numUAV, input.readInt());
							MUSCOPParam.flyingFormationHeading.set(numUAV, input.readDouble());
							MUSCOPParam.takeoffAltitude.set(numUAV, input.readDouble());

							// The number of WP is read to know the loop length
							int size = input.readInt();
							Location3DUTM[] mission = new Location3DUTM[size];
							Location2DGeo[] missionGeo = new Location2DGeo[size];

							for (int i = 0; i < mission.length; i++) {
								mission[i] = new Location3DUTM(input.readDouble(),
										input.readDouble(), input.readDouble());
								try {
									missionGeo[i] = mission[i].getGeo();
								} catch (ArduSimNotReadyException e) {
									e.printStackTrace();
									gui.exit(e.getMessage());
								}
							}
							MUSCOPParam.uavMissionReceivedUTM.set(numUAV, mission);
							MUSCOPParam.uavMissionReceivedGeo.set(numUAV, missionGeo);
						}
					}
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
						MUSCOPParam.state.set(numUAV, READY_TO_FLY);
					}
				}
			}
		}
		
		/** READY TO FLY PHASE */
		gui.logUAV(MUSCOPText.READY_TO_FLY);
		gui.updateProtocolState(MUSCOPText.READY_TO_FLY);
		int numUAVs = MUSCOPParam.numUAVs.get(numUAV);
		
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_READY_TO_FLY_ACK_LISTENER);
			acks.clear();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.READY_TO_FLY_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							if (MUSCOPParam.idPrev.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
								MUSCOPParam.state.set(numUAV, TAKING_OFF);
							} else {
								MUSCOPParam.state.set(numUAV, WAIT_TAKE_OFF);
							}
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_WAIT_READY_TO_FLY_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedReadyToFly > MUSCOPParam.SETUP_TIMEOUT) {
					if (MUSCOPParam.idPrev.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
						MUSCOPParam.state.set(numUAV, TAKING_OFF);
					} else {
						MUSCOPParam.state.set(numUAV, WAIT_TAKE_OFF);
					}
				}
			}
		}
		
		/** WAIT TAKE OFF PHASE */
		if (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			gui.logUAV(MUSCOPText.WAIT_TAKE_OFF);
			gui.updateProtocolState(MUSCOPText.WAIT_TAKE_OFF);
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING_TAKE_OFF);
			while (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKE_OFF_NOW || type == Message.TARGET_REACHED_ACK) {
						long id = input.readLong();
						if (id == idPrev) {
							MUSCOPParam.state.set(numUAV, TAKING_OFF);
						}
					}
				}
			}
		}
		
		/** TAKING OFF PHASE */
		gui.logUAV(MUSCOPText.TAKING_OFF);
		gui.updateProtocolState(MUSCOPText.TAKING_OFF);
		double altitude = MUSCOPParam.takeoffAltitude.get(numUAV);
		copter.takeOff(altitude, new TakeOffListener() {
			
			@Override
			public void onFailureListener() {
				gui.exit(MUSCOPText.TAKE_OFF_ERROR + " " + selfId);
			}
			
			@Override
			public void onCompletedListener() {
				MUSCOPParam.state.set(numUAV, MOVE_TO_TARGET);
			}
		}).start();
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == TAKING_OFF) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
		}
		
		/** MOVE TO TARGET PHASE */
		gui.logUAV(MUSCOPText.MOVE_TO_WP + " 0");
		gui.updateProtocolState(MUSCOPText.MOVE_TO_WP + " 0");
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		Location3DUTM[] missionUTM = MUSCOPParam.uavMissionReceivedUTM.get(numUAV);
		Location2DGeo[] missionGeo = MUSCOPParam.uavMissionReceivedGeo.get(numUAV);
		Location2DGeo destinationGeo = missionGeo[0];
		double relAltitude = missionUTM[0].z;
		copter.moveTo(destinationGeo, relAltitude, new MoveToListener() {
			
			@Override
			public void onFailureListener() {
				gui.exit(MUSCOPText.MOVE_ERROR_2 + " " + selfId);
			}
			
			@Override
			public void onCompletedListener() {
				MUSCOPParam.state.set(numUAV, TARGET_REACHED);
			}
		}).start();
		while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
		}
		
		/** TARGET REACHED PHASE */
		gui.logUAV(MUSCOPText.TARGET_REACHED);
		gui.updateProtocolState(MUSCOPText.TARGET_REACHED);
		boolean iAmCenter = MUSCOPParam.iAmCenter[numUAV].get();
		long lastReceivedTakeoffEnd = 0;
		if (iAmCenter) {
			gui.logVerboseUAV(MUSCOPText.CENTER_TARGET_REACHED_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TARGET_REACHED_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							MUSCOPParam.state.set(numUAV, READY_TO_START);
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAIT_TAKEOFF_END_ACK);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
						MUSCOPParam.state.set(numUAV, READY_TO_START);
					}
				}
			}
		}
		
		/** READY TO START PHASE */
		gui.logUAV(MUSCOPText.READY_TO_START);
		gui.updateProtocolState(MUSCOPText.READY_TO_START);
		if (iAmCenter) {
			gui.logVerboseUAV(MUSCOPText.CENTER_TAKEOFF_END_ACK_LISTENER);
			acks.clear();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TAKEOFF_END_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							MUSCOPParam.state.set(numUAV, SETUP_FINISHED);
						}
					}
				}
			}
		} else {
			gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedTakeoffEnd > MUSCOPParam.TAKEOFF_TIMEOUT) {
					MUSCOPParam.state.set(numUAV, SETUP_FINISHED);
				}
			}
		}
		
		/** SETUP FINISHED PHASE */
		gui.logUAV(MUSCOPText.SETUP_FINISHED);
		gui.updateProtocolState(MUSCOPText.SETUP_FINISHED);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == SETUP_FINISHED) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
			// Coordination with ArduSim
			if (ardusim.isExperimentInProgress()) {
				MUSCOPParam.state.set(numUAV, FOLLOWING_MISSION);
			}
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		Map<Long, Long> reached = null;
		if (iAmCenter) {
			reached = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
		}
		int currentWP = 0;
		MUSCOPParam.moveSemaphore.set(numUAV, 1);	// We reach waypoint 0 and start moving towards waypoint 1
		Location2DUTM centerUAVFinalLocation = null;
		while (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			gui.logUAV(MUSCOPText.WP_REACHED);
			gui.updateProtocolState(MUSCOPText.WP_REACHED);
			if (iAmCenter) {
				gui.logVerboseUAV(MUSCOPText.CENTER_WP_REACHED_ACK_LISTENER);
				reached.clear();
				
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					inBuffer = link.receiveMessage();
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();

						if (type == Message.WAYPOINT_REACHED_ACK) {
							long id = input.readLong();
							int wp = input.readInt();
							if (wp == currentWP) {
								reached.put(id, id);
								if (reached.size() == numUAVs - 1) {
									if (currentWP == missionUTM.length - 1) {
										MUSCOPParam.state.set(numUAV, LANDING);
									}
									MUSCOPParam.wpReachedSemaphore.incrementAndGet(numUAV);
								}
							}
						}
					}
				}
			} else {
				gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAIT_ORDER_LISTENER);
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					inBuffer = link.receiveMessage();
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();
						
						if (type == Message.MOVE_TO_WAYPOINT){
							int wp = input.readInt();
							if (wp > currentWP) {
								MUSCOPParam.wpReachedSemaphore.incrementAndGet(numUAV);
							}
						}
						
						if (type == Message.LAND) {
							centerUAVFinalLocation = new Location2DUTM(input.readDouble(), input.readDouble());
							MUSCOPParam.state.set(numUAV, MOVE_TO_LAND);
							MUSCOPParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
				currentWP++;
				gui.logUAV(MUSCOPText.MOVE_TO_WP + " " + currentWP);
				gui.updateProtocolState(MUSCOPText.MOVE_TO_WP + " " + currentWP);
				gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
				
				destinationGeo = missionGeo[currentWP];
				relAltitude = missionUTM[currentWP].z;
				copter.moveTo(destinationGeo, relAltitude, new MoveToListener() {
					
					@Override
					public void onFailureListener() {
						gui.exit(MUSCOPText.MOVE_ERROR_1 + " " + selfId);
					}
					
					@Override
					public void onCompletedListener() {
						MUSCOPParam.moveSemaphore.incrementAndGet(numUAV);
					}
				}).start();
				while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
					// Discard message
					link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				}
			}
		}
		
		/** MOVE TO LAND PHASE */
		long waitingTime;
		// This only happens for UAVs not located in the center of the formation
		if (MUSCOPParam.state.get(numUAV) == MOVE_TO_LAND) {
			gui.logUAV(MUSCOPText.MOVE_TO_LAND);
			gui.updateProtocolState(MUSCOPText.MOVE_TO_LAND);
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			FlightFormation flyingFormation =
					FlightFormation.getFormation(MUSCOPParam.flyingFormation.get(numUAV), numUAVs, FlightFormation.getLandingFormationDistance());
			int position = MUSCOPParam.flyingFormationPosition.get(numUAV);
			double formationHeading = MUSCOPParam.flyingFormationHeading.get(numUAV);
			Location2DUTM landingLocation = flyingFormation.getLocation(position, centerUAVFinalLocation, formationHeading);
			try {
				destinationGeo = landingLocation.getGeo();
				relAltitude = (float)copter.getAltitudeRelative();
				MoveTo moveTo = copter.moveTo(destinationGeo, relAltitude, new MoveToListener() {
					
					@Override
					public void onFailureListener() {
						gui.exit(MUSCOPText.MOVE_ERROR_2 + " " + selfId);
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
				MUSCOPParam.state.set(numUAV, LANDING);
			} catch (ArduSimNotReadyException e) {
				gui.log(e.getMessage());
				e.printStackTrace();
				MUSCOPParam.state.set(numUAV, LANDING);
			}
		}
		
		/** LANDING PHASE */
		if (!copter.land()) {
			gui.exit(MUSCOPText.LAND_ERROR + " " + selfId);
		}
		gui.logUAV(MUSCOPText.LANDING_UAV);
		gui.updateProtocolState(MUSCOPText.LANDING_UAV);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		while (MUSCOPParam.state.get(numUAV) == LANDING) {
			if(!copter.isFlying()) {
				MUSCOPParam.state.set(numUAV, FINISH);
			} else {
				cicleTime = cicleTime + MUSCOPParam.LAND_CHECK_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
		
		/** FINISH PHASE */
		gui.logUAV(MUSCOPText.FINISH);
		gui.updateProtocolState(MUSCOPText.FINISH);
		gui.logVerboseUAV(MUSCOPText.LISTENER_FINISHED);
	}

}