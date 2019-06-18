package api.pojo.location;

import java.util.Objects;

import main.sim.logic.SimParam;

/** This class generates points in Geographic coordinates.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Location2DGeo {

	/** (degrees) Latitude. */
	public double latitude;
	/** (degrees) Longitude. */
	public double longitude;

	protected Location2DGeo() {}

	/**
	 * Create a location in geographic coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 */
	public Location2DGeo(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/**
	 * Make a deep copy of a location.
	 * @param location Location to copy.
	 */
	public Location2DGeo(Location2DGeo location) {
		this.latitude = location.latitude;
		this.longitude = location.longitude;
	}

	@Override
	public String toString() {
		return "(lat=" + this.latitude + ", lon=" + this.longitude + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof Location2DGeo)) {
			return false;
		}
		
		Location2DGeo o = (Location2DGeo) obj;
		return Double.compare(this.latitude, o.latitude) == 0 && Double.compare(this.longitude, o.longitude) == 0;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.latitude, this.longitude);
	}

	/**
	 * Transform Geographic coordinates into UTM coordinates.
	 * @param latitude (degrees).
	 * @param longitude (degrees).
	 * @return Coordinates in UTM coordinate system.
	 */
	public static Location2DUTM getUTM(double latitude, double longitude) {
		double x;
		double y;
		int Zone = (int) Math.floor(longitude / 6 + 31);
		char Letter;
		if (latitude < -72)		Letter = 'C';
		else if (latitude < -64)	Letter = 'D';
		else if (latitude < -56)	Letter = 'E';
		else if (latitude < -48)	Letter = 'F';
		else if (latitude < -40)	Letter = 'G';
		else if (latitude < -32)	Letter = 'H';
		else if (latitude < -24)	Letter = 'J';
		else if (latitude < -16)	Letter = 'K';
		else if (latitude < -8)	Letter = 'L';
		else if (latitude < 0)	Letter = 'M';
		else if (latitude < 8)	Letter = 'N';
		else if (latitude < 16)	Letter = 'P';
		else if (latitude < 24)	Letter = 'Q';
		else if (latitude < 32)	Letter = 'R';
		else if (latitude < 40)	Letter = 'S';
		else if (latitude < 48)	Letter = 'T';
		else if (latitude < 56)	Letter = 'U';
		else if (latitude < 64)	Letter = 'V';
		else if (latitude < 72)	Letter = 'W';
		else				Letter = 'X';
		x = 0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin(longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2) / 3) + 500000;
		x = Math.round(x * 100) * 0.01;
		y = (Math.atan(Math.tan(latitude * Math.PI / 180) / Math.cos((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) - latitude * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(latitude * Math.PI / 180) * Math.sin((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(latitude * Math.PI / 180) * Math.sin((longitude * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (latitude * Math.PI / 180 - 0.005054622556 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (latitude * Math.PI / 180 + Math.sin(2 * latitude * Math.PI / 180) / 2) + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 4 + Math.sin(2 * latitude * Math.PI / 180) * Math.pow(Math.cos(latitude * Math.PI / 180), 2) * Math.pow(Math.cos(latitude * Math.PI / 180), 2)) / 3);
		if (Letter < 'M')
			y = y + 10000000;
		y = Math.round(y * 100) * 0.01;
	
		SimParam.zone = Zone;
		SimParam.letter = Letter;
		
		return new Location2DUTM(x, y);
	}
	
	/**
	 * Get the UTM coordinates equivalent to this geographic coordinates.
	 * @return Coordinates in UTM coordinate system.
	 */
	public Location2DUTM getUTM() {
		return Location2DGeo.getUTM(this.latitude, this.longitude);
	}

}
