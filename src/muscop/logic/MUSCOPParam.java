package muscop.logic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import es.upv.grc.mapper.Location3DUTM;

/** 
 * Parameters needed by the protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPParam {
	
	/** (rad) Master UAV yaw or heading (first mission segment orientation in simulation). */
	public static volatile double formationYaw;
	
	/** Mission sent from master to slaves (center mission). */
	public static AtomicReference<Location3DUTM[]> missionSent = new AtomicReference<>();// Array containing the mission sent to the slaves
	
	/** Maximum number of waypoints that fit in a datagram. */
	public static final int MAX_WAYPOINTS = 61;	// main.api.CommLink.DATAGRAM_MAX_LENGTH - 2 - 2 - 3x8xn >= 0
	
	/** Current protocol state for each UAV. */
	public static AtomicInteger[] state;

	// Timeouts
	public static final int RECEIVING_TIMEOUT = 50;			// (ms) The port is unlocked after this time when receiving messages
	public static final long SENDING_TIMEOUT = 200;			// (ms) Time between packets sent
	public static final long LAND_CHECK_TIMEOUT = 250;		// (ms) Between checks if the UAV has landed
	public static final long STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
	public static final long MISSION_TIMEOUT = 2000;		// (ms) Timeout to wait after the mission is received and the master UAV changes its state
	public static final long TTL = 5000;					// (ms) Time to life for a UAV.
	public static final int RECEIVETIMEOUT = 200;			// (ms) Timeout for link.receiveMessage()
}
