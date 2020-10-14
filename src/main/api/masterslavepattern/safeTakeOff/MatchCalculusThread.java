package main.api.masterslavepattern.safeTakeOff;

import es.upv.grc.mapper.Location2DUTM;
import main.api.formations.FlightFormation;
import main.api.formations.helpers.Permutation;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.*;
import java.util.Map.Entry;

public class MatchCalculusThread extends Thread {
	
	private volatile Quartet<Integer, Long, Location2DUTM, Double>[] result = null;
	
	private Map<Long, Location2DUTM> groundLocations;
	private FlightFormation flightFormation;
	private double formationYaw;
	private Long masterID;
	
	@SuppressWarnings("unused")
	private MatchCalculusThread() {}
	
	public MatchCalculusThread(Map<Long, Location2DUTM> groundLocations,
			FlightFormation flightFormation, double formationYaw, Long masterID) {
		this.groundLocations = groundLocations;
		this.flightFormation = flightFormation;
		this.formationYaw = formationYaw;
		this.masterID = masterID;
	}

	@Override
	public void run() {
		this.result = MatchCalculusThread.safeTakeOffGetMatch(groundLocations, flightFormation, formationYaw, masterID);
	}
	
	public Quartet<Integer, Long, Location2DUTM, Double>[] getResult() {
		return this.result;
	}
	
