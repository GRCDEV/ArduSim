package com.api.masterslavepattern;

/**
 * Parameters used for master-slave model tasks.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MSParam {
	
	public static final int MASTER_POSITION = 0; // Position of master UAV into array of UAVs
	public static volatile String[] macAddresses = new String[] { "b8:27:eb:57:4c:0e", "b8:27:eb:02:19:5b" };// MACs of master (Hexacopter) with standard format
	public static volatile long[] macIDs = new long[] { 202481591602190L, 202481586018651L };// MACs of master with long format
	
	public static final int RECEIVING_TIMEOUT = 200;		// (ms) Timeout trying to receive a message
	public static final int SENDING_PERIOD = 200;			// (ms) Time between sending two messages
	public static final int DISCOVERY_TIMEOUT = 2000;		// (ms) Timeout since the last received message from the master UAV for sending the discovery data
	public static final int TAKE_OFF_DATA_TIMEOUT = 2000;	// (ms) Timeout since the last received message from the master UAV for sending the safe take off data
	public static final int TAKE_OFF_TIMEOUT = 2000;		// (ms) Timeout since the last received message from the center UAV for the take off process
	
	
	// Safe take off sending data states
	public static final short STATE_EXCLUDE = 0;
	public static final short STATE_SENDING_TAKEOFFDATA = 1;
	public static final short STATE_SENDING_LIDERORDER = 2;
	public static final short STATE_SENDING_FINISHED = 3;
	
	// Safe take off taking off states
	public static final short STATE_WAIT_TAKE_OFF = 4;
	public static final short STATE_TAKE_OFF_STEP_1 = 5;
	public static final short STATE_TAKE_OFF_STEP_2 = 6;
	public static final short STATE_TARGET_REACHED = 7;
	public static final short STATE_TAKE_OFF_CHECK = 8;
	public static final short STATE_TAKE_OFF_FINISHED = 9;

}
