package com.api;

import com.api.copter.Copter;
import com.setup.Param;
import com.setup.Text;
import com.api.pojo.FlightMode;
import com.api.pojo.location.Waypoint;
import com.api.pojo.location.WaypointSimplified;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.util.EnumValue;
import com.api.hiddenFunctions.HiddenFunctions;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.util.ArrayList;
import java.util.List;

/**
 * API for mission based applications.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MissionHelper {
	
	private int numUAV;
	private ArduSim ardusim;
	private Copter copter;
	private static final int NAV_WAYPOINT_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_WAYPOINT).value();
	private static final int NAV_LOITER_UNLIM_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LOITER_UNLIM).value();
	private static final int NAV_LOITER_TURNS_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LOITER_TURNS).value();
	private static final int NAV_LOITER_TIME_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LOITER_TIME).value();
	private static final int NAV_SPLINE_WAYPOINT_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_SPLINE_WAYPOINT).value();
	private static final int PAYLOAD_PREPARE_DEPLOY_COMMAND = EnumValue.of(MavCmd.MAV_CMD_PAYLOAD_PREPARE_DEPLOY).value();
	private static final int NAV_LOITER_TO_ALT_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LOITER_TO_ALT).value();
	private static final int NAV_LAND_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LAND).value();
	private static final int NAV_RETURN_TO_LAUNCH_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH).value();
	private static final int NAV_TAKEOFF_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_TAKEOFF).value();
	
	@SuppressWarnings("unused")
	private MissionHelper() {}
	
	public MissionHelper(int numUAV, Copter copter) {
		this.numUAV = numUAV;
		this.ardusim = API.getArduSim();
		this.copter = copter;
	}
	
	/**
	 * Get the current waypoint of the mission for a UAV.
	 * <p>Use only when the UAV is performing a planned mission.</p>
	 * @return The current waypoint of the mission, starting from 0.
	 */
	public int getCurrentWaypoint() {
		return UAVParam.currentWaypoint.get(numUAV);
	}
	
	/**
	 * Get the mission currently stored on the multicopter.
	 * <p>Mission only available if it is previously sent to the drone <i>updateUAV(List&lt;Waypoint&gt;)</i>.</p>
	 * @return The mission currently stored in the UAV in geographic coordinates.
	 */
	public List<Waypoint> get() {
		return UAVParam.currentGeoMission[numUAV];
	}
	
	/**
	 * Get the missions loaded from files for all the UAVs, in geographic coordinates, whether they were previously sent to the UAV or not.
	 * @return Array with the missions of the UAVs. They are only available when they are set with the method <i>setMissionsLoaded(List<Waypoint>[])</i> in the configuration dialog. If this method is used before, it returns null.
	 */
	public List<Waypoint>[] getMissionsLoaded() {
		return UAVParam.missionGeoLoaded;
	}
	
	/**
	 * Get the mission shown on screen.
	 * <p>Mission only available if it is previously sent to the drone with <i>updateUAV(List&lt;Waypoint&gt;)</i>.</p>
	 * @return The simplified mission shown in the screen in UTM coordinates.
	 */
	public List<WaypointSimplified> getSimplified() {
		return UAVParam.missionUTMSimplified.get(numUAV);
	}
	
	/**
	 * Find out if the UAV has reached the last waypoint of the mission.
	 * @return true if the last waypoint of the mission has been reached.
	 */
	public boolean isLastWaypointReached() {
		return UAVParam.lastWaypointReached[numUAV].get();
	}
	
	/**
	 * Land the UAV if it is close enough to the last waypoint.
	 * <p>This method can be launched periodically, it only informs that the last waypoint is reached once, and it only lands the UAV if it close enough to the last waypoint and not already landing or on the ground.</p>
	 * @param distanceThreshold (meters) Horizontal distance from the last waypoint of the mission to assert that the UAV has to land.
	 */
	public void landIfEnded(double distanceThreshold) {
		Waypoint lastWP = UAVParam.lastWP[numUAV];
		// Only check when the last waypoint is reached
		if (UAVParam.lastWaypointReached[numUAV].get()) {
			// Do nothing if the UAV is already landing
			FlightMode current = UAVParam.flightMode.get(numUAV);
			if (current.getCustomMode() == 9) {
				return;
			}
			if (lastWP.getCommand().value() == MissionHelper.NAV_LAND_COMMAND
					|| (lastWP.getCommand().value() == MissionHelper.NAV_RETURN_TO_LAUNCH_COMMAND && UAVParam.RTLAltitudeFinal[numUAV] == 0)) {
				return;
			}
			
			// Land only if the UAV is really close to the last waypoint force it to land
			if (UAVParam.uavCurrentData[numUAV].getUTMLocation().distance(UAVParam.lastWPUTM[numUAV]) < distanceThreshold) {
				if (current.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING
						&& current != FlightMode.LAND_ARMED) {
					// We assume that the command will not fail to avoid concurrency problems when several UAVs are run in the same machine (simulations)
					UAVParam.newFlightMode[numUAV] = FlightMode.LAND;
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_MODE);
				}
			}
		}
	}
	
	/**
	 * Stops the mission and stabilizes the UAV in the current location, changing to brake flight mode.
	 * <p>Blocking method until the UAV is almost stopped.</p>
	 * @return true if all the commands were successful.
	 */
	public boolean pause() {
		if (HiddenFunctions.stabilize(numUAV) && this.copter.setFlightMode(FlightMode.BRAKE)) {
			long time = System.nanoTime();
			while (UAVParam.uavCurrentData[numUAV].getHorizontalSpeed() > UAVParam.STABILIZATION_SPEED) {
				ardusim.sleep(UAVParam.STABILIZATION_WAIT_TIME);
				if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
					ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.STOP_ERROR_1);
					return true;
				}
			}

			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.STOP);
			return true;
		}
		ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.STOP_ERROR_2);
		return false;
	}
	
	/**
	 * Remove the current mission from the UAV.
	 * @return true if the command was successful.
	 */
	private boolean remove() {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_CLEAR_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.MISSION_DELETE_ERROR);
			return false;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.MISSION_DELETE);
			return true;
		}
	}
	
	/**
	 * Resume the current mission previously paused.
	 * @return true if the command was successful.
	 */
	public boolean resume() {
		
		return this.copter.setFlightMode(FlightMode.AUTO);
		
	}
	
	/**
	 * Retrieve the mission stored on the UAV.
	 * <p>The new value available on <i>get()</i>.
	 * The simplified version of the mission in UTM coordinates available on <i>getSimplified()</i>.</p>
	 * @return true if the command was successful.
	 */
	private boolean retrieve() {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.MISSION_GET_ERROR);
			return false;
		} else {
			this.simplify();
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.MISSION_GET);
			return true;
		}
	}
	
	/**
	 * Create the simplified mission shown on screen, and forces view to re-scale.
	 */
	private void simplify() {
		List<WaypointSimplified> missionUTMSimplified = new ArrayList<>();
	
		// Hypothesis:
		//   The first take off waypoint retrieved from the UAV is used as "home"
		//   The "home" coordinates are no modified along the mission
		WaypointSimplified first = null;
		boolean foundFirst = false;
		Waypoint wp;
		Location2DUTM utm;
		for (int i=1; i<UAVParam.currentGeoMission[numUAV].size(); i++) {
			wp = UAVParam.currentGeoMission[numUAV].get(i);
			int command = wp.getCommand().value();
			if (command == MissionHelper.NAV_WAYPOINT_COMMAND || command == MissionHelper.NAV_LOITER_UNLIM_COMMAND
					|| command == MissionHelper.NAV_LOITER_TURNS_COMMAND || command == MissionHelper.NAV_LOITER_TIME_COMMAND
					|| command == MissionHelper.NAV_SPLINE_WAYPOINT_COMMAND || command == MissionHelper.PAYLOAD_PREPARE_DEPLOY_COMMAND
					|| command == MissionHelper.NAV_LOITER_TO_ALT_COMMAND || command == MissionHelper.NAV_LAND_COMMAND) {
				if (wp.getLatitude() != 0.0 || wp.getLongitude() != 0.0) {
					utm = wp.getUTM();
					WaypointSimplified swp = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
					missionUTMSimplified.add(swp);
				}
			} else if (command == MissionHelper.NAV_RETURN_TO_LAUNCH_COMMAND) {
				if (!foundFirst) {
					ArduSimTools.logGlobal(Text.SIMPLIFYING_WAYPOINT_LIST_ERROR + Param.id[numUAV]);
				} else {
					WaypointSimplified s = new WaypointSimplified(wp.getNumSeq(),
							first.x, first.y, UAVParam.RTLAltitude[numUAV]);
					missionUTMSimplified.add(s);
				}
			} else if (command == MissionHelper.NAV_TAKEOFF_COMMAND) {
				// The geographic coordinates have been set by the flight controller
				utm = wp.getUTM();
				WaypointSimplified twp = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
				missionUTMSimplified.add(twp);
				if (!foundFirst) {
					utm = wp.getUTM();
					first = new WaypointSimplified(wp.getNumSeq(), utm.x, utm.y, wp.getAltitude());
					foundFirst = true;
				}
			}
		}
		UAVParam.missionUTMSimplified.set(numUAV, missionUTMSimplified);
		Waypoint lastWP = UAVParam.currentGeoMission[numUAV].get(UAVParam.currentGeoMission[numUAV].size() - 1);
		UAVParam.lastWP[numUAV] = lastWP;
		if (lastWP.getCommand().value() == MissionHelper.NAV_RETURN_TO_LAUNCH_COMMAND) {
			Waypoint home = UAVParam.currentGeoMission[numUAV].get(0);
			UAVParam.lastWPUTM[numUAV] = home.getUTM();
		} else if (lastWP.getCommand().value() == MissionHelper.NAV_LAND_COMMAND) {
			if (lastWP.getLatitude() == 0 && lastWP.getLongitude() == 0) {
				Waypoint prevLastWP = UAVParam.currentGeoMission[numUAV].get(UAVParam.currentGeoMission[numUAV].size() - 2);
				UAVParam.lastWPUTM[numUAV] = prevLastWP.getUTM();
				// Be careful, usually, the UAV lands many meters before reaching that waypoint if the planned speed is high
			} else {
				UAVParam.lastWPUTM[numUAV] = lastWP.getUTM();
			}
		} else {
			UAVParam.lastWPUTM[numUAV] = lastWP.getUTM();
		}
	}
	
	/**
	 * Modify the current waypoint of the mission stored on the UAV.
	 * @return true if the command was successful.
	 */
	private boolean setCurrentWaypoint() {
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.newCurrentWaypoint[numUAV] = 0;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_CURRENT_WP);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT_ERROR);
			return false;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT + " = " + 0);
			return true;
		}
	}
	
	/**
	 * Send a new mission to the UAV.
	 * <p>The waypoint 0 of the mission should be the current coordinates retrieved from the controller, but it is ignored anyway.
	 * The waypoint 1 must be take off.
	 * The last waypoint can also be land or RTL.</p>
	 * @param list Mission to be sent to the flight controller.
	 * @return true if the command was successful.
	 */
	private boolean set(List<Waypoint> list) {
		if (list == null || list.size() < 2) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_1);
			return false;
		}
		// There is a minimum altitude to fly (waypoint 0 is home, and waypoint 1 is takeoff)
		if (list.get(1).getAltitude() < UAVParam.minAltitude) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_2 + "(" + UAVParam.minAltitude + " " + Text.METERS+ ").");
			return false;
		}
		int current = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isCurrent()) {
				current = i;
			}
		}
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		// Specify the current waypoint
		UAVParam.currentWaypoint.set(numUAV, current);
		UAVParam.newCurrentWaypoint[numUAV] = current;
		// Relative altitude calculus to take off
		UAVParam.takeOffAltitude.set(numUAV, list.get(1).getAltitude());

		UAVParam.currentGeoMission[numUAV].clear();
		for (Waypoint waypoint : list) {
			UAVParam.currentGeoMission[numUAV].add(new Waypoint(waypoint));
		}
		Location2DGeo geo = UAVParam.uavCurrentData[numUAV].getGeoLocation();
		// The take off waypoint must be modified to include current coordinates
		UAVParam.currentGeoMission[numUAV].get(1).setLatitude(geo.latitude);
		UAVParam.currentGeoMission[numUAV].get(1).setLongitude(geo.longitude);
		
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SEND_WPS);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_3);
			return false;
		} else {
			ArduSimTools.logVerboseGlobal(SimParam.prefix[numUAV] + Text.MISSION_SENT);
			return true;
		}
	}
	
	/**
	 * Set the loaded missions from file/s for the UAVs, in geographic coordinates. Used in the configuration window or in the method <i>setStartingLocation</i> of the protocol implementation, when needed.
	 * <p>Please, check that the length of the array is the same as the number of running UAVs in the same machine (method <i>API.getArduSim.getNumUAVs()</i>).</p>
	 * @param missions Missions that must be loaded and set with this method in the protocol configuration window, when needed.
	 */
	public void setMissionsLoaded(List<Waypoint>[] missions) {
		UAVParam.missionGeoLoaded = missions;
	}
	
	/**
	 * Add a Class as listener for the event: waypoint reached (more than one can be set).
	 * @param listener Class to be added as listener for the event.
	 */
	public void setWaypointReachedListener(WaypointReachedListener listener) {
		ArduSimTools.listeners.add(listener);
	}
	
	/**
	 * Take off a UAV and start the planned mission.
	 * <p>Issues three commands: armEngines, setFlightMode --> AUTO, and stabilize.
	 * Previously, on a real UAV you have to press the hardware switch for safety arm, if available.
	 * The UAV must be on the ground and in an armable flight mode (STABILIZE, LOITER, ALT_HOLD, GUIDED).</p>
	 * @return true if all the commands were successful.
	 */
	public boolean start() {
		// Documentation says: While on the ground, 1st arm, 2nd auto mode, 3rd some throttle, and the mission begins
		// Since ardupilot v4.0.0 this doesn`t work anymore. however everything works if we first give some throttle and than go to auto mode 
		
		//    If the copter is flying, the take off waypoint will be considered to be completed, and the UAV goes to the next waypoint
		return HiddenFunctions.armEngines(numUAV)
				&& HiddenFunctions.stabilize(numUAV)
				&& this.copter.setFlightMode(FlightMode.AUTO);
	}
	
	/**
	 * Delete the current mission of the UAV, sends a new one, and gets it to be shown on the GUI.
	 * <p>Blocking method.</p>
	 * <p>Method automatically used by ArduSim on start to send available missions to the UAVs.
	 * Must be used only if the UAV must follow a mission.</p>
	 * @param mission Mission to be sent to the flight controller.
	 * @return true if all the commands were successful.
	 */
	public boolean updateUAV(List<Waypoint> mission) {
		boolean success = false;
		if (this.remove()
				&& this.set(mission)
				&& this.retrieve()
				&& this.setCurrentWaypoint()) {
			Param.numMissionUAVs.incrementAndGet();
			success = true;
		}
		return success;
	}
	
}
