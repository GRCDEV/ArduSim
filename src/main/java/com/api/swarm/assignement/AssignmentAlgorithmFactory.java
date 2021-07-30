package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;

import java.util.Map;

public class AssignmentAlgorithmFactory {

    public static AssignmentAlgorithm newAssignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms algorithm,
                                                             Map<Long, Location3DUTM> groundLocations,
                                                             Map<Long, Location3DUTM> airLocations){
        switch (algorithm){
            case BRUTE_FORCE:
                return new BruteForce(groundLocations,airLocations);
            case HEURISTIC:
                return new Heuristic(groundLocations, airLocations);
            case KMA:
                return new KMA(groundLocations, airLocations);
            case RANDOM:
                return new Randomly(groundLocations, airLocations);
            default:
                throw new IllegalArgumentException("Unexpected value: " + algorithm);
        }
    }
}
