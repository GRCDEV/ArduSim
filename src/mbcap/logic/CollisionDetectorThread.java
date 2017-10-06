package mbcap.logic;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import api.API;
import api.GUIHelper;
import api.MissionHelper;
import main.Param;
import main.Param.SimulatorState;
import mbcap.gui.MBCAPGUITools;
import mbcap.logic.MBCAPParam.MBCAPState;
import mbcap.pojo.Beacon;
import sim.logic.SimParam;
import uavLogic.UAVParam;
import uavLogic.UAVParam.Mode;

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
		this.sortingQueue = new PriorityQueue<Beacon>(Param.numUAVs, Collections.reverseOrder());
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
		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
		}

		int waitingTime;
		// If two UAVs collide, then the protocol stops. Also, it stops when the UAV lands
		while (!MBCAPParam.stopProtocol && Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			// Analyze received information while flying
			if (UAVParam.flightMode.get(numUAV).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
				// 1. Periodic check if the UAV is stuck too many time in a protocol state. Cases:
				//   a) The other UAV has gone, so the current UAV can continue the mission
				//   b) The UAV was overtaking. As it has priority, we assume that it can directly change the state (it is already moving)
				//   c) Deadlock. The UAVs have to be landed
				MBCAPState state = MBCAPParam.state[numUAV];
				if (state != MBCAPState.NORMAL
						&& state != MBCAPState.EMERGENCY_LAND
						&& System.nanoTime() - stateTime > MBCAPParam.deadlockTimeout) {
					MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.PROT_TIMED);
					// Case a. The UAV can resume the mission
					if (avoidingBeacon == null
							&& state != MBCAPState.OVERTAKING) {
						if (API.setMode(numUAV, UAVParam.Mode.AUTO_ARMED)) {
							stateTime = System.nanoTime();
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MISSION_RESUME);
							
							// Clean all risk marks as we don't know which UAV has failed
							if (!Param.IS_REAL_UAV) {
								MBCAPGUITools.removeImpactRiskMarks();
							}
							
							// Status update
							MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
							MBCAPParam.event.incrementAndGet(numUAV);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MISSION_RESUME_ERROR);
						}
					} else if (state == MBCAPState.OVERTAKING) {
						// Case b. No need for commands for the UAV, just change the state
						if (avoidingBeacon!=null) {
							if (!Param.IS_REAL_UAV) {
								MBCAPGUITools.locateImpactRiskMark(null, numUAV, (int)avoidingBeacon.uavId);
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
						if (API.setMode(numUAV, UAVParam.Mode.LAND_ARMED)) {
							GUIHelper.warn(MBCAPText.PROT_ERROR, SimParam.prefix[numUAV] + MBCAPText.DEADLOCK);
						} else {
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.DEADLOCK_ERROR);
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
							sortingQueue.add(entry.getValue());
						}

						// Selecting and ordering the UAVs that suppose a collision risk
						PriorityQueue<Beacon> riskyUAVs = new PriorityQueue<Beacon>(Param.numUAVs, Collections.reverseOrder());
						while (sortingQueue.size()>0) {
							auxBeacon = sortingQueue.poll();
							if (auxBeacon != null && MBCAPHelper.hasCollisionRisk(numUAV, selfBeacon, auxBeacon)) {
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
									updateLocated = true;
								}
							}
						}
					}

					// 3. Machine state analysis based on the previous information
					// 3.1 In normal flight state. Stop due to a new collision risk
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.NORMAL) {
						MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.RISK_DETECTED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						MissionHelper.setMissionGlobalInformation(MBCAPText.COLLISION_RISK_DETECTED);
						
						if (API.stopUAV(numUAV)) {
							stateTime = System.nanoTime();
							MBCAPParam.idAvoiding.set(numUAV, avoidingBeacon.uavId);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.STAND_STILL);
						} else {
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.RISK_DETECTED_ERROR);
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
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.RESUMING_MISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							if (API.setMode(numUAV, UAVParam.Mode.AUTO_ARMED)) {
								stateTime = System.nanoTime();
								// Progress update
								MBCAPGUITools.updateState(numUAV, MBCAPState.OVERTAKING);
							} else {
								MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.RESUMING_MISSION_ERROR);
							}
						} else if (selfBeacon.uavId < avoidingBeacon.uavId
								&& avoidingBeacon.idAvoiding == selfBeacon.uavId
								&& MBCAPParam.idAvoiding.get(numUAV) == avoidingBeacon.uavId) {
							// UAV with less priority

							// Change to the states moving aside or go on, please
							if (MBCAPHelper.needsReposition(numUAV, avoidingBeacon.points)) {
								// Change to the state moving aside. Changing MAV mode as previous step
								if (API.setMode(0, Mode.GUIDED_ARMED)) {
									MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MOVING + "...");
									stateTime = System.nanoTime();
									// Progress update
									MBCAPGUITools.updateState(numUAV, MBCAPState.MOVING_ASIDE);
									// Moving
									if (API.moveUAV(numUAV, MBCAPParam.targetPointGeo[numUAV], (float) UAVParam.uavCurrentData[numUAV].getZRelative())) {
										MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MOVED);
									} else {
										MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MOVING_ERROR);
									}
								} else {
									MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.MOVING_ERROR);
								}
							} else {
								// Change to the state go on, please
								// There is no need to apply commands to the UAV
								MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
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
							&& MBCAPHelper.hasOvertaken(numUAV, avoidingBeacon.points.get(0))) {

						// There is no need to apply commands to the UAV
						MissionHelper.log(SimParam.prefix[numUAV]
								+ MBCAPText.MISSION_RESUMED + " " + (avoidingBeacon.uavId+1) + "."); // uavId==numUAV in the simulator
						if (!Param.IS_REAL_UAV) {
							MBCAPGUITools.locateImpactRiskMark(null, numUAV, (int)avoidingBeacon.uavId);
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
							&& UAVParam.uavCurrentData[numUAV].getUTMLocation()
							.distance(MBCAPParam.targetPointUTM[numUAV]) < MBCAPParam.SAFETY_DISTANCE_RANGE) {
						// There is no need to apply commands to the UAV
						MissionHelper.log(SimParam.prefix[numUAV]
								+ MBCAPText.SAFE_PLACE + " " + MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
						stateTime = System.nanoTime();
						// Progress update
						MBCAPGUITools.updateState(numUAV, MBCAPState.GO_ON_PLEASE);
						
					}

					// In go on, please state. Change to normal state when the risk collision is solved
					if (avoidingBeacon != null
							&& MBCAPParam.state[numUAV] == MBCAPState.GO_ON_PLEASE
							&& hasBeenOvertaken) {
						hasBeenOvertaken = false;
						if (API.setMode(numUAV, UAVParam.Mode.AUTO_ARMED)) {
							MissionHelper.log(SimParam.prefix[numUAV]
									+ MBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							MBCAPGUITools.locateImpactRiskMark(null, numUAV, (int)avoidingBeacon.uavId);
							
							avoidingBeacon = null;
							stateTime = System.nanoTime();
							MBCAPParam.idAvoiding.set(numUAV, MBCAPParam.ID_AVOIDING_DEFAULT);
							MBCAPParam.event.incrementAndGet(numUAV);
							// Progress update
							MBCAPGUITools.updateState(numUAV, MBCAPState.NORMAL);
							
						} else {
							MissionHelper.log(SimParam.prefix[numUAV] + MBCAPText.RESUMING_MISSION_ERROR);
						}
					}
				}

				// Passive waiting
				cicleTime = cicleTime + MBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			} else {
				// While not flying, only passive waiting
				cicleTime = cicleTime + MBCAPParam.riskCheckPeriod;
				waitingTime = (int)((cicleTime - System.nanoTime()) * 0.000001);
				if (waitingTime > 0) {
					GUIHelper.waiting(waitingTime);
				}
			}
		}
	}
}
