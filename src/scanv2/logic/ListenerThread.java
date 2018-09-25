package scanv2.logic;

import static scanv2.pojo.State.*;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import main.Text;
import scanv2.pojo.Message;
import scanv2.pojo.MovedMission;
import scanv2.pojo.uav2DPosition;
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
		Map<Long, uav2DPosition> UAVsDetected = null;
		GUI.log(numUAV, ScanText.START);
		if (this.isMaster) {
			GUI.logVerbose(numUAV, ScanText.MASTER_START_LISTENER);
			UAVsDetected = new HashMap<Long, uav2DPosition>();	// Detecting UAVs
			while (ScanParam.state.get(numUAV) == START) {
				inBuffer = Copter.receiveMessage(numUAV, ScanParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.HELLO) {
						// Read id (MAC on real UAV) of UAVs and write it into Map if is not duplicated
						Long idSlave = input.readLong();
						uav2DPosition location = new uav2DPosition(input.readDouble(), input.readDouble(), idSlave,
								input.readDouble());
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
		List<WaypointSimplified> screenMission = Tools.getUAVMissionSimplified(numUAV);
		uav2DPosition[] currentLocations = null;
		if (this.isMaster) {
			GUI.log(numUAV, ScanText.SETUP);
			GUI.log(numUAV, ScanText.DETECTED + UAVsDetected.size() + " UAVs");
			
			// 1. Calculate the take-off altitude
			double takeoffAltitude = screenMission.get(0).z;
			double takeOffAltitudeStepOne;
			if (takeoffAltitude <= 5.0) {
				takeOffAltitudeStepOne = 2.0;
			} else if (takeoffAltitude >= 10.0) {
				takeOffAltitudeStepOne = 5.0;
			} else {
				takeOffAltitudeStepOne = takeoffAltitude / 2;
			}
			
			// 2. Make permanent the list of UAVs detected, including master
			// 2.1. Populate the array with slaves
			Iterator<Map.Entry<Long, uav2DPosition>> entries = UAVsDetected.entrySet().iterator();
			Map.Entry<Long, uav2DPosition> entry;
			int i = 0;
			currentLocations = new uav2DPosition[UAVsDetected.size() + 1];	// +1 to include master ID
			while (entries.hasNext()) {
				entry = entries.next();
				currentLocations[i] = entry.getValue();
				i++;
			}
			// 2.2. Insert master in the last position of the array and store
			Point2D.Double UTMMaster = Copter.getUTMLocation(numUAV);
			Double headingMaster = Copter.getHeading(numUAV);
			uav2DPosition masterLocation = new uav2DPosition();
			masterLocation.id = Tools.getIdFromPos(numUAV);
			masterLocation.x = UTMMaster.x;
			masterLocation.y = UTMMaster.y;
			masterLocation.heading = headingMaster;
			currentLocations[currentLocations.length - 1] = masterLocation;
			if (Tools.getArduSimRole() == Tools.MULTICOPTER) {
				ScanParam.masterHeading = Copter.getHeading(numUAV);
			}// In simulation, masterHeading is assigned when SwarmProtHelper.setStartingLocation() is called
			// 3. Calculus of the flying formation
			Triplet<Long, Integer, uav2DPosition[]> result = ScanHelper.getBestFormation(currentLocations, ScanParam.masterHeading);

			ScanParam.masterPosition = result.getValue1();	// Position of master in the reorganized vector
			long centerId = result.getValue0();
			uav2DPosition[] takeoffLocations = result.getValue2();

			GUI.log(numUAV, "Center UAV id: " + centerId + ", master UAV position in the take off locations array: " + ScanParam.masterPosition);

			if (screenMission.size() > ScanParam.MAX_WAYPOINTS) {
				GUI.exit(ScanText.MAX_WP_REACHED);
			}

			// 3. Calculus of the missions to be sent to the slaves and storage of the mission to be followed by the master
			// 3.1. Calculus of the mission to be followed by the UAV in the center of the formation
			Point3D[] centerMission = new Point3D[screenMission.size()];
			//	First, find the center UAV in the formation
			boolean found = false;
			int centerPosition = 0;
			for (int r = 0; r < takeoffLocations.length && !found; r++) {
				if (takeoffLocations[r].id == centerId) {
					centerPosition = r;
					found = true;
				}
			}
			// Then, calculus of the mission of the central drone
			centerMission[0] = new Point3D(takeoffLocations[centerPosition].x, takeoffLocations[centerPosition].y, takeoffAltitude);
			for (i = 1; i < screenMission.size(); i++) {
				WaypointSimplified wp = screenMission.get(i);
				centerMission[i] = new Point3D(wp.x, wp.y, wp.z);
			}
			// 3.2 Copy of the mission of the central drone to the neighbors
			MovedMission[] movedMission = new MovedMission[takeoffLocations.length];
			for (i = 0; i < takeoffLocations.length; i++) {
				Point3D[] locations = new Point3D[centerMission.length];
				if (i == centerPosition) {
					for (int j = 0; j < centerMission.length; j++) {
						locations[j] = centerMission[j];
					}
				} else {
					double incX = takeoffLocations[i].x - takeoffLocations[centerPosition].x;
					double incY = takeoffLocations[i].y - takeoffLocations[centerPosition].y;
					Point3D location, reference;
					for (int j = 0; j < centerMission.length; j++) {
						location = new Point3D();
						reference = centerMission[j];
						location.x = reference.x + incX;
						location.y = reference.y + incY;
						location.z = reference.z;
						locations[j] = location;
					}
				}
				movedMission[i] = new MovedMission(takeoffLocations[i].id, locations);
			}
			// 3.3. Calculus of the mission to be sent to slaves and storage of the mission of the master
			byte[] outBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			Output output = new Output(outBuffer);
			for (i = 0; i < movedMission.length; i++) {
				long prev, next;
				if (i == 0) {
					prev = ScanParam.BROADCAST_MAC_ID;
				} else {
					prev = movedMission[i - 1].id;
				}
				if (i == movedMission.length - 1) {
					next = ScanParam.BROADCAST_MAC_ID;
				} else {
					next = movedMission[i + 1].id;
				}

				Point3D[] currentMission = movedMission[i].posiciones;
				if (i == ScanParam.masterPosition) {
					ScanParam.idPrev.set(numUAV, prev);
					ScanParam.idNext.set(numUAV, next);
					ScanParam.takeoffAltitude.set(numUAV, takeOffAltitudeStepOne);

					Point3D[] missionMaster = new Point3D[currentMission.length];
					for (int j = 0; j < currentMission.length; j++) {
						missionMaster[j] = new Point3D(currentMission[j].x, currentMission[j].y,
								currentMission[j].z);
					}
					ScanParam.uavMissionReceivedUTM.set(numUAV, missionMaster);
					GeoCoordinates[] missionMasterGeo = new GeoCoordinates[missionMaster.length];
					for (int j = 0; j < missionMaster.length; j++) {
						// Save each coordinates point in flightListGeo
						missionMasterGeo[j] = Tools.UTMToGeo(missionMaster[j].x, missionMaster[j].y);
					}
					ScanParam.uavMissionReceivedGeo.set(numUAV, missionMasterGeo);
					// missionSent[masterPosition] remains null
				} else {
					output.clear();
					output.writeShort(Message.DATA);
					output.writeLong(movedMission[i].id);
					output.writeLong(prev);
					output.writeLong(next);
					output.writeDouble(takeOffAltitudeStepOne);
					// Number of points (Necessary for the Talker to know how many to read)
					output.writeInt(centerMission.length);
					// Points to send
					for (int j = 0; j < currentMission.length; j++) {
						output.writeDouble(currentMission[j].x);
						output.writeDouble(currentMission[j].y);
						output.writeDouble(currentMission[j].z);
					}
					output.flush();
					ScanParam.data.set(i, Arrays.copyOf(outBuffer, output.position()));
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
							ScanParam.idPrev.set(numUAV, input.readLong());
							ScanParam.idNext.set(numUAV, input.readLong());
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
				if (ScanParam.idPrev.get(numUAV) == ScanParam.BROADCAST_MAC_ID) {
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
		Map<Long, Long> reached = null;
		if (this.isMaster) {
			reached = new HashMap<>(currentLocations.length);
		}
		int currentWP = 0;
		while (ScanParam.state.get(numUAV) == FOLLOWING_MISSION) {
			/** MOVE_TO_WP PHASE */
			GUI.updateProtocolState(numUAV, ScanText.MOVE_TO_WP);
			GUI.log(numUAV, ScanText.MOVE_TO_WP);
			GUI.logVerbose(numUAV, ScanText.LISTENER_WAITING);
			
			GeoCoordinates geo = missionGeo[currentWP];
			UTMCoordinates utm = Tools.geoToUTM(geo.latitude, geo.longitude);
			Point2D.Double destination = new Point2D.Double(utm.Easting, utm.Northing);
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
			if (this.isMaster) {
				GUI.logVerbose(numUAV, ScanText.MASTER_WP_REACHED_ACK_LISTENER);
			} else {
				GUI.logVerbose(numUAV, ScanText.SLAVE_WAIT_ORDER_LISTENER);
			}
			if (this.isMaster) {
				reached.clear();
			}
			while (ScanParam.wpReachedSemaphore.get(numUAV) == currentWP) {
				inBuffer = Copter.receiveMessage(numUAV);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (this.isMaster && type == Message.WP_REACHED_ACK) {
						long id = input.readLong();
						int wp = input.readInt();
						if (wp == currentWP) {
							reached.put(id, id);
						}
						if (reached.size() == currentLocations.length - 1) {
							if (currentWP == mission.length - 1) {
								ScanParam.state.set(numUAV, LANDING);
							}
							ScanParam.wpReachedSemaphore.incrementAndGet(numUAV);
						}
					}
					if (!this.isMaster) {
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