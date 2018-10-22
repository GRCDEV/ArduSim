package api.pojo.formations;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import api.pojo.Permutation;
import api.pojo.UAV2DLocation;
import api.pojo.UTMCoordinates;
import main.Text;
import uavController.UAVParam;

public abstract class FlightFormation {
	
	public enum Formation {
		LINEAR((short)0, Text.LINEAR_FORMATION),
		MATRIX((short)1, Text.MATRIX_FORMATION),
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
	
	/** Provides a flight formation.
	 * @param formation Flight formation type as detailed in enumerator api.pojo.formations.FlightFormation.Formation
	 * @param numUAVs. Number of multicopters in the formation.
	 * @param minDistance. Minimum distance between multicopters in meters.
	 * @return A FlightFormation.
	 */
	public static FlightFormation getFormation(Formation formation, int numUAVs, double minDistance) {
		if (formation == Formation.LINEAR) {
			return new LinearFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.MATRIX) {
			return new MatrixFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.CIRCLE) {
			return new CircularFlightFormation(numUAVs, minDistance);
		}
		if (formation == Formation.COMPACT_MESH) {
			return new MeshCompactFlightFormation(numUAVs, minDistance);
		}
		return null;
	}
	
	/** Provides the formation of UAVs used for the ground layout in simulations. */
	public static FlightFormation.Formation getGroundFormation() {
		return UAVParam.groundFormation.get();
	}
	
	/** Provides the minimum distance between contiguous UAVs in the formation used for the ground layout in simulations. */
	public static double getGroundFormationDistance() {
		return UAVParam.groundDistanceBetweenUAV;
	}
	
	/** Sets the flight formation of UAVs used for the ground layout in simulations. */
	public static void setGroundFormation(FlightFormation.Formation formation) {
		UAVParam.groundFormation.set(formation);
	}
	
	/** Sets the minimum distance between contiguous UAVs in the formation used for the ground layout in simulations. */
	public static void setGroundFormationDistance(double distance) {
		UAVParam.groundDistanceBetweenUAV = distance;
	}
	
	/** Provides the formation of UAVs used for the flying layout. */
	public static FlightFormation.Formation getFlyingFormation() {
		return UAVParam.airFormation.get();
	}
	
	/** Provides the minimum distance between contiguous UAVs in the formation used for the flying layout. */
	public static double getFlyingFormationDistance() {
		return UAVParam.airDistanceBetweenUAV;
	}
	
	/** Sets the flight formation of UAVs used for the flying layout. */
	public static void setFlyingFormation(FlightFormation.Formation formation) {
		UAVParam.airFormation.set(formation);
	}
	
	/** Sets the minimum distance between contiguous UAVs in the formation used for the flying layout. */
	public static void setFlyingFormationDistance(double distance) {
		UAVParam.airDistanceBetweenUAV = distance;
	}
	
	/** Provides the minimum distance between contiguous UAVs while landing. */
	public static double getLandingFormationDistance() {
		return UAVParam.landDistanceBetweenUAV;
	}
	
