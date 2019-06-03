package main.api.formations;

import api.API;
import main.api.ValidationTools;
import main.api.formations.helpers.FormationPoint;

/** The formation numbering starts in 0, and increases from left to the right.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationLinear extends FlightFormation {

	protected FormationLinear(int numUAVs, double minDistance) {
		super(numUAVs, minDistance);
	}

	@Override
	protected void initializeFormation() {
		this.centerUAV = this.numUAVs / 2;
		
		double x;
		ValidationTools validationTools = API.getValidationTools();
		for (int i = 0; i < this.numUAVs; i++) {
			x = validationTools.roundDouble((i - this.centerUAV) * this.minDistance, 6);
			this.point[i] = new FormationPoint(i, x, 0);
		}
	}

}
