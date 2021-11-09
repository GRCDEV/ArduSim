package com.api;

import com.api.communications.lowLevel.LowLevelCommLink;
import com.api.cpuHelper.CPUData;
import com.api.swarm.SwarmParam;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.uavController.atomicDoubleArray.AtomicDoubleArray;
import com.api.pojo.ConcurrentBoundedQueue;
import com.api.copter.CopterParam;
import com.api.pojo.FlightMode;
import com.api.pojo.location.LogPoint;
import com.api.pojo.location.Waypoint;
import com.api.pojo.location.WaypointSimplified;
import com.setup.*;
import com.setup.Param.SimulatorState;
import com.setup.pccompanion.logic.PCCompanionParam;
import com.setup.sim.gui.MainWindow;
import com.setup.sim.gui.MissionKmlSimProperties;
import com.setup.sim.logic.GPSEnableThread;
import com.setup.sim.logic.SimParam;
import com.setup.sim.pojo.PortScanResult;
import com.setup.sim.pojo.WinRegistry;
import com.uavController.UAVControllerThread;
import com.uavController.UAVCurrentData;
import com.uavController.UAVCurrentStatus;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import io.dronefleet.mavlink.util.EnumValue;
import javafx.scene.control.TextFormatter;
import org.javatuples.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** This class contains general tools used by the simulator.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ArduSimTools {

	// Used to avoid the exit dialog to be opened more than once at the same time.
	private static final AtomicBoolean exiting = new AtomicBoolean();
	
	public static final Queue<WaypointReachedListener> listeners = new ConcurrentLinkedQueue<>();
	
	private static volatile boolean storingResults = false;
	
	// Available protocols (Internal use by ArduSim)
	public static Class<?>[] ProtocolClasses;
	public static volatile String[] ProtocolNames = null;
	public static volatile String noneProtocolName = null;

	// Selected protocol (Internal use by ArduSim)
	public static volatile String selectedProtocol;
	public static volatile ProtocolHelper selectedProtocolInstance;
	
	private static final EnumValue<MavCmd> NAV_WAYPOINT_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_WAYPOINT);
	private static final EnumValue<MavCmd> NAV_TAKEOFF_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_TAKEOFF);
	private static final EnumValue<MavCmd> NAV_LAND_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_LAND);
	private static final EnumValue<MavCmd> NAV_RETURN_TO_LAUNCH_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH);
	private static final EnumValue<MavCmd> NAV_SPLINE_WAYPOINT_COMMAND = EnumValue.of(MavCmd.MAV_CMD_NAV_SPLINE_WAYPOINT);
	/**
	 * Arducopter Sim_speedup parameter: 1 = normal wall-clock time. e.g 5 = 5x realtime or 0.1 = 1/10th realtime
	 */
	private static String SIM_SPEEDUP;
	
	/** Parses the command line of the simulator.
	 * <p>Returns false if running a PC companion and the main thread execution must stop.</p> */
	public static void parseArgs(String[] args) {
		String commandLine = "Command line:\n    java -jar ArduSim.jar <mode> <filePath>\nChoose mode:\n" +
				"    multicopter\n    simulator-gui\n    simulator-cli\n    com.api.pccompanion\n" +
				" select a File path to the ardusim.properties file " +
				" default is currentDirectory/resources/ardusim/SimulationParam.properties";
		if (args.length < 1) {
			System.out.println(commandLine);
			System.out.flush();
			System.exit(1);
		}

		if (args[0].equalsIgnoreCase("multicopter")) {
			Param.role = ArduSim.MULTICOPTER;
		}else if (args[0].equalsIgnoreCase("simulator-gui")) {
			Param.role = ArduSim.SIMULATOR_GUI;
		}else if (args[0].equalsIgnoreCase("pccompanion")) {
			Param.role = ArduSim.PCCOMPANION;
		}else if(args[0].equalsIgnoreCase("simulator-cli")){
			Param.role = ArduSim.SIMULATOR_CLI;
		}else{
			System.out.println(commandLine);
			System.out.flush();
			System.exit(1);
		}
		if(args.length == 2){
			SimParam.resourcesFile = new File(args[1]);
		}else{
			String fs = File.separator;
			SimParam.resourcesFile = new File(API.getFileTools().getSourceFolder().toString() +
					fs + "main" + fs + "resources" + fs + "setup" + fs + "SimulationParam.properties");
		}
	}
	
	/** Parses the ini file located beside the ArduSim executable file (Project folder in simulations). */
	public static void parseIniFile() {
		Map<String, String> parameters;
		String param;
		// Load INI file
		parameters = ArduSimTools.loadIniFile();
		if (parameters.size() == 0) {
			return;
		}
		
		// Parse parameters common to real UAV and PC Companion
		ValidationTools validationTools = API.getValidationTools();
		if (Param.role == ArduSim.PCCOMPANION || Param.role == ArduSim.MULTICOPTER) {
			param = parameters.get(Param.COMPUTER_PORT);
			if (param == null) {
				ArduSimTools.logGlobal(Param.COMPUTER_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + PCCompanionParam.computerPort);
			} else {
				if (!validationTools.isValidPort(param)) {
					ArduSimTools.logGlobal(Param.COMPUTER_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				PCCompanionParam.computerPort = Integer.parseInt(param);
			}
			param = parameters.get(Param.UAV_PORT);
			if (param == null) {
				ArduSimTools.logGlobal(Param.UAV_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + PCCompanionParam.uavPort);
			} else {
				if (!validationTools.isValidPort(param)) {
					ArduSimTools.logGlobal(Param.UAV_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				PCCompanionParam.uavPort = Integer.parseInt(param);
			}
			param = parameters.get(Param.BROADCAST_IP);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BROADCAST_IP + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.broadcastIP);
			} else {
				UAVParam.broadcastIP = param;
			}
			ArduSimTools.logGlobal(Text.INI_FILE_PARAM_BROADCAST_IP_WARNING + " " + param);
			param = parameters.get(Param.BROADCAST_PORT);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BROADCAST_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.broadcastPort);
			} else {
				if (!validationTools.isValidPort(param)) {
					ArduSimTools.logGlobal(Param.BROADCAST_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.broadcastPort = Integer.parseInt(param);
			}
		}

		// Parse parameters for a real UAV
		if (Param.role == ArduSim.MULTICOPTER) {
			if (!parameters.containsKey(Param.PROTOCOL)) {
				ArduSimTools.logGlobal(Text.INI_FILE_PROTOCOL_NOT_FOUND_ERROR);
				System.exit(1);
			}
			if (!parameters.containsKey(Param.SPEED)) {
				ArduSimTools.logGlobal(Text.INI_FILE_SPEED_NOT_FOUND_ERROR);
				System.exit(1);
			}
			// Set protocol
			param = parameters.get(Param.PROTOCOL);
			if (param == null) {
				ArduSimTools.logGlobal(Param.PROTOCOL + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_2);
				System.exit(1);
			} else {
				boolean found = false;
				for (int i = 0; i < ArduSimTools.ProtocolNames.length; i++) {
					if (ArduSimTools.ProtocolNames[i].equalsIgnoreCase(param)) {
						found = true;
						break;
					}
				}
				if (!found) {
					ArduSimTools.logGlobal(param + " " + Text.PROTOCOL_NOT_FOUND_ERROR + "\n\n");

					for (int i = 0; i < ArduSimTools.ProtocolNames.length; i++) {
						ArduSimTools.logGlobal(ArduSimTools.ProtocolNames[i]);
					}
					System.exit(1);
				}
				ArduSimTools.selectedProtocol = param;
				ArduSimTools.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();
				if (ArduSimTools.selectedProtocolInstance == null) {
					System.exit(1);
				}
			}
			
			// Set speed
			param = parameters.get(Param.SPEED);
			if (param == null) {
				ArduSimTools.logGlobal(Param.SPEED + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_2);
				System.exit(1);
			} else {
				if (!validationTools.isValidPositiveDouble(param)) {
					ArduSimTools.logGlobal(Text.SPEED_ERROR + "\n");
					System.exit(1);
				}
				UAVParam.initialSpeeds = new double[1];
				UAVParam.initialSpeeds[0] = Double.parseDouble(param);
			}

			// Master-slave parameters
			param = parameters.get(Param.MACS);
			if (param == null) {
				ArduSimTools.logGlobal(Param.MACS + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + Arrays.toString(SwarmParam.macAddresses) + " / " + Arrays.toString(SwarmParam.macIDs));
			} else {
				String[] macs = param.split(",");
				SwarmParam.macAddresses = macs;
				
				long[] ids = new long[macs.length];
				for (int i = 0; i < macs.length; i++) {
					macs[i] = macs[i].replaceAll("[^a-fA-F0-9]", "");
					if (macs[i].length() < 12) {
						ArduSimTools.logGlobal(Param.MACS + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
						System.exit(1);
					}
					ids[i] = Long.parseUnsignedLong(macs[i], 16);
				}
				SwarmParam.macIDs = ids;
			}
			
			// ArduSim-UAV communication parameters
			param = parameters.get(Param.SERIAL_PORT);
			if (param == null) {
				ArduSimTools.logGlobal(Param.SERIAL_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.serialPort);
			} else {
				if (!param.equalsIgnoreCase(UAVParam.serialPort)) {
					ArduSimTools.logGlobal(Text.INI_FILE_PARAM_SERIAL_PORT_WARNING + " " + param);
					UAVParam.serialPort = param;
				}
			}
			param = parameters.get(Param.BAUD_RATE);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BAUD_RATE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.baudRate);
			} else {
				if (!validationTools.isValidPositiveInteger(param)) {
					ArduSimTools.logGlobal(Param.BAUD_RATE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.baudRate = Integer.parseInt(param);
			}
			
			param = parameters.get(Param.BATTERY_CELLS);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BATTERY_CELLS + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.lipoBatteryCells);
			} else {
				if (!validationTools.isValidPositiveInteger(param)) {
					ArduSimTools.logGlobal(Param.BATTERY_CELLS + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.lipoBatteryCells = Integer.parseInt(param);
				UAVParam.lipoBatteryChargedVoltage = UAVParam.lipoBatteryCells * UAVParam.CHARGED_VOLTAGE;
				UAVParam.lipoBatteryNominalVoltage = UAVParam.lipoBatteryCells * UAVParam.NOMINAL_VOLTAGE;
				UAVParam.lipoBatteryAlarmVoltage = UAVParam.lipoBatteryCells * UAVParam.ALARM_VOLTAGE;
				UAVParam.lipoBatteryFinalVoltage = UAVParam.lipoBatteryCells * UAVParam.FINAL_VOLTAGE;
				UAVParam.lipoBatteryDischargedVoltage = UAVParam.lipoBatteryCells * UAVParam.FULLY_DISCHARGED_VOLTAGE;
				UAVParam.lipoBatteryStorageVoltage = UAVParam.lipoBatteryCells * UAVParam.STORAGE_VOLTAGE;
			}
			param = parameters.get(Param.BATTERY_CAPACITY);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BATTERY_CAPACITY + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.lipoBatteryCapacity);
			} else {
				if (!validationTools.isValidPositiveInteger(param)) {
					ArduSimTools.logGlobal(Param.BATTERY_CAPACITY + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.lipoBatteryCapacity = Integer.parseInt(param);
			}
		}
		
		// Parse remaining parameters, for real UAV or simulation
		
		// Master-slave parameters
		param = parameters.get(Param.ASSIGNMENT_ALGORITHM);
		if (param == null) {
			ArduSimTools.logGlobal(Param.ASSIGNMENT_ALGORITHM + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + SwarmParam.assignmentAlgorithm);
		} else {
			AssignmentAlgorithm.AssignmentAlgorithms algorithm = null;
			for(AssignmentAlgorithm.AssignmentAlgorithms algo: AssignmentAlgorithm.AssignmentAlgorithms.values()){
				if (param.equalsIgnoreCase(algo.name())) {
					algorithm = algo;
				}
			}
			if (algorithm == null) {
				ArduSimTools.logGlobal(Param.ASSIGNMENT_ALGORITHM + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			SwarmParam.assignmentAlgorithm = algorithm;
		}
		
		param = parameters.get(Param.AIR_FORMATION);
		if (param == null) {
			ArduSimTools.logGlobal(Param.AIR_FORMATION + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.airFormation.get().getLayout());
			// Check if the minimum distance is also included (meters)
			param = parameters.get(Param.AIR_DISTANCE);
			if (param == null) {
				ArduSimTools.logGlobal(Param.AIR_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.airDistanceBetweenUAV);
			}
		} else {
			// Check if it is included in the list
			try {
				Formation airformation = FormationFactory.newFormation(Formation.Layout.valueOf(param.toUpperCase()));
				UAVParam.airFormation.set(airformation);
			}catch(IllegalArgumentException | NullPointerException e ){
				ArduSimTools.logGlobal(Param.AIR_FORMATION + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}

			// Check if the minimum distance is also included (meters)
			param = parameters.get(Param.AIR_DISTANCE);
			if (param == null) {
				ArduSimTools.logGlobal(Param.AIR_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.airDistanceBetweenUAV);
			} else {
				if (!validationTools.isValidPositiveDouble(param)) {
					ArduSimTools.logGlobal(Param.AIR_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.airDistanceBetweenUAV = Double.parseDouble(param);
			}
		}
		// Check if the minimum distance for landing is also included (meters)
		param = parameters.get(Param.LAND_DISTANCE);
		if (param == null) {
			ArduSimTools.logGlobal(Param.LAND_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.landDistanceBetweenUAV);
		} else {
			if (!validationTools.isValidPositiveDouble(param)) {
				ArduSimTools.logGlobal(Param.LAND_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			UAVParam.landDistanceBetweenUAV = Double.parseDouble(param);
		}
		param = parameters.get(Param.MEASURE_CPU);
		if (param == null) {
			ArduSimTools.logGlobal(Param.MEASURE_CPU + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + Param.measureCPUEnabled);
		} else {
			if (!validationTools.isValidBoolean(param)) {
				ArduSimTools.logGlobal(Param.MEASURE_CPU + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.measureCPUEnabled = Boolean.parseBoolean(param);
		}
		param = parameters.get(Param.VERBOSE_LOGGING);
		if (param == null) {
			ArduSimTools.logGlobal(Param.VERBOSE_LOGGING + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + Param.verboseLogging);
		} else {
			if (!validationTools.isValidBoolean(param)) {
				ArduSimTools.logGlobal(Param.VERBOSE_LOGGING + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.verboseLogging = Boolean.parseBoolean(param);
		}
		param = parameters.get(Param.VERBOSE_STORE);
		if (param == null) {
			ArduSimTools.logGlobal(Param.VERBOSE_STORE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + Param.storeData);
		} else {
			if (!validationTools.isValidBoolean(param)) {
				ArduSimTools.logGlobal(Param.VERBOSE_STORE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.storeData = Boolean.parseBoolean(param);
		}
		param = parameters.get(Param.WAYPOINT_YAW_OVERRIDE);
		if (param == null) {
			ArduSimTools.logGlobal(Param.WAYPOINT_YAW_OVERRIDE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.overrideYaw);
		} else {
			if (!validationTools.isValidBoolean(param)) {
				ArduSimTools.logGlobal(Param.WAYPOINT_YAW_OVERRIDE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			UAVParam.overrideYaw = Boolean.parseBoolean(param);
		}
		if (UAVParam.overrideYaw) {
			param = parameters.get(Param.WAYPOINT_YAW_VALUE);
			if (param == null) {
				ArduSimTools.logGlobal(Param.WAYPOINT_YAW_VALUE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.YAW_VALUES[UAVParam.yawBehavior]);
			} else {
				if (!validationTools.isValidNonNegativeInteger(param)) {
					ArduSimTools.logGlobal(Param.WAYPOINT_YAW_VALUE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				int yawBehavior = Integer.parseInt(param);
				if (yawBehavior > 3) {
					ArduSimTools.logGlobal(Param.WAYPOINT_YAW_VALUE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.yawBehavior = yawBehavior;
			}
		}
		param = parameters.get(Param.KML_MIN_ALTITUDE);
		if (param == null) {
			ArduSimTools.logGlobal(Param.KML_MIN_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.minAltitude);
		} else {
			if (!validationTools.isValidPositiveDouble(param)) {
				ArduSimTools.logGlobal(Param.KML_MIN_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			UAVParam.minAltitude = Double.parseDouble(param);
		}
		param = parameters.get(Param.KML_OVERRIDE_ALTITUDE);
		if (param == null) {
			ArduSimTools.logGlobal(Param.KML_OVERRIDE_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.overrideAltitude);
		} else {
			if (!validationTools.isValidBoolean(param)) {
				ArduSimTools.logGlobal(Param.KML_OVERRIDE_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			UAVParam.overrideAltitude = Boolean.parseBoolean(param);
		}
		if (UAVParam.overrideAltitude) {
			param = parameters.get(Param.KML_ALTITUDE);
			if (param == null) {
				if (UAVParam.minFlyingAltitude < UAVParam.minAltitude) {
					UAVParam.minFlyingAltitude = UAVParam.minAltitude;
				}
				ArduSimTools.logGlobal(Param.KML_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + UAVParam.minFlyingAltitude);
			} else {
				if (!validationTools.isValidPositiveDouble(param)) {
					ArduSimTools.logGlobal(Param.KML_ALTITUDE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				if (Double.parseDouble(param) < UAVParam.minAltitude) {
					ArduSimTools.logGlobal(Param.KML_ALTITUDE + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + UAVParam.minAltitude);
					UAVParam.minFlyingAltitude = UAVParam.minAltitude;
				} else {
					UAVParam.minFlyingAltitude = Double.parseDouble(param);
				}
			}
		}
		param = parameters.get(Param.KML_MISSION_END);
		if (param == null) {
			ArduSimTools.logGlobal(Param.KML_MISSION_END + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + MissionKmlSimProperties.missionEnd);
		} else {
			if (param.equalsIgnoreCase(MissionKmlSimProperties.MISSION_END_UNMODIFIED)) {
				MissionKmlSimProperties.missionEnd = MissionKmlSimProperties.MISSION_END_UNMODIFIED;
			} else if (param.equalsIgnoreCase(MissionKmlSimProperties.MISSION_END_LAND)) {
				MissionKmlSimProperties.missionEnd = MissionKmlSimProperties.MISSION_END_LAND;
			} else if (param.equalsIgnoreCase(MissionKmlSimProperties.MISSION_END_RTL)) {
				MissionKmlSimProperties.missionEnd = MissionKmlSimProperties.MISSION_END_RTL;
				
				param = parameters.get(Param.KML_RTL_END_ALT);
				if (param == null) {
					ArduSimTools.logGlobal(Param.KML_RTL_END_ALT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + MissionKmlSimProperties.finalAltitudeForRTL);
				} else {
					if (!validationTools.isValidPositiveDouble(param)) {
						ArduSimTools.logGlobal(Param.KML_RTL_END_ALT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
						System.exit(1);
					} else {
						MissionKmlSimProperties.finalAltitudeForRTL = Double.parseDouble(param);
					}
				}
			} else {
				ArduSimTools.logGlobal(Param.KML_MISSION_END + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				ArduSimTools.logGlobal(Param.KML_MISSION_END + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + MissionKmlSimProperties.missionEnd);
			}
		}
		param = parameters.get(Param.KML_WAYPOINT_DELAY);
		if (param == null) {
			ArduSimTools.logGlobal(Param.KML_WAYPOINT_DELAY + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + MissionKmlSimProperties.inputMissionDelay);
		} else {
			if (!validationTools.isValidNonNegativeInteger(param)) {
				ArduSimTools.logGlobal(Param.KML_WAYPOINT_DELAY + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + MissionKmlSimProperties.inputMissionDelay);
			} else {
				int delay = Integer.parseInt(param);
				if (delay < 0 || delay > 65535) {
					ArduSimTools.logGlobal(Param.KML_WAYPOINT_DELAY + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + MissionKmlSimProperties.inputMissionDelay);
				} else {
					MissionKmlSimProperties.finalAltitudeForRTL = delay;
					if (delay > 0) {
						param = parameters.get(Param.KML_WAYPOINT_DISTANCE);
						if (param == null) {
							ArduSimTools.logGlobal(Param.KML_WAYPOINT_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_1 + " " + MissionKmlSimProperties.distanceToWaypointReached);
						} else {
							try {
								int distance = Integer.parseInt(param);
								if (distance < 10 || distance > 1000) {
									ArduSimTools.logGlobal(Param.KML_WAYPOINT_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
									ArduSimTools.logGlobal(Param.KML_WAYPOINT_DISTANCE + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + MissionKmlSimProperties.distanceToWaypointReached);
								} else {
									MissionKmlSimProperties.distanceToWaypointReached = distance;
								}
							} catch (NumberFormatException e) {
								ArduSimTools.logGlobal(Param.KML_WAYPOINT_DISTANCE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
								ArduSimTools.logGlobal(Param.KML_WAYPOINT_DISTANCE + " " + Text.INI_FILE_PARAM_USING_DEFAULT + " " + MissionKmlSimProperties.distanceToWaypointReached);
							}
						}
					}
				}
			}
		}
		
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			param = parameters.get(Param.BING_KEY);
			if (param == null) {
				ArduSimTools.logGlobal(Param.BING_KEY + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR_3);
			} else {
				SimParam.bingKey = param;
			}
		}
	}
	
	/** Auxiliary method to load parameters from the ArduSim ini configuration file. */
	private static Map<String, String> loadIniFile() {
		FileTools fileTools = API.getFileTools();
		Map<String, String> parameters = new HashMap<>();
		File folder = fileTools.getCurrentFolder();
		File [] files = folder.listFiles((dir, name) -> {
			boolean extensionIsIni = false;
			if(name.lastIndexOf(".") > 0) {
				final String extension = name.substring(name.lastIndexOf(".")+1);
				extensionIsIni = extension.equalsIgnoreCase("ini");
			}
			return extensionIsIni;
		});
		if (files == null || files.length == 0) {
			ArduSimTools.logGlobal(Text.INI_FILE_NOT_FOUND);
			return parameters;
		}
		int pos = -1;
		boolean found = false;
		for (int i = 0; i < files.length && !found; i++) {
			if (files[i].getName().equalsIgnoreCase(Text.INI_FILE_NAME)) {
				pos = i;
				found = true;
			}
		}
		if (pos == -1) {
			ArduSimTools.logGlobal(Text.INI_FILE_NOT_FOUND);
			return parameters;
		}
		File iniFile = files[pos];
		
		parameters = fileTools.parseINIFile(iniFile);
		
		if (parameters.size() > 0) {
			// Check waste parameters
			for (final String key : parameters.keySet()) {
				found = false;
				for (int i = 0; i < Param.PARAMETERS.length; i++) {
					if (key.equalsIgnoreCase(Param.PARAMETERS[i])) {
						found = true;
						break;
					}
				}
				if (!found) {
					ArduSimTools.logGlobal(Text.INI_FILE_WASTE_PARAMETER_WARNING + " " + key);
				}
			}
		} else {
			ArduSimTools.logGlobal(Text.INI_FILE_EMPTY);
		}
		return parameters;
	}
	
	/** Dynamically loads a Windows library. */
	public static void loadDll(String name) throws IOException {
		// File copy to temporary folder
	    InputStream in = Main.class.getResourceAsStream(name);
	    byte[] buffer = new byte[1024];
	    int read;
	    File temp = new File(new File(System.getProperty("java.io.tmpdir")),name);
	    FileOutputStream fos = new FileOutputStream(temp);
	    while((read = in.read(buffer)) != -1) {
	        fos.write(buffer, 0, read);
	    }
	    fos.close();
	    in.close();
	    temp.deleteOnExit();
	    // File load from temporary folder
	    System.load(temp.getAbsolutePath());
	}

	/** Initializes the data structures at the simulator start. */
	@SuppressWarnings("unchecked")
	public static void initializeDataStructures() {
		UAVParam.uavCurrentData = new UAVCurrentData[Param.numUAVs];
		UAVParam.uavCurrentStatus = new UAVCurrentStatus[Param.numUAVs];
		UAVParam.lastUTMLocations = new ConcurrentBoundedQueue[Param.numUAVs];
		UAVParam.flightMode = new AtomicReferenceArray<>(Param.numUAVs);
		UAVParam.flightStarted = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.MAVStatus = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentWaypoint = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentGeoMission = new ArrayList[Param.numUAVs];
		UAVParam.lastWP = new Waypoint[Param.numUAVs];
		UAVParam.lastWPUTM = new Location2DUTM[Param.numUAVs];
		UAVParam.RTLAltitude = new double[Param.numUAVs];
		UAVParam.RTLAltitudeFinal = new double[Param.numUAVs];
		UAVParam.mavId = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.gcsId = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCmapRoll = new int[Param.numUAVs];
		UAVParam.RCmapPitch = new int[Param.numUAVs];
		UAVParam.RCmapThrottle = new int[Param.numUAVs];
		UAVParam.RCmapYaw = new int[Param.numUAVs];
		UAVParam.RCminValue = new int[Param.numUAVs][8];
		UAVParam.RCtrimValue = new int[Param.numUAVs][8];
		UAVParam.RCmaxValue = new int[Param.numUAVs][8];
		UAVParam.RCDZValue = new int[Param.numUAVs][8];
		UAVParam.flightModeMap = new int[Param.numUAVs][6];
		UAVParam.customModeToFlightModeMap = new int[Param.numUAVs][24];
		
		UAVParam.newFlightMode = new FlightMode[Param.numUAVs];
		UAVParam.takeOffAltitude = new AtomicDoubleArray(Param.numUAVs);
		UAVParam.newSpeed = new double[Param.numUAVs];
		UAVParam.newParam = new CopterParam[Param.numUAVs];
		UAVParam.newParamValue = new AtomicDoubleArray(Param.numUAVs);
		UAVParam.newParamIndex = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.newCurrentWaypoint = new int[Param.numUAVs];
		UAVParam.newGeoMission = new Waypoint[Param.numUAVs][];
		UAVParam.stabilizationThrottle = new int[Param.numUAVs];
		UAVParam.throttleDZ = new int[Param.numUAVs];
		UAVParam.newLocation = new float[Param.numUAVs][3];

		UAVParam.missionUTMSimplified = new AtomicReferenceArray<>(Param.numUAVs);
		UAVParam.lastWaypointReached = new AtomicBoolean[Param.numUAVs];
		
		UAVParam.rcs = new AtomicReference[Param.numUAVs];
		UAVParam.overrideOn = new AtomicIntegerArray(Param.numUAVs);
		
		UAVParam.target = new AtomicReference[Param.numUAVs];
		UAVParam.targetSpeed = new AtomicReference[Param.numUAVs];
		
		UAVParam.loadedParams = new Map[Param.numUAVs];
		UAVParam.totParams = new int[Param.numUAVs];
		UAVParam.paramLoaded =new boolean[Param.numUAVs][];
		UAVParam.lastParamReceivedTime = new AtomicLong[Param.numUAVs];
		
		SimParam.uavUTMPath = new ArrayList[Param.numUAVs];	// Useful for logging purposes
		
		SimParam.prefix = new String[Param.numUAVs];

		Param.testEndTime = new long[Param.numUAVs];
		
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			SimParam.xUTM = new double[Param.numUAVs];
			SimParam.yUTM = new double[Param.numUAVs];
			SimParam.z = new double[Param.numUAVs];
			SimParam.speed = new double[Param.numUAVs];
			
			SimParam.processes = new Process[Param.numUAVs];
			
			if (Param.id == null) {	// Could be initialized by a protocol
				Param.id = new long[Param.numUAVs];
			}
		}
		
		for (int i = 0; i < Param.numUAVs; i++) {
			UAVParam.uavCurrentData[i] = new UAVCurrentData();
			UAVParam.uavCurrentStatus[i] = new UAVCurrentStatus();
			UAVParam.lastUTMLocations[i] = new ConcurrentBoundedQueue<>(Location2DUTM.class, UAVParam.LOCATIONS_SIZE);
			UAVParam.currentGeoMission[i] = new ArrayList<>(UAVParam.WP_LIST_SIZE);
			Arrays.fill(UAVParam.customModeToFlightModeMap[i], -1);
			
			UAVParam.newParamIndex.set(i, -1);
			
			UAVParam.lastWaypointReached[i] = new AtomicBoolean();
			
			UAVParam.rcs[i] = new AtomicReference<>();
			UAVParam.overrideOn.set(i, 1);	// Initially RC values can be overridden
			
			UAVParam.target[i] = new AtomicReference<>();
			UAVParam.targetSpeed[i] = new AtomicReference<>();
			
			UAVParam.loadedParams[i] = Collections.synchronizedMap(new HashMap<>(1250));
			UAVParam.lastParamReceivedTime[i] = new AtomicLong();
			
			SimParam.uavUTMPath[i] = new ArrayList<>(SimParam.PATH_INITIAL_SIZE);
			
			if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
				// On the simulator, the UAV identifier is i
				if (Param.id[i] == 0) {	// Could be initialized by a protocol
					Param.id[i] = i;
				}
			}
			
			SimParam.prefix[i] = Text.UAV_ID + " " + Param.id[i] + ": ";
		}
	}
	
	/** Auxiliary method to check the available TCP and UDP ports needed by SITL instances and calculate the number of possible instances. */
	public static Integer[] getSITLPorts() throws InterruptedException, ExecutionException {
		final int PORT_OFFSET = 12;
		// SITL starts using TCP5760 and UDP5501,5502,5503
		ExecutorService es = Executors.newFixedThreadPool(20);
		List<Integer> reservedPort = new ArrayList<>();
		reservedPort.add(UAVParam.broadcastPort);
		reservedPort.add(PCCompanionParam.computerPort);
		reservedPort.add(PCCompanionParam.uavPort);
	    List<Future<PortScanResult>> tcpMain = new ArrayList<>();
	    List<Future<PortScanResult>> udpAux1 = new ArrayList<>();
	    List<Future<PortScanResult>> udpAux2 = new ArrayList<>();
	    List<Future<PortScanResult>> udpAux3 = new ArrayList<>();
	    List<Future<PortScanResult>> irLockAux = new ArrayList<>();
	    List<Integer> ports = new ArrayList<>(UAVParam.MAX_SITL_INSTANCES);
	    int tcpPort = UAVParam.MAV_INITIAL_PORT;
	    int udp1Port = UAVParam.MAV_INTERNAL_PORT[0];
	    int udp2Port = UAVParam.MAV_INTERNAL_PORT[1];
	    int udp3Port = UAVParam.MAV_INTERNAL_PORT[2];
	    int irLockPort = UAVParam.MAV_INTERNAL_PORT[3];
	    int remaining = UAVParam.MAX_SITL_INSTANCES;
	    PortScanResult tcp, udp1, udp2, udp3, irLock;
	    while (irLockPort <= UAVParam.MAX_PORT && remaining > 0) {
	    	for (int i = 0; irLockPort <= UAVParam.MAX_PORT && i < remaining; i++) {
	    		while (reservedPort.contains(tcpPort) || reservedPort.contains(udp1Port)
	    				|| reservedPort.contains(udp2Port) || reservedPort.contains(udp3Port)
	    				|| reservedPort.contains(irLockPort)) {
	    			tcpPort = tcpPort + PORT_OFFSET;
			    	udp1Port = udp1Port + PORT_OFFSET;
			    	udp2Port = udp2Port + PORT_OFFSET;
			    	udp3Port = udp3Port + PORT_OFFSET;
			    	irLockPort = irLockPort + PORT_OFFSET;
	    		}
	    		if (irLockPort <= UAVParam.MAX_PORT) {
	    			tcpMain.add(portIsAvailable(es, tcpPort));
			    	udpAux1.add(portIsAvailable(es, udp1Port));
			    	udpAux2.add(portIsAvailable(es, udp2Port));
			    	udpAux3.add(portIsAvailable(es, udp3Port));
			    	irLockAux.add(portIsAvailable(es, irLockPort));
			    	tcpPort = tcpPort + PORT_OFFSET;
			    	udp1Port = udp1Port + PORT_OFFSET;
			    	udp2Port = udp2Port + PORT_OFFSET;
			    	udp3Port = udp3Port + PORT_OFFSET;
			    	irLockPort = irLockPort + PORT_OFFSET;
	    		}
	    	}
	    	es.awaitTermination(UAVParam.PORT_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
	    	
	    	for (int i = 0; i < tcpMain.size(); i++) {
	    		tcp = tcpMain.get(i).get();
		    	udp1 = udpAux1.get(i).get();
		    	udp2 = udpAux2.get(i).get();
		    	udp3 = udpAux3.get(i).get();
		    	irLock = irLockAux.get(i).get();
	    		if (tcp.isOpen() && udp1.isOpen() && udp2.isOpen() && udp3.isOpen() && irLock.isOpen()) {
	    			ports.add(tcp.getPort());
	    		} else {
	    			if (!tcp.isOpen()) {
	    				System.out.println(Text.PORT_ERROR_3 + " " + tcp.getPort());
	    			}
	    			if (!udp1.isOpen()) {
	    				System.out.println(Text.PORT_ERROR_3 + " " + udp1.getPort());
	    			}
	    			if (!udp2.isOpen()) {
	    				System.out.println(Text.PORT_ERROR_3 + " " + udp2.getPort());
	    			}
	    			if (!udp3.isOpen()) {
	    				System.out.println(Text.PORT_ERROR_3 + " " + udp3.getPort());
	    			}
	    			if (!irLock.isOpen()) {
	    				System.out.println(Text.PORT_ERROR_3 + " " + irLock.getPort());
	    			}
	    		}
	    	}
	    	
	    	tcpMain.clear();
	    	udpAux1.clear();
	    	udpAux2.clear();
	    	udpAux3.clear();
	    	irLockAux.clear();
	    	remaining = UAVParam.MAX_SITL_INSTANCES - ports.size();
	    }
	    
	    es.shutdown();
		return ports.toArray(new Integer[ports.size()]);
	}
	
	/** Auxiliary method to detect asynchronously if a port is in use by the system or any other application. */
	private static Future<PortScanResult> portIsAvailable(final ExecutorService es, final int port) {
	    return es.submit(() -> {
			boolean isAvailable = true;
			try (DatagramSocket ignored = new DatagramSocket(new InetSocketAddress(UAVParam.MAV_NETWORK_IP, port))) {
			} catch (Exception ex) {
				isAvailable = false;
			}

			if (isAvailable) {
				ServerSocket socketTCP = null;
				try {
					socketTCP = new ServerSocket();
					socketTCP.setReuseAddress(false);	// Needed for MacOS
					socketTCP.bind(new InetSocketAddress(UAVParam.MAV_NETWORK_IP, port), 1);
				} catch (Exception ex) {
					isAvailable = false;
				} finally {
					if (socketTCP != null) {
						try {
							socketTCP.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			return new PortScanResult(port, isAvailable);
		});
	}
	
	/** Gets the id when using a real UAV, based on the MAC address. */
	public static long getRealId() {
		List<Long> ids = new ArrayList<>();
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			Enumeration<NetworkInterface> interfaces;
			Formatter formatter;
			String hexId;
			byte[] mac;
			try {
				interfaces = NetworkInterface.getNetworkInterfaces();
				while(interfaces.hasMoreElements()) {
					mac = (interfaces.nextElement()).getHardwareAddress();
					if (mac != null) {
						formatter = new Formatter();
						for (byte b : mac) {
							formatter.format("%02X", b);
						}
						hexId = formatter.toString();
						formatter.close();
						ids.add(Long.parseUnsignedLong(hexId, 16));
					}
				}
			} catch (SocketException | NumberFormatException ignored) { }
		} else if (Param.runningOperatingSystem == Param.OS_LINUX) {
			List<String> interfaces = new ArrayList<>();
	        Pattern pattern = Pattern.compile("^ *(.*):");
	        String hexId;
	        BufferedReader in = null;
	        BufferedReader in2;
	        try (FileReader reader = new FileReader("/proc/net/dev")) {
	            in = new BufferedReader(reader);
	            String line;
	            while( (line = in.readLine()) != null) {
	                Matcher m = pattern.matcher(line);
	                if (m.find()) {
	                	// Ignore loopback interface
	                	String nameFound = m.group(1);
	                	if (!nameFound.equals("lo")) {
	                		// Ignoring disabled interfaces
	                		try (FileReader reader3 = new FileReader("/sys/class/net/" + nameFound + "/operstate")) {
	                			in2 = new BufferedReader(reader3);
	    	                    String state = in2.readLine();
	    	                    if (state.equals("up")) {
	    	                    	interfaces.add(nameFound);
	    	                    }
	                		} catch (IOException ignored) {
	    	                }
	                	}
	                }
	            }
	            // 2. Read the MAC address for each interface
	            for (String interfaceName : interfaces) {
	                try (FileReader reader2 = new FileReader("/sys/class/net/" + interfaceName + "/address")) {
	                    in = new BufferedReader(reader2);
	                    String addr = in.readLine();
	                    hexId = addr.replaceAll("[^a-fA-F0-9]", "");
	                    ids.add(Long.parseUnsignedLong(hexId, 16));
	                } catch (IOException ignored) {
	                }
	            }
	        } catch (IOException ignored) {
	        } finally {
	        	try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ignored) {}
	        }
		} else if (Param.runningOperatingSystem == Param.OS_MAC) {
			Formatter formatter;
			String hexId;
			byte[] mac;
			//NetworkInterface ni;
			InetAddress ip;
			
			try {
				ip = InetAddress.getLocalHost();
				NetworkInterface network = NetworkInterface.getByInetAddress(ip);
				mac = network.getHardwareAddress();
				
				if (mac != null) {
					formatter = new Formatter();
					for (byte b : mac) {
						formatter.format("%02X", b);
					}
					hexId = formatter.toString();
					formatter.close();
					ids.add(Long.parseUnsignedLong(hexId, 16));
				}
				
				
			} catch (SocketException | NumberFormatException ignored) {
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		if (ids.size() == 0) {
			ArduSimTools.closeAll(Text.MAC_ERROR);
		}
		return Collections.max(ids);
	}
	
	/** Gets the folder for temporary files, and creates a RAM disk if necessary. Return null in case of error. */
	public static String defineTemporaryFolder() {
		// If possible, use a virtual RAM drive
		if (SimParam.userIsAdmin) {
			if (Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled) {
				// Check if temporal filesystem is already mounted and dismount
				File ramDiskFile = ArduSimTools.checkDriveMountedWindows();
				if (ramDiskFile != null) {
					if (!ArduSimTools.dismountDriveWindows(ramDiskFile.getAbsolutePath())) {
						return null;
					}
				}
				ramDiskFile = ArduSimTools.mountDriveWindows();
				if (ramDiskFile != null) {
					SimParam.usingRAMDrive = true;
					return ramDiskFile.getAbsolutePath();
				}
			}
			if (Param.runningOperatingSystem == Param.OS_LINUX) {
				String ramDiskPath = "/media/" + SimParam.RAM_DRIVE_NAME;
				File ramDiskFile = new File(ramDiskPath);
				if (ramDiskFile.exists()) {
					// Check if temporal filesystem is already mounted and dismount
					if (ArduSimTools.checkDriveMountedLinuxMac(ramDiskPath)) {
						if (!ArduSimTools.dismountDriveLinux(ramDiskPath)) {
							return null;
						}
					}
				} else {
					// Create the folder
					ramDiskFile.mkdirs();
				}
				if (ArduSimTools.mountDriveLinux(ramDiskPath)) {
					SimParam.usingRAMDrive = true;
					return ramDiskPath;
				}
			}
			if (Param.runningOperatingSystem == Param.OS_MAC) {
				String ramDiskPath = "/tmp/" + SimParam.RAM_DRIVE_NAME;
				File ramDiskFile = new File(ramDiskPath);
				if (ramDiskFile.exists()) {
					// Check if temporal filesystem is already mounted and dismount
					if (ArduSimTools.checkDriveMountedLinuxMac(ramDiskPath)) {
						if (!ArduSimTools.dismountDriveMac(ramDiskPath)) {
							return null;
						}
					}
				} else {
					// Create the folder
					ramDiskFile.mkdirs();
				}
				if (ArduSimTools.mountDriveMac(ramDiskPath)) {
					SimParam.usingRAMDrive = true;
					return ramDiskPath;
				}
			}
		}
		
		// When it is not possible, use physical storage
		if (!SimParam.userIsAdmin || (Param.runningOperatingSystem == Param.OS_WINDOWS && !SimParam.imdiskIsInstalled)) {
			return API.getFileTools().getCurrentFolder().getAbsolutePath();
		}
		return null;
	}
	
	/** If an appropriate virtual RAM drive is already mounted under Windows, it returns its location, or null in case of error. */
	private static File checkDriveMountedWindows() {
		File[] roots = File.listRoots();
		if (roots != null && roots.length > 0) {
			for (File root : roots) {
				String name = FileSystemView.getFileSystemView().getSystemDisplayName(root);
				if (name.contains(SimParam.RAM_DRIVE_NAME.toUpperCase())) {
					return root;
				}
			}
		}
		return null;
	}

	/** Mounts a virtual RAM drive under Windows, and returns the location, or null in case of error. */
	private static File mountDriveWindows() {
		// 1. Find an available drive letter
		File[] roots = File.listRoots();
		if (roots != null && roots.length > 0) {
			List<String> driveLetters = new ArrayList<>();
			for (File root : roots) {
				driveLetters.add("" + root.getAbsolutePath().charAt(0));
			}
			boolean found = true;
			String freeDrive = null;
			for (int i=0; i<SimParam.WINDOWS_DRIVES.length && found; i++) {
				if (!driveLetters.contains(SimParam.WINDOWS_DRIVES[i])) {
					freeDrive = SimParam.WINDOWS_DRIVES[i];
					found = false;
				}
			}
			if (freeDrive == null) {
				ArduSimTools.logGlobal(Text.MOUNT_DRIVE_ERROR_1);
				return null;
			}
			
			// 2. Mount the temporary filesystem
			List<String> commandLine = new ArrayList<>();
			BufferedReader input;
			commandLine.add("imdisk");
			commandLine.add("-a");
			commandLine.add("-s");
			int ramSize;
			if (SimParam.arducopterLoggingEnabled) {
				ramSize = (SimParam.UAV_RAM_DRIVE_SIZE + SimParam.LOGGING_RAM_DRIVE_SIZE)*Param.numUAVs;
			} else {
				ramSize = SimParam.UAV_RAM_DRIVE_SIZE*Param.numUAVs;
			}
			ramSize = Math.max(ramSize, SimParam.MIN_FAT32_SIZE);
			commandLine.add(ramSize + "M");
			commandLine.add("-m");
			commandLine.add(freeDrive + ":");
			commandLine.add("-p");
			commandLine.add("/fs:fat32 /q /v:" + SimParam.RAM_DRIVE_NAME + " /y");
			ProcessBuilder pb = new ProcessBuilder(commandLine);
			try {
				Process p = pb.start();
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
			    while ((line = input.readLine()) != null) {
			        sb.append(line).append("\n");
			    }
				String s = sb.toString();
				if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
					s = s.substring(0, s.length()-1);
				}
				if (s.endsWith("Done.")) {
					return ArduSimTools.checkDriveMountedWindows();
				}
			} catch (IOException ignored) {
			}
		} else {
			ArduSimTools.logGlobal(Text.MOUNT_DRIVE_ERROR_2);
		}
		return null;
	}

	/** Dismounts a virtual RAM drive under Windows. Returns true if the command was successful. */
	private static boolean dismountDriveWindows(String diskPath) {
		List<String> commandLine = new ArrayList<>();
		BufferedReader input;
		commandLine.add("imdisk");
		commandLine.add("-D");
		commandLine.add("-m");
		commandLine.add(diskPath.substring(0, 2));
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
		    while ((line = input.readLine()) != null) {
		        sb.append(line).append("\n");
		    }
			String s = sb.toString();
			if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
				s = s.substring(0, s.length()-1);
			}
			if (s.endsWith("Done.")) {
				return true;
			}
		} catch (IOException ignored) {
		}
		return false;
	}
	
	/** If an appropriate virtual RAM drive is already mounted under Linux and Mac, it returns true if mounted, or false in case of error. */
	private static boolean checkDriveMountedLinuxMac(String diskPath) {
		List<String> commandLine = new ArrayList<>();
		BufferedReader input;
		commandLine.add("df");
		commandLine.add("-ah");
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		boolean found = false;
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
		    while ((line = input.readLine()) != null && !found) {
		    	found = line.endsWith(diskPath);
		    }
			return found;
		} catch (IOException ignored) {
		}
		return false;
	}
	
	/** Mount a virtual RAM drive under Linux. Return true if the command was successful. */
	private static boolean mountDriveLinux(String diskPath) {
		List<String> commandLine = new ArrayList<>();
		BufferedReader input;
		commandLine.add("mount");
		commandLine.add("-t");
		commandLine.add("tmpfs");
		commandLine.add("-o");
		int ramSize;
		if (SimParam.arducopterLoggingEnabled) {
			ramSize = (SimParam.UAV_RAM_DRIVE_SIZE + SimParam.LOGGING_RAM_DRIVE_SIZE)*Param.numUAVs;
		} else {
			ramSize = SimParam.UAV_RAM_DRIVE_SIZE*Param.numUAVs;
		}
		ramSize = Math.max(ramSize, SimParam.MIN_FAT32_SIZE);
		commandLine.add("size=" + ramSize + "M");
		commandLine.add("tmpfs");
		commandLine.add(diskPath);
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
		    while ((line = input.readLine()) != null) {
		        sb.append(line).append("\n");
		    }
			String s = sb.toString();
			if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
				s = s.substring(0, s.length()-1);
			}
			if (s.length() == 0) {
				return true;
			}
		} catch (IOException ignored) {
		}
		return false;
	}
	
	/** Mount a virtual RAM drive under Mac. Return true if the command was successful. */
	private static boolean mountDriveMac(String diskPath) {		
		List<String> commandLine = new ArrayList<>();
		BufferedReader input;
		commandLine.add("hdiutil");
		commandLine.add("attach");
		commandLine.add("-nomount");
		int ramSize;
		if (SimParam.arducopterLoggingEnabled) {
			ramSize = (SimParam.UAV_RAM_DRIVE_SIZE + SimParam.LOGGING_RAM_DRIVE_SIZE)*Param.numUAVs;
		} else {
			ramSize = SimParam.UAV_RAM_DRIVE_SIZE*Param.numUAVs;
		}
		commandLine.add("ram://" + ramSize * 2048);
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
		    while ((line = input.readLine()) != null) {
		        sb.append(line);
		    }
			String s = sb.toString();
			if (s.length() > 0) {
				if (s.charAt(s.length()-1) == '\n') s = s.substring(0, s.length()-1);
				s = s.trim();
			}
			if (s.length() != 0 && p.waitFor() == 0) {
				commandLine.clear();
				commandLine.add("newfs_hfs");
				commandLine.add(s);
				pb = new ProcessBuilder(commandLine);
				p = pb.start();
				if (p.waitFor() == 0) {
					commandLine.clear();
					commandLine.add("mount");
					commandLine.add("-t");
					commandLine.add("hfs");
					commandLine.add(s);
					commandLine.add(diskPath);
					pb = new ProcessBuilder(commandLine);
					p = pb.start();
					if (p.waitFor() == 0) return true;
				}
			}
		} catch (IOException | InterruptedException ignored) {
		}
		return false;
	}
	
	/** Dismounts a virtual RAM drive under Linux. Returns true if the command was successful. */
	private static boolean dismountDriveLinux(String diskPath) {
		List<String> commandLine = new ArrayList<>();
		BufferedReader input;
		commandLine.add("umount");
		commandLine.add(diskPath);
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
		    while ((line = input.readLine()) != null) {
		        sb.append(line).append("\n");
		    }
			String s = sb.toString();
			if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
				s = s.substring(0, s.length()-1);
			}
			if (s.length() == 0) {
				return true;
			}
		} catch (IOException ignored) {
		}
		return false;
	}
	
	/** Dismounts a virtual RAM drive under Mac. Returns true if the command was successful. */
	private static boolean dismountDriveMac(String diskPath) {
		List<String> commandLine = new ArrayList<>();
		commandLine.add("hdiutil");
		commandLine.add("detach");
		commandLine.add(diskPath);
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			if (p.waitFor() == 0) {
				return true;
			}
		} catch (IOException | InterruptedException ignored) {
		}
		return false;
	}
	
	/** Checks if the application is stand alone (.jar) or it is running inside an IDE. */
	public static boolean isRunningFromJar() {
		String filePath = ArduSimTools.class.getResource("/" + ArduSimTools.class.getName().replace('.', '/') + ".class").toString();
		return (filePath.startsWith("jar:") || filePath.startsWith("rsrc:"));
	}
	
	/** Gets an object of the available implementation for the selected protocol.
	 * <p>Returns null if no valid or more than one implementation were found. An error message is already displayed.</p> */
	public static ProtocolHelper getSelectedProtocolInstance() {
		// Target protocol class and object
		ProtocolHelper protocolLaunched = null;
		Class<?>[] validImplementations = ArduSimTools.ProtocolClasses;
		if (validImplementations != null && validImplementations.length > 0) {
			try {
				ProtocolHelper[] protocolImplementations = getProtocolImplementationInstances(validImplementations, ArduSimTools.selectedProtocol);
				if (protocolImplementations == null) {
					ArduSimTools.logGlobal(Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + ArduSimTools.selectedProtocol);
				} else if (protocolImplementations.length > 1) {
					ArduSimTools.logGlobal(Text.PROTOCOL_MANY_IMPLEMENTATIONS_ERROR + ArduSimTools.selectedProtocol);
				} else {
					protocolLaunched = protocolImplementations[0];
				}
			} catch (InstantiationException | IllegalAccessException e) {
				ArduSimTools.logGlobal(Text.PROTOCOL_GETTING_PROTOCOL_CLASSES_ERROR);
			}
		}
		return protocolLaunched;
	}
	
	/** Loads the implemented protocols and retrieves the name of each one.
	 * <p>Protocol names are case-sensitive. Returns null if no valid implementations were found.</p> */
	public static void loadAndStoreProtocols() {
		String[] existingProtocols = loadProtocols();
		if (existingProtocols == null) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.PROTOCOL_DUPLICATED);
			System.exit(1);
		}
		ArduSimTools.ProtocolNames = existingProtocols;
	}

	private static String[] loadProtocols(){
		ArduSimTools.noneProtocolName = "Mission";
		String[] names = null;
		Class<?>[] validImplementations = ArduSimTools.getAnyProtocolImplementations();
		if (validImplementations.length > 0) {
			ArduSimTools.ProtocolClasses = validImplementations;
			// Avoiding more than one implementation of the same protocol and avoiding implementations without name
			Map<String, String> imp = new HashMap<>();
			ProtocolHelper protocol;
			try {
				int size;
				for (Class<?> validImplementation : validImplementations) {
					try {
						protocol = (ProtocolHelper) validImplementation.getDeclaredConstructor().newInstance();
						protocol.setProtocol();
						if (protocol.protocolString != null) {
							size = imp.size();
							imp.put(protocol.protocolString, protocol.protocolString);
							if (imp.size() == size) {
								return null;
							}
						}
					} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
							| SecurityException e) {
						e.printStackTrace();
					}

				}
				if (imp.size() > 0) {
					names = imp.values().toArray(new String[imp.size()]);
					Arrays.sort(names);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				ArduSimTools.logGlobal(Text.PROTOCOL_LOADING_ERROR);
			}
		}
		return names;
	}
	
	/** Gets all classes that extend ProtocolHelper or implement a protocol.
	 * <p>Returns null or an array of size 0 if no valid implementations were found.</p> */
	private static Class<?>[] getAnyProtocolImplementations() {
		Class<?>[] res = null;
		// Get all Java classes included in ArduSim
		List<String> existingClasses = ArduSimTools.getClasses();
		// Get classes that extend ProtocolHelper class
		if (existingClasses != null && existingClasses.size() > 0) {
			res = ArduSimTools.getAllImplementations(existingClasses);
		} else {
			ArduSimTools.logGlobal(Text.PROTOCOL_GETTING_CLASSES_ERROR);
		}
		if (res.length == 0) {
			ArduSimTools.logGlobal(Text.PROTOCOL_GETTING_PROT_CLASSES_ERROR);
		}
		return res;
	}
	
	/** Returns the existing Java classes in the jar file or Eclipse project.
	 * <p>Returns null or empty list if some error happens.</p> */
	private static List<String> getClasses() {
		List<String> existingClasses = null;
		if (isRunningFromJar()) {
			// Process the jar file contents when running from a jar file
			String jar = ArduSimTools.getJarFile();
			if (jar != null) {
				try {
					existingClasses = getClassNamesFromJar(jar);
				} catch (Exception ignored) {}
			}
		} else {
			// Explore .class file tree for class files when running on Eclipse
			File projectFolder = new File(API.getFileTools().getCurrentFolder(), "classes");
			existingClasses = new ArrayList<>();
			String projectFolderPath;
			try {
				projectFolderPath = projectFolder.getCanonicalPath() + File.separator;
				int prefixLength = projectFolderPath.length();	// To remove from the classes path
				getClassNamesFromIDE(existingClasses, projectFolder, prefixLength);
			} catch (IOException ignored) {}
		}
		return existingClasses;
	}
	
	/** Returns the String representation of the jar File the ArduSim instance is running from (path+name).
	 * <p>Returns null if some error happens.</p> */
	private static String getJarFile() {
		Class<Main> c = Main.class;
		CodeSource codeSource = c.getProtectionDomain().getCodeSource();
		String jarFile = null;
		if (codeSource != null && codeSource.getLocation() != null) {
			try {
				jarFile = codeSource.getLocation().toURI().getPath();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} else {
			String path = c.getResource(c.getSimpleName() + ".class").getPath();
			String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
			jarFile = URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8);
		}
		
		return jarFile;
	}
	
	/** Returns the list of classes included in the Jar file.
	 * <p>Returns null or empty list if some error happens.</p> */
	private static List<String> getClassNamesFromJar(String jarFile) throws Exception {
		List<String> res = new ArrayList<>();
		JarInputStream jarFileStream = new JarInputStream(new FileInputStream(jarFile));
		JarEntry jarEntry;
		while (true) {
			jarEntry = jarFileStream.getNextJarEntry();
			if (jarEntry == null) {
				break;
			}
			if ((jarEntry.getName().endsWith(".class"))) {
				String className = jarEntry.getName().replaceAll("/", "\\.");
				if (!className.startsWith("smile.") && !className.startsWith("ch.ethz")
						&& !className.startsWith("org.objenesis")
						&& !className.startsWith("org.javatuples") && !className.startsWith("gnu.io")
						&& !className.startsWith("org.apache") && !className.startsWith("org.mavlink")
						&& !className.startsWith("org.objectweb") && !className.startsWith("org.hyperic")
						&& !className.startsWith("javafx")
						//used to include && !className.startsWith("com")
				) {
					res.add(className.substring(0, className.lastIndexOf('.')));
				}
			}
		}
		try {
			jarFileStream.close();
		} catch (IOException ignored) {}
		return res;
	}
	
	/** Recursively adds classes from the Eclipse project to the existing list.*/
	private static void getClassNamesFromIDE(List<String> res, File folder, int prefixLength) throws IOException {
		List<File> dir = new ArrayList<>();
		String temp, canonicalPath;
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            dir.add(fileEntry);
	        } else {
	        	canonicalPath = fileEntry.getCanonicalPath();
	        	if (canonicalPath.toUpperCase().endsWith(".CLASS")) {
	        		temp = canonicalPath.substring(prefixLength);
		        	temp = temp.substring(0, temp.lastIndexOf("class") - 1); // Remove also the dot
		        	temp = temp.replace(File.separator, ".");
		        	res.add(temp);
	        	}
	        }
	    }
		for (File file : dir) {
			getClassNamesFromIDE(res, file, prefixLength);
		}
	}
	
	/** Returns all the classes that contain a valid implementation of any protocol.
	 * <p>Returns an array of size 0 if no valid implementations were found.</p> */
	private static Class<?>[] getAllImplementations(List<String> existingClasses) {
		String className;
		Class<?> currentClass;
		Map<String, Class<?>> classesMap = new HashMap<>();
		for (String existingClass : existingClasses) {
			className = existingClass;
			try {
				// Ignore special case
				/*
				if (!className.toUpperCase().endsWith("WINREGISTRY")
						&& !className.equals("module-info") ) {
					currentClass = Class.forName(className);
					if (ProtocolHelper.class.isAssignableFrom(currentClass) && !ProtocolHelper.class.equals(currentClass)) {
						classesMap.put(existingClass, currentClass);
					}
				}
				 */
				if(className.startsWith("com.protocols.")){
					currentClass = Class.forName(className);
					if (ProtocolHelper.class.isAssignableFrom(currentClass) && !ProtocolHelper.class.equals(currentClass)) {
						classesMap.put(existingClass, currentClass);
					}
				}
			} catch (ClassNotFoundException e) {
				System.out.println("problems with class " + className);
			}
		}
		Class<?>[] res = new Class<?>[classesMap.size()];
		if (classesMap.size() > 0) {
			int i = 0;
			for (Class<?> aClass : classesMap.values()) {
				res[i] = aClass;
				i++;
			}
		}
		return res;
	}
	
	/** Returns an instance for all the implementations of the selected protocol among all available implementations.
	 * <p>Returns a valid ProtocolHelper object for each implementation. Returns null if no valid implementation was found.</p> */
	private static ProtocolHelper[] getProtocolImplementationInstances(Class<?>[] implementations, String selectedProtocol) throws InstantiationException, IllegalAccessException {
		Class<?> c;
		ProtocolHelper o;
		ProtocolHelper[] res = null;
		List<ProtocolHelper> imp = new ArrayList<>();
		for (Class<?> implementation : implementations) {
			c = implementation;
			try {
				o = (ProtocolHelper) c.getDeclaredConstructor().newInstance();
				o.setProtocol();
				if (o.protocolString.equalsIgnoreCase(selectedProtocol)) {
					imp.add(o);
				}
			} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
			}
		}
		if (imp.size() > 0) {
			res = imp.toArray(new ProtocolHelper[imp.size()]);
		}
		return res;
	}
	
	/** Starts the virtual UAVs. */
	public static void startVirtualUAVs(Pair<Location2DGeo, Double>[] location) {
		
		ArduSimTools.logGlobal(Text.STARTING_UAVS);
		SIM_SPEEDUP = Double.toString(UAVParam.SIM_SPEEDUP);
		try {
			// 1. Under Windows, first close the possible running SITL processes
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				Runtime rt = Runtime.getRuntime();
				rt.exec("taskkill /F /IM " + (new File(SimParam.sitlPath)).getName());
			}

			// 2. Create the temporary folders, one per UAV, and copy files to RAM drive if possible
			File parentFolder = new File(SimParam.tempFolderBasePath);
			try {
				File file1, file2;
				// Trying to speed up locating files on the RAM drive (under Windows)
				if (SimParam.usingRAMDrive && Param.runningOperatingSystem == Param.OS_WINDOWS) {
					file1 = new File(SimParam.sitlPath);
					file2 = new File(parentFolder, file1.getName());
					Files.copy(file1.toPath(), file2.toPath());
					file1 = new File(SimParam.paramPath);
					file2 = new File(parentFolder, file1.getName());
					Files.copy(file1.toPath(), file2.toPath());
				}
				for (int i=0; i<Param.numUAVs; i++) {
					File tempFolder = new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i);
					Path tempPath = tempFolder.toPath();
					// Delete recursively in case it already exists
					if (Files.exists(tempPath)) {
						ArduSimTools.deleteFolder(tempPath);
					}
					Files.createDirectory(tempPath);
					
					// Trying to speed up locating files on the RAM drive (under Linux)
					if (SimParam.usingRAMDrive && (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)) {
						file1 = new File(SimParam.sitlPath);
						file2 = new File(tempFolder, file1.getName());
						Files.copy(file1.toPath(), file2.toPath());
						file1 = new File(SimParam.paramPath);
						file2 = new File(tempFolder, file1.getName());
						Files.copy(file1.toPath(), file2.toPath());
					}
				}
			} catch (Exception e) {
				ArduSimTools.logGlobal(Text.UAVS_START_ERROR_2 + "\n" + parentFolder);
				throw new IOException();
			}

			// 3. Execute each SITL instance
			List<String> commandLine = new ArrayList<>();
			BufferedReader input;
			String s, file1, file2;
			boolean success;
			String tempFolder;
			int tcpPort, udp1Port, udp2Port, udp3Port, irLockPort;
			ArduSim ardusim = API.getArduSim();
			for (int i = 0; i < Param.numUAVs; i++) {
				tempFolder = (new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i)).getAbsolutePath();
				
				tcpPort = UAVParam.mavPort[i];
				udp1Port = tcpPort + UAVParam.MAV_PORT_OFFSET[0];
				udp2Port = tcpPort + UAVParam.MAV_PORT_OFFSET[1];
				udp3Port = tcpPort + UAVParam.MAV_PORT_OFFSET[2];
				irLockPort = tcpPort + UAVParam.MAV_PORT_OFFSET[3];
				
				// Under Windows, launch Cygwin console with arducopter command:
				if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
					commandLine.clear();
					commandLine.add(SimParam.cygwinPath);
					commandLine.add("--login");
					commandLine.add("-i");
					commandLine.add("-c");
					if (SimParam.usingRAMDrive) {
						file1 = (new File(parentFolder, (new File(SimParam.sitlPath)).getName())).getAbsolutePath();
						file2 = (new File(parentFolder, (new File(SimParam.paramPath)).getName())).getAbsolutePath();
						commandLine.add("cd /cygdrive/" + tempFolder.replace("\\", "/").replace(":", "")
								+ ";/cygdrive/" + file1.replace("\\", "/").replace(":", "") + " -S --base-port " + tcpPort
								+ " --com.setup.sim-port-in " + udp1Port + " --com.setup.sim-port-out " + udp2Port + " --rc-in-port " + udp3Port
								+ " --irlock-port " + irLockPort + " --disable-fgview --home "
								+ location[i].getValue0().latitude + "," + location[i].getValue0().longitude
								+ "," + UAVParam.initialAltitude + "," + (location[i].getValue1() / Math.PI * 180) + " --model + --speedup 1 --defaults "
								+ "/cygdrive/" + file2.replace("\\", "/").replace(":", ""));
					} else {
						commandLine.add("cd /cygdrive/" + tempFolder.replace("\\", "/").replace(":", "")
								+ ";/cygdrive/" + SimParam.sitlPath.replace("\\", "/").replace(":", "") + " -S --base-port " + tcpPort
								+ " --com.setup.sim-port-in " + udp1Port + " --com.setup.sim-port-out " + udp2Port + " --rc-in-port " + udp3Port
								+ " --irlock-port " + irLockPort + " --disable-fgview --home "
								+ location[i].getValue0().latitude + "," + location[i].getValue0().longitude
								+ "," + UAVParam.initialAltitude + "," + (location[i].getValue1() / Math.PI * 180) + " --model + --speedup " + SIM_SPEEDUP +" --defaults "
								+ "/cygdrive/" + SimParam.paramPath.replace("\\", "/").replace(":", ""));
					}
				}
				// Under Linux, launch arducopter command:
				if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
					commandLine.clear();
					if (SimParam.usingRAMDrive) {
						commandLine.add((new File(tempFolder, (new File(SimParam.sitlPath)).getName())).getAbsolutePath());
					} else {
						commandLine.add(SimParam.sitlPath);
					}
					commandLine.add("-S");
					commandLine.add("--base-port");
					commandLine.add("" + tcpPort);
					commandLine.add("--sim-port-in");
					commandLine.add("" + udp1Port);
					commandLine.add("--sim-port-out");
					commandLine.add("" + udp2Port);
					commandLine.add("--rc-in-port");	// Flight Gear uses RC input
					commandLine.add("" + udp3Port);
					commandLine.add("--irlock-port");	// Used for sensor input for precision landing (not supported)
					commandLine.add("" + irLockPort);
					commandLine.add("--disable-fgview");	// Flight Gear View disabled for performance
					commandLine.add("--home");
					commandLine.add(location[i].getValue0().latitude + "," + location[i].getValue0().longitude + "," + UAVParam.initialAltitude + "," + (location[i].getValue1() / Math.PI * 180));
					commandLine.add("--model");
					commandLine.add("+");
					commandLine.add("--speedup");
					commandLine.add(SIM_SPEEDUP);
					commandLine.add("--defaults");
					if (SimParam.usingRAMDrive) {
						commandLine.add((new File(tempFolder, (new File(SimParam.paramPath)).getName())).getAbsolutePath());
					} else {
						commandLine.add(SimParam.paramPath);
					}
				}
				
				ProcessBuilder pb = new ProcessBuilder(commandLine);
				if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
					pb.directory(new File(SimParam.tempFolderBasePath, SimParam.TEMP_FOLDER_PREFIX + i));
				}
				SimParam.processes[i] = pb.start();
				// Waiting for SITL to be prepared for a TCP connection
				input = new BufferedReader(new InputStreamReader(SimParam.processes[i].getInputStream()));
				success = false;
				long startTime = System.nanoTime();
				try {
					while (!success) {
						if (input.ready()) {
							s = input.readLine();
							ArduSimTools.logVerboseGlobal(SimParam.prefix[i] + s);
							if (s.contains("Waiting for connection")) {
								ArduSimTools.logVerboseGlobal(SimParam.prefix[i] + Text.SITL_UP);
								success = true;
							}
						} else {
							ardusim.sleep(SimParam.CONSOLE_READ_RETRY_WAITING_TIME);
							if (System.nanoTime() - startTime > SimParam.SITL_STARTING_TIMEOUT) {
								break;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (!success) {
					ArduSimTools.logGlobal(Text.UAVS_START_ERROR_5 + " " + i);
					throw new IOException();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			ArduSimTools.closeAll(Text.APP_NAME + " " + Text.UAVS_START_ERROR_1);
		}
	}

	/** Removes a folder recursively. */
	public static void deleteFolder (Path file) throws Exception {
		try (DirectoryStream<Path> content = Files.newDirectoryStream(file)) {
			Iterator<Path> it = content.iterator();
			Path child;
			while (it.hasNext()) {
				child = it.next();
				if (Files.isDirectory(child)) {
					ArduSimTools.deleteFolder(child);
				} else {
					Files.deleteIfExists(child);
				}
			}
		}
		Files.deleteIfExists(file);
	}
	
	/** Detects the running Operating System. */
	public static void detectOS() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.contains("win")) {
			Param.runningOperatingSystem = Param.OS_WINDOWS;
			ArduSimTools.logVerboseGlobal(Text.OPERATING_SYSTEM_WINDOWS);
		} else if (OS.contains("nux") || OS.contains("nix") || OS.contains("aix")) {
			Param.runningOperatingSystem = Param.OS_LINUX;
			ArduSimTools.logVerboseGlobal(Text.OPERATING_SYSTEM_LINUX);
		}else if (OS.contains("mac")) {
			Param.runningOperatingSystem = Param.OS_MAC;
			ArduSimTools.logVerboseGlobal(Text.OPERATING_SYSTEM_MAC);
		} else {
			ArduSimTools.closeAll(Text.UAVS_START_ERROR_4);
		}
	}

	/** Detects the SITL executable (if it is in the same folder), and Cygwin (the later, only under Windows). */
	public static void locateSITL() {
		FileTools fileTools = API.getFileTools();
		String sitlPath = null;
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			// Cygwin detection
			File cygwinPath = new File(SimParam.CYGWIN_PATH1);
			if (cygwinPath.exists() && cygwinPath.canExecute()) {
				SimParam.cygwinPath = SimParam.CYGWIN_PATH1;
			} else {
				cygwinPath = new File(SimParam.CYGWIN_PATH2);
				if (cygwinPath.exists() && cygwinPath.canExecute()) {
					SimParam.cygwinPath = SimParam.CYGWIN_PATH2;
				}
			}
			if (SimParam.cygwinPath == null) {
				ArduSimTools.closeAll(Text.UAVS_START_ERROR_3 + "\n" + SimParam.CYGWIN_PATH1 + " or,\n" + SimParam.CYGWIN_PATH2);
			}
			sitlPath = fileTools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_WINDOWS_FILE_NAME;
		} else if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
			sitlPath = fileTools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_LINUX_FILE_NAME;
		}
		// SITL detection
		if (Param.runningOperatingSystem == Param.OS_WINDOWS
				|| Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
			String paramPath = fileTools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.PARAM_FILE_NAME;
			File sitlPathFile = new File(sitlPath);
			File paramPathFile = new File(paramPath);
			if (sitlPathFile.exists() && sitlPathFile.canExecute() && paramPathFile.exists()) {
				SimParam.sitlPath = sitlPath;
				SimParam.paramPath = paramPath;
			}
		}
	}
	
	/** Detects whether the program is executed with root/administrator privileges. */
	public static void checkAdminPrivileges() {
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			try {
				String programFiles = System.getenv("ProgramFiles");
				if (programFiles == null) {
					programFiles = "C:\\Program Files";
				}
				File temp = new File(programFiles, SimParam.FAKE_FILE_NAME);
				if (temp.createNewFile()) {
					temp.delete();
					SimParam.userIsAdmin = true;
					return;
				}
			} catch (IOException e) {
				// If fails writing the file, for sure the execution doesn't have administrator privileges
			}
		}
		if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
			String userName = System.getProperty("user.name");
			if (userName.equals("root")) {
				SimParam.userIsAdmin = true;
				return;
			}
			// The user can be root but without root name
			List<String> commandLine = new ArrayList<>();
			BufferedReader input;
			commandLine.add("getent");
			commandLine.add("group");
			commandLine.add("root");
			ProcessBuilder pb = new ProcessBuilder(commandLine);
			try {
				Process p = pb.start();
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
			    while ((line = input.readLine()) != null) {
			        sb.append(line).append("\n");
			    }
				String s = sb.toString();
				if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
					s = s.substring(0, s.length()-1);
				}
				if (s.contains(userName)) {
					SimParam.userIsAdmin = true;
					return;
				}
			} catch (IOException e) {
				// If fails starting the process, for sure the execution doesn't have root privileges
			}
		}
		SimParam.userIsAdmin = false;
	}

	/** Checks if ImDisk RAM Disk Driver is installed. Use only under Windows OS. */
	public static void checkImdiskInstalled() {
		File imdiskFile = new File(System.getenv("windir") + SimParam.IMDISK_PATH);
		// Check if ImDisk executable is present
		if (imdiskFile.exists() && imdiskFile.canExecute()) {
			// Asynchronously check if ImDisk is on Windows 64 and 32bits registry, under LM or CU
			String result = null;
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			
			// Check again with ImDisk Toolkit (other installation pack)
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
		}
		SimParam.imdiskIsInstalled = false;
	}
	
	/**
	 * For ArduSim internal purposes only.
	 * @param message Error message to be shown.
	 */
	public static void closeAll(String message) {
		boolean exiting = ArduSimTools.exiting.getAndSet(true);
		if (!exiting) {
			if (Param.role == ArduSim.MULTICOPTER) {
				System.out.println(Text.FATAL_ERROR + ": " + message);
				if (UAVParam.flightMode != null
						&& UAVParam.flightMode.get(0).getBaseMode() >= UAVParam.MIN_MODE_TO_BE_FLYING) {
					if (!API.getCopter(0).setFlightMode(FlightMode.RTL)) {
						System.out.println(Text.FATAL_ERROR + ": " + Text.FLIGHT_MODE_ERROR_3);
						System.out.flush();
					}
				}
			} else {
				JOptionPane.showMessageDialog(null, message, Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
				if (Param.role == ArduSim.SIMULATOR_GUI) {
					if (Param.simStatus != SimulatorState.CONFIGURING
						&& Param.simStatus != SimulatorState.CONFIGURING_PROTOCOL) {
						ArduSimTools.closeSITL();
					}
				}
			}
			System.exit(1);
		}
	}
	
	/** Closes the SITL simulator instances, and the application. Also the Cygwin consoles if using Windows. */
	public static void shutdown() {
		if (ArduSimTools.storingResults) {
			ArduSimTools.warnGlobal(Text.STORE_WARNING, Text.STORING_NOT_FINISHED);
			return;
		}

		// 1. Ask for confirmation if the experiment has not started or finished
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			boolean reallyClose = true;
			if (Param.simStatus != SimulatorState.TEST_FINISHED) {
				Object[] options = {Text.YES_OPTION, Text.NO_OPTION};
				int result = JOptionPane.showOptionDialog(MainWindow.window.mainWindowFrame,
						Text.CLOSING_QUESTION,
						Text.CLOSING_DIALOG_TITLE,
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[1]);
				if (result!=JOptionPane.YES_OPTION) {
					reallyClose = false;
				}
			}
			if (!reallyClose) return;
		}
		// 2. Change the status and close SITL
		ArduSimTools.logGlobal(Text.CLOSING);
		Param.simStatus = SimulatorState.SHUTTING_DOWN;
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> {
				MainWindow.buttonsPanel.exitButton.setEnabled(false);
				MainWindow.buttonsPanel.statusLabel.setText(Text.SHUTTING_DOWN);
			});
		}
		if(Param.role == ArduSim.SIMULATOR_CLI || Param.role == ArduSim.SIMULATOR_GUI){
			ArduSimTools.closeSITL();
		}
		// 3. Close the application
		ArduSimTools.logGlobal(Text.EXITING);
		try {
			FileDescriptor.out.sync();
		} catch (SyncFailedException ignored) {}
		API.getArduSim().sleep(SimParam.LONG_WAITING_TIME);

		if (Param.role == ArduSim.MULTICOPTER) {
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				try {
					Runtime.getRuntime().exec("shutdown.exe -s -t 0");
				} catch (IOException ignored) {}
			} else if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
				try {
					Runtime.getRuntime().exec("sudo shutdown -h now");
				} catch (IOException ignored) {}
			} else {
				ArduSimTools.logGlobal(Text.SHUTDOWN_ERROR);
			}
		} else if (Param.role == ArduSim.SIMULATOR_GUI) {
			MainWindow.window.mainWindowFrame.dispose();
		}

		System.exit(0);
	}
	
	/** Closes the SITL simulator instances and removes temporary files and folders. */
	public static void closeSITL() {
		// 1. Close Cygwin under Windows or SITL under Linux
		for (int i=0; i<Param.numUAVs; i++) {
			if (SimParam.processes[i] != null) {
				SimParam.processes[i].destroy();
			}
		}

		// 2. Close SITL under Windows
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			Runtime rt = Runtime.getRuntime();
			try {
				Process p = rt.exec("taskkill /F /IM " + (new File(SimParam.sitlPath)).getName());
				try {
					p.waitFor();
				} catch (InterruptedException e) {
				}
				p.destroyForcibly();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// 3. Removes temporary files and folders or dismount virtual RAM drive
		if (SimParam.usingRAMDrive) {
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				// Unmount temporal filesystem
				if (ArduSimTools.checkDriveMountedWindows() != null && !ArduSimTools.dismountDriveWindows(SimParam.tempFolderBasePath)) {
					ArduSimTools.logGlobal(Text.DISMOUNT_DRIVE_ERROR);
				}
			}
			
			if (Param.runningOperatingSystem == Param.OS_LINUX) {
				// Unmount temporal filesystem and remove folder
				if (ArduSimTools.checkDriveMountedLinuxMac(SimParam.tempFolderBasePath) && !ArduSimTools.dismountDriveLinux(SimParam.tempFolderBasePath)) {
					ArduSimTools.logGlobal(Text.DISMOUNT_DRIVE_ERROR);
				}
				File ramDiskFile = new File(SimParam.tempFolderBasePath);
				if (ramDiskFile.exists()) {
					ramDiskFile.delete();
				}
			}
			if (Param.runningOperatingSystem == Param.OS_MAC) {
				// Unmount temporal filesystem and remove folder
				if (ArduSimTools.checkDriveMountedLinuxMac(SimParam.tempFolderBasePath) && !ArduSimTools.dismountDriveMac(SimParam.tempFolderBasePath)) {
					ArduSimTools.logGlobal(Text.DISMOUNT_DRIVE_ERROR);
				}
				File ramDiskFile = new File(SimParam.tempFolderBasePath);
				if (ramDiskFile.exists()) {
					ramDiskFile.delete();
				}
			}
		} else {
			if (SimParam.tempFolderBasePath != null) {
				File parentFolder = new File(SimParam.tempFolderBasePath);
				for (int i=0; i<UAVParam.MAX_SITL_INSTANCES; i++) {
					Path tempFolder = new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i).toPath();
					if (Files.exists(tempFolder)) {
						try {
							deleteFolder(tempFolder);
						} catch (Exception ignored) {
						}
					}
				}
			}
		}
	}

	/** Starts the UAVs controllers threads. */
	public static void startUAVControllers() {
		Param.controllers = new UAVControllerThread[Param.numUAVs];
		for (int i=0; i<Param.numUAVs; i++) {
			Param.controllers[i] = new UAVControllerThread(i);
			Param.controllers[i].start();
		}
		ArduSimTools.logGlobal(Param.numUAVs + " " + Text.CONTROLLERS_STARTED);
	}
	
	/** Waits until the MAVLink link is available in all executing UAVs. */
	public static void waitMAVLink() {
		long time = System.nanoTime();
		boolean allReady = false;
		boolean error = false;
		ArduSim ardusim = API.getArduSim();
		while (!allReady) {
			if (UAVParam.numMAVLinksOnline.get() == Param.numUAVs) {
				allReady = true;
			}
			// On simulation, stop if error happens and they are not online
			if (System.nanoTime() - time > UAVParam.MAVLINK_ONLINE_TIMEOUT) {
				if (Param.role == ArduSim.MULTICOPTER) {
					if (!error) {
						ArduSimTools.logGlobal(Text.MAVLINK_ERROR1);
						error = true;
					}
				} else {
					break;
				}
			}
			if (!allReady) {
				ardusim.sleep(UAVParam.MAVLINK_WAIT);
			}
		}
		if (!allReady) {
			ArduSimTools.closeAll(Text.MAVLINK_ERROR2);
		}
	}

	/** Forces the UAV to send the GPS location. */
	public static void forceGPS() {
		// All UAVs GPS position forced on a different thread, but the first
		GPSEnableThread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new GPSEnableThread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new GPSEnableThread(i);
			}
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1].start();
			}
		}
		GPSEnableThread.forceGPS(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException ignored) {
				}
			}
		}
		if (GPSEnableThread.GPS_FORCED.get() < Param.numUAVs) {
			ArduSimTools.closeAll(Text.INITIAL_CONFIGURATION_ERROR_1);
		}
	}
	
	/** Retrieve all the parameters and the current version of the ArduCopter compilation.
	 * <p>It is assumed that all the multicopters running in the same machine use the same compilation. Returns null if an error happens.</p> */
	public static void getArduCopterParameters(int numUAV) {
		ArduSim ardusim = API.getArduSim();
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
		}
		UAVParam.lastParamReceivedTime[numUAV].set(System.currentTimeMillis());
		UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_REQUEST_ALL_PARAM);
		while (UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_OK
				&& UAVParam.MAVStatus.get(numUAV) != UAVParam.MAV_STATUS_ERROR_ALL_PARAM) {
			ardusim.sleep(UAVParam.COMMAND_WAIT);
			if (System.currentTimeMillis() - UAVParam.lastParamReceivedTime[numUAV].get() > UAVParam.ALL_PARAM_TIMEOUT) {
				UAVParam.MAVStatus.set(numUAV, UAVParam.MAV_STATUS_OK);
			}
		}
		if (UAVParam.MAVStatus.get(numUAV) == UAVParam.MAV_STATUS_ERROR_ALL_PARAM) {
			ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.ARDUCOPTER_VERSION_ERROR_1);
		} else {
			if (numUAV == 0) {
				long start = System.currentTimeMillis();
				while (UAVParam.arducopterVersion.get() == null) {
					ardusim.sleep(UAVParam.COMMAND_WAIT);
					if (System.currentTimeMillis() - start > UAVParam.VERSION_TIMEOUT) {
						ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.ARDUCOPTER_VERSION_ERROR_2);
						return;
					}
				}
				ArduSimTools.logGlobal(Text.ARDUCOPTER_PARAMS + " " + UAVParam.loadedParams[numUAV].size());
				ArduSimTools.logGlobal(Text.ARDUCOPTER_VERSION + " " + UAVParam.arducopterVersion.get());
			} else {
				ArduSimTools.logVerboseGlobal(Text.ARDUCOPTER_PARAMS + " " + UAVParam.loadedParams[numUAV].size());
			}
		}
	}
	
	/** Waits until GPS is available in all executing UAVs. */
	public static void getGPSFix() {
		ArduSimTools.logGlobal(Text.WAITING_GPS);
		ArduSim ardusim = API.getArduSim();
		long time = System.nanoTime();
		boolean startProcess = false;
		boolean error = false;
		while (!startProcess) {
			if (UAVParam.numGPSFixed.get() == Param.numUAVs) {
				startProcess = true;
			}
			if (System.nanoTime() - time > UAVParam.GPS_FIX_TIMEOUT) {
				if (Param.role == ArduSim.MULTICOPTER) {
					if (!error) {
						ArduSimTools.logGlobal(Text.GPS_FIX_ERROR_2);
						error = true;
					}
				} else {
					break;
				}
			}
			if (!startProcess) {
				ardusim.sleep(UAVParam.GPS_FIX_WAIT);
			}
		}
		if (!startProcess) {
			ArduSimTools.closeAll(Text.GPS_FIX_ERROR_1);
		}
		ArduSimTools.logGlobal(Text.GPS_OK);
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(Text.UAVS_ONLINE);
				}
			});
		}
	}
	
	/** Loads missions from a Google Earth kml file.
	 * <p>Returns null if the file is not valid or it is empty.</p> */
	@SuppressWarnings("unchecked")
	public static List<Waypoint>[] loadXMLMissionsFile(File xmlFile) {
		List<Waypoint>[] missions;
		try {
			Waypoint wp;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName(Text.LINE_TAG);
			if (nList.getLength()==0) {
				ArduSimTools.logGlobal(Text.XML_PARSING_ERROR_1);
				return null;
			}
	
			missions = new ArrayList[nList.getLength()];
			String[][] lines = new String[nList.getLength()][];
			// One UAV per line
			String[] aux;
			double lat, lon, z;
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					lines[i] = eElement.getElementsByTagName(Text.COORDINATES_TAG).item(0).getTextContent().trim().split(" ");
					// Check if there are at least two coordinates to define a mission
					if (lines[i].length<2) {
						ArduSimTools.logGlobal(Text.XML_PARSING_ERROR_2 + " " + (i+1));
						return null;
					}
					missions[i] = new ArrayList<>(lines[i].length+2); // Adding fake home, and substitute first real waypoint with take off
					// Add a fake waypoint for the home position (essential but ignored by the flight controller)
					wp = new Waypoint(0, true, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, ArduSimTools.NAV_WAYPOINT_COMMAND, 0, 0, 0, 0, 0, 0, 0, 1);
					missions[i].add(wp);
					int delay = MissionKmlSimProperties.inputMissionDelay;
					// Check the coordinate triplet format
					for (int j=0; j<lines[i].length; j++) {
						aux = lines[i][j].split(",");
						if (aux.length!=3) {
							ArduSimTools.logGlobal(Text.XML_PARSING_ERROR_3);
							return null;
						}
						try {
							lon = Double.parseDouble(aux[0].trim());
							lat = Double.parseDouble(aux[1].trim());
							//  Usually, Google Earth sets z=0
							z = Double.parseDouble(aux[2].trim());
							
							if (UAVParam.overrideAltitude) {
								if (z < UAVParam.minFlyingAltitude) {
									//  Default flying altitude
									z = UAVParam.minFlyingAltitude;
								}
							} else {
								if (z < UAVParam.minAltitude) {
									//  Minimum flying altitude
									z = UAVParam.minAltitude;
									ArduSimTools.logGlobal(Text.XML_PARSING_WARNING + " " + UAVParam.minAltitude);
								}
							}
							
							// Waypoint 0 is home and current
							// Waypoint 1 is take off
							if (j==0) {
								if (Param.role == ArduSim.MULTICOPTER) {
									wp = new Waypoint(1, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											ArduSimTools.NAV_TAKEOFF_COMMAND, 0, 0, 0, 0, 0, 0, z, 1);
									missions[i].add(wp);
									wp = new Waypoint(2, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											ArduSimTools.NAV_WAYPOINT_COMMAND, delay, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
									wp = new Waypoint(1, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											ArduSimTools.NAV_TAKEOFF_COMMAND, 0, 0, 0, 0, lat, lon, z, 1);
									missions[i].add(wp);
								}
							} else {
								if (Param.role == ArduSim.MULTICOPTER) {
									wp = new Waypoint(j+2, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											ArduSimTools.NAV_WAYPOINT_COMMAND, delay, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
									wp = new Waypoint(j+1, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
											ArduSimTools.NAV_WAYPOINT_COMMAND, delay, 0, 0, 0, 
											lat, lon, z, 1);
									missions[i].add(wp);
								}
							}
						} catch (NumberFormatException e) {
							ArduSimTools.logGlobal(Text.XML_PARSING_ERROR_4 + " " + (i+1) + "/" + (j+1));
							return null;
						}
					}
				}
				// Add a last waypoint to land or RTL depending on the input option
				int numSeq = missions[i].get(missions[i].size() - 1).getNumSeq() + 1;
				if (MissionKmlSimProperties.missionEnd.equals(MissionKmlSimProperties.MISSION_END_LAND)) {
					wp = new Waypoint(numSeq, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
							ArduSimTools.NAV_LAND_COMMAND, 0, 0, 0, 0, 0, 0, 0, 0);
					missions[i].add(wp);
					if (Param.role == ArduSim.MULTICOPTER) {
						ArduSimTools.logVerboseGlobal(Text.XML_LAND_ADDED + Param.id[i]);
					}
					if (Param.role == ArduSim.SIMULATOR_GUI) {
						ArduSimTools.logVerboseGlobal(Text.XML_LAND_ADDED + i);
					}
				}
				if (MissionKmlSimProperties.missionEnd.equals(MissionKmlSimProperties.MISSION_END_RTL)) {
					wp = new Waypoint(numSeq, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT,
							ArduSimTools.NAV_RETURN_TO_LAUNCH_COMMAND, 0, 0, 0, 0, 0, 0, 0, 0);
					missions[i].add(wp);
					if (Param.role == ArduSim.MULTICOPTER) {
						ArduSimTools.logVerboseGlobal(Text.XML_RTL_ADDED + Param.id[i]);
					}
					if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
						ArduSimTools.logVerboseGlobal(Text.XML_RTL_ADDED + i);
					}
				}
			}
			
			return missions;
		} catch (ParserConfigurationException | SAXException | IOException ignored) {
		}
		return null;
	}

	/** Loads a mission from a standard QGroundControl file.
	 * <p>Returns null if the file is not valid or it is empty.</p> */
	public static List<Waypoint> loadMissionFile(String path) {
	
		List<String> list = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Check if there are at least take off and a waypoint to define a main.java.com.protocols.mission (first line is header, and wp0 is home)
		// During simulation an additional waypoint is needed in order to stablish the starting location
		if ((Param.role == ArduSim.MULTICOPTER && list.size()<4) || ((Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) && list.size()<5)) {
			ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_1);
			return null;
		}
		// Check the header of the file
		if (!list.get(0).trim().toUpperCase().equals(Text.FILE_HEADER)) {
			ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_2);
			return null;
		}

		List<Waypoint> mission = new ArrayList<>(list.size()-1);
		Waypoint wp;
		String[] line;
		// One line per waypoint
		// The first waypoint is supposed to be a fake point that will be ignored by the flight controller, but it is needed
		wp = new Waypoint(0, true, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, ArduSimTools.NAV_WAYPOINT_COMMAND, 0, 0, 0, 0, 0, 0, 0, 1);
		mission.add(wp);
		for (int i=2; i<list.size(); i++) { // After header and fake home lines
			// Tabular delimited line
			line = list.get(i).split("\t");
			if (line.length!=12) {
				ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_3 + " " + i + " expected 12 tabs but found " + line.length);
				for (String s : line) {
					System.out.println(s);
				}
				return null;
			}
			int numSeq, command;
			MavFrame frame;
			double param1, param2, param3, param4, param5, param6, param7;
			int autoContinue;
			try {
				numSeq = Integer.parseInt(line[0].trim());
				frame = EnumValue.create(MavFrame.class, Integer.parseInt(line[2].trim())).entry();
				command = Integer.parseInt(line[3].trim());
				param1 = Double.parseDouble(line[4].trim());
				param2 = Double.parseDouble(line[5].trim());
				param3 = Double.parseDouble(line[6].trim());
				param4 = Double.parseDouble(line[7].trim());
				param5 = Double.parseDouble(line[8].trim());
				param6 = Double.parseDouble(line[9].trim());
				param7 = Double.parseDouble(line[10].trim());
				autoContinue = Integer.parseInt(line[11].trim());
				
				if (numSeq + 1 != i) {
					ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_6);
					return null;
				}

				EnumValue<MavCmd> c = EnumValue.create(MavCmd.class, command);
				if (numSeq == 1) {
					if ((frame != MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT
							|| command != ArduSimTools.NAV_TAKEOFF_COMMAND.value()
							|| (Param.role == ArduSim.MULTICOPTER && param7 <= 0))) {
						ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_5);
						return null;
					}
					wp = new Waypoint(numSeq, false, frame, c, param1, param2, param3, param4, param5, param6, param7, autoContinue);
					mission.add(wp);
				} else {
					if (Param.role == ArduSim.MULTICOPTER) {
						wp = new Waypoint(numSeq, false, frame, c, param1, param2, param3, param4, param5, param6, param7, autoContinue);
						mission.add(wp);
					} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
						if (numSeq == 2) {
							// Set takeoff coordinates from the first wapoint that has coordinates
							wp = mission.get(mission.size()-1);
							wp.setLatitude(param5);
							wp.setLongitude(param6);
							wp.setAltitude(param7);
						} else {
							numSeq = numSeq - 1;
							wp = new Waypoint(numSeq, false, frame, c, param1, param2, param3, param4, param5, param6, param7, autoContinue);
							mission.add(wp);
						}
					}
				}
			} catch (NumberFormatException e1) {
				ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_4 + " " + i);
				return null;
			}
		}
		
		// Mission validation
		// Currently only TAKEOFF (wp 1), WAYPOINT, SPLINE_WAYPOINT, LAND (wp last) and RETURN_TO_LAUNCH (wp last) are accepted.
		int command;
		for (int i = 2; i < mission.size() - 1; i++) {
			command = mission.get(i).getCommand().value();
			if (command != ArduSimTools.NAV_WAYPOINT_COMMAND.value()
					&& command != ArduSimTools.NAV_SPLINE_WAYPOINT_COMMAND.value()) {
				ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_7);
				return null;
			}
		}
		command = mission.get(mission.size() - 1).getCommand().value();
		if (command != ArduSimTools.NAV_WAYPOINT_COMMAND.value()
					&& command != ArduSimTools.NAV_SPLINE_WAYPOINT_COMMAND.value()
					&& command != ArduSimTools.NAV_LAND_COMMAND.value()
					&& command != ArduSimTools.NAV_RETURN_TO_LAUNCH_COMMAND.value()) {
			ArduSimTools.logGlobal(Text.FILE_PARSING_ERROR_7);
			return null;
		}
		
		return mission;
	}

	/** Loads the first main.java.com.protocols.mission found in a folder.
	 * <p>Priority loading: 1st xml file, 2nd QGRoundControl file, 3rd txt file. Only the first valid file/main.java.com.protocols.mission found is used.
	 * Returns null if no valid main.java.com.protocols.mission was found.</p> */
	public static List<Waypoint> loadMission(File parentFolder) {
		// 1. kml file case
		File[] files = parentFolder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_KML) && pathname.isFile());
		if (files != null && files.length>0) {
			// Only one kml file must be on the current folder
			if (files.length>1) {
				ArduSimTools.closeAll(Text.MISSIONS_ERROR_6);
			}
			List<Waypoint>[] missions = ArduSimTools.loadXMLMissionsFile(files[0]);
			if (missions != null && missions.length>0) {
				ArduSimTools.logGlobal(Text.MISSION_XML_SELECTED + " " + files[0].getName());
				return missions[0];
			}
		}
		
		// 2. kml file not found or not valid. waypoints file case
		files = parentFolder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_WAYPOINTS) && pathname.isFile());
		List<Waypoint> mission;
		if (files != null && files.length>0) {
			// Only one waypoints file must be on the current folder
			if (files.length>1) {
				ArduSimTools.closeAll(Text.MISSIONS_ERROR_7);
			}
			mission = ArduSimTools.loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				ArduSimTools.logGlobal(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		
		// 3. waypoints file not found or not valid. txt file case
		files = parentFolder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_TXT) && pathname.isFile());
		if (files != null && files.length>0) {
			// Not checking single txt file existence, as other file types can use the same extension
			mission = ArduSimTools.loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				ArduSimTools.logGlobal(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		return null;
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, retrieves controller configuration, etc. */
	public static void sendBasicConfiguration1() {
		
		ArduSimTools.logGlobal(Text.SEND_BASIC_CONFIGURATION_1);
		// All UAVs configured on a different thread, but the first
		InitialConfiguration1Thread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new InitialConfiguration1Thread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new InitialConfiguration1Thread(i);
			}
			int size = threads.length;
			int block = 5;
			int uav = 1;
			ArduSim ardusim = API.getArduSim();
			while (uav <= size) {
				while (uav % block != 0 && uav <= size) {
					threads[uav - 1].start();
					uav++;
				}
				if (uav <= size) {
					threads[uav - 1].start();
					uav++;
				}
				ardusim.sleep(200);
			}
		}
		InitialConfiguration1Thread.sendBasicConfiguration(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException ignored) {
				}
			}
		}
		if (InitialConfiguration1Thread.UAVS_CONFIGURED.get() < Param.numUAVs) {
			ArduSimTools.closeAll(Text.INITIAL_CONFIGURATION_ERROR_2);
		}
	}
	
	/** Sends the initial configuration: sends main.java.com.protocols.mission (if any), and lauches the protocol instance initial configuration. */
	public static void sendBasicConfiguration2() {
		
		ArduSimTools.logGlobal(Text.SEND_BASIC_CONFIGURATION_2);
		// All UAVs configured on a different thread, but the first
		InitialConfiguration2Thread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new InitialConfiguration2Thread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new InitialConfiguration2Thread(i);
			}
			int size = threads.length;
			int block = 5;
			int uav = 1;
			ArduSim ardusim = API.getArduSim();
			while (uav <= size) {
				while (uav % block != 0 && uav <= size) {
					threads[uav - 1].start();
					uav++;
				}
				if (uav <= size) {
					threads[uav - 1].start();
					uav++;
				}
				ardusim.sleep(200);
			}
		}
		InitialConfiguration2Thread.sendBasicConfiguration(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException ignored) {
				}
			}
		}
		if (InitialConfiguration2Thread.UAVS_CONFIGURED.get() < Param.numUAVs) {
			ArduSimTools.closeAll(Text.INITIAL_CONFIGURATION_ERROR_3);
		}
	}
	
	/** Method used by ArduSim (forbidden to users) to trigger a waypoint reached event.
	 * <p>This method is NOT thread-safe.</p> */
	public static void triggerWaypointReached(int numUAV, int numSeq) {
		for (WaypointReachedListener listener : ArduSimTools.listeners) {
			listener.onWaypointReachedActionPerformed(numUAV, numSeq);
		}
	}
	
	/** Detects if a UAV is running out of battery. */
	public static void checkBatteryLevel() {
		int depleted = 0;
		double alarmLevel = Double.MAX_VALUE;
		if (Param.role == ArduSim.MULTICOPTER) {
			alarmLevel = UAVParam.lipoBatteryAlarmVoltage;
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			alarmLevel = UAVParam.VIRT_BATTERY_ALARM_VOLTAGE;
		}
		int percentage;
		double voltage;
		boolean isDepleted;
		for (int i=0; i<Param.numUAVs; i++) {
			percentage = UAVParam.uavCurrentStatus[i].getRemainingBattery();
			voltage = UAVParam.uavCurrentStatus[i].getVoltage();
			isDepleted = percentage != -1 && percentage < UAVParam.BATTERY_DEPLETED_THRESHOLD * 100;
			if (voltage != -1 && voltage <alarmLevel) {
				isDepleted = true;
			}
			if (percentage != -1 && voltage != -1) {
				if (Param.role == ArduSim.MULTICOPTER) {
					ArduSimTools.logGlobal(Text.BATTERY_LEVEL + " " + percentage + " % - " + voltage + " V");
				} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
					ArduSimTools.logVerboseGlobal(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " % - " + voltage + " V");
				}
			} else if (percentage != -1) {
				if (Param.role == ArduSim.MULTICOPTER) {
					ArduSimTools.logGlobal(Text.BATTERY_LEVEL + " " + percentage + " %");
				} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
					ArduSimTools.logVerboseGlobal(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " %");
				}
			} else if (voltage != -1) {
				if (Param.role == ArduSim.MULTICOPTER) {
					ArduSimTools.logGlobal(Text.BATTERY_LEVEL + " " + voltage + " V");
				} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
					ArduSimTools.logVerboseGlobal(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + voltage + " V");
				}
			}
			if (isDepleted) {
				if (Param.role == ArduSim.MULTICOPTER) {
					ArduSimTools.logGlobal(Text.BATTERY_FAILING2);
				} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
					depleted++;
				}
			}
		}
		if ((Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) && depleted > 0) {
			ArduSimTools.logGlobal(Text.BATTERY_FAILING + depleted + " " + Text.UAV_ID + "s");
		}
	}
	
	/** Detects if all the UAVs have started the experiment (they are all flying). */
	public static boolean isTestStarted() {
		int length = UAVParam.flightStarted.length();
		for (int i = 0; i < length; i++) {
			if (UAVParam.flightStarted.get(i) == 0) {
				return false;
			}
		}
		return true;
	}
	
	/** Detects if all the UAVs have finished the experiment (they are all on the ground, with engines not armed). */
	public static boolean isTestFinished() {
		boolean finished = true;
		long latest = 0;
		for (int i = 0; i < Param.numUAVs; i++) {
			FlightMode mode = UAVParam.flightMode.get(i);
			if (mode.getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING) {
				if (Param.testEndTime[i] == 0) {
					Param.testEndTime[i] = System.currentTimeMillis();
					if (Param.testEndTime[i] > latest) {
						latest = Param.testEndTime[i];
					}
				}
			} else {
				finished = false;
			}
		}
		Param.latestEndTime = latest;
		return finished;
	}

	/** Builds a String with the experiment results. */
	public static String getTestResults() {
		StringBuilder sb = new StringBuilder(2000);
		// 1. Get experiment length and UAVs global time
		long maxTime = Long.MIN_VALUE;
		for (int i = 0; i < Param.numUAVs; i++) {
			if (Param.testEndTime[i] > maxTime) {
				maxTime = Param.testEndTime[i];
			}
		}
		if (!ArduSimTools.selectedProtocol.equals(ArduSimTools.noneProtocolName)) {
			sb.append(Text.LOG_GLOBAL).append(":\n\n");
		}

		// 2. Global times
		ValidationTools validationTools = API.getValidationTools();
		sb.append(Text.LOG_TOTAL_TIME).append(": ").append(validationTools.timeToString(Param.startTime, maxTime)).append("\n");
		for (int i = 0; i < Param.numUAVs; i++) {
			sb.append("\t").append(Text.UAV_ID).append(" ").append(Param.id[i]).append(": ")
				.append(validationTools.timeToString(Param.startTime, Param.testEndTime[i])).append("\n");
		}

		return sb.toString();
	}
	
	/** Builds a String with the experiment parameters. */
	public static String getTestGlobalConfiguration() {
		StringBuilder sb = new StringBuilder(2000);
		if (Param.role == ArduSim.MULTICOPTER) {
			sb.append("\n").append(Text.GENERAL_PARAMETERS);
			sb.append("\n\t").append(Text.LOG_SPEED).append(UAVParam.initialSpeeds[0]).append(Text.METERS_PER_SECOND);
			sb.append("\n\t").append(Text.VERBOSE_LOGGING_ENABLE).append(" ");
			if (Param.verboseLogging) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			sb.append("\n").append(Text.SIMULATION_PARAMETERS);
			sb.append("\n\t").append(Text.UAV_NUMBER).append(" ").append(Param.numUAVs);
			sb.append("\n\t").append(Text.LOG_SPEED).append(" (").append(Text.METERS_PER_SECOND).append("):\n\t");
			for (int i=0; i<Param.numUAVs; i++) {
				sb.append(UAVParam.initialSpeeds[i]);
				if (i%10 == 9) {
					sb.append("\n\t");
				} else if (i != Param.numUAVs - 1) {
					sb.append(", ");
				}
			}
			sb.append("\n").append(Text.PERFORMANCE_PARAMETERS);
			sb.append("\n\t").append(Text.SCREEN_REFRESH_RATE).append(" ").append(SimParam.screenUpdatePeriod).append(" ").append(Text.MILLISECONDS);
			sb.append("\n\t").append(Text.REDRAW_DISTANCE).append(" ").append(SimParam.minScreenMovement).append(" ").append(Text.PIXELS);
			sb.append("\n\t").append(Text.LOGGING).append(" ");
			if (SimParam.arducopterLoggingEnabled) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
			sb.append("\n\t").append(Text.BATTERY).append(" ");
			if (UAVParam.batteryCapacity != UAVParam.VIRT_BATTERY_MAX_CAPACITY) {
				sb.append(Text.YES_OPTION);
				sb.append("\n\t\t").append(UAVParam.batteryCapacity).append(" ").append(Text.BATTERY_CAPACITY);
			} else {
				sb.append(Text.NO_OPTION);
			}
			sb.append("\n\t").append(Text.CPU_MEASUREMENT_ENABLED).append(" ");
			if (Param.measureCPUEnabled) {
				sb.append(Text.OPTION_ENABLED);
				Iterator<CPUData> data = Param.cpu.iterator();
				CPUData d;
				int count = 0;
				double global = 0.0;
				double processOneCore = 0.0;
				while (data.hasNext()) {
					d = data.next();
					if (d.simState.equals(Param.SimulatorState.TEST_IN_PROGRESS.name())) {
						count++;
						global = global + d.globalCPU;
						processOneCore = processOneCore + d.processCPU;
					}
				}
				if (count > 0) {
					if (count > 1) {
						global = global / count;
						processOneCore = processOneCore / count;
					}
					ValidationTools validationTools = API.getValidationTools();
					sb.append("\n\t\tDuring the experiment:")
						.append("\n\t\t\tGlobalCPU: ").append(validationTools.roundDouble(global, 3))
						.append("%\n\t\t\tGlobalCPU(oneCore): ").append(validationTools.roundDouble(global * Param.numCPUs, 3))
						.append("%\n\t\t\tJavaCPU: ").append(validationTools.roundDouble(processOneCore/Param.numCPUs, 3))
						.append("%\n\t\t\tJavaCPU(oneCore): ").append(validationTools.roundDouble(processOneCore, 3)).append("%");
				}
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
			sb.append("\n").append(Text.GENERAL_PARAMETERS);
			sb.append("\n\t").append(Text.VERBOSE_LOGGING_ENABLE).append(" ");
			if (Param.verboseLogging) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
		}
		sb.append("\n").append(Text.UAV_PROTOCOL_USED).append(" ").append(ArduSimTools.selectedProtocol);
		sb.append("\n").append(Text.COMMUNICATIONS);
		sb.append(LowLevelCommLink.getCommLink(0).toString());
		
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			sb.append("\n").append(Text.COLLISION_PARAMETERS);
			boolean collisionCheck = UAVParam.collisionCheckEnabled;
			sb.append("\n\t").append(Text.COLLISION_ENABLE).append(" ");
			if (collisionCheck) {
				sb.append(Text.YES_OPTION);
				sb.append("\n\t").append(Text.COLLISION_PERIOD).append(" ").append(UAVParam.collisionCheckPeriod).append(" ").append(Text.SECONDS);
				sb.append("\n\t").append(Text.COLLISION_DISTANCE).append(" ").append(UAVParam.collisionDistance).append(" ").append(Text.METERS);
				sb.append("\n\t").append(Text.COLLISION_ALTITUDE).append(" ").append(UAVParam.collisionAltitudeDifference).append(" ").append(Text.METERS);
			} else {
				sb.append(Text.NO_OPTION);
			}
			sb.append("\n").append(Text.WIND).append(" ");
			if (Param.windSpeed == Param.DEFAULT_WIND_SPEED
					&& Param.windDirection == Param.DEFAULT_WIND_DIRECTION) {
				// There is no wind
				sb.append(Text.NO_OPTION);
			} else {
				sb.append(Text.YES_OPTION);
				sb.append("\n\t").append(Text.WIND_SPEED).append(" ").append(Param.windSpeed).append(" ").append(Text.METERS_PER_SECOND);
				sb.append("\n\t").append(Text.WIND_DIRECTION).append(" ").append(Param.windDirection).append(" ").append(Text.DEGREE_SYMBOL);
			}
		}
		
		sb.append("\n").append(Text.ADDITIONAL_PARAMETERS);
		sb.append("\n\t").append(Param.KML_MIN_ALTITUDE).append("=").append(UAVParam.minAltitude);
		sb.append("\n\t").append(Param.KML_OVERRIDE_ALTITUDE).append("=").append(UAVParam.overrideAltitude);
		if(UAVParam.overrideAltitude) {
			sb.append("\n\t").append(Param.KML_ALTITUDE).append("=").append(UAVParam.minFlyingAltitude);
		}
		sb.append("\n\t").append(Param.KML_MISSION_END).append("=").append(MissionKmlSimProperties.missionEnd);
		sb.append("\n\t").append(Param.KML_WAYPOINT_DELAY).append("=").append(MissionKmlSimProperties.inputMissionDelay);
		if (MissionKmlSimProperties.inputMissionDelay != 0) {
			sb.append("\n\t").append(Param.KML_WAYPOINT_DISTANCE).append("=").append(MissionKmlSimProperties.distanceToWaypointReached);
		}
		
		if (Param.role == ArduSim.MULTICOPTER) {
			sb.append("\n\t").append(Text.REAL_COMMUNICATIONS_PARAMETERS);
			sb.append("\n\t\t").append(Param.BROADCAST_IP).append("=").append(UAVParam.broadcastIP);
			sb.append("\n\t\t").append(Param.BROADCAST_PORT).append("=").append(UAVParam.broadcastPort);
			sb.append("\n\t\t").append(Param.COMPUTER_PORT).append("=").append(PCCompanionParam.computerPort);
			sb.append("\n\t\t").append(Param.UAV_PORT).append("=").append(PCCompanionParam.uavPort);
			
			sb.append("\n\t").append(Text.SERIAL_COMMUNICATIONS_PARAMETERS);
			sb.append("\n\t\t").append(Param.SERIAL_PORT).append("=").append(UAVParam.serialPort);
			sb.append("\n\t\t").append(Param.BAUD_RATE).append("=").append(UAVParam.baudRate);
			
			sb.append("\n\t").append(Text.BATTERY_PARAMETERS);
			sb.append("\n\t\t").append(Param.BATTERY_CELLS).append("=").append(UAVParam.lipoBatteryCells);
			sb.append("\n\t\t").append(Param.BATTERY_CAPACITY).append("=").append(UAVParam.lipoBatteryCapacity);
		}
		if(UAVParam.groundFormation.get() != null) {
			sb.append("\n\t").append(Text.FORMATION_PARAMETERS);
			if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
				sb.append("\n\t\t").append(Param.GROUND_FORMATION).append("=").append(UAVParam.groundFormation.get().getLayout());
				sb.append("\n\t\t").append(Param.GROUND_DISTANCE).append("=").append(UAVParam.groundDistanceBetweenUAV);
			}
			sb.append("\n\t\t").append(Param.AIR_FORMATION).append("=").append(UAVParam.airFormation.get().getLayout());
			sb.append("\n\t\t").append(Param.AIR_DISTANCE).append("=").append(UAVParam.airDistanceBetweenUAV);
			sb.append("\n\t\t").append(Param.LAND_DISTANCE).append("=").append(UAVParam.landDistanceBetweenUAV);
		}
		return sb.toString();
	}

	/** Logs to files the UAV path and general information. */
	public static long storeLogAndPath(String folder, String baseFileName) {
		// 1. Find the first data stored from each UAV during setup and experiment
		long firstSetupNanoTime = Long.MAX_VALUE;
		long firstExperimentNanoTime = Long.MAX_VALUE;
		long nanoTime;
		boolean found;
		LogPoint searching;
		int inSetup = SimulatorState.SETUP_IN_PROGRESS.getStateId();
		int inExperiment = SimulatorState.TEST_IN_PROGRESS.getStateId();
		int state;
		for (int i = 0; i < Param.numUAVs; i++) {
			found = false;
			for (int j = 0; j < SimParam.uavUTMPath[i].size() && !found; j++) {
				searching = SimParam.uavUTMPath[i].get(j);
				state = searching.getSimulatorState();
				nanoTime = searching.getNanoTime();
				if (state == inSetup && nanoTime < firstSetupNanoTime) {
					firstSetupNanoTime = nanoTime;
				}
				if (state == inExperiment) {
					if (nanoTime < firstExperimentNanoTime) {
						firstExperimentNanoTime = nanoTime;
					}
					found = true;
				}
				
			}
		}
		// Nothing to store if setup and experiment where not recorded
		if (firstSetupNanoTime == Long.MAX_VALUE && firstExperimentNanoTime == Long.MAX_VALUE) {
			ArduSimTools.warnGlobal(Text.STORE_WARNING, Text.STORE_PATH_ERROR);
			return firstExperimentNanoTime;
		}
		
		// 2. Storing UAV path during setup and experiment, and AutoCAD path files
		File file1, file2, file3;
		File file4;
		StringBuilder sb1, sb3, sb2;
		StringBuilder sb4;
		LogPoint sp;
		LogPoint spPrev;
		double x, y, z, zRel, a;
		double time;
		boolean firstSetupData, setupDataPresent, uavSetupDataPresent;
		boolean firstExperimentData, experimentDataPresent, uavExperimentDataPresent;
		double lastTime = 0;
		File file5, file6;
		StringBuilder sb5 = new StringBuilder(2000);
		sb5.append("<?xml version=\"1.0\"?>\n<kml xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
			.append("\t<Document>\n");
		StringBuilder sbSetup;
		StringBuilder sb6 = new StringBuilder(2000);
		sb6.append("<?xml version=\"1.0\"?>\n<kml xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
			.append("\t<Document>\n");
		StringBuilder sbExperiment;
		String[] colors = new String[] {"FF000000", "FFFF0000", "FF0000FF", "FF00FF00", "FFFFFF00",
				"FFFF00FF", "FF00A5FF", "FFCBC0FF", "FF00FFFF"};
		Location2DGeo geo;
		boolean newData;
		
		setupDataPresent = false;
		experimentDataPresent = false;
		FileTools fileTools = API.getFileTools();
		ValidationTools validationTools = API.getValidationTools();
		boolean coordinateError = false;
		for (int i=0; i<Param.numUAVs; i++) {
			file1 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.PATH_TEST_SUFIX);
			sb1 = new StringBuilder(2000);
			sb1.append("x(m),y(m),z(m),zRelative(m),heading(rad),t(s),s(m/s),a(m/s\u00B2),\u0394d(m),d(m)\n");
			file2 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.PATH_SETUP_SUFIX);
			sb2 = new StringBuilder(2000);
			sb2.append("x(m),y(m),z(m),zRelative(m),heading(rad),t(s)\n");
			file3 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.PATH_2D_SUFIX);
			sb3 = new StringBuilder(2000);
			sb3.append("._PLINE\n");
			sbSetup = new StringBuilder(2000);
			sbSetup.append("\t\t<Placemark>\n\t\t\t<name>UAV ").append(Param.id[i])
				.append("</name>\n\t\t\t<Style>\n\t\t\t\t<LineStyle>\n\t\t\t\t\t<color>").append(colors[i % colors.length])
				.append("</color>\n\t\t\t\t\t<colorMode>normal</colorMode>\n\t\t\t\t\t<width>4</width>\n\t\t\t\t</LineStyle>\n")
				.append("\t\t\t</Style>\n\t\t\t<LineString>\n\t\t\t\t<extrude>0</extrude>\n")
				.append("\t\t\t\t<altitudeMode>absolute</altitudeMode>\n\t\t\t\t<coordinates>");
			
			sbExperiment = new StringBuilder(2000);
			sbExperiment.append("\t\t<Placemark>\n\t\t\t<name>UAV ").append(Param.id[i])
				.append("</name>\n\t\t\t<Style>\n\t\t\t\t<LineStyle>\n\t\t\t\t\t<color>").append(colors[i % colors.length])
				.append("</color>\n\t\t\t\t\t<colorMode>normal</colorMode>\n\t\t\t\t\t<width>4</width>\n\t\t\t\t</LineStyle>\n")
				.append("\t\t\t</Style>\n\t\t\t<LineString>\n\t\t\t\t<extrude>0</extrude>\n")
				.append("\t\t\t\t<altitudeMode>absolute</altitudeMode>\n\t\t\t\t<coordinates>");

			int j = 0;			// Position in the path list
			double dist = 0;	// Accumulated distance to origin
			double d;
			spPrev = null;

			file4 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.PATH_3D_SUFIX);
			sb4 = new StringBuilder(2000);
			sb4.append("._3DPOLY\n");


			firstSetupData = true;
			uavSetupDataPresent = false;
			firstExperimentData  = true;
			uavExperimentDataPresent = false;
			while (j<SimParam.uavUTMPath[i].size()) {
				sp = SimParam.uavUTMPath[i].get(j);
				// Calculus of the unfiltered acceleration
				if (j == 0) {
					a = 0.0;
				} else {
					a = (sp.getSpeed() - SimParam.uavUTMPath[i].get(j-1).getSpeed())/
							(sp.getNanoTime() - SimParam.uavUTMPath[i].get(j-1).getNanoTime())*1000000000L;
				}
				state = sp.getSimulatorState();
				
				// Consider only not repeated locations and during setup
				if (state == inSetup) {
					sp.setTime(firstSetupNanoTime);
					time = sp.getTime();
					x = validationTools.roundDouble(sp.x, 3);
					y = validationTools.roundDouble(sp.y, 3);
					z = validationTools.roundDouble(sp.z, 3);
					zRel = validationTools.roundDouble(sp.zRel, 3);
					if (firstSetupData) {
						// Adding 0 point
						sb2.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
							.append(",").append(sp.getHeading()).append(",").append(0).append("\n");
						// At the beginning we may need to add a 0 time point
						if (time != 0) {
							sb2.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
								.append(",").append(sp.getHeading()).append(",").append(time).append("\n");
						}
						spPrev = sp;
						firstSetupData = false;
						try {
							geo = sp.getGeo();
							sbSetup.append(geo.longitude).append(",").append(geo.latitude).append(",").append(z);
							uavSetupDataPresent = true;
						} catch (LocationNotReadyException e) {
							if (!coordinateError) {
								e.printStackTrace();
								coordinateError = true;
							}
						}
					} else {
						if (spPrev == null || sp.x!=spPrev.x || sp.y!=spPrev.y || sp.z!=spPrev.z) {
							sb2.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
								.append(",").append(sp.getHeading()).append(",").append(time).append("\n");
							spPrev = sp;
							try {
								geo = sp.getGeo();
								sbSetup.append(" ").append(geo.longitude).append(",").append(geo.latitude).append(",").append(z);
								uavSetupDataPresent = true;
							} catch (LocationNotReadyException e) {
								if (!coordinateError) {
									e.printStackTrace();
									coordinateError = true;
								}
							}
						}
					}
				}

				// Consider only not repeated locations and under test
				if (state == inExperiment) {
					sp.setTime(firstExperimentNanoTime);
					time = sp.getTime();
					if (time > lastTime) {
						lastTime = time;
					}
					x = validationTools.roundDouble(sp.x, 3);
					y = validationTools.roundDouble(sp.y, 3);
					z = validationTools.roundDouble(sp.z, 3);
					zRel = validationTools.roundDouble(sp.zRel, 3);
					
					newData = false;
					if (firstExperimentData) {
						// Adding 0 point
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
							.append(",").append(sp.getHeading()).append(",").append(0).append(",")
							.append(validationTools.roundDouble(sp.getSpeed(), 3)).append(",").append(validationTools.roundDouble(a, 3))
							.append(",0.000,0.000\n");
						sb3.append(x).append(",").append(y).append("\n");
						sb4.append(x).append(",").append(y).append(",").append(z).append("\n");

						// At the beginning we may need to add a 0 time point
						if (time != 0) {
							sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
								.append(",").append(sp.getHeading()).append(",").append(time).append(",")
								.append(validationTools.roundDouble(sp.getSpeed(), 3)).append(",").append(validationTools.roundDouble(a, 3))
								.append(",0.000,0.000\n");
						}
						spPrev = sp;
						firstExperimentData = false;
						try {
							geo = sp.getGeo();
							sbExperiment.append(geo.longitude).append(",").append(geo.latitude).append(",").append(z);
							uavExperimentDataPresent = true;
						} catch (LocationNotReadyException e) {
							if (!coordinateError) {
								e.printStackTrace();
								coordinateError = true;
							}
						}
					} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
						// Moved horizontally
						d = sp.distance(spPrev);
						dist = dist + d;
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
							.append(",").append(sp.getHeading()).append(",").append(time).append(",")
							.append(validationTools.roundDouble(sp.getSpeed(), 3)).append(",").append(validationTools.roundDouble(a, 3))
							.append(",").append(validationTools.roundDouble(d, 3)).append(",").append(validationTools.roundDouble(dist, 3))
							.append("\n");
						sb3.append(x).append(",").append(y).append("\n");
						sb4.append(x).append(",").append(y).append(",").append(z).append("\n");

						spPrev = sp;
						newData = true;
					} else if (sp.z!=spPrev.z) {
						// Only moved vertically
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(zRel)
							.append(",").append(sp.getHeading()).append(",").append(time).append(",")
							.append(validationTools.roundDouble(sp.getSpeed(), 3)).append(",").append(validationTools.roundDouble(a, 3))
							.append(",0.0,").append(validationTools.roundDouble(dist, 3)).append("\n");

						sb4.append(x).append(",").append(y).append(",").append(z).append("\n");

						spPrev = sp;
						newData = true;
					}
					if (newData) {
						try {
							geo = sp.getGeo();
							sbExperiment.append(" ").append(geo.longitude).append(",").append(geo.latitude).append(",").append(z);
							uavExperimentDataPresent = true;
						} catch (LocationNotReadyException e) {
							if (!coordinateError) {
								e.printStackTrace();
								coordinateError = true;
							}
						}
						
					}
				}
				j++;
			}
			fileTools.storeFile(file1, sb1.toString());
			fileTools.storeFile(file2, sb2.toString());
			sb3.append("\n");
			fileTools.storeFile(file3, sb3.toString());
			sb4.append("\n");
			fileTools.storeFile(file4, sb4.toString());
			if (uavSetupDataPresent && !coordinateError) {
				sb5.append(sbSetup.toString()).append("</coordinates>\n\t\t\t</LineString>\n\t\t</Placemark>\n");
				setupDataPresent = true;
			}
			if (uavExperimentDataPresent && !coordinateError) {
				sb6.append(sbExperiment.toString()).append("</coordinates>\n\t\t\t</LineString>\n\t\t</Placemark>\n");
				experimentDataPresent = true;
			}
		}
		if (setupDataPresent && !coordinateError) {
			sb5.append("\t</Document>\n</kml>");
			file5 = new File(folder, baseFileName + "_setup_path_Google_Earth.kmz");
			try {
				FileOutputStream out = new FileOutputStream(file5);
				ZipOutputStream stream = new ZipOutputStream(out);
				ZipEntry zipEntry = new ZipEntry(baseFileName + "_setup_path_Google_Earth.kml");
				stream.putNextEntry(zipEntry);
				stream.write(sb5.toString().getBytes(StandardCharsets.UTF_8));
				stream.closeEntry();
				stream.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (experimentDataPresent && !coordinateError) {
			sb6.append("\t</Document>\n</kml>");
			file6 = new File(folder, baseFileName + "_test_path_Google_Earth.kmz");
			try {
				FileOutputStream out = new FileOutputStream(file6);
				ZipOutputStream stream = new ZipOutputStream(out);
				ZipEntry zipEntry = new ZipEntry(baseFileName + "_test_path_Google_Earth.kml");
				stream.putNextEntry(zipEntry);
				stream.write(sb6.toString().getBytes(StandardCharsets.UTF_8));
				stream.closeEntry();
				stream.close();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Storing mobility files, only during the experiment
		File file7, file8, file9, file10;
		StringBuilder sb7, sb8, sb9, sb10;
		file9 = new File(folder, baseFileName + "_" + Text.MOBILITY_OMNET_SUFIX_2D);
		file10 =new File(folder, baseFileName + "_" + Text.MOBILITY_OMNET_SUFIX_3D);
		sb9 = new StringBuilder(2000);
		sb10 = new StringBuilder(2000);
		Double t;
		for (int i = 0; i < Param.numUAVs; i++) {
			file7 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.MOBILITY_NS2_SUFIX_2D);
			file8 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.MOBILITY_NS2_SUFIX_3D);
			sb7 = new StringBuilder(2000);
			sb8 = new StringBuilder(2000);
			firstExperimentData = true;
			spPrev = null;
			for (int j = 0; j < SimParam.uavUTMPath[i].size(); j++) {
				sp = SimParam.uavUTMPath[i].get(j);
				state = sp.getSimulatorState();
				if (state == inExperiment) {
					t = sp.getTime();
					if (t == null) {
						sp.setTime(firstExperimentNanoTime);
						t = sp.getTime();
					}
					time = t;
					if (time <= lastTime) {
						x = validationTools.roundDouble(sp.x, 2);
						y = validationTools.roundDouble(sp.y, 2);
						z = validationTools.roundDouble(sp.z, 2);
						if (firstExperimentData) {
							// Adding 0 point
							sb7.append("$node_(0) set X_ ").append(x).append("\n");
							sb7.append("$node_(0) set Y_ ").append(y).append("\n");
							sb8.append("$node_(0) set X_ ").append(x).append("\n");
							sb8.append("$node_(0) set Y_ ").append(y).append("\n");
							sb8.append("$node_(0) set Z_ ").append(z).append("\n");
							sb9.append("0 ").append(x).append(" ").append(y);
							sb10.append("0 ").append(x).append(" ").append(y).append(" ").append(z);
							// At the beginning we may need to add a 0 time point
							if (time != 0) {
								sb7.append("$ns_ at ").append(time).append(" $node_(0) set X_ ").append(x).append("\n");
								sb7.append("$ns_ at ").append(time).append(" $node_(0) set Y_ ").append(y).append("\n");
								sb8.append("$ns_ at ").append(time).append(" $node_(0) set X_ ").append(x).append("\n");
								sb8.append("$ns_ at ").append(time).append(" $node_(0) set Y_ ").append(y).append("\n");
								sb8.append("$ns_ at ").append(time).append(" $node_(0) set Z_ ").append(z).append("\n");
								sb9.append(" ").append(time).append(" ").append(x).append(" ").append(y);
								sb10.append(" ").append(time).append(" ").append(x).append(" ").append(y).append(" ").append(z);
							}
							spPrev = sp;
							firstExperimentData = false;
						} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
							sb7.append("$ns_ at ").append(time).append(" $node_(0) set X_ ").append(x).append("\n");
							sb7.append("$ns_ at ").append(time).append(" $node_(0) set Y_ ").append(y).append("\n");
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set X_ ").append(x).append("\n");
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set Y_ ").append(y).append("\n");
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set Z_ ").append(z).append("\n");
							sb9.append(" ").append(time).append(" ").append(x).append(" ").append(y);
							sb10.append(" ").append(time).append(" ").append(x).append(" ").append(y).append(" ").append(z);
							spPrev = sp;
						} else if (sp.z!=spPrev.z) {
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set X_ ").append(x).append("\n");
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set Y_ ").append(y).append("\n");
							sb8.append("$ns_ at ").append(time).append(" $node_(0) set Z_ ").append(z).append("\n");
							sb10.append(" ").append(time).append(" ").append(x).append(" ").append(y).append(" ").append(z);
							spPrev = sp;
						}
					}
				}
			}
			if (spPrev.getTime() < lastTime) {
				time = spPrev.getTime();
				x = validationTools.roundDouble(spPrev.x, 2);
				y = validationTools.roundDouble(spPrev.y, 2);
				z = validationTools.roundDouble(spPrev.z, 2);
				sb7.append("$ns_ at ").append(lastTime).append(" $node_(0) set X_ ").append(x).append("\n");
				sb7.append("$ns_ at ").append(lastTime).append(" $node_(0) set Y_ ").append(y).append("\n");
				sb8.append("$ns_ at ").append(lastTime).append(" $node_(0) set X_ ").append(x).append("\n");
				sb8.append("$ns_ at ").append(lastTime).append(" $node_(0) set Y_ ").append(y).append("\n");
				sb8.append("$ns_ at ").append(lastTime).append(" $node_(0) set Z_ ").append(z).append("\n");
				sb9.append(" ").append(lastTime).append(" ").append(x).append(" ").append(y);
				sb10.append(" ").append(lastTime).append(" ").append(x).append(" ").append(y).append(" ").append(z);
			}
			
			fileTools.storeFile(file7, sb7.toString());
			fileTools.storeFile(file8, sb8.toString());
			
			sb9.append("\n");
			sb10.append("\n");
		}
		
		fileTools.storeFile(file9, sb9.toString());
		fileTools.storeFile(file10, sb10.toString());
		
		return firstExperimentNanoTime;
	}
	
	/** Stores the experiment results. */
	public static void storeResults(String results, File file) {
		ArduSimTools.storingResults = true;
		ArduSimTools.logGlobal(Text.STORING);
		
		// Extracting the name of the file without path or extension for later logging
		String folder = file.getParent();
		String baseFileName = file.getName();
		FileTools fileTools = API.getFileTools();
		String extension = fileTools.getFileExtension(file);
		if (extension.length() > 0 && extension.equalsIgnoreCase(Text.FILE_EXTENSION_TXT)) {
			baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(extension) - 1); // Remove also the dot
		}
		
		// 1. Store results
		// Add extension if needed
		if (!file.getAbsolutePath().toUpperCase().endsWith("." + Text.FILE_EXTENSION_TXT.toUpperCase())) {
			file = new File(file.toString() + "." + Text.FILE_EXTENSION_TXT);
		}
		// Store simulation and protocol progress information and configuration
		fileTools.storeFile(file, results);
		
		// 2. Store the UAV path and general information
		long startTestNanoTime = ArduSimTools.storeLogAndPath(folder, baseFileName);
		
		// 3. Store missions when used
		ArduSimTools.logMission(folder, baseFileName);
		
		// 4. Store CPU utilization
		if (Param.measureCPUEnabled) {
			ArduSimTools.logCPUUtilization(folder, baseFileName);
		}
		
		// 5. Store protocol specific information
		if (startTestNanoTime != Long.MAX_VALUE) {
			ArduSimTools.selectedProtocolInstance.logData(folder, baseFileName, startTestNanoTime);
		}
		
		// 6. Store ArduCopter logs if needed
		if ((Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) && SimParam.arducopterLoggingEnabled) {
			File logSource, logDestination;
			File parentFolder = new File(SimParam.tempFolderBasePath);
			for (int i=0; i<Param.numUAVs; i++) {
				File tempFolder = new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i);
				if (tempFolder.exists()  ) {
					logSource = new File(new File(tempFolder, SimParam.LOG_FOLDER), SimParam.LOG_SOURCE_FILENAME + "." + SimParam.LOG_SOURCE_FILEEXTENSION);
					logDestination = new File(folder, baseFileName + "_" + i + "_" + SimParam.LOG_DESTINATION_FILENAME + "." + SimParam.LOG_SOURCE_FILEEXTENSION);
					try {
						Files.copy(logSource.toPath(), logDestination.toPath());
					} catch (IOException ignored) {
					}
				}
			}
		}
		
		ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
		ArduSimTools.storingResults = false;
	}
	
	/** Logging to file the UAVs main.java.com.protocols.mission, in AutoCAD format. */
	private static void logMission(String folder, String baseFileName) {
		File file1;
		StringBuilder sb1;
		
		int j;
		ValidationTools validationTools = API.getValidationTools();
		List<WaypointSimplified> missionUTMSimplified;
		for (int i=0; i<Param.numUAVs; i++) {
			missionUTMSimplified = UAVParam.missionUTMSimplified.get(i);
			if (missionUTMSimplified != null && missionUTMSimplified.size() > 1) {
				file1 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.MISSION_SUFIX);
				sb1 = new StringBuilder(2000);
				sb1.append("._PLINE\n");
				WaypointSimplified prev = null;
				WaypointSimplified current;
				for (j = 0; j<missionUTMSimplified.size(); j++) {
					current = missionUTMSimplified.get(j);
					if (prev == null) {
						sb1.append(validationTools.roundDouble(current.x, 3)).append(",").append(validationTools.roundDouble(current.y, 3)).append("\n");
						prev = current;
					} else if (!current.equals(prev)) {
						sb1.append(validationTools.roundDouble(current.x, 3)).append(",").append(validationTools.roundDouble(current.y, 3)).append("\n");
						prev = current;
					}
				}
				sb1.append("\n");
				API.getFileTools().storeFile(file1, sb1.toString());
			}
		}
		
		
		File file2;
		StringBuilder sb2 = new StringBuilder(2000);
		sb2.append("<?xml version=\"1.0\"?>\n<kml xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
			.append("\t<Document>\n");
		String[] colors = new String[] {"FF000000", "FFFF0000", "FF0000FF", "FF00FF00", "FFFFFF00",
				"FFFF00FF", "FF00A5FF", "FFCBC0FF", "FF00FFFF"};
		Location2DGeo geo;
		int located = 0;
		boolean coordinateError = false;
		for (int i=0; i<Param.numUAVs && !coordinateError; i++) {
			missionUTMSimplified = UAVParam.missionUTMSimplified.get(i);
			if (missionUTMSimplified != null && missionUTMSimplified.size() > 1) {
				located++;
				sb2.append("\t\t<Placemark>\n\t\t\t<name>UAV ").append(Param.id[i])
				.append("</name>\n\t\t\t<Style>\n\t\t\t\t<LineStyle>\n\t\t\t\t\t<color>").append(colors[i % colors.length])
				.append("</color>\n\t\t\t\t\t<colorMode>normal</colorMode>\n\t\t\t\t\t<width>1</width>\n\t\t\t\t</LineStyle>\n")
				.append("\t\t\t</Style>\n\t\t\t<LineString>\n\t\t\t\t<extrude>0</extrude>\n")
				.append("\t\t\t\t<altitudeMode>clampToGround</altitudeMode>\n\t\t\t\t<coordinates>");

				WaypointSimplified prev = null;
				WaypointSimplified current;
				for (j = 0; j<missionUTMSimplified.size() && !coordinateError; j++) {
					current = missionUTMSimplified.get(j);
					try {
						geo = current.getGeo();
						if (prev == null) {
							sb2.append(geo.longitude).append(",").append(geo.latitude).append(",").append(validationTools.roundDouble(current.z, 3));
							prev = current;
						} else if (!current.equals(prev)) {
							sb2.append(" ").append(geo.longitude).append(",").append(geo.latitude).append(",").append(validationTools.roundDouble(current.z, 3));
							prev = current;
						}
					} catch (LocationNotReadyException e) {
						e.printStackTrace();
						coordinateError = true;
					}
				}
				sb2.append("</coordinates>\n\t\t\t</LineString>\n\t\t</Placemark>\n");
			}
		}
		
		if (located > 0 && !coordinateError) {
			sb2.append("\t</Document>\n</kml>");
			file2 = new File(folder, baseFileName + "_mission_Google_Earth.kmz");
			try {
				FileOutputStream out = new FileOutputStream(file2);
				ZipOutputStream stream = new ZipOutputStream(out);
				ZipEntry zipEntry = new ZipEntry(baseFileName + "_mission_Google_Earth.kml");
				stream.putNextEntry(zipEntry);
				stream.write(sb2.toString().getBytes(StandardCharsets.UTF_8));
				stream.closeEntry();
				stream.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** Logging to file the CPU utilization. */
	private static void logCPUUtilization(String folder, String baseFileName) {
		File file = new File(folder, baseFileName + "_" + Text.CPU_SUFIX);
		StringBuilder sb = new StringBuilder(2000);
		sb.append("time(ns),GlobalCPU(%),GlobalCPU(%oneCore),JavaCPU(%),JavaCPU(%oneCore),ArduSimState\n");
		CPUData data = Param.cpu.poll();
		ValidationTools validationTools = API.getValidationTools();
		while (data != null) {
			sb.append(data.time).append(",")
				.append(validationTools.roundDouble(data.globalCPU, 1)).append(",")
				.append(validationTools.roundDouble(data.globalCPU * Param.numCPUs, 2)).append(",")
				.append(data.processCPU/Param.numCPUs).append(",")
				.append(data.processCPU).append(",")
				.append(data.simState).append("\n");
			data = Param.cpu.poll();
		}
		API.getFileTools().storeFile(file, sb.toString());
	}

	/**
	 * For ArduSim internal purposes only.
	 * @param text Text to be shown.
	 */
	public static void logGlobal(String text) {
		final String res;
		if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
			res = API.getValidationTools().timeToString(Param.setupTime, System.currentTimeMillis())
					+ " " + text;
		} else if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			res = API.getValidationTools().timeToString(Param.startTime, System.currentTimeMillis())
					+ " " + text;
		} else {
			res = text;
		}
		System.out.println(res);
		System.out.flush();
		// Update GUI only when using simulator and the main window is already loaded
		if (Param.role == ArduSim.SIMULATOR_GUI && MainWindow.buttonsPanel != null && MainWindow.buttonsPanel.logArea != null) {
			SwingUtilities.invokeLater(() -> {
				MainWindow.buttonsPanel.logArea.append(res + "\n");
				int pos = MainWindow.buttonsPanel.logArea.getText().length();
				MainWindow.buttonsPanel.logArea.setCaretPosition( pos );
			});
		}
	}

	/**
	 * For ArduSim internal purposes only.
	 * @param text Text to be shown.
	 */
	public static void logVerboseGlobal(String text) {
		if (Param.verboseLogging) {
			logGlobal(text);
		}
	}

	/**
	 * For ArduSim internal purposes only.
	 * @param text Text to be shown.
	 */
	public static void updateGlobalInformation(final String text) {
		// Update GUI only when using simulator
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(text));
		}
	}

	/**
	 * For ArduSim internal purposes only.
	 * @param title Title for the dialog.
	 * @param message Message to be shown.
	 */
	public static void warnGlobal(String title, String message) {
		if (Param.role == ArduSim.MULTICOPTER || Param.role == ArduSim.SIMULATOR_CLI) {
			System.out.println(title + ": " + message);
			System.out.flush();
		} else {
			JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
		}
	}

	public static final UnaryOperator<TextFormatter.Change> doubleFilter = t -> {
		if (t.isReplaced())
			if(t.getText().matches("[^0-9]"))
				t.setText(t.getControlText().substring(t.getRangeStart(), t.getRangeEnd()));

		if (t.isAdded()) {
			if (t.getControlText().contains(".")) {
				if (t.getText().matches("[^0-9]")) {
					t.setText("");
				}
			} else if (t.getText().matches("[^0-9.]")) {
				t.setText("");
			}
		}
		return t;
	};

}
