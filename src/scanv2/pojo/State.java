package scanv2.pojo;

/** This class represents the states of the finite state machine used in the protocol. */

public final class State {
	public static final short START = 0;
	public static final short SETUP = 1;
	public static final short SETUP_FINISHED = 2;
	public static final short WAIT_TAKE_OFF = 3;
	public static final short TAKING_OFF = 4;
	public static final short FOLLOWING_MISSION = 5;
	public static final short LANDING = 6;
	public static final short FINISH = 7;
}
