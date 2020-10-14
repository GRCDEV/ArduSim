package main.api.formations;

import api.API;
import main.api.ValidationTools;
import main.api.formations.helpers.FormationPoint;
import protocols.muscop.gui.MuscopSimProperties;

public class FormationSplitUp extends FlightFormation{

	protected FormationSplitUp(int numUAVs, double minDistance, Formation formation) {
		super(numUAVs, minDistance, formation);
	}
	@Override
	protected void initializeFormation() {
		ValidationTools validationTools = API.getValidationTools();
		
		int clusters = MuscopSimProperties.numberOfClusters;
		double x,y;
		double offset = 600;
		this.centerUAVPosition = 0;
		int placedCounter = 0;
		
		//center UAV
		this.point[placedCounter] = new FormationPoint(placedCounter, 0, 0);
		placedCounter++;
		
		int slavesPerCluster = (numUAVs-1)/clusters;
		for(double alfa = 0; alfa < 2*Math.PI; alfa += 2*Math.PI/clusters) {
			double cosA = validationTools.roundDouble(Math.cos(alfa),6);
			double sinA = validationTools.roundDouble(Math.sin(alfa),6);
			for(int i=0;i<slavesPerCluster;i++) {
				if(cosA == 0.0) {
					x = 0;
					y = Math.signum(sinA)*(offset + i*minDistance);
				}else if(sinA == 0.0) {
					x = Math.signum(cosA)*(offset + i*minDistance);
					y = 0;
				}else {
					x = cosA*offset + Math.signum(cosA)*i*minDistance;
					y = sinA*offset + Math.signum(sinA)*i*minDistance;
				}
				this.point[placedCounter] = new FormationPoint(placedCounter, x, y);
				placedCounter++;
			}
		}
		// set the rest of the UAVs in the last cluster
		// TODO completed
		/*
		for(int i= 0;i<numUAVs-placedCounter;i++) {
			x = validationTools.roundDouble(Math.cos(2*Math.PI/clusters)*offset + (slavesPerCluster + i)*minDistance,6);
			y = validationTools.roundDouble(Math.sin(2*Math.PI/clusters)*offset + (slavesPerCluster + i)*minDistance,6);
			this.point[placedCounter] = new FormationPoint(placedCounter, x, y);
			placedCounter++;
		}*/

	}
}
