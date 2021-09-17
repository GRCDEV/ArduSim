package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;
import java.util.Map;

public abstract class AssignmentAlgorithm {

    public enum AssignmentAlgorithms {
        BRUTE_FORCE,HEURISTIC,KMA,RANDOM
    }

    protected Map<Long, Location3DUTM> groundLocations;
    protected Map<Long, Location3DUTM> airLocations;
    protected Map<Long, Location3DUTM> assignment;

    public Map<Long, Location3DUTM> getAssignment(){
        if(this.assignment == null){
            calculateAssignment();
        }
        return assignment;
    }

    public Map<Long, Location3DUTM> getGroundLocations(){
        return groundLocations;
    }


    public abstract AssignmentAlgorithms getAssignmentAlgorithm();

    public float getTotalDistanceSquared(){
        if(assignment == null){
            calculateAssignment();
        }
        float totalDist=0;
        for(Map.Entry<Long,Location3DUTM> entry:assignment.entrySet()){
            Location3DUTM groundLocation = groundLocations.get(entry.getKey());
            Location3DUTM airLocation = entry.getValue();
            totalDist += Math.pow(groundLocation.distance3D(airLocation),2);
        }
        return totalDist;
    }

    protected abstract void calculateAssignment();

    protected void validateInput(Map<Long, Location3DUTM> groundLocations, Map<Long, Location3DUTM> airLocations) {
        if(groundLocations == null){
            throw new IllegalArgumentException("groundLocations cannot be null");
        }
        if(groundLocations.size() == 0){
            throw new IllegalArgumentException("groundLocations cannot be empty");
        }
        if(airLocations == null){
            throw new IllegalArgumentException("airlocations cannot be null");
        }
        if(airLocations.size() == 0){
            throw new IllegalArgumentException("airlocations cannot be empty");
        }
        if(groundLocations.size() != airLocations.size()){
            throw  new IllegalArgumentException("Size of airlocations must be equal to groundlocations");
        }
    }

}