	/**
	 * This is used in the master UAV only. Get the best match between the current UAVs location on the ground and the target formation in the air. This solution minimizes the risk of collision while the UAVs perform the take off.
	 * @param groundLocations Location and ID of each of all the UAVs, including the UAV that could be in the center of the flight formation.
	 * @param flightFormation Flight formation to use while flying.
	 * @param formationYaw (rad) Yaw that will be set for the flight formation.
	 * @param masterID The ID of the master UAV if it must be in the middle of the flight formation, null otherwise. The default implementation of this method searches which UAV should be in the middle of the formation to minimize the take off time. Setting the master UAV as the center UAV builds the flight formation around the current location of the master UAV.
	 * @return An array containing the match for each of that UAVs included in the takeoff process, it is the position in the provided <i>flightFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 * Quartet Integer IDofUAV, Long IDofUAV, Location2DUTM airlocation, Double error (distance2)
	 */
	private static Quartet<Integer, Long, Location2DUTM, Double>[] safeTakeOffGetMatch(Map<Long, Location2DUTM> groundLocations,
			FlightFormation flightFormation, double formationYaw, Long masterID) {
		
		if (groundLocations == null || groundLocations.size() == 0
				|| flightFormation == null || groundLocations.size() != flightFormation.getNumUAVs()) {
			return null;
		}
		
		Quartet<Integer, Long, Location2DUTM, Double>[] match = null;
		
		int numUAVs = groundLocations.size();
		
		Map<Long, Location2DUTM> ground = new HashMap<>(groundLocations);
		Map<Integer, Location2DUTM> air = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
		
		Location2DUTM groundCenterLocation;

		if (masterID != null) {
			// The center of the flying formation will be over the current location of the master UAV on the ground
			// Solution provided for Follow Me protocol, as the master UAV must take-off vertically always to the center of the flight formation
			// We arrange all the UAVs but the center UAV (not included in ground nor airLocations for the calculus)
			ground.remove(masterID);
			groundCenterLocation = groundLocations.get(masterID);
			int flyingCenterPosition = flightFormation.getCenterUAVPosition();
			for (int j = 0; j < flightFormation.getNumUAVs(); j++) {
				if (j != flyingCenterPosition) {
					air.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
				}
			}
			Quartet<Integer, Long, Location2DUTM, Double>[] fit = null;
			
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.OPTIMAL) {
				fit = MatchCalculusThread.getOptimalMatch(ground, air).getValue1();
			}
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.SIMPLIFIED) {
				fit = MatchCalculusThread.getSimplifiedMatch(ground, air, groundCenterLocation);
			}
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.RANDOM) {
				fit = MatchCalculusThread.getRandomMatch(ground, air);
			}
			if(TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.HUNGARIAN){
				fit = new HungarianTakeOff(groundLocations,air).getHungarianMatch();
			}
			// Now we include again the center UAV in the center of the flight formation
			if(fit != null) {
				match = Arrays.copyOf(fit, fit.length + 1);
				match[match.length - 1] = Quartet.with(flyingCenterPosition, masterID, groundCenterLocation, 0.0);
			}
			
		} else {
			// General case
			// First we store the IDs to iterate looking for the best fit
			Long[] ids = ground.keySet().toArray(new Long[numUAVs]);
			
			Pair<Double, Quartet<Integer, Long, Location2DUTM, Double>[]> currentFit;
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.OPTIMAL) {
				// The center of the flying formation will be one of the coordinates of the UAVs
				// 1. Search looking the better center UAV for the flying formation
				double errorBestFit = Double.MAX_VALUE;
				for (int i = 0; i < numUAVs; i++) {
					groundCenterLocation = ground.get(ids[i]);
					// 2. For the selected center UAV, get the formation in the air
					air.clear();
					for (int j = 0; j < numUAVs; j++) {
						air.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
					}
					
					// 3. We check all possible permutations of the UAVs IDs looking for the combination that better fits
					//   with the current UAVs location
					currentFit = MatchCalculusThread.getOptimalMatch(ground, air);
					
					// 4. If the minimum error with this center is lower, update the best solution
					if (match == null || currentFit.getValue0() < errorBestFit) {
						match = Arrays.copyOf(currentFit.getValue1(), currentFit.getValue1().length);
						errorBestFit = currentFit.getValue0();
					}
				}
			}
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.SIMPLIFIED) {
				// The center of the flying formation will be on  the mean coordinates of all the UAVs on the ground
				double x = 0;
				double y = 0;
				Location2DUTM location;
				for (int i = 0; i < numUAVs; i++) {
					location = groundLocations.get(ids[i]);
					x = x + location.x;
					y = y + location.y;
				}
				groundCenterLocation = new Location2DUTM(x / numUAVs, y / numUAVs);

				for (int j = 0; j < numUAVs; j++) {
					air.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
				}
				match = MatchCalculusThread.getSimplifiedMatch(ground, air, groundCenterLocation);
			}
			if (TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.RANDOM) {
				// The center of the flying formation will be on  the mean coordinates of all the UAVs on the ground
				double x = 0;
				double y = 0;
				Location2DUTM location;
				for (int i = 0; i < numUAVs; i++) {
					location = groundLocations.get(ids[i]);
					x = x + location.x;
					y = y + location.y;
				}
				groundCenterLocation = new Location2DUTM(x / numUAVs, y / numUAVs);
				
				for (int j = 0; j < numUAVs; j++) {
					air.put(j, flightFormation.getLocation(j, groundCenterLocation, formationYaw));
				}
				match = MatchCalculusThread.getRandomMatch(ground, air);
			}
			if(TakeOffMasterDataListenerThread.selectedAlgorithm == TakeOffAlgorithm.HUNGARIAN){
				match = new HungarianTakeOff(groundLocations,air).getHungarianMatch();
			}
		}


		return match;
	}
	
	/**
	 * Get the optimal match between the current UAVs location and the target formation in the air.
	 * @param groundLocations ID and location of all the UAVs that will take part in the takeoff.
	 * @param airLocations Location of all the UAVs that will take part in the takeoff in the flying formation.
	 * @return First, the minimized value <i>sum(distance^2)</i> of the distance between the current (ground) and target (air) location of all the UAVs that will take part in the takeoff.
	 * Second, an array containing the match for each of that UAVs, it is the position in the provided <i>airFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	private static Pair<Double, Quartet<Integer, Long, Location2DUTM, Double>[]> getOptimalMatch(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations) {
		if (groundLocations == null || groundLocations.size() == 0 || airLocations == null
				|| groundLocations.size() != airLocations.size()) {
			return null;
		}
		int numUAVs = groundLocations.size();
		Long[] ids = groundLocations.keySet().toArray(new Long[0]);
		
		Permutation<Long> p = new Permutation<>(ids);
		Quartet<Integer, Long, Location2DUTM, Double>[] bestFitCurrentCenter = null;
		double bestFitCurrentCenterError = Double.MAX_VALUE;
		Long[] permutation;
		@SuppressWarnings("unchecked")
		Quartet<Integer, Long, Location2DUTM, Double>[] match = new Quartet[numUAVs];
		double error, errorTot;
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
				error = Math.pow(groundLocation.distance(airLocation), 2);
				
				match[j] = Quartet.with(airPosition.getKey(), id, airLocation, error);
				errorTot = errorTot + error;
			}
			
			// 5. If the error of this permutation is lower, store the permutation, and the error
			if (bestFitCurrentCenter == null || errorTot < bestFitCurrentCenterError) {
				bestFitCurrentCenter = Arrays.copyOf(match, match.length);
				bestFitCurrentCenterError = errorTot;
			}
			permutation = p.next();
		}
		
		// 6. We sort the result given the distance in descending order to get the take-off sequence
		if(bestFitCurrentCenter != null) {
			Arrays.sort(bestFitCurrentCenter, (o1, o2) -> {
				return o2.getValue3().compareTo(o1.getValue3());    // Descending order
			});
		}
		
		return Pair.with(bestFitCurrentCenterError, bestFitCurrentCenter);
	}
	
	/**
	 * Get the optimal match between the current UAVs location and the target formation in the air.
	 * @param groundLocations ID and location of all the UAVs that will take part in the takeoff.
	 * @param airLocations Location of all the UAVs that will take part in the takeoff in the flying formation.
	 * @param centerLocation Location of the center of the ground formation that will be used as center of the flight formation.
	 * @return An array containing the match for each of that UAVs, it is the position in the provided <i>airFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	private static Quartet<Integer, Long, Location2DUTM, Double>[] getSimplifiedMatch(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations, Location2DUTM centerLocation) {
		if (groundLocations == null || groundLocations.size() == 0
				|| airLocations == null || groundLocations.size() != airLocations.size()) {
			return null;
		}
		
		int numUAVs = groundLocations.size();
		
		// 1. We get the distance of all the airLocations to the formation center
		@SuppressWarnings("unchecked")
		Quartet<Integer, Long, Location2DUTM, Double>[] airSorted = new Quartet[numUAVs];
		Iterator<Entry<Integer, Location2DUTM>> it = airLocations.entrySet().iterator();
		Entry<Integer, Location2DUTM> entry;
		for (int i = 0; i < numUAVs; i++) {
			entry = it.next();
			airSorted[i] = Quartet.with(entry.getKey(), null, entry.getValue(), centerLocation.distance(entry.getValue()));
		}
		
		// 2. We sort the flight locations in descending distance to the formation center
		Arrays.sort(airSorted, (o1, o2) -> {
			return o2.getValue3().compareTo(o1.getValue3());	// Descending order
		});
		
		//3. Assign ground location to each air location
		Iterator<Entry<Long, Location2DUTM>> it2;
		Entry<Long, Location2DUTM> current;
		Long bestID;
		Location2DUTM airLocation;
		double error;
		double bestError;
		for (int i = 0; i < numUAVs; i++) {
			bestID = null;
			bestError = Double.MAX_VALUE;
			airLocation = airSorted[i].getValue2();
			it2 = groundLocations.entrySet().iterator();
			while (it2.hasNext()) {
				current = it2.next();
				error = Math.pow(airLocation.distance(current.getValue()), 2);
				if (error < bestError) {
					bestID = current.getKey();
					bestError = error;
				}
			}
			
			airSorted[i] = airSorted[i].setAt1(bestID);
			airSorted[i] = airSorted[i].setAt3(bestError);
			groundLocations.remove(bestID);
		}
		
		return airSorted;
	}

	
	private static Quartet<Integer, Long, Location2DUTM, Double>[] getRandomMatch(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations) {
		// Copy ground and air locations to lists
		Entry<Long, Location2DUTM> entry1;
		Iterator<Entry<Long, Location2DUTM>> it1 = groundLocations.entrySet().iterator();
		List<Pair<Long, Location2DUTM>> ground = new ArrayList<>();
		while (it1.hasNext()) {
			entry1 = it1.next();
			ground.add(Pair.with(entry1.getKey(), entry1.getValue()));
		}
		Entry<Integer, Location2DUTM> entry2;
		Iterator<Entry<Integer, Location2DUTM>> it2 = airLocations.entrySet().iterator();
		List<Pair<Integer, Location2DUTM>> air = new ArrayList<>();
		while (it2.hasNext()) {
			entry2 = it2.next();
			air.add(Pair.with(entry2.getKey(), entry2.getValue()));
		}
		
		// Shuffle air locations
		Collections.shuffle(air);
		
		// Takeoff order and assignment random due to the previous shuffle
		double error;
		
		Location2DUTM gLocation, aLocation;
		@SuppressWarnings("unchecked")
		Quartet<Integer, Long, Location2DUTM, Double>[] airSorted = new Quartet[ground.size()];
		for (int i = 0; i < ground.size(); i ++) {
			gLocation = ground.get(i).getValue1();
			aLocation = air.get(i).getValue1();
			error = Math.pow(gLocation.distance(aLocation), 2);
			
			airSorted[i] = Quartet.with(air.get(i).getValue0(), ground.get(i).getValue0(),
					aLocation, error);
		}
		
		return airSorted;
	}
	
}