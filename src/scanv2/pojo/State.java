package scanv2.pojo;

/** This class represents the states of the finite state machine used in the protocol. */

public final class State {
	public static final short START = 0;
	public static final short SETUP = 1;
	public static final short READY_TO_FLY = 2;
	public static final short WAIT_TAKE_OFF = 3;
	public static final short TAKING_OFF = 4;
	public static final short MOVE_TO_TARGET = 5;
	public static final short TARGET_REACHED = 6;
	public static final short READY_TO_START = 7;
	public static final short SETUP_FINISHED = 8;
	public static final short FOLLOWING_MISSION = 9;
	public static final short LANDING = 10;
	public static final short FINISH = 11;
}
