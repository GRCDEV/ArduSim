package api.pojo;

import java.util.Arrays;

/** This class allows the developer to get access to the last few locations of a UAV.
 * <p>The locations stored time increases with the position on the array. */

public class LastPositions {
	
	private Point3D[] pos;
	
	@SuppressWarnings("unused")
	private LastPositions() {}
	
	/** The number of last positions to store never must be less than 2. */
	public LastPositions(int size) {
		this.pos = new Point3D[size];
	}
	
	/** Stores the last known positions of the UAV. Data is feeded from the end of the array an shifts towards the beginning. */
	public synchronized void updateLastPositions(Point3D p) {
		if (pos[pos.length - 1] == null) {
			pos[pos.length - 1] = p;
		} else if (!pos[pos.length - 1].equals(p)) {
			for (int i = 1; i < pos.length; i++) {
				pos[i - 1] = pos[i];
			}
			pos[pos.length - 1] = p;
		}
	}
	
	/** Retrieves the last known positions of the UAV. */
	public synchronized Point3D[] getLastPositions() {
		return Arrays.copyOf(this.pos, this.pos.length);
	}
	
	

}
