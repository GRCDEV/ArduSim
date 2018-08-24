package main;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.WaypointReachedListener;
import api.pojo.AtomicDoubleArray;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.LastLocations;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param.SimulatorState;
import main.Param.WirelessModel;
import main.cpuHelper.CPUData;
import none.ProtocolNoneHelper;
import pccompanion.logic.PCCompanionParam;
import sim.board.BoardParam;
import sim.gui.MainWindow;
import sim.logic.GPSStartThread;
import sim.logic.SimParam;
import sim.pojo.IncomingMessage;
import sim.pojo.IncomingMessageQueue;
import sim.pojo.PortScanResult;
import sim.pojo.WinRegistry;
import uavController.UAVControllerThread;
import uavController.UAVCurrentData;
import uavController.UAVCurrentStatus;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;

/** This class contains general tools used by the simulator. */

public class ArduSimTools {
	
	public static List<WaypointReachedListener> listeners = new ArrayList<WaypointReachedListener>();
	
	/** Parses the command line of the simulator.
	 * <p>Returns false if running a PC companion and the main thread execution must stop. */
	public static boolean parseArgs(String[] args) {
		String commandLine = "Command line:\n    java -jar ArduSim.jar <option>\nChoose option:\n    multicopter\n    simulator\n    pccompanion";
		if (args.length != 1) {
			System.out.println(commandLine);
			System.out.flush();
			return false;
		}
		
		if (!args[0].equalsIgnoreCase("multicopter") && !args[0].equalsIgnoreCase("simulator") && !args[0].equalsIgnoreCase("pccompanion")) {
			System.out.println(commandLine);
			System.out.flush();
			return false;
		}
		
		if (args[0].equalsIgnoreCase("multicopter")) {
			Param.role = Tools.MULTICOPTER;
		}
		if (args[0].equalsIgnoreCase("simulator")) {
			Param.role = Tools.SIMULATOR;
		}
		if (args[0].equalsIgnoreCase("pccompanion")) {
			Param.role = Tools.PCCOMPANION;
		}
		return true;
	}
	
