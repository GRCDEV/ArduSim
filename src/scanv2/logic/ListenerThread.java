package scanv2.logic;

import static scanv2.pojo.State.*;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UAV2DLocation;
import api.pojo.UTMCoordinates;
import api.pojo.WaypointSimplified;
import api.pojo.formations.FlightFormation;
import api.pojo.formations.FlightFormation.Formation;
import main.Text;
import scanv2.pojo.Message;
import scanv2.pojo.MovedMission;

public class ListenerThread extends Thread {

	private int numUAV;
	private long selfId;
	private boolean isMaster;
	
	byte[] inBuffer;
	Input input;

	@SuppressWarnings("unused")
	private ListenerThread() {}
	
	public ListenerThread(int numUAV) {
		this.numUAV = numUAV;
		this.selfId = Tools.getIdFromPos(numUAV);
		this.isMaster = ScanHelper.isMaster(numUAV);
		
		this.inBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
	}

	@Override
	public void run() {
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		Map<Long, UAV2DLocation> UAVsDetected = null;
		GUI.log(numUAV, ScanText.START);
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.MASTER_START_LISTENER);
			UAVsDetected = new HashMap<Long, UAV2DLocation>();	// Detecting UAVs
			int numUAVsDetected = 0;
			while (ScanParam.state.get(numUAV) == START) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.HELLO) {
						// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
						Long idSlave = input.readLong();
						UAV2DLocation location = new UAV2DLocation(idSlave, input.readDouble(), input.readDouble());
						// ADD to Map the new UAV detected
						UAVsDetected.put(idSlave, location);
						if (UAVsDetected.size() > numUAVsDetected) {
							numUAVsDetected = UAVsDetected.size();
							GUI.log(ScanText.MASTER_DETECTED_UAVS + numUAVsDetected);
						}
					}
				}
				// Coordination with ArduSim
				if (Tools.isSetupInProgress() || Tools.isSetupFinished()) {// The setup could be too fast
					ScanParam.state.set(numUAV, SETUP);
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
			while (ScanParam.state.get(numUAV) == START) {
				// Discard message
				Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				// Coordination with ArduSim
				if (Tools.isSetupInProgress() || Tools.isSetupFinished()) {
					ScanParam.state.set(numUAV, SETUP);
				}
			}
		}
		
		/** SETUP PHASE */
		GUI.log(numUAV, ScanText.SETUP);
		GUI.updateProtocolState(numUAV, ScanText.SETUP);
		long lastReceivedReadyToFly = 0;	// Used in slaves to detect the end of this phase
		Map<Long, Long> acks = null;
		if (this.isMaster) {
			// 1. Make a permanent list of UAVs detected, including master
			// 1.1. Add master to the Map
			Point2D.Double masterLocation = Copter.getUTMLocation(numUAV);
			UAVsDetected.put(this.selfId, new UAV2DLocation(this.selfId, masterLocation.x, masterLocation.y));
			// 1.2. Get the list
			UAV2DLocation[] currentLocations = UAVsDetected.values().toArray(new UAV2DLocation[UAVsDetected.size()]);
			// 1.3. Set the number of UAVs running
			int numUAVs = currentLocations.length;
			
			// 2. Set the heading used to orient the formation
			if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
				// We use the master heading
				ScanParam.formationHeading = Copter.getHeading(numUAVs);
			}// In simulation, centerHeading is assigned when SwarmProtHelper.setStartingLocation() is called,
			 //   using the first mission segment to orient the formation
			
			// 3. Calculus of the UAVs distribution that better fit the current layout on the ground
			Formation flyingFormation = FlightFormation.getFlyingFormation();
			FlightFormation airFormation = FlightFormation.getFormation(flyingFormation, numUAVs, FlightFormation.getFlyingFormationDistance());
			Triplet<Integer, Long, UTMCoordinates>[] match = airFormation.matchIDs(currentLocations, ScanParam.formationHeading);
			
			// 4. Get the target coordinates for the central UAV
			int centerUAVAirPos = airFormation.getCenterUAVPosition();	// Position in the formation
			Triplet<Integer, Long, UTMCoordinates> centerUAVAir = null;
			for (int i = 0; i < numUAVs; i++) {
				if (match[i].getValue0() == centerUAVAirPos) {
					centerUAVAir = match[i];
				}
			}
			if (centerUAVAir == null) {
				GUI.exit(ScanText.CENTER_ID_NOT_FOUND);
			}
			long centerUAVId = centerUAVAir.getValue1();
			
			// 5. Load the mission
			List<WaypointSimplified> screenMission = null;
			if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
				screenMission = Tools.getUAVMissionSimplified(numUAV);
			} else {
				// In simulation we need to locate which is the center UAV on the ground
				for (int i = 0; i < numUAVs && screenMission == null; i++) {
					screenMission = Tools.getUAVMissionSimplified(i);
				}
			}
			if (screenMission == null || screenMission.size() > ScanParam.MAX_WAYPOINTS) {
				GUI.exit(ScanText.MAX_WP_REACHED);
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
			Point3D[] centerMission = new Point3D[screenMission.size()];
			centerMission[0] = new Point3D(centerUAVAir.getValue2().x, centerUAVAir.getValue2().y, takeoffAltitude);
			for (int i = 1; i < screenMission.size(); i++) {
				WaypointSimplified wp = screenMission.get(i);
				centerMission[i] = new Point3D(wp.x, wp.y, wp.z);
			}
			// 7.2. Copying the mission of the central UAV to the others
			Map<Integer, MovedMission> movedMission = new HashMap<Integer, MovedMission>((int)Math.ceil(numUAVs / 0.75) + 1);
			int formationPosition;
			long id;
			Point3D[] locations;
			Point3D location, reference;
			for (int i = 0; i < numUAVs; i++) {
				formationPosition = match[i].getValue0();
				id = match[i].getValue1();
				locations = new Point3D[centerMission.length];
				if (id == centerUAVId) {
					for (int j = 0; j < centerMission.length; j++) {
						locations[j] = centerMission[j];
					}
				} else {
					UTMCoordinates offset = airFormation.getOffset(formationPosition, ScanParam.formationHeading);
					for (int j = 0; j < centerMission.length; j++) {
						location = new Point3D();
						reference = centerMission[j];
						location.x = reference.x + offset.x;
						location.y = reference.y + offset.y;
						location.z = reference.z;
						locations[j] = location;
					}
				}
				movedMission.put(formationPosition, new MovedMission(id, formationPosition, locations));
			}
			
			// 7.3 Define the takeoff sequence and store data
			byte[] outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			Output output = new Output(outBuffer);
			Pair<Integer, Long>[] sequence = airFormation.getTakeoffSequence(centerUAVAir, ScanParam.formationHeading, match);
			Map<Long, byte[]> messages = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
			long prevId, nextId;
			MovedMission currentMission;
			Point3D[] points;
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
				points = currentMission.posiciones;
				formationPosition = currentMission.formationPosition;
				id = currentMission.id;
				if (this.selfId == id) {
					if (this.selfId == centerUAVId) {
						ScanParam.iAmCenter[numUAV].set(true);
					}
					ScanParam.idPrev.set(numUAV, prevId);
					ScanParam.idNext.set(numUAV, nextId);
					ScanParam.numUAVs.set(numUAV, numUAVs);
					ScanParam.flyingFormation.set(numUAV, flyingFormation);
					ScanParam.flyingFormationPosition.set(numUAV, formationPosition);
					ScanParam.flyingFormationHeading.set(numUAV, ScanParam.formationHeading);
					ScanParam.takeoffAltitude.set(numUAV, takeOffAltitudeStepOne);
					ScanParam.uavMissionReceivedUTM.set(numUAV, points);
					GeoCoordinates[] missionMasterGeo = new GeoCoordinates[points.length];
					for (int j = 0; j < points.length; j++) {
						missionMasterGeo[j] = Tools.UTMToGeo(points[j].x, points[j].y);
					}
					ScanParam.uavMissionReceivedGeo.set(numUAV, missionMasterGeo);
					// ScanParam.data of masterPosition remains null
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
					output.writeDouble(ScanParam.formationHeading);
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
			ScanParam.data.set(messages.values().toArray(new byte[messages.size()][]));
			
			try {
				output.close();
			} catch (KryoException e) {}
			
			
			// 4. Wait for data ack from all the slaves
			GUI.logVerbose(numUAV, ScanText.MASTER_DATA_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (ScanParam.state.get(numUAV) == SETUP) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							ScanParam.state.set(numUAV, READY_TO_FLY);
						}
					}
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.SLAVE_WAIT_DATA_LISTENER);
			while (ScanParam.state.get(numUAV) == SETUP) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.DATA && ScanParam.uavMissionReceivedGeo.get(numUAV) == null) {
						long id = input.readLong();
						if (id == selfId) {
							if (this.selfId == input.readLong()) {
								ScanParam.iAmCenter[numUAV].set(true);
							}
							ScanParam.idPrev.set(numUAV, input.readLong());
							ScanParam.idNext.set(numUAV, input.readLong());
							ScanParam.numUAVs.set(numUAV, input.readInt());
							ScanParam.flyingFormation.set(numUAV, Formation.getFormation(input.readShort()));
							ScanParam.flyingFormationPosition.set(numUAV, input.readInt());
							ScanParam.flyingFormationHeading.set(numUAV, input.readDouble());
							ScanParam.takeoffAltitude.set(numUAV, input.readDouble());

							// The number of WP is read to know the loop length
							int size = input.readInt();
							Point3D[] mission = new Point3D[size];
							GeoCoordinates[] missionGeo = new GeoCoordinates[size];

							for (int i = 0; i < mission.length; i++) {
								mission[i] = new Point3D(input.readDouble(),
										input.readDouble(), input.readDouble());
								missionGeo[i] = Tools.UTMToGeo(mission[i].x, mission[i].y);
							}
							ScanParam.uavMissionReceivedUTM.set(numUAV, mission);
							ScanParam.uavMissionReceivedGeo.set(numUAV, missionGeo);
						}
					}
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
						ScanParam.state.set(numUAV, READY_TO_FLY);
					}
				}
			}
		}
		
		/** READY TO FLY PHASE */
		GUI.log(numUAV, ScanText.READY_TO_FLY);
		GUI.updateProtocolState(numUAV, ScanText.READY_TO_FLY);
		int numUAVs = ScanParam.numUAVs.get(numUAV);
		
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.MASTER_READY_TO_FLY_ACK_LISTENER);
			acks.clear();
			while (ScanParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.READY_TO_FLY_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							if (ScanParam.idPrev.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
								ScanParam.state.set(numUAV, TAKING_OFF);
							} else {
								ScanParam.state.set(numUAV, WAIT_TAKE_OFF);
							}
						}
					}
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.SLAVE_WAIT_READY_TO_FLY_LISTENER);
			while (ScanParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.READY_TO_FLY) {
						lastReceivedReadyToFly = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedReadyToFly > ScanParam.SETUP_TIMEOUT) {
					if (ScanParam.idPrev.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
						ScanParam.state.set(numUAV, TAKING_OFF);
					} else {
						ScanParam.state.set(numUAV, WAIT_TAKE_OFF);
					}
				}
			}
		}
		
		/** WAIT TAKE OFF PHASE */
		if (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			GUI.log(numUAV, ScanText.WAIT_TAKE_OFF);
			GUI.updateProtocolState(numUAV, ScanText.WAIT_TAKE_OFF);
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING_TAKE_OFF);
			while (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKE_OFF_NOW) {
						long id = input.readLong();
						if (id == selfId) {
							ScanParam.state.set(numUAV, TAKING_OFF);
						}
					}
				}
			}
		}
		
		/** TAKING OFF PHASE */
		GUI.log(numUAV, ScanText.TAKING_OFF);
		GUI.updateProtocolState(numUAV, ScanText.TAKING_OFF);
		double altitude = ScanParam.takeoffAltitude.get(numUAV);
		double thresholdAltitude = altitude * 0.95;
		if (!Copter.takeOffNonBlocking(numUAV, altitude)) {
			GUI.exit(ScanText.TAKE_OFF_ERROR + " " + selfId);
		}
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		long logTime = cicleTime;
		while (ScanParam.state.get(numUAV) == TAKING_OFF) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			// Wait until target altitude is reached
			if (System.currentTimeMillis() - cicleTime > ScanParam.TAKE_OFF_CHECK_TIMEOUT) {
				if (Copter.getZRelative(numUAV) >= thresholdAltitude) {
					ScanParam.state.set(numUAV, MOVE_TO_TARGET);
				} else {
					if (System.currentTimeMillis() - logTime > ScanParam.TAKE_OFF_LOG_TIMEOUT) {
						GUI.logVerbose(numUAV, Text.ALTITUDE_TEXT
								+ " = " + String.format("%.2f", Copter.getZ(numUAV))
								+ " " + Text.METERS);
						logTime = logTime + ScanParam.TAKE_OFF_LOG_TIMEOUT;
					}
					
					cicleTime = cicleTime + ScanParam.TAKE_OFF_CHECK_TIMEOUT;
				}
			}
		}
		
		/** MOVE TO TARGET PHASE */
		GUI.log(numUAV, ScanText.MOVE_TO_WP + " 0");
		GUI.updateProtocolState(numUAV, ScanText.MOVE_TO_WP + " 0");
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		Point3D[] missionUTM = ScanParam.uavMissionReceivedUTM.get(numUAV);
		GeoCoordinates[] missionGeo = ScanParam.uavMissionReceivedGeo.get(numUAV);
		GeoCoordinates destinationGeo = missionGeo[0];
		Point3D destinationUTM = missionUTM[0];
		double relAltitude = missionUTM[0].z;
		if (!Copter.moveUAVNonBlocking(numUAV, destinationGeo, (float)relAltitude)) {
			GUI.exit(ScanText.MOVE_ERROR_2 + " " + selfId);
		}
		cicleTime = System.currentTimeMillis();
		while (ScanParam.state.get(numUAV) == MOVE_TO_TARGET) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			// Wait until target location is reached
			if (System.currentTimeMillis() - cicleTime > ScanParam.MOVE_CHECK_TIMEOUT) {
				if (Copter.getUTMLocation(numUAV).distance(destinationUTM) <= ScanParam.MIN_DISTANCE_TO_WP
						&& Math.abs(relAltitude - Copter.getZRelative(numUAV)) <= 0.2) {
					ScanParam.state.set(numUAV, TARGET_REACHED);
				} else {
					cicleTime = cicleTime + ScanParam.MOVE_CHECK_TIMEOUT;
				}
			}
		}
		
		/** TARGET REACHED PHASE */
		GUI.log(numUAV, ScanText.TARGET_REACHED);
		GUI.updateProtocolState(numUAV, ScanText.TARGET_REACHED);
		boolean iAmCenter = ScanParam.iAmCenter[numUAV].get();
		long lastReceivedTakeoffEnd = 0;
		if (iAmCenter) {
			GUI.logVerbose(numUAV, ScanText.CENTER_TARGET_REACHED_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (ScanParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TARGET_REACHED_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							ScanParam.state.set(numUAV, READY_TO_START);
						}
					}
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (ScanParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
						ScanParam.state.set(numUAV, READY_TO_START);
					}
				}
			}
		}
		
		/** READY TO START PHASE */
		GUI.log(numUAV, ScanText.READY_TO_START);
		GUI.updateProtocolState(numUAV, ScanText.READY_TO_START);
		if (iAmCenter) {
			GUI.logVerbose(numUAV, ScanText.CENTER_TAKEOFF_END_ACK_LISTENER);
			acks.clear();
			while (ScanParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.TAKEOFF_END_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs - 1) {
							ScanParam.state.set(numUAV, SETUP_FINISHED);
						}
					}
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (ScanParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKEOFF_END) {
						lastReceivedTakeoffEnd = System.currentTimeMillis();
					}
				}
				
				if (System.currentTimeMillis() - lastReceivedTakeoffEnd > ScanParam.TAKEOFF_TIMEOUT) {
					ScanParam.state.set(numUAV, SETUP_FINISHED);
				}
			}
		}
		
		/** SETUP FINISHED PHASE */
		GUI.log(numUAV, ScanText.SETUP_FINISHED);
		GUI.updateProtocolState(numUAV, ScanText.SETUP_FINISHED);
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		while (ScanParam.state.get(numUAV) == SETUP_FINISHED) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			// Coordination with ArduSim
			if (Tools.isExperimentInProgress()) {
				ScanParam.state.set(numUAV, FOLLOWING_MISSION);
			}
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		Map<Long, Long> reached = null;
		if (iAmCenter) {
			reached = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
		}
		int currentWP = 0;
		ScanParam.moveSemaphore.set(numUAV, 1);	// We reach waypoint 0 and start moving towards waypoint 1
		UTMCoordinates centerUAVFinalLocation = null;
		while (ScanParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			GUI.log(numUAV, ScanText.WP_REACHED);
			GUI.updateProtocolState(numUAV, ScanText.WP_REACHED);
			if (iAmCenter) {
				GUI.logVerbose(numUAV, ScanText.CENTER_WP_REACHED_ACK_LISTENER);
				reached.clear();
				
				while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					inBuffer = Copter.receiveMessage(numUAV);
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
										ScanParam.state.set(numUAV, LANDING);
									}
									ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
								}
							}
						}
					}
				}
			} else {
				GUI.logVerbose(numUAV, ScanText.NO_CENTER_WAIT_ORDER_LISTENER);
				while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					inBuffer = Copter.receiveMessage(numUAV);
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();
						
						if (type == Message.MOVE_TO_WAYPOINT){
							int wp = input.readInt();
							if (wp > currentWP) {
								ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
							}
						}
						
						if (type == Message.LAND) {
							centerUAVFinalLocation = new UTMCoordinates(input.readDouble(), input.readDouble());
							ScanParam.state.set(numUAV, MOVE_TO_LAND);
							ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (ScanParam.state.get(numUAV) == FOLLOWING_MISSION) {
				currentWP++;
				GUI.log(numUAV, ScanText.MOVE_TO_WP + " " + currentWP);
				GUI.updateProtocolState(numUAV, ScanText.MOVE_TO_WP + " " + currentWP);
				GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
				
				destinationGeo = missionGeo[currentWP];
				destinationUTM = missionUTM[currentWP];
				relAltitude = missionUTM[currentWP].z;
				if (!Copter.moveUAVNonBlocking(numUAV, destinationGeo, (float)relAltitude)) {
					GUI.exit(ScanText.MOVE_ERROR_1 + " " + selfId);
				}
				cicleTime = System.currentTimeMillis();
				while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
					// Discard message
					Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
					// Wait until target location is reached
					if (System.currentTimeMillis() - cicleTime > ScanParam.MOVE_CHECK_TIMEOUT) {
						if (Copter.getUTMLocation(numUAV).distance(destinationUTM) <= ScanParam.MIN_DISTANCE_TO_WP
								&& Math.abs(relAltitude - Copter.getZRelative(numUAV)) <= 0.2) {
							ScanParam.moveSemaphore.incrementAndGet(numUAV);
						} else {
							cicleTime = cicleTime + ScanParam.MOVE_CHECK_TIMEOUT;
						}
					}
				}
			}
		}
		
		/** MOVE TO LAND PHASE */
		int waitingTime;
		// This only happens for UAVs not located in the center of the formation
		if (ScanParam.state.get(numUAV) == MOVE_TO_LAND) {
			GUI.log(numUAV, ScanText.LAND_LOCATION_REACHED);
			GUI.updateProtocolState(numUAV, ScanText.LAND_LOCATION_REACHED);
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
			FlightFormation flyingFormation =
					FlightFormation.getFormation(ScanParam.flyingFormation.get(numUAV), numUAVs, FlightFormation.getLandingFormationDistance());
			int position = ScanParam.flyingFormationPosition.get(numUAV);
			double formationHeading = ScanParam.flyingFormationHeading.get(numUAV);
			UTMCoordinates landingLocation = flyingFormation.getLocation(position, centerUAVFinalLocation, formationHeading);
			GeoCoordinates target = Tools.UTMToGeo(landingLocation);
			float currentAltitude = (float)Copter.getZRelative(numUAV);
			if (!Copter.moveUAVNonBlocking(numUAV, target, currentAltitude)) {
				GUI.exit(ScanText.MOVE_ERROR_2 + " " + selfId);
			}
			cicleTime = System.currentTimeMillis();
			while (ScanParam.state.get(numUAV) == MOVE_TO_LAND) {
				// Wait until target location is reached
				if (landingLocation.distance(Copter.getUTMLocation(numUAV)) <= ScanParam.MIN_DISTANCE_TO_WP
						&& Math.abs(currentAltitude - Copter.getZRelative(numUAV)) <= 0.2) {
					ScanParam.state.set(numUAV, LANDING);
				}
				if (ScanParam.state.get(numUAV) != LANDING) {
					cicleTime = cicleTime + ScanParam.MOVE_CHECK_TIMEOUT;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		/** LANDING PHASE */
		if (!Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED)) {
			GUI.exit(ScanText.LAND_ERROR + " " + selfId);
		}
		GUI.log(numUAV, ScanText.LANDING_UAV);
		GUI.updateProtocolState(numUAV, ScanText.LANDING_UAV);
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		cicleTime = System.currentTimeMillis();
		while (ScanParam.state.get(numUAV) == LANDING) {
			if(!Copter.isFlying(numUAV)) {
				ScanParam.state.set(numUAV, FINISH);
			} else {
				cicleTime = cicleTime + ScanParam.LAND_CHECK_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		
		/** FINISH PHASE */
		GUI.log(numUAV, ScanText.FINISH);
		GUI.updateProtocolState(numUAV, ScanText.FINISH);
		GUI.logVerbose(numUAV, ScanText.LISTENER_FINISHED);
	}

}