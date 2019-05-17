package followme.pojo;

/** This class represents the states of the finite state machine used in the protocol.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public final class State {
	public static final short START = 0;
	public static final short SETUP = 1;
	public static final short READY_TO_FLY = 2;
	public static final short WAIT_TAKE_OFF = 3;
	public static final short TAKING_OFF = 4;
	public static final short MOVE_TO_TARGET = 5;
	public static final short TARGET_REACHED = 6;
	public static final short WAIT_SLAVES = 7;
	public static final short READY_TO_START = 8;
	public static final short SETUP_FINISHED = 9;
	public static final short FOLLOWING = 10;
	public static final short MOVE_TO_LAND = 11;
	public static final short LANDING = 12;
	public static final short FINISH = 13;
}
