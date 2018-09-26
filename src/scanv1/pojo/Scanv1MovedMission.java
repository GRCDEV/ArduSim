package scanv1.pojo;

import api.pojo.Point3D;

public class Scanv1MovedMission {

	public long id;
	public Point3D[] posiciones;
	
	@SuppressWarnings("unused")
	private Scanv1MovedMission() {
		
	}
	public Scanv1MovedMission(long id, Point3D[] locations) {
		this.id=id;
		this.posiciones = locations;
	}
	
}
