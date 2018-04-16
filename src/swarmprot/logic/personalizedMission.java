package swarmprot.logic;

import api.pojo.Point3D;

public class personalizedMission {

	public long id;
	private int pos;
	public Point3D[] posiciones;
	
	@SuppressWarnings("unused")
	private personalizedMission() {
		
	}
	public personalizedMission(long id, int size) {
		posiciones = new Point3D[size];
		pos=0;
		this.id=id;
		
	}
	
	public void addPoint(Point3D point) {
		posiciones[pos] = point;
		pos++;
	}
}
