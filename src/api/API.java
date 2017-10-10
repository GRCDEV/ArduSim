package api;

import java.awt.geom.Point2D;
import java.util.List;

import api.pojo.GeoCoordinates;
import api.pojo.Waypoint;
import main.Text;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;

/** This class contains exclusively static methods to control the UAV "numUAV" in the arrays included in the application. */

public class API {

	/** API: Sets a new value for a controller or SITL parameter. */
	public static boolean setParam(int numUAV, ControllerParam parameter, double value) {
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.newParamValue[numUAV] = value;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_1_PARAM) {
			SimTools.println(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_1 + " " + parameter.getId() + ".");
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.PARAMETER_1 + " " + parameter.getId() + " = " + value);
			return true;
		}
	}
	
	/** API: Gets the value of a controller or SITL parameter.
	 * <p>New value available on UAVParam.newParamValue[numUAV]. */
	public static boolean getParam(int numUAV, ControllerParam parameter) {
		UAVParam.newParam[numUAV] = parameter;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_GET_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_2_PARAM) {
			SimTools.println(SimParam.prefix[numUAV] + Text.PARAMETER_ERROR_2 + " " + parameter.getId() + ".");
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.PARAMETER_2 + " " + parameter.getId() + " = " + UAVParam.newParamValue[numUAV]);
			return true;
		}
	}

	/** API: Changes the UAV flight mode. */
	public static boolean setMode(int numUAV, UAVParam.Mode mode) {
		UAVParam.newFlightMode[numUAV] = mode;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_MODE);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_MODE) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_MODE) {
			SimTools.println(SimParam.prefix[numUAV] + Text.FLIGHT_MODE_ERROR_1);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.FLIGHT_MODE + " = " + mode.getMode());
			return true;
		}
	}

	/** API: Arms the engines. */
	public static boolean armEngines(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_ARM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_ARM) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_ARM) {
			SimTools.println(SimParam.prefix[numUAV] + Text.ARM_ENGINES_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.ARM_ENGINES);
			return true;
		}
	}

	/** API: Takes off. */
	public static boolean doTakeOff(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_TAKE_OFF);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_TAKE_OFF) {
			SimTools.println(SimParam.prefix[numUAV] + Text.TAKE_OFF_ERROR_2);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.TAKE_OFF);
			return true;
		}
	}

	/** API: Changes the planned flight speed (m/s). */
	public static boolean setSpeed(int numUAV, double speed) {
		UAVParam.newSpeed[numUAV] = speed;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_SPEED);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SET_SPEED) {
			SimTools.println(SimParam.prefix[numUAV] + Text.SPEED_2_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.SPEED_2 + " = " + speed);
			return true;
		}
	}

	/** API: Modifies the current waypoint of the mission stored on the UAV. */
	public static boolean setCurrentWaypoint(int numUAV, int currentWP) {
		UAVParam.newCurrentWaypoint[numUAV] = currentWP;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SET_CURRENT_WP);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CURRENT_WP) {
			SimTools.println(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.CURRENT_WAYPOINT + " = " + currentWP);
			return true;
		}
	}

	/** API: Suspends temporally a mission, entering on loiter flight mode to force a fast stop.
	 * <p>This method already includes the "setThrottle" function. */
	public static boolean stopUAV(int numUAV) {
		if (API.setThrottle(numUAV) && setMode(numUAV, UAVParam.Mode.LOITER_ARMED)) {
			long time = System.nanoTime();
			while (UAVParam.uavCurrentData[numUAV].getSpeed() > UAVParam.STABILIZATION_SPEED) {
				GUIHelper.waiting(UAVParam.STABILIZATION_WAIT_TIME);
				if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
					SimTools.println(SimParam.prefix[numUAV] + Text.STOP_ERROR_1);
					return false;
				}
			}

			SimTools.println(SimParam.prefix[numUAV] + Text.STOP);
			return true;
		}
		SimTools.println(SimParam.prefix[numUAV] + Text.STOP_ERROR_2);
		return false;
	}

	/** API: Moves the throttle stick to half power using RC3.
	 * <p>Useful for starting auto flight when being on the ground, andto stabilize altitude when going out of auto mode. */
	public static boolean setThrottle(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_THROTTLE_ON);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_THROTTLE_ON_ERROR) {
			SimTools.println(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.STABILIZE_ALTITUDE);
			return true;
		}
	}

	/** API: Moves the UAV to a new position.
	 * <p>The UAV must be in guided mode. */
	public static boolean moveUAV(int numUAV, GeoCoordinates geo, float relAltitude) {
		UAVParam.newLocation[numUAV][0] = (float)geo.latitude;
		UAVParam.newLocation[numUAV][1] = (float)geo.longitude;
		UAVParam.newLocation[numUAV][2] = relAltitude;
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_MOVE_UAV_ERROR) {
			SimTools.println(SimParam.prefix[numUAV] + Text.MOVING_ERROR_1);
			return false;
		} else {
			// Once the command is issued, we have to wait until the UAV starts the movement
			long time = System.nanoTime();
			while (UAVParam.uavCurrentData[numUAV].getSpeed() < 4 * UAVParam.STABILIZATION_SPEED) {
				GUIHelper.waiting(UAVParam.STABILIZATION_WAIT_TIME);
				if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
					SimTools.println(SimParam.prefix[numUAV] + Text.MOVING_ERROR_2);
					return false;
				}
			}
			// Finally, we have to wait until the UAV stops
			while (UAVParam.uavCurrentData[numUAV].getSpeed() > UAVParam.STABILIZATION_SPEED) {
				GUIHelper.waiting(UAVParam.STABILIZATION_WAIT_TIME);
				if (System.nanoTime() - time > UAVParam.STABILIZATION_TIMEOUT) {
					SimTools.println(SimParam.prefix[numUAV] + Text.MOVING_ERROR_3);
					return false;
				}
			}
			return true;
		}
	}

	/** API: Removes the current mission from the UAV. */
	public static boolean clearMission(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_CLEAR_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST) {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_DELETE_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_DELETE);
			return true;
		}
	}

	/** API: Sends a new mission to the UAV.
	 * <p>The waypoint 0 must be the current coordinates retrieved from the controller.
	 * <p>The waypoint 1 must be take off.
	 * <p>The last waypoint can be land or RTL. */
	public static boolean sendMission(int numUAV, List<Waypoint> list) {
		if (list == null || list.size() < 2) {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_1);
			return false;
		}
		// There is a minimum altitude to fly (waypoint 0 is home)
		if (list.get(1).getAltitude() < UAVParam.MIN_FLYING_ALTITUDE) {
			SimTools
			.println(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_2 + "(" + UAVParam.MIN_FLYING_ALTITUDE + " " + Text.METERS+ ").");
			return false;
		}
		int current = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isCurrent()) {
				current = i;
			}
		}
		// Specify the current waypoint
		UAVParam.currentWaypoint.set(numUAV, current);
		UAVParam.newCurrentWaypoint[numUAV] = current;
		// Relative altitude calculus to take off
		UAVParam.takeOffAltitude[numUAV] = list.get(1).getAltitude();

		UAVParam.currentGeoMission[numUAV].clear();
		for (int i = 0; i < list.size(); i++) {
			UAVParam.currentGeoMission[numUAV].add(list.get(i).clone());
		}
		Point2D.Double p = UAVParam.uavCurrentData[numUAV].getUTMLocation();
		GeoCoordinates geo = GUIHelper.UTMToGeo(p.x, p.y);
		// The take off waypoint must be modified to include current coordinates
		UAVParam.currentGeoMission[numUAV].get(1).setLatitude(geo.latitude);
		UAVParam.currentGeoMission[numUAV].get(1).setLongitude(geo.longitude);
		
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SEND_WPS);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_SENT_ERROR_3);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_SENT);
			return true;
		}
	}

	/** API: Retrieves the mission stored on the UAV.
	 * <p>New value available on UAVParam.currentGeoMission[numUAV].
	 * <p>Simplified version of the mission in UTM coordinates available on UAVParam.missionUTMSimplified[numUAV]. */
	public static boolean getMission(int numUAV) {
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WP_LIST);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST) {
			GUIHelper.waiting(UAVParam.COMMAND_WAIT);
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_SENDING_WPS) {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_GET_ERROR);
			return false;
		} else {
			SimTools.println(SimParam.prefix[numUAV] + Text.MISSION_GET);
			return true;
		}
	}
}
