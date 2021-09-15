package com.api.swarm.formations;

import java.util.ArrayList;

import static com.api.swarm.formations.Formation.Layout.RANDOM;

class Random extends Formation{
    private final double occupancyRate = 0.5;
    private final double maxJitter = 0.40;
    final java.util.Random rand = new java.util.Random(2);

    private ArrayList<Integer> freeSquares = new ArrayList<>();
    private ArrayList<FormationPoint> positions = new ArrayList<>();
    private double minDistance;

    @Override
    public Layout getLayout() {
        return RANDOM;
    }

    @Override
    public int getCenterIndex() {
        return 0;
    }

    @Override
    protected ArrayList<FormationPoint> calculateFormation(int numUAVs, double minDistance) {
        int lengthSide = (int)Math.ceil(Math.sqrt(numUAVs/occupancyRate));
        int halfLength = createHalfLength(lengthSide);
        this.minDistance = minDistance;

        for(int i = 0;i<lengthSide*lengthSide;i++){
            freeSquares.add(i);
        }

        placeUAVInTheCenter(lengthSide, halfLength);

        for(int i = 1;i<numUAVs;i++) {
            int cellNumber = popRandomFreeCell();

            int row = (cellNumber/lengthSide)- halfLength;
            int column = (cellNumber%lengthSide)- halfLength;
            double x = getRandomPlaceInCell(row);
            double y = getRandomPlaceInCell(column);
            positions.add(new FormationPoint(i,x,y,0));
        }
        return positions;
    }

    private double getRandomPlaceInCell(int cell){
        double jitter = rand.nextDouble()*minDistance*maxJitter;
        return cell*(1+maxJitter)*minDistance + jitter;
    }

    private int popRandomFreeCell() {
        int randomIndex = rand.nextInt(freeSquares.size());
        int cellNumber = freeSquares.get(randomIndex);
        freeSquares.remove(randomIndex);
        return cellNumber;
    }

    private void placeUAVInTheCenter(int lengthSide, int halfLength) {
        positions.add(new FormationPoint(0,0,0,0));
        int centerCellNr = halfLength * lengthSide + halfLength;
        freeSquares.remove(centerCellNr);
    }

    private int createHalfLength(int lengthSide) {
        int halfLength;
        if(lengthSide %2 ==1){
            halfLength = (lengthSide - 1) / 2;
        }else{
            halfLength = lengthSide / 2;
        }
        return halfLength;
    }
}
