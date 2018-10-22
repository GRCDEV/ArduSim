package api.pojo.formations;

/** The formation numbering starts in 0 an increases from left to right, and down to up.
 * The selection of the center UAV tries to minimize the distance to the further UAVs. */

public class MatrixFlightFormation extends FlightFormation {

	protected MatrixFlightFormation(int numUAVs, double minDistance) {
		super(numUAVs, minDistance);
	}

	@Override
	protected void initializeFormation() {
		int cols = (int)Math.ceil(Math.sqrt(this.numUAVs));
		int rows = (int)Math.ceil(this.numUAVs / cols);
		int prevColsCenter = (cols - 1) / 2;
		int prevRowsCenter = rows / 2;
		this.centerUAV = prevRowsCenter * cols + prevColsCenter;
		
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