	public static void parseIniFile() {
		Map<String, String> parameters = null;
		String param;
		// Load INI file
		parameters = ArduSimTools.loadIniFile();
		if (parameters.size() == 0) {
			return;
		}
		
		// Parse parameters common to real UAV and PC Companion
		if (Param.role == Tools.PCCOMPANION || Param.role == Tools.MULTICOPTER) {
			param = parameters.get(Param.COMPUTER_PORT);
			if (param == null) {
				GUI.log(Param.COMPUTER_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + PCCompanionParam.computerPort);
			} else {
				if (!Tools.isValidPort(param)) {
					GUI.log(Param.COMPUTER_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				PCCompanionParam.computerPort = Integer.parseInt(param);
			}
			param = parameters.get(Param.UAV_PORT);
			if (param == null) {
				GUI.log(Param.UAV_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + PCCompanionParam.uavPort);
			} else {
				if (!Tools.isValidPort(param)) {
					GUI.log(Param.UAV_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				PCCompanionParam.uavPort = Integer.parseInt(param);
			}
			param = parameters.get(Param.BROADCAST_IP);
			if (param == null) {
				GUI.log(Param.BROADCAST_IP + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.broadcastIP);
			} else {
				UAVParam.broadcastIP = param;
			}
			GUI.log(Text.INI_FILE_PARAM_BROADCAST_IP_WARNING + " " + param);
			param = parameters.get(Param.BROADCAST_PORT);
			if (param == null) {
				GUI.log(Param.BROADCAST_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.broadcastPort);
			} else {
				if (!Tools.isValidPort(param)) {
					GUI.log(Param.BROADCAST_PORT + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.broadcastPort = Integer.parseInt(param);
			}
		}

		// Parse parameters for a real UAV
		if (Param.role == Tools.MULTICOPTER) {
			if (!parameters.containsKey(Param.PROTOCOL)) {
				GUI.log(Text.INI_FILE_PROTOCOL_NOT_FOUND_ERROR);
				System.exit(1);
			}
			if (!parameters.containsKey(Param.SPEED)) {
				GUI.log(Text.INI_FILE_SPEED_NOT_FOUND_ERROR);
				System.exit(1);
			}
			// Set protocol
			String pr = parameters.get(Param.PROTOCOL);
			String pr2 = pr.toUpperCase();
			boolean found = false;
			for (int i = 0; i < ProtocolHelper.ProtocolNames.length && !found; i++) {
				if (ProtocolHelper.ProtocolNames[i].toUpperCase().equals(pr2)) {
					found = true;
				}
			}
			if (!found) {
				GUI.log(pr + " " + Text.PROTOCOL_NOT_FOUND_ERROR + "\n\n");

				for (int i = 0; i < ProtocolHelper.ProtocolNames.length; i++) {
					GUI.log(ProtocolHelper.ProtocolNames[i]);
				}
				System.exit(1);
			}
			ProtocolHelper.selectedProtocol = pr;
			ProtocolHelper.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();
			if (ProtocolHelper.selectedProtocolInstance == null) {
				System.exit(1);
			}
			// Set speed
			String spe = parameters.get(Param.SPEED);
			if (!Tools.isValidPositiveDouble(spe)) {
				GUI.log(Text.SPEED_ERROR + "\n");
				System.exit(1);
			}
			UAVParam.initialSpeeds = new double[1];
			UAVParam.initialSpeeds[0] = Double.parseDouble(spe);

			param = parameters.get(Param.SERIAL_PORT);
			if (param == null) {
				GUI.log(Param.SERIAL_PORT + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.serialPort);
			} else {
				if (!param.equalsIgnoreCase(UAVParam.serialPort)) {
					GUI.log(Text.INI_FILE_PARAM_SERIAL_PORT_WARNING + " " + param);
					UAVParam.serialPort = param;
				}
			}
			param = parameters.get(Param.BAUD_RATE);
			if (param == null) {
				GUI.log(Param.BAUD_RATE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.baudRate);
			} else {
				if (!Tools.isValidPositiveInteger(param)) {
					GUI.log(Param.BAUD_RATE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.baudRate = Integer.parseInt(param);
			}
			
			param = parameters.get(Param.BATTERY_CELLS);
			if (param == null) {
				GUI.log(Param.BATTERY_CELLS + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.lipoBatteryCells);
			} else {
				if (!Tools.isValidPositiveInteger(param)) {
					GUI.log(Param.BATTERY_CELLS + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
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
				GUI.log(Param.BATTERY_CAPACITY + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + UAVParam.lipoBatteryCapacity);
			} else {
				if (!Tools.isValidPositiveInteger(param)) {
					GUI.log(Param.BATTERY_CAPACITY + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
					System.exit(1);
				}
				UAVParam.lipoBatteryCapacity = Integer.parseInt(param);
			}
		}

		// Parse remaining parameters, for real UAV or simulation
		param = parameters.get(Param.MEASURE_CPU);
		if (param == null) {
			GUI.log(Param.MEASURE_CPU + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + Param.measureCPUEnabled);
		} else {
			if (!Tools.isValidBoolean(param)) {
				GUI.log(Param.MEASURE_CPU + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.measureCPUEnabled = Boolean.valueOf(param);
		}
		param = parameters.get(Param.VERBOSE_LOGGING);
		if (param == null) {
			GUI.log(Param.VERBOSE_LOGGING + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + Param.verboseLogging);
		} else {
			if (!Tools.isValidBoolean(param)) {
				GUI.log(Param.VERBOSE_LOGGING + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.verboseLogging = Boolean.valueOf(param);
		}
		param = parameters.get(Param.VERBOSE_STORE);
		if (param == null) {
			GUI.log(Param.VERBOSE_STORE + " " + Text.INI_FILE_PARAM_NOT_FOUND_ERROR + " " + Param.verboseStore);
		} else {
			if (!Tools.isValidBoolean(param)) {
				GUI.log(Param.VERBOSE_STORE + " " + Text.INI_FILE_PARAM_NOT_VALID_ERROR + " " + param);
				System.exit(1);
			}
			Param.verboseStore = Boolean.valueOf(param);
		}
	}
	
	private static Map<String, String> loadIniFile() {
		Map<String, String> parameters = new HashMap<>();
		File folder = Tools.getCurrentFolder();
		
		File [] files = folder.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(final File dir, String name) {
		    	boolean extensionIsIni = false;
		    	if(name.lastIndexOf(".") > 0) {
		    		final String extension = name.substring(name.lastIndexOf(".")+1);
		    		extensionIsIni = extension.equalsIgnoreCase("ini");
				}
		    	return extensionIsIni;
		    }
		});
		if (files == null || files.length == 0) {
			GUI.log(Text.INI_FILE_NOT_FOUND);
			return parameters;
		}
		int pos = -1;
		boolean found = false;
		for (int i = 0; i < files.length && !found; i++) {
			if (files[i].getName().equalsIgnoreCase("ardusim.ini")) {
				pos = i;
				found = true;
			}
		}
		if (pos == -1) {
			GUI.log(Text.INI_FILE_NOT_FOUND);
			return parameters;
		}
		File iniFile = files[pos];
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(iniFile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			return parameters;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			GUI.log(Text.INI_FILE_EMPTY);
			return parameters;
		}
		List<String> checkedLines = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.length() > 0 && !line.startsWith("#") && (line.length() - line.replace("=", "").length() == 1)) {
				checkedLines.add(line);
			}
		}
		if (checkedLines.size() > 0) {
			String key, value;
			String[] pair;
			for (int i = 0; i < checkedLines.size(); i++) {
				pair = checkedLines.get(i).split("=");
				key = pair[0].trim().toUpperCase();
				value = pair[1].trim();
				parameters.put(key, value);
			}
		}
		if (parameters.size() > 0) {
			// Check waste parameters
			for (final String key : parameters.keySet()) {
				found = false;
				for (int i = 0; i < Param.PARAMETERS.length && !found; i++) {
					if (key.equals(Param.PARAMETERS[i])) {
						found = true;
					}
				}
				if (!found) {
					GUI.log(Text.INI_FILE_WASTE_PARAMETER_WARNING + " " + key);
				}
			}
			// Check parameters with default value
			for (int i = 0; i < Param.PARAMETERS.length; i++) {
				if (!parameters.containsKey(Param.PARAMETERS[i])) {
					GUI.log(Param.PARAMETERS[i] + " " + Text.INI_FILE_MISSING_PARAMETER_WARNING);
				}
			}
		}
		return parameters;
	}
	
	/** Dynamically loads a Windows library. */
	public static void loadDll(String name) throws IOException {
		// File copy to temporary folder
	    InputStream in = Main.class.getResourceAsStream(name);
	    byte[] buffer = new byte[1024];
	    int read = -1;
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
		UAVParam.lastLocations = new LastLocations[Param.numUAVs];
		UAVParam.flightMode = new AtomicReferenceArray<FlightMode>(Param.numUAVs);
		UAVParam.MAVStatus = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentWaypoint = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentGeoMission = new ArrayList[Param.numUAVs];
		UAVParam.RTLAltitude = new double[Param.numUAVs];
		UAVParam.RTLAltitudeFinal = new double[Param.numUAVs];
		UAVParam.mavId = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.gcsId = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCmapRoll = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCmapPitch = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCmapThrottle = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCmapYaw = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.RCminValue = new AtomicIntegerArray[Param.numUAVs];
		UAVParam.RCtrimValue = new AtomicIntegerArray[Param.numUAVs];
		UAVParam.RCmaxValue = new AtomicIntegerArray[Param.numUAVs];
		UAVParam.flightModeMap = new AtomicIntegerArray[Param.numUAVs];
		UAVParam.customModeToFlightModeMap = new int[Param.numUAVs][24];
		
		UAVParam.newFlightMode = new FlightMode[Param.numUAVs];
		UAVParam.takeOffAltitude = new AtomicDoubleArray(Param.numUAVs);
		UAVParam.newSpeed = new double[Param.numUAVs];
		UAVParam.newParam = new ControllerParam[Param.numUAVs];
		UAVParam.newParamValue = new AtomicDoubleArray(Param.numUAVs);
		UAVParam.newCurrentWaypoint = new int[Param.numUAVs];
		UAVParam.newGeoMission = new Waypoint[Param.numUAVs][];
		UAVParam.stabilizationThrottle = new int[Param.numUAVs];
		UAVParam.newLocation = new float[Param.numUAVs][3];

		UAVParam.missionUTMSimplified = new AtomicReferenceArray<List<WaypointSimplified>>(Param.numUAVs);
		UAVParam.lastWaypointReached = new boolean[Param.numUAVs];
		
		UAVParam.rcs = new AtomicReference[Param.numUAVs];
		UAVParam.overrideOn = new AtomicIntegerArray(Param.numUAVs);
		
		SimParam.uavUTMPathReceiving = new ArrayBlockingQueue[Param.numUAVs];
		SimParam.uavUTMPath = new ArrayList[Param.numUAVs];	// Useful for logging purposes
		
		SimParam.prefix = new String[Param.numUAVs];

		Param.testEndTime = new long[Param.numUAVs];
		
		if (Param.role == Tools.SIMULATOR) {
			// Prepare matrix to brighten the background map image
			for (int i=0; i<256; i++) {
				BoardParam.brightness[i] = (short)(128+i/2);
			}
			
			UAVParam.MissionPx = new ArrayList[Param.numUAVs];
			BoardParam.uavPXPathLines = new ArrayList[Param.numUAVs];
			BoardParam.uavPrevUTMLocation = new LogPoint[Param.numUAVs];
			BoardParam.uavPrevPXLocation = new LogPoint[Param.numUAVs];
			BoardParam.uavCurrentUTMLocation = new LogPoint[Param.numUAVs];
			BoardParam.uavCurrentPXLocation = new LogPoint[Param.numUAVs];
			
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
			UAVParam.lastLocations[i] = new LastLocations<Point3D>(Point3D.class, UAVParam.LOCATIONS_SIZE);
			UAVParam.currentGeoMission[i] = new ArrayList<Waypoint>(UAVParam.WP_LIST_SIZE);
			UAVParam.RCminValue[i] = new AtomicIntegerArray(8);
			UAVParam.RCtrimValue[i] = new AtomicIntegerArray(8);
			UAVParam.RCmaxValue[i] = new AtomicIntegerArray(8);
			UAVParam.flightModeMap[i] = new AtomicIntegerArray(6);
			for (int j = 0; j < UAVParam.customModeToFlightModeMap[i].length; j++) {
				UAVParam.customModeToFlightModeMap[i][j] = -1;
			}
			
			UAVParam.lastWaypointReached[i] = false;
			
			UAVParam.rcs[i] = new AtomicReference<>();
			UAVParam.overrideOn.set(i, 1);	// Initially RC values can be overridden
			
			SimParam.uavUTMPathReceiving[i] = new ArrayBlockingQueue<LogPoint>(SimParam.UAV_POS_QUEUE_INITIAL_SIZE);
			SimParam.uavUTMPath[i] = new ArrayList<LogPoint>(SimParam.PATH_INITIAL_SIZE);
			
			if (Param.role == Tools.SIMULATOR) {
				UAVParam.MissionPx[i] = new ArrayList<Shape>();
				BoardParam.uavPXPathLines[i] = new ArrayList<Shape>(SimParam.PATH_INITIAL_SIZE);
				
				// On the simulator, the UAV identifier is i
				if (Param.id[i] == 0) {	// Could be initialized by a protocol
					Param.id[i] = i;
				}
			}
			
			SimParam.prefix[i] = Text.UAV_ID + " " + Param.id[i] + ": ";
		}
		
		// UAV to UAV communication structures
		if (Param.role == Tools.SIMULATOR) {
			UAVParam.distances = new AtomicReference[Param.numUAVs][Param.numUAVs];
			UAVParam.isInRange = new AtomicBoolean[Param.numUAVs][Param.numUAVs];
			for (int i = 0; i < Param.numUAVs; i++) {
				for (int j = 0; j < Param.numUAVs; j++) {
					UAVParam.distances[i][j] = new AtomicReference<Double>();
					UAVParam.isInRange[i][j] = new AtomicBoolean();
				}
			}
			
			UAVParam.prevSentMessage = new AtomicReferenceArray<IncomingMessage>(Param.numUAVs);
			UAVParam.mBuffer = new IncomingMessageQueue[Param.numUAVs];
			for (int i = 0; i < Param.numUAVs; i++) {
				UAVParam.mBuffer[i] = new IncomingMessageQueue();
			}
			if (UAVParam.pCollisionEnabled) {
				UAVParam.vBuffer = new ConcurrentSkipListSet[Param.numUAVs];
				UAVParam.vBufferUsedSpace = new AtomicIntegerArray(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					UAVParam.vBuffer[i] = new ConcurrentSkipListSet<>();
				}
				UAVParam.successfullyProcessed = new AtomicIntegerArray(Param.numUAVs);
				UAVParam.maxCompletedTEndTime = new AtomicLongArray(Param.numUAVs);
				UAVParam.lock = new ReentrantLock[Param.numUAVs];
				for (int i = 0; i < Param.numUAVs; i++) {
					UAVParam.lock[i] = new ReentrantLock();
				}
			}
			
			UAVParam.packetWaitedPrevSending = new int[Param.numUAVs];
			UAVParam.packetWaitedMediaAvailable = new int[Param.numUAVs];
			UAVParam.receiverOutOfRange = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverWasSending = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverVirtualQueueFull = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverQueueFull = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.successfullyReceived = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.discardedForCollision = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.successfullyEnqueued = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.communicationsClosed = new ConcurrentHashMap<>(Param.numUAVs);
		} else {
			try {
				UAVParam.sendSocket = new DatagramSocket();
				UAVParam.sendSocket.setBroadcast(true);
				UAVParam.sendPacket = new DatagramPacket(new byte[Tools.DATAGRAM_MAX_LENGTH],
						Tools.DATAGRAM_MAX_LENGTH,
						InetAddress.getByName(UAVParam.broadcastIP),
						UAVParam.broadcastPort);
				UAVParam.receiveSocket = new DatagramSocket(UAVParam.broadcastPort);
				UAVParam.receiveSocket.setBroadcast(true);
				UAVParam.receivePacket = new DatagramPacket(new byte[Tools.DATAGRAM_MAX_LENGTH], Tools.DATAGRAM_MAX_LENGTH);
			} catch (SocketException | UnknownHostException e) {
				GUI.exit(Text.THREAD_START_ERROR);
			}
		}
		UAVParam.sentPacket = new int[Param.numUAVs];
		UAVParam.receivedPacket = new AtomicIntegerArray(Param.numUAVs);
		
	}
	
	/** Rescales data structures when the screen scale is modified. */
	public static void rescaleDataStructures() {
		// Rescale the collision circles diameter
		Point2D.Double locationUTM = null;
		boolean found = false;
		int numUAVs = Tools.getNumUAVs();
		for (int i=0; i<numUAVs && !found; i++) {
			locationUTM = Copter.getUTMLocation(i);
			if (locationUTM != null) {
				found = true;
			}
		}
		Point2D.Double a = GUI.locatePoint(locationUTM.x, locationUTM.y);
		Point2D.Double b = GUI.locatePoint(locationUTM.x + UAVParam.collisionDistance, locationUTM.y);
		UAVParam.collisionScreenDistance = b.x - a.x;
	}
	
	/** Auxiliary method to check the available TCP and UDP ports needed by SITL instances and calculate the number of possible instances. */
	public static Integer[] getSITLPorts() throws InterruptedException, ExecutionException {
		// SITL starts using TCP5760 and UDP5501,5502,5503
		ExecutorService es = Executors.newFixedThreadPool(20);
	    List<Future<PortScanResult>> tcpMain = new ArrayList<Future<PortScanResult>>();
	    List<Future<PortScanResult>> udpAux1 = new ArrayList<Future<PortScanResult>>();
	    List<Future<PortScanResult>> udpAux2 = new ArrayList<Future<PortScanResult>>();
	    List<Future<PortScanResult>> udpAux3 = new ArrayList<Future<PortScanResult>>();
	    List<Integer> ports = new ArrayList<Integer>(UAVParam.MAX_SITL_INSTANCES);
	    int tcpPort = UAVParam.MAV_INITIAL_PORT;
	    int udp1Port = UAVParam.MAV_INTERNAL_PORT[0];
	    int udp2Port = UAVParam.MAV_INTERNAL_PORT[1];
	    int udp3Port = UAVParam.MAV_INTERNAL_PORT[2];
	    while (tcpPort <= UAVParam.MAV_FINAL_PORT) {
	    	tcpMain.add(portIsOpen(es, UAVParam.MAV_NETWORK_IP, tcpPort, UAVParam.PORT_CHECK_TIMEOUT));
	    	udpAux1.add(portIsOpen(es, UAVParam.MAV_NETWORK_IP, udp1Port, UAVParam.PORT_CHECK_TIMEOUT));
	    	udpAux2.add(portIsOpen(es, UAVParam.MAV_NETWORK_IP, udp2Port, UAVParam.PORT_CHECK_TIMEOUT));
	    	udpAux3.add(portIsOpen(es, UAVParam.MAV_NETWORK_IP, udp3Port, UAVParam.PORT_CHECK_TIMEOUT));
	    	tcpPort = tcpPort + 10;
	    	udp1Port = udp1Port + 10;
	    	udp2Port = udp2Port + 10;
	    	udp3Port = udp3Port + 10;
	    }
	    es.awaitTermination((long)UAVParam.PORT_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
	    Future<PortScanResult> tcp, udp1, udp2, udp3;
	    for (int i=0; i<tcpMain.size(); i++) {
	    	tcp = tcpMain.get(i);
	    	udp1 = udpAux1.get(i);
	    	udp2 = udpAux2.get(i);
	    	udp3 = udpAux3.get(i);
	    	if (!tcp.get().isOpen() && !udp1.get().isOpen() && !udp2.get().isOpen() && !udp3.get().isOpen()) {
	    		ports.add(tcp.get().getPort());
	    	}
	    }
	    es.shutdown();
		return ports.toArray(new Integer[ports.size()]);
	}
	
	/** Auxiliary method to detect asynchronously if a TCP port is in use by the system or any other application. */
	private static Future<PortScanResult> portIsOpen(final ExecutorService es, final String ip, final int port,
	        final int timeout) {
	    return es.submit(new Callable<PortScanResult>() {
	        @Override
	        public PortScanResult call() {
	            try {
	                Socket socket = new Socket();
	                socket.connect(new InetSocketAddress(ip, port), timeout);
	                socket.close();
	                return new PortScanResult(port, true);
	            } catch (Exception ex) {
	                return new PortScanResult(port, false);
	            }
	        }
	    });
	}
	
	/** Gets the id when using a real UAV, based on the MAC address. */
	public static long getRealId() {
		List<Long> ids = new ArrayList<Long>();
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			Enumeration<NetworkInterface> interfaces;
			Formatter formatter;
			String hexId;
			byte[] mac;
			try {
				interfaces = NetworkInterface.getNetworkInterfaces();
				while(interfaces.hasMoreElements()) {
					mac = ((NetworkInterface)interfaces.nextElement()).getHardwareAddress();
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
			} catch (SocketException e1) {
			} catch (NumberFormatException e1) {
			}
		} else if (Param.runningOperatingSystem == Param.OS_LINUX) {
			List<String> interfaces = new ArrayList<String>();
	        Pattern pattern = Pattern.compile("^ *(.*):");
	        String hexId;
	        BufferedReader in = null;
	        BufferedReader in2 = null;
	        try (FileReader reader = new FileReader("/proc/net/dev")) {
	            in = new BufferedReader(reader);
	            String line = null;
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
	                		} catch (IOException e) {
	    	                }
	                	}
	                }
	            }
	            // 2. Read the MAC address for each interface
	            for (String interfaceName : interfaces) {
	                try (FileReader reader2 = new FileReader("/sys/class/net/" + interfaceName + "/address")) {
	                    in = new BufferedReader(reader2);
	                    String addr = in.readLine();
	                    hexId = addr.replaceAll("[^a-zA-z0-9]", "");
	                    ids.add(Long.parseUnsignedLong(hexId, 16));
	                } catch (IOException e) {
	                }
	            }
	        } catch (IOException e) {
	        } finally {
	        	try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {}
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
				
				
			} catch (SocketException e1) {
			} catch (NumberFormatException e1) {
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		if (ids.size() == 0) {
			GUI.exit(Text.MAC_ERROR);
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
					if (ArduSimTools.checkDriveMountedLinux(ramDiskPath)) {
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
					if (ArduSimTools.checkDriveMountedLinux(ramDiskPath)) {
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
		}
		
		// When it is not possible, use physical storage
		if ((SimParam.userIsAdmin && Param.runningOperatingSystem == Param.OS_WINDOWS && !SimParam.imdiskIsInstalled)
				|| !SimParam.userIsAdmin) {
			File parentFolder = (new File(SimParam.sitlPath)).getParentFile();
			return parentFolder.getAbsolutePath();
		}
		return null;
	}
	
	/** If an appropriate virtual RAM drive is already mounted under Windows, it returns its location, or null in case of error. */
	private static File checkDriveMountedWindows() {
		File[] roots = File.listRoots();
		if (roots != null && roots.length > 0) {
			for(int i = 0; i < roots.length ; i++) {
				String name = FileSystemView.getFileSystemView().getSystemDisplayName(roots[i]);
				if (name.contains(SimParam.RAM_DRIVE_NAME.toUpperCase())) {
					return roots[i];
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
			for(int i = 0; i < roots.length ; i++) {
				driveLetters.add("" + roots[i].getAbsolutePath().charAt(0));
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
				GUI.log(Text.MOUNT_DRIVE_ERROR_1);
				return null;
			}
			
			// 2. Mount the temporary filesystem
			List<String> commandLine = new ArrayList<String>();
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
				String line = null;
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
			} catch (IOException e) {
			}
		} else {
			GUI.log(Text.MOUNT_DRIVE_ERROR_2);
		}
		return null;
	}

	/** Dismounts a virtual RAM drive under Windows. Returns true if the command was successful. */
	private static boolean dismountDriveWindows(String diskPath) {
		List<String> commandLine = new ArrayList<String>();
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
			String line = null;
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
		} catch (IOException e) {
		}
		return false;
	}
	
	/** If an appropriate virtual RAM drive is already mounted under Linux, it returns its location, or null in case of error. */
	private static boolean checkDriveMountedLinux(String diskPath) {
		List<String> commandLine = new ArrayList<String>();
		BufferedReader input;
		commandLine.add("df");
		commandLine.add("-aTh");
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
		    while ((line = input.readLine()) != null) {
		        sb.append(line).append("\n");
		    }
			String s = sb.toString();
			if (s.length() > 0 && s.charAt(s.length()-1) == '\n') {
				s = s.substring(0, s.length()-1);
			}
			if (s.contains(diskPath)) {
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
	
	/** Mount a virtual RAM drive under Linux. Return true if the command was successful. */
	private static boolean mountDriveLinux(String diskPath) {
		List<String> commandLine = new ArrayList<String>();
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
			String line = null;
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
		} catch (IOException e) {
		}
		return false;
	}
	
	/** Dismounts a virtual RAM drive under Linux. Returns true if the command was successful. */
	private static boolean dismountDriveLinux(String diskPath) {
		List<String> commandLine = new ArrayList<String>();
		BufferedReader input;
		commandLine.add("umount");
		commandLine.add(diskPath);
		ProcessBuilder pb = new ProcessBuilder(commandLine);
		try {
			Process p = pb.start();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
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
		} catch (IOException e) {
		}
		return false;
	}
	
	/** Checks if the application is stand alone (.jar) or it is running inside an IDE. */
	public static boolean isRunningFromJar() {
		String file = ArduSimTools.class.getResource("/" + ArduSimTools.class.getName().replace('.', '/') + ".class").toString();
		if (file.startsWith("jar:")) {
			return true;
		} else {
			return false;
		}
	}
	
	/** Gets an object of the available implementation for the selected protocol.
	 * <p>Returns null if no valid or more than one implementation were found.
	 * <p>An error message is already displayed.*/
	public static ProtocolHelper getSelectedProtocolInstance() {
		// Target protocol class and object
		ProtocolHelper protocolLaunched = null;
		Class<?>[] validImplementations = ProtocolHelper.ProtocolClasses;
		if (validImplementations != null && validImplementations.length > 0) {
			try {
				ProtocolHelper[] protocolImplementations = getProtocolImplementationInstances(validImplementations, ProtocolHelper.selectedProtocol);
				if (protocolImplementations == null) {
					GUI.log(Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + ProtocolHelper.selectedProtocol);
				} else if (protocolImplementations.length > 1) {
					GUI.log(Text.PROTOCOL_MANY_IMPLEMENTATIONS_ERROR + ProtocolHelper.selectedProtocol);
				} else {
					protocolLaunched = protocolImplementations[0];
				}
			} catch (InstantiationException | IllegalAccessException e) {
				GUI.log(Text.PROTOCOL_GETTING_PROTOCOL_CLASSES_ERROR);
			}
		}
		return protocolLaunched;
	}
	
	/** Gets all classes that extend ProtocolHelper or implement a protocol.
	 * <p>Returns null or an array of size 0 if no valid implementations were found. */
	private static Class<?>[] getAnyProtocolImplementations() {
		Class<?>[] res = null;
		// Get all Java classes included in ArduSim
		List<String> existingClasses = getClasses();
		// Get classes that extend ProtocolHelper class
		if (existingClasses != null && existingClasses.size() > 0) {
			res = getAllImplementations(existingClasses);
		} else {
			GUI.log(Text.PROTOCOL_GETTING_CLASSES_ERROR);
		}
		if (res.length == 0) {
			GUI.log(Text.PROTOCOL_GETTING_PROT_CLASSES_ERROR);
		}
		return res;
	}
	
	/** Loads the implemented protocols and retrieves the name of each one.
	 * <p>Protocol names are case-sensitive.
	 * <p>Returns null if no valid implementations were found. */
	public static String[] loadProtocols() {
		// First store the identifier of None protocol
		ProtocolNoneHelper noneInstance = new ProtocolNoneHelper();
		noneInstance.setProtocol();
		ProtocolHelper.noneProtocolName = noneInstance.protocolString;
		String[] names = null;
		Class<?>[] validImplementations = ArduSimTools.getAnyProtocolImplementations();
		if (validImplementations != null && validImplementations.length > 0) {
			ProtocolHelper.ProtocolClasses = validImplementations;
			// Avoiding more than one implementation of the same protocol and avoiding implementations without name
			Map<String, String> imp = new HashMap<>();
			ProtocolHelper protocol;
			try {
				for (int i = 0; i < validImplementations.length; i++) {
					protocol = (ProtocolHelper)validImplementations[i].newInstance();
					protocol.setProtocol();
					if (protocol.protocolString != null) {
						imp.put(protocol.protocolString, protocol.protocolString);
					}
				}
				if (imp.size() > 0) {
					names = imp.values().toArray(new String[imp.size()]);
					Arrays.sort(names);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				GUI.log(Text.PROTOCOL_LOADING_ERROR);
			}
		}
		return names;
	}
	
	/** Returns the existing Java classes in the jar file or Eclipse project.
	 * <p>Returns null or empty list if some error happens.*/
	private static List<String> getClasses() {
		List<String> existingClasses = null;
		if (isRunningFromJar()) {
			// Process the jar file contents when running from a jar file
			String jar = getJarFile();
			if (jar != null) {
				try {
					existingClasses = getClassNamesFromJar(jar);
				} catch (Exception e) {}
			}
		} else {
			// Explore .class file tree for class files when running on Eclipse
			File projectFolder = new File(Tools.getCurrentFolder(), "bin");
			existingClasses = new ArrayList<>();
			String projectFolderPath;
			try {
				projectFolderPath = projectFolder.getCanonicalPath() + File.separator;
				int prefixLength = projectFolderPath.length();	// To remove from the classes path
				getClassNamesFromIDE(existingClasses, projectFolder, prefixLength);
			} catch (IOException e) {}
		}
		return existingClasses;
	}
	
	/** Returns the String representation of the jar File the ArduSim instance is running from (path+name).
	 * <p>Returns null if some error happens.*/
	private static String getJarFile() {
		Class<Main> c = main.Main.class;
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
			try {
				jarFile = URLDecoder.decode(jarFilePath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		return jarFile;
	}
	
	/** Returns the list of classes included in the Jar file.
	 * <p>Returns null or empty list if some error happens.*/
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
						&& !className.startsWith("com.esotericsoftware") && !className.startsWith("org.objenesis")
						&& !className.startsWith("org.javatuples") && !className.startsWith("gnu.io")
						&& !className.startsWith("org.apache") && !className.startsWith("org.mavlink")
						&& !className.startsWith("org.objectweb") && !className.startsWith("org.hyperic")) {
					res.add(className.substring(0, className.lastIndexOf('.')));
				}
			}
		}
		try {
			if (jarFileStream != null) {
				jarFileStream.close();
			}
		} catch (IOException e) {}
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
		for (int i = 0; i < dir.size(); i++) {
			getClassNamesFromIDE(res, dir.get(i), prefixLength);
		}
	}
	
	/** Returns all the classes that contain a valid implementation of any protocol.
	 * <p>Returns an array of size 0 if no valid implementations were found.*/
	private static Class<?>[] getAllImplementations(List<String> existingClasses) {
		String className;
		Class<?> currentClass;
		Map<String, Class<?>> classesMap = new HashMap<>();
		for (int i = 0; i < existingClasses.size(); i++) {
			try {
				className = existingClasses.get(i);
				// Ignore special case
				if (!className.toUpperCase().endsWith("WINREGISTRY")
						&& !className.equals("module-info")) {
					currentClass = Class.forName(className);
					if (ProtocolHelper.class.isAssignableFrom(currentClass) && !ProtocolHelper.class.equals(currentClass)) {
						classesMap.put(existingClasses.get(i), currentClass);
					}
				}
			} catch (ClassNotFoundException e) {}
		}
		Class<?>[] res = new Class<?>[classesMap.size()];
		if (classesMap.size() > 0) {
			int i = 0;
			Iterator<Class<?>> it = classesMap.values().iterator();
			while (it.hasNext()) {
				res[i] = it.next();
				i++;
			}
		}
		return res;
	}
	
	/** Returns an instance for all the implementations of the selected protocol among all available implementations.
	 * <p>Returns a valid ProtocolHelper object for each implementation.
	 * <p>Returns null if no valid implementation was found. */
	private static ProtocolHelper[] getProtocolImplementationInstances(Class<?>[] implementations, String selectedProtocol) throws InstantiationException, IllegalAccessException {
		Class<?> c;
		ProtocolHelper o;
		ProtocolHelper[] res = null;
		List<ProtocolHelper> imp = new ArrayList<>();
		String pr = selectedProtocol.toUpperCase();
		for (int i = 0; i < implementations.length; i++) {
			c = implementations[i];
			o = (ProtocolHelper)c.newInstance();
			o.setProtocol();
			if (o.protocolString.toUpperCase().equals(pr)) {
				imp.add(o);
			}
		}
		if (imp.size() > 0) {
			res = imp.toArray(new ProtocolHelper[imp.size()]);
		}
		return res;
	}
	
	/** Starts the virtual UAVs. */
	public static void startVirtualUAVs(Pair<GeoCoordinates, Double>[] location) {
		
		GUI.log(Text.STARTING_UAVS);
		
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
					// Delete recursively in case it already exists
					if (tempFolder.exists()) {
						ArduSimTools.deleteFolder(tempFolder);
					}
					if (!tempFolder.mkdir()) {
						throw new IOException();
					}
					
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
			} catch (IOException e) {
				GUI.log(Text.UAVS_START_ERROR_2 + "\n" + parentFolder);
				throw new IOException();
			}

			// 3. Execute each SITL instance
			List<String> commandLine = new ArrayList<String>();
			BufferedReader input;
			String s, file1, file2;
			boolean success = false;
			String tempFolder;
			int instance;
			for (int i = 0; i < Param.numUAVs; i++) {
				tempFolder = (new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i)).getAbsolutePath();
				instance = (UAVParam.mavPort[i] - UAVParam.MAV_INITIAL_PORT) / 10;
				
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
								+ ";/cygdrive/" + file1.replace("\\", "/").replace(":", "") + " -S -I" + instance
								+ " --home " + location[i].getValue0().latitude + "," + location[i].getValue0().longitude
								+ ",-0.1," + location[i].getValue1() + " --model + --speedup 1 --defaults "
								+ "/cygdrive/" + file2.replace("\\", "/").replace(":", ""));
					} else {
						commandLine.add("cd /cygdrive/" + tempFolder.replace("\\", "/").replace(":", "")
								+ ";/cygdrive/" + SimParam.sitlPath.replace("\\", "/").replace(":", "") + " -S -I" + instance
								+ " --home " + location[i].getValue0().latitude + "," + location[i].getValue0().longitude
								+ ",-0.1," + location[i].getValue1() + " --model + --speedup 1 --defaults "
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
					commandLine.add("-I" + instance);
					commandLine.add("--home");
					commandLine.add(location[i].getValue0().latitude + "," + location[i].getValue0().longitude + ",-0.1," + location[i].getValue1());
					commandLine.add("--model");
					commandLine.add("+");
					commandLine.add("--speedup");
					commandLine.add("1");
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
							GUI.logVerbose(SimParam.prefix[i] + s);
							if (s.contains("Waiting for connection")) {
								GUI.logVerbose(SimParam.prefix[i] + Text.SITL_UP);
								success = true;
							}
						} else {
							Tools.waiting(SimParam.CONSOLE_READ_RETRY_WAITING_TIME);
							if (System.nanoTime() - startTime > SimParam.SITL_STARTING_TIMEOUT) {
								break;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (!success) {
					GUI.log(Text.UAVS_START_ERROR_5 + " " + i);
					throw new IOException();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			GUI.exit(Text.APP_NAME + " " + Text.UAVS_START_ERROR_1);
		}
	}

	/** Removes a folder recursively. */
	public static void deleteFolder (File file) throws IOException {
		for (File childFile : file.listFiles()) {
			if (childFile.isDirectory()) {
				ArduSimTools.deleteFolder(childFile);
			} else {
				if (!childFile.delete()) {
					throw new IOException();
				}
			}
		}
		if (!file.delete()) {
			throw new IOException();
		}
	}
	
	/** Detects the Operating System. */
	public static void detectOS() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.contains("win")) {
			Param.runningOperatingSystem = Param.OS_WINDOWS;
			GUI.logVerbose(Text.OPERATING_SYSTEM_WINDOWS);
		} else if (OS.contains("nux") || OS.contains("nix") || OS.contains("aix")) {
			Param.runningOperatingSystem = Param.OS_LINUX;
			GUI.logVerbose(Text.OPERATING_SYSTEM_LINUX);
		}else if (OS.contains("mac")) {
			Param.runningOperatingSystem = Param.OS_MAC;
			GUI.logVerbose(Text.OPERATING_SYSTEM_MAC);
		} else {
			GUI.exit(Text.UAVS_START_ERROR_4);
		}
	}

	/** Detects the SITL executable (if it is in the same folder), and Cygwin (the later, only under Windows). */
	public static void locateSITL() {
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
				GUI.exit(Text.UAVS_START_ERROR_3 + "\n" + SimParam.CYGWIN_PATH1 + " or,\n" + SimParam.CYGWIN_PATH2);
			}
			sitlPath = Tools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_WINDOWS_FILE_NAME;
		} else if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
			sitlPath = Tools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_LINUX_FILE_NAME;
		}
		// SITL detection
		if (Param.runningOperatingSystem == Param.OS_WINDOWS
				|| Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
			String paramPath = Tools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.PARAM_FILE_NAME;
			File sitlPathFile = new File(sitlPath);
			File paramPathFile = new File(paramPath);
			if (sitlPathFile.exists() && sitlPathFile.canExecute() && paramPathFile.exists()) {
				SimParam.sitlPath = sitlPath;
				SimParam.paramPath = paramPath;
			}
		}//TODO add support for MAC OS
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
			List<String> commandLine = new ArrayList<String>();
			BufferedReader input;
			commandLine.add("getent");
			commandLine.add("group");
			commandLine.add("root");
			ProcessBuilder pb = new ProcessBuilder(commandLine);
			try {
				Process p = pb.start();
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line = null;
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
		}//TODO add support for MAC OS
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
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			
			// Check again with ImDisk Toolkit (other installation pack)
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_64KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
			try {
				result = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						SimParam.IMDISK_REGISTRY_PATH2, SimParam.IMDISK_REGISTRY_KEY, WinRegistry.KEY_WOW64_32KEY);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
			if (result != null && result.contains(SimParam.IMDISK_REGISTRY_VALUE2)) {
				SimParam.imdiskIsInstalled = true;
				return;
			}
		}
		SimParam.imdiskIsInstalled = false;
	}
	
	/** Closes the SITL simulator instances, and the application. Also the Cygwin consoles if using Windows. */
	public static void shutdown() {
		// 1. Ask for confirmation if the experiment has not started or finished
		if (Param.role == Tools.SIMULATOR) {
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
		Param.simStatus = SimulatorState.SHUTTING_DOWN;
		if (Param.role == Tools.SIMULATOR) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.exitButton.setEnabled(false);
					MainWindow.buttonsPanel.statusLabel.setText(Text.SHUTTING_DOWN);
				}
			});

			ArduSimTools.closeSITL();
		}
		
		// 3. Close the application
		GUI.log(Text.EXITING);
		try {
			FileDescriptor.out.sync();
		} catch (SyncFailedException e1) {}
		Tools.waiting(SimParam.LONG_WAITING_TIME);
		if (Param.role == Tools.MULTICOPTER) {
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				try {
			    	Runtime.getRuntime().exec("shutdown.exe -s -t 0");
			    } catch (IOException e) {}
			} else if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
				try {
			    	Runtime.getRuntime().exec("sudo shutdown -h now");
			    } catch (IOException e) {}
			} else {
				GUI.log(Text.SHUTDOWN_ERROR);
			}
		} else if (Param.role == Tools.SIMULATOR) {
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
					GUI.log(Text.DISMOUNT_DRIVE_ERROR);
				}
			}
			
			if (Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC) {
				// Unmount temporal filesystem and remove folder
				if (ArduSimTools.checkDriveMountedLinux(SimParam.tempFolderBasePath) && !ArduSimTools.dismountDriveLinux(SimParam.tempFolderBasePath)) {
					GUI.log(Text.DISMOUNT_DRIVE_ERROR);
				}
				File ramDiskFile = new File(SimParam.tempFolderBasePath);
				if (ramDiskFile.exists()) {
					ramDiskFile.delete();
				}
			}//TODO add support for MAC OS
		} else {
			if (SimParam.tempFolderBasePath != null) {
				File parentFolder = new File(SimParam.tempFolderBasePath);
				for (int i=0; i<UAVParam.MAX_SITL_INSTANCES; i++) {
					File tempFolder = new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i);
					if (tempFolder.exists()) {
						try {
							deleteFolder(tempFolder);
						} catch (IOException e) {
						}
					}
				}
			}
		}
	}

	/** Starts the UAVs controllers threads. */
	public static void startUAVControllers() {
		Param.controllers = new UAVControllerThread[Param.numUAVs];
		try {
			for (int i=0; i<Param.numUAVs; i++) {
				Param.controllers[i] = new UAVControllerThread(i);
				Param.controllers[i].start();
			}
			GUI.log(Text.CONTROLLERS_STARTED);
		} catch (SocketException e) {
			GUI.exit(Text.THREAD_START_ERROR);
		}
	}
	
	/** Waits until the MAVLink link is available in all executing UAVs. */
	public static void waitMAVLink() {
		long time = System.nanoTime();
		boolean allReady = false;
		boolean error = false;
		while (!allReady) {
			if (UAVParam.numMAVLinksOnline.get() == Param.numUAVs) {
				allReady = true;
			}

			// On simulation, stop if error happens and they are not online
			if (System.nanoTime() - time > UAVParam.MAVLINK_ONLINE_TIMEOUT) {
				if (Param.role == Tools.MULTICOPTER) {
					if (!error) {
						GUI.log(Text.MAVLINK_ERROR);
						error = true;
					}
				} else {
					break;
				}
			}
			if (!allReady) {
				Tools.waiting(UAVParam.MAVLINK_WAIT);
			}
		}
		if (!allReady) {
			GUI.exit(Text.MAVLINK_ERROR);
		}
	}

	/** Forces the UAV to send the GPS location. */
	public static void forceGPS() {
		// All UAVs GPS position forced on a different thread, but the first
		GPSStartThread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new GPSStartThread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new GPSStartThread(i);
			}
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1].start();
			}
		}
		GPSStartThread.forceGPS(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException e) {
				}
			}
		}
		if (GPSStartThread.GPS_FORCED.get() < Param.numUAVs) {
			GUI.exit(Text.INITIAL_CONFIGURATION_ERROR_1);
		}
	}
	
	/** Waits until GPS is available in all executing UAVs. */
	public static void getGPSFix() {
		GUI.log(Text.WAITING_GPS);
		long time = System.nanoTime();
		boolean startProcess = false;
		boolean error = false;
		while (!startProcess) {
			if (UAVParam.numGPSFixed.get() == Param.numUAVs) {
				startProcess = true;
			}
			if (System.nanoTime() - time > UAVParam.GPS_FIX_TIMEOUT) {
				if (Param.role == Tools.MULTICOPTER) {
					if (!error) {
						GUI.log(Text.GPS_FIX_ERROR_2);
						error = true;
					}
				} else {
					break;
				}
			}
			if (!startProcess) {
				Tools.waiting(UAVParam.GPS_FIX_WAIT);
			}
		}
		if (!startProcess) {
			GUI.exit(Text.GPS_FIX_ERROR_1);
		}
		GUI.log(Text.GPS_OK);
		if (Param.role == Tools.SIMULATOR) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(Text.UAVS_ONLINE);
				}
			});
		}
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, retrieves controller configuration, loads missions, etc. */
	public static void sendBasicConfiguration() {
		
		GUI.log(Text.SEND_BASIC_CONFIGURATION);
		// All UAVs configured on a different thread, but the first
		InitialConfigurationThread[] threads = null;
		if (Param.numUAVs > 1) {
			threads = new InitialConfigurationThread[Param.numUAVs - 1];
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1] = new InitialConfigurationThread(i);
			}
			for (int i=1; i<Param.numUAVs; i++) {
				threads[i-1].start();
			}
		}
		InitialConfigurationThread.sendBasicConfiguration(0);
		if (Param.numUAVs > 1) {
			for (int i=1; i<Param.numUAVs; i++) {
				try {
					threads[i-1].join();
				} catch (InterruptedException e) {
				}
			}
		}
		if (InitialConfigurationThread.UAVS_CONFIGURED.get() < Param.numUAVs) {
			GUI.exit(Text.INITIAL_CONFIGURATION_ERROR_2);
		}
	}
	
	/** Method used by ArduSim (forbidden to users) to trigger a waypoint reached event. */
	public static void triggerWaypointReached(int numUAV) {
		for (int i = 0; i < ArduSimTools.listeners.size(); i++) {
			WaypointReachedListener listener = ArduSimTools.listeners.get(i);
			if (listener.getNumUAV() == numUAV) {
				listener.onWaypointReached();
			}
		}
	}
	
	/** Detects if a UAV is running out of battery. */
	public static void checkBatteryLevel() {
		int depleted = 0;
		double alarmLevel = Double.MAX_VALUE;
		if (Param.role == Tools.MULTICOPTER) {
			alarmLevel = UAVParam.lipoBatteryAlarmVoltage;
		} else if (Param.role == Tools.SIMULATOR) {
			alarmLevel = UAVParam.VIRT_BATTERY_ALARM_VOLTAGE;
		}
		int percentage;
		double voltage;
		boolean isDepleted;
		for (int i=0; i<Param.numUAVs; i++) {
			percentage = UAVParam.uavCurrentStatus[i].getRemainingBattery();
			voltage = UAVParam.uavCurrentStatus[i].getVoltage();
			isDepleted = false;
			if (percentage != -1 && percentage < UAVParam.BATTERY_DEPLETED_THRESHOLD * 100) {
				isDepleted = true;
			}
			if (voltage != -1 && voltage <alarmLevel) {
				isDepleted = true;
			}
			if (percentage != -1 && voltage != -1) {
				if (Param.role == Tools.MULTICOPTER) {
					GUI.log(Text.BATTERY_LEVEL + " " + percentage + " % - " + voltage + " V");
				} else if (Param.role == Tools.SIMULATOR) {
					GUI.logVerbose(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " % - " + voltage + " V");
				}
			} else if (percentage != -1) {
				if (Param.role == Tools.MULTICOPTER) {
					GUI.log(Text.BATTERY_LEVEL + " " + percentage + " %");
				} else if (Param.role == Tools.SIMULATOR) {
					GUI.logVerbose(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " %");
				}
			} else if (voltage != -1) {
				if (Param.role == Tools.MULTICOPTER) {
					GUI.log(Text.BATTERY_LEVEL + " " + voltage + " V");
				} else if (Param.role == Tools.SIMULATOR) {
					GUI.logVerbose(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + voltage + " V");
				}
			}
			if (isDepleted) {
				if (Param.role == Tools.MULTICOPTER) {
					GUI.log(Text.BATTERY_FAILING2);
				} else if (Param.role == Tools.SIMULATOR) {
					depleted++;
				}
			}
		}
		if (Param.role == Tools.SIMULATOR && depleted > 0) {
			GUI.log(Text.BATTERY_FAILING + depleted + " " + Text.UAV_ID + "s");
		}
	}
	
	/** Detects if all the UAVs have finished the experiment (they are all on the ground, with engines not armed). */
	public static boolean isTestFinished() {
		boolean allFinished = true;
		if (UAVParam.flightStarted) {
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
					allFinished = false;
				}
			}
			if (allFinished) {
				Param.latestEndTime = latest;
			}
		} else {
			allFinished = false;
		}
		return allFinished;
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
		if (!ProtocolHelper.selectedProtocol.equals(ProtocolHelper.noneProtocolName)) {
			sb.append(Text.LOG_GLOBAL).append(":\n\n");
		}
		
		long totalTime = maxTime - Param.startTime;
		long[] uavsTotalTime = new long[Param.numUAVs];
		for (int i = 0; i < Param.numUAVs; i++) {
			uavsTotalTime[i] = Param.testEndTime[i] - Param.startTime;
		}

		// 2. Global times
		sb.append(Text.LOG_TOTAL_TIME).append(": ").append(Tools.timeToString(0, totalTime)).append("\n");
		for (int i = 0; i < Param.numUAVs; i++) {
			sb.append("\t").append(Text.UAV_ID).append(" ").append(Param.id[i]).append(": ").append(Tools.timeToString(0, uavsTotalTime[i])).append("\n");
		}

		return sb.toString();
	}
	
	/** Builds a String with the experiment parameters. */
	public static String getTestGlobalConfiguration() {
		StringBuilder sb = new StringBuilder(2000);
		if (Param.role == Tools.MULTICOPTER) {
			sb.append("\n").append(Text.GENERAL_PARAMETERS);
			sb.append("\n\t").append(Text.LOG_SPEED).append(UAVParam.initialSpeeds[0]).append(Text.METERS_PER_SECOND);
			sb.append("\n\t").append(Text.VERBOSE_LOGGING_ENABLE).append(" ");
			if (Param.verboseLogging) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
			sb.append("\n\t").append(Text.VERBOSE_STORAGE_ENABLE).append(" ");
			if (Param.verboseStore) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
		} else if (Param.role == Tools.SIMULATOR) {
			sb.append("\n").append(Text.SIMULATION_PARAMETERS);
			sb.append("\n\t").append(Text.UAV_NUMBER).append(" ").append(Param.numUAVs);
			sb.append("\n\t").append(Text.LOG_SPEED).append(" (").append(Text.METERS_PER_SECOND).append("):\n");
			for (int i=0; i<Param.numUAVs; i++) {
				sb.append(UAVParam.initialSpeeds[i]);
				if (i%10 == 9) {
					sb.append("\n\t");
				} else if (i != Param.numUAVs - 1) {
					sb.append(", ");
				}
			}
			sb.append("\n").append(Text.PERFORMANCE_PARAMETERS);
			sb.append("\n\t").append(Text.SCREEN_REFRESH_RATE).append(" ").append(BoardParam.screenDelay).append(" ").append(Text.MILLISECONDS);
			sb.append("\n\t").append(Text.REDRAW_DISTANCE).append(" ").append(BoardParam.minScreenMovement).append(" ").append(Text.PIXELS);
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
					sb.append("\n\t\tDuring the experiment:")
						.append("\n\t\t\tGlobalCPU: ").append(Tools.round(global, 3))
						.append("%\n\t\t\tGlobalCPU(oneCore): ").append(Tools.round(global * Param.numCPUs, 3))
						.append("%\n\t\t\tJavaCPU: ").append(Tools.round(processOneCore/Param.numCPUs, 3))
						.append("%\n\t\t\tJavaCPU(oneCore): ").append(Tools.round(processOneCore, 3)).append("%");
				}
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
			sb.append("\n\t").append(Text.RENDER).append(" ").append(SimParam.renderQuality.getName());
			sb.append("\n").append(Text.GENERAL_PARAMETERS);
			sb.append("\n\t").append(Text.VERBOSE_LOGGING_ENABLE).append(" ");
			if (Param.verboseLogging) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
			sb.append("\n\t").append(Text.VERBOSE_STORAGE_ENABLE).append(" ");
			if (Param.verboseStore) {
				sb.append(Text.OPTION_ENABLED);
			} else {
				sb.append(Text.OPTION_DISABLED);
			}
		}
		sb.append("\n").append(Text.UAV_PROTOCOL_USED).append(" ").append(ProtocolHelper.selectedProtocol);
		sb.append("\n").append(Text.COMMUNICATIONS);
		long sentPacketTot = 0;
		long receivedPacketTot = 0;
		for (int i = 0; i < Param.numUAVs; i++) {
			sentPacketTot = sentPacketTot + UAVParam.sentPacket[i];
			receivedPacketTot = receivedPacketTot + UAVParam.receivedPacket.get(i);
		}
		if (Param.role == Tools.MULTICOPTER) {
			sb.append("\n\t").append(Text.BROADCAST_IP).append(" ").append(UAVParam.broadcastIP);
			sb.append("\n\t").append(Text.BROADCAST_PORT).append(" ").append(UAVParam.broadcastPort);
			sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(sentPacketTot);
			sb.append("\n\t").append(Text.TOT_PROCESSED).append(" ").append(receivedPacketTot);
		} else if (Param.role == Tools.SIMULATOR) {
			sb.append("\n\t").append(Text.CARRIER_SENSING_ENABLED).append(" ").append(UAVParam.carrierSensingEnabled);
			sb.append("\n\t").append(Text.PACKET_COLLISION_DETECTION_ENABLED).append(" ").append(UAVParam.pCollisionEnabled);
			sb.append("\n\t").append(Text.BUFFER_SIZE).append(" ").append(UAVParam.receivingBufferSize).append(" ").append(Text.BYTES);
			sb.append("\n\t").append(Text.WIFI_MODEL).append(" ").append(Param.selectedWirelessModel.getName());
			if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
				sb.append(": ").append(Param.fixedRange).append(" ").append(Text.METERS);
			}
			sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(sentPacketTot);
			if (Param.numUAVs > 1 && sentPacketTot > 0) {
				long packetWaitedPrevSendingTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					packetWaitedPrevSendingTot = packetWaitedPrevSendingTot + UAVParam.packetWaitedPrevSending[i];
				}
				sb.append("\n\t\t").append(Text.TOT_WAITED_PREV_SENDING).append(" ").append(packetWaitedPrevSendingTot)
					.append(" (").append(Tools.round((100.0 * packetWaitedPrevSendingTot)/sentPacketTot, 3)).append("%)");
				if (UAVParam.carrierSensingEnabled) {
					long packetWaitedMediaAvailableTot = 0;
					for (int i = 0; i < Param.numUAVs; i++) {
						packetWaitedMediaAvailableTot = packetWaitedMediaAvailableTot + UAVParam.packetWaitedMediaAvailable[i];
					}
					sb.append("\n\t\t").append(Text.TOT_WAITED_MEDIA_AVAILABLE).append(" ").append(packetWaitedMediaAvailableTot)
						.append(" (").append(Tools.round((100.0 * packetWaitedMediaAvailableTot)/sentPacketTot, 3)).append("%)");
				}
				long potentiallyReceived = sentPacketTot * (Param.numUAVs - 1);
				long receiverOutOfRangeTot = 0;
				long receiverWasSendingTot = 0;
				long successfullyReceivedTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					receiverOutOfRangeTot = receiverOutOfRangeTot + UAVParam.receiverOutOfRange.get(i);
					receiverWasSendingTot = receiverWasSendingTot + UAVParam.receiverWasSending.get(i);
					successfullyReceivedTot = successfullyReceivedTot + UAVParam.successfullyReceived.get(i);
				}
				long receiverVirtualQueueFullTot = 0;
				long successfullyEnqueuedTot = 0;
				if (UAVParam.pCollisionEnabled) {
					for (int i = 0; i < Param.numUAVs; i++) {
						receiverVirtualQueueFullTot = receiverVirtualQueueFullTot + UAVParam.receiverVirtualQueueFull.get(i);
						successfullyEnqueuedTot = successfullyEnqueuedTot + UAVParam.successfullyEnqueued.get(i);
					}
				}
				long receiverQueueFullTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					receiverQueueFullTot = receiverQueueFullTot + UAVParam.receiverQueueFull.get(i);
				}
				sb.append("\n\t").append(Text.TOT_POTENTIALLY_RECEIVED).append(" ").append(potentiallyReceived);
				sb.append("\n\t\t").append(Text.TOT_OUT_OF_RANGE).append(" ").append(receiverOutOfRangeTot)
					.append(" (").append(Tools.round((100.0 * receiverOutOfRangeTot)/potentiallyReceived, 3)).append("%)");
				sb.append("\n\t\t").append(Text.TOT_LOST_RECEIVER_WAS_SENDING).append(" ").append(receiverWasSendingTot)
					.append(" (").append(Tools.round((100.0 * receiverWasSendingTot)/potentiallyReceived, 3)).append("%)");
				if (UAVParam.pCollisionEnabled) {
					sb.append("\n\t\t").append(Text.TOT_VIRTUAL_QUEUE_WAS_FULL).append(" ").append(receiverVirtualQueueFullTot)
						.append(" (").append(Tools.round((100.0 * receiverVirtualQueueFullTot)/potentiallyReceived, 3)).append("%)");
					sb.append("\n\t\t").append(Text.TOT_RECEIVED_IN_VBUFFER).append(" ").append(successfullyEnqueuedTot)
						.append(" (").append(Tools.round((100.0 * successfullyEnqueuedTot)/potentiallyReceived, 3)).append("%)");
				} else {
					// We must include the received messages but discarded because the buffer was full
					successfullyReceivedTot = successfullyReceivedTot + receiverQueueFullTot;
					sb.append("\n\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
						.append(" (").append(Tools.round((100.0 * successfullyReceivedTot)/potentiallyReceived, 3)).append("%)");
				}
				long inBufferTot = successfullyReceivedTot - receiverQueueFullTot - receivedPacketTot;
				if (UAVParam.pCollisionEnabled) {
					if (successfullyEnqueuedTot != 0) {
						long successfullyProcessedTot = 0;
						for (int i = 0; i < Param.numUAVs; i++) {
							successfullyProcessedTot = successfullyProcessedTot + UAVParam.successfullyProcessed.get(i);
						}
						long inVBufferTot = successfullyEnqueuedTot - successfullyProcessedTot;
						sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_VBUFFER).append(" ").append(inVBufferTot)
							.append(" (").append(Tools.round((100.0 * inVBufferTot)/successfullyEnqueuedTot, 3)).append("%)");
						sb.append("\n\t\t\t").append(Text.TOT_PROCESSED).append(" ").append(successfullyProcessedTot)
							.append(" (").append(Tools.round((100.0 * successfullyProcessedTot)/successfullyEnqueuedTot, 3)).append("%)");
						if (successfullyProcessedTot != 0) {
							long discardedForCollisionTot = 0;
							for (int i = 0; i < Param.numUAVs; i++) {
								discardedForCollisionTot = discardedForCollisionTot + UAVParam.discardedForCollision.get(i);
							}
							sb.append("\n\t\t\t\t").append(Text.TOT_DISCARDED_FOR_COLLISION).append(" ").append(discardedForCollisionTot)
								.append(" (").append(Tools.round((100.0 * discardedForCollisionTot)/successfullyProcessedTot, 3)).append("%)");
							sb.append("\n\t\t\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
								.append(" (").append(Tools.round((100.0 * successfullyReceivedTot)/successfullyProcessedTot, 3)).append("%)");
							if (successfullyReceivedTot != 0) {
								sb.append("\n\t\t\t\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
									.append(" (").append(Tools.round((100.0 * receiverQueueFullTot)/successfullyReceivedTot, 3)).append("%)");
								sb.append("\n\t\t\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(inBufferTot)
									.append(" (").append(Tools.round((100.0 * inBufferTot)/successfullyReceivedTot, 3)).append("%)");
								sb.append("\n\t\t\t\t\t").append(Text.TOT_USED_OK).append(" ").append(receivedPacketTot)
									.append(" (").append(Tools.round((100.0 * receivedPacketTot)/successfullyReceivedTot, 3)).append("%)");
							}
						}
					}
				} else {
					if (successfullyReceivedTot != 0) {
						sb.append("\n\t\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
						.append(" (").append(Tools.round((100.0 * receiverQueueFullTot)/successfullyReceivedTot, 3)).append("%)");
					sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(inBufferTot)
						.append(" (").append(Tools.round((100.0 * inBufferTot)/successfullyReceivedTot, 3)).append("%)");
					sb.append("\n\t\t\t").append(Text.TOT_USED_OK).append(" ").append(receivedPacketTot)
						.append(" (").append(Tools.round((100.0 * receivedPacketTot)/successfullyReceivedTot, 3)).append("%)");
					}
				}
			}
			
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
		return sb.toString();
	}

	/** Logs to files the UAV path and general information. */
	public static void storeLogAndPath(String folder, String baseFileName) {
		File file1, file2;
		File file3 = null;
		StringBuilder sb1, sb2;
		StringBuilder sb3 = null;
		LogPoint sp;
		LogPoint spPrev;
		Double x, y, z, a;
		long firstTime = Long.MAX_VALUE;
		long lastTime = 0;
		for (int i=0; i<Param.numUAVs; i++) {
			file1 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + Text.PATH_SUFIX);
			sb1 = new StringBuilder(2000);
			sb1.append("x(m),y(m),z(m),heading(rad),t(arbitrary ns),s(m/s),a(m/s\u00B2),\u0394d(m),d(m)\n");
			file2 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + Text.PATH_2D_SUFIX);
			sb2 = new StringBuilder(2000);
			sb2.append("._PLINE\n");

			int j = 0;
			double dist = 0; // Accumulated distance to origin
			double d;
			spPrev = null;
			z = y = x = null;
			
			if (Param.verboseStore) {
				file3 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + Text.PATH_3D_SUFIX);
				sb3 = new StringBuilder(2000);
				sb3.append("._3DPOLY\n");

			}

			while (j<SimParam.uavUTMPath[i].size()) {
				sp = SimParam.uavUTMPath[i].get(j);
				// Calculus of the unfiltered acceleration
				if (j == 0) {
					a = 0.0;
				} else {
					a = (sp.speed - SimParam.uavUTMPath[i].get(j-1).speed)/
							(sp.time - SimParam.uavUTMPath[i].get(j-1).time)*1000000000l;
				}

				// Considers only not repeated locations and under test
				if (sp.inTest) {
					if (sp.time > lastTime) {
						lastTime = sp.time;
					}
					x = Tools.round(sp.x, 3);
					y = Tools.round(sp.y, 3);
					z = Tools.round(sp.z, 3);
					if (spPrev == null) {
						// First test location
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(sp.heading)
							.append(",").append(sp.time).append(",").append(Tools.round(sp.speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",0.000,0.000\n");
						sb2.append(x).append(",").append(y).append("\n");
						if (Param.verboseStore) {
							sb3.append(x).append(",").append(y).append(",").append(z).append("\n");
						}
						spPrev = sp;
						// Getting global test stating time
						if (sp.time < firstTime) {
							firstTime = sp.time;
						}
					} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
						// Moved horizontally
						d = sp.distance(spPrev);
						dist = dist + d;
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(sp.heading)
							.append(",").append(sp.time).append(",").append(Tools.round(sp.speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",").append(Tools.round(d, 3))
							.append(",").append(Tools.round(dist, 3)).append("\n");
						sb2.append(x).append(",").append(y).append("\n");
						if (Param.verboseStore) {
							sb3.append(x).append(",").append(y).append(",").append(z).append("\n");
						}
						spPrev = sp;
					} else if (sp.z!=spPrev.z) {
						// Only moved vertically
						sb1.append(x).append(",").append(y).append(",").append(z).append(",").append(sp.heading)
							.append(",").append(sp.time).append(",").append(Tools.round(sp.speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",0.0,").append(Tools.round(dist, 3)).append("\n");
						if (Param.verboseStore) {
							sb3.append(x).append(",").append(y).append(",").append(z).append("\n");
						}
						spPrev = sp;
					}
				}
				j++;
			}
			Tools.storeFile(file1, sb1.toString());
			sb2.append("\n");
			Tools.storeFile(file2, sb2.toString());
			if (Param.verboseStore) {
				sb3.append("\n");
				Tools.storeFile(file3, sb3.toString());
			}
		}
		
		// Storing mobility files
		File file4, file5, file6, file7;
		StringBuilder sb4, sb5, sb6, sb7;
		double time;
		boolean firstData;
		file6 = new File(folder, baseFileName + "_" + Text.MOBILITY_OMNET_SUFIX_2D);
		file7 =new File(folder, baseFileName + "_" + Text.MOBILITY_OMNET_SUFIX_3D);
		sb6 = new StringBuilder(2000);
		sb7 = new StringBuilder(2000);
		for (int i = 0; i < Param.numUAVs; i++) {
			file4 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.MOBILITY_NS2_SUFIX_2D);
			file5 = new File(folder, baseFileName + "_" + Param.id[i] + "_" + Text.MOBILITY_NS2_SUFIX_3D);
			sb4 = new StringBuilder(2000);
			sb5 = new StringBuilder(2000);
			firstData = true;
			double t;
			spPrev = null;
			for (int j = 0; j < SimParam.uavUTMPath[i].size(); j++) {
				sp = SimParam.uavUTMPath[i].get(j);
				if (sp.inTest && sp.time - firstTime >= 0 && sp.time <= lastTime) {
					time = (sp.time - firstTime) * 0.000000001;
					t = Tools.round(time, 9);
					x = Tools.round(sp.x, 2);
					y = Tools.round(sp.y, 2);
					z = Tools.round(sp.z, 2);
					if (firstData) {
						// Adding 0 point
						sb4.append("$node_(0) set X_ ").append(x).append("\n");
						sb4.append("$node_(0) set Y_ ").append(y).append("\n");
						sb5.append("$node_(0) set X_ ").append(x).append("\n");
						sb5.append("$node_(0) set Y_ ").append(y).append("\n");
						sb5.append("$node_(0) set Z_ ").append(z).append("\n");
						sb6.append("0 ").append(x).append(" ").append(y);
						sb7.append("0 ").append(x).append(" ").append(y).append(" ").append(z);
						// At the beginning we may need to add a 0 time point
						if (sp.time != firstTime) {
							sb4.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
							sb4.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
							sb5.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
							sb5.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
							sb5.append("$ns_ at ").append(t).append(" $node_(0) set Z_ ").append(z).append("\n");
							sb6.append(" ").append(t).append(" ").append(x).append(" ").append(y);
							sb7.append(" ").append(t).append(" ").append(x).append(" ").append(y).append(" ").append(z);
						}
						spPrev = sp;
						firstData = false;
					} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
						sb4.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
						sb4.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set Z_ ").append(z).append("\n");
						sb6.append(" ").append(t).append(" ").append(x).append(" ").append(y);
						sb7.append(" ").append(t).append(" ").append(x).append(" ").append(y).append(" ").append(z);
						spPrev = sp;
					} else if (sp.z!=spPrev.z) {
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
						sb5.append("$ns_ at ").append(t).append(" $node_(0) set Z_ ").append(z).append("\n");
						sb7.append(" ").append(t).append(" ").append(x).append(" ").append(y).append(" ").append(z);
						spPrev = sp;
					}
				}
			}
			if (spPrev.time < lastTime) {
				t = Tools.round((lastTime - firstTime) * 0.000000001, 9);
				x = Tools.round(spPrev.x, 2);
				y = Tools.round(spPrev.y, 2);
				z = Tools.round(spPrev.z, 2);
				sb4.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
				sb4.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
				sb5.append("$ns_ at ").append(t).append(" $node_(0) set X_ ").append(x).append("\n");
				sb5.append("$ns_ at ").append(t).append(" $node_(0) set Y_ ").append(y).append("\n");
				sb5.append("$ns_ at ").append(t).append(" $node_(0) set Z_ ").append(z).append("\n");
				sb6.append(" ").append(t).append(" ").append(x).append(" ").append(y);
				sb7.append(" ").append(t).append(" ").append(x).append(" ").append(y).append(" ").append(z);
			}
			
			Tools.storeFile(file4, sb4.toString());
			Tools.storeFile(file5, sb5.toString());
			
			sb6.append("\n");
			sb7.append("\n");
		}
		
		Tools.storeFile(file6, sb6.toString());
		Tools.storeFile(file7, sb7.toString());
	}
	
	/** Stores the experiment results. */
	public static void storeResults(String results, File file) {
		// Extracting the name of the file without path or extension for later logging
		String folder = file.getParent();
		String baseFileName = file.getName();
		String extension = Tools.getFileExtension(file);
		if (extension.length() > 0) {
			baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(extension) - 1); // Remove also the dot
		}
		
		// 1. Store results
		// Add extension if needed
		if (!file.getAbsolutePath().toUpperCase().endsWith("." + Text.FILE_EXTENSION_TXT.toUpperCase())) {
			file = new File(file.toString() + "." + Text.FILE_EXTENSION_TXT);
		}
		// Store simulation and protocol progress information and configuration
		Tools.storeFile(file, results);
		
		// 2. Store the UAV path and general information
		ArduSimTools.storeLogAndPath(folder, baseFileName);
		
		// 3. Store missions when used
		ArduSimTools.logMission(folder, baseFileName);
		
		// 4. Store CPU utilization
		if (Param.measureCPUEnabled) {
			ArduSimTools.logCPUUtilization(folder, baseFileName);
		}
		
		// 5. Store protocol specific information
		ProtocolHelper.selectedProtocolInstance.logData(folder, baseFileName);
		
		// 6. Store ArduCopter logs if needed
		if (Param.role == Tools.SIMULATOR && SimParam.arducopterLoggingEnabled) {
			File logSource, logDestination;
			File parentFolder = new File(SimParam.tempFolderBasePath);
			for (int i=0; i<Param.numUAVs; i++) {
				File tempFolder = new File(parentFolder, SimParam.TEMP_FOLDER_PREFIX + i);
				if (tempFolder.exists()  ) {
					logSource = new File(new File(tempFolder, SimParam.LOG_FOLDER), SimParam.LOG_SOURCE_FILENAME + "." + SimParam.LOG_SOURCE_FILEEXTENSION);
					logDestination = new File(folder, baseFileName + "_" + i + "_" + SimParam.LOG_DESTINATION_FILENAME + "." + SimParam.LOG_SOURCE_FILEEXTENSION);
					try {
						Files.copy(logSource.toPath(), logDestination.toPath());
					} catch (IOException e1) {
					}
				}
			}
		}
	}
	
	/** Logging to file the UAVs mission, in AutoCAD format. */
	private static void logMission(String folder, String baseFileName) {
		File file;
		StringBuilder sb;
		int j;
		for (int i=0; i<Param.numUAVs; i++) {
			file = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_" + Text.MISSION_SUFIX);
			sb = new StringBuilder(2000);
			sb.append("._PLINE\n");
			j = 0;
			List<WaypointSimplified> missionUTMSimplified = Tools.getUAVMissionSimplified(i);
			if (missionUTMSimplified != null && missionUTMSimplified.size() > 1) {
				WaypointSimplified prev = null;
				WaypointSimplified current;
				while (j<missionUTMSimplified.size()) {
					current = missionUTMSimplified.get(j);
					if (prev == null) {
						sb.append(Tools.round(current.x, 3)).append(",").append(Tools.round(current.y, 3)).append("\n");
						prev = current;
					} else if (!current.equals(prev)) {
						sb.append(Tools.round(current.x, 3)).append(",").append(Tools.round(current.y, 3)).append("\n");
						prev = current;
					}
					j++;
				}
				sb.append("\n");
				Tools.storeFile(file, sb.toString());
			}
		}
	}
	
	/** Logging to file the CPU utilization. */
	private static void logCPUUtilization(String folder, String baseFileName) {
		File file = new File(folder, baseFileName + "_" + Text.CPU_SUFIX);
		StringBuilder sb = new StringBuilder(2000);
		sb.append("time(s),GlobalCPU(%),GlobalCPU(%oneCore),JavaCPU(%),JavaCPU(%oneCore),ArduSimState\n");
		CPUData data = Param.cpu.poll();
		while (data != null) {
			sb.append(data.time).append(",")
				.append(Tools.round(data.globalCPU, 1)).append(",")
				.append(Tools.round(data.globalCPU * Param.numCPUs, 2)).append(",")
				.append(data.processCPU/Param.numCPUs).append(",")
				.append(data.processCPU).append(",")
				.append(data.simState).append("\n");
			data = Param.cpu.poll();
		}
		Tools.storeFile(file, sb.toString());
	}

}
