package com.api.swarm.formations;

import com.api.API;
import es.upv.grc.mapper.Location2DUTM;
import com.api.ValidationTools;
import es.upv.grc.mapper.Location3DUTM;

import java.util.ArrayList;

import static com.api.swarm.formations.Formation.Layout.LINEAR;

/**
 * Linear formation
 * all UAVs are on a line with a certain distance between them
 * Use {@link FormationFactory} to get this layout
 * And use the function {@link #init(int, double, double)} to instantiate the formation
 * use the function {@link #get3DUTMLocation(Location3DUTM, int)} to get the 3DUTM coordinate of a specific UAV
 */
class Linear extends Formation {

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
        int numUAVs = positions.size();
        return numUAVs /2;
    }

    /**
     * Place all UAVs in a line, UAV 0 to the left and spaces of minDistance between them
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
    @Override
    protected ArrayList<FormationPoint> calculateFormation(int numUAVs, double minDistance){
        ArrayList<FormationPoint> positions = new ArrayList<>();
        int centerUAVIndex = numUAVs / 2;

        double x;
        ValidationTools validationTools = API.getValidationTools();
        for (int i = 0; i < numUAVs; i++) {
            x = validationTools.roundDouble((i - centerUAVIndex) * minDistance, 6);
            positions.add(new FormationPoint(i, x, 0,altitude));
        }
        return positions;
    }

}
