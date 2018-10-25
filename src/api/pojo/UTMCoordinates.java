package api.pojo;

import java.awt.geom.Point2D;

/** This class generates a point in UTM coordinates in meters. */

public class UTMCoordinates implements Cloneable {

	public double x;
	public double y;

	@SuppressWarnings("unused")
	private UTMCoordinates() {}

	public UTMCoordinates(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distance(UTMCoordinates location) {
		return Math.sqrt(Math.pow(this.x - location.x, 2) + Math.pow(this.y - location.y, 2));
	}

	public double distance(double x, double y) {
		return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
	}

	public double distance(Point2D.Double location) {
		return Math.sqrt(Math.pow(this.x - location.x, 2) + Math.pow(this.y - location.y, 2));
	}
	
	public double distance(Location2D location) {
		return this.distance(location.getUTMLocation());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof UTMCoordinates)) {
			return false;
		}
		UTMCoordinates location = (UTMCoordinates)obj;
		return this.x == location.x && this.y == location.y;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new UTMCoordinates(this.x, this.y);
	}

	@Override
	public String toString() {
		return "(" + this.x + ", " + this.y + ")";
	}
	
	

}
