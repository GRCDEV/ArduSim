package main.api.masterslavepattern.safeTakeOff;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2DUTM;
import main.Text;
import main.api.ArduSimNotReadyException;
import main.api.Copter;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;
import main.api.formations.helpers.Permutation;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;
import main.uavController.UAVParam;

/**
 * Thread used by the master UAV to coordinate the safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOffMasterDataListenerThread extends Thread {
	
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
		
		AtomicInteger state = new AtomicInteger(MSParam.STATE_EXCLUDE);
		
		gui.logVerboseUAV(MSText.MASTER_LISTENER_WAITING_EXCLUDES);
		Map<Long, Boolean> excluded = new HashMap<Long, Boolean>((int)Math.ceil(numSlaves / 0.75) + 1);
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
		if (targetAltitude <= 5.0) {
			takeOffAltitudeStep1 = 2.0;
		} else if (targetAltitude >= 10.0) {
			takeOffAltitudeStep1 = 5.0;
		} else {
			takeOffAltitudeStep1 = targetAltitude / 2;
		}
		
		// 2. Get the best match between current ground locations and flight locations
		// First, we add the master UAV
		groundLocations.put(selfID, copter.getLocationUTM());
		Long masterID = null;
		if (isCenterUAV) {
			masterID = selfID;
		}
		Triplet<Integer, Long, Location2DUTM>[] match = TakeOffMasterDataListenerThread.safeTakeOffGetMatch(groundLocations, flightFormation, formationYaw, masterID);
		
		// 3. Get the target location of the UAV that should be in the center of the formation
		int centerUAVAirPos = flightFormation.getCenterUAVPosition();
		Triplet<Integer, Long, Location2DUTM> centerMatch = null;
		for (int i = 0; i < numUAVs; i++) {
			if (match[i].getValue0() == centerUAVAirPos) {
				centerMatch = match[i];
			}
		}
		if (centerMatch == null) {
			gui.exit(MSText.CENTER_ID_NOT_FOUND);
		}
		long centerUAVID = centerMatch.getValue1();
		Location2DUTM centerUAVLocationUTM = centerMatch.getValue2();
		
		// 4. Build the messages to send with the Talker Thread
		byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Output output = new Output(outBuffer);
		Pair<Integer, Long>[] sequence = TakeOffMasterDataListenerThread.getTakeoffSequence(groundLocations, match);
		Map<Long, byte[]> messages = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		long curID, prevID, nextID;
		// i represents the position in the takeoff sequence
		int formationPos = 0;
		boolean isReallyTheCenterUAV = false;
		long pID = 0;
		long nID = 0;
		Location2DGeo targetLocation = null;
		SafeTakeOffContext data = null;
		for (int i = 0; i < numUAVs; i++) {
			curID = sequence[i].getValue1();
			if (i == 0) {
				prevID = SafeTakeOffContext.BROADCAST_MAC_ID;
			} else {
				prevID = sequence[i - 1].getValue1();
			}
			if (i == numUAVs - 1) {
				nextID = SafeTakeOffContext.BROADCAST_MAC_ID;
			} else {
				nextID = sequence[i + 1].getValue1();
			}
			
			// Master UAV
			formationPos = sequence[i].getValue0();
			if (curID == selfID) {
				pID = prevID;
				nID = nextID;
				try {
					targetLocation = flightFormation.getLocation(formationPos, centerUAVLocationUTM, formationYaw).getGeo();
				} catch (ArduSimNotReadyException e) {
					e.printStackTrace();
					gui.exit(e.getMessage());
				}
				isReallyTheCenterUAV = selfID == centerUAVID;
				FlightFormation landFormation = FlightFormation.getFormation(Formation.getFormation(flightFormation.getFormationId()),
						numUAVs, UAVParam.landDistanceBetweenUAV);
				data = new SafeTakeOffContext(pID, nID, targetLocation, takeOffAltitudeStep1, targetAltitude, exclude, isReallyTheCenterUAV, centerUAVLocationUTM,
						numUAVs, flightFormation, landFormation, formationPos, formationYaw);
			} else {
				output.clear();
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
		output.close();
		if (targetLocation == null) {
			gui.exit(MSText.MASTER_ID_NOT_FOUND);
		}
		
		
		
		
		// 5. Get the data to be returned by this Thread and start the thread talker
		this.result.set(data);
		
		new TakeOffMasterDataTalkerThread(numUAV, messages.values().toArray(new byte[messages.size()][]), state).start();
		
		// 6. Wait slaves for ack
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
		Triplet<Integer, Long, Location2DUTM>[] match = TakeOffMasterDataListenerThread.safeTakeOffGetMatch(groundLocations, flightFormation, formationYaw, masterID);
		
		// 2. Get the target location of the UAV that should be in the center of the formation
		int centerUAVAirPos = flightFormation.getCenterUAVPosition();
		Triplet<Integer, Long, Location2DUTM> centerMatch = null;
		for (int i = 0; i < numUAVs; i++) {
			if (match[i].getValue0() == centerUAVAirPos) {
				centerMatch = match[i];
			}
		}
		if (centerMatch == null) {
			API.getGUI(0).exit(MSText.CENTER_ID_NOT_FOUND);
		}
		
		return Pair.with(centerMatch.getValue1(), centerMatch.getValue2());
	}
	
	/**
	 * This is used in the master UAV only. Get the best match between the current UAVs location on the ground and the target formation in the air. This solution minimizes the risk of collision while the UAVs perform the take off.
	 * @param groundLocations Location and ID of each of all the UAVs, including the UAV that could be in the center of the flight formation.
	 * @param flightFormation Flight formation to use while flying.
	 * @param formationYaw (rad) Yaw that will be set for the flight formation.
	 * @param masterID The ID of the master UAV if it must be in the middle of the formation, null otherwise. The default implementation of this method searches which UAV should be in the middle of the formation to minimize the take off time. Setting the master UAV as the center UAV builds the flight formation around the current location of the master UAV.
	 * @return An array containing the match for each of that UAVs included in the takeoff process, it is the position in the provided <i>flightFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	private static Triplet<Integer, Long, Location2DUTM>[] safeTakeOffGetMatch(Map<Long, Location2DUTM> groundLocations,
			FlightFormation flightFormation, double formationYaw, Long masterID) {
		
		if (groundLocations == null || groundLocations.size() == 0
				|| flightFormation == null || groundLocations.size() != flightFormation.getNumUAVs()) {
			return null;
		}
		
		int numUAVs = groundLocations.size();
		
		Long[] ids;
		Map<Long, Location2DUTM> ground = new HashMap<Long, Location2DUTM>(groundLocations);
		Location2DUTM groundCenterLocation;
		Map<Integer, Location2DUTM> airLocations = new HashMap<Integer, Location2DUTM>((int)Math.ceil(numUAVs / 0.75) + 1);
		if (masterID != null) {
			// The center of the flying formation will be on the center UAV of the ground formation
			// We arrange all the UAVs but the center UAV (not included in ground nor airLocations for the calculus)
			numUAVs--;
			ground.remove(masterID);
			groundCenterLocation = groundLocations.get(masterID);
			int flyingCenterPosition = flightFormation.getCenterUAVPosition();
			for (int j = 0; j < flightFormation.getNumUAVs(); j++) {
				if (j != flyingCenterPosition) {
					airLocations.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
				}
			}
			// Now we include again the center UAV in the center of the flight formation
			Pair<Double, Triplet<Integer, Long, Location2DUTM>[]> bestFit = TakeOffMasterDataListenerThread.getOptimalMatch(ground, airLocations);
			Triplet<Integer, Long, Location2DUTM>[] newFit = Arrays.copyOf(bestFit.getValue1(), bestFit.getValue1().length + 1);
			newFit[newFit.length - 1] = Triplet.with(flyingCenterPosition, masterID, groundCenterLocation);
			
			return newFit;
		} else {
			boolean useOptimal = true;//TODO modificar para comparar soluciones
			
			// First we store the IDs to iterate looking for the best fit
			ids = ground.keySet().toArray(new Long[numUAVs]);
			
			if (useOptimal) {
				// The center of the flying formation will be one of the coordinates of the UAVs
				// 1. Search looking the better center UAV for the flying formation
				double errorBestFit = Double.MAX_VALUE;
				Triplet<Integer, Long, Location2DUTM>[] bestFit = null;
				Pair<Double, Triplet<Integer, Long, Location2DUTM>[]> currentFit;
				
				for (int i = 0; i < numUAVs; i++) {
					groundCenterLocation = ground.get(ids[i]);
					// 2. For the selected center UAV, get the formation in the air
					for (int j = 0; j < numUAVs; j++) {
						airLocations.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
					}
					
					// 3. We check all possible permutations of the UAVs IDs looking for the combination that better fits
					//   with the current UAVs location
					currentFit = TakeOffMasterDataListenerThread.getOptimalMatch(ground, airLocations);
					
					// 6. If the minimum error with this center is lower, update the best solution
					if (bestFit == null || currentFit.getValue0() < errorBestFit) {
						bestFit = Arrays.copyOf(currentFit.getValue1(), currentFit.getValue1().length);
						errorBestFit = currentFit.getValue0();
					}
				}
				
				return bestFit;
			} else {
				// The center of the flying formation will be on  the mean coordinates of all the UAVs on the ground
				double x = 0;
				double y = 0;
				Location2DUTM location;
				for (int i = 0; i < numUAVs; i++) {
					location = ground.get(ids[i]);
					x = x + location.x;
					y = y + location.y;
				}
				groundCenterLocation = new Location2DUTM(x / numUAVs, y / numUAVs);
				for (int j = 0; j < numUAVs; j++) {
					airLocations.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
				}
				
				Pair<Double, Triplet<Integer, Long, Location2DUTM>[]> bestFit = TakeOffMasterDataListenerThread.getOptimalMatch(ground, airLocations);
				return bestFit.getValue1();
			}
		}
	}
	
	/**
	 * Get the optimal match between the current UAVs location and the target formation in the air.
	 * @param groundLocations ID and location of all the UAVs that will take part in the takeoff.
	 * @param airLocations Location of all the UAVs that will take part in the takeoff in the flying formation.
	 * @return Two values: First, the minimized value <i>sum(distance^2)</i> of the distance between the current (ground) and target (air) location of all the UAVs that will take part in the takeoff.
	 * Second, an array containing the match for each of that UAVs, it is the position in the provided <i>airFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	private static Pair<Double, Triplet<Integer, Long, Location2DUTM>[]> getOptimalMatch(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations) {
		if (groundLocations == null || groundLocations.size() == 0 || airLocations == null
				|| groundLocations.size() != airLocations.size()) {
			return null;
		}
		int numUAVs = groundLocations.size();
		Long[] ids = groundLocations.keySet().toArray(new Long[groundLocations.size()]);
		
		Permutation<Long> p = new Permutation<Long>(ids);
		Triplet<Integer, Long, Location2DUTM>[] bestFitCurrentCenter = null;
		double errorBestFitCurrentCenter = Double.MAX_VALUE;
		Long[] permutation;
		@SuppressWarnings("unchecked")
		Triplet<Integer, Long, Location2DUTM>[] match = new Triplet[numUAVs];
		double errorTot;
		Location2DUTM groundLocation, airLocation;
		long id;
		Iterator<Entry<Integer, Location2DUTM>> air;
		Entry<Integer, Location2DUTM> airPosition;
		permutation = p.next();
		while (permutation != null) {
			errorTot = 0;
			// 4. Sum of the distance^2 of all UAVs on the ground, relative to the location in the air
			air = airLocations.entrySet().iterator();
			for (int j = 0; j < numUAVs; j++) {
				id = permutation[j];
				airPosition = air.next();
				groundLocation = groundLocations.get(id);
				airLocation = airPosition.getValue();
				match[j] = Triplet.with(airPosition.getKey(), id, airLocation);
				errorTot = errorTot + Math.pow(groundLocation.distance(airLocation), 2);
			}
			
			// 5. If the error of this permutation is lower, store the permutation, and the error
			if (bestFitCurrentCenter == null || errorTot < errorBestFitCurrentCenter) {
				bestFitCurrentCenter = Arrays.copyOf(match, match.length);
				errorBestFitCurrentCenter = errorTot;
			}
			permutation = p.next();
		}
		return Pair.with(errorBestFitCurrentCenter, bestFitCurrentCenter);
	}
	
	/**
	 * Get the best takeoff sequence to reduce the collision probability.
	 * @param groundLocations ID and location of all the UAVs that will take part in the takeoff.
	 * @param airMatch Position in the flying formation, ID, and flying location for all the UAVs that will take part in the takeoff.
	 * @return An array with the position of the UAVs in the air formation sorted in descending distance from the ground location to the target flying location, and the ID.
	 */
	private static Pair<Integer, Long>[] getTakeoffSequence(Map< Long, Location2DUTM> groundLocations,
			Triplet<Integer, Long, Location2DUTM>[] airMatch) {
		if (groundLocations == null || groundLocations.size() == 0 ||
				airMatch == null || airMatch.length != groundLocations.size()) {
			return null;
		}
		
		// Calculus of the distance between the current location of the UAV and the target location in the flying formation
		@SuppressWarnings("unchecked")
		Triplet<Integer, Long, Double>[] distance = new Triplet[airMatch.length];
		Triplet<Integer, Long, Location2DUTM> match;
		for (int i = 0; i < airMatch.length; i++) {
			match = airMatch[i];
			distance[i] = Triplet.with(match.getValue0(),
					match.getValue1(),
					groundLocations.get(match.getValue1()).distance(match.getValue2()));
		}
		// Sorting by the distance, in descending order
		Arrays.sort(distance, new Comparator<Triplet<Integer, Long, Double>>() {

			@Override
			public int compare(Triplet<Integer, Long, Double> o1, Triplet<Integer, Long, Double> o2) {
				return o2.getValue2().compareTo(o1.getValue2());	// Descending order
			}
		});
		// Remove the distance from the resulting array
		@SuppressWarnings("unchecked")
		Pair<Integer, Long>[] sequence = new Pair[distance.length];
		for (int i = 0; i < sequence.length; i++) {
			sequence[i] = Pair.with(distance[i].getValue0(), distance[i].getValue1()) ;
		}
		
		return sequence;
	}

}
