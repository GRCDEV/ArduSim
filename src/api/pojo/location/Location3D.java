package api.pojo.location;

import java.awt.geom.Point2D;
import java.util.Objects;

import main.api.ArduSimNotReadyException;

/** This class generates points with valid UTM and Geographic coordinates, and it is thread-safe.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Location3D {
	
	protected Location3DUTM utm;	// (m) UTM coordinates location
	protected Location2DGeo geo;	// (degrees) Geographic coordinates location
	
	@SuppressWarnings("unused")
	private Location3D() {}
	
	/**
	 * Make a deep copy of a location.
	 * @param location Location to copy.
	 */
	public Location3D(Location3D location) {
		this.utm = new Location3DUTM(location.utm);
		this.geo = new Location2DGeo(location.geo);
	}
	
	/**
	 * Create a location from known Geographic coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @param altitude (m).
	 */
	public Location3D(double latitude, double longitude, double altitude) {
		this.geo = new Location2DGeo(latitude, longitude);
		this.utm = new Location3DUTM(this.geo.getUTM(), altitude);
	}
	
	/**
	 * Create a location from known Geographic coordinates.
	 * @param location Geographic coordinates.
	 * @param altitude (m).
	 */
	public Location3D(Location2DGeo location, double altitude) {
		this.geo = new Location2DGeo(location);
		this.utm = new Location3DUTM(this.geo.getUTM(), altitude);
	}

	/**
	 * Create a location from known UTM coordinates.
	 * @param utmX (m) X coordinate.
	 * @param utmY (m) Y coordinate.
	 * @param altitude (m).
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public Location3D(Double utmX, Double utmY, double altitude) throws ArduSimNotReadyException {
		this.utm = new Location3DUTM(utmX, utmY, altitude);
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Create a location from known UTM coordinates.
	 * @param location UTM coordinates.
	 * @param altitude (m).
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public Location3D(Location2DUTM location, double altitude) throws ArduSimNotReadyException {
		this.utm = new Location3DUTM(location, altitude);
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Create a location from known Geographic coordinates.
	 * @param location Geographic coordinates.
	 */
	public Location3D(Location3DGeo location) {
		this.geo = new Location2DGeo(location.latitude, location.longitude);
		this.utm = location.getUTM3D();
	}
	
	/**
	 * Create a location from known UTM 3D coordinates.
	 * @param location UTM 3D coordinates.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public Location3D(Location3DUTM location) throws ArduSimNotReadyException {
		this.utm = new Location3DUTM(location);
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Create a 3D location from a 2D location.
	 * @param location
	 * @param altitude
	 */
	public Location3D(Location2D location, double altitude) {
		this.utm = new Location3DUTM(location.utm.x, location.utm.y, altitude);
		this.geo = new Location2DGeo(location.geo.latitude, location.geo.longitude);
	}
	
	/**
	 * Get this location altitude.
	 * @return (m) altitude.
	 */
	public synchronized double getAltitude() {
		return this.utm.z;
	}
	
	/**
	 * Get this location latitude.
	 * @return (degrees).
	 */
	public synchronized double getLatitude() {
		return this.geo.latitude;
	}
	
	/**
	 * Get this location longitude.
	 * @return (degrees).
	 */
	public synchronized double getLongitude() {
		return this.geo.longitude;
	}
	
	/**
	 * Get this location easting.
	 * @return (m) Easting.
	 */
	public synchronized double getX() {
		return this.utm.x;
	}
	
	/**
	 * Get this location northing.
	 * @return (m) Northing.
	 */
	public synchronized double getY() {
		return this.utm.y;
	}
	
	/**
	 * Get the 2D geographic coordinates.
	 * @return The current Geographic coordinates.
	 */
	public synchronized Location2DGeo getGeoLocation() {
		return new Location2DGeo(this.geo.latitude, this.geo.longitude);
	}
	
	/**
	 * Get the 3D geographic coordinates.
	 * @return The current Geographic coordinates.
	 */
	public synchronized Location3DGeo getGeoLocation3D() {
		return new Location3DGeo(this.geo.latitude, this.geo.longitude, this.utm.z);
	}
	
	/**
	 * Get UTM 2D coordinates.
	 * @return The current UTM coordinates.
	 */
	public synchronized Location2DUTM getUTMLocation() {
		return new Location2DUTM(this.utm.x, this.utm.y);
	}
	
	/**
	 * Get UTM 3D coordinates.
	 * @return The current UTM coordinates.
	 */
	public synchronized Location3DUTM getUTMLocation3D() {
		return new Location3DUTM(this.utm.x, this.utm.y, this.utm.z);
	}
	
	/**
	 * Update this object with a new 2D UTM location
	 * @param utmX (m) East.
	 * @param utmY (m) North.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM2D(double utmX, double utmY) throws ArduSimNotReadyException {
		this.utm.x = utmX;
		this.utm.y = utmY;
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new 2D UTM location
	 * @param utm UTM location.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM2D(Location2DUTM utm) throws ArduSimNotReadyException {
		this.utm.x = utm.x;
		this.utm.y = utm.y;
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new 3D UTM location
	 * @param utmX (m) East.
	 * @param utmY (m) North.
	 * @param altitude (m).
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM3D(double utmX, double utmY, double altitude) throws ArduSimNotReadyException {
		this.utm.x = utmX;
		this.utm.y = utmY;
		this.utm.z = altitude;
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new 3D UTM location
	 * @param utm UTM location.
	 * @param altitude (m).
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM3D(Location2DUTM utm, double altitude) throws ArduSimNotReadyException {
		this.utm.x = utm.x;
		this.utm.y = utm.y;
		this.utm.z = altitude;
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new 3D UTM location
	 * @param utm UTM 3D location.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>LocationXGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM3D(Location3DUTM utm) throws ArduSimNotReadyException {
		this.utm.x = utm.x;
		this.utm.y = utm.y;
		this.utm.z = utm.z;
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new 2D Geographic location.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 */
	public synchronized void updateGeo(double latitude, double longitude) {
		this.geo.latitude = latitude;
		this.geo.longitude = longitude;
		Location2DUTM value = this.geo.getUTM();
		this.utm.x = value.x;
		this.utm.y = value.y;
	}
	
	/**
	 * Update this object with a new 2D Geographic location.
	 * @param geo Geographic coordinates.
	 */
	public synchronized void updateGeo(Location2DGeo geo) {
		this.geo.latitude = geo.latitude;
		this.geo.longitude = geo.longitude;
		Location2DUTM value = this.geo.getUTM();
		this.utm.x = value.x;
		this.utm.y = value.y;
	}
	
	/**
	 * Update this object with a new 3D Geographic location.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @param altitude (m).
	 */
	public synchronized void updateGeo(double latitude, double longitude, double altitude) {
		this.geo.latitude = latitude;
		this.geo.longitude = longitude;
		Location2DUTM value = this.geo.getUTM();
		this.utm.x = value.x;
		this.utm.y = value.y;
		this.utm.z = altitude;
	}
	
	/**
	 * Update this object with a new 3D Geographic location.
	 * @param geo Geographic coordinates.
	 * @param altitude (m).
	 */
	public synchronized void updateGeo(Location2DGeo geo, double altitude) {
		this.geo.latitude = geo.latitude;
		this.geo.longitude = geo.longitude;
		Location2DUTM value = this.geo.getUTM();
		this.utm.x = value.x;
		this.utm.y = value.y;
		this.utm.z = altitude;
	}
	
	/**
	 * Update this object with a new 3D Geographic location.
	 * @param geo Geographic coordinates.
	 */
	public synchronized void updateGeo(Location3DGeo geo) {
		this.geo.latitude = geo.latitude;
		this.geo.longitude = geo.longitude;
		Location2DUTM value = this.geo.getUTM();
		this.utm.x = value.x;
		this.utm.y = value.y;
		this.utm.z = geo.altitude;
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param location Location of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(Location2D location) {
		return this.utm.distance(location.utm);
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param location Location of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(Location3D location) {
		return this.utm.distance(location.utm);
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param location UTM coordinate of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(Point2D.Double location) {
		return this.utm.distance(location);
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param location Location of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(Location2DUTM location) {
		return this.utm.distance(location);
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param location Location of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(Location3DUTM location) {
		return this.utm.distance(location);
	}
	
	/**
	 * Get the horizontal distance to another location.
	 * @param utmX (m) UTM east of the second point.
	 * @param utmY (m) UTM north of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(double utmX, double utmY) {
		return this.utm.distance(utmX, utmY);
	}
	
	
	
	
	/**
	 * Get the 3D distance to another location.
	 * @param x (m) Easting.
	 * @param y (m) Northing.
	 * @param z (m) Altitude.
	 * @return (m) 3D distance between this and the provided location.
	 */
	public double distance3D(double x, double y, double z) {
		return Math.sqrt(Math.pow(this.utm.x - x, 2) + Math.pow(this.utm.y - y, 2) + Math.pow(this.utm.z - z, 2));
	}
	
	/**
	 * Get the 3D distance to another location.
	 * @param location 3D location
	 * @return (m) 3D distance between this and the provided location.
	 */
	public double distance3D(Location3DUTM location) {
		return Math.sqrt(Math.pow(this.utm.x - location.x, 2) + Math.pow(this.utm.y - location.y, 2) + Math.pow(this.utm.z - location.z, 2));
	}
	
	/**
	 * Get the 3D distance to another location.
	 * @param location 3D location
	 * @return (m) 3D distance between this and the provided location.
	 */
	public double distance3D(Location3D location) {
		return Math.sqrt(Math.pow(this.utm.x - location.utm.x, 2) + Math.pow(this.utm.y - location.utm.y, 2) + Math.pow(this.utm.z - location.utm.z, 2));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
	
		if (obj == null || !(obj instanceof Location3D)) {
			return false;
		}
		Location3D location = (Location3D)obj;
		return this.geo.equals(location.geo) && this.utm.equals(location.utm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.geo, this.utm);
	}

	@Override
	public synchronized String toString() {
		return "UTM: " + this.utm.toString() + " - Geo: " + this.geo.toString();
	}
	
	
	
}
