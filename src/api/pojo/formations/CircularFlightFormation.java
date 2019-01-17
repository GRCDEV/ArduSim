package api.pojo.formations;

import api.Tools;

/** The formation numbering starts in 0, with the UAV in the center of the circle.
 * The remaining UAVs are numbered beginning in the east with 1, and moving counterclockwise.
 * The UAVs keep at least the minDistance with the former and the later in the formation, and with the center UAV.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class CircularFlightFormation extends FlightFormation {

	protected CircularFlightFormation(int numUAVs, double minDistance) {
		super(numUAVs, minDistance);
	}

	@Override
	protected void initializeFormation() {
		this.centerUAV = 0;
		this.point[this.centerUAV] = new FormationPoint(this.centerUAV, 0, 0);
		if (this.numUAVs > 1) {
			double radius;
			if (this.numUAVs <= 7) {
				radius = this.minDistance;
			} else {
				double side  = this.minDistance;
				radius = side / (2 * Math.sin(Math.PI / (this.numUAVs - 1)));
			}
			double x, y;
			for (int i = 1; i < this.numUAVs; i++) {
				x = Tools.round(radius * Math.cos((i -1) * 2 * Math.PI / (this.numUAVs - 1)), 6);
				y = Tools.round(radius * Math.sin((i -1) * 2 * Math.PI / (this.numUAVs - 1)), 6);
				this.point[i] = new FormationPoint(i, x, y);
			}
		}
	}

}
