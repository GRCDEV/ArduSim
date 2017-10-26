package main;

import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.javatuples.Pair;

import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.LastPositions;
import api.pojo.LogPoint;
import api.pojo.UAVCurrentData;
import api.pojo.Waypoint;
import api.pojo.WaypointSimplified;
import main.Param.Protocol;
import main.Param.SimulatorState;
import main.Param.WirelessModel;
import mbcap.logic.MBCAPText;
import sim.board.BoardParam;
import sim.gui.MainWindow;
import sim.logic.GPSStartThread;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVControllerThread;
import uavController.UAVParam;
import uavController.UAVParam.ControllerParam;
import uavController.UAVParam.Mode;
import sim.logic.InitialConfigurationThread;

/** This class contains general tools used by the simulator. */

public class Tools {

	/** Loads a mission from file when using a real UAV.
	 * <p>Priority loading: 1st xml, 2nd waypoints, 3rd txt.
	 * <p>Only the first valid file/mission found is used.
	 * <p>Returns null if no valid mission was found. */
	public static List<Waypoint> loadMission(File parentFolder) {
		// 1. kml file case
		File[] files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_KML) && pathname.isFile();
			}
		});
		if (files != null && files.length>0) {
			// Only one kml file must be on the current folder
			if (files.length>1) {
				GUIHelper.exit(Text.MISSIONS_ERROR_6);
			}
			List<Waypoint>[] missions = MissionHelper.loadXMLMissionsFile(files[0]);
			if (missions != null && missions.length>0) {
				SimTools.println(Text.MISSION_XML_SELECTED + " " + files[0].getName());
				return missions[0];
			}
		}
		
		// 2. kml file not found or not valid. waypoints file case
		files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_WAYPOINTS) && pathname.isFile();
			}
		});
		List<Waypoint> mission;
		if (files != null && files.length>0) {
			// Only one waypoints file must be on the current folder
			if (files.length>1) {
				GUIHelper.exit(Text.MISSIONS_ERROR_7);
			}
			mission = MissionHelper.loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				SimTools.println(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		
		// 3. waypoints file not found or not valid. txt file case
		files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_TXT) && pathname.isFile();
			}
		});
		if (files != null && files.length>0) {
			// Not checking single txt file existence, as other file types can use the same extension
			mission = MissionHelper.loadMissionFile(files[0].getAbsolutePath());
			if (mission != null) {
				SimTools.println(Text.MISSION_WAYPOINTS_SELECTED + "\n" + files[0].getName());
				return mission;
			}
		}
		return null;
	}
	
	/** Loads a speed from csv file when using a real UAV.
	 * <p>Only the first valid file/speed value found is used.
	 * <p>Returns null if no valid speed value was found. */
	public static Double loadSpeed(File parentFolder) {
		File[] files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_CSV) && pathname.isFile();
			}
		});
		if (files != null && files.length>0) {
			// Only one csv file must be on the current folder
			if (files.length>1) {
				GUIHelper.exit(Text.SPEEDS_ERROR_3);
			}
			double[] speeds = SimTools.loadSpeedsFile(files[0].getAbsolutePath());
			if (speeds != null && speeds.length > 0) {
				SimTools.println(Text.SPEEDS_CSV_SELECTED + "\n" + files[0].getName());
				if (speeds[0] <= 0) {
					GUIHelper.exit(Text.SPEEDS_ERROR_4);
				}
				return speeds[0];
			}
		}
		return null;
	}
	
	/** Loads from a txt file the protocol to be appplied when using a real UAV.
	 * <p>Only the first valid file/protocol name found is used.
	 * <p>Returns null if no valid protocol name was found. */
	public static Protocol loadProtocol(File parentFolder) {
		File[] files = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(Text.FILE_EXTENSION_TXT) && pathname.isFile();
			}
		});
		Protocol prot = null;
		if (files != null && files.length>0) {
			for (int i=0; i<files.length; i++) {
				// Trying to read one file
				List<String> lines = new ArrayList<>();
				try (BufferedReader br = Files.newBufferedReader(files[i].toPath())) {
					lines = br.lines().collect(Collectors.toList());
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				// Check file length
				if (lines==null || lines.size()<1) {
					continue;
				}
				String protName = lines.get(0).trim(); 
				prot = Protocol.getProtocolByName(protName);
				if (prot != null) {
					return prot;
				}
			}
		}
		return prot;
	}
	
	/** Initializes the data structures at the simulator start. */
	@SuppressWarnings("unchecked")
	public static void initializeDataStructures() {
		UAVParam.uavCurrentData = new UAVCurrentData[Param.numUAVs];
		UAVParam.lastLocations = new LastPositions[Param.numUAVs];
		UAVParam.flightMode = new AtomicReferenceArray<Mode>(Param.numUAVs);
		UAVParam.MAVStatus = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentWaypoint = new AtomicIntegerArray(Param.numUAVs);
		UAVParam.currentGeoMission = new ArrayList[Param.numUAVs];
		UAVParam.RTLAltitude = new double[Param.numUAVs];
		UAVParam.RTLAltitudeFinal = new double[Param.numUAVs];

		UAVParam.newFlightMode = new Mode[Param.numUAVs];
		UAVParam.takeOffAltitude = new double[Param.numUAVs];
		UAVParam.newSpeed = new double[Param.numUAVs];
		UAVParam.newParam = new ControllerParam[Param.numUAVs];
		UAVParam.newParamValue = new double[Param.numUAVs];
		UAVParam.newCurrentWaypoint = new int[Param.numUAVs];
		UAVParam.newGeoMission = new Waypoint[Param.numUAVs][];
		UAVParam.stabilizationThrottle = new int[Param.numUAVs];
		UAVParam.newLocation = new float[Param.numUAVs][3];

		UAVParam.missionUTMSimplified = new AtomicReferenceArray<List<WaypointSimplified>>(Param.numUAVs);
		UAVParam.lastWaypointReached = new boolean[Param.numUAVs];
		
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
			UAVParam.lastLocations[i] = new LastPositions(UAVParam.LOCATIONS_SIZE);
			UAVParam.currentGeoMission[i] = new ArrayList<Waypoint>(UAVParam.WP_LIST_SIZE);
			
			UAVParam.lastWaypointReached[i] = false;
			
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
	
	/** Auxiliary method to get a number of available TCP ports, starting from an initial port. */
	public static Integer[] getAvailablePorts(int numPorts, int initialPort) throws InterruptedException, ExecutionException {
		ExecutorService es = Executors.newFixedThreadPool(20);
	    List<Future<PortScanResult>> futures = new ArrayList<>();
	    List<Integer> ports = new ArrayList<Integer>(numPorts);
	    int usedPorts = 0;
	    int port = initialPort;
	    int i = 0;
	    int testPort;
	    while (port <= UAVParam.MAX_PORT && ports.size() < numPorts) {
	    	futures.clear();
	    	while (i < numPorts + usedPorts) {
	        	futures.add(portIsOpen(es, UAVParam.MAV_NETWORK_IP, port, UAVParam.PORT_CHECK_TIMEOUT));
	        	port = port + 10;
	    		i++;
	    	}
	    	es.awaitTermination((long)UAVParam.PORT_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
	        for (final Future<PortScanResult> f : futures) {
	        	testPort = f.get().getPort();
	            if (f.get().isOpen()) {
	            	usedPorts++;
	            } else {
	            	ports.add(testPort);
	            }
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
	        try (FileReader reader = new FileReader("/proc/net/dev")) {
	            BufferedReader in = new BufferedReader(reader);
	            String line = null;
	            while( (line = in.readLine()) != null) {
	                Matcher m = pattern.matcher(line);
	                if (m.find()) {
	                	// Ignore loopback interface
	                	String nameFound = m.group(1);
	                	if (!nameFound.equals("lo")) {
	                		interfaces.add(nameFound);
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
	        }
		}
		if (ids.size() == 0) {
			GUIHelper.exit(Text.MAC_ERROR);
		}
		return Collections.max(ids);
	}
	
	/** Gets the folder for temporary files, and creates a RAM disk if necessary. Return null in case of error. */
	public static String defineTemporaryFolder() {
		// If possible, use a virtual RAM drive
		if (SimParam.userIsAdmin) {
			if (Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled) {
				// Check if temporal filesystem is already mounted and dismount
				File ramDiskFile = Tools.checkDriveMountedWindows();
				if (ramDiskFile != null) {
					if (!Tools.dismountDriveWindows(ramDiskFile.getAbsolutePath())) {
						return null;
					}
				}
				ramDiskFile = Tools.mountDriveWindows();
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
					if (Tools.checkDriveMountedLinux(ramDiskPath)) {
						if (!Tools.dismountDriveLinux(ramDiskPath)) {
							return null;
						}
					}
				} else {
					// Create the folder
					ramDiskFile.mkdirs();
				}
				if (Tools.mountDriveLinux(ramDiskPath)) {
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
				String result = input.lines().collect(Collectors.joining("\n"));
				if (result.endsWith("Done.")) {
					return Tools.checkDriveMountedWindows();
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
			String result = input.lines().collect(Collectors.joining("\n"));
			if (result.endsWith("Done.")) {
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
			String rootUsers = input.lines().collect(Collectors.joining("\n"));
			if (rootUsers.contains(diskPath)) {
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
			String result = input.lines().collect(Collectors.joining("\n"));
			if (result.length() == 0) {
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
			String result = input.lines().collect(Collectors.joining("\n"));
			if (result.length() == 0) {
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
	
	/** Checks if the application is stand alone (.jar) or it is running inside an IDE. */
	public static boolean isRunningFromJar() {
		String file = Tools.class.getResource("/" + Tools.class.getName().replace('.', '/') + ".class").toString();
		if (file.startsWith("jar:")) {
			return true;
		} else {
			return false;
		}
	}
	
	/** Starts the virtual UAVs. */
	public static void startVirtualUAVs(Pair<GeoCoordinates, Double>[] location) {
		
		SimTools.println(Text.STARTING_UAVS);
		
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
						Tools.deleteFolder(tempFolder);
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
				SimTools.println(Text.UAVS_START_ERROR_2 + "\n" + parentFolder);
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
								SimTools.println(SimParam.prefix[i] + s);
							}
							if (s.contains("Waiting for connection")) {
								SimTools.println(SimParam.prefix[i] + Text.SITL_UP);
								success = true;
							}
						} else {
							GUIHelper.waiting(SimParam.CONSOLE_READ_RETRY_WAITING_TIME);
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
			GUIHelper.exit(Text.APP_NAME + " " + Text.UAVS_START_ERROR_1);
		}
	}

	/** Removes a folder recursively. */
	public static void deleteFolder (File file) throws IOException {
		for (File childFile : file.listFiles()) {
			if (childFile.isDirectory()) {
				Tools.deleteFolder(childFile);
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
				SimTools.println(Text.OPERATING_SYSTEM_WINDOWS);
			}
		} else if (OS.contains("nux") || OS.contains("nix") || OS.contains("aix")) {
			Param.runningOperatingSystem = Param.OS_LINUX;
			if (Param.VERBOSE_LOGGING){
				SimTools.println(Text.OPERATING_SYSTEM_LINUX);
			}
		} else {
			GUIHelper.exit(Text.UAVS_START_ERROR_4);
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
				GUIHelper.exit(Text.UAVS_START_ERROR_3 + "\n" + SimParam.CYGWIN_PATH1 + " or,\n" + SimParam.CYGWIN_PATH2);
			}
			sitlPath = GUIHelper.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_WINDOWS_FILE_NAME;
		} else if (Param.runningOperatingSystem == Param.OS_LINUX) {
			sitlPath = GUIHelper.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.SITL_LINUX_FILE_NAME;
		}
		// SITL detection
		if (Param.runningOperatingSystem == Param.OS_WINDOWS
				|| Param.runningOperatingSystem == Param.OS_LINUX) {
			String paramPath = GUIHelper.getCurrentFolder().getAbsolutePath() + File.separator + SimParam.PARAM_FILE_NAME;
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
				String rootUsers = input.lines().collect(Collectors.joining("\n"));
				if (rootUsers.contains(userName)) {
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
		}
		SimParam.imdiskIsInstalled = false;
	}
	
	/** Closes the SITL simulator instances, and the application. Also the Cygwin consoles if using Windows. */
	public static void shutdown(JFrame mainWindowFrame) {
		// 1. Ask for confirmation if the experiment has not started or finished
		if (!Param.IS_REAL_UAV) {
			boolean reallyClose = true;
			if (Param.simStatus != SimulatorState.TEST_FINISHED) {
				Object[] options = {Text.YES_OPTION, Text.NO_OPTION};
				int result = JOptionPane.showOptionDialog(mainWindowFrame,
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

		// 2. Change the simulator status and close SITL
		Param.simStatus = SimulatorState.SHUTTING_DOWN;
		if (!Param.IS_REAL_UAV) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.exitButton.setEnabled(false);
					MainWindow.buttonsPanel.statusLabel.setText(Text.SHUTTING_DOWN);
				}
			});

			Tools.closeSITL();
		}
		
		// 3. Close the application
		SimTools.println(Text.EXITING);
		GUIHelper.waiting(SimParam.LONG_WAITING_TIME);
		if (!Param.IS_REAL_UAV) {
			mainWindowFrame.dispose();
		}
		System.exit(0);
	}
	
	/** Closes the SITL simulator instances and removes temporary files and folders. */
	public static void closeSITL() {
		// 1. Close Cygwin under Windows or SITL under Linux
		for (int i=0; i<Param.numUAVs; i++) {
			if (SimParam.processes[i] != null) {
				SimParam.processes[i].destroyForcibly();
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
				if (Tools.checkDriveMountedWindows() != null && !Tools.dismountDriveWindows(SimParam.tempFolderBasePath)) {
					System.out.println(Text.DISMOUNT_DRIVE_ERROR);
				}
			}
			
			if (Param.runningOperatingSystem == Param.OS_LINUX) {
				// Unmount temporal filesystem and remove folder
				if (Tools.checkDriveMountedLinux(SimParam.tempFolderBasePath) && !Tools.dismountDriveLinux(SimParam.tempFolderBasePath)) {
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
				for (int i=0; i<Param.numUAVs; i++) {
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
			SimTools.println(Text.CONTROLLERS_STARTED);
		} catch (SocketException e) {
			GUIHelper.exit(Text.THREAD_START_ERROR);
		}
	}
	
	/** Waits until the MAVLink link is available in all executing UAVs. */
	public static void waitMAVLink() {
		long time = System.nanoTime();
		boolean allReady = false;
		while (!allReady) {
			if (UAVParam.numMAVLinksOnline.get() == Param.numUAVs) {
				allReady = true;
			}

			if (System.nanoTime() - time > UAVParam.MAVLINK_ONLINE_TIMEOUT) {
				break;
			}
			if (!allReady) {
				GUIHelper.waiting(UAVParam.MAVLINK_WAIT);
			}
		}
		if (!allReady) {
			GUIHelper.exit(Text.MAVLINK_ERROR);
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
			GUIHelper.exit(Text.INITIAL_CONFIGURATION_ERROR_1);
		}
	}
	
	/** Waits until GPS is available in all executing UAVs. */
	public static void getGPSFix() {
		long time = System.nanoTime();
		boolean startProcess = false;
		while (!startProcess) {
			if (UAVParam.numGPSFixed.get() == Param.numUAVs) {
				startProcess = true;
			}
			if (System.nanoTime() - time > UAVParam.GPS_FIX_TIMEOUT) {
				break;
			}
			if (!startProcess) {
				GUIHelper.waiting(UAVParam.GPS_FIX_WAIT);
			}
		}
		if (!startProcess) {
			GUIHelper.exit(Text.GPS_FIX_ERROR);
		}
		SimTools.println(Text.GPS_OK);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MainWindow.buttonsPanel.statusLabel.setText(Text.UAVS_ONLINE);
			}
		});
	}
	
	/** Sends the initial configuration: increases battery capacity, sets wind configuration, retrieves controller configuration, loads missions, etc. */
	public static void sendBasicConfiguration() {
		
		MissionHelper.log(Text.SEND_MISSION);
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
			GUIHelper.exit(Text.INITIAL_CONFIGURATION_ERROR_2);
		}
	}
	
	/** Detects if all the UAVs have finished the experiment (they are all on the ground, with engines not armed). */
	public static boolean isTestFinished() {
		boolean allFinished = true;
		for (int i = 0; i < Param.numUAVs; i++) {
			Mode mode = UAVParam.flightMode.get(i);
			if (mode.getBaseMode() < UAVParam.MIN_MODE_TO_BE_FLYING) {
				if (Param.testEndTime[i] == 0) {
					Param.testEndTime[i] = System.currentTimeMillis();
				}
			} else {
				allFinished = false;
			}
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
		if (Param.selectedProtocol != Protocol.NONE) {
			sb.append(Text.LOG_GLOBAL + ":\n\n");
		}
		long totalTime = maxTime - Param.startTime;
		long[] uavsTotalTime = new long[Param.numUAVs];
		for (int i = 0; i < Param.numUAVs; i++) {
			uavsTotalTime[i] = Param.testEndTime[i] - Param.startTime;
		}

		// 2. Global times
		sb.append(Text.LOG_TOTAL_TIME + ": " + GUIHelper.timeToString(0, totalTime) + "\n");
		for (int i = 0; i < Param.numUAVs; i++) {
			sb.append(Text.UAV_ID + " " + Param.id[i] + ": " + GUIHelper.timeToString(0, uavsTotalTime[i]) + "\n");
		}

		// 3. Protocol times header, if needed
		if (Param.selectedProtocol != Protocol.NONE) {
			sb.append("\n" + Param.selectedProtocol.getName() + ":\n\n");
		}
		
		return sb.toString();
	}
	
	/** Builds a String with the experiment parameters. */
	public static String getTestGlobalConfiguration() {
		// 1. Global configuration
		StringBuilder sb = new StringBuilder(2000);
		if (Param.IS_REAL_UAV) {
			sb.append("\n" + Text.GENERAL_PARAMETERS + "\n" + Text.LOG_SPEED + " (" + Text.METERS_PER_SECOND + "): " + UAVParam.initialSpeeds[0]);
		} else {
			sb.append("\n" + Text.SIMULATION_PARAMETERS + "\n" + Text.UAV_NUMBER + " " + Param.numUAVs
					+ "\n" + Text.LOG_SPEED + " (" + Text.METERS_PER_SECOND + "):\n");
			for (int i=0; i<Param.numUAVs; i++) {
				sb.append(UAVParam.initialSpeeds[i]);
				if (i%10 == 9) {
					sb.append("\n");
				} else if (i != Param.numUAVs - 1) {
					sb.append(", ");
				}
			}
			sb.append("\n" + Text.PERFORMANCE_PARAMETERS + "\n\t" + Text.SCREEN_REFRESH_RATE + " " + BoardParam.screenDelay
					+ " " + Text.MILLISECONDS + "\n\t" + Text.REDRAW_DISTANCE + " " + BoardParam.minScreenMovement
					+ " " + Text.PIXELS);
		}
		
		if (Param.selectedProtocol != Protocol.NONE) {
			sb.append("\n" + Text.UAV_PROTOCOL_USED + " " + Param.selectedProtocol.getName());
		}
		if (!Param.IS_REAL_UAV) {
			sb.append("\n" + Text.WIFI_MODEL + " " + Param.selectedWirelessModel.getName());
			if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
				sb.append(": " + Param.fixedRange + " " + Text.METERS);
			}
			sb.append("\n" + Text.ENABLE_WIND + " ");
			if (Param.windSpeed == Param.DEFAULT_WIND_SPEED
					&& Param.windDirection == Param.DEFAULT_WIND_DIRECTION) {
				// There is no wind
				sb.append(Text.NO_OPTION);
			} else {
				sb.append(Text.YES_OPTION+ "\n\t" + Text.WIND_SPEED + " " + Param.windSpeed
						+ "\n\t" + Text.WIND_DIRECTION + " " + Param.windDirection + " " + Text.DEGREE_SYMBOL);
			}
		}
		
		// 2. Protocol configuration, if needed
		if (Param.selectedProtocol != Protocol.NONE) {
			sb.append("\n\n" + Param.selectedProtocol.getName() + " " + MBCAPText.CONFIGURATION + ":\n");
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
						sb1.append(x + "," + y + "," + sp.z
						+ "," + SimParam.uavUTMPath[i].get(j).time
						+ "," + SimParam.uavUTMPath[i].get(j).speed + "," + a + ",0.0,0.0\n");
						sb2.append(x + "," + y + "\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(x + "," + y + "," + sp.z + "\n");
						}
						spPrev = sp;
					} else if (sp.x!=spPrev.x || sp.y!=spPrev.y) {
						// Moved horizontally
						d = sp.distance(spPrev);
						dist = dist + d;
						sb1.append(x + "," + y + "," + sp.z
						+ "," + SimParam.uavUTMPath[i].get(j).time
						+ "," + SimParam.uavUTMPath[i].get(j).speed
						+ "," + a + "," + d + "," + dist + "\n");
						sb2.append(x + "," + y + "\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(x + "," + y + "," + sp.z + "\n");
						}
						spPrev = sp;
					} else if (sp.z!=spPrev.z) {
						// Only moved vertically
						sb1.append(x + "," + y + "," + sp.z
						+ "," + SimParam.uavUTMPath[i].get(j).time
						+ "," + SimParam.uavUTMPath[i].get(j).speed
						+ "," + a + ",0.0," + dist + "\n");
						if (Param.VERBOSE_STORE) {
							sb3.append(x + "," + y + "," + sp.z + "\n");
						}
						spPrev = sp;
					}
				}
				j++;
			}
			GUIHelper.storeFile(file1, sb1.toString());
			sb2.append("\n");
			GUIHelper.storeFile(file2, sb2.toString());
			if (Param.VERBOSE_STORE) {
				sb3.append("\n");
				GUIHelper.storeFile(file3, sb3.toString());
			}
		}
	}
	
	/** Stores the experiment results. */
	public static void storeResults(String results, File file) {
		// Extracting the name of the file without path or extension for later logging
		String folder = file.getParent();
		String baseFileName = file.getName();
		String extension = GUIHelper.getFileExtension(file);
		if (extension.length() > 0) {
			baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(extension) - 1); // Remove also the dot
		}
		
		// 1. Store results
		// Add extension if needed
		if (!file.getAbsolutePath().toUpperCase().endsWith("." + Text.FILE_EXTENSION_TXT.toUpperCase())) {
			file = new File(file.toString() + "." + Text.FILE_EXTENSION_TXT);
		}
		// Store simulation and protocol progress information and configuration
		GUIHelper.storeFile(file, results);
		
		// 2. Store the UAV path and general information
		Tools.storeLogAndPath(folder, baseFileName);
		// 3. Store protocol specific information
		if (Param.simulationIsMissionBased) {
			MissionHelper.logMissionData(folder, baseFileName);
		} else {
			SwarmHelper.logSwarmData(folder, baseFileName);
		}
	}

}
