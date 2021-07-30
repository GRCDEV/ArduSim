package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;

import java.util.HashMap;
import java.util.Map;

class BruteForce extends AssignmentAlgorithm{

    BruteForce(Map<Long, Location3DUTM> groundLocations, Map<Long, Location3DUTM> airLocations){
        super.validateInput(groundLocations, airLocations);
        this.groundLocations = groundLocations;
        this.airLocations = airLocations;
    }

    @Override
    public AssignmentAlgorithms getAssignmentAlgorithm() {
        return AssignmentAlgorithms.BRUTE_FORCE;
    }

    @Override
    protected void calculateAssignment() {
        assignment = new HashMap<>();
        Long[] groundIds = groundLocations.keySet().toArray(new Long[0]);
        Long[] airIds = airLocations.keySet().toArray(new Long[0]);

        Long[] bestPermutation = calculate(groundIds, airIds);
        saveAssignment(groundIds, bestPermutation);
    }

    private Long[] calculate(Long[] groundIds, Long[] airIds) {
        double bestDistance = Double.MAX_VALUE;
        Long[] bestPermutation = null;

        Permutation<Long> p = new Permutation<>(airIds);
        Long[] permutation = p.next();
        while(permutation != null){
            double totalDist = calculateDistanceSquared(groundIds, permutation);
            if(totalDist < bestDistance){
                bestDistance = totalDist;
                bestPermutation = permutation;
            }
            permutation = p.next();
        }
        return bestPermutation;
    }

    private double calculateDistanceSquared(Long[] groundIds, Long[] permutation) {
        double totalDistance = 0;
        for(int i = 0; i< groundIds.length; i++){
            Location3DUTM groundloc = groundLocations.get(groundIds[i]);
            Location3DUTM airloc = airLocations.get(permutation[i]);
            double distance = Math.pow(groundloc.distance3D(airloc),2);
            totalDistance += distance;
        }
        return totalDistance;
    }

    private void saveAssignment(Long[] groundIds, Long[] bestPermutation) {
        for(int i=0;i<groundLocations.size();i++){
            assignment.put(groundIds[i], airLocations.get(bestPermutation[i]));
        }
    }
}
