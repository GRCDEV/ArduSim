package mbcap.pojo;

import java.awt.geom.Point2D;

/** This class generates objects with UAV location and the instant when the data was retrieved. */

public class PointTime extends Point2D.Double {

	private static final long serialVersionUID = 1L;
	public long time;
	
	public PointTime(long time, double x, double y) {
		super(x, y);
		this.time = time;
	}

}
