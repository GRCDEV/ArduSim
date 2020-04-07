package main.api.formations;

import api.API;
import main.api.ValidationTools;
import main.api.formations.helpers.FormationPoint;

public class FormationSplitUp extends FlightFormation{

	protected FormationSplitUp(int numUAVs, double minDistance, Formation formation) {
		super(numUAVs, minDistance, formation);
	}
	@Override
	protected void initializeFormation() {
		//linear
		
		this.centerUAVPosition = this.numUAVs / 2;
		
		double x;
		double offset = 200;
		ValidationTools validationTools = API.getValidationTools();
		
		/*
		x = validationTools.roundDouble((0 - this.centerUAVPosition) * this.minDistance - offset, 6);
		this.point[0] = new FormationPoint(0, x, 0);
		
		for (int i = 1; i < this.numUAVs; i++) {
			x = validationTools.roundDouble((i - this.centerUAVPosition) * this.minDistance, 6);
			this.point[i] = new FormationPoint(i, x, 0);
		}
		
		*/
		//left part
		for (int i = 0; i < this.centerUAVPosition; i++) {
			x = validationTools.roundDouble((i - this.centerUAVPosition) * this.minDistance - offset, 6);
			this.point[i] = new FormationPoint(i, x, 0);
		}
		//center UAV
		x = validationTools.roundDouble(0, 6);
		this.point[this.centerUAVPosition] = new FormationPoint(this.centerUAVPosition, x, 0);
		
		//right part
		for(int i = this.centerUAVPosition+1;i<this.numUAVs;i++) {
			x = validationTools.roundDouble((i - this.centerUAVPosition) * this.minDistance + offset, 6);
			this.point[i] = new FormationPoint(i, x, 0);
		}
		
		
	}

}
