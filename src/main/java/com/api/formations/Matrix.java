package com.api.formations;

/**
 * Matrix formation
 * all UAVs are on a grid with a certain distance between them, last row might be incomplete
 * Use {@link com.api.formations.FormationFactory} to get and intract with this layout
 */
public class Matrix extends Formation{
    /**
     * @return the layout (enum: MATRIX)
     */
    @Override
    public Layout getLayout() {
        return Layout.MATRIX;
    }

    /**
     * Used to get the index of the UAV which is in the center of the formation
     * @return center UAV index
     */
    @Override
    public int getCenterIndex() {
        int numUAVs = positions.length;
        int cols = (int)Math.ceil(Math.sqrt(numUAVs));
        int rows = (int)Math.ceil(numUAVs/cols);
        int prevColsCenter = (cols -1)/2;
        int prevRowscenter = rows /2;
        int centerIndex = prevRowscenter * cols + prevColsCenter;
        //int n = (int)Math.ceil(Math.sqrt(positions.length));
        return centerIndex;
    }

    @Override
    /**
     * Place all UAVs in a grid, UAV 0 top left corner with spaces of minDistance between them
     * last row might be incomplete
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
    protected FormationPoint[] calculateFormation(int numUAVs, double minDistance) {
        FormationPoint[] positions = new FormationPoint[numUAVs];
        // N*N matrix where the last row might be incomplete
        int n = (int)Math.ceil(Math.sqrt(numUAVs));

        for(int index = 0;index<numUAVs;index++){
            int row = index/n;
            int col = index%n;
            double x = col * minDistance;
            double y = row * minDistance;
            positions[index] = new FormationPoint(index,x,y);
        }
        return positions;
    }


}
