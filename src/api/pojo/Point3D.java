package api.pojo;

import java.awt.geom.Point2D;

/** This class generates a 3 dimensional point.
 * <p>The distance between two points does not include the third coordinate. */

public class Point3D extends Point2D.Double {
	
	private static final long serialVersionUID = 1L;
	public double z; // (m)

	public Point3D() {
		super();
	}

	/** Values in meters. */
	public Point3D(double x, double y, double z) {
		super(x, y);
		this.z = z;
	}
	
	/** Two Point3D objects are equal if their three coordinates are the same (x, y, z). */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Point3D) {
			return super.equals(obj) && ((Point3D)obj).z == this.z;
		} else {
			return false;
		}
	}

	@Override
	public Object clone() {
		return new Point3D(this.x, this.y, this.z);
	}

	@Override
	public String toString() {
		return "(" + this.x + "," + this.y + "," + this.z + ")";
	}

}
