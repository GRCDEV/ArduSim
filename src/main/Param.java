package main;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import main.cpuHelper.CPUData;
import uavController.UAVControllerThread;

/** This class contains general parameters of the application. */

public class Param {
	
	// Role of ArduSim
	public static int role;
	
	// Name of the parameters loaded from INI file
	public static final String COMPUTER_PORT = "COMPUTERPORT";
	public static final String UAV_PORT = "UAVPORT";
	public static final String PROTOCOL = "UAVPROTOCOL";
	public static final String SPEED = "UAVSPEED";
	public static final String WAYPOINT_DELAY = "WAYPOINTDELAY";
	public static final String MISSION_END = "MISSIONEND";
	public static final String SERIAL_PORT = "SERIALPORT";
	public static final String BAUD_RATE = "BAUDRATE";
	public static final String BROADCAST_IP = "BROADCASTIP";
	public static final String BROADCAST_PORT = "BROADCASTPORT";
	public static final String BATTERY_CELLS = "BATTERYCELLS";
	public static final String BATTERY_CAPACITY = "BATTERYCAPACITY";
	public static final String MEASURE_CPU = "MEASURECPU";
	public static final String VERBOSE_LOGGING = "VERBOSELOGGING";
	public static final String VERBOSE_STORE = "VERBOSESTORE";
	public static final String MIN_ALTITUDE = "MINALTITUDE";
	public static final String[] PARAMETERS = {COMPUTER_PORT, UAV_PORT, PROTOCOL, SPEED, WAYPOINT_DELAY, MISSION_END, SERIAL_PORT, BAUD_RATE,
			BROADCAST_IP, BROADCAST_PORT, BATTERY_CELLS, BATTERY_CAPACITY, MEASURE_CPU, VERBOSE_LOGGING, VERBOSE_STORE, MIN_ALTITUDE};
	
	// Number of UAVs to be simulated
	public static int numUAVs;
	public static AtomicInteger numUAVsTemp = new AtomicInteger();	// Used as semaphore to modify numUAVs in configuration dialogs
	
	// Unique UAV identifier that must be used on protocols, based on MAC address (simple enumeration while in simulator)
	public static long[] id;
	
	// Number of UAVs that need a mission to be loaded
	public static AtomicInteger numMissionUAVs = new AtomicInteger();
	
	// Verbose logging?
	public static volatile boolean verboseLogging = false;
	
	// Store additional information in files?
	public static volatile boolean verboseStore = true;
	
	// Array containing the UAV controllers. Useful to set a listener to detect when a waypoint is reached, if needed.
	public static UAVControllerThread[] controllers;

	// Running Operating System
	public static final int OS_WINDOWS = 0;
	public static final int OS_LINUX = 1;
	public static final int OS_MAC = 2;
	public static volatile int runningOperatingSystem;
	
	// Wind parameters
	public static final int DEFAULT_WIND_DIRECTION = 180;	// (degrees) North wind by default. Integer values only
	public static final double DEFAULT_WIND_SPEED = 0.0;
	public static volatile int windDirection = Param.DEFAULT_WIND_DIRECTION;
	public static volatile double windSpeed = Param.DEFAULT_WIND_SPEED;

	// Statistics parameters
	public static volatile long startTime;	// (ms) experiment start in local time
	public static long[] testEndTime;		// (ms) one UAV experiment finish in local time
	public static long latestEndTime;		// (ms) experiment finish in local time

	// Selected wireless model
	public static WirelessModel selectedWirelessModel;
	public static double fixedRange = 800.0;				// (m) Fixed range distance threshold
	public static final double FIXED_MAX_RANGE = 1500.0;	// (m) Fixed range maximum distance accepted
	
	// CPU usage parameters
	public static boolean measureCPUEnabled = false;	// Whether the CPU utilization must be measured or not
	public static int numCPUs;							// Number of cores available
	public static ConcurrentLinkedQueue<CPUData> cpu = new ConcurrentLinkedQueue<>();	// CPU usage data
	public static final int CPU_CHECK_PERIOD = 1;		// (s) Time between measurements
	public static final long CPU_CONSOLE_TIMEOUT = 50;	// (ms) Timeout waiting the console to read a line when retrieving CPU usage data
	
	// Simulator state
	public static volatile SimulatorState simStatus;
	
	// Simulator finite state machine states enumerator
	public enum SimulatorState {
		CONFIGURING(0),
		CONFIGURING_PROTOCOL(1),
		STARTING_UAVS(2),
		UAVS_CONFIGURED(3),
		SETUP_IN_PROGRESS(4),
		READY_FOR_TEST(5),
		TEST_IN_PROGRESS(6),
		TEST_FINISHED(7),
		SHUTTING_DOWN(8);
		
		private final int id;
		private SimulatorState(int id) {
			this.id = id;
		}
		public int getStateId() {
			return this.id;
		}
		public static SimulatorState getStateById(int id) {
			for (SimulatorState e : SimulatorState.values()) {
				if (e.getStateId() == id) {
					return e;
				}
			}
			return null;
		}
	}
	
	// Wireless models enumerator
	public enum WirelessModel {
		NONE(0, Text.WIRELESS_MODEL_NONE),					// Unrestricted model
		FIXED_RANGE(1, Text.WIRELESS_MODEL_FIXED_RANGE),	// Fixed distance model
		DISTANCE_5GHZ(2, Text.WIRELESS_MODEL_5GHZ);			// Real distance model based on WiFi 5.18 GHz (channel 36) 
		// New models should follow the increasing numeration. The method Tools.isInRange() must also be modified
		
		private final int id;
		private final String name;
		private WirelessModel(int id, String name) {
			this.id = id;
			this.name = name;
		}
		public int getId() {
			return this.id;
		}
		public String getName() {
			return this.name;
		}
		public static WirelessModel getHighestIdModel() {
			WirelessModel res = null;
			for (WirelessModel p : WirelessModel.values()) {
				if (res == null) {
					res = p;
				} else {
					if (p.getId() > res.getId()) {
						res = p;
					}
				}
			}
			return res;
		}
		public static String getModelNameById(int id) {
			for (WirelessModel p : WirelessModel.values()) {
				if (p.getId() == id) {
					return p.getName();
				}
			}
			return "";
		}
		public static WirelessModel getModelByName(String name) {
			for (WirelessModel p : WirelessModel.values()) {
				if (p.getName().equals(name)) {
					return p;
				}
			}
			return null;
		}
	}

	
	
}
