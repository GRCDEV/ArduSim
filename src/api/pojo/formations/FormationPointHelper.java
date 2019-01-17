package api.pojo.formations;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class FormationPointHelper implements Comparable<FormationPointHelper> {
	
	public double distance;
	public int layer, side;
	public double offsetX, offsetY;
	
	@SuppressWarnings("unused")
	private FormationPointHelper() {}
	
	public FormationPointHelper(double distance, int layer, int side, double offsetX, double offsetY) {
		this.distance = distance;
		this.layer = layer;
		this.side = side;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
	

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FormationPointHelper)) {
			return false;
		}
		FormationPointHelper p = (FormationPointHelper) obj;
		return this.distance == p.distance && this.layer == p.layer
				&& this.side == p.side && this.offsetX == p.offsetX && this.offsetY == p.offsetY;
	}

	@Override
	public int compareTo(FormationPointHelper o) {
		double res = this.distance - o.distance;
		if (res != 0.0) {
			if (res < 0) {
				return -1;
			} else {
				return 1;
			}
		}
		// At the same distance
		int res2 = this.layer - o.layer;
		if (res2 != 0) {
			return res2;
		}
		// Even in the same layer
		return this.side - o.side;
	}
	
	
	
}
