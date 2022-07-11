package com.protocols.mbcapSwarm.logic;

import com.api.swarm.Swarm;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.discovery.BasicDiscover;
import com.api.swarm.formations.Formation;
import com.api.swarm.takeoff.TakeoffAlgorithm;

public class MBCAPSwarmThread extends Thread{

    private int numUAV;


    public MBCAPSwarmThread(int numUAV){
        this.numUAV = numUAV;
    }
    public void run(){
        Swarm s = new Swarm.Builder(numUAV)
                .discover(new BasicDiscover(numUAV))
                .assignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms.HEURISTIC)
                .airFormationLayout(Formation.Layout.MATRIX,20)
                .takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms.SIMULTANEOUS,25)
                .build();

        s.takeOff(numUAV);
    }
}
