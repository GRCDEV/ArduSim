package mbcapOLD.logic;

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
import main.Text;
import mbcap.logic.MBCAPText;
import main.Param.SimulatorState;
import mbcapOLD.gui.OLDMBCAPGUITools;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.logic.MBCAPParam;
import mbcapOLD.pojo.OLDBeacon;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVParam;

/** This class implements the collision risk check finite state machine. */

public class OLDCollisionDetectorThread extends Thread {

	private int numUAV; // UAV identifier in the simulator, beginning from 0
	private long cicleTime;
	private PriorityQueue<OLDBeacon> sortingQueue;

	@SuppressWarnings("unused")
	private OLDCollisionDetectorThread() {
	}

	public OLDCollisionDetectorThread(int numUAV) {
		this.numUAV = numUAV;
		this.cicleTime = 0;
		this.sortingQueue = new PriorityQueue<OLDBeacon>(Param.numUAVs, Collections.reverseOrder());
	}

	@Override
	public void run() {
		OLDBeacon selfBeacon;					// Beacon of this UAV
		OLDBeacon avoidingBeacon = null;		// Last beacon received from the UAV with which this UAV has collision risk
											// In the simulator Id==numUAV (the UAV position in arrays is Id)
		OLDBeacon auxBeacon = null;			// Beacon for auxiliary operations
		boolean hasBeenOvertaken = false;	// Whether this UAV has been finally overtaken by the other UAV
		long stateTime = System.nanoTime();	// Time elapsed since the current protocol state was set
											// In the beginning, the protocol state is always "Normal"

		// Do nothing until the experiment begins
		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}

		int waitingTime;
		boolean isRealUAV = Tools.getArduSimRole() == Tools.MULTICOPTER;
		// If two UAVs collide, then the protocol stops. Also, it stops when the experiment finishes
		while (!UAVParam.collisionDetected && Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			// Analyze received information while flying
			if (UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
				// 1. Periodic check if the UAV is stuck too many time in a protocol state. Cases:
				//   a) The other UAV has gone, so the current UAV can continue the mission
				//   b) The UAV was overtaking. As it has priority, we assume that it can directly change the state (it is already moving)
				//   c) Deadlock. The UAVs have to be landed
				MBCAPState state = MBCAPParam.state[numUAV];
				if (state != MBCAPState.NORMAL
						&& state != MBCAPState.EMERGENCY_LAND
						&& System.nanoTime() - stateTime > OLDMBCAPParam.uavDeadlockTimeout[numUAV]) {
					GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.PROT_TIMED);
					// Case a. The UAV can resume the mission
					if (avoidingBeacon == null
							&& state != MBCAPState.OVERTAKING) {
						if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
							stateTime = System.nanoTime();
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MISSION_RESUME);
							
							// Clean all risk marks as we don't know which UAV has failed
							if (!isRealUAV) {
								OLDMBCAPGUITools.removeImpactRiskMarks();
							}
							
							// Status update
							OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
							OLDMBCAPParam.event.incrementAndGet(numUAV);
							MBCAPParam.eventDeadlockSolved.incrementAndGet(numUAV);
							// Progress update
							OLDMBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MISSION_RESUME_ERROR);
						}
					} else if (state == MBCAPState.OVERTAKING) {
						// Case b. No need for commands for the UAV, just change the state
						if (avoidingBeacon!=null) {
							if (!isRealUAV) {
								OLDMBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
								OLDMBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
							}
							avoidingBeacon = null;
						}
						stateTime = System.nanoTime();
						OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
						OLDMBCAPParam.event.incrementAndGet(numUAV);
						MBCAPParam.eventDeadlockSolved.incrementAndGet(numUAV);
						// Progress update
						OLDMBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
					} else {
						// Case c. Deadlock. Landing the UAV
//						if (Copter.setFlightMode(numUAV, FlightMode.LAND)) {
//							GUI.warn(OLDMBCAPText.PROT_ERROR, SimParam.prefix[numUAV] + OLDMBCAPText.DEADLOCK);
//						} else {
//							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.DEADLOCK_ERROR);
//						}
						if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
							if (avoidingBeacon!=null) {
								if (!isRealUAV) {
									OLDMBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
									OLDMBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
								}
								avoidingBeacon = null;
							}
							stateTime = System.nanoTime();
							OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
							OLDMBCAPParam.event.incrementAndGet(numUAV);
							MBCAPParam.eventDeadlockFailed.incrementAndGet(numUAV);
							OLDMBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
						} else {
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MISSION_RESUME_ERROR);
						}//TODO restaurar
						
						
