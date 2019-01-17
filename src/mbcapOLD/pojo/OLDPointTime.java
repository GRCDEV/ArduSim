package mbcapOLD.pojo;

import java.awt.geom.Point2D;

/** This class generates objects with UAV location and the instant when the data was retrieved. */

public class OLDPointTime extends Point2D.Double {

	private static final long serialVersionUID = 1L;
	public long time;
	
	public OLDPointTime(long time, double x, double y) {
		super(x, y);
		this.time = time;
	}

}
