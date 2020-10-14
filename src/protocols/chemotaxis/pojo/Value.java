package protocols.chemotaxis.pojo;

/**
 * Represents a value in a 2d grid.
 * @author Carlos Martínez Úbeda
 *
 */
public class Value {
	int x, y;
	double v;
	
	public Value(int x, int y, double v) {
		this.x = x;
		this.y = y;
		this.v = v;
	}

	/**
	 * @return The serialised data.
	 */
	@Override
	public String toString() {
		return "[" + x + ", " + y + "] = " + v;
	}

	public double distance(Point p) {
		int a = this.x - p.x;
		int b = this.y - p.y;
		return Math.sqrt(a*a + b*b);
	}
	
	/**
	 * Adds a Point to this Point.
	 * Useful for adding Points representing distance vectors.
	 * @param p The Point to add to this object.
	 * @return Itself to allow chaining.
	 */
	public Value add(Point p) {
		x += p.x;
		y += p.y;
		return this;
	}
	
	/**
	 * Adds to the x and y components of the Point.
	 * @param x The x to add.
	 * @param y The y to add.
	 * @return value
	 */
	public Value add(int x, int y) {
		this.x += x;
		this.y += y;
		return this;
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
	 * @return The v
	 */
	public double getV() {
		return v;
	}

}