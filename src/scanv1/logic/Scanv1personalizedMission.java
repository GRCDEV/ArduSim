package scanv1.logic;

import api.pojo.Point3D;

public class Scanv1personalizedMission {

	public long id;
	private int pos;
	public Point3D[] posiciones;
	
	@SuppressWarnings("unused")
	private Scanv1personalizedMission() {
		
	}
	public Scanv1personalizedMission(long id, int size) {
		posiciones = new Point3D[size];
		pos=0;
		this.id=id;
		
	}
	
	public void addPoint(Point3D point) {
		posiciones[pos] = point;
		pos++;
	}
}
