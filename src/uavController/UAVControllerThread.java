package uavController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

import org.mavlink.IMAVLinkMessage;
import org.mavlink.MAVLinkReader;
import org.mavlink.messages.IMAVLinkMessageID;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_AUTOPILOT;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_COMPONENT;
import org.mavlink.messages.MAV_FRAME;
import org.mavlink.messages.MAV_MISSION_RESULT;
import org.mavlink.messages.MAV_RESULT;
import org.mavlink.messages.MAV_STATE;
import org.mavlink.messages.MAV_TYPE;
import org.mavlink.messages.ardupilotmega.msg_command_ack;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_global_position_int;
import org.mavlink.messages.ardupilotmega.msg_heartbeat;
import org.mavlink.messages.ardupilotmega.msg_mission_ack;
import org.mavlink.messages.ardupilotmega.msg_mission_clear_all;
import org.mavlink.messages.ardupilotmega.msg_mission_count;
import org.mavlink.messages.ardupilotmega.msg_mission_current;
import org.mavlink.messages.ardupilotmega.msg_mission_item;
import org.mavlink.messages.ardupilotmega.msg_mission_item_reached;
import org.mavlink.messages.ardupilotmega.msg_mission_request;
import org.mavlink.messages.ardupilotmega.msg_mission_request_list;
import org.mavlink.messages.ardupilotmega.msg_mission_set_current;
import org.mavlink.messages.ardupilotmega.msg_param_request_list;
import org.mavlink.messages.ardupilotmega.msg_param_request_read;
import org.mavlink.messages.ardupilotmega.msg_param_set;
import org.mavlink.messages.ardupilotmega.msg_param_value;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_override;
import org.mavlink.messages.ardupilotmega.msg_set_mode;
import org.mavlink.messages.ardupilotmega.msg_set_position_target_global_int;
import org.mavlink.messages.ardupilotmega.msg_statustext;
import org.mavlink.messages.ardupilotmega.msg_sys_status;

import com.fazecast.jSerialComm.SerialPort;

import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.Location2D;
import api.pojo.LogPoint;
import api.pojo.MAVParam;
import api.pojo.Point3D;
import api.pojo.RCValues;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import main.ArduSimTools;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.logic.SimParam;
import sim.logic.SimTools;

