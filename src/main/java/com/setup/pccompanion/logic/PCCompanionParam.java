package com.setup.pccompanion.logic;

import com.api.pojo.StatusPacket;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** 
 * Parameters used in the PC Companion.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class PCCompanionParam {

	// Remote assistant computer parameters
	public static volatile int computerPort = 5750;				// Destination port on the assistant computer
	public static volatile int uavPort = 5755;					// Destination port for commands on the UAV
	public static final long STATUS_SEND_TIMEOUT = 1000;		// (ms) Period between sent program status messages
	public static final long COMMAND_SEND_TIMEOUT = 250;		// (ms) Period between sent commands
	public static final int RECEIVE_TIMEOUT = 100;				// (ms) Period between receiving checks
	public static AtomicLong lastStartCommandTime = null;		// (ms) Time of the last Start command received
	public static final long STATUS_CHANGE_CHECK_TIMEOUT = 500;	// (ms) Between checks about the status of the UAVs connected
	public static final long MAX_TIME_SINCE_LAST_START_COMMAND = 5 * COMMAND_SEND_TIMEOUT;// (ms) Time to detect that all UAVs have started the experiment
	// Action to take selected by the user
	public static final int ACTION_NONE = 1;
	public static final int ACTION_RECOVER_CONTROL = 2;
	public static final int ACTION_RTL = 3;
	public static final int ACTION_LAND = 4;
	public static AtomicInteger action = new AtomicInteger(PCCompanionParam.ACTION_NONE);
	// Commands sent by the user
	public static final int CHANGE_STATE_COMMAND = 1;
	public static final int EMERGENCY_COMMAND = 2;
	
	// List of UAVs detected when the Setup button is pressed
	public static AtomicReference<StatusPacket[]> connectedUAVs = new AtomicReference<>();
}
