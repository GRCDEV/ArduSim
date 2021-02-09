package main.api.formations;

import api.API;
import es.upv.grc.mapper.Location2DUTM;
import main.api.ValidationTools;

import static main.api.formations.Formation.Layout.LINEAR;

public class Linear extends Formation {

    @Override
    public Layout getLayout() {
        return LINEAR;
    }

    @Override
    public void init(int numUAVs, double minDistance) {
        this.positions = calculateFormation(numUAVs, minDistance);
    }

    @Override
    public Location2DUTM get2DUTMLocation(Location2DUTM centerLocation, int index) {
        double x = centerLocation.x - positions[index].offsetX;
        double y = centerLocation.y;
        return new Location2DUTM(x,y);
    }

    @Override
    public int getCenterIndex() {
        int numUAVs = positions.length;
        return numUAVs /2;
    }

    @Override
    public int getNumUAVs() {
        return positions.length;
    }

    private FormationPoint[] calculateFormation(int numUAVs, double minDistance){
        FormationPoint[] positions = new FormationPoint[numUAVs];
        int centerUAVIndex = numUAVs / 2;

        double x;
        ValidationTools validationTools = API.getValidationTools();
        for (int i = 0; i < numUAVs; i++) {
            x = validationTools.roundDouble((i - centerUAVIndex) * minDistance, 6);
            positions[i] = new FormationPoint(i, x, 0);
        }
        return positions.clone();
    }


}
