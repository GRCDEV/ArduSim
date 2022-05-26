package com.protocols.magnetics.logic;

import com.api.API;
import com.api.ArduSimTools;
import com.api.ProtocolHelper;
import com.api.pojo.location.Waypoint;
import com.protocols.magnetics.gui.MagneticsDialogApp;
import com.protocols.magnetics.gui.MagneticsSimProperties;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import es.upv.grc.mapper.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class MagneticsHelper extends ProtocolHelper {
    @Override
    public void setProtocol() { this.protocolString = "Magnetics";}

    @Override
    public boolean loadMission() {return true;}

    @Override
    public JDialog openConfigurationDialog() {return null;}

    @Override
    public void openConfigurationDialogFX() {
        Platform.runLater(()->new MagneticsDialogApp().start(new Stage()));}

    @Override
    public void configurationCLI() {
        MagneticsSimProperties properties = new MagneticsSimProperties();
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
    }

    @Override
    public void initializeDataStructures() {}

    @Override
    public String setInitialState() {
        return null;
    }

    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();
        @SuppressWarnings("unchecked")
        Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];
        double heading = 0.0;
        Waypoint waypoint1, waypoint2;
        waypoint1 = waypoint2 = null;
        int waypoint1pos = 0;
        boolean waypointFound;
        Location2DUTM p1UTM, p2UTM;
        double incX, incY;
        List<Waypoint>[] missions = API.getCopter(0).getMissionHelper().getMissionsLoaded();
        List<Waypoint> mission;
        for (int i = 0; i < numUAVs; i++) {
            mission = missions[i];
            if (mission != null) {
                waypointFound = false;
                for (int j=0; j<mission.size() && !waypointFound; j++) {
                    waypoint1 = mission.get(j);
                    if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
                        waypoint1pos = j;
                        waypointFound = true;
                    }
                }
                if (!waypointFound) {
                    API.getGUI(0).exit(com.protocols.mbcap.logic.MBCAPText.UAVS_START_ERROR_2 + " " + API.getCopter(i).getID());
                }
                waypointFound = false;
                for (int j=waypoint1pos+1; j<mission.size() && !waypointFound; j++) {
                    waypoint2 = mission.get(j);
                    if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
                        waypointFound = true;
                    }
                }
                if (waypointFound) {
                    // We only can set a heading if at least two points with valid coordinates are found
                    p1UTM = waypoint1.getUTM();
                    p2UTM = waypoint2.getUTM();
                    incX = p2UTM.x - p1UTM.x;
                    incY = p2UTM.y - p1UTM.y;
                    if (incX != 0 || incY != 0) {
                        if (incX == 0) {
                            if (incY > 0)	heading = 0.0;
                            else			heading = 180.0;
                        } else if (incY == 0) {
                            if (incX > 0)	heading = 90;
                            else			heading = 270.0;
                        } else {
                            double gamma = Math.atan(incY/incX);
                            if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
                            else 			heading = 270.0 - gamma * 180 / Math.PI;
                        }
                    }
                }
            } else {
                // Assuming that all UAVs have a mission loaded
                API.getGUI(0).exit(com.protocols.mbcap.logic.MBCAPText.APP_NAME + ": " + com.protocols.mbcap.logic.MBCAPText.UAVS_START_ERROR_1 + " " + API.getCopter(i).getID() + ".");
            }
            startingLocations[i] = Pair.with(new Location2DGeo(waypoint1.getLatitude(), waypoint1.getLongitude()), heading * Math.PI / 180);
        }
        return startingLocations;
    }

    @Override
    public boolean sendInitialConfiguration(int numUAV) {return true;}

    @Override
    public void startThreads() { }

    @Override
    public void setupActionPerformed() { }

    @Override
    public void startExperimentActionPerformed() {
        int numUAVs = API.getArduSim().getNumUAVs();
        for(int i=0;i<numUAVs;i++){
            MagneticsAvoidance m = new MagneticsAvoidance(i);
            m.start();
        }
    }

    @Override
    public void forceExperimentEnd() { }

    @Override
    public String getExperimentResults() {return null;}

    @Override
    public String getExperimentConfiguration() { return null;}

    @Override
    public void logData(String folder, String baseFileName, long baseNanoTime) { }

    @Override
    public void openPCCompanionDialog(JFrame PCCompanionFrame) {}
}
