package pccompanion;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import main.Param.Protocol;
import main.Param.SimulatorState;

public class PCCompanionParam {

	// Remote assistant computer parameters
	public static final int COMPUTER_PORT = 5750;		// Destination port on the assistant computer
	public static final int UAV_PORT = 5755;			// Destination port for commands on the UAV
	public static final int STATUS_SEND_TIMEOUT = 1000;	// (ms) Period between sent program status messages
	public static final int COMMAND_SEND_TIMEOUT = 200;	// (ms) Period between sent commands
	public static final int RECEIVE_TIMEOUT = 100;		// (ms) Period between receiving checks
	public static AtomicLong lastStartCommandTime = null;		// (ms) Time of the last Start command received
	public static final int STATUS_CHANGE_CHECK_TIMEOUT = 200;	// (ms) Between checks about the status of the UAVs connected
	public static final int MAX_TIME_SINCE_LAST_START_COMMAND = 5 * COMMAND_SEND_TIMEOUT;// (ms) Time to detect that all UAVs have started the experiment
	public static final AtomicReference<Protocol> SELECTED_PROTOCOL = new AtomicReference<>();
}
