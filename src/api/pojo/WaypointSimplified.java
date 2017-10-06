package api.pojo;

/** This class generates a simplified waypoint, including only coordinates and the sequence number. */

public class WaypointSimplified extends Point3D {
	private static final long serialVersionUID = 1L;
	public int numSeq;		// Waypoint position in the mission sequence

	public WaypointSimplified(int numSeq, double x, double y, double z) {
		super(x, y, z);
		this.numSeq = numSeq;
	}

	@Override
	public Object clone() {
		return new WaypointSimplified(this.numSeq, this.x, this.y, this.z);
	}

	@Override
	public String toString() {
		return "WP" + this.numSeq + "(" + this.x + "," + this.y + "," + this.z + ")";
	}

}
