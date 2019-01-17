package sim.logic;

import org.mavlink.messages.MAV_CMD;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import main.Param;
import main.Text;
import mbcap.logic.MBCAPText;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class CollisionDetector extends Thread {
	
	public static volatile int collisions = 0;

	@Override
	public void run() {

		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
			Tools.waiting(SimParam.LONG_WAITING_TIME);
		}

		long checkTime = System.currentTimeMillis();
		int waitingTime;
		double distance;
		FlightMode mode;
		Waypoint selfLastWP, otherLastWP;
		int currentWP;
		int lastWP;
		UTMCoordinates lastWPUTM;
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
											GUI.log(Text.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + "(d=" + Tools.round(distance, 2) + ").");
											GUI.updateGlobalInformation(Text.COLLISION_DETECTED);
											
											collisions++;//TODO quitar
											
											
//											// The protocols must be stopped TODO descomentar
//											UAVParam.collisionDetected = true;
//											if (!Copter.landAllUAVs()) {
//												GUI.log(MBCAPText.LANDING_ERROR);
//											}
//											// Advising the user
//											GUI.warn(Text.COLLISION_TITLE, Text.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
										}
									}
								}
							}
						}
					}
				}
			}
			checkTime = checkTime + UAVParam.appliedCollisionCheckPeriod;
			waitingTime = (int)(checkTime - System.currentTimeMillis());
			if (waitingTime > 0) {
				Tools.waiting(waitingTime);
			}
		}
	}
}
