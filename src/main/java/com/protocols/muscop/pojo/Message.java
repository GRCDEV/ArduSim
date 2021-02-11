package com.protocols.muscop.pojo;

/** 
 * List of messages sent.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public final class Message {
	
	public static final short DATA = 0;					// "Sending main.java.com.protocols.mission"
	public static final short DATA_ACK = 1;				// "Mission received"
	public static final short WAYPOINT_REACHED_ACK = 2;	// "Reached a waypoint"
	public static final short MOVE_TO_WAYPOINT = 3;		// "Move to a waypoint now"
	public static final short LAND = 4;					// "Land now"
}
