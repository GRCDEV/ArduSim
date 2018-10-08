package scanv2.pojo;

import api.pojo.Point3D;

public class MovedMission implements Comparable<MovedMission> {

	public long id;
	public int formationPosition;
	public Point3D[] posiciones;
	
	@SuppressWarnings("unused")
	private MovedMission() {
		
	}
	public MovedMission(long id, int formationPosition, Point3D[] locations) {
		this.id=id;
		this.formationPosition = formationPosition;
		this.posiciones = locations;
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
