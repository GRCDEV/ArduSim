package com.api.swarm.formations;

import com.api.API;
import com.api.ValidationTools;
import es.upv.grc.mapper.Location3DUTM;

import java.util.ArrayList;

/**
 * Circle formation
 * all UAVs are on a Circle with UAV0 in the center
 * Use {@link FormationFactory} to get this layout
 * And use the function {@link #init(int, double, double)} to instantiate the formation
 * use the function {@link #get3DUTMLocation(Location3DUTM, int)} to get the 3DUTM coordinate of a specific UAV
 */
class Circle extends Formation{

    /**
     * @return the layout (enum: CIRCLE)
     */
    @Override
    public Layout getLayout() { return Layout.CIRCLE; }

    /**
     * Used to get the index of the UAV which is in the center of the formation
     * @return center UAV index
     */
    @Override
    public int getCenterIndex() {
        return 0;
    }

    /**
     * Place all UAVs in a circle, UAV 0 in the center, all others on the radius with spaces of minDistance between them
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
    @Override
    protected ArrayList<FormationPoint> calculateFormation(int numUAVs, double minDistance) {
        ArrayList<FormationPoint> positions = new ArrayList<>();
        int centerUAVPosition = 0;
        positions.add(new FormationPoint(centerUAVPosition, 0, 0,0));

        double radius;
        if (numUAVs <= 7) {
            radius = minDistance;
        } else {
            radius = minDistance / (2 * Math.sin(Math.PI / (numUAVs - 1)));
        }
        double x, y;
        ValidationTools validationTools = API.getValidationTools();
        for (int i = 1; i < numUAVs; i++) {
            x = validationTools.roundDouble(radius * Math.cos((i -1) * 2 * Math.PI / (numUAVs - 1)), 6);
            y = validationTools.roundDouble(radius * Math.sin((i -1) * 2 * Math.PI / (numUAVs - 1)), 6);
            positions.add(new FormationPoint(i, x, y, altitude));
        }

        return positions;
    }
}
