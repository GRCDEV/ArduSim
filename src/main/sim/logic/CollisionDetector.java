package main.sim.logic;

import org.mavlink.messages.MAV_CMD;

import api.API;
import api.pojo.FlightMode;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Waypoint;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.api.ArduSim;
import main.uavController.UAVParam;

/** 
 * Thread used to detect possible collisions among UAVs in simulations.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CollisionDetector extends Thread {

	@Override
	public void run() {
		
		ArduSim ardusim = API.getArduSim();

		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
			ardusim.sleep(SimParam.LONG_WAITING_TIME);
		}

		long checkTime = System.currentTimeMillis();
		long waitingTime;
		double distance;
		FlightMode mode;
		Waypoint selfLastWP, otherLastWP;
		int currentWP;
		int lastWP;
		Location2DUTM lastWPUTM;
		while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS && !UAVParam.collisionDetected) {
			if (UAVParam.distanceCalculusIsOnline) {
				for (int i = 0; i < Param.numUAVs && !UAVParam.collisionDetected; i++) {
					mode = UAVParam.flightMode.get(i);
					// If the UAV is flying, and not landing
					if (mode.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
							&& mode.getCustomMode() != 9) {
						boolean check = true;
						// If there is a mission, avoid checking while landing
						selfLastWP = UAVParam.lastWP[i];
						if (selfLastWP != null) {
							lastWPUTM = UAVParam.lastWPUTM[i];
							lastWP = selfLastWP.getNumSeq();
							currentWP = UAVParam.currentWaypoint.get(i);
							if (selfLastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND) {
								// Landing when reaching the end of the mission (lastWaypoint - 1)
								if (currentWP >= lastWP - 1) {
									check = false;
								}
							} else if (selfLastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH) {
								// Landing after returning to home
								if (currentWP >= lastWP - 1 && UAVParam.uavCurrentData[i].getUTMLocation().distance(lastWPUTM) < UAVParam.LAST_WP_THRESHOLD) {
									check = false;
								}
							}
						}

						if (check) {
							for (int j = i + 1; j < Param.numUAVs && !UAVParam.collisionDetected; j++) {
								mode = UAVParam.flightMode.get(j);
								if (mode.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
										&& mode.getCustomMode() != 9) {
									distance = UAVParam.distances[i][j].get();
									if (distance < UAVParam.collisionDistance
											&& Math.abs(UAVParam.uavCurrentData[i].getZ()-UAVParam.uavCurrentData[j].getZ()) < UAVParam.collisionAltitudeDifference) {
										check = true;
										// If there is a mission, avoid checking while landing
										otherLastWP = UAVParam.lastWP[j];
										if (otherLastWP != null) {
											lastWPUTM = UAVParam.lastWPUTM[j];
											lastWP = otherLastWP.getNumSeq();
											currentWP = UAVParam.currentWaypoint.get(j);
											if (otherLastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_LAND) {
												// Landing when reaching the end of the mission (lastWaypoint - 1)
												if (currentWP >= lastWP - 1) {
													check = false;
												}
											} else if (otherLastWP.getCommand() == MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH) {
												// Landing after returning to home
												if (currentWP >= lastWP - 1 && UAVParam.uavCurrentData[j].getUTMLocation().distance(lastWPUTM) < UAVParam.LAST_WP_THRESHOLD) {
													check = false;
												}
											}
										}
										
										if (check) {
											ArduSimTools.logGlobal(Text.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + "(d=" + API.getValidationTools().roundDouble(distance, 2) + ").");
											ArduSimTools.updateGlobalInformation(Text.COLLISION_DETECTED);
											
											// The protocols must be stopped
											UAVParam.collisionDetected = true;
											this.landAll();
											// Advising the user
											ArduSimTools.warnGlobal(Text.COLLISION_TITLE, Text.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
										}
									}
								}
							}
						}
					}
				}
			}
			checkTime = checkTime + UAVParam.appliedCollisionCheckPeriod;
			waitingTime = checkTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
	}
	
	/**
	 * Land all the UAVs that are flying.
	 * @return true if all the commands were successful, or even when they are not needed because one or more UAVs were not flying.
	 */
	private void landAll() {
		for (int i=0; i<Param.numUAVs; i++) {
			FlightMode current = UAVParam.flightMode.get(i);
			if (current.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
					&& current != FlightMode.LAND_ARMED) {
				UAVParam.newFlightMode[i] = FlightMode.LAND;
				UAVParam.MAVStatus.set(i, UAVParam.MAV_STATUS_REQUEST_MODE);
			}
		}
	}
}
