package main.api.masterslavepattern;

/**
 * Identifiers for messages internally used to coordinate UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MSMessageID {
	
	public static final short DISCOVERING = 0;
	public static final short HELLO = 1;
	
	public static final short EXCLUDE = 2;
	public static final short TAKE_OFF_DATA = 3;
	public static final short TAKE_OFF_DATA_ACK = 4;
	
	public static final short TAKE_OFF_NOW = 5;
	public static final short REACHED_ACK = 6;
	public static final short TAKE_OFF_END = 7;
	public static final short TAKE_OFF_END_ACK = 8;
	
}
