package com.api.formations;

import com.api.API;
import es.upv.grc.mapper.Location2DUTM;
import com.api.ValidationTools;

import static com.api.formations.Formation.Layout.LINEAR;

/**
 * Linear formation
 * all UAVs are on a line with a certain distance between them
 * Use {@link FormationFactory} to get this layout
 * And use the function {@link #init(int, double)} to instantiate the formation
 * use the function {@link #get2DUTMLocation(Location2DUTM, int)} to get the 2D UTM coordinate of a specific UAV
 */
public class Linear extends Formation {

    /**
     * @return the layout (enum: LINEAIR)
     */
    @Override
    public Layout getLayout() {
        return LINEAR;
    }

    /**
     * Used to get the index of the UAV which is in the center of the formation
     * @return center UAV index
     */
    @Override
    public int getCenterIndex() {
        int numUAVs = positions.length;
        return numUAVs /2;
    }

    /**
     * Place all UAVs in a line, UAV 0 to the left and spaces of minDistance between them
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
    @Override
    protected FormationPoint[] calculateFormation(int numUAVs, double minDistance){
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
