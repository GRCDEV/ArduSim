package scanv1.logic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

import api.pojo.GeoCoordinates;
import api.pojo.Point3D;

public class Scanv1ProtParam {
	public static final int posMaster = 0; // Position of master UAV into array of UAVs
	public static long idMaster; // Id real
	public static final String[] MAC = new String[] { "b8:27:eb:74:0c:d1", "00:c0:ca:90:32:05" };// MACs of master with standard format
	public static final long[] MACId = new long[] { 202481593486545L, 202481593486545L };// MACs of master with long format
	public static final long idMasterSimulation = 0;
	public static final int ALTITUDE_WAIT = 500; // Time between checks while take off (ms)
	public static double[] initial_speeds; // (m/s) Initial UAVs speed
	public static double masterHeading; // Master UAV Heading
	// Ground
	public static int initialDistanceBetweenUAV = 1; // Initial distance between uav's on simulation
	// Air
	public static int initialDistanceBetweenUAVreal = 5; // Initial distance between uav's on real flight

	// TCP parameters
	public static final int DGRAM_MAX_LENGTH = 1472; // (B) 1500-20-8 (MTU - IP - UDP)

	public static final String BROADCAST_IP_LOCAL = "127.0.0.1"; // Broadcast IP on simulator

	public static final String BROADCAST_IP_REAL = "192.168.1.255"; // Broadcast IP on real life
	public static int port = 14600; // Simulated broadcast port
	public static int portTalker = 15100; // Simulated broadcast port
	public static final int recTimeOut = 500; // The port is unlocked after this time (ms)
	
	// Assert if the setup step has finished (0=no, 1=yes)
	public static AtomicIntegerArray setupFinished;

	// Protocol state included
	public static SwarmProtState[] state;

	// Waiting time in listening or reading threads
	public static final int waitState = 250;
	
	// Wait between ACK
	public static int swarmStateWait = 200; // (ms) Time between sends
	 
	// Broadcast MAC comes from FFFFFFFFFFFF MAC address
	public static final long broadcastMAC = 281474976710655L;
	
	// Maximum number of waypoints
	public static final int maxWaypoints = 60;
	
	// Maximum number of waypoints reached
	public static final String maxWpMes = "Maximum number of waypoints reached, please use less than 60";
	
	// Matrix with individual missions of each Drone UTM
	public static Point3D[][] flightListPersonalized;
	public static Double[][] flightListPersonalizedAlt;
	
	// Matrix with individual missions of each Drone GEO
	public static GeoCoordinates[][] flightListPersonalizedGeo;
	public static Double[][] flightListPersonalizedAltGeo;
	
	// Matrix with UAV number and its idPrev and idNext in that order
	public static long[][] fightPrevNext;
	
	// Matrix with detection of the last point of the Mission
	public static boolean[][] WpLast;
	
	// Distance between WP destination in which it is accepted as having arrived at said point (meters)
	public static final double acceptableRadiusDistanceWP = 2.0;
	
	// Distance from the ground to conclude that the experiment has finished
	public static final int landingAtiFinal = 1;
	
	// Maximum speed at which you can go to determine if is landed (m / s)
	public static final double speedLanding = 0.2;
	
	// Time the master waits for the slaves in the START phase
	public static final int waitTimeForSlaves = 3000;
	
	// Heading of the mission
	public static double missionHeading = 0;
	
	// Distance from which it is accepted that you have reached the WP
	public static double distToAcceptPointReached = 1.0;
	
	// Timeout for sending methods (ms), like TCP/IP
	public static final int timeoutSendingShort = 2000;


	// SwarmProt finite state machine states enumerator
	public enum SwarmProtState {
		START((short) 1, Scanv1ProtText.START), 
		SEND_DATA((short) 2, Scanv1ProtText.SEND_DATA), 
		WAIT_LIST((short) 3, Scanv1ProtText.WAIT_LIST), 
		SEND_LIST((short) 4, Scanv1ProtText.SEND_LIST), 
		WAIT_TAKE_OFF((short) 5, Scanv1ProtText.WAIT_TAKE_OFF),
		SEND_TAKE_OFF((short) 6, Scanv1ProtText.SEND_TAKE_OFF),
		TAKING_OFF((short) 7, Scanv1ProtText.TAKING_OFF), 
		MOVE_TO_WP((short) 8, Scanv1ProtText.MOVE_TO_WP), 
		WP_REACHED((short) 9, Scanv1ProtText.WP_REACHED), 
		LANDING((short) 10, Scanv1ProtText.LANDING), 
		FINISH((short) 11, Scanv1ProtText.FINISH),;

		private final short id;
		private final String name;

		private SwarmProtState(short id, String name) {
			this.id = id;
			this.name = name;
		}

		public short getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public static SwarmProtState getSatateById(short id) {
			SwarmProtState res = null;
			for (SwarmProtState p : SwarmProtState.values()) {
				if (p.getId() == id) {
					res = p;
					break;
				}
			}
			return res;
		}
	}

}
