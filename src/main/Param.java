package main;

import java.util.concurrent.atomic.AtomicInteger;

import uavController.UAVControllerThread;

/** This class contains general parameters of the application. */

public class Param {
	
	// The experiment is done in simulator or on a real UAV?
	public static volatile boolean isRealUAV;
	
	// The application is being run as a PC companion
	public static volatile boolean isPCCompanion;

	// Number of UAVs to be simulated
	public static int numUAVs;
	public static AtomicInteger numUAVsTemp = new AtomicInteger();	// Used as semaphore to modify numUAVs in configuration dialogs
	
	// Unique UAV identifier that must be used on protocols, based on MAC address (simple enumeration while in simulator)
	public static long[] id;
	
	// Number of UAVs that need a mission to be loaded
	public static AtomicInteger numMissionUAVs = new AtomicInteger();
	
	// Verbose logging?
	public static final boolean VERBOSE_LOGGING = false;
	
	// Store additional information in files?
	public static final boolean VERBOSE_STORE = true;
	
	// Array containing the UAV controllers. Useful to set a listener to detect when a waypoint is reached, if needed.
	public static UAVControllerThread[] controllers;

	// Running Operating System
	public static final int OS_WINDOWS = 0;
	public static final int OS_LINUX = 1;
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
	public static final long STARTING_TIMEOUT = 10000;	// (ms) Time to wait before checking if all UAVs are on the ground 

	// Selected wireless model
	public static WirelessModel selectedWirelessModel;
	public static double fixedRange = 800.0;				// (m) Fixed range distance threshold
	public static final double FIXED_MAX_RANGE = 1500.0;	// (m) Fixed range maximum distance accepted
	
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