//						avoidingBeacon = null;
//						stateTime = System.nanoTime();
//						OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
//						OLDMBCAPParam.event.incrementAndGet(numUAV);
//						MBCAPParam.eventDeadlockFailed.incrementAndGet(numUAV);
//						// Progress update
//						OLDMBCAPGUITools.updateState(numUAV, MBCAPState.EMERGENCY_LAND);
					}
				}

				selfBeacon = OLDMBCAPParam.selfBeacon.get(numUAV);
				// Only start to check the collision risk if this UAV has started to send beacons
				if (selfBeacon != null) {
					// 2. Update the collision risk status information
					// 2.1. In normal flight mode, decide if there is risk of collision with other UAV
					if (MBCAPParam.state[numUAV] == MBCAPState.NORMAL) {
						// Sorting the beacons by priority
						sortingQueue.clear();
						Iterator<Map.Entry<Long, OLDBeacon>> entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry<Long, OLDBeacon> entry = entries.next();
							// Ignoring obsolete beacons
							if (System.nanoTime() - entry.getValue().time < OLDMBCAPParam.beaconExpirationTime) {
								sortingQueue.add(entry.getValue());
							}
						}

						// Selecting and ordering the UAVs that suppose a collision risk
						PriorityQueue<OLDBeacon> riskyUAVs = new PriorityQueue<OLDBeacon>(Param.numUAVs, Collections.reverseOrder());
						while (sortingQueue.size()>0) {
							auxBeacon = sortingQueue.poll();
							if (auxBeacon != null && OLDMBCAPHelper.hasCollisionRisk(numUAV, selfBeacon, auxBeacon)) {
								riskyUAVs.add(auxBeacon);
							}
						}

						// If no risk has been detected, another UAV could detect this UAV as a risk
						if (riskyUAVs.isEmpty()) {
							// Timeout needed if a collision risk situation has just been solved
							if (System.nanoTime()-stateTime>OLDMBCAPParam.solvedTimeout) {
								entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext()) {
									Map.Entry<Long, OLDBeacon> entry = entries.next();
									if (avoidingBeacon == null
											&& entry.getValue().idAvoiding == selfBeacon.uavId) {
										avoidingBeacon = entry.getValue();
										OLDMBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
									}
								}
							}
						} else {
							// Risk detected. Using the beacon with higher priority.
							OLDBeacon first = riskyUAVs.poll();
							// If there is risk with a UAV with more priority than current, it is also changed
							if (avoidingBeacon==null
									|| avoidingBeacon.uavId<first.uavId) {
								avoidingBeacon = first;
								OLDMBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
							}
						}

					} else if (MBCAPParam.state[numUAV] != MBCAPState.EMERGENCY_LAND) {
						// 2.2. If the protocol is being applied, a collision risk is already being analyzed

						// Special case, when the UAV stands still
						if (MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL) {

							// While waiting the timeout for protocol stability we check if other UAV with higher priority appears
							if (System.nanoTime()-stateTime<OLDMBCAPParam.standStillTimeout) {
								boolean newRiskDetected = false;
								OLDBeacon update = null;
								Iterator<Map.Entry<Long, OLDBeacon>> entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext() && !newRiskDetected) {
									Map.Entry<Long, OLDBeacon> entry = entries.next();
									auxBeacon = entry.getValue();
									// a. Change to other UAV with greater priority
									if (MBCAPState.getSatateById(auxBeacon.state) == MBCAPState.STAND_STILL && (
											(avoidingBeacon!=null
											&& auxBeacon.uavId>avoidingBeacon.uavId
											&& auxBeacon.idAvoiding==selfBeacon.uavId) ||
											(avoidingBeacon==null
											&& auxBeacon.idAvoiding==selfBeacon.uavId))) {
										avoidingBeacon = auxBeacon;
										OLDMBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
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
								Iterator<Map.Entry<Long, OLDBeacon>> entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
								while (entries.hasNext() && !updateLocated) {
									Map.Entry<Long, OLDBeacon> entry = entries.next();
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
							Iterator<Map.Entry<Long, OLDBeacon>> entries = OLDMBCAPParam.beacons[numUAV].entrySet().iterator();
							while (entries.hasNext() && !updateLocated) {
								Map.Entry<Long, OLDBeacon> entry = entries.next();
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
									if (!OLDMBCAPParam.impactLocationUTM[numUAV].containsKey(avoidingBeacon.uavId)
											&& avoidingBeacon.state == MBCAPState.GO_ON_PLEASE.getId()) {
										Point3D riskLocation = avoidingBeacon.points.get(1);
										if (riskLocation.x != 0 || riskLocation.y != 0 || riskLocation.z != 0) {
											OLDMBCAPParam.impactLocationUTM[numUAV].put(avoidingBeacon.uavId, riskLocation);
											OLDMBCAPGUITools.locateImpactRiskMark(riskLocation, numUAV, avoidingBeacon.uavId);
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
						GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.RISK_DETECTED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						GUI.updateGlobalInformation(OLDMBCAPText.COLLISION_RISK_DETECTED);
						
						if (Copter.stopUAV(numUAV)) {
							stateTime = System.nanoTime();
							OLDMBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
							// Progress update
							OLDMBCAPGUITools.updateState(numUAV, MBCAPState.STAND_STILL);
						} else {
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.RISK_DETECTED_ERROR);
						}
					}

					// 3.2 In stand still state (fist also wait for the UAV to stabilize)
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.STAND_STILL
							&& System.nanoTime()-stateTime>=OLDMBCAPParam.standStillTimeout) {

						// Change to passing by state (UAV with more priority)
						if (selfBeacon.uavId > avoidingBeacon.uavId
								&& avoidingBeacon.idAvoiding == selfBeacon.uavId
								&& OLDMBCAPParam.idAvoiding.get(numUAV) == avoidingBeacon.uavId
								&& MBCAPState.getSatateById(avoidingBeacon.state) == MBCAPState.GO_ON_PLEASE) {
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.RESUMING_MISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
								stateTime = System.nanoTime();
								// Progress update
								OLDMBCAPGUITools.updateState(numUAV, MBCAPState.OVERTAKING);
							} else {
								GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.RESUMING_MISSION_ERROR);
							}
						} else if (selfBeacon.uavId < avoidingBeacon.uavId
								&& avoidingBeacon.idAvoiding == selfBeacon.uavId
								&& OLDMBCAPParam.idAvoiding.get(numUAV) == avoidingBeacon.uavId) {
							// UAV with less priority

							// Change to the states moving aside or go on, please
							if (OLDMBCAPHelper.needsReposition(numUAV, avoidingBeacon.points)) {
								// Change to the state moving aside. Changing MAV mode as previous step
								if (Copter.setFlightMode(numUAV, FlightMode.GUIDED)) {
									GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MOVING + "...");
									stateTime = System.nanoTime();
									// Progress update
									OLDMBCAPGUITools.updateState(numUAV, MBCAPState.MOVING_ASIDE);
									// Moving
									if (Copter.moveUAV(numUAV, OLDMBCAPParam.targetPointGeo[numUAV], (float) UAVParam.uavCurrentData[numUAV].getZRelative(), OLDMBCAPParam.SAFETY_DISTANCE_RANGE, OLDMBCAPParam.SAFETY_DISTANCE_RANGE)) {
										// We have to wait until the UAV stops
										long time = System.nanoTime();
										while (UAVParam.uavCurrentData[numUAV].getSpeed() > UAVParam.STABILIZATION_SPEED) {
											Tools.waiting(UAVParam.STABILIZATION_WAIT_TIME);
											if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
												GUI.log(SimParam.prefix[numUAV] + MBCAPText.MOVING_ERROR_2);
												break;
											}
										}
										GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MOVED);
									} else {
										GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MOVING_ERROR);
									}
								} else {
									GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.MOVING_ERROR);
								}
							} else {
								// Change to the state go on, please
								// There is no need to apply commands to the UAV
								GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
								stateTime = System.nanoTime();
								// Progress update
								OLDMBCAPGUITools.updateState(numUAV, MBCAPState.GO_ON_PLEASE);
							}
						} else if (avoidingBeacon.idAvoiding != OLDMBCAPParam.ID_AVOIDING_DEFAULT
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
							&& System.nanoTime() - stateTime > OLDMBCAPParam.passingTimeout
							&& OLDMBCAPHelper.hasOvertaken(numUAV, avoidingBeacon.uavId, avoidingBeacon.points.get(0))) {

						// There is no need to apply commands to the UAV
						GUI.log(SimParam.prefix[numUAV]
								+ OLDMBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						if (!isRealUAV) {
							OLDMBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
							OLDMBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
						}
						avoidingBeacon = null;
						stateTime = System.nanoTime();
						OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
						OLDMBCAPParam.event.incrementAndGet(numUAV);
						// Progress update
						OLDMBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
					}

					// In moving aside state. Change to go on, please state
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.MOVING_ASIDE
							&& UAVParam.uavCurrentData[numUAV].getUTMLocation()
							.distance(OLDMBCAPParam.targetPointUTM[numUAV]) < OLDMBCAPParam.SAFETY_DISTANCE_RANGE) {
						// There is no need to apply commands to the UAV
						GUI.log(SimParam.prefix[numUAV]
								+ OLDMBCAPText.SAFE_PLACE + " " + OLDMBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						stateTime = System.nanoTime();
						// Progress update
						OLDMBCAPGUITools.updateState(numUAV, MBCAPState.GO_ON_PLEASE);
						OLDMBCAPParam.projectPath.set(numUAV, 0);	// Avoid projecting the predicted path over the theoretical one
					}

					// In go on, please state. Change to normal state when the risk collision is solved
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE
							&& hasBeenOvertaken) {
						hasBeenOvertaken = false;
						if (Copter.setFlightMode(numUAV, FlightMode.AUTO)) {
							GUI.log(SimParam.prefix[numUAV]
									+ OLDMBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							OLDMBCAPParam.impactLocationUTM[numUAV].remove(avoidingBeacon.uavId);
							OLDMBCAPGUITools.locateImpactRiskMark(null, numUAV, avoidingBeacon.uavId);
							
							avoidingBeacon = null;
							stateTime = System.nanoTime();
							OLDMBCAPParam.idAvoiding.set(numUAV, OLDMBCAPParam.ID_AVOIDING_DEFAULT);
							OLDMBCAPParam.event.incrementAndGet(numUAV);
							// Progress update
							OLDMBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							GUI.log(SimParam.prefix[numUAV] + OLDMBCAPText.RESUMING_MISSION_ERROR);
						}
					}
				}

				// Passive waiting
				cicleTime = cicleTime + OLDMBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			} else {
				// While not flying, only passive waiting
				cicleTime = cicleTime + OLDMBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					Tools.waiting(waitingTime);
				}
			}
		}
	}
}
