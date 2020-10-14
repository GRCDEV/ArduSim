package main.api.masterslavepattern.safeTakeOff;

import es.upv.grc.mapper.Location2DUTM;
import org.javatuples.Quartet;

import java.util.Map;

public class HungarianTakeOff {
    Map<Long, Location2DUTM> groundLocations;
    Map<Integer, Location2DUTM> airLocations;

    public HungarianTakeOff(Map<Long, Location2DUTM> groundLocations, Map<Integer, Location2DUTM> airLocations){
        this.airLocations = airLocations;
        this.groundLocations= groundLocations;
    }

    public Quartet<Integer, Long, Location2DUTM, Double>[] getHungarianMatch(){
        double[][] costmatrix = createCostmatrix();

        return null;
    }

    private double[][] createCostmatrix() {
        return null;
    }
}
