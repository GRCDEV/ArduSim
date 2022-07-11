package com.api.swarm;

import com.api.API;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.assignement.AssignmentAlgorithmFactory;
import com.api.swarm.discovery.BasicDiscover;
import com.api.swarm.discovery.Discover;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.api.swarm.takeoff.TakeoffAlgorithm;
import com.api.swarm.takeoff.TakeoffAlgorithmFactory;
import com.uavController.UAVParam;
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
        API.getGUI(numUAV).updateProtocolState("TAKE_OFF");
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
        private Discover d;
        private double altitude;
        private Formation airFormation;
        private double minDistanceAir;

        public Builder(long numUAV){
            this.numUAV = (int) numUAV;
            this.numUAVs = API.getArduSim().getNumUAVs();
        }

        public Builder discover(Discover discover){
            this.d = discover;
            return this;
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
            this.minDistanceAir = minDistance;
            return this;
        }

        public Swarm build(){
            d.start();

            AssignmentAlgorithm assignment = null;
            Set<Long> IDs = new HashSet<>();
            if(d.getMasterUAVId() == numUAV) {
                Location3DUTM airCenterLoc = d.getMasterLocation();
                airCenterLoc.z = altitude;
                Map<Long, Location3DUTM> groundLocations = d.getUAVsDiscovered();
                assignment = getAssignment(groundLocations, airCenterLoc);
                IDs = groundLocations.keySet();
            }

            TakeoffAlgorithm takeoff = TakeoffAlgorithmFactory.newTakeoffAlgorithm(takeoffAlgo, assignment);
            return new Swarm(takeoff, d.getMasterUAVId(),IDs,airFormation);
        }

        private AssignmentAlgorithm getAssignment( Map<Long, Location3DUTM> groundLocations, Location3DUTM centerUAVLocation) {
            API.getGUI(numUAV).updateProtocolState("ASSIGNMENT");
            airFormation.init(d.getUAVsDiscovered().size(),minDistanceAir);
            Map<Long, Location3DUTM> airLocations = new HashMap<>();
            int i=0;
            for(long id: groundLocations.keySet()){
                airLocations.put(id, airFormation.get3DUTMLocation(centerUAVLocation,i));
                i++;
            }
            return AssignmentAlgorithmFactory.newAssignmentAlgorithm(assignmentAlgo, groundLocations, airLocations);
        }

    }
}
