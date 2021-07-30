package com.setup;

import com.api.communications.WirelessModel;
import com.api.cpuHelper.CPUData;
import com.uavController.UAVControllerThread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** This class contains general parameters of the application.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Param {
	
	// Role of ArduSim
	public static int role;
	
	// Name of the parameters loaded from INI file
	public static final String COMPUTER_PORT = "COMPUTERPORT";
	public static final String UAV_PORT = "UAVPORT";
	public static final String PROTOCOL = "UAVPROTOCOL";
	public static final String SPEED = "UAVSPEED";
	public static final String MACS = "MACS";
	public static final String ASSIGNMENT_ALGORITHM = "ASSIGNMENTALGORITHM";
	public static final String SERIAL_PORT = "SERIALPORT";
	public static final String BAUD_RATE = "BAUDRATE";
	public static final String BROADCAST_IP = "BROADCASTIP";
	public static final String BROADCAST_PORT = "BROADCASTPORT";
	public static final String BATTERY_CELLS = "BATTERYCELLS";
	public static final String BATTERY_CAPACITY = "BATTERYCAPACITY";
	public static final String AIR_FORMATION = "AIRFORMATION";
	public static final String AIR_DISTANCE = "AIRDISTANCE";
	public static final String LAND_DISTANCE = "LANDDISTANCE";
	public static final String MEASURE_CPU = "MEASURECPU";
	public static final String VERBOSE_LOGGING = "VERBOSELOGGING";
	public static final String VERBOSE_STORE = "VERBOSESTORE";
	public static final String WAYPOINT_YAW_OVERRIDE = "YAWOVERRIDE";
	public static final String WAYPOINT_YAW_VALUE = "YAWVALUE";
	public static final String KML_MIN_ALTITUDE = "KMLMINALTITUDE";
	public static final String KML_OVERRIDE_ALTITUDE = "KMLOVERRIDEALTITUDE";
	public static final String KML_ALTITUDE = "KMLALTITUDE";
	public static final String KML_MISSION_END = "KMLMISSIONEND";
	public static final String KML_RTL_END_ALT = "KMLRTLENDALTITUDE";
	public static final String KML_WAYPOINT_DELAY = "KMLWAYPOINTDELAY";
	public static final String KML_WAYPOINT_DISTANCE = "KMLWAYPOINTDISTANCE";
	public static final String BING_KEY = "BINGKEY";
	public static final String[] PARAMETERS = {COMPUTER_PORT, UAV_PORT, PROTOCOL, SPEED, MACS, ASSIGNMENT_ALGORITHM,
			SERIAL_PORT, BAUD_RATE, BROADCAST_IP, BROADCAST_PORT, BATTERY_CELLS, BATTERY_CAPACITY,
			AIR_FORMATION, AIR_DISTANCE,LAND_DISTANCE,MEASURE_CPU, VERBOSE_LOGGING, VERBOSE_STORE, WAYPOINT_YAW_OVERRIDE, WAYPOINT_YAW_VALUE,
			KML_MIN_ALTITUDE, KML_OVERRIDE_ALTITUDE, KML_ALTITUDE, KML_MISSION_END, KML_RTL_END_ALT, KML_WAYPOINT_DELAY, KML_WAYPOINT_DISTANCE,
			BING_KEY};
	// Additional simulation parameters not included in the INI file
	public static final String GROUND_FORMATION = "GROUNDFORMATION";
	public static final String GROUND_DISTANCE = "GROUNDDISTANCE";

	// Number of UAVs to be simulated
	public static int numUAVs;
	public static AtomicInteger numUAVsTemp = new AtomicInteger();	// Used as semaphore to modify numUAVs in configuration dialogs
	
	// Unique UAV identifier that must be used on main.java.com.api.protocols, based on MAC address (simple enumeration while in simulator)
	public static long[] id;
	
	// Number of UAVs that need a main.java.com.protocols.mission to be loaded
	public static AtomicInteger numMissionUAVs = new AtomicInteger();
	
	// Verbose logging?
	public static volatile boolean verboseLogging = false;
	
	// Store additional information in files?
	public static volatile boolean storeData = true;
	
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
	public static volatile long setupTime;	// (ms) setup start in local time
	public static volatile long startTime;	// (ms) experiment start in local time
	public static long[] testEndTime;		// (ms) one UAV experiment finish in local time
	public static long latestEndTime;		// (ms) experiment finish in local time
	public static volatile long timeOffset;	// (ms) time offset between PC Companion and real multicopter clock

	// Selected wireless model
	public static WirelessModel selectedWirelessModel;
	public static double fixedRange = 800.0;				// (m) Fixed range distance threshold
	public static final double FIXED_MAX_RANGE = 1500.0;	// (m) Fixed range maximum distance accepted
	
	// CPU usage parameters
	public static boolean measureCPUEnabled = false;	// Whether the CPU utilization must be measured or not
	public static int numCPUs;							// Number of cores available
	public static ConcurrentLinkedQueue<CPUData> cpu = new ConcurrentLinkedQueue<>();	// CPU usage data
	public static final long CPU_CHECK_PERIOD = 1;		// (s) Time between measurements
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
		SimulatorState(int id) {
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
	
}
