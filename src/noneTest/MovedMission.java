package noneTest;

import api.pojo.Point3D;

public class MovedMission {

	public long id;
	public Point3D[] posiciones;
	
	@SuppressWarnings("unused")
	private MovedMission() {
		
	}
	public MovedMission(long id, Point3D[] locations) {
		this.id=id;
		this.posiciones = locations;
	}
	
}
