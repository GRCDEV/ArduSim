package main.api.formations;

import es.upv.grc.mapper.Location2DUTM;
import main.Text;
import main.api.formations.helpers.FormationPoint;

/** 
 * The base flight formation that must be extended by any new flight formation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public abstract class FlightFormation {
	
	public enum Formation {
		/**
		 * The UAVs form a line with the center UAV in the middle of it.
		 * The formation numbering starts in 0, and increases from left to the right. */
		LINEAR((short)0, Text.LINEAR_FORMATION),
		/**
		 * The UAVs form a matrix.
		 * The formation numbering starts in 0 an increases from left to right, and down to up.
		 * The selection of the center UAV tries to minimize the distance to the further UAVs. */
		REGULAR_MATRIX((short)1, Text.MATRIX_REGULAR_FORMATION),
		/**
		 * The UAVs form a matrix.
		 * The formation numbering starts in 0 in the center of the formation and increases with the distance to the center of the formation. */
		COMPACT_MATRIX((short)2, Text.MATRIX_COMPACT_FORMATION),
		/**
		 * The UAVs form a circle around a central UAV.
		 * The formation numbering starts in 0, with the UAV in the center of the circle.
		 * The remaining UAVs are numbered beginning in the east with 1, and moving counterclockwise.
		 * The UAVs keep at least the minimum distance with the former and the later in the formation, and with the center UAV. */
		CIRCLE((short)3, Text.CIRCLE_FORMATION),
		/**
		 * The UAVs form a mesh where the UAVs are as close as possible, it is, the form equilateral triangles surrounding the center of the formation.
		 * The formation numbering starts in 0 in the center of the formation and increases with the distance to the center of the formation. */
		COMPACT_MESH((short)4, Text.MESH_COMPACT_FORMATION),
		/**
		 * Convenient formation for virtual UAVs deployment in simulations (<i>setStartingLocation</i> method of the protocol implementation).
		 * The UAVs are deployed surrounding a central UAV, and in random locations.
		 * The formation numbering starts in 0, in the center UAV, approximately located at the center of the formation. The remaining UAVs are located around it, keeping the minimum distance, and trying to maintain a circular distribution around the center UAV. */
		RANDOM((short)5, Text.RANDOM);
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
	 * @return A FlightFormation or null if the formation was not found, or <i>numUAVs</i> <= 0.
	 */
	public static FlightFormation getFormation(Formation formation, int numUAVs, double minDistance) {
		if (numUAVs <= 0) {
			return null;
		}
		
		if (formation == Formation.LINEAR) {
			return new FormationLinear(numUAVs, minDistance, formation);
		}
		if (formation == Formation.REGULAR_MATRIX) {
			return new FormationMatrix(numUAVs, minDistance, formation);
		}
		if (formation == Formation.COMPACT_MATRIX) {
			return new FormationMatrixCompact(numUAVs, minDistance, formation);
		}
		if (formation == Formation.CIRCLE) {
			return new FormationCircular(numUAVs, minDistance, formation);
		}
		if (formation == Formation.COMPACT_MESH) {
			return new FormationMeshCompact(numUAVs, minDistance, formation);
		}
		if (formation == Formation.RANDOM) {
			return new FormationRandom(numUAVs, minDistance, formation);
		}
		// Add here new formations
		return null;
	}
	
	protected int numUAVs;
	protected double minDistance;
	protected Formation formation;
	protected int centerUAVPosition;
	protected FormationPoint[] point;
	
	
	@SuppressWarnings("unused")
	private FlightFormation() {};
	
	/** Just leave the default implementation of the constructor.
	 * @param numUAVs. The number of UAVs in the formation (at least one).
	 * @param minDistance. Minimum distance between two contiguous UAVs.
	 */
	protected FlightFormation(int numUAVs, double minDistance, Formation formation) {
		this.numUAVs = numUAVs;
		this.minDistance = minDistance;
		this.formation = formation;
		this.point = new FormationPoint[numUAVs];
		this.initializeFormation();
	}
	
	/** Calculate the centerUAV (located in [0,0] coordinates), and the offset of the remaining UAVs to the centerUAV.
	 * <p>Don't call this method. It is automatically used when the formation object is created.</p> */
	protected abstract void initializeFormation();
	
	/** Get the position of the center UAV in the formation. */
	public int getCenterUAVPosition() {
		return this.centerUAVPosition;
	}
	
	/** Get the identifier of the formation type. */
	public short getFormationId() {
		return this.formation.getFormationId();
	}
	
	/** Get the minimum distance between the UAVs in the formation. */
	public double getFormationMinimumDistance() {
		return this.minDistance;
	}
	
	/** Get the name of the formation type. */
	public String getFormationName() {
		return this.formation.getName();
	}
	
	/** Get the number of UAVs that will take part in the flight formation. */
	public int getNumUAVs() {
		return this.numUAVs;
	}
	
	/**
	 * Get the offset of the UAV from the center UAV in the formation in UTM coordinates.
	 * @param position Position of the UAV in the flight formation.
	 * @param centerUAVYaw (rad) Yaw or heading of the formation, which matches with the center UAV yaw.
	 * @return Offset between the UAV and the UAV in the center of the formation in UTM coordinates.
	 */
	public Location2DUTM getOffset(int position, double centerUAVYaw) {
		FormationPoint p = this.point[position];
		double x = p.offsetX * Math.cos(centerUAVYaw) + p.offsetY * Math.sin(centerUAVYaw);
		double y = - p.offsetX * Math.sin(centerUAVYaw) + p.offsetY * Math.cos(centerUAVYaw);
		return new Location2DUTM(x, y);
		
	}
	
	/** Get location of a UAV in UTM coordinates, given its position in the formation, and the center UAV location (UTM) and heading (rad). */
	/**
	 * Get the location of a UAV in the formation.
	 * @param position Position of the UAV in the flight formation.
	 * @param centerUAVLocation Location of the UAV in the center of the formation.
	 * @param centerUAVYaw (rad) Yaw or heading of the formation, which matches with the center UAV yaw.
	 * @return Location of a UAV in the formation in UTM coordinates.
	 */
	public Location2DUTM getLocation(int position, Location2DUTM centerUAVLocation, double centerUAVYaw) {
		Location2DUTM res = this.getOffset(position, centerUAVYaw);
		res.x = res.x + centerUAVLocation.x;
		res.y = res.y + centerUAVLocation.y;
		return res;
	}
	
}
