package api.pojo;

import java.awt.geom.Point2D;

/** This class generates a 3 dimensions point.
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

	@Override
	public Object clone() {
		return new Point3D(this.x, this.y, this.z);
	}

	@Override
	public String toString() {
		return "(" + this.x + "," + this.y + "," + this.z + ")";
	}

}
