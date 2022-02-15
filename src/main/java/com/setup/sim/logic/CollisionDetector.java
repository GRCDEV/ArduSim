package com.setup.sim.logic;

import com.api.API;
import com.api.pojo.FlightMode;
import com.api.pojo.location.Waypoint;
import es.upv.grc.mapper.Location2DUTM;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.util.EnumValue;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.ArduSim;
import com.uavController.UAVParam;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/** 
 * Thread used to detect possible collisions among UAVs in simulations.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CollisionDetector extends Thread {
	
	private static final int NAV_LAND_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LAND).value();
	private static final int NAV_RETURN_TO_LAUNCH_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH).value();

	@Override
	public void run() {
		
		ArduSim ardusim = API.getArduSim();

		while (Param.simStatus == Param.SimulatorState.STARTING_UAVS
				|| Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
			ardusim.sleep(SimParam.LONG_WAITING_TIME);
		}

		Map<Integer, Set<Integer>> collisionMap = new HashMap<>();
		for(int i=0;i<Param.numUAVs;i++){
			collisionMap.put(i,new HashSet<>());
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
						// If there is a main.java.com.protocols.mission, avoid checking while landing
						selfLastWP = UAVParam.lastWP[i];
						if (selfLastWP != null) {
							lastWPUTM = UAVParam.lastWPUTM[i];
							lastWP = selfLastWP.getNumSeq();
							currentWP = UAVParam.currentWaypoint.get(i);
							if (selfLastWP.getCommand().value() == NAV_LAND_COMMAND) {
								// Landing when reaching the end of the main.java.com.protocols.mission (lastWaypoint - 1)
								if (currentWP >= lastWP - 1) {
									check = false;
								}
							} else if (selfLastWP.getCommand().value() == NAV_RETURN_TO_LAUNCH_COMMAND) {
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
											if (otherLastWP.getCommand().value() == NAV_LAND_COMMAND) {
												// Landing when reaching the end of the main.java.com.protocols.mission (lastWaypoint - 1)
												if (currentWP >= lastWP - 1) {
													check = false;
												}
											} else if (otherLastWP.getCommand().value() == NAV_RETURN_TO_LAUNCH_COMMAND) {
												// Landing after returning to home
												if (currentWP >= lastWP - 1 && UAVParam.uavCurrentData[j].getUTMLocation().distance(lastWPUTM) < UAVParam.LAST_WP_THRESHOLD) {
													check = false;
												}
											}
										}
										
										if (check) {
											ArduSimTools.logGlobal(Text.COLLISION_DETECTED_ERROR_1 + " " + i + " - " + j + "(d=" + API.getValidationTools().roundDouble(distance, 2) + ").");
											ArduSimTools.updateGlobalInformation(Text.COLLISION_DETECTED);
											collisionMap.get(i).add(j);
											if(UAVParam.stopAtCollision){
												UAVParam.collisionDetected = true;
												this.landAll();
												//Advising the user
												ArduSimTools.warnGlobal(Text.COLLISION_TITLE, Text.COLLISION_DETECTED_ERROR_2 + " " + i + " - " + j);
											}

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

		if(!UAVParam.stopAtCollision) {
			writeMapToCSV(collisionMap);
		}
	}

	private void writeMapToCSV(Map<Integer, Set<Integer>> collisionMap) {
		String timeStamp = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
		String filename = "Collisions"+timeStamp;
		try {
			FileWriter fw = new FileWriter(filename);
			for(int i=0;i<Param.numUAVs;i++){
				fw.write(i + ":");
				fw.write(collisionMap.get(i).toString());
				fw.write("\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Land all the UAVs that are flying.
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
