package api.pojo;

import java.awt.geom.Point2D;

/** This class generates a point in UTM coordinates in meters.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class UTMCoordinates extends Point2D.Double implements Cloneable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private UTMCoordinates() {
		super();
	}

	public UTMCoordinates(double x, double y) {
		super(x, y);
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
	public Object clone() {
		return new UTMCoordinates(this.x, this.y);
	}

	@Override
	public String toString() {
		return "(" + this.x + ", " + this.y + ")";
	}
	
	

}
