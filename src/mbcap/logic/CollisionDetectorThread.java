package mbcap.logic;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.Point3D;
import main.Param;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.pojo.Beacon;

/** This class implements the collision risk check finite state machine. */

public class CollisionDetectorThread extends Thread {

	private int numUAV; // UAV identifier in the simulator, beginning from 0
	private long cicleTime;
	private PriorityQueue<Beacon> sortingQueue;

	@SuppressWarnings("unused")
	private CollisionDetectorThread() {
	}

	public CollisionDetectorThread(int numUAV) {
		this.numUAV = numUAV;
		this.cicleTime = 0;
		this.sortingQueue = new PriorityQueue<Beacon>(Tools.getNumUAVs(), Collections.reverseOrder());
	}

	@Override
	public void run() {
		Beacon selfBeacon;					// Beacon of this UAV
		Beacon avoidingBeacon = null;		// Last beacon received from the UAV with which this UAV has collision risk
											// In the simulator Id==numUAV (the UAV position in arrays is Id)
		Beacon auxBeacon = null;			// Beacon for auxiliary operations
		boolean hasBeenOvertaken = false;	// Whether this UAV has been finally overtaken by the other UAV
		long stateTime = System.nanoTime();	// Time elapsed since the current protocol state was set
											// In the beginning, the protocol state is always "Normal"

		// Do nothing until the experiment begins
		while (!Tools.isExperimentInProgress()) {
			Tools.waiting(MBCAPParam.SHORT_WAITING_TIME);
		}

		int waitingTime;
		boolean isRealUAV = Tools.getArduSimRole() == Tools.MULTICOPTER;
		// If two UAVs collide, then the protocol stops. Also, it stops when the experiment finishes
		while (!Tools.isCollisionDetected() && Tools.isExperimentInProgress()) {
			// Analyze received information while flying
			if (Copter.isFlying(numUAV)) {
				// 1. Periodic check if the UAV is stuck too many time in a protocol state. Cases:
				//   a) The other UAV has gone, so the current UAV can continue the mission
				//   b) The UAV was overtaking. As it has priority, we assume that it can directly change the state (it is already moving)
				//   c) Deadlock. The UAVs have to be landed
				MBCAPState state = MBCAPParam.state[numUAV];
				if (state != MBCAPState.NORMAL
						&& state != MBCAPState.EMERGENCY_LAND
						&& System.nanoTime() - stateTime > MBCAPParam.uavDeadlockTimeout[numUAV]) {
					GUI.log(numUAV, MBCAPText.PROT_TIMED);
					// Case a. The UAV can resume the mission
					if (avoidingBeacon == null
							&& state != MBCAPState.OVERTAKING) {
						if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
							stateTime = System.nanoTime();
							GUI.log(numUAV, MBCAPText.MISSION_RESUME);
							
							// Clean all risk marks as we don't know which UAV has failed
							if (!isRealUAV) {
								MBCAPGUITools.removeImpactRiskMarks();
							}
							
							// Status update
							MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
							MBCAPParam.event.incrementAndGet(numUAV);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							GUI.log(numUAV, MBCAPText.MISSION_RESUME_ERROR);
						}
					} else if (state == MBCAPState.OVERTAKING) {
						// Case b. No need for commands for the UAV, just change the state
						if (avoidingBeacon!=null) {
							if (!isRealUAV) {
								MBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
								MBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
							}
							avoidingBeacon = null;
							MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
						}
						stateTime = System.nanoTime();
						MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
						MBCAPParam.event.incrementAndGet(numUAV);
						// Progress update
						MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
					} else {
						// Case c. Deadlock. Landing the UAV
						if (Copter.setFlightMode(numUAV, FlightMode.LAND)) {
							GUI.warn(numUAV, MBCAPText.PROT_ERROR, MBCAPText.DEADLOCK);
						} else {
							GUI.log(numUAV, MBCAPText.DEADLOCK_ERROR);
						}
						avoidingBeacon = null;
						stateTime = System.nanoTime();
						MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
						MBCAPParam.event.incrementAndGet(numUAV);
						// Progress update
						MBCAPGUITools.updateState(numUAV, MBCAPState.EMERGENCY_LAND);
					}
				}

				selfBeacon = MBCAPParam.selfBeacon.get(numUAV);
				// Only start to check the collision risk if this UAV has started to send beacons
				if (selfBeacon != null) {
					// 2. Update the collision risk status information
					// 2.1. In normal flight mode, decide if there is risk of collision with other UAV
					if (MBCAPParam.state[numUAV] == MBCAPState.NORMAL) {
						// Sorting the beacons by priority
						sortingQueue.clear();
						Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry<Long, Beacon> entry = entries.next();
							// Ignoring obsolete beacons
							if (System.nanoTime() - entry.getValue().time < MBCAPParam.beaconExpirationTime) {
								sortingQueue.add(entry.getValue());
							}
						}

						// Selecting and ordering the UAVs that suppose a collision risk
						PriorityQueue<Beacon> riskyUAVs = new PriorityQueue<Beacon>(Tools.getNumUAVs(), Collections.reverseOrder());
						while (sortingQueue.size()>0) {
							auxBeacon = sortingQueue.poll();
							if (auxBeacon != null && MBCAPv3Helper.hasCollisionRisk(numUAV, selfBeacon, auxBeacon)) {
								riskyUAVs.add(auxBeacon);
							}
						}

						// If no risk has been detected, another UAV could detect this UAV as a risk
						if (riskyUAVs.isEmpty()) {
							// Timeout needed if a collision risk situation has just been solved
							if (System.nanoTime()-stateTime>MBCAPParam.solvedTimeout) {
								entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext()) {
									Map.Entry<Long, Beacon> entry = entries.next();
									if (avoidingBeacon == null
											&& entry.getValue().idAvoiding == selfBeacon.uavId) {
										avoidingBeacon = entry.getValue();
										MBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
									}
								}
							}
						} else {
							// Risk detected. Using the beacon with higher priority.
							Beacon first = riskyUAVs.poll();
							// If there is risk with a UAV with more priority than current, it is also changed
							if (avoidingBeacon==null
									|| avoidingBeacon.uavId<first.uavId) {
								avoidingBeacon = first;
								MBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
							}
						}

					} else if (MBCAPParam.state[numUAV] != MBCAPState.EMERGENCY_LAND) {
						// 2.2. If the protocol is being applied, a collision risk is already being analyzed

						// Special case, when the UAV stands still
						if (MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL) {

							// While waiting the timeout for protocol stability we check if other UAV with higher priority appears
							if (System.nanoTime()-stateTime<MBCAPParam.standStillTimeout) {
								boolean newRiskDetected = false;
								Beacon update = null;
								Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext() && !newRiskDetected) {
									Map.Entry<Long, Beacon> entry = entries.next();
									auxBeacon = entry.getValue();
									// a. Change to other UAV with greater priority
									if (MBCAPState.getSatateById(auxBeacon.state) == MBCAPState.STAND_STILL && (
											(avoidingBeacon!=null
											&& auxBeacon.uavId>avoidingBeacon.uavId
											&& auxBeacon.idAvoiding==selfBeacon.uavId) ||
											(avoidingBeacon==null
											&& auxBeacon.idAvoiding==selfBeacon.uavId))) {
										avoidingBeacon = auxBeacon;
										MBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
										newRiskDetected = true;
									}
									// b. If no other UAV with higher priority is detected, update avoiding UAV information
									if (!newRiskDetected && avoidingBeacon!=null
											&& auxBeacon.equals(avoidingBeacon)) {
										update = auxBeacon;
									}
								}
								if (!newRiskDetected && update!=null) {
									avoidingBeacon = update;
								}
							} else if (avoidingBeacon!=null) {
								// Stand still timeout exceeded, so we look for an update of the avoiding UAV
								boolean updateLocated = false;
								Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext() && !updateLocated) {
									Map.Entry<Long, Beacon> entry = entries.next();
									auxBeacon = entry.getValue();
									if (auxBeacon.equals(avoidingBeacon)) {
										avoidingBeacon = auxBeacon;
										updateLocated = true;
									}
								}
							}
						} else if (avoidingBeacon!=null) {
							// If the protocol is in other state, the UAV information is updated
							boolean updateLocated = false;
							Iterator<Map.Entry<Long, Beacon>> entries = MBCAPParam.beacons[numUAV].entrySet().iterator();
							while (entries.hasNext() && !updateLocated) {
								Map.Entry<Long, Beacon> entry = entries.next();
								auxBeacon = entry.getValue();
								if (auxBeacon.equals(avoidingBeacon)) {
									// Detecting the fact that this UAV has overtaken the other UAV
									if (MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE
											&& auxBeacon.event > avoidingBeacon.event) {
										hasBeenOvertaken = true;
									} else {
										hasBeenOvertaken = false;
									}
									avoidingBeacon = auxBeacon;
									// Store the other UAV risk location in case this UAV has not detected the risk by itself
									//  and that UAV is in "go on, please" state
									if (!MBCAPParam.impactLocationUTM[numUAV].containsKey(avoidingBeacon.uavId)
											&& avoidingBeacon.state == MBCAPState.GO_ON_PLEASE.getId()) {
										Point3D riskLocation = avoidingBeacon.points.get(1);
										if (riskLocation.x != 0 || riskLocation.y != 0 || riskLocation.z != 0) {
											MBCAPParam.impactLocationUTM[numUAV].put(avoidingBeacon.uavId, riskLocation);
											MBCAPGUITools.locateImpactRiskMark(riskLocation, numUAV, avoidingBeacon.uavId);
										}
									}
									updateLocated = true;
								}
							}
						}
					}

					// 3. Machine state analysis based on the previous information
					// 3.1 In normal flight state. Stop due to a new collision risk
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.NORMAL) {
						GUI.log(numUAV, MBCAPText.RISK_DETECTED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						GUI.updateGlobalInformation(MBCAPText.COLLISION_RISK_DETECTED);
						
						if (Copter.stopUAV(numUAV)) {
							stateTime = System.nanoTime();
							MBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.STAND_STILL);
						} else {
							GUI.log(numUAV, MBCAPText.RISK_DETECTED_ERROR);
						}
					}

					// 3.2 In stand still state (fist also wait for the UAV to stabilize)
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL
							&& System.nanoTime()-stateTime>=MBCAPParam.standStillTimeout) {

						// Change to passing by state (UAV with more priority)
						if (selfBeacon.uavId > avoidingBeacon.uavId
								&& avoidingBeacon.idAvoiding == selfBeacon.uavId
								&& MBCAPParam.idAvoiding.get(numUAV) == avoidingBeacon.uavId
								&& MBCAPState.getSatateById(avoidingBeacon.state) == MBCAPState.GO_ON_PLEASE) {
							GUI.log(numUAV, MBCAPText.RESUMING_MISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
								stateTime = System.nanoTime();
								// Progress update
								MBCAPGUITools.updateState(numUAV, MBCAPState.OVERTAKING);
							} else {
								GUI.log(numUAV, MBCAPText.RESUMING_MISSION_ERROR);
							}
						} else if (selfBeacon.uavId < avoidingBeacon.uavId
								&& avoidingBeacon.idAvoiding == selfBeacon.uavId
								&& MBCAPParam.idAvoiding.get(numUAV) == avoidingBeacon.uavId) {
							// UAV with less priority

							// Change to the states moving aside or go on, please
							if (MBCAPv3Helper.needsToMoveAside(numUAV, avoidingBeacon.points)) {
								// Change to the state moving aside. Changing MAV mode as previous step
								if (Copter.setFlightMode(0, FlightMode.GUIDED)) {
									GUI.log(numUAV, MBCAPText.MOVING + "...");
									stateTime = System.nanoTime();
									// Progress update
									MBCAPGUITools.updateState(numUAV, MBCAPState.MOVING_ASIDE);
									// Moving
									if (Copter.moveUAV(numUAV, MBCAPParam.targetPointGeo[numUAV], (float) Copter.getZRelative(numUAV), MBCAPParam.SAFETY_DISTANCE_RANGE, MBCAPParam.SAFETY_DISTANCE_RANGE)) {
										// Even when the UAV is close to destination, we also wait for it to be almost still
										long time = System.nanoTime();
										double speed = Copter.getSpeed(numUAV);
										while (speed > MBCAPParam.STABILIZATION_SPEED) {
											Tools.waiting(MBCAPParam.STABILIZATION_WAIT_TIME);
											if (System.nanoTime() - time > MBCAPParam.STABILIZATION_TIMEOUT) {
												break;
											}
											speed = Copter.getSpeed(numUAV);
										}
										if (speed > MBCAPParam.STABILIZATION_SPEED) {
											GUI.log(numUAV, MBCAPText.MOVING_ERROR_2);
										} else {
											GUI.log(numUAV, MBCAPText.MOVED);
										}
									} else {
										GUI.log(numUAV, MBCAPText.MOVING_ERROR);
									}
								} else {
									GUI.log(numUAV, MBCAPText.MOVING_ERROR);
								}
							} else {
								// Change to the state go on, please
								// There is no need to apply commands to the UAV
								GUI.log(numUAV, MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
								stateTime = System.nanoTime();
								// Progress update
								MBCAPGUITools.updateState(numUAV, MBCAPState.GO_ON_PLEASE);
							}
						} else if (avoidingBeacon.idAvoiding != MBCAPParam.ID_AVOIDING_DEFAULT
								&& avoidingBeacon.idAvoiding != selfBeacon.uavId) {
							// If this is a third UAV, reset the timeout and wait for its turn
							stateTime = System.nanoTime();
						}
					}

					// In passing by state. Change to normal state after overtaking the lower priority UAV
					// A timeout is used to increase stability
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.OVERTAKING
							&& selfBeacon.uavId > avoidingBeacon.uavId
							&& System.nanoTime() - stateTime > MBCAPParam.passingTimeout
							&& MBCAPv3Helper.overtakingFinished(numUAV, avoidingBeacon.uavId, avoidingBeacon.points.get(0))) {

						// There is no need to apply commands to the UAV
						GUI.log(numUAV, MBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						if (!isRealUAV) {
							MBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
							MBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
						}
						avoidingBeacon = null;
						stateTime = System.nanoTime();
						MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
						MBCAPParam.event.incrementAndGet(numUAV);
						// Progress update
						MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
					}

					// In moving aside state. Change to go on, please state
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.MOVING_ASIDE
							&& Copter.getUTMLocation(numUAV)
							.distance(MBCAPParam.targetPointUTM[numUAV]) < MBCAPParam.SAFETY_DISTANCE_RANGE) {
						// There is no need to apply commands to the UAV
						GUI.log(numUAV, MBCAPText.SAFE_PLACE + " " + MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						stateTime = System.nanoTime();
						// Progress update
						MBCAPGUITools.updateState(numUAV, MBCAPState.GO_ON_PLEASE);
						MBCAPParam.projectPath.set(numUAV, 0);	// Avoid projecting the predicted path over the theoretical one
					}

					// In go on, please state. Change to normal state when the risk collision is solved
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE
							&& hasBeenOvertaken) {
						hasBeenOvertaken = false;
						if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
							GUI.log(numUAV, MBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							MBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
							MBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
							
							avoidingBeacon = null;
							stateTime = System.nanoTime();
							MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
							MBCAPParam.event.incrementAndGet(numUAV);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							GUI.log(numUAV, MBCAPText.RESUMING_MISSION_ERROR);
						}
					}
				}

				// Passive waiting
				cicleTime = cicleTime + MBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			} else {
				// While not flying, only passive waiting
				cicleTime = cicleTime + MBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
	}
}
