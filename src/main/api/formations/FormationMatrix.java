package main.api.formations;

import main.api.formations.helpers.FormationPoint;

/** The formation numbering starts in 0 an increases from left to right, and down to up.
 * The selection of the center UAV tries to minimize the distance to the further UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationMatrix extends FlightFormation {

	protected FormationMatrix(int numUAVs, double minDistance, Formation formation) {
		super(numUAVs, minDistance, formation);
	}

	@Override
	protected void initializeFormation() {
		int cols = (int)Math.ceil(Math.sqrt(this.numUAVs));
		int rows = (int)Math.ceil(this.numUAVs/cols);
		int prevColsCenter = (cols - 1) / 2;
		int prevRowsCenter = rows / 2;
		this.centerUAVPosition = prevRowsCenter * cols + prevColsCenter;
		
		int prevRows, prevCols;
		double x, y;
		for (int i = 0; i < this.numUAVs; i++) {
			prevRows =  i / cols;
			prevCols = i - prevRows * cols;
			x = (prevCols - prevColsCenter) * this.minDistance;
			y = (prevRows - prevRowsCenter) * this.minDistance;
			this.point[i] = new FormationPoint(i, x, y);
		}
	}

}
