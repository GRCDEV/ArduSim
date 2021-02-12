package com.api.formations;

import java.util.ArrayList;

import static com.api.formations.Formation.Layout.RANDOM;

public class Random extends Formation{
    @Override
    public Layout getLayout() {
        return RANDOM;
    }

    @Override
    public int getCenterIndex() {
        return 0;
    }

    @Override
    protected FormationPoint[] calculateFormation(int numUAVs, double minDistance) {
        FormationPoint[] positions = new FormationPoint[numUAVs];
        final double occupancyRate = 0.5;
        final double maxJitter = 0.40;
        final java.util.Random rand = new java.util.Random(1);

        int lengthSide = (int)Math.ceil(Math.sqrt(numUAVs/occupancyRate));
        ArrayList<Integer> freeSquares = new ArrayList<>();
        for(int i = 0;i<lengthSide*lengthSide;i++){
            freeSquares.add(i);
        }

        // place UAV 0 in the center
        positions[0] = new FormationPoint(0,0,0);
        int halfLength;
        if(lengthSide%2 ==1){
            halfLength = (lengthSide - 1) / 2;
        }else{
            halfLength = lengthSide/ 2;
        }
        int centerCellNr = halfLength *lengthSide + halfLength;
        freeSquares.remove(centerCellNr);

        for(int i = 1;i<numUAVs;i++) {
            // put them in a random cell en remove the cell from the list
            int randomIndex = rand.nextInt(freeSquares.size());
            int cellNumber = freeSquares.get(randomIndex);
            freeSquares.remove(randomIndex);
            int row = (cellNumber/lengthSide)- halfLength;
            int column = (cellNumber%lengthSide)- halfLength;

            // add some extra change in the cell as well
            double jitterX = rand.nextDouble()*minDistance*maxJitter;
            double jitterY = rand.nextDouble()*minDistance*maxJitter;
            positions[i] = new FormationPoint(i,row*(1+maxJitter)*minDistance + jitterX,
                    column*(1+maxJitter)*minDistance + jitterY);
        }
        return positions;
    }
}
