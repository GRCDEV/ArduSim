package api.pojo;

import java.util.List;

/** This class generates a simplified waypoint, including only coordinates and the sequence number in the waypoints that form the mission.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

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
	
	public static String ListToString(List<WaypointSimplified> list) {
		StringBuilder sb =new StringBuilder(500);
		sb.append("Mission:");
		for (int i = 0; i < list.size(); i++) {
			sb.append("\n").append(list.get(i).toString());
		}
		return sb.toString();
	}

}
