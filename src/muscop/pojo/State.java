package muscop.pojo;

/** This class represents the states of the finite state machine used in the protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public final class State {
	public static final short START = 0;
	public static final short SETUP = 1;
	public static final short SEND_MISSION = 2;
	public static final short TAKING_OFF = 3;
	public static final short SETUP_FINISHED = 4;
	public static final short FOLLOWING_MISSION = 5;
	public static final short MOVE_TO_LAND = 6;
	public static final short LANDING = 7;
	public static final short FINISH = 8;
}
