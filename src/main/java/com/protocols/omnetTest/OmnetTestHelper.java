package com.protocols.omnetTest;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.copter.TakeOffListener;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.setup.Param;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.javatuples.Pair;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class OmnetTestHelper extends ProtocolHelper {
    @Override
    public void setProtocol() {
        this.protocolString = "omnetTest";
    }

    @Override
    public boolean loadMission() {return false;}

    @Override
    public JDialog openConfigurationDialog() {return null; }

    @Override
    public void openConfigurationDialogFX() {
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
    }

    @Override
    public void configurationCLI() { }

    @Override
    public void initializeDataStructures() {}

    @Override
    public String setInitialState() {return null;}

    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();
        Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];

        Location2DGeo centerGeo = new Location2DGeo(39.48271345905396, -0.3467886203790445);
        Location3DUTM center1 = new Location3DUTM(centerGeo.getUTM(),0);
        Formation f = FormationFactory.newFormation(Formation.Layout.CIRCLE2);
        f.init(numUAVs,100);

        startingLocations[0] = Pair.with(centerGeo,0.0);
        for(int i=1;i<numUAVs;i++ ){
            Location3DUTM locUTM = f.get3DUTMLocation(center1,i);
            try {
                Location2DGeo locGeo = new Location2DUTM(locUTM.x,locUTM.y).getGeo();
                startingLocations[i] = Pair.with(locGeo, 0.0);
            } catch (LocationNotReadyException e) {
                throw new RuntimeException(e);
            }
        }

        return startingLocations;
    }

    @Override
    public boolean sendInitialConfiguration(int numUAV) {return true;}

    @Override
    public void startThreads() {}

    @Override
    public void setupActionPerformed() {
        int numUAVs = API.getArduSim().getNumUAVs();
        List<Thread> threads = new ArrayList<>();
        for(int i=0;i<numUAVs;i++){
            threads.add(API.getCopter(i).takeOff(10, new TakeOffListener() {
                @Override
                public void onCompleteActionPerformed() {}

                @Override
                public void onFailure() {}
            }));
        }
        for(Thread t:threads){
            t.start();
        }
        for(Thread t:threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void startExperimentActionPerformed() {
        for(int i=0;i<API.getArduSim().getNumUAVs();i++){
           new OmnetTestThread(i).start();
        }
    }

    @Override
    public void forceExperimentEnd() {}

    @Override
    public String getExperimentResults() {return null;}

    @Override
    public String getExperimentConfiguration() {return null;}

    @Override
    public void logData(String folder, String baseFileName, long baseNanoTime) {}

    @Override
    public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
}
