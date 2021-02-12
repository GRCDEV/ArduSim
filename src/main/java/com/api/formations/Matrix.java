package com.api.formations;

import java.util.ArrayList;

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
        return 0;
    }

    /**
     * Place all UAVs in a grid, UAV 0 in the center, then the cross is filled then the corners, etc.
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: distance (x) between each UAV
     * @return array of FormationPoints
     */
    @Override
    protected FormationPoint[] calculateFormation(int numUAVs, double minDistance) {
        FormationPoint[] positions = new FormationPoint[numUAVs];
        // Center UAV
        positions[0] = new FormationPoint(0, 0, 0);

        ArrayList<Double> startAngles = getStartAngles(19);

        int distance = 0;
        for(int i=0;i<numUAVs-1;i++){
            boolean currentEven = (Math.floor(Math.sqrt(i)) %2 == 0);
            boolean nextOdd = (Math.floor(Math.sqrt(i+1)) %2 == 1);
            if(currentEven && nextOdd){
                distance +=1;
            }

            // place point on (1,0)
            double x=1,y;
            // Turn it until its in the right position
            double theta = i*Math.PI/2 + startAngles.get(i/4);
            y = x*Math.sin(theta); // + y*Math.cos(theta) but y = 0
            x = x*Math.cos(theta); // - y*Math.sin(theta) but y = 0
            // Scale the point outwards
            x *= distance;
            y *= distance;
            // Round to integers and multiply by mindistance
            double offsetX = ceilAbs(x) * minDistance;
            double offsetY = ceilAbs(y) * minDistance;
            FormationPoint p = new FormationPoint(i+1,offsetX,offsetY);
            positions[i+1] = p;
        }

        return positions;
    }

    /**
     * Ceil a double if it is positive, otherwise floor it.
     * @param value: to be ceiled/floored
     * @return Integer
     */
    private int ceilAbs(double value){
        if(Math.abs(value) < 0.00001){
            value = 0;
        }
        if(value >= 0){
            return (int)Math.ceil(value);
        }else{
            return (int)Math.floor(value);
        }
    }

    /**
     * Returns the list of angles to rotate the UAVs over {0,PI/4,0,PI/4,PI/8,3PI/8,0,....}
     * @param numUAVs: number of UAVs
     * @return ArrayList with angles (in rad)
     */
    private ArrayList<Double> getStartAngles(int numUAVs){
        numUAVs--;
        ArrayList<Double> angles = new ArrayList<>();
        if(numUAVs < 8){
            angles.add(0.0);
            angles.add(Math.PI/4);
        }else{
            while(numUAVs > 0){
                numUAVs -= 8;
                angles.addAll(getStartAngles(numUAVs));
            }
            int length = angles.size();
            angles.add(Math.PI/(4*length));
            angles.add( ((4*length/2)-1) * Math.PI/(4*length));
        }
        return angles;
    }
}
