package scanv1.logic;

import java.awt.geom.Point2D.Double;

public class Scanv1uavPosition extends Double {

	private static final long serialVersionUID = 1L;
	public long id;
	public double heading;

	public Scanv1uavPosition() {
		super();
	}

	/** Values in meters. */
	public Scanv1uavPosition(double x, double y, long id, double heading) {
		super(x, y);
		this.id = id;
		this.heading = heading;
	}
	public String toString() {
		String s = "Id: "+this.id+" Heading: "+heading+" X: "+x+" Y: "+y;
		return s;
	}

}
