package com.api.masterslavepattern.safeTakeOff;

import com.api.API;
import es.upv.grc.mapper.Location2DUTM;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public class HungarianTakeOff {
    Map<Long, Location2DUTM> groundLocations;
    Map<Integer, Location2DUTM> airLocations;
    long numUAVs;

    public HungarianTakeOff(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations){
        this.airLocations = airLocations;
        this.groundLocations= groundLocations;
        this.numUAVs = API.getArduSim().getNumUAVs();
    }

    public Quartet<Integer, Long, Location2DUTM, Double>[] getHungarianMatch(){
        if (groundLocations == null || groundLocations.size() == 0
                || airLocations == null || groundLocations.size() != airLocations.size()) {
            return null;
        }
        double[][] costmatrix = createCostmatrix();
        if(costmatrix != null) {
            HungarianAlgorithm hungarian = new HungarianAlgorithm(costmatrix.length);
            hungarian.fillWithCostmatrix(costmatrix);
            hungarian.solve();
            return getFit(hungarian.getAssignment());
        }
        return null;
    }

    private Quartet<Integer, Long, Location2DUTM, Double>[] getFit(ArrayList<Pair<Integer,Integer>> assignment) {
        @SuppressWarnings("unchecked")
        Quartet<Integer,Long,Location2DUTM,Double>[] fit = new Quartet[(int)numUAVs];
        for(int id = 0; id< numUAVs;id++){
            Pair<Integer,Integer> p = assignment.get(id);
            long groundLocationIndex = p.getValue0();
            int airLocationIndex = p.getValue1();
            double error = groundLocations.get(groundLocationIndex).distance(airLocations.get(airLocationIndex));
            double errorSquared = error * error;
            fit[id] = Quartet.with(airLocationIndex, groundLocationIndex, airLocations.get(airLocationIndex),errorSquared);
        }
        fit = sortAndReverse(fit);
        return fit;
    }

    private Quartet<Integer, Long, Location2DUTM, Double>[] sortAndReverse(Quartet<Integer, Long, Location2DUTM, Double>[] fit) {
        Arrays.sort(fit, Comparator.comparing(Quartet::getValue3));

        @SuppressWarnings("unchecked")
        Quartet<Integer,Long,Location2DUTM,Double>[] temp = new Quartet[(int)numUAVs];

        for(int i = 0;i<fit.length;i++){
            temp[i] = fit[fit.length-i-1];
        }
        return temp;
    }

    private double[][] createCostmatrix() {
        double[][] costmatrix = new double[(int)numUAVs][(int)numUAVs];
        for(int i =0;i<numUAVs;i++){
            Location2DUTM groundLocation = groundLocations.get((long)i);
            for(int j = 0;j<numUAVs;j++){
                Location2DUTM airLocation = airLocations.get(j);
                // can be null when master is excluded from the list to ensure it's the center UAV
                if(groundLocation != null && airLocation != null) {
                    costmatrix[i][j] = Math.pow(groundLocation.distance(airLocation), 2);
                }
            }
        }
        return costmatrix;
    }
}
