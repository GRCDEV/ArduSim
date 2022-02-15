package com.protocols.mbcap.logic;

import com.api.API;
import com.api.copter.Copter;
import com.api.MissionHelper;
import com.api.copter.MoveTo;
import com.api.copter.MoveToListener;
import com.api.pojo.FlightMode;
import es.upv.grc.mapper.*;
import com.api.*;
import com.protocols.mbcap.gui.MBCAPSimProperties;
import com.protocols.mbcap.pojo.Beacon;
import com.protocols.mbcap.pojo.MBCAPState;
import com.protocols.mbcap.pojo.ProgressState;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** This class implements the collision risk check finite state machine.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CollisionDetectorThread extends Thread implements WaypointReachedListener {
	
	private AtomicInteger event;
	private AtomicInteger deadlockSolved;
	private AtomicInteger deadlockFailed;
	private AtomicReference<MBCAPState> currentState;
	private AtomicLong idAvoiding;
	private AtomicInteger projectPath;
	private AtomicReference<Beacon> beacon;
	private AtomicReference<Location2DUTM> targetLocationUTM;
	private AtomicReference<DrawableSymbolGeo> targetLocationPX;
	private Map<Long, Location3DUTM> impactLocationUTM;
	private Map<Long, DrawableImageGeo> impactLocationPX;
	private Map<Long, Beacon> beacons;
	private List<ProgressState> progress;
	private int numUAV;
	
	private boolean isRealUAV;
	private Copter copter;
	private MissionHelper missionHelper;
	private GUI gui;
	private ArduSim ardusim;
	private int numUAVs;
	
	private long cicleTime;
	private PriorityQueue<Beacon> sortingQueue;
	
	@SuppressWarnings("unused")
	private CollisionDetectorThread() {
	}

	public CollisionDetectorThread(int numUAV) {
		this.event = MBCAPParam.event[numUAV];
		this.deadlockSolved = MBCAPParam.deadlockSolved[numUAV];
		this.deadlockFailed = MBCAPParam.deadlockFailed[numUAV];
		this.currentState = MBCAPParam.state[numUAV];
		this.idAvoiding = MBCAPParam.idAvoiding[numUAV];
		this.projectPath = MBCAPParam.projectPath[numUAV];
		this.beacon = MBCAPParam.selfBeacon[numUAV];
		this.targetLocationUTM = MBCAPParam.targetLocationUTM[numUAV];
		this.impactLocationUTM = MBCAPParam.impactLocationUTM[numUAV];
		this.ardusim = API.getArduSim();
		if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
			this.targetLocationPX = MBCAPParam.targetLocationScreen[numUAV];
			this.impactLocationPX = MBCAPParam.impactLocationScreen[numUAV];
		}
		this.beacons = MBCAPParam.beacons[numUAV];
		this.progress = MBCAPParam.progress[numUAV];
		this.numUAV = numUAV;
		
		this.copter = API.getCopter(numUAV);
		this.missionHelper = this.copter.getMissionHelper();
		this.gui = API.getGUI(numUAV);
		this.numUAVs = this.ardusim.getNumUAVs();
		
		this.cicleTime = 0;
		this.sortingQueue = new PriorityQueue<>(this.ardusim.getNumUAVs(), Collections.reverseOrder());
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
		
		Long prevRiskId = null;				// Used to avoid checking collision risk with the previously solved situation UAV ID
		long prevSolvedTimeout = 0;
		
		// Load the image used to show the risk location
		if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
			System.out.println(MBCAPSimProperties.EXCLAMATION_IMAGE_PATH);
			MBCAPSimProperties.exclamationImage = API.getFileTools().loadImage(MBCAPSimProperties.EXCLAMATION_IMAGE_PATH);
			if (MBCAPSimProperties.exclamationImage == null) {
				API.getGUI(0).exit(MBCAPText.WARN_IMAGE_LOAD_ERROR);
			}
		}

		// Do nothing until the experiment begins
		while (!ardusim.isExperimentInProgress()) {
			ardusim.sleep(MBCAPParam.SHORT_WAITING_TIME);
		}

		long waitingTime;
		// If two UAVs collide, then the protocol stops. Also, it stops when the experiment finishes
		while (!ardusim.collisionIsDetected() && ardusim.isExperimentInProgress()) {
			// While flying, analyze received information, and control the UAV
			if (copter.isFlying()) {
				
				// 1. Periodic check if the UAV is stuck too many time in a protocol state. Cases:
				//   a) The other UAV has gone, so the current UAV can continue the main.java.com.protocols.mission
				//   b) The UAV was overtaking. As it has priority, we assume that it can directly change the state (it is already moving)
				//   c) Deadlock. The UAVs have to be landed
				MBCAPState state = currentState.get();
				if (state != MBCAPState.NORMAL
						&& state != MBCAPState.EMERGENCY_LAND
						&& System.nanoTime() - stateTime > MBCAPParam.globalDeadlockTimeout) {
					gui.logUAV(MBCAPText.PROT_TIMED);
					// Case a. The UAV can resume the main.java.com.protocols.mission
					if (avoidingBeacon == null
							&& state != MBCAPState.OVERTAKING) {
						if (missionHelper.resume()) {
							stateTime = System.nanoTime();
							gui.logUAV(MBCAPText.MISSION_RESUME);
							
							// Clean all risk marks as we don't know which UAV has failed
							if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
								for (int i=0; i<this.numUAVs; i++) {
									MBCAPParam.impactLocationUTM[i].clear();
									
									for (Map.Entry<Long, DrawableImageGeo> entry : MBCAPParam.impactLocationScreen[i].entrySet()) {
										try {
											Mapper.Drawables.removeDrawable(entry.getValue());
										} catch (GUIMapPanelNotReadyException e) {
											e.printStackTrace();
										}
									}
									MBCAPParam.impactLocationScreen[i].clear();
								}
							}
							
							// Status update
							idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
							event.incrementAndGet();
							deadlockSolved.incrementAndGet();
							// Progress update
							this.updateState(MBCAPState.NORMAL);
							
						} else {
							gui.logUAV(MBCAPText.MISSION_RESUME_ERROR);
						}
					} else if (state == MBCAPState.OVERTAKING) {
						// Case b. No need for commands for the UAV, just change the state
						if (avoidingBeacon!=null) {
							prevRiskId = avoidingBeacon.uavId;
							prevSolvedTimeout = System.currentTimeMillis();
							if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
								impactLocationUTM.remove(avoidingBeacon.uavId);
								this.locateImpactRiskMark(null, avoidingBeacon.uavId);
							}
							avoidingBeacon = null;
						}
						stateTime = System.nanoTime();
						idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
						event.incrementAndGet();
						deadlockSolved.incrementAndGet();
						// Progress update
						this.updateState(MBCAPState.NORMAL);
					} else {
						// Case c. Deadlock. Landing the UAV
						if (copter.land()) {
							gui.warnUAV(MBCAPText.PROT_ERROR, MBCAPText.DEADLOCK);
						} else {
							gui.logUAV(MBCAPText.DEADLOCK_ERROR);
						}
						
						stateTime = System.nanoTime();
						prevRiskId = avoidingBeacon.uavId;
						prevSolvedTimeout = System.currentTimeMillis();
						avoidingBeacon = null;
						idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
						event.incrementAndGet();
						deadlockFailed.incrementAndGet();
						// Progress update
						this.updateState(MBCAPState.EMERGENCY_LAND);
					}
				}
				
				// No timeout, but the other UAV started landing --> consider the situation solved
				if (avoidingBeacon != null && avoidingBeacon.isLanding) {
					if (state == MBCAPState.OVERTAKING) {
						// No need for commands for the UAV, just change the state
						prevRiskId = avoidingBeacon.uavId;
						prevSolvedTimeout = System.currentTimeMillis();
						if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
							impactLocationUTM.remove(avoidingBeacon.uavId);
							this.locateImpactRiskMark(null, avoidingBeacon.uavId);
						}
						avoidingBeacon = null;
						
						stateTime = System.nanoTime();
						idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
						event.incrementAndGet();
						deadlockSolved.incrementAndGet();
						// Progress update
						this.updateState(MBCAPState.NORMAL);
					} else {
						if (missionHelper.resume()) {
							prevRiskId = avoidingBeacon.uavId;
							prevSolvedTimeout = System.currentTimeMillis();
							if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
								impactLocationUTM.remove(avoidingBeacon.uavId);
								this.locateImpactRiskMark(null, avoidingBeacon.uavId);
							}
							avoidingBeacon = null;
							
							stateTime = System.nanoTime();
							idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
							event.incrementAndGet();
							deadlockSolved.incrementAndGet();
							// Progress update
							this.updateState(MBCAPState.NORMAL);
						} else {
							gui.logUAV(MBCAPText.MISSION_RESUME_ERROR);
						}
					}
				}

				// 2. Update the collision risk status information
				selfBeacon = beacon.get();
				// Only start to check the collision risk if this UAV has started to send beacons
				if (selfBeacon != null) {
					// 2.1. In normal flight mode, decide if there is risk of collision with other UAV
					state = currentState.get();
					if (state == MBCAPState.NORMAL) {
						if (selfBeacon.state == MBCAPState.NORMAL.getId()
								&& !missionHelper.isLastWaypointReached()) {
							// Sorting the beacons by priority
							sortingQueue.clear();
							for (Map.Entry<Long, Beacon> entry : beacons.entrySet()) {
								// Ignoring obsolete beacons
								if (System.nanoTime() - entry.getValue().time < MBCAPParam.beaconExpirationTime) {
									sortingQueue.add(entry.getValue());
								}
							}

							// Selecting and ordering the UAVs that suppose a collision risk
							PriorityQueue<Beacon> riskyUAVs = new PriorityQueue<>(this.numUAVs, Collections.reverseOrder());
							boolean check;
							Location3DUTM riskyLocation;
							while (sortingQueue.size()>0) {
								auxBeacon = sortingQueue.poll();
								if (auxBeacon != null) {
									check = true;
									if (prevRiskId != null && auxBeacon.uavId == prevRiskId) {
										if (System.currentTimeMillis() - prevSolvedTimeout > MBCAPParam.recheckTimeout) {
											prevRiskId = null;
										} else {
											check = false;
										}
									}
									
									if (check) {
										riskyLocation = this.hasCollisionRisk(selfBeacon, auxBeacon);
										if (riskyLocation != null && !auxBeacon.isLanding) {
											impactLocationUTM.put(auxBeacon.uavId, riskyLocation);
											this.locateImpactRiskMark(riskyLocation, auxBeacon.uavId);
											riskyUAVs.add(auxBeacon);
										}
									}
								}
							}

							// If no risk has been detected, another UAV could detect this UAV as a risk
							if (riskyUAVs.isEmpty()) {
								// Timeout needed if a collision risk situation has just been solved
								if (System.nanoTime()-stateTime > MBCAPParam.resumeTimeout) {
									Iterator<Map.Entry<Long, Beacon>> entries = beacons.entrySet().iterator();
									while (entries.hasNext()) {
										Map.Entry<Long, Beacon> entry = entries.next();
										if (avoidingBeacon == null
												&& entry.getValue().idAvoiding == selfBeacon.uavId) {
											avoidingBeacon = entry.getValue();
											idAvoiding.set(avoidingBeacon.uavId);
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
									idAvoiding.set(avoidingBeacon.uavId);
								}
							}
						}

					} else if (state != MBCAPState.EMERGENCY_LAND) {
						// 2.2. If the protocol is being applied, a collision risk is already being analyzed

						// Special case, when the UAV stands still
						if (state == MBCAPState.STAND_STILL) {

							// While waiting the timeout for protocol stability we check if other UAV with higher priority appears
							if (System.nanoTime()-stateTime < MBCAPParam.standStillTimeout) {
								boolean newRiskDetected = false;
								Beacon update = null;
								Iterator<Map.Entry<Long, Beacon>> entries = beacons.entrySet().iterator();
								while (entries.hasNext() && !newRiskDetected) {
									Map.Entry<Long, Beacon> entry = entries.next();
									auxBeacon = entry.getValue();
									// a. Change to other UAV with greater priority, only if it is not landing
									if (MBCAPState.getSatateById(auxBeacon.state) == MBCAPState.STAND_STILL
											&& !auxBeacon.isLanding
											&& auxBeacon.idAvoiding==selfBeacon.uavId
											&& (avoidingBeacon == null || auxBeacon.uavId>avoidingBeacon.uavId)) {
										avoidingBeacon = auxBeacon;
										idAvoiding.set(avoidingBeacon.uavId);
										newRiskDetected = true;
									}
									// b. If no other UAV with higher priority is detected, update avoiding UAV information
									if (!newRiskDetected && auxBeacon.equals(avoidingBeacon)) {
										update = auxBeacon;
									}
								}
								if (!newRiskDetected && update!=null) {
									avoidingBeacon = update;
								}
							} else if (avoidingBeacon!=null) {
								// Stand still timeout exceeded, so we look for an update of the avoiding UAV
								boolean updateLocated = false;
								Iterator<Map.Entry<Long, Beacon>> entries = beacons.entrySet().iterator();
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
							Iterator<Map.Entry<Long, Beacon>> entries = beacons.entrySet().iterator();
							while (entries.hasNext() && !updateLocated) {
								Map.Entry<Long, Beacon> entry = entries.next();
								auxBeacon = entry.getValue();
								if (auxBeacon.equals(avoidingBeacon)) {
									// Detecting the fact that this UAV has overtaken the other UAV
									hasBeenOvertaken = state == MBCAPState.GO_ON_PLEASE
											&& auxBeacon.event > avoidingBeacon.event;
									avoidingBeacon = auxBeacon;
									// Store the other UAV risk location in case this UAV has not detected the risk by itself
									//  and that UAV is in "go on, please" state
									if (!impactLocationUTM.containsKey(avoidingBeacon.uavId)
											&& avoidingBeacon.state == MBCAPState.GO_ON_PLEASE.getId()) {
										Location3DUTM riskLocation = avoidingBeacon.points.get(1);
										if (riskLocation.x != 0 || riskLocation.y != 0 || riskLocation.z != 0) {
											impactLocationUTM.put(avoidingBeacon.uavId, riskLocation);
											this.locateImpactRiskMark(riskLocation, avoidingBeacon.uavId);
										}
									}
									updateLocated = true;
								}
							}
						}
					}

					// 3. Machine state analysis based on the previous information
					if (avoidingBeacon != null) {
						// 3.1 In normal flight state. Stop due to a new collision risk unless this UAV is landing
						if (state == MBCAPState.NORMAL && !missionHelper.isLastWaypointReached()
								&& copter.getFlightMode().getCustomMode() != 9) {
							gui.logUAV(MBCAPText.RISK_DETECTED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							gui.updateGlobalInformation(MBCAPText.COLLISION_RISK_DETECTED);
							if (missionHelper.pause()) {
								stateTime = System.nanoTime();
								idAvoiding.set(avoidingBeacon.uavId);
								// Progress update
								this.updateState(MBCAPState.STAND_STILL);
							} else {
								gui.logUAV(MBCAPText.RISK_DETECTED_ERROR);
							}
						}
						
						// 3.2 In stand still state (fist also wait for the UAV to stabilize)
						if (state == MBCAPState.STAND_STILL
								&& System.nanoTime() - stateTime >= MBCAPParam.standStillTimeout) {
							
							// If the other UAV starts landing, the situation is solved
							if (avoidingBeacon.isLanding) {
								if (missionHelper.resume()) {
									prevRiskId = avoidingBeacon.uavId;
									prevSolvedTimeout = System.currentTimeMillis();
									if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
										impactLocationUTM.remove(avoidingBeacon.uavId);
										this.locateImpactRiskMark(null, avoidingBeacon.uavId);
									}
									avoidingBeacon = null;
									
									stateTime = System.nanoTime();
									idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
									event.incrementAndGet();
									deadlockSolved.incrementAndGet();
									// Progress update
									this.updateState(MBCAPState.NORMAL);
								} else {
									gui.logUAV(MBCAPText.MISSION_RESUME_ERROR);
								}
							} else if (avoidingBeacon.idAvoiding == selfBeacon.uavId
									&& idAvoiding.get() == avoidingBeacon.uavId) {
								// Change to passing by state (UAV with more priority)
								if (selfBeacon.uavId > avoidingBeacon.uavId
										&& MBCAPState.getSatateById(avoidingBeacon.state) == MBCAPState.GO_ON_PLEASE) {
									gui.logUAV(MBCAPText.RESUMING_MISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
									if (missionHelper.resume()) {
										stateTime = System.nanoTime();
										// Progress update
										this.updateState(MBCAPState.OVERTAKING);
									} else {
										gui.logUAV(MBCAPText.RESUMING_MISSION_ERROR);
									}
								} else if (selfBeacon.uavId < avoidingBeacon.uavId
										&& MBCAPState.getSatateById(auxBeacon.state) == MBCAPState.STAND_STILL) {
									// UAV with less priority

									// Change to the states moving aside or go on, please
									if (this.needsToMoveAside(avoidingBeacon.points, avoidingBeacon.plannedSpeed)) {
										// Change to the state moving aside. Changing MAV mode as previous step
										if (copter.setFlightMode(FlightMode.GUIDED)) {
											gui.logUAV(MBCAPText.MOVING + "...");
											stateTime = System.nanoTime();
											// Progress update
											this.updateState(MBCAPState.MOVING_ASIDE);
											// Moving
											Location2DUTM utm = targetLocationUTM.get();
											Location3D location;
											try {
												location = new Location3D(utm, copter.getAltitudeRelative());
												
												MoveTo moveTo = copter.moveTo(location, new MoveToListener() {
													
													@Override
													public void onFailure() {
														gui.logUAV(MBCAPText.MOVING_ERROR);
													}
													
													@Override
													public void onCompleteActionPerformed() {
														// Not needed as we wait the thread to finish
													}
												});
												moveTo.start();
												try {
													moveTo.join();
												} catch (InterruptedException ignored) {}
												// Even when the UAV is close to destination, we also wait for it to be almost still
												long time = System.nanoTime();
												double speed = copter.getHorizontalSpeed();
												while (speed > MBCAPParam.STABILIZATION_SPEED) {
													ardusim.sleep(MBCAPParam.STABILIZATION_WAIT_TIME);
													if (System.nanoTime() - time > MBCAPParam.STABILIZATION_TIMEOUT) {
														break;
													}
													speed = copter.getHorizontalSpeed();
												}
												if (speed > MBCAPParam.STABILIZATION_SPEED) {
													gui.logUAV(MBCAPText.MOVING_ERROR_2);
												} else {
													gui.logUAV(MBCAPText.MOVED);
												}
											} catch (LocationNotReadyException e) {
												e.printStackTrace();
												if (copter.land()) {
													gui.warnUAV(MBCAPText.PROT_ERROR, e.getMessage());
												} else {
													gui.logUAV(MBCAPText.LOCATION_ERROR);
												}
												stateTime = System.nanoTime();
												prevRiskId = avoidingBeacon.uavId;
												prevSolvedTimeout = System.currentTimeMillis();
												avoidingBeacon = null;
												idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
												event.incrementAndGet();
												deadlockFailed.incrementAndGet();
												// Progress update
												this.updateState(MBCAPState.EMERGENCY_LAND);
											}
										} else {
											gui.logUAV(MBCAPText.MOVING_ERROR);
										}
									} else {
										// Change to the state go on, please
										// There is no need to apply commands to the UAV
										gui.logUAV(MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
										stateTime = System.nanoTime();
										// Progress update
										this.updateState(MBCAPState.GO_ON_PLEASE);
									}
								} else if (avoidingBeacon.idAvoiding != MBCAPParam.ID_AVOIDING_DEFAULT
										&& avoidingBeacon.idAvoiding != selfBeacon.uavId) {
									// If this is a third UAV, reset the timeout and wait for its turn
									stateTime = System.nanoTime();
								}
							}
						}
						
						// 3.3 In passing by state. Change to normal state after overtaking the lower priority UAV
						// A timeout is used to increase stability
						if (state == MBCAPState.OVERTAKING
								&& selfBeacon.uavId > avoidingBeacon.uavId
								&& System.nanoTime() - stateTime > MBCAPParam.passingTimeout) {
							Location3DUTM avoidingLocation = avoidingBeacon.points.get(0);
							if (this.overtakingFinished(avoidingBeacon.uavId, avoidingLocation)) {
								// There is no need to apply commands to the UAV
								gui.logUAV(MBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
								if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
									impactLocationUTM.remove(avoidingBeacon.uavId);
									this.locateImpactRiskMark(null, avoidingBeacon.uavId);
								}
								stateTime = System.nanoTime();
								prevRiskId = avoidingBeacon.uavId;
								prevSolvedTimeout = System.currentTimeMillis();
								avoidingBeacon = null;
								idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
								event.incrementAndGet();
								// Progress update
								this.updateState(MBCAPState.NORMAL);
							}
						}
						
						// 3.4 In moving aside state. Change to go on, please state
						if (state == MBCAPState.MOVING_ASIDE
								&& copter.getLocationUTM()
								.distance(targetLocationUTM.get()) < MBCAPParam.SAFETY_DISTANCE_RANGE) {
							// There is no need to apply commands to the UAV
							gui.logUAV(MBCAPText.SAFE_PLACE + " " + MBCAPText.GRANT_PERMISSION + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
							stateTime = System.nanoTime();
							// Progress update
							this.updateState(MBCAPState.GO_ON_PLEASE);
							projectPath.set(0);	// Avoid projecting the predicted path over the theoretical one
							targetLocationUTM.set(null);
							if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
								DrawableSymbolGeo symbol = targetLocationPX.getAndSet(null);
								if (symbol != null) {
									try {
										Mapper.Drawables.removeDrawable(symbol);
									} catch (GUIMapPanelNotReadyException e) {
										e.printStackTrace();
									}
								}
							}
							
						}
						
						// 3.5 In go on, please state. Change to normal state when the risk collision is solved
						if (state == MBCAPState.GO_ON_PLEASE
								&& hasBeenOvertaken) {
							hasBeenOvertaken = false;
							if (missionHelper.resume()) {
								gui.logUAV(MBCAPText.MISSION_RESUMED + " " + avoidingBeacon.uavId + "."); // uavId==numUAV in the simulator
								if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
									impactLocationUTM.remove(avoidingBeacon.uavId);
									this.locateImpactRiskMark(null, avoidingBeacon.uavId);
								}
								stateTime = System.nanoTime();
								prevRiskId = avoidingBeacon.uavId;
								prevSolvedTimeout = System.currentTimeMillis();
								avoidingBeacon = null;
								idAvoiding.set(MBCAPParam.ID_AVOIDING_DEFAULT);
								event.incrementAndGet();
								// Progress update
								this.updateState(MBCAPState.NORMAL);
								
							} else {
								gui.logUAV(MBCAPText.RESUMING_MISSION_ERROR);
							}
						}
					}
				}
			}

			// Passive waiting
			cicleTime = cicleTime + MBCAPParam.riskCheckPeriod;
			waitingTime = Math.round((cicleTime - System.nanoTime()) * 0.000001);
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
	}
	
	/** Update the protocol state. */
	public void updateState(MBCAPState state) {
		// Update the protocol state
		currentState.set(state);
		// Update the record of states used
		progress.add(new ProgressState(state, System.currentTimeMillis()));

		// Update the log in the main window
		gui.logUAV(MBCAPText.CAP + " = " + state.getName());
		// Update the progress dialog
		gui.updateProtocolState(state.getName());
	}
	
	/** Calculates if two UAVs have collision risk.
	 * <p>Requires to be in the normal protocol state.
	 * Returns null if no risk has been detected, or the location of the risky point otherwise.</p> */
	private Location3DUTM hasCollisionRisk(Beacon selfBeacon, Beacon receivedBeacon) {
		double distance;
		long selfTime, beaconTime;
		Location3DUTM selfPoint, receivedPoint;
		boolean checkTime = receivedBeacon.state == MBCAPState.NORMAL.getId()
				&& receivedBeacon.speed >= MBCAPParam.minSpeed && receivedBeacon.points.size() > 1
				&& selfBeacon.speed >= MBCAPParam.minSpeed && selfBeacon.points.size() > 1;
		for (int i = 0; i < selfBeacon.points.size(); i++) {
			selfTime = selfBeacon.time + i * MBCAPParam.hopTimeNS;
			selfPoint = selfBeacon.points.get(i);
			for (int j = 0; j < receivedBeacon.points.size(); j++) {
				// Do not check if the other UAV is in Go on, please and the location is the detected risk location
				if (j == 1
						&& (receivedBeacon.state == MBCAPState.GO_ON_PLEASE.getId() || receivedBeacon.state == MBCAPState.STAND_STILL.getId())) {
					break;
				}
				
				boolean risky = true;
				// Temporal collision risk (only if the other UAV is also in the normal protocol state, and with enough speed)
				if (checkTime) {
					beaconTime = receivedBeacon.time + j * MBCAPParam.hopTimeNS;
					if (Math.abs(selfTime - beaconTime) >= MBCAPParam.collisionRiskTime) {
						risky = false;
					}
				}
				// X,Y, Z collision risk
				if (risky) {
					receivedPoint = receivedBeacon.points.get(j);
					distance = selfPoint.distance(receivedPoint);
					if (distance < MBCAPParam.collisionRiskDistance
							&& Math.abs(selfPoint.z - receivedPoint.z) < MBCAPParam.collisionRiskAltitudeDifference) {
						return selfPoint;
					}
				}
			}
		}
		return null;
	}
	
	/** Calculates if the current UAV has overtaken another UAV. */
	private boolean overtakingFinished(long avoidingId, Location3DUTM target) {
		// Overtaken happened if the distance to the target UAV and to the risk location is increasing
		boolean success = MBCAPHelper.isMovingAway(copter.getLocationUTMLastKnown(), target);
		if (!success) {
			return false;
		}
		if (impactLocationUTM != null) {
			Location3DUTM riskLocation = impactLocationUTM.get(avoidingId);
			if (riskLocation != null) {
				success = MBCAPHelper.isMovingAway(copter.getLocationUTMLastKnown(), riskLocation);
				if (!success) {
					return false;
				}
			}
		}
		
		// AND the UAV has moved far enough
		return target.distance(beacon.get().points.get(0)) > MBCAPParam.collisionRiskDistance;
	}
	
	/** Checks if the UAV has to move aside, and it gets that safe point when needed. */
	private boolean needsToMoveAside(List<Location3DUTM> avoidPredictedLocations, double plannedSpeed) {
		// Errors detection
		if (avoidPredictedLocations == null || avoidPredictedLocations.size() <= 1) {
			gui.logUAV(MBCAPText.REPOSITION_ERROR_1);
			return false;
		}
		int locations = avoidPredictedLocations.size();
		
		
		Location3DUTM[][] segment = new Location3DUTM[locations - 1][2];
		for (int i = 0; i < segment.length; i++) {
			segment[i][0] = avoidPredictedLocations.get(i);
			segment[i][1] = avoidPredictedLocations.get(i+1);
		}

		Location2DUTM currentUTMLocation = copter.getLocationUTM();
		
		// Calculus of the angle with each segment with the previous and next segment
		double[][] angles;
		if (segment.length == 1) {
			angles = new double[][] {{0, 0}};
		} else {
			angles = new double[segment.length][2];
			Double angle = 0.0;
			for (int i = 0; i < segment.length; i++) {
				if (angle != null) {
					angles[i][0] = angle;
				}
				if (i == segment.length - 1) {
					angle = 0.0;
				} else {
					angle = CollisionDetectorThread.getAngleDifference(segment[i][0], segment[i][1], segment[i + 1][0], segment[i + 1][1]);
				}
				if (angle != null) {
					angles[i][1] = angle;
				}
			}
		}
		
		// Checking the distance of the UAV to the segments of the main.java.com.protocols.mission of the other UAV, and to the vertex of the main.java.com.protocols.mission
		Location2DUTM newLocation = null;
		boolean isInSafePlace = false;
		boolean foundConflict;
		Location2DUTM auxLocation;
		double waypointThreshold = CollisionDetectorThread.getWaypointThreshold(plannedSpeed);
		while (!isInSafePlace) {
			foundConflict = false;
			for (int i = 0; i < segment.length; i++) {
				auxLocation = CollisionDetectorThread.getSegmentSafeLocation(currentUTMLocation, segment[i][0], segment[i][1], angles[i],
						plannedSpeed, waypointThreshold);
				if (auxLocation != null) {
					currentUTMLocation = auxLocation;
					newLocation = auxLocation;
					foundConflict = true;
				}
			}
			if (!foundConflict) {
				// Check if the UAV is close to any vertex (waypoint) of the main.java.com.protocols.mission
				Location3DUTM currentWaypoint;
				for (int i = 1; i < locations - 1; i++) {
					currentWaypoint = avoidPredictedLocations.get(i);
					auxLocation = CollisionDetectorThread.getWaypointSafeLocation(currentUTMLocation, currentWaypoint);
					if (auxLocation != null) {
						currentUTMLocation = auxLocation;
						newLocation = auxLocation;
						foundConflict = true;
					}
				}
				if (!foundConflict) {
					isInSafePlace = true;
				}
			}
		}

		// If new coordinates have been found, it means that the UAV must move to a safer position
		if (newLocation != null) {
			targetLocationUTM.set(newLocation);
			if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
				try {
					targetLocationPX.set(Mapper.Drawables.addSymbolGeo(2, newLocation.getGeo(),
							DrawableSymbol.CROSS, 10, Color.BLACK, MBCAPParam.STROKE_POINT));
				} catch (GUIMapPanelNotReadyException | LocationNotReadyException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}
	
	private static double getWaypointThreshold(double speed) {
		double[] function = MBCAPParam.FUNCTION_WAYPOINT_THRESHOLD;
		return function[0] + function[1] * speed + function[2] * speed * speed;
	}
	
	/** Get the angle between two lines.
	 * <p>Returns null if any error happens, or the angle [-Math.PI, Math.PI] otherwise (positive if the second line turns left).</p> */
	private static Double getAngleDifference(Point2D.Double l0Start, Point2D.Double l0End,
			Point2D.Double l1Start, Point2D.Double l1End) {
		Double l0Angle = CollisionDetectorThread.getAngle(l0Start, l0End);
		Double l1Angle = CollisionDetectorThread.getAngle(l1Start, l1End);
		if (l0Angle == null || l1Angle == null) {
			return null;
		}
		double res = l1Angle - l0Angle;
		if (res < -Math.PI) {
			res = Math.PI * 2 + res;
		}
		if (res > Math.PI) {
			res = -Math.PI * 2 + res;
		}
		return res;
	}

	/** Get the angle between a line, and the line that goes from left to right (X axis, positive direction). */
	private static Double getAngle(Point2D.Double Start, Point2D.Double End) {
		double angle;
		double incX = End.x - Start.x;
		double incY = End.y - Start.y;
		if (incX == 0) {
			if (incY > 0) {
				angle = Math.PI / 2;
			} else if (incY < 0) {
				angle = -Math.PI / 2;
			} else {
				return null;
			}
		} else {
			angle = Math.atan(incY / incX);
			if (incX < 0) {
				if (angle > 0) {
					angle = angle - Math.PI;
				} else {
					angle = angle + Math.PI;
					// if the UAV is moving in opposite direction, the angle will be: Math.PI
				}
			}
		}
		return angle;
	}
	
	/** Calculates the safe place to move aside from a path segment.
	 * <p>Returns null if there is no need of moving aside.</p> */
	private static Location2DUTM getSegmentSafeLocation(Location2DUTM currentLocation,
			Location3DUTM prev, Location3DUTM post, double[] angles, double plannedSpeed, double waypointThreshold) {
		double currentX = currentLocation.x;
		double currentY = currentLocation.y;
		double prevX = prev.x;
		double prevY = prev.y;
		double postX = post.x;
		double postY = post.y;
		double x, y;
		
		Location2DUTM intersection = MBCAPHelper.getIntersection(currentLocation, prev, post);
		
		// currentLocation out of the segment case
		double incX = postX - prevX;
		double incY = postY - prevY;
		if (incX == 0) {
			// Vertical line case
			if (currentY < Math.min(prevY, postY)
					|| currentY > Math.max(prevY, postY)) {
				return null;
			}
		} else if (incY == 0) {
			// Horizontal line case
			if (currentX < Math.min(prevX, postX)
					|| currentX > Math.max(prevX, postX)) {
				return null;
			}
		} else {
			// General case
			if (intersection.x < Math.min(prevX, postX)
					|| intersection.x > Math.max(prevX, postX)) {
				return null;
			}
		}
		
		// Maybe it is close to one of the waypoints
		boolean goToTheOtherSide = false;
		double maxDistance;
		Double currentAngle = CollisionDetectorThread.getAngleDifference(prev, post, prev, currentLocation);
		double currentDistance = currentLocation.distance(intersection);
		if (currentAngle != null) {
			double dPrev = intersection.distance(prev);
			double dPost = intersection.distance(post);

			boolean isPrevCloser = dPrev <= dPost;
			// Analyze the waypoint the UAV is closer to, and if the distance is adequate
			if (isPrevCloser && dPrev < waypointThreshold) {
				// UAV in the inner side of the waypoint
				if ((currentAngle > 0 && angles[0] > 0 && angles[0] <= Math.PI / 2)
						|| (currentAngle < 0 && angles[0] < 0 && angles[0] >= -Math.PI / 2)) {
					// Analyze if the distance to the segment is enough
					maxDistance = CollisionDetectorThread.getCurveDistance(plannedSpeed, angles[0]);
					if (currentDistance < maxDistance + MBCAPParam.safePlaceDistance) {
						goToTheOtherSide = true;
					}
				}
			}
			if (!isPrevCloser && dPost < waypointThreshold) {
				// UAV in the inner side of the waypoint
				if ((currentAngle > 0 && angles[1] > 0 && angles[1] <= Math.PI / 2)
						|| (currentAngle < 0 && angles[1] < 0 && angles[1] >= -Math.PI / 2)) {
					// Analyze if the distance to the segment is enough
					maxDistance = CollisionDetectorThread.getCurveDistance(plannedSpeed, angles[1]);
					if (currentDistance < maxDistance + MBCAPParam.safePlaceDistance) {
						goToTheOtherSide = true;
					}
				}
			}
		}
		
		// Far enough case
		if (!goToTheOtherSide && currentDistance > MBCAPParam.safePlaceDistance) {
			return null;
		}
		
		// Has to move apart case
		if (incX == 0) {
			// Vertical line case
			if (currentX < prevX) {
				if (goToTheOtherSide) {
					x = prevX + MBCAPParam.safePlaceDistance + 0.1;	// We add a little margin due to double precision errors
				} else {
					x = prevX - MBCAPParam.safePlaceDistance - 0.1;
				}
			} else {
				if (goToTheOtherSide) {
					x = prevX - MBCAPParam.safePlaceDistance - 0.1;
				} else {
					x = prevX + MBCAPParam.safePlaceDistance + 0.1;
				}
			}
			y = currentY;
		} else if (incY == 0) {
			// Horizontal line case
			if (currentY < prevY) {
				if (goToTheOtherSide) {
					y = prevY + MBCAPParam.safePlaceDistance + 0.1;
				} else {
					y = prevY - MBCAPParam.safePlaceDistance - 0.1;
				}
			} else {
				if (goToTheOtherSide) {
					y = prevY - MBCAPParam.safePlaceDistance - 0.1;
				} else {
					y = prevY + MBCAPParam.safePlaceDistance + 0.1;
				}
			}
			x = currentX;
		} else {
			// General case
			double ds = MBCAPParam.safePlaceDistance + 0.1;
			double d12 = prev.distance(post);
			double incXS = ds / d12 * Math.abs(incY);
			if (currentX <= intersection.x) {
				if (goToTheOtherSide) {
					x = intersection.x + incXS;
				} else {
					x = intersection.x - incXS;
				}
			} else {
				if (goToTheOtherSide) {
					x = intersection.x - incXS;
				} else {
					x = intersection.x + incXS;
				}
			}
			y = currentY - incX / incY * (x - currentX);
		}

		// Returns the safe place in UTM coordinates
		return new Location2DUTM(x, y);
	}
	
	/** Get the maximum distance a UAV moves aside a waypoint performing a main.java.com.protocols.mission, depending on the flight speed and the angle between the two segments of the main.java.com.protocols.mission.
	 * <p> We get the values from experimental equations for the previous and next values available, and then we interpolate for the given speed and angle. */
	private static double getCurveDistance(double speed, double angle) {
		
		double[][] fSpeed = MBCAPParam.FUNCTION_DISTANCE_VS_SPEED;
		double[][] fAlpha = MBCAPParam.FUNCTION_DISTANCE_VS_ALPHA;
		double alpha = Math.abs(angle);
		
		double[] dS = new double[2];
		
		// We already know that the first equation (i == 0) is for the angle 0 radians, so the previous value starts in 0
		int prevSpeed = 0;
		int postSpeed;
		for (int i = 1; i < fSpeed.length; i++) {
			if (fSpeed[i][0] < alpha) {
				prevSpeed = i;
			}
		}
		double inc;
		if (prevSpeed == fSpeed.length - 1) {
			postSpeed = prevSpeed;
			inc = 0;
		} else {
			postSpeed = prevSpeed + 1;
			inc = (alpha - fSpeed[prevSpeed][0]) / (fSpeed[postSpeed][0] -fSpeed[prevSpeed][0]);
		}
		dS[0] = fSpeed[prevSpeed][1] + fSpeed[prevSpeed][2] * speed + fSpeed[prevSpeed][3] * speed * speed;
		dS[1] = fSpeed[postSpeed][1] + fSpeed[postSpeed][2] * speed + fSpeed[postSpeed][3] * speed * speed;
		
		double dSpeed = dS[0] + (dS[1] - dS[0]) * inc;
		
		double[] dA = new double[2];
		int prevAngle = 0;
		int postAngle;
		for (int i = 1; i < fAlpha.length; i++) {
			if (fAlpha[i][0] < speed) {
				prevAngle = i;
			}
		}
		
		if (prevAngle == fAlpha.length - 1) {
			postAngle = prevAngle;
			inc = 0;
		} else {
			postAngle = prevAngle + 1;
			inc = (speed - fAlpha[prevAngle][0]) / (fAlpha[postAngle][0] -fAlpha[prevAngle][0]);
		}
		dA[0] = fAlpha[prevAngle][1] + fAlpha[prevAngle][2] * alpha + fAlpha[prevAngle][3] * alpha * alpha;
		dA[1] = fAlpha[postAngle][1] + fAlpha[postAngle][2] * alpha + fAlpha[postAngle][3] * alpha * alpha;
		
		double dAngle = dA[0] + (dA[1] - dA[0]) * inc;
		
		return Math.max(dSpeed, dAngle);
	}
	
	/** Calculates the safe place to move aside from a waypoint.
	 * <p>Returns null if there is no need of moving aside.</p> */
	private static Location2DUTM getWaypointSafeLocation(Location2DUTM currentUTMLocation, Location3DUTM currentWaypoint) {
		double currentDistance = currentUTMLocation.distance(currentWaypoint);
		if (currentDistance > MBCAPParam.safePlaceDistance) {
			return null;
		}
		double incX, incY;
		incX = currentUTMLocation.x - currentWaypoint.x;
		incY = currentUTMLocation.y - currentWaypoint.y;
		double x, y;
		if (incX == 0) {
			// Vertical line case
			if (currentUTMLocation.y < currentWaypoint.y) {
				y = currentWaypoint.y - MBCAPParam.safePlaceDistance - 0.1;	// We add a little margin due to double precision errors;
			} else {
				y = currentWaypoint.y + MBCAPParam.safePlaceDistance + 0.1;
			}
			x = currentUTMLocation.x;
		} else if (incY == 0) {
			// Horizontal line case
			if (currentUTMLocation.x < currentWaypoint.x) {
				x = currentWaypoint.x - MBCAPParam.safePlaceDistance - 0.1;
			} else {
				x = currentWaypoint.x + MBCAPParam.safePlaceDistance + 0.1;
			}
			y = currentUTMLocation.y;
		} else {
			// General case
			double ds = MBCAPParam.safePlaceDistance + 0.1;
			double incXS = ds / currentDistance * Math.abs(incX);
			if (currentUTMLocation.x < currentWaypoint.x) {
				x = currentWaypoint.x - incXS;
			} else {
				x = currentWaypoint.x + incXS;
			}
			y = currentWaypoint.y + incY / incX * (x - currentWaypoint.x);
		}
		return new Location2DUTM(x, y);
	}
	
	/** Stores (or removes when riskUTMLocation==null) the collision risk location that is drawn. */
	public void locateImpactRiskMark(Location3DUTM riskUTMLocation, long beaconId) {
		if (ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
			if (riskUTMLocation == null) {
				DrawableImageGeo current = impactLocationPX.remove(beaconId);
				if (current != null && ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
					try {
						Mapper.Drawables.removeDrawable(current);
					} catch (GUIMapPanelNotReadyException e) {
						e.printStackTrace();
					}
				}
			} else {
				DrawableImageGeo current = impactLocationPX.get(beaconId);
				try {
					Location2DGeo target = riskUTMLocation.getGeo();
					
					if (current == null && ardusim.getArduSimRole() == ArduSim.SIMULATOR_GUI) {
						impactLocationPX.put(beaconId, Mapper.Drawables.addImageGeo(1, target, 0, MBCAPSimProperties.exclamationImage, MBCAPSimProperties.EXCLAMATION_PX_SIZE));
					} else {
						current.updateLocation(target);
					}
				} catch (LocationNotReadyException | GUIMapPanelNotReadyException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onWaypointReachedActionPerformed(int numUAV, int numSeq) {
		// Project the predicted path over the planned main.java.com.protocols.mission
		if (this.numUAV == numUAV) {
			projectPath.set(1);
		}
	}
	
}
