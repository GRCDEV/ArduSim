package api.pojo;

public class UAV2DLocation {
	
	public long id;					// UAV ID
	public UTMCoordinates location;	// UAV location in UTM coordinates

	@SuppressWarnings("unused")
	private UAV2DLocation() {}

	/** Values in meters. */
	public UAV2DLocation(long id, double x, double y) {
		this.id = id;
		this.location = new UTMCoordinates(x, y);
	}
	
	public UAV2DLocation(long id, UTMCoordinates location) {
		this.id = id;
		this.location = location;
	}
	
	public String toString() {
		return "Id: " + this.id +" location: (" + this.location.x +", " + this.location.y + ")";
	}
}
