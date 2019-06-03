package main.api.formations;

import api.API;
import main.api.ValidationTools;
import main.api.formations.helpers.FormationPoint;

/** The formation numbering starts in 0, with the UAV in the center of the circle.
 * The remaining UAVs are numbered beginning in the east with 1, and moving counterclockwise.
 * The UAVs keep at least the minDistance with the former and the later in the formation, and with the center UAV.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationCircular extends FlightFormation {

	protected FormationCircular(int numUAVs, double minDistance) {
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
			ValidationTools validationTools = API.getValidationTools();
			for (int i = 1; i < this.numUAVs; i++) {
				x = validationTools.roundDouble(radius * Math.cos((i -1) * 2 * Math.PI / (this.numUAVs - 1)), 6);
				y = validationTools.roundDouble(radius * Math.sin((i -1) * 2 * Math.PI / (this.numUAVs - 1)), 6);
				this.point[i] = new FormationPoint(i, x, y);
			}
		}
	}

}
