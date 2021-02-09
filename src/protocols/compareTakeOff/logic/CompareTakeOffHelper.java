package protocols.compareTakeOff.logic;

import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.LocationNotReadyException;
import javafx.application.Platform;
import javafx.stage.Stage;
import main.ArduSimTools;
import main.api.formations.Formation;
import main.sim.logic.SimParam;
import main.uavController.UAVParam;
import org.javatuples.Pair;
import protocols.compareTakeOff.gui.CompareTakeOffDialogApp;
import protocols.compareTakeOff.gui.CompareTakeOffSimProperties;
import protocols.compareTakeOff.pojo.Text;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class CompareTakeOffHelper extends ProtocolHelper {

    static AtomicInteger isTakeOffFinished = new AtomicInteger();

    @Override
    public void setProtocol() {this.protocolString = Text.PROTOCOL_TEXT;}
    @Override
    public boolean loadMission() {return false;}

    @Override
    public JDialog openConfigurationDialog() {return null;}

    @Override
    public void openConfigurationDialogFX() {
        Platform.runLater(()-> new CompareTakeOffDialogApp().start(new Stage()));
    }

    @Override
    public void configurationCLI() {
        CompareTakeOffSimProperties properties = new CompareTakeOffSimProperties();
        ResourceBundle resources;
        try{
            FileInputStream fis = new FileInputStream(SimParam.protocolParamFile);
            resources = new PropertyResourceBundle(fis);
            fis.close();
            Properties p = new Properties();
            for(String key: resources.keySet()){
                p.setProperty(key,resources.getString(key));
            }
            properties.storeParameters(p,resources);
        }catch (IOException e){
            ArduSimTools.warnGlobal(main.Text.LOADING_ERROR, main.Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND);
            System.exit(0);
        }
    }

    @Override
    public void initializeDataStructures() {

    }

    @Override
    public String setInitialState() {return null;}

    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();

        // get center location
        Location2D centerLocation = new Location2D(CompareTakeOffSimProperties.masterInitialLatitude, CompareTakeOffSimProperties.masterInitialLongitude);
        double yaw = CompareTakeOffSimProperties.masterInitialYaw;
        Pair[] startingLocations = new Pair[numUAVs];

        // set the location of all the UAVs based on position of master and type of Formation
        Formation groundFormation = UAVParam.groundFormation.get();
        for(int i = 0;i<numUAVs;i++){
            try {
                startingLocations[i] = Pair.with(groundFormation.get2DUTMLocation(centerLocation.getUTMLocation(),i).getGeo(), yaw);
            } catch (LocationNotReadyException e) {
                e.printStackTrace();
            }
        }

		return startingLocations;
    }

    @Override
    public boolean sendInitialConfiguration(int numUAV) {return true;}

    @Override
    public void startThreads() {
        for (int i = 0; i < API.getArduSim().getNumUAVs(); i++) {
            TakeOffThread t = new TakeOffThread(i);
            t.start();
        }
    }

    @Override
    public void setupActionPerformed() { }

    @Override
    public void startExperimentActionPerformed() { }

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
