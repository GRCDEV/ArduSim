package com.api.swarm;

import com.api.API;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.assignement.AssignmentAlgorithmFactory;
import com.api.swarm.discovery.Discover;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.api.swarm.takeoff.TakeoffAlgorithm;
import com.api.swarm.takeoff.TakeoffAlgorithmFactory;
import es.upv.grc.mapper.Location3DUTM;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Swarm {

    private final TakeoffAlgorithm takeoff;
    private final long masterUAV;
    private final Set<Long> IDs;
    private final Formation airFormation;

    private Swarm(TakeoffAlgorithm takeoff,long masterUAV,Set<Long> IDs, Formation airFormation){
        this.takeoff = takeoff;
        this.masterUAV = masterUAV;
        this.IDs = IDs;
        this.airFormation = airFormation;
    }

    public void takeOff(int numUAV){
        takeoff.takeOff(numUAV);
    }

    public Set<Integer> getIDs(){
        return  IDs.stream()
                .map(Long::intValue)
                .collect(Collectors.toSet());
    }

    public boolean isMaster(long id){
        return masterUAV == id;
    }

    public Formation getAirFormation(){return airFormation;}

    public static class Builder{
        private final int numUAV;
        private final int numUAVs;
        private AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgo;
        private TakeoffAlgorithm.TakeoffAlgorithms takeoffAlgo;
        private double altitude;
        private Formation airFormation;

        public Builder(long numUAV){
            this.numUAV = (int) numUAV;
            this.numUAVs = API.getArduSim().getNumUAVs();
        }
        public Builder assignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms algo){
            this.assignmentAlgo = algo;
            return this;
        }

        public Builder takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms algo, double altitude){
            this.takeoffAlgo = algo;
            this.altitude = altitude;
            return this;
        }

        public Builder airFormationLayout(Formation.Layout layout, double minDistance){
            this.airFormation = FormationFactory.newFormation(layout);
            airFormation.init(API.getArduSim().getNumUAVs(),minDistance);
            return this;
        }

        public Swarm build(){
            System.out.println("START BUILING SWARM");
            Discover d = new Discover(numUAV);
            d.start();
            Map<Long, Location3DUTM> assignment = null;
            Set<Long> IDs = new HashSet<>();
            if(d.getMasterUAVId() == numUAV) {
                Location3DUTM airCenterLoc = d.getCenterLocation();
                airCenterLoc.z = altitude;
                assignment = getAssignment(numUAVs, d.getUAVsDiscovered(), airCenterLoc);
                IDs = assignment.keySet();
            }

            TakeoffAlgorithm takeoff = TakeoffAlgorithmFactory.newTakeoffAlgorithm(takeoffAlgo, assignment);
            return new Swarm(takeoff, d.getMasterUAVId(),IDs,airFormation);
        }

        private Map<Long, Location3DUTM> getAssignment(int numUAVs, Map<Long, Location3DUTM> groundLocations, Location3DUTM centerUAVLocation) {
            Map<Long, Location3DUTM> assignment;
            Map<Long, Location3DUTM> airLocations = new HashMap<>();
            for(int i = 0; i< numUAVs; i++){
                airLocations.put((long)i, airFormation.get3DUTMLocation(centerUAVLocation,i));
            }
            AssignmentAlgorithm a = AssignmentAlgorithmFactory.newAssignmentAlgorithm(assignmentAlgo, groundLocations, airLocations);
            assignment = a.getAssignment();
            return assignment;
        }

    }
}
