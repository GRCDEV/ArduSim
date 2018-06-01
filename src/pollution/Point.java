package pollution;

/**
 * Represents a 2d point on a grid.
 * @author Carlos Martínez Úbeda
 *
 */
public class Point {
	int x, y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public Point() {
	}
	
	/**
	 * @return The serialised data.
	 */
	public String toString() {
		return x + ", " + y;
	}
	
	/**
	 * Deep copy of another Point object onto this object.
	 * @param p Object to copy from.
	 */
	public void copyFrom(Point p) {
		x = p.x;
		y = p.y;
	}
	
	/**
	 * Make a deep copy of this object
	 * @return A deep copy of the object.
	 */
	public Point copy() {
		return new Point(x, y);
	}
	
	/**
	 * Calculate the distance vector to this Point from another Point.
	 * @param p The Point from which to calculate the distance.
	 * @return The distance vector as new Point object.
	 */
	public Point distVector(Point p) {
		return new Point(x - p.x, y - p.y);
	}
	
	/**
	 * Adds a Point to this Point.
	 * Useful for adding Points representing distance vectors.
	 * @param p The Point to add to this object.
	 */
	public void add(Point p) {
		x += p.x;
		y += p.y;
	}
	
	/**
	 * @return The x
	 */
	public int getX() {
		return x;
	}
	/**
	 * @param x The x to set
	 */
	public void setX(int x) {
		this.x = x;
	}
	/**
	 * @return The y
	 */
	public int getY() {
		return y;
	}
	/**
	 * @param y The y to set
	 */
	public void setY(int y) {
		this.y = y;
	}
	/**
	 * Add to x
	 * @param i Amount to add to x
	 */
	public void addX(int i) {
		this.x += i;
	}
	/**
	 * Add to y
	 * @param i Amount to add to y
	 */
	public void addY(int i) {
		this.y += i;
	}
}