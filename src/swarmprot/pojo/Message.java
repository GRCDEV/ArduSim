package swarmprot.pojo;

public final class Message {
	
	public static final short HELLO = 0;			// "I am here"
	public static final short DATA = 1;				// "Sending previous and next UAVs, take off altitude and mission"
	public static final short DATA_ACK = 2;			// "Data received"
	public static final short TAKE_OFF_NOW = 3;		// "Take off now"
	public static final short MOVE_NOW = 4;			// "Move to a waypoint now"
	public static final short WP_REACHED_ACK = 5;	// "Reached a waypoint"
	public static final short LAND_NOW = 6;			// "Land now"
}
