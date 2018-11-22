package mbcap.pojo;

import api.pojo.UTMCoordinates;

/** This class generates objects with UAV location and the instant when the data was retrieved.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class ErrorPoint extends UTMCoordinates {

	private static final long serialVersionUID = 1L;
	public double time;
	
	public ErrorPoint(double time, double x, double y) {
		super(x, y);
		this.time = time;
	}

}
