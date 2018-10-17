package api.pojo;

import java.awt.geom.Point2D;

import api.Tools;
import sim.logic.SimParam;

/** This class generates objects with valid UTM and Geographic coordinates, and it is thread-safe. */

public class Location2D {
	
	private UTMCoordinates utm;	// (m) UTM coordinates location
	private GeoCoordinates geo;	// (degrees) Geographic coordinates location
	
	@SuppressWarnings("unused")
	private Location2D() {}
	
	public static Location2D NewLocation(double latitude, double longitude) {
		Location2D res = new Location2D();
		res.geo = new GeoCoordinates(latitude, longitude);
		res.utm = Tools.geoToUTM(latitude, longitude);
		return res;
	}
	
	/** It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function geoToUTM is previously used, in order to get the zone and the letter of the UTM projection. Otherwise, it returns null. */
	public static Location2D NewLocation(Double utmX, Double utmY) {
		if (SimParam.zone == -1) {
			return null;
		}
		Location2D res = new Location2D();
		res.utm = new UTMCoordinates(utmX, utmY);
		res.geo = Tools.UTMToGeo(utmX, utmY);
		return res;
	}
	
	/** Equivalent to the clone function. */
	public static Location2D copyLocation(Location2D location) {
		if (location == null) {
			return null;
		}
		Location2D res = new Location2D();
		synchronized(location) {
			res.utm = new UTMCoordinates(location.utm.x, location.utm.y);
			res.geo = new GeoCoordinates(location.geo.latitude, location.geo.longitude);
		}
		return res;
	}
	
	public synchronized UTMCoordinates getUTMLocation() {
		return new UTMCoordinates(this.utm.x, this.utm.y);
	}
	
	public synchronized GeoCoordinates getGeoLocation() {
		return new GeoCoordinates(this.geo.latitude, this.geo.longitude);
	}
	
	/** It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function geoToUTM is previously used, in order to get the zone and the letter of the UTM projection. Otherwise, it returns false. */
	public synchronized boolean updateUTM(double utmX, double utmY) {
		GeoCoordinates geo = Tools.UTMToGeo(utmX, utmY);
		if (geo == null) {
			return false;
		}
		this.utm = new UTMCoordinates(utmX, utmY);
		this.geo = geo;
		return true;
	}
	
	/** It is assumed that this function is used when at least one coordinate set is received from the UAV, or the function geoToUTM is previously used, in order to get the zone and the letter of the UTM projection. Otherwise, it returns false. */
	public synchronized boolean updateUTM(UTMCoordinates utm) {
		GeoCoordinates geo = Tools.UTMToGeo(utm.x, utm.y);
		if (geo == null) {
			return false;
		}
		this.utm = new UTMCoordinates(utm.x, utm.y);
		this.geo = geo;
		return true;
	}
	
	public synchronized void updateGeo(double latitude, double longitude) {
		this.geo = new GeoCoordinates(latitude, longitude);
		this.utm = Tools.geoToUTM(latitude, longitude);
	}
	
	public synchronized void updateGeo(GeoCoordinates geo) {
		this.geo = new GeoCoordinates(geo.latitude, geo.longitude);
		this.utm = Tools.geoToUTM(geo);
	}
	
	public synchronized double distance(Location2D location) {
		return this.utm.distance(location.utm);
	}
	
	public synchronized double distance(Point2D.Double location) {
		return this.utm.distance(location);
	}
	
	public synchronized double distance(UTMCoordinates location) {
		return this.utm.distance(location);
	}
	
	public synchronized double distance(double utmX, double utmY) {
		return this.utm.distance(utmX, utmY);
	}
	
}
