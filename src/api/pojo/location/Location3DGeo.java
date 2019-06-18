package api.pojo.location;

import java.util.Objects;

public class Location3DGeo extends Location2DGeo {
	
	public double altitude; // (m)
	
	protected Location3DGeo() {
		super();
	}
	
	/**
	 * Create a location in geographic coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @param altitude (m).
	 */
	public Location3DGeo(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	/**
	 * Create a location in geographic coordinates.
	 * @param location Geographic coordinates.
	 * @param altitude (m).
	 */
	public Location3DGeo(Location2DGeo location, double altitude) {
		this.latitude = location.latitude;
		this.longitude = location.longitude;
		this.altitude = altitude;
	}
	
	/**
	 * Make a deep copy of a location.
	 * @param location Location to copy.
	 */
	public Location3DGeo(Location3DGeo location) {
		this.latitude = location.latitude;
		this.longitude = location.longitude;
		this.altitude = location.altitude;
	}
	
	/** Two 3D locations are equal if their three coordinates are the same (latitude, longitude, altitude). */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof Location3DUTM)) {
			return false;
		}
		Location3DGeo location = (Location3DGeo)obj;
		return java.lang.Double.compare(this.latitude, location.latitude) == 0
				&& java.lang.Double.compare(this.longitude, location.longitude) == 0
				&& java.lang.Double.compare(this.altitude, location.altitude) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.latitude, this.longitude, this.altitude);
	}

	@Override
	public String toString() {
		return "(" + this.latitude + "," + this.longitude + "," + this.altitude + ")";
	}
	
	/**
	 * Transform Geographic 3D coordinates into UTM 3D coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @param altitude (m).
	 * @return 3D coordinates in UTM coordinate system.
	 */
	public static Location3DUTM getUTM(double latitude, double longitude, double altitude) {
		Location2DUTM location = Location2DGeo.getUTM(latitude, longitude);
		return new Location3DUTM(location.x, location.y, altitude);
	}
	
	/**
	 * Get the UTM 3D coordinates equivalent to this geographic 3D coordinates.
	 * @return 3D coordinates in UTM coordinate system.
	 */
	public Location3DUTM getUTM3D() {
		Location2DUTM location = Location2DGeo.getUTM(this.latitude, this.longitude);
		return new Location3DUTM(location.x, location.y, altitude);
	}
	
}
