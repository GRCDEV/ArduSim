package main.api.formations.helpers;

import java.util.Objects;

/** 
 * Used in compact formations for the initialization of the formation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

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
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof FormationPointHelper)) {
			return false;
		}
		FormationPointHelper p = (FormationPointHelper) obj;
		return Double.compare(this.distance, p.distance) == 0 && this.layer == p.layer
				&& this.side == p.side
				&& Double.compare(this.offsetX, p.offsetX) == 0 && Double.compare(this.offsetY, p.offsetY) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.distance, this.layer, this.side, this.offsetX, this.offsetY);
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
