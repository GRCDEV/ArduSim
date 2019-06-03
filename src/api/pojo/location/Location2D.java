package api.pojo.location;

import java.awt.geom.Point2D;

import main.api.ArduSimNotReadyException;

/** This class generates points with valid UTM and Geographic coordinates, and it is thread-safe.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Location2D {
	
	private Location2DUTM utm;	// (m) UTM coordinates location
	private Location2DGeo geo;	// (degrees) Geographic coordinates location
	
	@SuppressWarnings("unused")
	private Location2D() {}
	
	/**
	 * Make a deep copy of a location.
	 * @param location Location to copy.
	 */
	public Location2D(Location2D location) {
		this.utm = new Location2DUTM(location.utm);
		this.geo = new Location2DGeo(location.geo);
	}
	
	/**
	 * Get a location from known Geographic coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 */
	public Location2D(double latitude, double longitude) {
		this.geo = new Location2DGeo(latitude, longitude);
		this.utm = this.geo.getUTM();
	}

	/**
	 * Create a location from known UTM coordinates.
	 * @param utmX (m) X coordinate.
	 * @param utmY (m) Y coordinate.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>Location2DGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public Location2D(Double utmX, Double utmY) throws ArduSimNotReadyException {
		this.utm = new Location2DUTM(utmX, utmY);
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Create a location from known UTM coordinates.
	 * @param location UTM coordinates
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>Location2DGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public Location2D(Location2DUTM location) throws ArduSimNotReadyException {
		this.utm = new Location2DUTM(location);
		this.geo = this.utm.getGeo();
	}
	
	
	/**
	 * Get UTM coordinates.
	 * @return The current UTM coordinates.
	 */
	public synchronized Location2DUTM getUTMLocation() {
		return new Location2DUTM(this.utm.x, this.utm.y);
	}
	
	/**
	 * Get the geographic coordinates.
	 * @return The current Geographic coordinates.
	 */
	public synchronized Location2DGeo getGeoLocation() {
		return new Location2DGeo(this.geo.latitude, this.geo.longitude);
	}
	
	/**
	 * Update this object with a new UTM location
	 * @param utmX (m) East.
	 * @param utmY (m) North.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>Location2DGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM(double utmX, double utmY) throws ArduSimNotReadyException {
		this.utm = new Location2DUTM(utmX, utmY);
		this.geo = this.utm.getGeo();
	}
	
	/**
	 * Update this object with a new UTM location
	 * @param utm UTM location.
	 * @throws ArduSimNotReadyException Thrown if the UAV is not located already. If you use <i>Location2DGeo.getUTM</i> function at least once, the exception will also not be thrown.
	 */
	public synchronized void updateUTM(Location2DUTM utm) throws ArduSimNotReadyException {
		this.updateUTM(utm.x, utm.y);
	}
	
	/**
	 * Update this object with a new Geographic location.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 */
	public synchronized void updateGeo(double latitude, double longitude) {
		this.geo = new Location2DGeo(latitude, longitude);
		this.utm = this.geo.getUTM();
	}
	
	/**
	 * Update this object with a new Geographic location.
	 * @param geo Geographic coordinates.
	 */
	public synchronized void updateGeo(Location2DGeo geo) {
		this.geo = new Location2DGeo(geo);
		this.utm = this.geo.getUTM();
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
	 * @param utmX (m) UTM east of the second point.
	 * @param utmY (m) UTM north of the second point.
	 * @return (m) Distance between this and the provided location.
	 */
	public synchronized double distance(double utmX, double utmY) {
		return this.utm.distance(utmX, utmY);
	}

	@Override
	public synchronized String toString() {
		return "UTM: " + this.utm.toString() + " - Geo: " + this.geo.toString();
	}
	
	
	
}
