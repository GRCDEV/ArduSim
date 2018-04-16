package swarmprot.logic;

import java.awt.geom.Point2D.Double;

public class uavPosition extends Double {

	private static final long serialVersionUID = 1L;
	public long id;
	public double heading;

	public uavPosition() {
		super();
	}

	/** Values in meters. */
	public uavPosition(double x, double y, long id, double heading) {
		super(x, y);
		this.id = id;
		this.heading = heading;
	}
	public String toString() {
		String s = "Id: "+this.id+" Heading: "+heading+" X: "+x+" Y: "+y;
		return s;
	}

}
