package com.api.swarm.takeoff;

import com.api.swarm.assignement.AssignmentAlgorithm;
import es.upv.grc.mapper.Location3DUTM;

import java.util.Map;

public class TakeoffAlgorithmFactory {
    public static TakeoffAlgorithm newTakeoffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms algorithms,
                                                       AssignmentAlgorithm assignmentAlgo){

        Map<Long, Location3DUTM> assignment = getAssigment(assignmentAlgo);

        switch (algorithms){
            case SEQUENTIAL:
                return new Sequential(assignment);
            case SEMI_SEQUENTIAL:
                return new SemiSequential(assignment);
            case SEMI_SIMULTANEOUS:
                return new SemiSimultaneous(assignment, getGroundlocations(assignmentAlgo));
            case SIMULTANEOUS:
                return new Simultaneous(assignment);
            default:
                throw new IllegalArgumentException("take off algorithm not recognized");
        }
    }

    private static Map<Long, Location3DUTM> getGroundlocations(AssignmentAlgorithm assignmentAlgo) {
        Map<Long,Location3DUTM> groundLocations = null;
        if(assignmentAlgo != null) {
            groundLocations = assignmentAlgo.getGroundLocations();
        }
        return groundLocations;
    }

    private static Map<Long, Location3DUTM> getAssigment(AssignmentAlgorithm assignmentAlgo) {
        Map<Long,Location3DUTM> assignment = null;
        if(assignmentAlgo != null) {
            assignment = assignmentAlgo.getAssignment();
        }
        return assignment;
    }
}
