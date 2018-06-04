package main;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.WaypointReachedListener;
import api.pojo.AtomicDoubleArray;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.LastPositions;
import api.pojo.LogPoint;
import api.pojo.UAVCurrentData;
import api.pojo.UAVCurrentStatus;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param.SimulatorState;
import main.Param.WirelessModel;
import pccompanion.PCCompanionGUI;
import sim.board.BoardParam;
import sim.gui.MainWindow;
import sim.logic.FakeSenderThread;
import sim.logic.GPSStartThread;
import sim.logic.InitialConfigurationThread;
import sim.logic.SimParam;
import sim.pojo.IncomingMessage;
import sim.pojo.IncomingMessageQueue;
import sim.pojo.PortScanResult;
import sim.pojo.WinRegistry;
import uavController.UAVControllerThread;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;

/** This class contains general tools used by the simulator. */

public class ArduSimTools {
	
	public static List<WaypointReachedListener> listeners = new ArrayList<WaypointReachedListener>();
	
	/** Parses the command line of the simulator.
	 * <p>Returns false if running a PC companion and the thread execution must stop. */
	public static boolean parseArgs(String[] args) {
		String usageCommand = "java -jar ArduSim.jar -c <arg> [-r <arg> [-p <arg> -s <arg>]] [-doFake -t <arg> -l <arg>] [-h]";
		Option control = Option.builder("c").longOpt("pccompanion").required(true).desc("whether running as a PC companion for real UAVs (true) or not (false)").hasArg(true).build();
		Option realUAV = Option.builder("r").longOpt("realUAV").required(false).desc("whether running in real UAV (true) or not (false)").hasArg(true).build();
		Option protocol = Option.builder("p").longOpt("protocol").required(false).desc("selected protocol when running in real UAV").hasArg(true).build();
		Option sp = Option.builder("s").longOpt("speed").required(false).desc("UAV speed (m/s)").hasArg(true).build();
		Option periodOption = Option.builder("t").longOpt("period").required(false).desc("period between messages (ms)").hasArg(true).build();
		Option sizeOption = Option.builder("l").longOpt("length").required(false).desc("message length (bytes)").hasArg(true).build();
		Option doFakeSendingOption = new Option("doFake", "if true, it activates fake threads to simulate communications behavior.");
		Options options = new Options();
		options.addOption(control);
		options.addOption(realUAV);
		options.addOption(protocol);
		options.addOption(sp);
		options.addOption(periodOption);
		options.addOption(sizeOption);
		options.addOption(doFakeSendingOption);
		Option help = Option.builder("h").longOpt("help").required(false).desc("ussage help").hasArg(false).build();
		options.addOption(help);
		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine;
		boolean pcCompanion = false;
		try {
			cmdLine = parser.parse(options, args);
			HelpFormatter myHelp = new HelpFormatter();
			String con = cmdLine.getOptionValue("c");
			if (con == null || con.length() == 0 || (!con.toUpperCase().equals("TRUE") && !con.toUpperCase().equals("FALSE"))) {
				myHelp.printHelp(usageCommand, options);
				System.exit(1);
			}
			if (con.toUpperCase().equals("TRUE")) {
				pcCompanion = true;
			}
			Param.IS_PC_COMPANION = pcCompanion;
			if (pcCompanion && (cmdLine.hasOption("r") || cmdLine.hasOption("doFake"))) {
				System.out.println(Text.COMPANION_ERROR + "\n");
				myHelp.printHelp(usageCommand, options);
				System.exit(1);
			}
			if (pcCompanion) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						PCCompanionGUI.companion = new PCCompanionGUI();
					}
				});
				return false;
			}
			
			if (cmdLine.hasOption("r")) {
				String iRU = cmdLine.getOptionValue("r");
				if (!iRU.toUpperCase().equals("TRUE") && !iRU.toUpperCase().equals("FALSE")) {
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				Param.IS_REAL_UAV = Boolean.parseBoolean(iRU.toLowerCase());
			} else {
				Param.IS_REAL_UAV = false;
			}
			if (Param.IS_REAL_UAV) {
				if (!cmdLine.hasOption("p") || !cmdLine.hasOption("s")) {
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				String pr = cmdLine.getOptionValue("p");
				ProtocolHelper.Protocol pro = ProtocolHelper.Protocol.getProtocolByName(pr.trim());
				if (pro == null) {
					System.out.println(Text.PROTOCOL_NOT_FOUND_ERROR);
					for (ProtocolHelper.Protocol p : ProtocolHelper.Protocol.values()) {
						System.out.println(p.getName());
					}
					System.out.println("\n");
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				ProtocolHelper.selectedProtocol = pro;
				ProtocolHelper protocolInstance = ArduSimTools.getSelectedProtocolInstance();
				if (protocolInstance == null) {
					System.exit(-1);
				}
				ProtocolHelper.selectedProtocolInstance = protocolInstance;
				Param.simulationIsMissionBased = ProtocolHelper.selectedProtocol.isMissionBased();
				String spe = cmdLine.getOptionValue("s");
				if (!Tools.isValidPositiveDouble(spe)) {
					System.out.println(Text.SPEED_ERROR + "\n");
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				UAVParam.initialSpeeds = new double[1];
				UAVParam.initialSpeeds[0] = Double.parseDouble(spe);
				
			}
			if (cmdLine.hasOption("h")) {
				myHelp.printHelp(usageCommand, options);
				System.exit(0);
			}
			if (cmdLine.hasOption("doFake")) {
				UAVParam.doFakeSending = true;
				if (!cmdLine.hasOption("t") || !cmdLine.hasOption("l")) {
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				String periodString = cmdLine.getOptionValue("t");
				if (!Tools.isValidInteger(periodString) || Integer.parseInt(periodString) > 10000) {
					System.out.println("The period between messages must be a valid positive integer lower or equal to 10000 ms.\n");
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}
				String sizeString = cmdLine.getOptionValue("l");
				if (!Tools.isValidInteger(sizeString) || Integer.parseInt(sizeString) > 1472) {
					System.out.println("The size of the messages must be a valid positive integer lower or equal to 1472 bytes.\n");
					myHelp.printHelp(usageCommand, options);
					System.exit(1);
				}

				FakeSenderThread.period = Integer.parseInt(periodString);
				FakeSenderThread.messageSize = Integer.parseInt(sizeString);
			} else {
				UAVParam.doFakeSending = false;
			}
		} catch (ParseException e1) {
			System.out.println(e1.getMessage() + "\n");
			HelpFormatter myHelp = new HelpFormatter();
			myHelp.printHelp(usageCommand, options);
			System.exit(1);
		}
		return true;
	}

	/** Initializes the data structures at the simulator start. */
	@SuppressWarnings("unchecked")
	public static void initializeDataStructures() {
		UAVParam.uavCurrentData = new UAVCurrentData[Param.numUAVs];
		UAVParam.uavCurrentStatus = new UAVCurrentStatus[Param.numUAVs];
		UAVParam.lastLocations = new LastPositions[Param.numUAVs];
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
		
		SimParam.uavUTMPathReceiving = new ArrayBlockingQueue[Param.numUAVs];
		SimParam.uavUTMPath = new ArrayList[Param.numUAVs];	// Useful for logging purposes
		
		SimParam.prefix = new String[Param.numUAVs];

		Param.testEndTime = new long[Param.numUAVs];
		
		if (!Param.IS_REAL_UAV) {
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
			UAVParam.lastLocations[i] = new LastPositions(UAVParam.LOCATIONS_SIZE);
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
			
			SimParam.uavUTMPathReceiving[i] = new ArrayBlockingQueue<LogPoint>(SimParam.UAV_POS_QUEUE_INITIAL_SIZE);
			SimParam.uavUTMPath[i] = new ArrayList<LogPoint>(SimParam.PATH_INITIAL_SIZE);
			
			if (!Param.IS_REAL_UAV) {
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
		if (Param.IS_REAL_UAV) {
			UAVParam.sendSocket = new DatagramSocket[Param.numUAVs];
			UAVParam.sendPacket = new DatagramPacket[Param.numUAVs];
			UAVParam.receiveSocket = new DatagramSocket[Param.numUAVs];
			UAVParam.receivePacket = new DatagramPacket[Param.numUAVs];
			for (int i = 0; i < Param.numUAVs; i++) {
				try {
					UAVParam.sendSocket[i] = new DatagramSocket();
					UAVParam.sendSocket[i].setBroadcast(true);
					UAVParam.sendPacket[i] = new DatagramPacket(new byte[Tools.DATAGRAM_MAX_LENGTH],
							Tools.DATAGRAM_MAX_LENGTH,
							InetAddress.getByName(UAVParam.BROADCAST_IP),
							UAVParam.BROADCAST_PORT);
					UAVParam.receiveSocket[i] = new DatagramSocket(UAVParam.BROADCAST_PORT);
					UAVParam.receiveSocket[i].setBroadcast(true);
					UAVParam.receivePacket[i] = new DatagramPacket(new byte[Tools.DATAGRAM_MAX_LENGTH], Tools.DATAGRAM_MAX_LENGTH);
				} catch (SocketException | UnknownHostException e) {
					GUI.exit(Text.THREAD_START_ERROR);
				}
			}
		} else {
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
				UAVParam.maxCompletedTEndTime = new long[Param.numUAVs];
			}
			
			UAVParam.packetWaitedPrevSending = new int[Param.numUAVs];
			UAVParam.packetWaitedMediaAvailable = new int[Param.numUAVs];
			UAVParam.receiverOutOfRange = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverWasSending = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverVirtualQueueFull = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.receiverQueueFull = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.successfullyReceived = new AtomicIntegerArray(Param.numUAVs);
			UAVParam.discardedForCollision = new int[Param.numUAVs];
			UAVParam.successfullyEnqueued = new int[Param.numUAVs];
		}
		UAVParam.sentPacket = new int[Param.numUAVs];
		UAVParam.receivedPacket = new int[Param.numUAVs];
		
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
	
	/** Auxiliary method to get the TCP ports available for SITL instances. */
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
				System.out.println(Text.MOUNT_DRIVE_ERROR_1);
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
			System.out.println(Text.MOUNT_DRIVE_ERROR_2);
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

		// Get all Java classes included in ArduSim
		List<String> existingClasses = getClasses();

		// Get classes that extend Protocol class, and implement Metodos class
		if (existingClasses != null && existingClasses.size() > 0) {
			Class<?>[] validImplementations = getAllImplementations(existingClasses);

			// If valid implementations were found, check if the selected protocol is implemented
			if (validImplementations.length > 0) {
				try {
					ProtocolHelper[] protocolImplementations = getProtocolImplementations(validImplementations, ProtocolHelper.selectedProtocol);
					if (protocolImplementations == null) {
						GUI.log(Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + ProtocolHelper.selectedProtocol.getName());
					} else if (protocolImplementations.length > 1) {
						GUI.log(Text.PROTOCOL_MANY_IMPLEMENTATIONS_ERROR + ProtocolHelper.selectedProtocol.getName());
					} else {
						protocolLaunched = protocolImplementations[0];
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException
						| NoSuchFieldException e) {
					GUI.log(Text.PROTOCOL_GETTING_PROTOCOL_CLASSES_ERROR);
				}
			}
		} else {
			GUI.log(Text.PROTOCOL_GETTING_CLASSES_ERROR);
		}
		return protocolLaunched;
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
				String myClass = className.substring(0, className.lastIndexOf('.'));
				res.add(myClass);
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
	
	/** Returns all the classes that contain valid implementation of any protocol.
	 * <p>Returns an array of size 0 if no valid implementations were found.*/
	private static Class<?>[] getAllImplementations(List<String> existingClasses) {
		String className;
		Class<?> currentClass;
		Map<String, Class<?>> classesMap = new HashMap<>();
		for (int i = 0; i < existingClasses.size(); i++) {
			try {
				className = existingClasses.get(i);
				// Ignore special case
				if (!className.toUpperCase().endsWith("WINREGISTRY")) {
					currentClass = Class.forName(existingClasses.get(i));
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
	
	/** Returns all the implementations of the selected protocol among all available implementations.
	 * <p>Returns a valid ProtocolHelper object for each implementation.
	 * <p>Returns null if no valid implementation was found.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchFieldException */
	private static ProtocolHelper[] getProtocolImplementations(Class<?>[] implementations, ProtocolHelper.Protocol selectedProtocol) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		Class<?> c;
		ProtocolHelper o;
		ProtocolHelper[] res = null;
		List<ProtocolHelper> imp = new ArrayList<>();
		for (int i = 0; i < implementations.length; i++) {
			c = implementations[i];
			o = (ProtocolHelper)c.newInstance();
			o.setProtocol();
			if (o.protocol == selectedProtocol) {
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
					if (SimParam.usingRAMDrive && Param.runningOperatingSystem == Param.OS_LINUX) {
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
				if (Param.runningOperatingSystem == Param.OS_LINUX) {
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
				if (Param.runningOperatingSystem == Param.OS_LINUX) {
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
							if (Param.VERBOSE_LOGGING) {
								GUI.log(SimParam.prefix[i] + s);
							}
							if (s.contains("Waiting for connection")) {
								GUI.log(SimParam.prefix[i] + Text.SITL_UP);
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
					System.out.println("Falla la instancia: " + i);
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
			if (Param.VERBOSE_LOGGING){
				GUI.log(Text.OPERATING_SYSTEM_WINDOWS);
			}
		} else if (OS.contains("nux") || OS.contains("nix") || OS.contains("aix")) {
			Param.runningOperatingSystem = Param.OS_LINUX;
			if (Param.VERBOSE_LOGGING){
				GUI.log(Text.OPERATING_SYSTEM_LINUX);
			}
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
		} else if (Param.runningOperatingSystem == Param.OS_LINUX) {
			sitlPath = Tools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_LINUX_FILE_NAME;
		}
		// SITL detection
		if (Param.runningOperatingSystem == Param.OS_WINDOWS
				|| Param.runningOperatingSystem == Param.OS_LINUX) {
			String paramPath = Tools.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.PARAM_FILE_NAME;
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
		if (Param.runningOperatingSystem == Param.OS_LINUX) {
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
		if (!Param.IS_REAL_UAV) {
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
		if (!Param.IS_REAL_UAV) {
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
		Tools.waiting(SimParam.LONG_WAITING_TIME);
		if (Param.IS_REAL_UAV) {
			String shutdownCommand;
		    String operatingSystem = System.getProperty("os.name");

		    if (operatingSystem.startsWith("Linux") || operatingSystem.startsWith("Mac")) {
		        shutdownCommand = "sudo shutdown -h now";
		        try {
			    	Runtime.getRuntime().exec(shutdownCommand);
			    } catch (IOException e) {}
		    }
		    else if (operatingSystem.startsWith("Win")) {
		        shutdownCommand = "shutdown.exe -s -t 0";
		        try {
			    	Runtime.getRuntime().exec(shutdownCommand);
			    } catch (IOException e) {}
		    }
		    else {
		    	GUI.log(Text.SHUTDOWN_ERROR);
		    }
		} else {
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
					System.out.println(Text.DISMOUNT_DRIVE_ERROR);
				}
			}
			
			if (Param.runningOperatingSystem == Param.OS_LINUX) {
				// Unmount temporal filesystem and remove folder
				if (ArduSimTools.checkDriveMountedLinux(SimParam.tempFolderBasePath) && !ArduSimTools.dismountDriveLinux(SimParam.tempFolderBasePath)) {
					System.out.println(Text.DISMOUNT_DRIVE_ERROR);
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
				if (Param.IS_REAL_UAV) {
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
				if (Param.IS_REAL_UAV) {
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
		if (!Param.IS_REAL_UAV) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(Text.UAVS_ONLINE);
				}
			});
		}
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, retrieves controller configuration, loads missions, etc. */
	public static void sendBasicConfiguration() {
		
		GUI.log(Text.SEND_MISSION);
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
		double alarmLevel;
		if (Param.IS_REAL_UAV) {
			alarmLevel = UAVParam.LIPO_BATTERY_ALARM_VOLTAGE;
		} else {
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
				if (Param.IS_REAL_UAV) {
					GUI.log(Text.BATTERY_LEVEL + " " + percentage + " % - " + voltage + " V");
				} else if (Param.VERBOSE_LOGGING) {
					GUI.log(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " % - " + voltage + " V");
				}
			} else if (percentage != -1) {
				if (Param.IS_REAL_UAV) {
					GUI.log(Text.BATTERY_LEVEL + " " + percentage + " %");
				} else if (Param.VERBOSE_LOGGING) {
					GUI.log(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + percentage + " %");
				}
			} else if (voltage != -1) {
				if (Param.IS_REAL_UAV) {
					GUI.log(Text.BATTERY_LEVEL + " " + voltage + " V");
				} else if (Param.VERBOSE_LOGGING) {
					GUI.log(Text.BATTERY_LEVEL2 + " " + Param.id[i] + ": " + voltage + " V");
				}
			}
			if (isDepleted) {
				if (Param.IS_REAL_UAV) {
					GUI.log(Text.BATTERY_FAILING2);
				} else {
					depleted++;
				}
			}
		}
		if (!Param.IS_REAL_UAV && depleted > 0) {
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
		if (ProtocolHelper.selectedProtocol != ProtocolHelper.Protocol.NONE) {
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
		if (Param.IS_REAL_UAV) {
			sb.append("\n").append(Text.GENERAL_PARAMETERS);
			sb.append("\n\t").append(Text.LOG_SPEED).append(UAVParam.initialSpeeds[0]).append(Text.METERS_PER_SECOND);
		} else {
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
			if (UAVParam.batteryCapacity != UAVParam.MAX_BATTERY_CAPACITY) {
				sb.append(Text.YES_OPTION);
				sb.append("\n\t\t").append(UAVParam.batteryCapacity).append(" ").append(Text.BATTERY_CAPACITY);
			} else {
				sb.append(Text.NO_OPTION);
			}
			sb.append("\n\t").append(Text.RENDER).append(" ").append(SimParam.renderQuality.getName());
		}
		sb.append("\n").append(Text.UAV_PROTOCOL_USED).append(" ").append(ProtocolHelper.selectedProtocol.getName());
		sb.append("\n").append(Text.COMMUNICATIONS);
		long sentPacketTot = 0;
		long receivedPacketTot = 0;
		for (int i = 0; i < Param.numUAVs; i++) {
			sentPacketTot = sentPacketTot + UAVParam.sentPacket[i];
			receivedPacketTot = receivedPacketTot + UAVParam.receivedPacket[i];
		}
		if (Param.IS_REAL_UAV) {
			sb.append("\n\t").append(Text.BROADCAST_IP).append(" ").append(UAVParam.BROADCAST_IP);
			sb.append("\n\t").append(Text.BROADCAST_PORT).append(" ").append(UAVParam.BROADCAST_PORT);
			sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(sentPacketTot);
			sb.append("\n\t").append(Text.TOT_PROCESSED).append(" ").append(receivedPacketTot);
		} else {
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
				long equivalentReceived = sentPacketTot * (Param.numUAVs - 1);
				long receiverOutOfRangeTot = 0;
				long receiverWasSendingTot = 0;
				long receiverVirtualQueueFullTot = 0;
				long receiverQueueFullTot = 0;
				long successfullyReceivedTot = 0;
				long discardedForCollisionTot = 0;
				long successfullyEnqueuedTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					packetWaitedPrevSendingTot = packetWaitedPrevSendingTot + UAVParam.packetWaitedPrevSending[i];
					receiverOutOfRangeTot = receiverOutOfRangeTot + UAVParam.receiverOutOfRange.get(i);
					receiverWasSendingTot = receiverWasSendingTot + UAVParam.receiverWasSending.get(i);
					receiverQueueFullTot = receiverQueueFullTot + UAVParam.receiverQueueFull.get(i);
					successfullyReceivedTot = successfullyReceivedTot + UAVParam.successfullyReceived.get(i);
				}
				long totRemainingInBuffer = successfullyReceivedTot - receivedPacketTot;
				if (UAVParam.pCollisionEnabled) {
					for (int i = 0; i < Param.numUAVs; i++) {
						receiverVirtualQueueFullTot = receiverVirtualQueueFullTot + UAVParam.receiverVirtualQueueFull.get(i);
						discardedForCollisionTot = discardedForCollisionTot + UAVParam.discardedForCollision[i];
						successfullyEnqueuedTot = successfullyEnqueuedTot + UAVParam.successfullyEnqueued[i];
					}
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
				sb.append("\n\t").append(Text.TOT_EQUIVALENT_RECEIVED).append(" ").append(equivalentReceived);
				sb.append("\n\t\t").append(Text.TOT_OUT_OF_RANGE).append(" ").append(receiverOutOfRangeTot)
					.append(" (").append(Tools.round((100.0 * receiverOutOfRangeTot)/equivalentReceived, 3)).append("%)");
				sb.append("\n\t\t").append(Text.TOT_LOST_RECEIVER_WAS_SENDING).append(" ").append(receiverWasSendingTot)
					.append(" (").append(Tools.round((100.0 * receiverWasSendingTot)/equivalentReceived, 3)).append("%)");
				if (UAVParam.pCollisionEnabled) {
					sb.append("\n\t\t").append(Text.TOT_VIRTUAL_QUEUE_WAS_FULL).append(" ").append(receiverVirtualQueueFullTot)
						.append(" (").append(Tools.round((100.0 * receiverVirtualQueueFullTot)/equivalentReceived, 3)).append("%)");
				}
				sb.append("\n\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
					.append(" (").append(Tools.round((100.0 * receiverQueueFullTot)/equivalentReceived, 3)).append("%)");
				sb.append("\n\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
					.append(" (").append(Tools.round((100.0 * successfullyReceivedTot)/equivalentReceived, 3)).append("%)");
				if (successfullyReceivedTot > 0) {
					sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(totRemainingInBuffer)
						.append(" (").append(Tools.round((100.0 * totRemainingInBuffer)/successfullyReceivedTot, 3)).append("%)");
					sb.append("\n\t\t\t").append(Text.TOT_PROCESSED).append(" ").append(receivedPacketTot)
						.append(" (").append(Tools.round((100.0 * receivedPacketTot)/successfullyReceivedTot, 3)).append("%)");
					if (receivedPacketTot > 0 && UAVParam.pCollisionEnabled) {
						sb.append("\n\t\t\t\t").append(Text.TOT_DISCARDED_FOR_COLLISION).append(" ").append(discardedForCollisionTot)
							.append(" (").append(Tools.round((100.0 * discardedForCollisionTot)/receivedPacketTot, 3)).append("%)");
						sb.append("\n\t\t\t\t").append(Text.TOT_ENQUEUED_OK).append(" ").append(successfullyEnqueuedTot)
							.append(" (").append(Tools.round((100.0 * successfullyEnqueuedTot)/receivedPacketTot, 3)).append("%)");
					}
				}
			}
			
			sb.append("\n").append(Text.COLLISION);
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
		File file1, file2, file3;
		StringBuilder sb1, sb2, sb3;
		LogPoint sp;
		LogPoint spPrev;
		Double x, y, a;
		for (int i=0; i<Param.numUAVs; i++) {
			file1 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_path.csv");
			sb1 = new StringBuilder(2000);
			sb1.append("x(m),y(m),z(m),t(arbitrary ns),s(m/s),a(m/s\u00B2),\u0394d(m),d(m)\n");
			file2 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_path_AutoCAD.scr");
			sb2 = new StringBuilder(2000);
			sb2.append("._PLINE\n");

			int j = 0;
			double dist = 0; // Accumulated distance to origin
			double d;
			spPrev = null;
			y = x = null;
			
			if (Param.VERBOSE_STORE) {
				file3 = new File(folder + File.separator + baseFileName + "_" + Param.id[i] + "_path_AutoCAD3d.scr");
				sb3 = new StringBuilder(2000);
				sb3.append("._3DPOLY\n");

			}

			while (j<SimParam.uavUTMPath[i].size()) {
				sp = SimParam.uavUTMPath[i].get(j);
				// Calculus of the unfiltered acceleration
				if (j == 0) {
					a = 0.0;
				} else {
					a = (SimParam.uavUTMPath[i].get(j).speed - SimParam.uavUTMPath[i].get(j-1).speed)/
							(SimParam.uavUTMPath[i].get(j).time - SimParam.uavUTMPath[i].get(j-1).time)*1000000000l;
				}

				// Considers only not repeated locations and under test
				if (SimParam.uavUTMPath[i].get(j).inTest) {
					x = sp.x;
					y = sp.y;
					if (spPrev == null) {
						// First test location
						sb1.append(Tools.round(x, 3)).append(",")
							.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3))
							.append(",").append(SimParam.uavUTMPath[i].get(j).time)
							.append(",").append(Tools.round(SimParam.uavUTMPath[i].get(j).speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",0.000,0.000\n");
						sb2.append(Tools.round(x, 3)).append(",")
							.append(Tools.round(y, 3)).append("\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(Tools.round(x, 3)).append(",")
								.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3)).append("\n");
						}
						spPrev = sp;
					} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
						// Moved horizontally
						d = sp.distance(spPrev);
						dist = dist + d;
						sb1.append(Tools.round(x, 3)).append(",")
							.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3))
							.append(",").append(SimParam.uavUTMPath[i].get(j).time)
							.append(",").append(Tools.round(SimParam.uavUTMPath[i].get(j).speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",").append(Tools.round(d, 3))
							.append(",").append(Tools.round(dist, 3)).append("\n");
						sb2.append(Tools.round(x, 3)).append(",")
							.append(Tools.round(y, 3)).append("\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(Tools.round(x, 3)).append(",")
								.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3)).append("\n");
						}
						spPrev = sp;
					} else if (sp.z!=spPrev.z) {
						// Only moved vertically
						sb1.append(Tools.round(x, 3)).append(",")
							.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3))
							.append(",").append(SimParam.uavUTMPath[i].get(j).time)
							.append(",").append(Tools.round(SimParam.uavUTMPath[i].get(j).speed, 3))
							.append(",").append(Tools.round(a, 3)).append(",0.0,").append(Tools.round(dist, 3)).append("\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(Tools.round(x, 3)).append(",")
								.append(Tools.round(y, 3)).append(",").append(Tools.round(sp.z, 3)).append("\n");
						}
						spPrev = sp;
					}
				}
				j++;
			}
			Tools.storeFile(file1, sb1.toString());
			sb2.append("\n");
			Tools.storeFile(file2, sb2.toString());
			if (Param.VERBOSE_STORE) {
				sb3.append("\n");
				Tools.storeFile(file3, sb3.toString());
			}
		}
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
		logMission(folder, baseFileName);
		
		// 4. Store protocol specific information
		ProtocolHelper.selectedProtocolInstance.logData(folder, baseFileName);
		// 5. Store ArduCopter logs if needed
		if (SimParam.arducopterLoggingEnabled) {
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

}
