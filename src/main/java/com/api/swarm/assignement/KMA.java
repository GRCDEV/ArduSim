package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

class KMA extends AssignmentAlgorithm {

    private int numUAVs;

    public KMA(Map<Long, Location3DUTM> groundLocations, Map<Long, Location3DUTM> airLocations) {
        super.validateInput(groundLocations,airLocations);
        this.groundLocations = groundLocations;
        this.airLocations = airLocations;
        numUAVs = airLocations.size();
    }

    @Override
    public AssignmentAlgorithms getAssignmentAlgorithm() {
        return AssignmentAlgorithms.KMA;
    }


    @Override
    protected void calculateAssignment() {
        float[][] costmatrix = createCostmatrix();
        ArrayList<Triplet<Long,Long,Double>> solutionKMA = KMASolve(costmatrix);
        assignment = sortKMASolution(solutionKMA);
    }

    private float[][] createCostmatrix() {
        float[][] costmatrix = new float[(int)numUAVs][(int)numUAVs];
        for(int i =0;i<numUAVs;i++){
            Location3DUTM groundLocation = groundLocations.get((long)i);
            for(int j = 0;j<numUAVs;j++){
                Location3DUTM airLocation = airLocations.get((long)j);
                float distanceSquared = (float) Math.pow(groundLocation.distance3D(airLocation), 2);
                costmatrix[i][j] = distanceSquared;
            }
        }
        return costmatrix;
    }

    private ArrayList<Triplet<Long,Long,Double>> KMASolve(float[][] costmatrix) {
        KMAAlgorithm kma = new KMAAlgorithm(costmatrix.length);
        kma.fillWithCostmatrix(costmatrix);
        kma.solve();
        ArrayList<Pair<Long,Long>> solution = kma.getAssignment();
        return getAssignmentTriplet(solution);
    }

    private ArrayList<Triplet<Long, Long, Double>> getAssignmentTriplet(ArrayList<Pair<Long, Long>> solution) {
        ArrayList<Triplet<Long,Long,Double>> KMASolution = new ArrayList<>();
        for(Pair<Long,Long> p: solution){
            long groundLocationId = p.getValue0();
            long airLocationId = p.getValue1();
            double distanceSquared = Math.pow(groundLocations.get(groundLocationId).distance3D(airLocations.get(airLocationId)),2);
            Triplet<Long,Long,Double> t = new Triplet<>(groundLocationId,airLocationId,distanceSquared);
            KMASolution.add(t);
        }
        return KMASolution;
    }

    private Map<Long, Location3DUTM> sortKMASolution(ArrayList<Triplet<Long, Long, Double>> solutionKMA) {
        Map<Long, Location3DUTM> sorted = new HashMap<>();
        solutionKMA.sort(Collections.reverseOrder(Comparator.comparing(Triplet::getValue2)));
        for(Triplet<Long,Long,Double> t:solutionKMA){
            long groundLocationId = t.getValue0();
            Location3DUTM airLocation = airLocations.get(t.getValue1());
            sorted.put(groundLocationId,airLocation);
        }
        return sorted;
    }
}
