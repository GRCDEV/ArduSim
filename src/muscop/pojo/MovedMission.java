package muscop.pojo;

import api.pojo.Point3D;

/** <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class MovedMission implements Comparable<MovedMission> {

	public long id;
	public int formationPosition;
	public Point3D[] locations;
	
	@SuppressWarnings("unused")
	private MovedMission() {
		
	}
	
	public MovedMission(long id, int formationPosition, Point3D[] locations) {
		this.id=id;
		this.formationPosition = formationPosition;
		this.locations = locations;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MovedMission)) {
			return false;
		}
		return this.formationPosition == ((MovedMission)obj).formationPosition;
	}
	@Override
	public int compareTo(MovedMission o) {
		return this.formationPosition - o.formationPosition;
	}
	
}
