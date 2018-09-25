package scanv2.pojo;

import java.awt.geom.Point2D.Double;

public class uav2DPosition extends Double {

	private static final long serialVersionUID = 1L;
	public long id;
	public double heading;

	public uav2DPosition() {
		super();
	}

	/** Values in meters. */
	public uav2DPosition(double x, double y, long id, double heading) {
		super(x, y);
		this.id = id;
		this.heading = heading;
	}
	public String toString() {
		String s = "Id: "+this.id+" Heading: "+heading+" X: "+x+" Y: "+y;
		return s;
	}

}