/** This class implements the communication with the UAV, whether it is real or simulated.
 * <p>The thread applies the communications finite state machine needed to support the MAVLink protocol.
 * If you need to send a new type of message to the flight controller, please contact with the original developer (it would be easier).</p>
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class UAVControllerThread extends Thread {
	
	private int numUAV;			// Id of the UAV
	private boolean isStarting; // Used to wait until the first valid coordinates set is received
	private int numTests;		// It allows to check several times if the waypoint has been sent well.
								// Several updates of the previous waypoint can be received after the current one

	/** Connection parameters */
	private Socket socket;
	private int port;
	private SerialPort serialPort;
	private DataInputStream din;
	private DataOutputStream dout;
	private MAVLinkReader reader = null;
	private MAVLinkMessage inMsg;
	
	/** Indicates when the UAV connects with the application (MAVLink ready). */
	private boolean uavConnected = false;
	
	/** Indicates when the UAV sends valid coordinates. */
	private int receivedGPSOnline = 0;
	private boolean locationReceived = false;
	
	@SuppressWarnings("unused")
	private UAVControllerThread() {}

	public UAVControllerThread(int numUAV) throws SocketException {
		this.numUAV = numUAV;
		this.isStarting = true;
		this.numTests = 0;

		// Connection through serial port on a real UAV
		if (Param.role == Tools.MULTICOPTER) {
			serialPort = SerialPort.getCommPort(UAVParam.serialPort);
//			comPort.setBaudRate(57600);
//			comPort.setNumDataBits(8);
//			comPort.setNumStopBits(1);
//			comPort.setParity(SerialPort.NO_PARITY);
			serialPort.setComPortParameters(UAVParam.baudRate, 8, 1, SerialPort.NO_PARITY);
			serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
			if (serialPort.openPort()) {
				din = new DataInputStream(serialPort.getInputStream());
				dout = new DataOutputStream(serialPort.getOutputStream());
				reader = new MAVLinkReader(din, IMAVLinkMessage.MAVPROT_PACKET_START_V10);
			}
			if (reader == null) {
				GUI.log(Text.SERIAL_ERROR);
				System.exit(1);
			}
		} else if (Param.role == Tools.SIMULATOR) {
			// Connection through TCP in the simulator
			this.port = UAVParam.mavPort[numUAV];
			try {
				socket = new Socket(UAVParam.MAV_NETWORK_IP, this.port);
				socket.setTcpNoDelay(true);
				din = new DataInputStream(socket.getInputStream());
				dout = new DataOutputStream(socket.getOutputStream());
				reader = new MAVLinkReader(din, IMAVLinkMessage.MAVPROT_PACKET_START_V10);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (reader == null) {
				GUI.exit(Text.TCP_ERROR);
			}
		}
	}

	@Override
	public void run() {
		// Periodically sending a heartbeat to keep control over the UAV, while reading received MAVLink messages
		long prevTime = System.nanoTime();
		long posTime;
		
//		long ini = System.currentTimeMillis();
		
		while (true) {
			inMsg = null;
			try {
				inMsg = reader.getNextMessage();
				if (inMsg != null) {
					
//					switch (inMsg.messageType) {
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_AHRS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_AHRS2:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_AHRS3:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_ATTITUDE:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_RC_CHANNELS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_VIBRATION:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_EKF_STATUS_REPORT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SYSTEM_TIME:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_HWSTATUS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_VFR_HUD:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_FENCE_STATUS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_GPS_RAW_INT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_CURRENT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MEMINFO:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_POWER_STATUS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SCALED_PRESSURE:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SCALED_IMU2:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_RAW_IMU:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SYS_STATUS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_HEARTBEAT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SENSOR_OFFSETS:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_PARAM_VALUE:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_COMMAND_ACK:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ACK:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_LOCAL_POSITION_NED:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_TERRAIN_REPORT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_TERRAIN_REQUEST:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_HOME_POSITION:
////						System.out.println(inMsg.toString());
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_REQUEST:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_COUNT:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ITEM:
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ITEM_REACHED:
//						break;
//					default:
//						System.out.println(inMsg.toString());
//					}
					
					// Identify and process the received message
					identifyMessage();
					
					// When all UAVs are connected, start to process commands and sending heartbeat
					if (UAVParam.numMAVLinksOnline.get() == Param.numUAVs) {
						posTime = System.nanoTime();
						if (posTime - prevTime > UAVParam.HEARTBEAT_PERIOD) {
							msgHeartBeat();
							prevTime = posTime;
						}
						processCommand();
					}
				}
			} catch (IOException e) {
			}
		}
	}

	/** Message identification to process it. */
	private void identifyMessage() {
		switch (inMsg.messageType) {
		case IMAVLinkMessageID.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
			processGlobalLocation();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_HEARTBEAT:
			processMode();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_STATUSTEXT:
			if (numUAV == 0) {
				processVersion();
			}
			if (!this.locationReceived) {
				processGPSFix();
			}
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_PARAM_VALUE:
			processParam();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_CURRENT:
			processCurrentWaypointACK();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ITEM_REACHED:
			processWaypointReached();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_COUNT:
			processWaypointCount();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ITEM:
			processWaypointReceived();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_REQUEST:
			processWaypointRequest();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_MISSION_ACK:
			processMissionAck();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_COMMAND_ACK:
			processCommandAck();
			break;
		case IMAVLinkMessageID.MAVLINK_MSG_ID_SYS_STATUS:
			processStatus();
			break;
		default:
			break;
		}
	}

	/** Process the received current location, speed and UAV orientation (heading). */
	private void processGlobalLocation() {
		msg_global_position_int message = (msg_global_position_int) inMsg;
		// Only process position when the GPS sends valid Mercator coordinates (non zero latitude and longitude)
		if (!this.isStarting || (message.lat != 0 && message.lon != 0)) {
			isStarting = false;
			if (!this.locationReceived && this.receivedGPSOnline == 2) {
				UAVParam.numGPSFixed.incrementAndGet();
				this.locationReceived = true;
			}

			long time = System.nanoTime();
			Location2D location = Location2D.NewLocation(message.lat * 0.0000001, message.lon * 0.0000001);
			UTMCoordinates locationUTM = location.getUTMLocation();
			
			// The last few UAV positions are stored for later use in protocols
			UAVParam.lastUTMLocations[numUAV].add(locationUTM);
			// Global horizontal speed estimation
			double hSpeed = Math.sqrt(Math.pow(message.vx * 0.01, 2) + Math.pow(message.vy * 0.01, 2));
			double[] speed = new double[] {message.vx * 0.01, message.vy * 0.01, message.vz * 0.01};
			// Update the UAV data, including the acceleration calculus
			double z = message.alt * 0.001;
			double zRel = message.relative_alt * 0.001;
			double heading = (message.hdg * 0.01) * Math.PI / 180;
			UAVParam.uavCurrentData[numUAV].update(time, location, z, zRel, speed, hSpeed, heading);
			// Send location to GUI to draw the UAV path and to log data
			SimulatorState currentState = Param.simStatus;
			int state = currentState.getStateId();
			// Differentiate if this specific UAV has finished the experiment
			if (currentState == SimulatorState.TEST_IN_PROGRESS	&& Param.testEndTime[numUAV] != 0) {
				state = SimulatorState.TEST_FINISHED.getStateId();
			}
			SimParam.uavUTMPathReceiving[numUAV].offer(new LogPoint(time, locationUTM.x, locationUTM.y, z, zRel, heading, hSpeed,
					state)); // UAV under test
		}
	}

	/** Process a received flight mode and assert that the MAVLink link has been established. */
	private void processMode() {
		msg_heartbeat message = (msg_heartbeat) inMsg;
		FlightMode prevMode = UAVParam.flightMode.get(numUAV);
		// Only process valid mode
		if (message.base_mode != 0 || message.custom_mode != 0) {
			// Only process when mode changes
			if (prevMode == null || prevMode.getBaseMode() != message.base_mode || prevMode.getCustomMode() != message.custom_mode) {
				FlightMode mode = FlightMode.getMode(message.base_mode, message.custom_mode);
				if (mode != null) {
					UAVParam.flightMode.set(numUAV, mode);
					SimTools.updateUAVMAVMode(numUAV, mode.getMode());	// Update the progress dialog
					if (mode.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
						UAVParam.flightStarted.set(numUAV, 1);
					}
				} else {
					GUI.log(SimParam.prefix[numUAV] + Text.FLIGHT_MODE_ERROR_2 + "(" + message.base_mode + "," + message.custom_mode + ")");
				}
			}
			
			// Detect when the UAV has connected (first heartbeat received)
			if (!this.uavConnected) {
				UAVParam.mavId.set(numUAV, message.sysId);
				UAVParam.numMAVLinksOnline.incrementAndGet();
				this.uavConnected = true;
			}
		}
	}
	
	/** Process the detection of the ArduCopter version. */
	private void processVersion() {
		msg_statustext message = (msg_statustext) inMsg;
		String text = message.getText();
		if (text.toUpperCase().startsWith("APM:COPTER")) {
			UAVParam.arducopterVersion.set(text.split(" ")[1].substring(1));
		}
	}

	/** Process the detection of the GPS fix. */
	private void processGPSFix() {
		// Both IMU0 and IMU1 must confirm that they are using GPS, and valid coordinates must be received
		if (this.receivedGPSOnline < 2) {
			msg_statustext message = (msg_statustext) inMsg;
			String text = message.getText();
			if (text.contains("EKF") && text.contains("IMU")
					&& text.contains("using GPS")) {
				this.receivedGPSOnline++;
			}
		}
		if (this.receivedGPSOnline == 2 && UAVParam.uavCurrentData[numUAV].getGeoLocation() != null) {
			UAVParam.numGPSFixed.incrementAndGet();
			this.locationReceived = true;
		}
	}

	/** Process a received param value. */
	private void processParam() {
		msg_param_value message = (msg_param_value) inMsg;
		String paramId = message.getParam_id();
		// General storage of parameters
		UAVParam.loadedParams[numUAV].put(paramId, new MAVParam(paramId, message.param_value, message.param_type));
		UAVParam.lastParamReceivedTime[numUAV].set(System.currentTimeMillis());
		
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ACK_PARAM
				&& paramId.equals(UAVParam.newParam[numUAV].getId())) {
			// A parameter has been modified
			if (message.param_value == UAVParam.newParamValue.get(numUAV)) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_1_PARAM);
			}
		} else if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_WAIT_FOR_PARAM
				&& paramId.equals(UAVParam.newParam[numUAV].getId())) {
			// A single param has been read
			UAVParam.newParamValue.set(numUAV, message.param_value);
			UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
		}
	}

	/** Process the ACK when changing the current target waypoint. */
	private void processCurrentWaypointACK() {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ACK_CURRENT_WP) {
			msg_mission_current message = (msg_mission_current) inMsg;
			if (message.seq == UAVParam.newCurrentWaypoint[numUAV]) {
				UAVParam.currentWaypoint.set(numUAV, UAVParam.newCurrentWaypoint[numUAV]);
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				numTests = 0;
			} else {
				if (numTests < UAVParam.MAX_CURRENT_WP_CHECKS) {
					numTests++;
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CURRENT_WP);
					numTests = 0;
				}
			}
		}
	}

	/** Detect when the UAV reaches the next waypoint. */
	private void processWaypointReached() {
		msg_mission_item_reached message = (msg_mission_item_reached) inMsg;
		// The received value begins in 0
		UAVParam.currentWaypoint.set(numUAV, message.seq);
		GUI.log(SimParam.prefix[numUAV] + Text.WAYPOINT_REACHED + " = " + message.seq);
		
		ArduSimTools.triggerWaypointReached(numUAV, message.seq);
	}

	/** Process the received message that gets the number of waypoints included in the UAV mission. */
	private void processWaypointCount() {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_REQUEST_WP0) {
			msg_mission_count message = (msg_mission_count) inMsg;
			UAVParam.newGeoMission[numUAV] = new Waypoint[message.count];
			if (message.count == 0) {
				UAVParam.currentGeoMission[numUAV].clear();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				try {
					msgGetWaypoint(0);
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WPS);
				} catch (IOException e) {
					e.printStackTrace();
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST);
				}
			}
		}
	}

	/** Process the received message that contains a waypoint. */ 
	private void processWaypointReceived() {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_REQUEST_WPS) {
			msg_mission_item message = (msg_mission_item) inMsg;
			if (message.seq >= 0 && message.seq < UAVParam.newGeoMission[numUAV].length) {
				Waypoint wp = new Waypoint(message.seq, message.current != 0, message.frame,
						message.command, message.param1, message.param2, message.param3,
						message.param4, message.x, message.y, message.z, message.autocontinue);
				UAVParam.newGeoMission[numUAV][message.seq] = wp;
				boolean allReceived = true;
				int i = 0;
				while (i < UAVParam.newGeoMission[numUAV].length && allReceived) {
					if (UAVParam.newGeoMission[numUAV][i] == null) {
						allReceived = false;
					} else {
						i++;
					}
				}
				if (allReceived) {
					try {
						msgSendMissionAck();
						UAVParam.currentGeoMission[numUAV].clear();
						UAVParam.currentGeoMission[numUAV].addAll(Arrays.asList(UAVParam.newGeoMission[numUAV]));
						UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
					} catch (IOException e) {
						e.printStackTrace();
						UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST);
					}
				} else {
					// Asking the flight controller for the missing waypoint
					try {
						// We asume that the waypoint request doesn't fail
						msgGetWaypoint(i);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				GUI.log(SimParam.prefix[numUAV] + Text.IMPOSSIBLE_WAYPOINT_ERROR);
			}
		}
	}

	/** Process the request for a waypoint (sending waypoints to the flight controller). */
	private void processWaypointRequest() {
		msg_mission_request message = (msg_mission_request) inMsg;
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_SENDING_WPS && message.seq >= 0
				&& message.seq < UAVParam.currentGeoMission[numUAV].size()) {
			try {
				msgSendWaypoint(UAVParam.currentGeoMission[numUAV].get(message.seq));
			} catch (IOException e) {
				// If the dispatch fails, it is expected that the flight controller will request it again
			}
		}
	}

	/** Process a mission ACK. */
	private void processMissionAck() {
		msg_mission_ack message = (msg_mission_ack) inMsg;
		switch (UAVParam.MAVStatus.get(numUAV)) {
		// ACK received when removing the current mission
		case UAVParam.MAV_STATUS_ACK_CLEAR_WP_LIST:
			if (message.type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST);
			}
			break;
		// ACK received when sending a waypoint
		case UAVParam.MAV_STATUS_SENDING_WPS:
			if (message.type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SENDING_WPS);
			}
			break;
		// ACK received when moving a UAV to another location
		case UAVParam.MAV_STATUS_ACK_MOVE_UAV:
			if (message.type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV_ERROR);
			}
			break;
		//default:
			// We must not print this, as sometimes the flight controller sends more than one ACK for the mission
			//SimTools.println(Text.NOT_REQUESTED_ACK_ERROR + " " + message.toString());
		}
	}

	/** Process a new command. */
	private void processCommand() {
		
		RCValues values = UAVParam.rcs[numUAV].getAndSet(null);
		if (values != null) {
			try {
				msgrcChannelsOverride(values.roll, values.pitch, values.throttle, values.yaw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Point3D target = UAVParam.target[numUAV].getAndSet(null);
		if (target != null) {
			try {
				msgMoveToTarget(target.x, target.y, target.z);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		switch (UAVParam.MAVStatus.get(numUAV)) {
		// Command to take control of the UAV using (half throttle)
		case UAVParam.MAV_STATUS_THROTTLE_ON:
			try {
				msgSetThrottle(UAVParam.stabilizationThrottle[numUAV]);
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} catch (IOException e1) {
				e1.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_THROTTLE_ON_ERROR);
			}
			break;
		case UAVParam.MAV_STATUS_RECOVER_CONTROL:
			try {
				msgReturnRCControl();
				// Now the RC channels will not be overriden by ArduSim
				UAVParam.overrideOn.set(numUAV, 0);
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} catch (IOException e1) {
				e1.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_RECOVER_ERROR);
			}
			break;
		// Command to move the UAV to a safe position stored in the variable UAVParam.newLocation[numUAV]
		case UAVParam.MAV_STATUS_MOVE_UAV:
			try {
				msgMoveUAV();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_MOVE_UAV);
			} catch (IOException e1) {
				e1.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV_ERROR);
			}
			break;
		// Command to change the flight mode to the one stored in the variable UAVParam.newFlightMode[numUAV]
		case UAVParam.MAV_STATUS_REQUEST_MODE:
			FlightMode mode = UAVParam.flightMode.get(numUAV);
			if (mode.getBaseMode() != UAVParam.newFlightMode[numUAV].getBaseMode()
			|| mode.getCustomMode() != UAVParam.newFlightMode[numUAV]
					.getCustomMode()) {
				try {
					msgSetMode();
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_MODE);
				} catch (IOException e) {
					e.printStackTrace();
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_MODE);
				}
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			}
			break;
		// Command to arm the UAV engines
		case UAVParam.MAV_STATUS_REQUEST_ARM:
			try {
				msgArmEngines();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_ARM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ARM);
			}
			break;
		// Command to take off the UAV to the absolute altitude stored in the variable UAVParam.takeOffAltitude[numUAV]
		case UAVParam.MAV_STATUS_REQUEST_TAKE_OFF:
			
			try {
				msgDoTakeOff();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_TAKE_OFF);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_TAKE_OFF);
			}
			break;
		// Command to set the flight speed stored in the variable UAVParam.newSpeed[numUAV]
		case UAVParam.MAV_STATUS_SET_SPEED:
			try {
				msgSetSpeed();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_SET_SPEED);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SET_SPEED);
			}
			break;
		// Command to change the value UAVParam.newParamValue[numUAV] of the parameter UAVParam.newParam[numUAV]
		case UAVParam.MAV_STATUS_SET_PARAM:
			try {
				msgSetParam();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_1_PARAM);
			}
			break;
		// Command to store the value of the parameterUAVParam.newParam[numUAV] on the variable UAVParam.newParamValue[numUAV]
		case UAVParam.MAV_STATUS_GET_PARAM:
			try {
				msgGetParam();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_WAIT_FOR_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_2_PARAM);
			}
			
			break;
		// Command to change the current waypoint to the one stored in the variable UAVParam.newCurrentWaypoint[numUAV]
		case UAVParam.MAV_STATUS_SET_CURRENT_WP:
			try {
				msgSetCurrentWaypoint();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_CURRENT_WP);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CURRENT_WP);
			}
			break;
		// Command to clear the mission stored in the UAV
		case UAVParam.MAV_STATUS_CLEAR_WP_LIST:
			try {
				msgClearMission();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_CLEAR_WP_LIST);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST);
			}
			break;
		// Command to send the number of waypoints of a new mission stored in UAVParam.currentGeoMission[numUAV]
		case UAVParam.MAV_STATUS_SEND_WPS:
			try {
				msgSendMissionSize();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SENDING_WPS);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SENDING_WPS);
			}
			break;
		// Command to request the number of waypoints of the mission stored in the UAV
		case UAVParam.MAV_STATUS_REQUEST_WP_LIST:
			try {
				msgGetMission();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WP0);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST);
			}
			break;
		case UAVParam.MAV_STATUS_REQUEST_ALL_PARAM:
			try {
				msgRequestAllParam();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_TIMEOUT_ALL_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ALL_PARAM);
			}
			break;
		}
		
	}

	/** Process a received command ACK. */
	private void processCommandAck() {
		msg_command_ack message = (msg_command_ack) inMsg;
		switch (UAVParam.MAVStatus.get(numUAV)) {
		// ACK received when changing the flight mode
		case UAVParam.MAV_STATUS_ACK_MODE:
			if (message.command == msg_set_mode.MAVLINK_MSG_ID_SET_MODE) {
				if (message.result == MAV_RESULT.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_MODE);
				}
			}
			break;
		// ACK received when arming motors
		case UAVParam.MAV_STATUS_ACK_ARM:
			if (message.command == MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM) {
				if (message.result == MAV_RESULT.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ARM);
				}
			}
			break;
		// ACK received when taking off
		case UAVParam.MAV_STATUS_ACK_TAKE_OFF:
			if (message.command == MAV_CMD.MAV_CMD_NAV_TAKEOFF) {
				if (message.result == MAV_RESULT.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_TAKE_OFF);
				}
			}
			break;
		// ACK received when changing the horizontal flight speed
		case UAVParam.MAV_STATUS_ACK_SET_SPEED:
			if (message.command == MAV_CMD.MAV_CMD_DO_CHANGE_SPEED) {
				if (message.result == MAV_RESULT.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SET_SPEED);
				}
			}
			break;
		default:
			GUI.log(Text.NOT_REQUESTED_ACK_ERROR
					+ " (command=" + message.command + ", result=" + message.result + ")");
		}
	}
	
	/** Process battery information received. */
	private void processStatus() {
		msg_sys_status message = (msg_sys_status) inMsg;
		UAVParam.uavCurrentStatus[numUAV].update(message.voltage_battery, message.current_battery,
				message.battery_remaining, message.load);
	}

	/** Sending heartbeat message. */
	private void msgHeartBeat() throws IOException {
		msg_heartbeat message = new msg_heartbeat();
		message.type = MAV_TYPE.MAV_TYPE_GCS;
		message.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
		message.system_status = MAV_STATE.MAV_STATE_UNINIT;
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Sending new flight mode message. */
	private void msgSetMode() throws IOException {
		// Currently, it should be better to use a msg_command_long, but SITL does not support it on MAVLink v1 and this solution depends on the compilation (ardupilot, PX4,...)
//		msg_command_long message = new msg_command_long();
//		message.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
//		message.sysId = 255;
//		message.componentId = MAV_COMPONENT.MAV_COMP_ID_MISSIONPLANNER;
//		message.target_system = 1;
//		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
//		
//		message.param1 = MAV_MODE.MAV_MODE_AUTO_ARMED;
//		message.param2 = UAVParam.newFlightMode[numUAV].getBaseMode();
//		message.param3 = UAVParam.newFlightMode[numUAV].getCustomMode();
		msg_set_mode message = new msg_set_mode();
		message.base_mode = UAVParam.newFlightMode[numUAV].getBaseMode();
		message.custom_mode = UAVParam.newFlightMode[numUAV].getCustomMode();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		this.sendMessage(message.encode());
	}

	/** Sending arm engines message. */
	private void msgArmEngines() throws IOException {
		msg_command_long message = new msg_command_long();
		message.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
		message.confirmation = 0;
		message.param1 = 1;
		//message.param2 = 21196; //Use ONLY to disarm the vehicle in flight
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending take off message. */
	private void msgDoTakeOff() throws IOException {
		msg_command_long message = new msg_command_long();
		message.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
		message.confirmation = 0;
		//message.param1 = 0; // Ignored: climb angle on planes
		message.param7 = (float)UAVParam.takeOffAltitude.get(numUAV);
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending new speed message. */
	private void msgSetSpeed() throws IOException {
		msg_command_long message = new msg_command_long();
		message.command = MAV_CMD.MAV_CMD_DO_CHANGE_SPEED;
		message.confirmation = 0;
		//message.param1 = 1; // Ignored: 1 Means ground speed, 0 air speed
		message.param2 = (float)UAVParam.newSpeed[numUAV];
		//message.param3 = -1; // Ignored: Throttle (0-100%). -1 means ignore
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Use yaw in degrees (0-360). */
	private void msgYaw(float yaw) throws IOException {
		msg_command_long message = new msg_command_long();
		message.command = MAV_CMD.MAV_CMD_CONDITION_YAW;// TODO probando, borrar el método
		message.confirmation = 0;
		message.param1 = yaw;	// [0-360] Angle in degress
		//message.param2 = ;	//
		//message.param3 = ;
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Sends the UAV at a specific speed (m/s).
	 * <p>speed. x=North, y=East, z=Down.</p> */
	private void msgMoveSpeed(Double speedX, Double speedY, Double speedZ) throws IOException {
		msg_set_position_target_global_int message = new msg_set_position_target_global_int();
		message.time_boot_ms = System.currentTimeMillis();
		message.coordinate_frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;// TODO method to be adapted in Copter Class
//		int bX = 1;// bit 0// they would be concatenated with logic or: bX | bY | bZ
//		int bY = 2;
//		int bZ = 4;
//		int bVx = 8;// bit 3
//		int bVy = 16;
//		int bVz = 32;
//		int bAx = 64;// bit 6	// Not supported in ArduPilot
//		int bAy = 128;
//		int bAz = 256;
//		int bForceA = 512;// bit 9
//		int bYaw = 1024;
//		int bYawRate = 2048;
		message.type_mask = 56;
		message.type_mask = ~message.type_mask & 0xFFFF; // 0=use, 1=ignore, so we need to flip bits
		message.vx= speedX.floatValue();
		message.vy= speedY.floatValue();
		message.vz= speedZ.floatValue();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;//se transmite *10^7
		this.sendMessage(message.encode());
	}
	
	/** Sends the UAV to a specific location.
	 * <p>You can set the three coordinates or the three speeds, but you can not combine them or leave them incomplete.</p> */
	private void msgMoveToTarget(double longitude, double latitude, Double altitude) throws IOException {
		// A possible alternative to this function is moveUAVNonBlocking
		msg_set_position_target_global_int message = new msg_set_position_target_global_int();
		message.time_boot_ms = System.currentTimeMillis();
		message.coordinate_frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
//		int bX = 1;// bit 0// they would be concatenated with logic or: bX | bY | bZ
//		int bY = 2;
//		int bZ = 4;
//		int bVx = 8;// bit 3
//		int bVy = 16;
//		int bVz = 32;
//		int bAx = 64;// bit 6	// Not supported in ArduPilot
//		int bAy = 128;
//		int bAz = 256;
//		int bForceA = 512;// bit 9
//		int bYaw = 1024;
//		int bYawRate = 2048;
		message.type_mask = 7;
		message.type_mask = ~message.type_mask & 0xFFFF; // 0=use, 1=ignore, so we need to flip bits (~ flips bits)
		message.lat_int = Math.round(10000000l * latitude);	// Degrees
		message.lon_int = Math.round(10000000l * longitude);
		message.alt = altitude.floatValue();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;//se transmite *10^7
		this.sendMessage(message.encode());
	}

	/** Sending new parameter value message. */
	private void msgSetParam() throws IOException {
		msg_param_set message = new msg_param_set();
		message.setParam_id(UAVParam.newParam[numUAV].getId());
		message.param_value = (float)UAVParam.newParamValue.get(numUAV);
		message.param_type = UAVParam.newParam[numUAV].getType();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Sending a query message for a parameter. */
	private void msgGetParam() throws IOException {
		msg_param_request_read  message = new msg_param_request_read();
		message.param_index = -1;	// Needs to be -1 to use the param id field
		message.setParam_id(UAVParam.newParam[numUAV].getId());
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending new current waypoint message.
	 * <p>Starts with 0.</p> */
	private void msgSetCurrentWaypoint() throws IOException {
		msg_mission_set_current message = new msg_mission_set_current();
		message.seq = UAVParam.newCurrentWaypoint[numUAV];
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending clear mission message. */
	private void msgClearMission() throws IOException {
		msg_mission_clear_all message = new msg_mission_clear_all();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a message with the number of waypoints of the new mission. */
	private void msgSendMissionSize() throws IOException {
		msg_mission_count message = new msg_mission_count();
		message.count = UAVParam.currentGeoMission[numUAV].size();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a message with a mission waypoint to the UAV. */
	private void msgSendWaypoint(Waypoint wp) throws IOException {
		msg_mission_item message = wp.getMessage();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a request message for the number of waypoints of the mission stored in the UAV. */
	private void msgGetMission() throws IOException {
		msg_mission_request_list message = new msg_mission_request_list();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a query message for a waypoint stored in the UAV. */
	private void msgGetWaypoint(int numWP) throws IOException {
		msg_mission_request message = new msg_mission_request();
		message.seq = numWP;
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending the final ACK when all the waypoints of a mission are received. */
	private void msgSendMissionAck() throws IOException {
		msg_mission_ack message = new msg_mission_ack();
		message.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a throttle value message. */
	private void msgSetThrottle(int throttle) throws IOException {
		msg_rc_channels_override message = new msg_rc_channels_override();
		// Initially, only trim values
		int[] values = new int[] {UAVParam.RCtrimValue[numUAV].get(0), UAVParam.RCtrimValue[numUAV].get(1),
				UAVParam.RCtrimValue[numUAV].get(2), UAVParam.RCtrimValue[numUAV].get(3),
				UAVParam.RCtrimValue[numUAV].get(4), UAVParam.RCtrimValue[numUAV].get(5),
				UAVParam.RCtrimValue[numUAV].get(6), UAVParam.RCtrimValue[numUAV].get(7)};
		// New throttle value
		values[UAVParam.RCmapThrottle.get(numUAV)-1] = throttle;
		// Flight mode
		int fltmode = UAVParam.customModeToFlightModeMap[numUAV][UAVParam.flightMode.get(numUAV).getCustomMode()];
		if (fltmode != -1) {
			values[4] = UAVParam.RC5_MODE_LEVEL[fltmode - 1][1];
		}
		message.chan1_raw = values[0];
		message.chan2_raw = values[1];
		message.chan3_raw = values[2];
		message.chan4_raw = values[3];
		message.chan5_raw = values[4];
		message.chan6_raw = values[5];
		message.chan7_raw = values[6];
		message.chan8_raw = values[7];
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Sending 0 to all RCs returns control to the remote controler. */
	private void msgReturnRCControl() throws IOException {
		msg_rc_channels_override message = new msg_rc_channels_override();
		message.chan1_raw = 0;
		message.chan2_raw = 0;
		message.chan3_raw = 0;
		message.chan4_raw = 0;
		message.chan5_raw = 0;
		message.chan6_raw = 0;
		message.chan7_raw = 0;
		message.chan8_raw = 0;
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Restricted method for API usage. Please, don't use it.
	 * <p>Value 0 returns control to the RC. UINT16_MAX avoids changing that channel.</p> */
	private void msgrcChannelsOverride(int roll, int pitch, int throttle, int yaw) throws IOException {
		msg_rc_channels_override message = new msg_rc_channels_override();
		// Initially, only trim values
		int[] values = new int[] {UAVParam.RCtrimValue[numUAV].get(0), UAVParam.RCtrimValue[numUAV].get(1),
				UAVParam.RCtrimValue[numUAV].get(2), UAVParam.RCtrimValue[numUAV].get(3),
				UAVParam.RCtrimValue[numUAV].get(4), UAVParam.RCtrimValue[numUAV].get(5),
				UAVParam.RCtrimValue[numUAV].get(6), UAVParam.RCtrimValue[numUAV].get(7)};
		// New roll, pitch, throttle, and yaw values
		values[UAVParam.RCmapRoll.get(numUAV)-1] = roll;
		values[UAVParam.RCmapPitch.get(numUAV)-1] = pitch;
		values[UAVParam.RCmapThrottle.get(numUAV)-1] = throttle;
		values[UAVParam.RCmapYaw.get(numUAV)-1] = yaw;
		// Flight mode
		int fltmode = UAVParam.customModeToFlightModeMap[numUAV][UAVParam.flightMode.get(numUAV).getCustomMode()];
		if (fltmode != -1) {
			values[4] = UAVParam.RC5_MODE_LEVEL[fltmode - 1][1];
		}
		message.chan1_raw = values[0];
		message.chan2_raw = values[1];
		message.chan3_raw = values[2];
		message.chan4_raw = values[3];
		message.chan5_raw = values[4];
		message.chan6_raw = values[5];
		message.chan7_raw = values[6];
		message.chan8_raw = values[7];
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Sending a message to move the UAV to a safe position in guided flight mode. */
	private void msgMoveUAV() throws IOException {
		msg_mission_item message = new msg_mission_item();
		message.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
		message.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
		message.current = 2; // Means that the command is issued in guided mode
		message.autocontinue = 0;
		message.param1 = 0; // Delay in decimal seconds before going to next waypoint
		message.param2 = 0; // Ignored
		message.param3 = 0; // Ignored
		message.param4 = 0; // Ignored
		message.x = UAVParam.newLocation[numUAV][0];
		message.y = UAVParam.newLocation[numUAV][1];
		message.z = UAVParam.newLocation[numUAV][2];
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}
	
	/** Sending a message to request all the ArduCopter parameters, and the compilation version. */
	private void msgRequestAllParam() throws IOException {
		msg_param_request_list message = new msg_param_request_list();
		
		message.sysId = UAVParam.gcsId.get(numUAV);
		message.componentId = MAV_COMPONENT.MAV_COMP_ID_ALL;
		message.target_system = UAVParam.mavId.get(numUAV);
		message.target_component = MAV_COMPONENT.MAV_COMP_ID_ALL;
		this.sendMessage(message.encode());
	}

	/** Auxiliary method to send to the UAV any of the previous messages. */
	private void sendMessage(byte[] b) throws IOException {
		dout.write(b);
		dout.flush();
	}
	
}
