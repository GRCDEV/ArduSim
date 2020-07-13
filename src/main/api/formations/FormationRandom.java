package main.api.formations;

import main.api.formations.helpers.FormationPoint;

import java.awt.geom.Point2D.Double;
import java.util.TreeMap;

/** Convenient formation for virtual UAVs deployment in simulations (<i>setStartingLocation</i> method of the protocol implementation). The formation numbering starts in 0, in the center UAV, approximately located at the center of the formation. The remaining UAVs are located around it, keeping the minimum distance, and trying to maintain a circular distribution around the center UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationRandom extends FlightFormation {
	
	protected FormationRandom(int numUAVs, double minDistance, Formation formation) {
		super(numUAVs, minDistance, formation);
	}

	@Override
	protected void initializeFormation() {
		this.centerUAVPosition = 0;
		
		double rangeMax = 0.3 * this.minDistance;
		double currentMax = 0;
		double d, alpha;
		
		TreeMap<java.lang.Double, Double> tree = new TreeMap<>();
		Double current;
		
		boolean accepted;
		for (int i = 0; i < this.numUAVs; i++) {
			while (this.point[i] == null) {
				d = (currentMax + rangeMax) * Math.random();
				
				alpha = 2 * Math.PI * Math.random();
				current = new Double(d * Math.cos(alpha), d * Math.sin(alpha));
				
				accepted = true;
				for (Double value: tree.values()) {
					if (value.distance(current) < this.minDistance) {
						accepted = false;
						break;
					}
				}
				if (accepted) {
					if (d > currentMax) {
						currentMax = d;
					}
					
					// Just to avoid a deadlock if the first UAV is located to close to the center of the formation
					if (i == 0) {
						currentMax = Math.max(rangeMax, this.minDistance - d);
					}
					
					tree.put(d, current);
					this.point[i] = new FormationPoint(i, current.x, current.y);
				}
			}
		}
	}

}
