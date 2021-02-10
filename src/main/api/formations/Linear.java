package main.api.formations;

import api.API;
import es.upv.grc.mapper.Location2DUTM;
import main.api.ValidationTools;

import static main.api.formations.Formation.Layout.LINEAR;

/**
 * Linear formation
 * all UAVs are on a line with a certain distance between them
 * Use {@link main.api.formations.FormationFactory} to get this layout
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
     * Used to initialize the formation
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: minimal distance between each UAV
     */
    @Override
    public void init(int numUAVs, double minDistance) {
        if(numUAVs < 1 | minDistance <= 0){
            throw new Error("Input parameters invalid");
        }
        this.positions = calculateFormation(numUAVs, minDistance);
    }

    /**
     * Used to get a specific (real) location in the formation
     * @param centerLocation: location of the centerUAV
     * @param index: index of the UAV in formation (0 based), 0 is most left UAV
     * @return Location: 2DUTM location of the UAV
     */
    @Override
    public Location2DUTM get2DUTMLocation(Location2DUTM centerLocation, int index) {
        if(index < 0 | index >= positions.length | centerLocation == null){
            throw new Error("input parameters invalid");
        }
        double x = centerLocation.x + positions[index].offsetX;
        double y = centerLocation.y;
        return new Location2DUTM(x,y);
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
     * @return number of UAVs in the formation
     */
    @Override
    public int getNumUAVs() {
        return positions.length;
    }

    /**
     * Place all UAVs in a line, UAV 0 to the left and spaces of minDistance between them
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
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
