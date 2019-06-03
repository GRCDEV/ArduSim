package api.pojo.location;

import java.util.Objects;

/** This class generates a 3 dimensional point.
 * <p>The distance between two points does not include the third coordinate if you do not use <i>distance3D</i> methods.</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Location3DUTM extends Location2DUTM {
	
	private static final long serialVersionUID = 1L;
	/** The z coordinate of this 3D location. */
	public double z; // (m)

	protected Location3DUTM() {
		super();
	}

	/**
	 * Creates a 3D location given UTM coordinates.
	 * @param x (m) Easting.
	 * @param y (m) Northing.
	 * @param z (m) Altitude.
	 */
	public Location3DUTM(double x, double y, double z) {
		super(x, y);
		this.z = z;
	}
	
	/**
	 * Make a deep copy of a location.
	 * @param location Location to copy.
	 */
	public Location3DUTM(Location3DUTM location) {
		this.x = location.x;
		this.y = location.y;
		this.z = location.z;
	}
	
	/**
	 * Get the 3D distance to another location.
	 * @param x (m) Easting.
	 * @param y (m) Northing.
	 * @param z (m) Altitude.
	 * @return (m) 3D distance between this and the provided location.
	 */
	public double distance3D(double x, double y, double z) {
		return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2) + Math.pow(this.z - z, 2));
	}
	
	/**
	 * Get the 3D distance to another location.
	 * @param location 3D location
	 * @return (m) 3D distance between this and the provided location.
	 */
	public double distance3D(Location3DUTM location) {
		return Math.sqrt(Math.pow(this.x - location.x, 2) + Math.pow(this.y - location.y, 2) + Math.pow(this.z - location.z, 2));
	}
	
	/** Two Point3D objects are equal if their three coordinates are the same (x, y, z). */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof Location3DUTM)) {
			return false;
		}
		Location3DUTM location = (Location3DUTM)obj;
		return java.lang.Double.compare(this.x, location.x) == 0 && java.lang.Double.compare(this.y, location.y) == 0
				&& java.lang.Double.compare(this.y, location.y) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.x, this.y, this.z);
	}

	@Override
	public String toString() {
		return "(" + this.x + "," + this.y + "," + this.z + ")";
	}

}
