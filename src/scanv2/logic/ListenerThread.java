package scanv2.logic;

import static scanv2.pojo.State.FINISH;
import static scanv2.pojo.State.FOLLOWING_MISSION;
import static scanv2.pojo.State.LANDING;
import static scanv2.pojo.State.SETUP;
import static scanv2.pojo.State.SETUP_FINISHED;
import static scanv2.pojo.State.START;
import static scanv2.pojo.State.TAKING_OFF;
import static scanv2.pojo.State.WAIT_TAKE_OFF;

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
import main.Text;
import scanv2.pojo.Message;
import scanv2.pojo.MovedMission;
import sim.logic.SimParam;
import uavController.UAVParam;

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
					}
				}
				if (Tools.isSetupInProgress()) {
					GUI.updateProtocolState(numUAV, ScanText.SETUP);
					ScanParam.state.set(numUAV, SETUP);
				}
			}
		} else {
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
			while (ScanParam.state.get(numUAV) == START) {
				// Discard message
				Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				
				if (Tools.isSetupInProgress()) {
					GUI.updateProtocolState(numUAV, ScanText.SETUP);
					ScanParam.state.set(numUAV, SETUP);
				}
			}
		}
		
		/** SETUP PHASE */
		if (this.isMaster) {
			GUI.log(numUAV, ScanText.SETUP);
			GUI.log(numUAV, ScanText.DETECTED + UAVsDetected.size() + " UAVs");
			
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
			FlightFormation airFormation = FlightFormation.getFormation(UAVParam.airFormation.get(), numUAVs, UAVParam.airDistanceBetweenUAV);
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
			Map<Integer, MovedMission> movedMission = new HashMap<Integer, MovedMission>(numUAVs);
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
						ScanParam.amICenter[numUAV].set(true);
					}
					ScanParam.idPrev.set(numUAV, prevId);
					ScanParam.idNext.set(numUAV, nextId);
					ScanParam.numUAVs.set(numUAV, numUAVs);
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
					ScanParam.data.set(formationPosition, Arrays.copyOf(outBuffer, output.position()));
				}
			}
			try {
				output.close();
			} catch (KryoException e) {}
			
			
			// 4. Wait for data ack form all the slaves
			GUI.logVerbose(numUAV, ScanText.MASTER_DATA_ACK_LISTENER);
			Map<Long, Long> acks = new HashMap<>(currentLocations.length);
			while (ScanParam.state.get(numUAV) == SETUP) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
					}
					
					if (acks.size() == currentLocations.length - 1) {
						GUI.updateProtocolState(numUAV, ScanText.SETUP_FINISHED);
						ScanParam.state.set(numUAV, SETUP_FINISHED);
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
					if (type == Message.DATA) {
						long id = input.readLong();
						if (id == selfId) {
							if (this.selfId == input.readLong()) {
								ScanParam.amICenter[numUAV].set(true);
							}
							ScanParam.idPrev.set(numUAV, input.readLong());
							ScanParam.idNext.set(numUAV, input.readLong());
							ScanParam.numUAVs.set(numUAV, input.readInt());
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

							GUI.updateProtocolState(numUAV, ScanText.SETUP_FINISHED);
							ScanParam.state.set(numUAV, SETUP_FINISHED);
						}
					}
				}
			}
		}
		
		/** SETUP FINISHED PHASE */
		GUI.log(numUAV, ScanText.SETUP_FINISHED);
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		while (ScanParam.state.get(numUAV) == SETUP_FINISHED) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			if (Tools.isExperimentInProgress()) {
				if (ScanParam.idPrev.get(numUAV) == FlightFormation.BROADCAST_MAC_ID) {
					GUI.updateProtocolState(numUAV, ScanText.TAKING_OFF);
					ScanParam.state.set(numUAV, TAKING_OFF);
				} else {
					GUI.updateProtocolState(numUAV, ScanText.WAIT_TAKE_OFF);
					ScanParam.state.set(numUAV, WAIT_TAKE_OFF);
				}
			}
		}
		
		/** WAIT TAKE OFF PHASE */
		if (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
			GUI.log(numUAV, ScanText.WAIT_TAKE_OFF);
			GUI.logVerbose(numUAV, ScanText.WAITING_TAKE_OFF);
			while (ScanParam.state.get(numUAV) == WAIT_TAKE_OFF) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					if (type == Message.TAKE_OFF_NOW) {
						long id = input.readLong();
						if (id == selfId) {
							GUI.updateProtocolState(numUAV, ScanText.TAKING_OFF);
							ScanParam.state.set(numUAV, TAKING_OFF);
						}
					}
				}
			}
		}
		
		/** TAKING OFF PHASE */
		GUI.log(numUAV, ScanText.TAKING_OFF);
		double altitude = ScanParam.takeoffAltitude.get(numUAV);
		double thresholdAltitude = altitude * 0.95;
		if (!Copter.takeOffNonBlocking(numUAV, altitude)) {
			GUI.exit(ScanText.TAKE_OFF_ERROR + " " + selfId);
		}
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		int waitingTime;
		while (ScanParam.state.get(numUAV) == TAKING_OFF) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			// Wait until target altitude is reached
			if (System.currentTimeMillis() - cicleTime > ScanParam.TAKE_OFF_CHECK_TIMEOUT) {
				GUI.logVerbose(SimParam.prefix[numUAV] + Text.ALTITUDE_TEXT
						+ " = " + String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ())
						+ " " + Text.METERS);
				if (UAVParam.uavCurrentData[numUAV].getZRelative() >= thresholdAltitude) {
					ScanParam.state.set(numUAV, FOLLOWING_MISSION);
				} else {
					cicleTime = cicleTime + ScanParam.TAKE_OFF_CHECK_TIMEOUT;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		Point3D[] mission = ScanParam.uavMissionReceivedUTM.get(numUAV);
		GeoCoordinates[] missionGeo = ScanParam.uavMissionReceivedGeo.get(numUAV);
		int numUAVs = ScanParam.numUAVs.get(numUAV);
		boolean amICenter = ScanParam.amICenter[numUAV].get();
		Map<Long, Long> reached = null;
		if (amICenter) {
			reached = new HashMap<>(numUAVs);
		}
		int currentWP = 0;
		while (ScanParam.state.get(numUAV) == FOLLOWING_MISSION) {
			/** MOVE_TO_WP PHASE */
			GUI.updateProtocolState(numUAV, ScanText.MOVE_TO_WP + " " + currentWP);
			GUI.log(numUAV, ScanText.MOVE_TO_WP + " " + currentWP);
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
			
			GeoCoordinates geo = missionGeo[currentWP];
			UTMCoordinates utm = Tools.geoToUTM(geo.latitude, geo.longitude);
			Point2D.Double destination = new Point2D.Double(utm.x, utm.y);
			double relAltitude = mission[currentWP].z;
			if (!Copter.moveUAVNonBlocking(numUAV, geo, (float)relAltitude)) {
				GUI.exit(ScanText.MOVE_ERROR + " " + selfId);
			}
			cicleTime = System.currentTimeMillis();
			while (ScanParam.moveSemaphore.get(numUAV) == currentWP) {
				// Discard message
				Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				// Wait until target location is reached
				if (System.currentTimeMillis() - cicleTime > ScanParam.MOVE_CHECK_TIMEOUT) {
					if (UAVParam.uavCurrentData[numUAV].getUTMLocation().distance(destination) <= ScanParam.MIN_DISTANCE_TO_WP
							&& Math.abs(relAltitude - UAVParam.uavCurrentData[numUAV].getZRelative()) <= 0.2) {
						ScanParam.moveSemaphore.incrementAndGet(numUAV);
					} else {
						cicleTime = cicleTime + ScanParam.MOVE_CHECK_TIMEOUT;
						waitingTime = (int)(cicleTime - System.currentTimeMillis());
						if (waitingTime > 0) {
							Tools.waiting(waitingTime);
						}
					}
				}
			}
			
			/** WP_REACHED PHASE */
			GUI.updateProtocolState(numUAV, ScanText.WP_REACHED);
			GUI.log(numUAV, ScanText.WP_REACHED);
			if (amICenter) {
				GUI.logVerbose(numUAV, ScanText.CENTER_WP_REACHED_ACK_LISTENER);
				reached.clear();
			} else {
				GUI.logVerbose(numUAV, ScanText.SLAVE_WAIT_ORDER_LISTENER);
			}
			while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (amICenter && type == Message.WP_REACHED_ACK) {
						long id = input.readLong();
						int wp = input.readInt();
						if (wp == currentWP) {
							reached.put(id, id);
						}
						if (reached.size() == numUAVs - 1) {
							if (currentWP == mission.length - 1) {
								ScanParam.state.set(numUAV, LANDING);
							}
							ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
					if (!amICenter) {
						if (type == Message.MOVE_NOW){
							int wp = input.readInt();
							if (wp > currentWP) {
								ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
							}
						}
						if (type == Message.LAND_NOW) {
							ScanParam.state.set(numUAV, LANDING);
							ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
				}
			}
			currentWP++;
		}
		
		/** LANDING PHASE */
		GUI.updateProtocolState(numUAV, ScanText.LANDING_UAV);
		if (!Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED)) {
			GUI.exit(ScanText.LAND_ERROR + " " + selfId);
		}
		GUI.log(numUAV, ScanText.LANDING);
		GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
		cicleTime = System.currentTimeMillis();
		while (ScanParam.state.get(numUAV) == LANDING) {
			// Discard message
			Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
			
			if (System.currentTimeMillis() - cicleTime > ScanParam.LAND_CHECK_TIMEOUT) {
				if(!Copter.isFlying(numUAV)) {
					ScanParam.state.set(numUAV, FINISH);
				} else {
					cicleTime = cicleTime + ScanParam.LAND_CHECK_TIMEOUT;
					waitingTime = (int)(cicleTime - System.currentTimeMillis());
					if (waitingTime > 0) {
						Tools.waiting(waitingTime);
					}
				}
			}
		}
		
		/** FINISH PHASE */
		GUI.updateProtocolState(numUAV, ScanText.FINISH);
		GUI.log(numUAV, ScanText.FINISH);
		GUI.logVerbose(numUAV, ScanText.LISTENER_FINISHED);
	}

}