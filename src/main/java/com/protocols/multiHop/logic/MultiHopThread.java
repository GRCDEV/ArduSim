package com.protocols.multiHop.logic;

import com.api.API;
import com.api.ArduSim;
import com.api.GUI;
import com.api.swarm.Swarm;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.discovery.MultiHopDiscovery;
import com.api.swarm.formations.Formation;
import com.api.swarm.takeoff.TakeoffAlgorithm;

public class MultiHopThread extends Thread{

    private final int numUAV;
    private boolean setupDone = false;
    private final ArduSim arduSim;
    private final GUI gui;
    private int numUAVs;
    private Swarm swarm;

    public MultiHopThread(int numUAV){
        this.numUAV = numUAV;
        this.gui = API.getGUI(numUAV);
        this.arduSim = new ArduSim();
    }

    @Override
    public void run() {
        setup();
        takeOff();
    }

    private void setup() {
        while (!arduSim.isSetupInProgress()) {arduSim.sleep(300);}
        buildSwarm();
        numUAVs = swarm.getIDs().size();
        setupDone = true;
        while (!arduSim.isExperimentInProgress()){arduSim.sleep(300);}
    }

    private void buildSwarm() {
        gui.updateProtocolState("SETUP");
        swarm =  new Swarm.Builder(numUAV)
                .discover(new MultiHopDiscovery(numUAV))
                .assignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms.KMA)
                .airFormationLayout(Formation.Layout.LINEAR,500)
                .takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms.SIMULTANEOUS,10)
                .build();
    }

    private void takeOff() {
        gui.updateProtocolState("TAKE OFF");
        swarm.takeOff(numUAV);
    }

    public boolean isSetupDone() {
        return setupDone;
    }
}
