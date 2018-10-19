package mbcap.pojo;

import api.pojo.UTMCoordinates;

/** This class generates objects with UAV location and the instant when the data was retrieved. */

public class PointTime extends UTMCoordinates {

	public long time;
	
	public PointTime(long time, double x, double y) {
		super(x, y);
		this.time = time;
	}

}
