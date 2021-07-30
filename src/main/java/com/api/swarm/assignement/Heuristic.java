package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;

import java.util.HashMap;
import java.util.Map;

class Heuristic extends AssignmentAlgorithm {

    public Heuristic(Map<Long, Location3DUTM> groundLocations, Map<Long, Location3DUTM> airLocations) {
        super.validateInput(groundLocations,airLocations);
        this.groundLocations = groundLocations;
        this.airLocations = airLocations;
    }

    @Override
    public AssignmentAlgorithms getAssignmentAlgorithm() {
        return AssignmentAlgorithms.HEURISTIC;
    }

    @Override
    protected void calculateAssignment() {
        assignment = new HashMap<>();
        Map<Long,Location3DUTM> airLocationsSorted = getAirLocationsSortedByDistancesToCenter();
        Map<Long,Location3DUTM> groundLocationsCopy = new HashMap<>(groundLocations);

        for(Location3DUTM airLocation: airLocationsSorted.values()){
            long bestID = getBestID(airLocation, groundLocationsCopy);
            assignment.put(bestID,airLocation);
            groundLocationsCopy.remove(bestID);
        }
    }

    private long getBestID(Location3DUTM airLocation, Map<Long, Location3DUTM> groundLocationsCopy) {
        double bestDistance = Double.MAX_VALUE;
        long bestID = -1;
        for(Map.Entry<Long,Location3DUTM> gLocation: groundLocationsCopy.entrySet()){
            double distanceSquared = Math.pow(gLocation.getValue().distance3D(airLocation),2);
            if(distanceSquared<bestDistance){
                bestDistance = distanceSquared;
                bestID = gLocation.getKey();
            }
        }
        return bestID;
    }

    private Map<Long, Location3DUTM> getAirLocationsSortedByDistancesToCenter() {
        return createAirLocationsSortedMap(sortDistances(calculateDistanceToAir(getGroundCenter())));
    }

    private Map<Long, Location3DUTM> createAirLocationsSortedMap(Map<Long, Double> distanceAirLcoationToGroundCenterSorted) {
        Map<Long,Location3DUTM> airLocationsSorted = new HashMap<>();
        for(Long airId: distanceAirLcoationToGroundCenterSorted.keySet()){
            airLocationsSorted.put(airId,airLocations.get(airId));
        }
        return airLocationsSorted;
    }


    private Map<Long, Double> sortDistances(Map<Long, Double> airDistances) {
        Map<Long,Double> sorted = new HashMap<>();
        airDistances.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> sorted.put(x.getKey(),x.getValue()));
        return sorted;
    }

    private Map<Long,Double> calculateDistanceToAir(Location3DUTM groundCenterLocation) {
        Map<Long,Double> distanceToAir = new HashMap<>();
        airLocations.forEach((k,v) -> distanceToAir.put(k,v.distance3D(groundCenterLocation)));
        return distanceToAir;
    }

    private Location3DUTM getGroundCenter() {
        double x=0,y=0,z=0;
        int numUAVs = airLocations.size();
        for(Location3DUTM loc:airLocations.values()){
            x += loc.x;
            y += loc.y;
            z += loc.z;
        }
        return new Location3DUTM(x/numUAVs,y/numUAVs,z/numUAVs);
    }
}
