package api.pojo.formations;

import api.Tools;

/** The formation numbering starts in 0, and increases from left to the right. */

public class LinearFlightFormation extends FlightFormation {

	protected LinearFlightFormation(int numUAVs, double minDistance) {
		super(numUAVs, minDistance);
	}

	@Override
	protected void initializeFormation() {
		this.centerUAV = this.numUAVs / 2;
		
		double x;
		for (int i = 0; i < this.numUAVs; i++) {
			x = Tools.round((i - this.centerUAV) * this.minDistance, 6);
			this.point[i] = new FormationPoint(i, x, 0);
		}
	}

}
