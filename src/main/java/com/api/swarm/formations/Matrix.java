package com.api.swarm.formations;

import org.javatuples.Pair;

import java.util.*;

/**
 * Matrix formation
 * all UAVs are on a grid with a certain distance between them, last row might be incomplete
 * Use {@link com.api.swarm.formations.FormationFactory} to get and interact with this layout
 */
class Matrix extends Formation{
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
    protected ArrayList<FormationPoint> calculateFormation(int numUAVs, double minDistance) {
        Set<Pair<Integer, Integer>> set = createSetWithCoordinates(numUAVs);
        List<Pair<Integer, Integer>> list = orderSetClosestToOrigin(set);
        return createFormationPoints(minDistance, list);
    }

    private ArrayList<FormationPoint> createFormationPoints(double minDistance, List<Pair<Integer, Integer>> list) {
        ArrayList<FormationPoint> positions = new ArrayList<>();
        int id = 0;
        for(Pair<Integer,Integer> pair : list){
            double x = pair.getValue0()* minDistance;
            double y = pair.getValue1()* minDistance;
            positions.add(new FormationPoint(id,x,y,0));
            id++;
        }
        return positions;
    }

    private List<Pair<Integer, Integer>> orderSetClosestToOrigin(Set<Pair<Integer, Integer>> set) {
        List<Pair<Integer, Integer>> list = new ArrayList<>(set);
        list.sort(new CoordinateComparator());
        return list;
    }

    private Set<Pair<Integer, Integer>> createSetWithCoordinates(int numUAVs) {
        Set<Pair<Integer,Integer>> set = new HashSet<>();
        int halfSide = (int)Math.ceil(Math.sqrt(numUAVs))/2;
        for(int x=0;x<=halfSide;x++){
            for(int y=0;y<=halfSide;y++){
                set.add(new Pair<>(x,y));
                set.add(new Pair<>(-x,y));
                set.add(new Pair<>(x,-y));
                set.add(new Pair<>(-x,-y));
            }
        }
        return set;
    }
}

class CoordinateComparator implements Comparator<Pair<Integer, Integer>> {
    @Override
    public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
        double distToOrigin1 = Math.sqrt(Math.pow(o1.getValue0(),2) + Math.pow(o1.getValue1(),2));
        double distToOrigin2 = Math.sqrt(Math.pow(o2.getValue0(),2) + Math.pow(o2.getValue1(),2));
        return Double.compare(distToOrigin1, distToOrigin2);
    }
}
