package com.protocols.mbcapSwarm.logic;

import com.api.API;
import com.api.ProtocolHelper;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.setup.Param;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.javatuples.Pair;

import javax.swing.*;

public class MBCAPSwarmHelper extends ProtocolHelper {

    @Override
    public void setProtocol() {this.protocolString = "MBCAPSwarm"; }

    @Override
    public boolean loadMission() {return false;}

    @Override
    public JDialog openConfigurationDialog() {return null;}

    @Override
    public void openConfigurationDialogFX() {
        //TODO implement mbcapSwarm GUI
        System.err.println("Implement GUI!");
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
        //Platform.runLater(()->new MBCAPSwarmConfigDialogApp().start(new Stage()));
    }

    @Override
    public void configurationCLI() {
        //TODO implement mbcapSwarm CLI
        System.err.println("implement CLI!");
        /*
        MBCAPSwarmSimProperties properties = new MBCAPSwarmSimProperties();
		ResourceBundle resources;
		try {
			FileInputStream fis = new FileInputStream(SimParam.protocolParamFile);
			resources = new PropertyResourceBundle(fis);
			fis.close();
			Properties p = new Properties();
			for(String key: resources.keySet()){
				p.setProperty(key,resources.getString(key));
			}
			properties.storeParameters(p,resources);
		} catch (IOException e) {
			ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND );
			System.exit(0);
		}
         */
    }

    @Override
    public void initializeDataStructures() { }

    @Override
    public String setInitialState() {
        return null;
    }

    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();
        Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];

        Location3DUTM center1 = new Location3DUTM(new Location2DGeo(39.48271345905396, -0.3467886203790445).getUTM(),0);
        Formation f = FormationFactory.newFormation(Formation.Layout.MATRIX);
        f.init(numUAVs/2,20);

        for(int i=0;i<numUAVs/2;i++ ){
            Location3DUTM locUTM = f.get3DUTMLocation(center1,i);
            try {
                Location2DGeo locGeo = new Location2DUTM(locUTM.x,locUTM.y).getGeo();
                startingLocations[i] = Pair.with(locGeo, 0.0);
            } catch (LocationNotReadyException e) {
                throw new RuntimeException(e);
            }
        }

        Location3DUTM center2 = new Location3DUTM(new Location2DGeo(39.47677160995003, -0.3241238672903294).getUTM(),0);
        for(int i=numUAVs/2;i<numUAVs;i++){
            //set swarm 2 at place
            Location3DUTM locUTM = f.get3DUTMLocation(center2,i-numUAVs/2);
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
    public boolean sendInitialConfiguration(int numUAV) {
        return true;
    }

    @Override
    public void startThreads() { }

    @Override
    public void setupActionPerformed() {

        for(int i = 0;i<API.getArduSim().getNumUAVs();i++){
            MBCAPSwarmThread t = new MBCAPSwarmThread(i);
            t.start();
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
