package com.protocols.multiHop.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.javatuples.Pair;

import javax.swing.*;

public class MultiHopHelper extends ProtocolHelper {

    private MultiHopThread master;

    @Override
    public void setProtocol() {
        this.protocolString = "MultiHop";
    }

    @Override
    public boolean loadMission() {
        return false;
    }

    @Override
    public JDialog openConfigurationDialog() {
        return null;
    }

    @Override
    public void openConfigurationDialogFX() {
        System.err.println("implement gui first!");
    }

    @Override
    public void configurationCLI() {
        System.err.println("Make some propertie files first!");
    }

    @Override
    public void initializeDataStructures() {

    }

    @Override
    public String setInitialState() {
        return "START";
    }

    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();
        Location3DUTM centerGroundLocation = new Location3DUTM(728235.99,4373697.17,0.0);

        Formation f = FormationFactory.newFormation(Formation.Layout.LINEAR);
        f.init(numUAVs,400);

        Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
        double yaw = 0;
        for(int i = 0;i<numUAVs;i++){
            try {
                startingLocation[i] = new Pair<>(f.get3DUTMLocation(centerGroundLocation,i).getGeo(),yaw);
            } catch (LocationNotReadyException e) {
                e.printStackTrace();
                return null;
            }
        }
        return startingLocation;

    }

    @Override
    public boolean sendInitialConfiguration(int numUAV) {
        return false;
    }

    @Override
    public void startThreads() {
        master = new MultiHopThread(0);
        master.start();
        for (int i = 1; i < API.getArduSim().getNumUAVs(); i++) {
            MultiHopThread t = new MultiHopThread(i);
            t.start();
        }
    }

    @Override
    public void setupActionPerformed() {
        while(!master.isSetupDone()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void startExperimentActionPerformed() {

    }

    @Override
    public void forceExperimentEnd() {

    }

    @Override
    public String getExperimentResults() {
        return null;
    }

    @Override
    public String getExperimentConfiguration() {
        return null;
    }

    @Override
    public void logData(String folder, String baseFileName, long baseNanoTime) {

    }

    @Override
    public void openPCCompanionDialog(JFrame PCCompanionFrame) {

    }
}
