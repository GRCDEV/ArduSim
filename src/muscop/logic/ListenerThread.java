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

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.Point3D;
import api.pojo.UTMCoordinates;
import api.pojo.WaypointSimplified;
import api.pojo.formations.FlightFormation;
import api.pojo.formations.FlightFormation.Formation;
import main.Text;
import muscop.pojo.Message;
import muscop.pojo.MovedMission;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

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
		this.isMaster = MUSCOPHelper.isMaster(numUAV);
		
		this.inBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
	}

	@Override
	public void run() {
		
		while (!Tools.areUAVsAvailable()) {
			Tools.waiting(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		Map<Long, UTMCoordinates> UAVsDetected = null;
		GUI.log(numUAV, MUSCOPText.START);
		if (this.isMaster) {
			GUI.logVerbose(numUAV, MUSCOPText.MASTER_START_LISTENER);
			UAVsDetected = new HashMap<Long, UTMCoordinates>();	// Detecting UAVs
			int numUAVsDetected = 0;
			while (MUSCOPParam.state.get(numUAV) == START) {
				inBuffer = Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.HELLO) {
						// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
						Long idSlave = input.readLong();
						UTMCoordinates location = new UTMCoordinates(input.readDouble(), input.readDouble());
						// ADD to Map the new UAV detected
						UAVsDetected.put(idSlave, location);
						if (UAVsDetected.size() > numUAVsDetected) {
							numUAVsDetected = UAVsDetected.size();
							GUI.log(MUSCOPText.MASTER_DETECTED_UAVS + numUAVsDetected);
						}
					}
				}
				// Coordination with ArduSim
				if (Tools.isSetupInProgress() || Tools.isSetupFinished()) {// The setup could be too fast
					MUSCOPParam.state.set(numUAV, SETUP);
				}
			}
		} else {
			GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
			while (MUSCOPParam.state.get(numUAV) == START) {
				// Discard message
				Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
				// Coordination with ArduSim
				if (Tools.isSetupInProgress() || Tools.isSetupFinished()) {
					MUSCOPParam.state.set(numUAV, SETUP);
				}
			}
		}
		
		/** SETUP PHASE */
		GUI.log(numUAV, MUSCOPText.SETUP);
		GUI.updateProtocolState(numUAV, MUSCOPText.SETUP);
		long lastReceivedReadyToFly = 0;	// Used in slaves to detect the end of this phase
		Map<Long, Long> acks = null;
		if (this.isMaster) {
			// 1. Make a permanent list of UAVs detected, including master
			// 1.1. Add master to the Map
			UTMCoordinates masterLocation = Copter.getUTMLocation(numUAV);
			UAVsDetected.put(this.selfId, new UTMCoordinates(masterLocation.x, masterLocation.y));
			// 1.2. Set the number of UAVs running
			int numUAVs = UAVsDetected.size();
			
			// 2. Set the heading used to orient the formation
			if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
				// We use the master heading
				MUSCOPParam.formationHeading = Copter.getHeading(numUAVs);
			}// In simulation, centerHeading is assigned when MUSCOPHelper.setStartingLocation() is called,
			 //   using the first mission segment to orient the formation
			
			// 3. Calculus of the UAVs distribution that better fit the current layout on the ground
			Formation flyingFormation = FlightFormation.getFlyingFormation();
			FlightFormation airFormation = FlightFormation.getFormation(flyingFormation, numUAVs, FlightFormation.getFlyingFormationDistance());
			Triplet<Integer, Long, UTMCoordinates>[] match =
					FlightFormation.matchIDs(UAVsDetected, MUSCOPParam.formationHeading, true, null, airFormation);
			
			// 4. Get the target coordinates for the central UAV
			int centerUAVAirPos = airFormation.getCenterUAVPosition();	// Position in the formation
			Triplet<Integer, Long, UTMCoordinates> centerUAVAir = null;
			for (int i = 0; i < numUAVs; i++) {
				if (match[i].getValue0() == centerUAVAirPos) {
					centerUAVAir = match[i];
				}
			}
			if (centerUAVAir == null) {
				GUI.exit(MUSCOPText.CENTER_ID_NOT_FOUND);
			}
			long centerUAVId = centerUAVAir.getValue1();
			
			// 5. Load the mission
			List<WaypointSimplified> screenMission = Tools.getUAVMissionSimplified(numUAV);
			if (screenMission == null || screenMission.size() > MUSCOPParam.MAX_WAYPOINTS) {
				GUI.exit(MUSCOPText.MAX_WP_REACHED);
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
					UTMCoordinates offset = airFormation.getOffset(formationPosition, MUSCOPParam.formationHeading);
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
			Pair<Integer, Long>[] sequence = FlightFormation.getTakeoffSequence(UAVsDetected, match);
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
				points = currentMission.locations;
				formationPosition = currentMission.formationPosition;
				id = currentMission.id;
				if (this.selfId == id) {
					if (this.selfId == centerUAVId) {
						MUSCOPParam.iAmCenter[numUAV].set(true);
					}
					MUSCOPParam.idPrev.set(numUAV, prevId);
					MUSCOPParam.idNext.set(numUAV, nextId);
					MUSCOPParam.numUAVs.set(numUAV, numUAVs);
					MUSCOPParam.flyingFormation.set(numUAV, flyingFormation);
					MUSCOPParam.flyingFormationPosition.set(numUAV, formationPosition);
					MUSCOPParam.flyingFormationHeading.set(numUAV, MUSCOPParam.formationHeading);
					MUSCOPParam.takeoffAltitude.set(numUAV, takeOffAltitudeStepOne);
					MUSCOPParam.uavMissionReceivedUTM.set(numUAV, points);
					GeoCoordinates[] missionMasterGeo = new GeoCoordinates[points.length];
					for (int j = 0; j < points.length; j++) {
						missionMasterGeo[j] = Tools.UTMToGeo(points[j].x, points[j].y);
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
			GUI.logVerbose(numUAV, MUSCOPText.MASTER_DATA_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				inBuffer = Copter.receiveMessage(numUAV);
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
			GUI.logVerbose(numUAV, MUSCOPText.SLAVE_WAIT_DATA_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == SETUP) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.DATA && MUSCOPParam.uavMissionReceivedGeo.get(numUAV) == null) {
						long id = input.readLong();
						if (id == selfId) {
							if (this.selfId == input.readLong()) {
								MUSCOPParam.iAmCenter[numUAV].set(true);
							}
							MUSCOPParam.idPrev.set(numUAV, input.readLong());
							MUSCOPParam.idNext.set(numUAV, input.readLong());
							MUSCOPParam.numUAVs.set(numUAV, input.readInt());
							MUSCOPParam.flyingFormation.set(numUAV, Formation.getFormation(input.readShort()));
							MUSCOPParam.flyingFormationPosition.set(numUAV, input.readInt());
							MUSCOPParam.flyingFormationHeading.set(numUAV, input.readDouble());
							MUSCOPParam.takeoffAltitude.set(numUAV, input.readDouble());

							// The number of WP is read to know the loop length
							int size = input.readInt();
							Point3D[] mission = new Point3D[size];
							GeoCoordinates[] missionGeo = new GeoCoordinates[size];

							for (int i = 0; i < mission.length; i++) {
								mission[i] = new Point3D(input.readDouble(),
										input.readDouble(), input.readDouble());
								missionGeo[i] = Tools.UTMToGeo(mission[i].x, mission[i].y);
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
		GUI.log(numUAV, MUSCOPText.READY_TO_FLY);
		GUI.updateProtocolState(numUAV, MUSCOPText.READY_TO_FLY);
		int numUAVs = MUSCOPParam.numUAVs.get(numUAV);
		
		if (this.isMaster) {
			GUI.logVerbose(numUAV, MUSCOPText.MASTER_READY_TO_FLY_ACK_LISTENER);
			acks.clear();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = Copter.receiveMessage(numUAV);
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
			GUI.logVerbose(numUAV, MUSCOPText.SLAVE_WAIT_READY_TO_FLY_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == READY_TO_FLY) {
				inBuffer = Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
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
			GUI.log(numUAV, MUSCOPText.WAIT_TAKE_OFF);
			GUI.updateProtocolState(numUAV, MUSCOPText.WAIT_TAKE_OFF);
			GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING_TAKE_OFF);
			while (MUSCOPParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.TAKE_OFF_NOW) {
						long id = input.readLong();
						if (id == selfId) {
							MUSCOPParam.state.set(numUAV, TAKING_OFF);
						}
					}
				}
			}
		}
		
		/** TAKING OFF PHASE */
		GUI.log(numUAV, MUSCOPText.TAKING_OFF);
		GUI.updateProtocolState(numUAV, MUSCOPText.TAKING_OFF);
		double altitude = MUSCOPParam.takeoffAltitude.get(numUAV);
		double minAltitude = MUSCOPHelper.getMinAltitude(altitude);
		if (!Copter.takeOffNonBlocking(numUAV, altitude)) {
			GUI.exit(MUSCOPText.TAKE_OFF_ERROR + " " + selfId);
		}
		GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		long logTime = cicleTime;
		while (MUSCOPParam.state.get(numUAV) == TAKING_OFF) {
			// Discard message
			Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
			// Wait until target altitude is reached
			if (System.currentTimeMillis() - cicleTime > MUSCOPParam.TAKE_OFF_CHECK_TIMEOUT) {
				if (Copter.getZRelative(numUAV) >= minAltitude) {
					MUSCOPParam.state.set(numUAV, MOVE_TO_TARGET);
				} else {
					if (System.currentTimeMillis() - logTime > MUSCOPParam.TAKE_OFF_LOG_TIMEOUT) {
						GUI.logVerbose(numUAV, Text.ALTITUDE_TEXT
								+ " = " + String.format("%.2f", Copter.getZ(numUAV))
								+ " " + Text.METERS);
						logTime = logTime + MUSCOPParam.TAKE_OFF_LOG_TIMEOUT;
					}
					
					cicleTime = cicleTime + MUSCOPParam.TAKE_OFF_CHECK_TIMEOUT;
				}
			}
		}
		
		/** MOVE TO TARGET PHASE */
		GUI.log(numUAV, MUSCOPText.MOVE_TO_WP + " 0");
		GUI.updateProtocolState(numUAV, MUSCOPText.MOVE_TO_WP + " 0");
		GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
		Point3D[] missionUTM = MUSCOPParam.uavMissionReceivedUTM.get(numUAV);
		GeoCoordinates[] missionGeo = MUSCOPParam.uavMissionReceivedGeo.get(numUAV);
		GeoCoordinates destinationGeo = missionGeo[0];
		Point3D destinationUTM = missionUTM[0];
		double relAltitude = missionUTM[0].z;
		double min = MUSCOPHelper.getMinAltitude(relAltitude);
		double max = MUSCOPHelper.getMaxAltitude(relAltitude);
		if (!Copter.moveUAVNonBlocking(numUAV, destinationGeo, (float)relAltitude)) {
			GUI.exit(MUSCOPText.MOVE_ERROR_2 + " " + selfId);
		}
		cicleTime = System.currentTimeMillis();
		double alt;
		while (MUSCOPParam.state.get(numUAV) == MOVE_TO_TARGET) {
			// Discard message
			Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
			// Wait until target location is reached
			if (System.currentTimeMillis() - cicleTime > MUSCOPParam.MOVE_CHECK_TIMEOUT) {
				if (Copter.getUTMLocation(numUAV).distance(destinationUTM) <= MUSCOPParam.MIN_DISTANCE_TO_WP) {
					alt = Copter.getZRelative(numUAV);
					if (alt >= min && alt <= max) {
						MUSCOPParam.state.set(numUAV, TARGET_REACHED);
					}
				} else {
					cicleTime = cicleTime + MUSCOPParam.MOVE_CHECK_TIMEOUT;
				}
			}
		}
		
		/** TARGET REACHED PHASE */
		GUI.log(numUAV, MUSCOPText.TARGET_REACHED);
		GUI.updateProtocolState(numUAV, MUSCOPText.TARGET_REACHED);
		boolean iAmCenter = MUSCOPParam.iAmCenter[numUAV].get();
		long lastReceivedTakeoffEnd = 0;
		if (iAmCenter) {
			GUI.logVerbose(numUAV, MUSCOPText.CENTER_TARGET_REACHED_ACK_LISTENER);
			acks = new HashMap<Long, Long>((int)Math.ceil(numUAVs / 0.75) + 1);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = Copter.receiveMessage(numUAV);
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
			GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == TARGET_REACHED) {
				inBuffer = Copter.receiveMessage(numUAV);
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
		GUI.log(numUAV, MUSCOPText.READY_TO_START);
		GUI.updateProtocolState(numUAV, MUSCOPText.READY_TO_START);
		if (iAmCenter) {
			GUI.logVerbose(numUAV, MUSCOPText.CENTER_TAKEOFF_END_ACK_LISTENER);
			acks.clear();
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = Copter.receiveMessage(numUAV);
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
			GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_WAIT_TAKEOFF_END_LISTENER);
			while (MUSCOPParam.state.get(numUAV) == READY_TO_START) {
				inBuffer = Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
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
		GUI.log(numUAV, MUSCOPText.SETUP_FINISHED);
		GUI.updateProtocolState(numUAV, MUSCOPText.SETUP_FINISHED);
		GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
		while (MUSCOPParam.state.get(numUAV) == SETUP_FINISHED) {
			// Discard message
			Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
			// Coordination with ArduSim
			if (Tools.isExperimentInProgress()) {
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
		UTMCoordinates centerUAVFinalLocation = null;
		while (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			GUI.log(numUAV, MUSCOPText.WP_REACHED);
			GUI.updateProtocolState(numUAV, MUSCOPText.WP_REACHED);
			if (iAmCenter) {
				GUI.logVerbose(numUAV, MUSCOPText.CENTER_WP_REACHED_ACK_LISTENER);
				reached.clear();
				
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
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
										MUSCOPParam.state.set(numUAV, LANDING);
									}
									MUSCOPParam.wpReachedSemaphore.incrementAndGet(numUAV);
								}
							}
						}
					}
				}
			} else {
				GUI.logVerbose(numUAV, MUSCOPText.NO_CENTER_WAIT_ORDER_LISTENER);
				while (MUSCOPParam.wpReachedSemaphore.get(numUAV) == currentWP) {
					inBuffer = Copter.receiveMessage(numUAV);
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
							centerUAVFinalLocation = new UTMCoordinates(input.readDouble(), input.readDouble());
							MUSCOPParam.state.set(numUAV, MOVE_TO_LAND);
							MUSCOPParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (MUSCOPParam.state.get(numUAV) == FOLLOWING_MISSION) {
				currentWP++;
				GUI.log(numUAV, MUSCOPText.MOVE_TO_WP + " " + currentWP);
				GUI.updateProtocolState(numUAV, MUSCOPText.MOVE_TO_WP + " " + currentWP);
				GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
				
				destinationGeo = missionGeo[currentWP];
				destinationUTM = missionUTM[currentWP];
				relAltitude = missionUTM[currentWP].z;
				min = MUSCOPHelper.getMinAltitude(relAltitude);
				max = MUSCOPHelper.getMaxAltitude(relAltitude);
				if (!Copter.moveUAVNonBlocking(numUAV, destinationGeo, (float)relAltitude)) {
					GUI.exit(MUSCOPText.MOVE_ERROR_1 + " " + selfId);
				}
				cicleTime = System.currentTimeMillis();
				while (MUSCOPParam.moveSemaphore.get(numUAV) == currentWP) {
					// Discard message
					Copter.receiveMessage(numUAV, MUSCOPParam.RECEIVING_TIMEOUT);
					// Wait until target location is reached
					if (System.currentTimeMillis() - cicleTime > MUSCOPParam.MOVE_CHECK_TIMEOUT) {
						if (Copter.getUTMLocation(numUAV).distance(destinationUTM) <= MUSCOPParam.MIN_DISTANCE_TO_WP) {
							alt = Copter.getZRelative(numUAV);
							if (alt >= min && alt <= max) {
								MUSCOPParam.moveSemaphore.incrementAndGet(numUAV);
							}
						} else {
							cicleTime = cicleTime + MUSCOPParam.MOVE_CHECK_TIMEOUT;
						}
					}
				}
			}
		}
		
		/** MOVE TO LAND PHASE */
		int waitingTime;
		// This only happens for UAVs not located in the center of the formation
		if (MUSCOPParam.state.get(numUAV) == MOVE_TO_LAND) {
			GUI.log(numUAV, MUSCOPText.LAND_LOCATION_REACHED);
			GUI.updateProtocolState(numUAV, MUSCOPText.LAND_LOCATION_REACHED);
			GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
			FlightFormation flyingFormation =
					FlightFormation.getFormation(MUSCOPParam.flyingFormation.get(numUAV), numUAVs, FlightFormation.getLandingFormationDistance());
			int position = MUSCOPParam.flyingFormationPosition.get(numUAV);
			double formationHeading = MUSCOPParam.flyingFormationHeading.get(numUAV);
			UTMCoordinates landingLocation = flyingFormation.getLocation(position, centerUAVFinalLocation, formationHeading);
			GeoCoordinates target = Tools.UTMToGeo(landingLocation);
			relAltitude = (float)Copter.getZRelative(numUAV);
			min = MUSCOPHelper.getMinAltitude(relAltitude);
			max = MUSCOPHelper.getMaxAltitude(relAltitude);
			if (!Copter.moveUAVNonBlocking(numUAV, target, (float)relAltitude)) {
				GUI.exit(MUSCOPText.MOVE_ERROR_2 + " " + selfId);
			}
			cicleTime = System.currentTimeMillis();
			while (MUSCOPParam.state.get(numUAV) == MOVE_TO_LAND) {
				// Wait until target location is reached
				if (landingLocation.distance(Copter.getUTMLocation(numUAV)) <= MUSCOPParam.MIN_DISTANCE_TO_WP) {
					alt = Copter.getZRelative(numUAV);
					if (alt >= min && alt <= max) {
						MUSCOPParam.state.set(numUAV, LANDING);
					}
				}
				if (MUSCOPParam.state.get(numUAV) != LANDING) {
					cicleTime = cicleTime + MUSCOPParam.MOVE_CHECK_TIMEOUT;
					waitingTime = (int) (cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		/** LANDING PHASE */
		if (!Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED)) {
			GUI.exit(MUSCOPText.LAND_ERROR + " " + selfId);
		}
		GUI.log(numUAV, MUSCOPText.LANDING_UAV);
		GUI.updateProtocolState(numUAV, MUSCOPText.LANDING_UAV);
		GUI.logVerbose(numUAV, MUSCOPText.LISTENER_WAITING);
		cicleTime = System.currentTimeMillis();
		while (MUSCOPParam.state.get(numUAV) == LANDING) {
			if(!Copter.isFlying(numUAV)) {
				MUSCOPParam.state.set(numUAV, FINISH);
			} else {
				cicleTime = cicleTime + MUSCOPParam.LAND_CHECK_TIMEOUT;
				waitingTime = (int) (cicleTime - System.currentTimeMillis());
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
		
		/** FINISH PHASE */
		GUI.log(numUAV, MUSCOPText.FINISH);
		GUI.updateProtocolState(numUAV, MUSCOPText.FINISH);
		GUI.logVerbose(numUAV, MUSCOPText.LISTENER_FINISHED);
	}

}