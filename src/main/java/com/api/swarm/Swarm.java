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
import java.util.Map;

public class Swarm {

    private final TakeoffAlgorithm takeoff;
    private final long masterUAV;

    private Swarm(TakeoffAlgorithm takeoff,long masterUAV){
        this.takeoff = takeoff;
        this.masterUAV = masterUAV;
    }

    public void takeOff(int numUAV){
        takeoff.takeOff(numUAV);
    }

    public boolean isMaster(long id){
        return masterUAV == id;
    }

    public static class Builder{
        private final int numUAV;
        private final int numUAVs;
        private AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgo;
        private TakeoffAlgorithm.TakeoffAlgorithms takeoffAlgo;
        private Formation.Layout airFormationLayout;
        private double airMinDistance;
        private double altitude;

        public Builder(long numUAV){
            this.numUAV = (int) numUAV;
            this.numUAVs = API.getArduSim().getNumUAVs();
        }
        public Builder assignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms algo){
            this.assignmentAlgo = algo;
            return this;
        }

        public Builder takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms algo){
            this.takeoffAlgo = algo;
            return this;
        }

        public Builder airFormationLayout(Formation.Layout f, double minDistance, double altitude){
            this.airFormationLayout = f;
            this.airMinDistance = minDistance;
            this.altitude = altitude;
            return this;
        }

        public Swarm build(){
            Discover d = new Discover(numUAV);
            d.start();
            Map<Long, Location3DUTM> assignment = null;
            if(d.getMasterUAVId() == numUAV) {
                assignment = getAssignment(numUAVs, d.getUAVsDiscovered(), d.getCenterLocation());
            }

            TakeoffAlgorithm takeoff = TakeoffAlgorithmFactory.newTakeoffAlgorithm(takeoffAlgo, assignment);
            return new Swarm(takeoff, d.getMasterUAVId());
        }

        private Map<Long, Location3DUTM> getAssignment(int numUAVs, Map<Long, Location3DUTM> groundLocations, Location3DUTM centerUAVLocation) {
            Map<Long, Location3DUTM> assignment;
            Formation f = FormationFactory.newFormation(airFormationLayout);
            f.init(numUAVs, airMinDistance,altitude);
            Map<Long, Location3DUTM> airLocations = new HashMap<>();
            for(int i = 0; i< numUAVs; i++){
                airLocations.put((long)i,f.get3DUTMLocation(centerUAVLocation,i));
            }
            AssignmentAlgorithm a = AssignmentAlgorithmFactory.newAssignmentAlgorithm(assignmentAlgo, groundLocations, airLocations);
            assignment = a.getAssignment();
            return assignment;
        }

    }
}
