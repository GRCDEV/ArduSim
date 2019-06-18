package main.api.masterslavepattern;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class MSText {
	
	public static final String MASTER_LISTENER_WAITING_EXCLUDES = "Master listener waiting slaves to inform if they take part in the take off process.";
	public static final String MASTER_LISTENER_CALCULATING_TAKE_OFF = "Master listener calculating the take off sequence.";
	public static final String MASTER_LISTENER_WAITING_DATA_ACK = "Master listener waiting slaves ack after receiving the take off data.";
	public static final String MASTER_LISTENER_TAKE_OFF_DATA_END = "Master listener finishes with the take off data.";
	
	public static final String MASTER_TALKER_SENDING_TAKE_OFF_DATA = "Master talker sending take off data to slaves.";
	public static final String MASTER_TALKER_TAKE_OFF_DATA_END = "Master talker finishes with the take off data.";
	
	public static final String SLAVE_LISTENER_WAITING_DATA = "Slave listener waiting master to send take off data.";
	public static final String SLAVE_LISTENER_WAITING_DATA_TIMEOUT = "Slave listener waiting timeout for the take off data.";
	public static final String SLAVE_LISTENER_TAKE_OFF_DATA_END = "Slave listener finishes with the take off data.";
	
	public static final String SLAVE_TALKER_SENDING_EXCLUDE = "Slave talker informing the master if it will take off or not.";
	public static final String SLAVE_TALKER_SENDING_TAKE_OFF_DATA_ACK = "Slave talker sending take off data ack.";
	public static final String SLAVE_TALKER_TAKE_OFF_DATA_END = "Slave talker finishes with the take off data.";
	
	public static final String CENTER_ID_NOT_FOUND = "Center UAV not found in the target flight formation.";
	public static final String MASTER_ID_NOT_FOUND = "Master UAV not found in the take off sequence.";
	
	public static final String LISTENER_WAITING_TAKE_OFF = "Listener waiting take off command.";
	public static final String LISTENER_TAKING_OFF_1 = "Listener taking off until initial altitude.";
	public static final String TAKE_OFF_ERROR = "Unable to perform the take off of the UAV ";
	public static final String LISTENER_TAKING_OFF_2 = "Listener taking off until the target location.";
	public static final String MOVE_ERROR = "Unable to move to target location. UAV ";
	public static final String LISTENER_WAITING_ON_GROUND = "Listener no take off performed.";
	public static final String LISTENER_TARGET_REACHED = "Listener reached target location.";
	public static final String LISTENER_CENTER_WAITING_SLAVES = "Center UAV listener waiting slaves to reach target location.";
	public static final String LISTENER_NO_CENTER_WAITING_END = "No center UAV listener waiting command to finish take off.";
	public static final String LISTENER_SYNCHRONIZING = "Listener waiting to synchronize the end of the take off process.";
	public static final String CENTER_TAKEOFF_END_ACK_LISTENER = "Center UAV listener waiting slaves to confirm they finished take off.";
	public static final String NO_CENTER_WAIT_TAKEOFF_END_LISTENER = "No center UAV listener waiting timeout to end setup.";
	
	public static final String TALKER_WAITING = "Talker waiting for take off turn.";
	public static final String TALKER_TAKING_OFF_1 = "Talker waiting while taking off until initial altitude.";
	public static final String TALKER_WAITING_TAKING_OFF_2 = "Talker waiting while taking off until the target location.";
	public static final String TALKER_TAKING_OFF_2 = "Talker sending take off command while taking off until the target location.";
	public static final String TALKER_TARGET_REACHED = "Talker sending target reached ack.";
	public static final String TALKER_WAITING_ALL_REACHED = "Center talker waiting no center UAVs to finish the take off phase 2.";
	public static final String TALKER_CENTER_WAITING_END = "Center UAV talker waiting to finish.";
	public static final String TALKER_NO_CENTER_WAITING_END = "No center UAV talker waiting timeout to finish.";
	
}
