package com.uavController;

import com.api.API;
import com.api.pojo.FlightMode;
import com.api.pojo.RCValues;
import com.api.pojo.location.LogPoint;
import com.api.pojo.location.Waypoint;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import es.upv.grc.mapper.*;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.annotations.MavlinkMessageInfo;
import io.dronefleet.mavlink.common.*;
import io.dronefleet.mavlink.util.EnumValue;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;
import com.api.ArduSim;
import com.api.copter.CopterParamLoaded;
import com.api.ValidationTools;
import com.setup.sim.logic.SimParam;
import com.setup.sim.logic.SimTools;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** This class implements the communication with the UAV, whether it is real or simulated.
 * <p>The thread applies the communications finite state machine needed to support the MAVLink protocol.
 * If you need to send a new type of message to the flight controller, please contact with the original developer (it would be easier).</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class UAVControllerThread extends Thread {
	
	// Lines format
	private static final Stroke STROKE_LINE = new BasicStroke(1f);
	private static final Stroke STROKE_TRACK = new BasicStroke(3f);
	private static final float[] DASHING_PATTERN = { 2f, 2f };
	private static final Stroke STROKE_WP_LIST = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			DASHING_PATTERN, 2.0f);
	private final Color color;
	
	private final int numUAV;	// Id of the UAV
	private boolean isStarting; // Used to wait until the first valid coordinates set is received
	private int numTests;		// It allows to check several times if the waypoint has been sent well.
								// Several updates of the previous waypoint can be received after the current one

	/** Connection parameters */
	private Socket socket = null;
	private SerialPort serialPort = null;
	private MavlinkConnection connection;
	
	/** Indicates when the UAV connects with the application (MAVLink ready). */
	private boolean uavConnected = false;
	
	/** Indicates when the UAV sends valid coordinates. */
	private int receivedGPSOnline = 0;
	private boolean locationReceived = false;
	
	private DrawableLinesGeo mission = null;
	private DrawablePathGeo path = null;
	private DrawableCircleGeo collisionRadius = null;
	private DrawableImageGeo uav = null;
	
	private final ValidationTools round;
	
	private static final int SET_MODE_COMMAND = SetMode.class.getAnnotation(MavlinkMessageInfo.class).id();
	private static final int NAV_LAND_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LAND).value();
	private static final int NAV_RETURN_TO_LAUNCH_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH).value();

	private OmnetppTalker omnetTalker;

	public UAVControllerThread(int numUAV) {
		this.numUAV = numUAV;
		this.isStarting = true;
		this.numTests = 0;
		this.color = SimParam.COLOR[numUAV % SimParam.COLOR.length];

		// Connection through serial port on a real UAV
		if (Param.role == ArduSim.MULTICOPTER) {
			try {
				serialPort = SerialPort.getCommPort(UAVParam.serialPort);
				serialPort.setComPortParameters(UAVParam.baudRate, 8, 1, SerialPort.NO_PARITY);
				serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);//100, 0); It must never timeout (0 better than 100ms)
				if (serialPort.openPort()) {
					connection = MavlinkConnection.create(serialPort.getInputStream(), serialPort.getOutputStream());
				} else {
					serialPort.closePort();
					serialPort = null;
				}
			} catch (SerialPortInvalidPortException ignored) {}
			
			if (serialPort == null) {
				ArduSimTools.logGlobal(Text.SERIAL_ERROR);
				System.exit(1);
			}
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			// Connection through TCP in the simulator
			int port = UAVParam.mavPort[numUAV];
			try {
				socket = new Socket(UAVParam.MAV_NETWORK_IP, port);
				socket.setTcpNoDelay(true);
				connection = MavlinkConnection.create(socket.getInputStream(), socket.getOutputStream());
				this.omnetTalker = new OmnetppTalker(numUAV);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (socket == null) {
				ArduSimTools.closeAll(Text.TCP_ERROR);
			}
		}
		
		this.round = API.getValidationTools();
	}

	@Override
	public void run() {
		omnetTalker.start();
		MavlinkMessage<?> inMsg;
		// Periodically sending a heartbeat to keep control over the UAV, while reading received MAVLink messages
		int trying = 0;
		long prevTime = System.nanoTime();
		while (true) {
			try {
				inMsg = this.connection.next();
				if (inMsg != null) {
					// Identify and process the received message
					identifyMessage(inMsg);
					// When all UAVs are connected, start to process commands and sending heartbeat
					if (UAVParam.numMAVLinksOnline.get() == Param.numUAVs) {
						prevTime = sendHeartBeatPeriodically(prevTime);
						processCommand();
					}
				}
			} catch(EOFException e) {
				if(Param.simStatus != SimulatorState.SHUTTING_DOWN){
					ArduSimTools.warnGlobal("MavLink closing" , "UAV " + numUAV +
							" mavlink connection closing before the protocol is ending due to end of stream");
				}
				// TODO check why the mavlink is not sending messages
				break;
			} catch(IOException ignored) {}
		}
		if (Param.role == ArduSim.MULTICOPTER) {
			serialPort.closePort();
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	private long sendHeartBeatPeriodically(long prevTime){
		long posTime = System.nanoTime();
		if (posTime - prevTime > UAVParam.HEARTBEAT_PERIOD) {
			try {
				this.connection.send1(UAVParam.gcsId.get(numUAV),
						0,    // MavComponent.MAV_COMP_ID_ALL
						Heartbeat.builder()
								.type(MavType.MAV_TYPE_GCS)
								.autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
								.systemStatus(MavState.MAV_STATE_UNINIT)
								.mavlinkVersion(3)
								.build());

			} catch (IOException ignored) {
				ignored.printStackTrace();
			}
			prevTime = posTime;
		}
		return prevTime;
	}

	/** Message identification to process it. */
	private void identifyMessage(MavlinkMessage<?> inMsg) {
		/*
		if (inMsg instanceof Mavlink2Message) {
			// Mavlink v2 message
			Mavlink2Message<?> msg = (Mavlink2Message<?>) inMsg;
			if (msg.isSigned()) {
				if (msg.validateSignature(secretKey)) {
					// TODO implement v2 signed messages

				} else {
					// ignore suspicious message
				}
			} else {
				// TODO implement v2 messages
				
				
			}
		} else {
			// mavlink v1 message
		}
		*/

		Object payload = inMsg.getPayload();
		if (payload instanceof Heartbeat) {
			// Detect when the UAV has connected (first heartbeat received)
			if (!uavConnected) {
				int id = inMsg.getOriginSystemId();
				UAVParam.mavId.set(numUAV, id);
				UAVParam.numMAVLinksOnline.incrementAndGet();
				uavConnected = true;
			}
			processMode((Heartbeat) payload);
		} else if (payload instanceof GlobalPositionInt) {
			processGlobalLocation((GlobalPositionInt) payload);
		} else if (payload instanceof CommandAck) {
			processCommandAck((CommandAck) payload);
		} else if (payload instanceof ParamValue) {
			processParam((ParamValue) payload);
		} else if (payload instanceof SysStatus) {
			processStatus((SysStatus) payload);
		} else if (payload instanceof Statustext) {
			if (numUAV == 0) {
				processVersion((Statustext) payload);
			}
			if (!this.locationReceived) {
				processGPSFix((Statustext) payload);
			}
		}else if (payload instanceof MissionItemReached) {
			processWaypointReached((MissionItemReached) payload);
		} else if (payload instanceof MissionCurrent) {
			processCurrentWaypointACK((MissionCurrent) payload);
		} else if (payload instanceof MissionCount) {
			processWaypointCount((MissionCount) payload);
		} else if (payload instanceof MissionItem) {
			processWaypointReceived((MissionItem) payload);
		} else if (payload instanceof MissionRequest) {
			processWaypointRequest((MissionRequest) payload);
		} else if (payload instanceof MissionAck) {
			processMissionAck((MissionAck) payload);
		}
//		else if (payload instanceof LocalPositionNed
//				|| payload instanceof GpsRawInt
//				|| payload instanceof NavControllerOutput
//				|| payload instanceof PowerStatus
//				|| payload instanceof Meminfo
//				|| payload instanceof HomePosition
//				|| payload instanceof GpsGlobalOrigin) {
//			
//		} else {
//			System.out.println(payload.toString());
//		}
	}
	
	/** Process a received flight mode and assert that the MAVLink link has been established. */
	private void processMode(Heartbeat msg) {
		FlightMode prevMode = UAVParam.flightMode.get(numUAV);
		// Only process valid mode
		int baseMode = msg.baseMode().value();
		long customMode = msg.customMode();
		if (baseMode != 0 || customMode != 0) {
			// Only process when mode changes
			if (prevMode == null || prevMode.getBaseMode() != baseMode || prevMode.getCustomMode() != customMode) {
				FlightMode mode = FlightMode.getMode(baseMode, customMode);
				if (mode != null) {
					UAVParam.flightMode.set(numUAV, mode);
					SimTools.updateUAVMAVMode(numUAV, mode.getMode());	// Update the progress dialog
					if (mode.getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
						UAVParam.flightStarted.set(numUAV, 1);
					}
				} else {
					ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.FLIGHT_MODE_ERROR_2 + "(" + baseMode + "," + customMode + ")");
				}
			}
		}
	}

	/** Process the received current location, speed and UAV orientation (heading). */
	private void processGlobalLocation(GlobalPositionInt msg) {
		int lat = msg.lat();
		int lon = msg.lon();
		int vx, vy;
		// Only process position when the GPS sends valid Mercator coordinates (non zero latitude and longitude)
		if (!this.isStarting || (lat != 0 && lon != 0)) {
			isStarting = false;
			if (!this.locationReceived && this.receivedGPSOnline == 2) {
				UAVParam.numGPSFixed.incrementAndGet();
				this.locationReceived = true;
			}

			long time = System.nanoTime();
			Location2D location = new Location2D(lat * 0.0000001, lon * 0.0000001);
			Location2DUTM locationUTM = location.getUTMLocation();
			
			// The last few UAV positions are stored for later use in main.java.com.api.protocols
			UAVParam.lastUTMLocations[numUAV].add(locationUTM);
			// Global horizontal speed estimation
			vx = msg.vx();
			vy = msg.vy();
			double hSpeed = Math.sqrt(Math.pow(vx * 0.01, 2) + Math.pow(vy * 0.01, 2));
			double[] speed = new double[] {vx * 0.01, vy * 0.01, msg.vz() * 0.01};
			// Update the UAV data, including the acceleration calculus
			double z = this.round.roundDouble(msg.alt() * 0.001, 2);
			double zRel = this.round.roundDouble(msg.relativeAlt() * 0.001, 2);
			double heading = (msg.hdg() * 0.01) * Math.PI / 180;
			UAVParam.uavCurrentData[numUAV].update(time, location, z, zRel, speed, hSpeed, heading);
			// Send location to GUI to draw the UAV path and to log data
			SimulatorState currentState = Param.simStatus;
			int state = currentState.getStateId();
			// Differentiate if this specific UAV has finished the experiment
			if (currentState == SimulatorState.TEST_IN_PROGRESS	&& Param.testEndTime[numUAV] != 0) {
				state = SimulatorState.TEST_FINISHED.getStateId();
			}
			// Log data
			SimParam.uavUTMPath[numUAV].add(new LogPoint(time, locationUTM.x, locationUTM.y, z, zRel, heading, hSpeed,
					state));
			
			// Draw the movement on screen
			if (Param.role == ArduSim.SIMULATOR_GUI) {
				Location2DGeo locGeo = location.getGeoLocation();
				if (this.path == null) {
					try {
						this.path = Mapper.Drawables.addPathGeo(SimParam.PATH_LEVEL, locGeo,
								SimParam.minScreenMovement, this.color, UAVControllerThread.STROKE_TRACK);
						if (UAVParam.collisionCheckEnabled) {
							this.collisionRadius = Mapper.Drawables.addCircleGeo(SimParam.COLLISION_LEVEL, locGeo,
									UAVParam.collisionDistance, Color.RED, UAVControllerThread.STROKE_LINE);
						}
						this.uav = Mapper.Drawables.addImageGeo(SimParam.UAV_LEVEL, locGeo, heading,
								SimParam.uavImage, SimParam.UAV_PX_SIZE);
						this.uav.updateBottomLeftText("UAV " + numUAV);
					} catch (GUIMapPanelNotReadyException e) {
						e.printStackTrace();
					}
					
				} else {
					this.path.updateLocation(locGeo);
					if (UAVParam.collisionCheckEnabled) {
						this.collisionRadius.updateLocation(locGeo);
					}
					this.uav.updateLocation(locGeo);
					this.uav.updateHeading(heading);
				}
				this.uav.updateUpRightText("" + z);
			}
		}
	}

	/** Process a received command ACK. */
	private void processCommandAck(CommandAck msg) {
		MavResult result = msg.result().entry();
		int status = UAVParam.MAVStatus.get(numUAV);
		
		// ACK received when changing the flight mode
		// Special case. TODO it is deprecated, move to the new function if using Mavlink v2
		if (status == UAVParam.MAV_STATUS_ACK_MODE) {
			if (msg.command().value() == UAVControllerThread.SET_MODE_COMMAND) {
				if (result == MavResult.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_MODE);
				}
			}
			return;
		}
		
		MavCmd command = msg.command().entry();
		switch (status) {
		// ACK received when arming motors
		case UAVParam.MAV_STATUS_ACK_ARM:
			if (command == MavCmd.MAV_CMD_COMPONENT_ARM_DISARM) {
				if (result == MavResult.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ARM);
				}
			}
			break;
		// ACK received when taking off
		case UAVParam.MAV_STATUS_ACK_TAKE_OFF:
			if (command == MavCmd.MAV_CMD_NAV_TAKEOFF) {
				if (result == MavResult.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_TAKE_OFF);
				}
			}
			break;
		// ACK received when changing the horizontal flight speed
		case UAVParam.MAV_STATUS_ACK_SET_SPEED:
			if (command == MavCmd.MAV_CMD_DO_CHANGE_SPEED) {
				if (result == MavResult.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				} else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SET_SPEED);
				}
			}
			break;
		// ACK received when asking for message
		case UAVParam.MAV_STATUS_ACK_REQUEST_MESSAGE:
			if (command == MavCmd.MAV_CMD_SET_MESSAGE_INTERVAL) {
				if(result == MavResult.MAV_RESULT_ACCEPTED) {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				}else {
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_MESSAGE);
				}
			}
			break;
		default:
			ArduSimTools.logGlobal(Text.NOT_REQUESTED_ACK_ERROR
					+ " (command=" + command.name() + ", result=" + result.name() + ")");
		}
	}
	
	/** Process a received param value. */
	private void processParam(ParamValue msg) {
		String paramId = msg.paramId();
		float value = msg.paramValue();
		// General storage of parameters
		UAVParam.loadedParams[numUAV].put(paramId, new CopterParamLoaded(paramId, value, msg.paramType()));
		UAVParam.lastParamReceivedTime[numUAV].set(System.currentTimeMillis());
		if (UAVParam.totParams[numUAV] == 0) {
			UAVParam.totParams[numUAV] = msg.paramCount();	// It starts counting from 0
			UAVParam.paramLoaded[numUAV] = new boolean[UAVParam.totParams[numUAV]];
		}
		if (msg.paramIndex() != -1 && msg.paramIndex() != 65535) {
			UAVParam.paramLoaded[numUAV][msg.paramIndex()] = true;
		}
		
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ACK_PARAM
				&& paramId.equals(UAVParam.newParam[numUAV].getId())) {
			// A parameter has been modified
			if (value == UAVParam.newParamValue.get(numUAV)) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_1_PARAM);
			}
		} else if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_WAIT_FOR_PARAM) {
			int index = UAVParam.newParamIndex.get(numUAV);
			if (index == -1) {
				if (paramId.equals(UAVParam.newParam[numUAV].getId())) {
					// A single param has been read
					UAVParam.newParamValue.set(numUAV, value);
					UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
				}
			} else if (msg.paramIndex() == index) {
				// A single param has been read
				UAVParam.newParamValue.set(numUAV, value);
				UAVParam.newParamIndex.set(numUAV, -1);
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			}
		}
	}
	
	/** Process battery information received. */
	private void processStatus(SysStatus msg) {
		UAVParam.uavCurrentStatus[numUAV].update(msg.voltageBattery(), msg.currentBattery(),
				msg.batteryRemaining(), msg.load());
	}
	
	/** Process the detection of the ArduCopter version. */
	private void processVersion(Statustext msg) {
		String text = msg.text().toUpperCase();
		if (text.startsWith("APM:COPTER") || text.startsWith("ARDUCOPTER")) {
			UAVParam.arducopterVersion.set(text.split(" ")[1].substring(1));
		}
	}

	/** Process the detection of the GPS fix. */
	private void processGPSFix(Statustext msg) {
		// Both IMU0 and IMU1 must confirm that they are using GPS, and valid coordinates must be received
		if (this.receivedGPSOnline < 2) {
			String text = msg.text();
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

	/** Detect when the UAV reaches the next waypoint. */
	private void processWaypointReached(MissionItemReached msg) {
		int seq = msg.seq();
		// The received value begins in 0
		UAVParam.currentWaypoint.set(numUAV, seq);
		ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.WAYPOINT_REACHED + " = " + seq);
		
		Waypoint lastWP = UAVParam.lastWP[numUAV];
		if (seq > 0) {
			if (lastWP.getCommand().value() == UAVControllerThread.NAV_LAND_COMMAND
					|| lastWP.getCommand().value() == UAVControllerThread.NAV_RETURN_TO_LAUNCH_COMMAND) {
				if (seq == lastWP.getNumSeq() - 1) {
					ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.LAST_WAYPOINT_REACHED);
					UAVParam.lastWaypointReached[numUAV].set(true);
				}
			} else {
				if (seq == lastWP.getNumSeq()) {
					ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.LAST_WAYPOINT_REACHED);
					UAVParam.lastWaypointReached[numUAV].set(true);
				}
			}
		}
		
		ArduSimTools.triggerWaypointReached(numUAV, seq);
	}
	
	/** Process the ACK when changing the current target waypoint. */
	private void processCurrentWaypointACK(MissionCurrent msg) {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ACK_CURRENT_WP) {
			if (msg.seq() == UAVParam.newCurrentWaypoint[numUAV]) {
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

	/** Process the received message that gets the number of waypoints included in the UAV main.java.com.protocols.mission. */
	private void processWaypointCount(MissionCount msg) {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_REQUEST_WP0) {
			int count = msg.count();
			UAVParam.newGeoMission[numUAV] = new Waypoint[count];
			if (count == 0) {
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
	private void processWaypointReceived(MissionItem msg) {
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_REQUEST_WPS) {
			int seq = msg.seq();
			if (seq >= 0 && seq < UAVParam.newGeoMission[numUAV].length) {
				Waypoint wp = new Waypoint(seq, msg.current() != 0, msg.frame().entry(),
						msg.command(), msg.param1(), msg.param2(), msg.param3(),
						msg.param4(), msg.x(), msg.y(), msg.z(), msg.autocontinue());
				UAVParam.newGeoMission[numUAV][seq] = wp;
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
						connection.send1(UAVParam.gcsId.get(numUAV),
								0,	// MavComponent.MAV_COMP_ID_ALL,
								MissionAck.builder()
									.targetSystem(UAVParam.mavId.get(numUAV))
									.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
									.type(MavMissionResult.MAV_MISSION_ACCEPTED)
									.build());
						UAVParam.currentGeoMission[numUAV].clear();
						UAVParam.currentGeoMission[numUAV].addAll(Arrays.asList(UAVParam.newGeoMission[numUAV]));
						UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
						
						if (Param.role == ArduSim.SIMULATOR_GUI) {
							if (this.mission != null) {
								try {
									Mapper.Drawables.removeDrawable(this.mission);
								} catch (GUIMapPanelNotReadyException e) {
									e.printStackTrace();
								}
							}
							List<Location2DGeo> m = new ArrayList<>(UAVParam.newGeoMission[numUAV].length);
							for (int j = 0; j < UAVParam.newGeoMission[numUAV].length; j++) {
								m.add(new Location2DGeo(UAVParam.newGeoMission[numUAV][j].getLatitude(), UAVParam.newGeoMission[numUAV][j].getLongitude()));
							}
							try {
								this.mission = Mapper.Drawables.addLinesGeo(SimParam.MISSION_LEVEL, m,
										this.color, UAVControllerThread.STROKE_WP_LIST);
							} catch (GUIMapPanelNotReadyException e) {
								e.printStackTrace();
							}
						}
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
				ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.IMPOSSIBLE_WAYPOINT_ERROR);
			}
		}
	}

	/** Process the request for a waypoint (sending waypoints to the flight controller). */
	private void processWaypointRequest(MissionRequest msg) {
		int seq = msg.seq();
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_SENDING_WPS && seq >= 0
				&& seq < UAVParam.currentGeoMission[numUAV].size()) {
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						UAVParam.currentGeoMission[numUAV].get(seq).getMessage()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.build());
			} catch (IOException e) {
				// If the dispatch fails, it is expected that the flight controller will request it again
			}
		}
	}

	/** Process a main.java.com.protocols.mission ACK. */
	private void processMissionAck(MissionAck msg) {
		int type = msg.type().value();
		switch (UAVParam.MAVStatus.get(numUAV)) {
		// ACK received when removing the current main.java.com.protocols.mission
		case UAVParam.MAV_STATUS_ACK_CLEAR_WP_LIST:
			if (type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST);
			}
			break;
		// ACK received when sending a waypoint
		case UAVParam.MAV_STATUS_SENDING_WPS:
			if (type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SENDING_WPS);
			}
			break;
		// ACK received when moving a UAV to another location
		case UAVParam.MAV_STATUS_ACK_MOVE_UAV:
			if (type == 0) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} else {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_MOVE_UAV_ERROR);
			}
			break;
		//default:
			// We must not print this, as sometimes the flight controller sends more than one ACK for the main.java.com.protocols.mission
			//SimTools.println(Text.NOT_REQUESTED_ACK_ERROR + " " + message.toString());
		}
	}

	/** Process a new command. */
	private void processCommand() {
		
		// First, override channel values, if available
		RCValues rcValues = UAVParam.rcs[numUAV].getAndSet(null);
		if (rcValues != null) {
			try {
				// Value 0 returns control to the RC. UINT16_MAX avoids changing that channel.
				// Initially, we only use trim values
				int[] values = new int[] {UAVParam.RCtrimValue[numUAV][0], UAVParam.RCtrimValue[numUAV][1],
						UAVParam.RCtrimValue[numUAV][2], UAVParam.RCtrimValue[numUAV][3],
						UAVParam.RCtrimValue[numUAV][4], UAVParam.RCtrimValue[numUAV][5],
						UAVParam.RCtrimValue[numUAV][6], UAVParam.RCtrimValue[numUAV][7]};
				// Now we set roll, pitch, throttle, and yaw values
				values[UAVParam.RCmapRoll[numUAV]-1] = rcValues.roll;
				values[UAVParam.RCmapPitch[numUAV]-1] = rcValues.pitch;
				values[UAVParam.RCmapThrottle[numUAV]-1] = rcValues.throttle;
				values[UAVParam.RCmapYaw[numUAV]-1] = rcValues.yaw;
				// Finally, the flight mode
				int fltmode = UAVParam.customModeToFlightModeMap[numUAV][(int)UAVParam.flightMode.get(numUAV).getCustomMode()];
				if (fltmode != -1) {
					values[4] = UAVParam.RC5_MODE_LEVEL[fltmode - 1][1];
				}
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						RcChannelsOverride.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.chan1Raw(values[0])
							.chan2Raw(values[1])
							.chan3Raw(values[2])
							.chan4Raw(values[3])
							.chan5Raw(values[4])
							.chan6Raw(values[5])
							.chan7Raw(values[6])
							.chan8Raw(values[7])
							.build());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Location3DGeo target = UAVParam.target[numUAV].getAndSet(null);
		if (target != null) {
			try {
				// A possible alternative to this message is MissionItem using ACK, but without waiting to reach destination
				// Never mix location and speed mask fields
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						SetPositionTargetGlobalInt.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.timeBootMs(System.currentTimeMillis())
							.coordinateFrame(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT)
							.typeMask(
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_X_IGNORE,	// bit 0, value 1
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_Y_IGNORE,
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_Z_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VX_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VY_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VZ_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AX_IGNORE,	// Acceleration not supported in ArduPilot
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AY_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AZ_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_FORCE_SET,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_YAW_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_YAW_RATE_IGNORE)
							.latInt((int)Math.round(10000000L * target.latitude))	// Degrees * 10^7
							.lonInt((int)Math.round(10000000L * target.longitude))
							.alt(Double.valueOf(target.altitude).floatValue())
							.build());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		float[] targetSpeed = UAVParam.targetSpeed[numUAV].getAndSet(null);
		if (targetSpeed != null) {
			try {
				// A possible alternative to this message is MissionItem using ACK, but without waiting to reach destination
				// Never mix location and speed mask fields
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						SetPositionTargetGlobalInt.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.timeBootMs(System.currentTimeMillis())
							.coordinateFrame(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT)
							.typeMask(
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_X_IGNORE,	// bit 0, value 1
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_Y_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_Z_IGNORE,
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VX_IGNORE,
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VY_IGNORE,
									//PositionTargetTypemask.POSITION_TARGET_TYPEMASK_VZ_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AX_IGNORE,	// Acceleration not supported in ArduPilot
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AY_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_AZ_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_FORCE_SET,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_YAW_IGNORE,
									PositionTargetTypemask.POSITION_TARGET_TYPEMASK_YAW_RATE_IGNORE)
							.vx(targetSpeed[0])
							.vy(targetSpeed[1])
							.vz(targetSpeed[2])
							.build());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		switch (UAVParam.MAVStatus.get(numUAV)) {
		// Command to take control of the UAV using (half throttle)
		case UAVParam.MAV_STATUS_THROTTLE_ON:
			try {
				// Initially, we only use trim values
				int[] values = new int[] {UAVParam.RCtrimValue[numUAV][0], UAVParam.RCtrimValue[numUAV][1],
						UAVParam.RCtrimValue[numUAV][2], UAVParam.RCtrimValue[numUAV][3],
						UAVParam.RCtrimValue[numUAV][4], UAVParam.RCtrimValue[numUAV][5],
						UAVParam.RCtrimValue[numUAV][6], UAVParam.RCtrimValue[numUAV][7]};
				// Now we set the throttle value
				values[UAVParam.RCmapThrottle[numUAV]-1] = UAVParam.stabilizationThrottle[numUAV];
				// Finally, the flight mode
				int fltmode = UAVParam.customModeToFlightModeMap[numUAV][(int)UAVParam.flightMode.get(numUAV).getCustomMode()];
				if (fltmode != -1) {
					values[4] = UAVParam.RC5_MODE_LEVEL[fltmode - 1][1];
				}
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						RcChannelsOverride.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.chan1Raw(values[0])
							.chan2Raw(values[1])
							.chan3Raw(values[2])
							.chan4Raw(values[3])
							.chan5Raw(values[4])
							.chan6Raw(values[5])
							.chan7Raw(values[6])
							.chan8Raw(values[7])
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			} catch (IOException e1) {
				e1.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_THROTTLE_ON_ERROR);
			}
			break;
		case UAVParam.MAV_STATUS_RECOVER_CONTROL:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						RcChannelsOverride.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.chan1Raw(0)
							.chan2Raw(0)
							.chan3Raw(0)
							.chan4Raw(0)
							.chan5Raw(0)
							.chan6Raw(0)
							.chan7Raw(0)
							.chan8Raw(0)
							.build());
				// From now, the RC channels will not be overriden by ArduSim
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
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						MissionItem.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.frame(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT)
							.command(MavCmd.MAV_CMD_NAV_WAYPOINT)
							.current(2)	// Means that the command is issued in guided mode
							.autocontinue(0)
							.param1(0)	// Delay in decimal seconds before going to next waypoint
							.param2(0)	// Ignored
							.param3(0)	// Ignored
							.param4(0)	// Ignored
							.x(UAVParam.newLocation[numUAV][0])
							.y(UAVParam.newLocation[numUAV][1])
							.z(UAVParam.newLocation[numUAV][2])
							.build());
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
					// Actualy, it should be better to use a msg_command_long, but SITL does not support it on MAVLink v1 and this solution depends on the compilation (ardupilot, PX4,...)
					connection.send1(UAVParam.gcsId.get(numUAV),
							0,	// MavComponent.MAV_COMP_ID_ALL
							SetMode.builder()
								.targetSystem(UAVParam.mavId.get(numUAV))
								.baseMode(EnumValue.create(MavMode.class, UAVParam.newFlightMode[numUAV].getBaseMode()))
								.customMode(UAVParam.newFlightMode[numUAV].getCustomMode())
								.build());
					// ALERT The base mode EnumValue gets null as entry because the custom flight modes are not
					//  included in MavMode enumerator, but it works because the library only uses the value
					//  provided to send the adequate command to the flight controller
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
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL
						CommandLong.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.command(MavCmd.MAV_CMD_COMPONENT_ARM_DISARM)
							.confirmation(0)
							.param1(1)
							.build());	// .param2(21196) ONLY to disarm the vehicle in flight
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_ARM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ARM);
			}
			break;
		// Command to take off the UAV to the absolute altitude stored in the variable UAVParam.takeOffAltitude[numUAV]
		case UAVParam.MAV_STATUS_REQUEST_TAKE_OFF:
			
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL
						CommandLong.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.command(MavCmd.MAV_CMD_NAV_TAKEOFF)
							.confirmation(0)
							//.param1(0)	Ignored: climb angle on planes
							.param7((float)UAVParam.takeOffAltitude.get(numUAV))
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_TAKE_OFF);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_TAKE_OFF);
			}
			break;
		// Command to set the flight speed stored in the variable UAVParam.newSpeed[numUAV]
		case UAVParam.MAV_STATUS_SET_SPEED:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL
						CommandLong.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.command(MavCmd.MAV_CMD_DO_CHANGE_SPEED)
							.confirmation(0)
							//.param1(1)	Ignored: 1 Means ground speed, 0 air speed
							.param2((float)UAVParam.newSpeed[numUAV])
							//.param3(-1)	Ignored: Throttle (0-100%). -1 means ignore
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_SET_SPEED);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SET_SPEED);
			}
			break;
		// Command to change the value UAVParam.newParamValue[numUAV] of the parameter UAVParam.newParam[numUAV]
		case UAVParam.MAV_STATUS_SET_PARAM:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						ParamSet.builder()
						.targetSystem(UAVParam.mavId.get(numUAV))
						.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
						.paramId(UAVParam.newParam[numUAV].getId())
						.paramValue((float)UAVParam.newParamValue.get(numUAV))
						.paramType(UAVParam.newParam[numUAV].getType())
						.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_1_PARAM);
			}
			break;
		// Command to store the value of the parameterUAVParam.newParam[numUAV] on the variable UAVParam.newParamValue[numUAV]
		case UAVParam.MAV_STATUS_GET_PARAM:
			int index = UAVParam.newParamIndex.get(numUAV);
			try {
				if (index >= 0 && index < UAVParam.totParams[numUAV]) {
					connection.send1(UAVParam.gcsId.get(numUAV),
							0,	// MavComponent.MAV_COMP_ID_ALL,
							ParamRequestRead.builder()
								.targetSystem(UAVParam.mavId.get(numUAV))
								.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
								.paramIndex(index)		// Needs to be -1 to use the param id field
								//.paramId(UAVParam.newParam[numUAV].getId())cambiar a usar index
								.build());
				} else {
					connection.send1(UAVParam.gcsId.get(numUAV),
							0,	// MavComponent.MAV_COMP_ID_ALL,
							ParamRequestRead.builder()
								.targetSystem(UAVParam.mavId.get(numUAV))
								.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
								.paramIndex(-1)		// Needs to be -1 to use the param id field
								.paramId(UAVParam.newParam[numUAV].getId())
								.build());
				}
				
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_WAIT_FOR_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_2_PARAM);
			}
			
			break;
		// Command to change the current waypoint to the one stored in the variable UAVParam.newCurrentWaypoint[numUAV]
		// Waypoints start in 0
		case UAVParam.MAV_STATUS_SET_CURRENT_WP:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						MissionSetCurrent.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.seq(UAVParam.newCurrentWaypoint[numUAV])
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_CURRENT_WP);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CURRENT_WP);
			}
			break;
		// Command to clear the main.java.com.protocols.mission stored in the UAV
		case UAVParam.MAV_STATUS_CLEAR_WP_LIST:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						MissionClearAll.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)
							.build());	// MavComponent.MAV_COMP_ID_ALL
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_CLEAR_WP_LIST);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_CLEAR_WP_LIST);
			}
			break;
		// Command to send the number of waypoints of a new main.java.com.protocols.mission stored in UAVParam.currentGeoMission[numUAV]
		case UAVParam.MAV_STATUS_SEND_WPS:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						MissionCount.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.count(UAVParam.currentGeoMission[numUAV].size())
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_SENDING_WPS);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_SENDING_WPS);
			}
			break;
		// Command to request the number of waypoints of the main.java.com.protocols.mission stored in the UAV
		case UAVParam.MAV_STATUS_REQUEST_WP_LIST:
			try {
				// The main.java.com.protocols.mission type must be set if using Mavlink v2
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						MissionRequestList.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
							.build());
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_WP0);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_WP_LIST);
			}
			break;
		case UAVParam.MAV_STATUS_REQUEST_ALL_PARAM:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,	// MavComponent.MAV_COMP_ID_ALL,
						ParamRequestList.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0)
							.build());	// MavComponent.MAV_COMP_ID_ALL
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_TIMEOUT_ALL_PARAM);
			} catch (IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_ALL_PARAM);
			}
			break;
		case UAVParam.MAV_STATUS_REQUEST_MESSAGE:
			try {
				connection.send1(UAVParam.gcsId.get(numUAV),
						0,
						CommandLong.builder()
							.targetSystem(UAVParam.mavId.get(numUAV))
							.targetComponent(0) // MavComponent.MAV_COMP_ID_ALL
							.command(MavCmd.MAV_CMD_SET_MESSAGE_INTERVAL) 
							.confirmation(0)
							.param1(UAVParam.messageId.get()) 	//messageId 
							.param2(250000) // interval 4 time per second 
							.param7(0) 		// respond target 0 = default
							.build()
						);
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ACK_REQUEST_MESSAGE);
			}catch(IOException e) {
				e.printStackTrace();
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_ERROR_REQUEST_MESSAGE);
			}
			break;
		}
		
	}
	
	/** Use yaw in degrees (0-360). */
	private void msgYaw(float yaw) throws IOException {
		connection.send1(UAVParam.gcsId.get(numUAV),
				0,	// MavComponent.MAV_COMP_ID_ALL
				CommandLong.builder()
					.targetSystem(UAVParam.mavId.get(numUAV))
					.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
					.command(MavCmd.MAV_CMD_CONDITION_YAW)
					.confirmation(0)
					.param1(yaw)	// [0-360] Angle in degress
					.build());
	}

	/** Sending a query message for a waypoint stored in the UAV. */
	private void msgGetWaypoint(int numWP) throws IOException {
		connection.send1(UAVParam.gcsId.get(numUAV),
				0,	// MavComponent.MAV_COMP_ID_ALL,
				MissionRequest.builder()
					.targetSystem(UAVParam.mavId.get(numUAV))
					.targetComponent(0)	// MavComponent.MAV_COMP_ID_ALL
					.seq(numWP)
					.build());
	}
	
}
