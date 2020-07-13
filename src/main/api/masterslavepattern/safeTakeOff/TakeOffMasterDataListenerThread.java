package main.api.masterslavepattern.safeTakeOff;

import api.API;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.ArduSimTools;
import main.Text;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;
import main.uavController.UAVParam;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread used by the master UAV to coordinate the safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOffMasterDataListenerThread extends Thread {
	
	public static volatile TakeOffAlgorithm selectedAlgorithm = TakeOffAlgorithm.OPTIMAL;
	
	private int numUAV;
	private long selfID;
	private Map<Long, Location2DUTM> groundLocations;
	private int numSlaves;
	private int numUAVs;
	private FlightFormation flightFormation;
	private double formationYaw;
	private double targetAltitude;
	private boolean isCenterUAV;
	private boolean exclude;
	private AtomicReference<SafeTakeOffContext> result;
	private InternalCommLink commLink;
	private byte[] inBuffer;
	private Input input;
	private GUI gui;
	private Copter copter;
	
	@SuppressWarnings("unused")
	private TakeOffMasterDataListenerThread() {}
	
	public TakeOffMasterDataListenerThread(int numUAV, Map<Long, Location2DUTM> groundLocations,
			FlightFormation flightFormation, double formationYaw, double targetAltitude, boolean isCenterUAV, boolean exclude,
			AtomicReference<SafeTakeOffContext> result) {
		super(Text.SAFE_TAKE_OFF_MASTER_CONTEXT_LISTENER + numUAV);
		this.numUAV = numUAV;
		this.copter = API.getCopter(numUAV);
		this.selfID = this.copter.getID();
		this.groundLocations = groundLocations;
		this.numSlaves = groundLocations.size();
		this.numUAVs = this.numSlaves + 1;
		this.flightFormation = flightFormation;
		this.formationYaw = formationYaw;
		this.targetAltitude = targetAltitude;
		this.isCenterUAV = isCenterUAV;
		this.exclude = exclude;
		this.result = result;
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.gui = API.getGUI(numUAV);
	}
	

	@Override
	public void run() {
		
		ArduSimTools.logGlobal(Text.TAKEOFF_ALGORITHM_IN_USE + " " + TakeOffMasterDataListenerThread.selectedAlgorithm.getName());
		
		AtomicInteger state = new AtomicInteger(MSParam.STATE_EXCLUDE);
		
		gui.logVerboseUAV(MSText.MASTER_LISTENER_WAITING_EXCLUDES);
		Map<Long, Boolean> excluded = new HashMap<>((int)Math.ceil(numSlaves / 0.75) + 1);
		while (state.get() == MSParam.STATE_EXCLUDE) {
			inBuffer = commLink.receiveMessage();
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.EXCLUDE) {
					long idSlave = input.readLong();
					boolean exclude = input.readBoolean();
					excluded.put(idSlave, exclude);
					if (excluded.size() == numSlaves) {
						state.set(MSParam.STATE_SENDING_DATA);
					}
				}
			}
		}
		
		gui.logVerboseUAV(MSText.MASTER_LISTENER_CALCULATING_TAKE_OFF);
		
		
		
		// 1. Take off altitude step 1
		double takeOffAltitudeStep1;
		if (this.targetAltitude <= 5.0) {
			takeOffAltitudeStep1 = 2.0;
		} else if (this.targetAltitude >= 10.0) {
			takeOffAltitudeStep1 = 5.0;
		} else {
			takeOffAltitudeStep1 = this.targetAltitude / 2;
		}
		
		// 2. Get the best match between current ground locations and flight locations
		// First, we add the master UAV
		groundLocations.put(selfID, copter.getLocationUTM());
		Long masterID = null;
		if (isCenterUAV) {
			masterID = selfID;
		}
		// Then, we get the match between ground and air locations
		MatchCalculusThread calculus = new MatchCalculusThread(groundLocations, flightFormation, formationYaw, masterID);
		calculus.start();
		// It waits the end of the calculus discarding received messages from slaves
		while (calculus.isAlive()) {
			commLink.receiveMessage(50);
		}
		Quartet<Integer, Long, Location2DUTM, Double>[] match = calculus.getResult();
		
		// 3. Get the target location and ID of the UAV that should be in the center of the formation
		int centerUAVAirPos = flightFormation.getCenterUAVPosition();
		Quartet<Integer, Long, Location2DUTM, Double> centerMatch = null;
		for (int i = 0; i < numUAVs && centerMatch == null; i++) {
			if (match[i].getValue0() == centerUAVAirPos) {
				centerMatch = match[i];
			}
		}
		if (centerMatch == null) {
			gui.exit(MSText.CENTER_ID_NOT_FOUND);
		}
		long centerUAVID = centerMatch.getValue1();
		Location2DUTM centerUAVLocationUTM = centerMatch.getValue2();
		
		// 4. Get the master assignment order from the match (fault tolerance)
		long[] ids = new long[numUAVs];
		for (int i = 0, j = numUAVs-1; i < numUAVs; i++, j--) {
			ids[i] = match[j].getValue1();
		}
		
		// 5. Build the messages to send with the Talker Thread
		byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Output output = new Output(outBuffer);
		Map<Long, byte[]> messages = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		long curID, prevID, nextID;
		// i represents the position in the takeoff sequence
		int formationPos;
		long pID;
		long nID;
		Location2DGeo targetLocation = null;
		SafeTakeOffContext data = null;
		for (int i = 0; i < numUAVs; i++) {
			curID = match[i].getValue1();
			if (i == 0) {
				prevID = SafeTakeOffContext.BROADCAST_MAC_ID;
			} else {
				prevID = match[i - 1].getValue1();
			}
			if (i == numUAVs - 1) {
				nextID = SafeTakeOffContext.BROADCAST_MAC_ID;
			} else {
				nextID = match[i + 1].getValue1();
			}
			
			// Master UAV
			formationPos = match[i].getValue0();
			if (curID == selfID) {
				pID = prevID;
				nID = nextID;
				try {
					targetLocation = flightFormation.getLocation(formationPos, centerUAVLocationUTM, formationYaw).getGeo();
				} catch (LocationNotReadyException e) {
					e.printStackTrace();
					gui.exit(e.getMessage());
				}
				FlightFormation landFormation = FlightFormation.getFormation(Formation.getFormation(flightFormation.getFormationId()),
						numUAVs, UAVParam.landDistanceBetweenUAV);
				data = new SafeTakeOffContext(pID, nID, targetLocation, takeOffAltitudeStep1, targetAltitude, exclude, ids, centerUAVLocationUTM,
						numUAVs, flightFormation, landFormation, formationPos, formationYaw);
			} else {
				output.reset();
				output.writeShort(MSMessageID.TAKE_OFF_DATA);
				output.writeLong(curID);
				output.writeLong(centerUAVID);
				output.writeDouble(centerUAVLocationUTM.x);
				output.writeDouble(centerUAVLocationUTM.y);
				output.writeLong(prevID);
				output.writeLong(nextID);
				output.writeInt(numUAVs);
				output.writeShort(flightFormation.getFormationId());
				output.writeDouble(flightFormation.getFormationMinimumDistance());
				output.writeDouble(UAVParam.landDistanceBetweenUAV);
				output.writeInt(formationPos);
				output.writeDouble(formationYaw);
				output.writeDouble(takeOffAltitudeStep1);
				output.writeDouble(targetAltitude);
				output.flush();
				messages.put(curID, Arrays.copyOf(outBuffer, output.position()));
			}
		}
		if (targetLocation == null) {
			gui.exit(MSText.MASTER_ID_NOT_FOUND);
		}
		
		int max = CommLink.DATAGRAM_MAX_LENGTH;
		boolean fragment = numUAVs > Math.floorDiv(max - 2, 8);
		
		byte[][] ms;
		if (fragment) {
			int maxSize = Math.floorDiv(max - 2 - 2, 8);	//183 UAVs
			int frags = (int) Math.ceil(numUAVs / (maxSize * 1.0));
			ms = messages.values().toArray(new byte[messages.size() + frags][]);
			
			int frag = 0;
			int idsPos = 0;
			for (int i = messages.size();i < messages.size() + frags; i++) {
				int subSize = Math.min(maxSize, numUAVs - frag * maxSize);
				output.reset();
				output.writeShort(MSMessageID.LIDER_ORDER_FRAG);
				output.writeShort(frag);
				while(idsPos < frag * maxSize + subSize) {
					output.writeLong(ids[idsPos]);
					idsPos++;
				}
				output.flush();
				ms[i] = Arrays.copyOf(outBuffer, output.position());
				frag++;
			}
		} else {
			ms = messages.values().toArray(new byte[messages.size() + 1][]);
			output.reset();
			output.writeShort(MSMessageID.LIDER_ORDER);
			for (long id : ids) {
				output.writeLong(id);
			}
			output.flush();
			ms[ms.length - 1] = Arrays.copyOf(outBuffer, output.position());
		}
		output.close();
		
		// 6. Get the data to be returned by this Thread and start the thread talker
		this.result.set(data);
		new TakeOffMasterDataTalkerThread(numUAV, ms, state).start();
		
		// 7. Wait slaves for ack
		gui.logVerboseUAV(MSText.MASTER_LISTENER_WAITING_DATA_ACK);
		excluded.clear();
		while (state.get() == MSParam.STATE_SENDING_DATA) {
			inBuffer = commLink.receiveMessage();
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.TAKE_OFF_DATA_ACK) {
					long idSlave = input.readLong();
					excluded.put(idSlave, false);	// the value has no sense in this case
					if (excluded.size() == numSlaves) {
						state.set(MSParam.STATE_SENDING_FINISHED);
					}
				}
			}
		}
		
		gui.logVerboseUAV(MSText.MASTER_LISTENER_TAKE_OFF_DATA_END);
	}
	
	/**
	 * This is used in the master UAV only. Get which will be the center UAV in the flight formation.
	 * @param groundLocations Location and ID of each of all the UAVs, including the UAV that could be in the center of the flight formation.
	 * @param formationYaw (rad) Yaw that will be set for the flight formation.
	 * @param isCenterUAV Whether the master UAV will be in the center of the flight formation or not. In the first case, the flight formation will be built around the current location of the master UAV.
	 * @return ID and air location of the UAV that will be in the center of the flight formation.
	 */
	public static Pair<Long, Location2DUTM> getCenterUAV(Map<Long, Location2DUTM> groundLocations,
			double formationYaw, boolean isCenterUAV) {
		
		// 1. Get the best match between current ground locations and flight locations
		Long masterID = null;
		if (isCenterUAV) {
			masterID = API.getCopter(0).getID();
		}
		int numUAVs = groundLocations.size();
		FlightFormation flightFormation = API.getFlightFormationTools().getFlyingFormation(numUAVs);
		MatchCalculusThread calculus = new MatchCalculusThread(groundLocations, flightFormation, formationYaw, masterID);
		calculus.start();
		try {
			calculus.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Quartet<Integer, Long, Location2DUTM, Double>[] match = calculus.getResult();
		
		// 2. Get the target location of the UAV that should be in the center of the formation
		int centerUAVAirPos = flightFormation.getCenterUAVPosition();
		Quartet<Integer, Long, Location2DUTM, Double> centerMatch = null;
		for (int i = 0; i < numUAVs && centerMatch == null; i++) {
			if (match[i].getValue0() == centerUAVAirPos) {
				centerMatch = match[i];
			}
		}
		if (centerMatch == null) {
			API.getGUI(0).exit(MSText.CENTER_ID_NOT_FOUND);
		}
		
		return Pair.with(centerMatch.getValue1(), centerMatch.getValue2());
	}
	
}