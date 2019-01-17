package api.pojo.formations;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import api.pojo.Permutation;
import api.pojo.UTMCoordinates;
import main.Text;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public abstract class FlightFormation {
	
	public enum Formation {
		LINEAR((short)0, Text.LINEAR_FORMATION),
		REGULAR_MATRIX((short)1, Text.MATRIX_REGULAR_FORMATION),
		COMPACT_MATRIX((short)2, Text.MATRIX_COMPACT_FORMATION),
		CIRCLE((short)2, Text.CIRCLE_FORMATION),
		COMPACT_MESH((short)3, Text.MESH_COMPACT_FORMATION);
		// Add here new formations
		
		private final short formationId;
		private final String name;
		
		private Formation(short formationId, String name) {
			this.formationId = formationId;
			this.name = name;
		}
		
		public short getFormationId() {
			return this.formationId;
		}
		
		public String getName() {
			return this.name;
		}
		
		public static Formation getFormation(short formationId) {
			Formation[] formations = Formation.values();
			for (int i = 0; i < formations.length; i++) {
				if (formations[i].getFormationId() == formationId) {
					return formations[i];
				}
			}
			return null;
		}
		
		public static Formation getFormation(String name) {
			Formation[] formations = Formation.values();
			for (int i = 0; i < formations.length; i++) {
				if (formations[i].getName().equals(name)) {
					return formations[i];
				}
			}
			return null;
		}
	}
	
	/** Get a flight formation.
	 * @param formation Flight formation type as detailed in enumerator api.pojo.formations.FlightFormation.Formation
	 * @param numUAVs. Number of multicopters in the formation.
	 * @param minDistance. Minimum distance between multicopters in meters.
	 * @return A FlightFormation or null if the formation was not found.
	 */
	public static FlightFormation getFormation(Formation formation, int numUAVs, double minDistance) {
		if (formation == Formation.LINEAR) {
			return new LinearFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.REGULAR_MATRIX) {
			return new MatrixFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.COMPACT_MATRIX) {
			return new MatrixCompactFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.CIRCLE) {
			return new CircularFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.COMPACT_MESH) {
			return new MeshCompactFlightFormation(numUAVs, minDistance);
		}
		return null;
	}
	
	/** Get the formation of UAVs used for the ground layout in simulations. */
	public static FlightFormation.Formation getGroundFormation() {
		return UAVParam.groundFormation.get();
	}
	
	/** Get the minimum distance between contiguous UAVs in the formation used for the ground layout in simulations. */
	public static double getGroundFormationDistance() {
		return UAVParam.groundDistanceBetweenUAV;
	}
	
	/** Set the flight formation of UAVs used for the ground layout in simulations. */
	public static void setGroundFormation(FlightFormation.Formation formation) {
		UAVParam.groundFormation.set(formation);
	}
	
	/** Set the minimum distance between contiguous UAVs in the formation used for the ground layout in simulations. */
	public static void setGroundFormationDistance(double distance) {
		UAVParam.groundDistanceBetweenUAV = distance;
	}
	
	/** Get the formation of UAVs used for the flying layout. */
	public static FlightFormation.Formation getFlyingFormation() {
		return UAVParam.airFormation.get();
	}
	
	/** Get the minimum distance between contiguous UAVs in the formation used for the flying layout. */
	public static double getFlyingFormationDistance() {
		return UAVParam.airDistanceBetweenUAV;
	}
	
	/** Set the flight formation of UAVs used for the flying layout. */
	public static void setFlyingFormation(FlightFormation.Formation formation) {
		UAVParam.airFormation.set(formation);
	}
	
	/** Set the minimum distance between contiguous UAVs in the formation used for the flying layout. */
	public static void setFlyingFormationDistance(double distance) {
		UAVParam.airDistanceBetweenUAV = distance;
	}
	
	/** Get the minimum distance between contiguous UAVs while landing. */
	public static double getLandingFormationDistance() {
		return UAVParam.landDistanceBetweenUAV;
	}
	
	/** Set the minimum distance between contiguous UAVs while landing. */
	public static void setLandingFormationDistance(double distance) {
		UAVParam.landDistanceBetweenUAV = distance;
	}
	
	/**
	 * Get the match between the current UAVs location and the target formation in the air.
	 * <p>The center UAV must only be specified if it should be excluded from the takeoff process, and even then, it must be included in the <i>groundLocations> Map, and in the size of the airFormation (<i>airFormation.numUAVs</i>). Returns null if any error happens.</p>
	 * @param groundLocations ID and location of all the UAVs, including the center UAV on the ground, whether it will be excluded or not from the takeoff process.
	 * @param heading Heading for the flying formation.
	 * @param centerIncluded Whether the center UAV on the ground should be included or not in the takeoff process.
	 * @param centerId ID of the center UAV to be excluded from the takeoff process. Value ignored if all the UAVs will be included in the takeoff process.
	 * @param airFormation The <i>FlightFormation</i> layout of the UAVs that they will form at the end of the takeoff process.
	 * @return An array containing the match for each of that UAVs included in the takeoff process, it is the position in the provided <i>airFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	public static Triplet<Integer, Long, UTMCoordinates>[] matchIDs(Map<Long, UTMCoordinates> groundLocations, double heading,
			boolean centerIncluded, Long centerId, FlightFormation airFormation) {
		if (groundLocations == null || groundLocations.size() == 0
				|| (!centerIncluded && (groundLocations.size() == 0 || centerId == null))
				|| airFormation == null || groundLocations.size() != airFormation.numUAVs) {
			return null;
		}
		int numUAVs = groundLocations.size();
		
		// First we store the IDs to iterate looking for the best fit
		Long[] ids;
		Map<Long, UTMCoordinates> ground = new HashMap<Long, UTMCoordinates>(groundLocations);
		if (!centerIncluded) {
			numUAVs--;
			ground.remove(centerId);
		}
		ids = ground.keySet().toArray(new Long[numUAVs]);
		
		UTMCoordinates groundCenterLocation;
		Map<Integer, UTMCoordinates> airLocations = new HashMap<Integer, UTMCoordinates>((int)Math.ceil(numUAVs / 0.75) + 1);
		if (centerIncluded) {
			boolean useOptimal = true;//TODO modificar para comparar soluciones
			
			
			if (useOptimal) {
				// The center of the flying formation will be one of the coordinates of the UAVs
				// 1. Search looking the better center UAV for the flying formation
				double errorBestFit = Double.MAX_VALUE;
				Triplet<Integer, Long, UTMCoordinates>[] bestFit = null;
				Pair<Double, Triplet<Integer, Long, UTMCoordinates>[]> currentFit;
				
				for (int i = 0; i < numUAVs; i++) {
					groundCenterLocation = ground.get(ids[i]);
					// 2. For the selected center UAV, get the formation in the air
					for (int j = 0; j < numUAVs; j++) {
						airLocations.put(j, airFormation.getLocation(j, groundCenterLocation, heading));
					}
					
					// 3. We check all possible permutations of the UAVs IDs looking for the combination that better fits
					//   with the current UAVs location
					currentFit = FlightFormation.getOptimalMatch(ground, airLocations);
					
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
				UTMCoordinates location;
				for (int i = 0; i < numUAVs; i++) {
					location = ground.get(ids[i]);
					x = x + location.x;
					y = y + location.y;
				}
				groundCenterLocation = new UTMCoordinates(x / numUAVs, y / numUAVs);
				for (int j = 0; j < numUAVs; j++) {
					airLocations.put(j, airFormation.getLocation(j, groundCenterLocation, heading));
				}
				
				Pair<Double, Triplet<Integer, Long, UTMCoordinates>[]> bestFit = FlightFormation.getOptimalMatch(ground, airLocations);
				return bestFit.getValue1();
			}
		} else {
			// The center of the flying formation will be on the center UAV of the ground formation
			groundCenterLocation = groundLocations.get(centerId);
			int flyingCenterPosition = airFormation.getCenterUAVPosition();
			for (int j = 0; j < airFormation.numUAVs; j++) {
				if (j != flyingCenterPosition) {
					airLocations.put(j, airFormation.getLocation(j, groundCenterLocation, heading));
				}
			}
			
			Pair<Double, Triplet<Integer, Long, UTMCoordinates>[]> bestFit = FlightFormation.getOptimalMatch(ground, airLocations);
			return bestFit.getValue1();
		}
	}
	
	/**
	 * Get the optimal match between the current UAVs location and the target formation in the air.
	 * @param groundLocations ID and location of all the UAVs that will take part in the takeoff.
	 * @param airLocations Location of all the UAVs that will take part in the takeoff in the flying formation.
	 * @return Two values: First, the minimized value <i>sum(distance^2)</i> of the distance between the current (ground) and target (air) location of all the UAVs that will take part in the takeoff.
	 * Second, an array containing the match for each of that UAVs, it is the position in the provided <i>airFormation</i> array, the ID, and target flying coordinates. Return null if any error happens.
	 */
	private static Pair<Double, Triplet<Integer, Long, UTMCoordinates>[]> getOptimalMatch(Map<Long, UTMCoordinates> groundLocations, Map<Integer, UTMCoordinates> airLocations) {
		if (groundLocations == null || groundLocations.size() == 0 || airLocations == null
				|| groundLocations.size() != airLocations.size()) {
			return null;
		}
		int numUAVs = groundLocations.size();
		Long[] ids = groundLocations.keySet().toArray(new Long[groundLocations.size()]);
		
		Permutation<Long> p = new Permutation<Long>(ids);
		Triplet<Integer, Long, UTMCoordinates>[] bestFitCurrentCenter = null;
		double errorBestFitCurrentCenter = Double.MAX_VALUE;
		Long[] permutation;
		@SuppressWarnings("unchecked")
		Triplet<Integer, Long, UTMCoordinates>[] match = new Triplet[numUAVs];
		double errorTot;
		UTMCoordinates groundLocation, airLocation;
		long id;
		Iterator<Entry<Integer, UTMCoordinates>> air;
		Entry<Integer, UTMCoordinates> airPosition;
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
	public static Pair<Integer, Long>[] getTakeoffSequence(Map< Long, UTMCoordinates> groundLocations,
			Triplet<Integer, Long, UTMCoordinates>[] airMatch) {
		if (groundLocations == null || groundLocations.size() == 0 ||
				airMatch == null || airMatch.length != groundLocations.size()) {
			return null;
		}
		
		// Calculus of the distance between the current location of the UAV and the target location in the flying formation
		@SuppressWarnings("unchecked")
		Triplet<Integer, Long, Double>[] distance = new Triplet[airMatch.length];
		Triplet<Integer, Long, UTMCoordinates> match;
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
	
	
	public static final long BROADCAST_MAC_ID = 281474976710655L; // Broadcast MAC (0xFFFFFFFFFFFF)
	
	protected int numUAVs;
	protected double minDistance;
	protected int centerUAV;
	protected FormationPoint[] point;
	
	
	@SuppressWarnings("unused")
	private FlightFormation() {};
	
	/** Just leave the default implementation of the constructor.
	 * @param numUAVs. The number of UAVs in the formation (at least one).
	 * @param minDistance. Minimum distance between two contiguous UAVs.
	 */
	protected FlightFormation(int numUAVs, double minDistance) {
		this.numUAVs = numUAVs;
		this.minDistance = minDistance;
		this.point = new FormationPoint[numUAVs];
		this.initializeFormation();
	}
	
	/** Calculate the centerUAV (located in [0,0] coordinates), and the offset of the remaining UAVs to the centerUAV.
	 * <p>Don't call this method. It is automatically used when the formation object is created.</p> */
	protected abstract void initializeFormation();
	
	/** Get the position of the center UAV in the formation. */
	public int getCenterUAVPosition() {
		return this.centerUAV;
	}
	
	/** Get the offset of the UAV from the center UAV in the formation in UTM coordinates, given its position in the formation, and the center UAV heading. */
	public UTMCoordinates getOffset(int position, double centerUAVHeading) {
		FormationPoint p = this.point[position];
		double x = p.offsetX * Math.cos(centerUAVHeading) + p.offsetY * Math.sin(centerUAVHeading);
		double y = - p.offsetX * Math.sin(centerUAVHeading) + p.offsetY * Math.cos(centerUAVHeading);
		return new UTMCoordinates(x, y);
		
	}
	
	/** Get location of a UAV in UTM coordinates, given its position in the formation, and the center UAV location (UTM) and heading (rad). */
	public UTMCoordinates getLocation(int position, UTMCoordinates centerUAVLocation, double centerUAVHeading) {
		UTMCoordinates res = this.getOffset(position, centerUAVHeading);
		res.x = res.x + centerUAVLocation.x;
		res.y = res.y + centerUAVLocation.y;
		return res;
	}
	
}