	/** Sets the minimum distance between contiguous UAVs while landing. */
	public static void setLandingFormationDistance(double distance) {
		UAVParam.landDistanceBetweenUAV = distance;
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
	 * <p>Don't call this method. It is automatically used when the formation object is created. */
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
	
	/** Match UAVs IDs and ground location with the closest position in this AIR FORMATION.
	 * <p>Returns null if any error happens. */
	public Triplet<Integer, Long, UTMCoordinates>[] matchIDs(UAV2DLocation[] groundLocations, double heading) {
		int numUAVs = groundLocations.length;
		if (this.numUAVs != numUAVs) {
			return null;
		}
		
		// First we store the IDs, and their corresponding locations for efficient access
		Long[] ids = new Long[numUAVs];// cargo los IDs en un array
		Map<Long, UTMCoordinates> ground = new HashMap<Long, UTMCoordinates>((int)Math.ceil(numUAVs / 0.75) + 1);
		for (int i = 0; i < groundLocations.length; i++) {
			ground.put(groundLocations[i].id, groundLocations[i].location);
			ids[i] = groundLocations[i].id;
		}
		
		// 1. Search looking the better center UAV for the flying formation
		Triplet<Integer, Long, UTMCoordinates>[] bestFit = null;
		double errorBestFit = Double.MAX_VALUE;
		UTMCoordinates[] airFormation = new UTMCoordinates[numUAVs];
		long groundCenterId;
		UTMCoordinates groundCenterLocation;
		for (int i = 0; i < numUAVs; i++) {
			// The air center UAV is the one located in i position of groundLocations[]
			groundCenterId = groundLocations[i].id;
			groundCenterLocation = groundLocations[i].location;
			
			// 2. For the selected center UAV, get the formation in the air
			for (int j = 0; j < numUAVs; j++) {
				airFormation[j] = this.getLocation(j, groundCenterLocation, heading);
			}
			
			// 3. We check all possible permutations of the UAVs IDs looking for the combination that better fits
			//   with the current UAVs location
			Permutation<Long> p = new Permutation<Long>(ids);
			Triplet<Integer, Long, UTMCoordinates>[] bestFitCurrentCenter = null;
			double errorBestFitCurrentCenter = Double.MAX_VALUE;
			Long[] permutation;
			@SuppressWarnings("unchecked")
			Triplet<Integer, Long, UTMCoordinates>[] match = new Triplet[numUAVs];
			double error, errorTot;
			UTMCoordinates groundLocation, airLocation;
			long id;
			boolean centerUAVMoves;
			permutation = p.next();
			while (permutation != null) {
				errorTot = 0;
				centerUAVMoves = false;
				// 4. Sum of the distance^2 of all UAVs on the ground, relative to the location in the air
				for (int j = 0; j < numUAVs; j++) {
					id = permutation[j];
					groundLocation = ground.get(id);
					airLocation = airFormation[j];
					match[j] = Triplet.with(j, id, groundLocation);
					error = Math.pow(groundLocation.distance(airLocation), 2);
					if (id == groundCenterId && error > 0) {
						// Initial hypothesis: The center UAV doesn't move horizontally to the target flying location
						// We discard that kind of solutions
						centerUAVMoves = true;
					}
					errorTot = errorTot + error;
				}
				
				// 5. If the error of this permutation is lower, store the permutation, and the error
				if ((bestFitCurrentCenter == null || errorTot < errorBestFitCurrentCenter) && !centerUAVMoves) {
					bestFitCurrentCenter = Arrays.copyOf(match, match.length);
					errorBestFitCurrentCenter = errorTot;
				}
				permutation = p.next();
			}
			
			// 6. If the minimum error with this center is lower, update the best solution
			if (bestFit == null || errorBestFitCurrentCenter < errorBestFit) {
				bestFit = Arrays.copyOf(bestFitCurrentCenter, bestFitCurrentCenter.length);
				errorBestFit = errorBestFitCurrentCenter;
			}
		}
		
		return bestFit;
	}

	/** Get the best takoff sequence.
	 * @param centerUAVAir. Position in the flying formation, ID, and ground location of the center UAV (center in the flying formation).
	 * @param centerUAVHeading. Heading of the center UAV in the flying formation.
	 * @param airMatch. Position in the flying formation, ID, and ground locaiton for each UAV.
	 * @return Array with the position of the UAVs in the air formation sorted in descending distance from the ground location to the target flying location.
	 */
	public Pair<Integer, Long>[] getTakeoffSequence(Triplet<Integer, Long, UTMCoordinates> centerUAVAir, double centerUAVHeading, Triplet<Integer, Long, UTMCoordinates>[] airMatch) {
		if (airMatch == null || airMatch.length == 0 || airMatch.length != this.numUAVs || centerUAVAir == null) {
			return null;
		}
		
		// Calculus of the distance between the current location of the UAV and the target location in the flying formation
		UTMCoordinates centerGroundLocation = centerUAVAir.getValue2();
		@SuppressWarnings("unchecked")
		Triplet<Integer, Long, Double>[] distance = new Triplet[airMatch.length];
		for (int i = 0; i < airMatch.length; i++) {
			distance[i] = Triplet.with(airMatch[i].getValue0(),
					airMatch[i].getValue1(),
					this.getLocation(airMatch[i].getValue0(), centerGroundLocation, centerUAVHeading).distance(airMatch[i].getValue2()));
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
