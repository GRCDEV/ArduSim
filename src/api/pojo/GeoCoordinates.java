package api.pojo;

/** This class generates a point in Geographic coordinates.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class GeoCoordinates {

	public double latitude;
	public double longitude;

	@SuppressWarnings("unused")
	private GeoCoordinates() {
	}

	public GeoCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public String toString() {
		return "(lat=" + this.latitude + ", lon=" + this.longitude + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GeoCoordinates)) {
			return false;
		}
		GeoCoordinates o = (GeoCoordinates) obj;
		return o.latitude == this.latitude && o.longitude == this.longitude;
	}


}
