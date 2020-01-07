package main.api.masterslavepattern.safeTakeOff;

import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import main.api.formations.FlightFormation;

/**
 * Object that contains all the information needed to perform a safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SafeTakeOffContext {
	
	/** Identifier used for <i>null</i> UAV. In a real multicoter it is equivalent to the broadcast MAC: 0xFFFFFFFFFFFF. */
	public static final long BROADCAST_MAC_ID = 281474976710655L;
	
	protected long prevID, nextID;
	protected Location2DGeo targetLocation;
	protected double altitudeStep1, altitudeStep2;
	protected boolean excluded;
	protected long[] masterOrder;
	
	private Location2DUTM centerLocation;
	protected int numUAVs;
	private FlightFormation flightFormation, landFormation;
	private int formationPosition;
	private double initialYaw;
	
	@SuppressWarnings("unused")
	private SafeTakeOffContext() {}
	
	public SafeTakeOffContext(long prevID, long nextID, Location2DGeo targetLocation, double altitudeStep1, double altitudeStep2,
			boolean excluded, long[] masterOrder, Location2DUTM centerLocation, int numUAVs,
			FlightFormation flightFormation, FlightFormation landFormation, int formationPosition, double initialYaw) {
		this.prevID = prevID;
		this.nextID = nextID;
		this.targetLocation = targetLocation;
		this.altitudeStep1 = altitudeStep1;
		this.altitudeStep2 = altitudeStep2;
		this.excluded = excluded;
		this.masterOrder = masterOrder;
		this.centerLocation = centerLocation;
		this.numUAVs = numUAVs;
		this.flightFormation = flightFormation;
		this.landFormation = landFormation;
		this.formationPosition = formationPosition;
		this.initialYaw = initialYaw;
	}

	/**
	 * Get the initial location of the center UAV in the flight formation.
	 * @return Initial location of the center UAV in the flight formation.
	 */
	public Location2DUTM getCenterUAVLocation() {
		return this.centerLocation;
	}

	/**
	 * Get the formation to be used during flight.
	 * @return Flight formation to be used during flight.
	 */
	public FlightFormation getFormationFlying() {
		return flightFormation;
	}

	/**
	 * Get the formation to be used during landing. It is the same Formation used during flight, but the UAVs get closer to the center UAV in order to land in a reduced area.
	 * @return Flight formation to be used during landing.
	 */
	public FlightFormation getFormationLanding() {
		return landFormation;
	}
	
	/**
	 * Get the position of this UAV in the flight formation.
	 * @return Position of this UAV in the flight formation.
	 */
	public int getFormationPosition() {
		return formationPosition;
	}
	
	/**
	 * Get the yaw of the flight formation when it is built.
	 * @return (rad) Yaw of the flight formation.
	 */
	public double getInitialYaw() {
		return initialYaw;
	}
	
	/**
	 * Get the ID of the next UAV in the take off sequence.
	 * @return ID of the next UAV in the take off sequence, or <i>SafeTakeOffContext.BROADCAST_MAC_ID</i> if this is the last UAV in the take off sequence.
	 */
	public long getNextID() {
		return this.nextID;
	}
	
	/**
	 * Get the number of UAVs coordinated for the take off process, even when some of them could remain on the ground after the process.
	 * @return Number of UAVs coordinated for the take off process.
	 */
	public int getNumUAVs() {
		return numUAVs;
	}
	
	/**
	 * Get the ID of the previous UAV in the take off sequence.
	 * @return ID of the previous UAV in the take off sequence, or <i>SafeTakeOffContext.BROADCAST_MAC_ID</i> if this is the first UAV in the take off sequence.
	 */
	public long getPrevID() {
		return this.prevID;
	}
	
	/**
	 * Get the ID of the UAVs in the optimal order to assign any of the as master for master-slave pattern protocols.
	 * #return list of IDs of the UAVs.
	 */
	public long[] getMasterOrder() {
		return this.masterOrder;
	}
	
}
