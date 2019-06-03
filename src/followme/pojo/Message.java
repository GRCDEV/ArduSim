package followme.pojo;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public final class Message {
	
	public static final short HELLO = 0;				// "I am here"
	public static final short DATA = 1;					// "Sending previous and next UAVs, take off altitude, mission, ..."
	public static final short DATA_ACK = 2;				// "Data received"
	public static final short READY_TO_FLY = 3;			// "All UAVs have data"
	public static final short READY_TO_FLY_ACK = 4;		// "Yes, I know that all of us have data"
	public static final short TAKE_OFF_NOW = 5;			// "Take off now"
	public static final short TARGET_REACHED_ACK = 6;	// "I reached the starting location"
	public static final short TAKEOFF_END = 7;			// "All UAVs completed the takeoff step"
	public static final short TAKEOFF_END_ACK = 8;		// "Yes, I know that all of us finished the takeoff step"
	public static final short I_AM_HERE = 9;			// "I am here" from master UAV
	public static final short LAND = 10;				// "Land now"
}
