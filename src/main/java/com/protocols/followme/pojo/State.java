package com.protocols.followme.pojo;

/** This class represents the states of the finite state machine used in the protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public final class State {
	public static final short START = 0;
	public static final short TAKE_OFF = 1;
	public static final short SETUP_FINISHED = 2;
	public static final short FOLLOWING = 3;
	public static final short MOVE_TO_LAND = 4;
	public static final short LANDING = 5;
	public static final short FINISH = 6;
}
