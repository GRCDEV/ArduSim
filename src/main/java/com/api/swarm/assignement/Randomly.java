package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;

import java.util.*;

class Randomly extends AssignmentAlgorithm {

    public Randomly(Map<Long, Location3DUTM> groundLocations, Map<Long, Location3DUTM> airLocations) {
        super.validateInput(groundLocations,airLocations);
        this.groundLocations = groundLocations;
        this.airLocations = airLocations;
    }

    @Override
    public AssignmentAlgorithms getAssignmentAlgorithm() {
        return AssignmentAlgorithms.RANDOM;
    }

    @Override
    protected void calculateAssignment() {
        List<Long> ids = new ArrayList<>(groundLocations.keySet());
        Collections.shuffle(ids);
        assignment = new HashMap<>();
        for(int i=0;i<airLocations.size();i++){
            assignment.put(ids.get(i), airLocations.get((long)i));
        }
    }
}
