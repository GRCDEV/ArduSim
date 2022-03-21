package com.protocols.mbcap.gui;

import com.api.API;
import com.api.pojo.location.Waypoint;
import es.upv.grc.mapper.DrawableCirclesGeo;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.MissionHelper;
import com.protocols.mbcap.logic.MBCAPParam;
import org.javatuples.Pair;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MBCAPSimProperties {

    // parameters set in GUI or file
    private List<File> missionFile;
    private int numUAVs;
    private int beaconInterval;
    private int beaconRenewalRate;
    private double interSampleTime;
    private double minAdvertismentSpeed;
    private double beaconExpirationTime;
    private double collisionWarningDistance;
    private double collisionWarningAltitudeOffset;
    private double collisionWarningTimeOffset;
    private double riskCheckPeriod;
    private int maxNrExpectedConsecutivePacketsLost;
    private double GPSExpectedError;
    private double hoveringTimeout;
    private double overtakeDelayTimeout;
    private double defaultFlightModeResumeDelay;
    private double checkRiskSameUAVDelay;
    private int deadlockBaseTimeout;

    // List of the predicted positions of each UAV in Geographic coordinates
    public static AtomicReference<DrawableCirclesGeo>[] predictedLocation;
    // Parameters needed to draw the warning image when a collision risk is detected
    static String fs = File.separator;
    public static final String EXCLAMATION_IMAGE_PATH =  "main" + fs + "resources" + fs + "protocols" + fs + "mbcap" + fs + "Exclamation.png"; // Warning image file path
    public static BufferedImage exclamationImage; // Warning image
    public static final int EXCLAMATION_PX_SIZE = 25; // (px) Size of the image when it is drawn

    public boolean storeParameters(Properties guiParams, ResourceBundle fileParams) {
        // First check if there are parameters set in the file who are not accessed by the gui
        Properties parameters = new Properties();
        // the file always consist of all the parameters but sometimes the value could be different because it is set in the GUI
        for(String key :fileParams.keySet()){
            if(guiParams.containsKey(key)){
                String guiValue = guiParams.getProperty(key);
                parameters.setProperty(key,guiValue);
            }else{
                String fileValue = fileParams.getString(key);
                parameters.setProperty(key,fileValue);
            }
        }
        Iterator<Object> itr = parameters.keySet().iterator();
        // get all the fields in this class
        Field[] variables = this.getClass().getDeclaredFields();
        Map<String,Field> variablesDict = new HashMap<>();
        for(Field var:variables){variablesDict.put(var.getName(),var);}

        // loop through all the parameters in the file
        while(itr.hasNext()){
            String key = itr.next().toString();
            String value = parameters.getProperty(key);
            if(!variablesDict.containsKey(key)){continue;}
            Field var = variablesDict.get(key);
            // set the value of the variable
            try {
                String type = var.getType().toString();
                //System.out.println(var.getName() + "\t" + type + "\t" + value);
                if(type.equals("int")){
                    var.setInt(this,Integer.parseInt(value));
                }else if(type.equals("double")){
                    var.setDouble(this,Double.parseDouble(value));
                }else if(type.contains("java.lang.String")){
                    var.set(this,value);
                }else if(type.contains("java.util.List")) {
                    String[] filesNames = value.split(";");
                    List<File> files = new ArrayList<>();
                    for (String fileName : filesNames) {
                        fileName = API.getFileTools().getResourceFolder().toString() + File.separator + fileName;
                        File f = new File(fileName);
                        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                        if (f.exists() && (extension.equals(Text.FILE_EXTENSION_WAYPOINTS) || extension.equals(Text.FILE_EXTENSION_KML))) {
                            files.add(f);
                        }
                    }
                    var.set(this, files);
                }else{
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_STORE_PARAMETERS + type);
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        String error = specificCheckVariables();
        if(error.equals("")){
            setSimulationParameters();
            return true;
        }else{
            ArduSimTools.warnGlobal(Text.LOADING_ERROR,"Error in parameter: " + error);
            return false;
        }
    }

    private void setSimulationParameters() {
        API.getArduSim().setNumUAVs(numUAVs);
        storeMissionFile(missionFile);
        // Beaconing parameters
        MBCAPParam.beaconingPeriod = beaconInterval;
        MBCAPParam.numBeacons = beaconRenewalRate;
        MBCAPParam.hopTime = interSampleTime;
        MBCAPParam.hopTimeNS = (long) (interSampleTime * 1000000000L);
        MBCAPParam.minSpeed = minAdvertismentSpeed;
        MBCAPParam.beaconExpirationTime = (long) (beaconExpirationTime * 1000000000L);

        // Collision avoidance protocol
        MBCAPParam.collisionRiskDistance = collisionWarningDistance;
        MBCAPParam.collisionRiskAltitudeDifference = collisionWarningAltitudeOffset;
        MBCAPParam.collisionRiskTime = (long) (collisionWarningTimeOffset * 1000000000L);
        MBCAPParam.riskCheckPeriod = (long)  (riskCheckPeriod *1000000000L);
        MBCAPParam.packetLossThreshold = maxNrExpectedConsecutivePacketsLost;
        MBCAPParam.gpsError = GPSExpectedError;
        MBCAPParam.safePlaceDistance = 20;//2 * MBCAPParam.gpsError + MBCAPParam.EXTRA_ERROR + MBCAPParam.PRECISION_MARGIN;
        MBCAPParam.standStillTimeout = (long) (hoveringTimeout * 1000000000L);
        MBCAPParam.passingTimeout = (long) (overtakeDelayTimeout * 1000000000L);
        MBCAPParam.resumeTimeout = (long) (defaultFlightModeResumeDelay * 1000000000L);
        MBCAPParam.recheckTimeout = (long) (checkRiskSameUAVDelay * 1000L);
        MBCAPParam.globalDeadlockTimeout = (deadlockBaseTimeout * 1000000000L);
    }

    private String specificCheckVariables() {
        if(missionFile.size() > 0){
            for(File f :missionFile){
                if(!f.exists()){return "missionFile";}
            }
        }else{return "missionFile";}
        if(numUAVs<=0){return "numUAVs";}
        if(beaconInterval<=0){return "beaconInterval";}
        if(beaconRenewalRate<=0){return "beaconRenewalRate";}
        if(interSampleTime<=0){return "interSampleTime";}
        if(minAdvertismentSpeed<=0){return "minAdvertismentSpeed";}
        if(beaconExpirationTime<=0){return "beaconExpirationTime";}
        if(collisionWarningDistance<=0){return "collisionWarningDistance";}
        if(collisionWarningAltitudeOffset<=0){return "collisionWaringAltitudeOffset";}
        if(collisionWarningTimeOffset<=0){return "collisionWarningTimeOffset";}
        if(riskCheckPeriod<=0){return "riskCheckPeriod";}
        if(maxNrExpectedConsecutivePacketsLost<=0){return "maxnrExpectedConsecutivePacketsLost";}
        if(GPSExpectedError<=0){return "GPSExpectedError";}
        if(hoveringTimeout<=0){return "hoveringTimeout";}
        if(overtakeDelayTimeout<=0){return "overtakedelayTimeout";}
        if(defaultFlightModeResumeDelay<=0){return "defaultFlightModeResumeDelay";}
        if(checkRiskSameUAVDelay<=0){return "checkRiskSameUAVDelay";}
        if(deadlockBaseTimeout < 0){return "deadlockBaseTimeout";}
        return "";
    }

    public boolean storeMissionFile(List<File> selection) {
        final Pair<String, List<Waypoint>[]> missions = API.getGUI(0).loadMissions(selection);
        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        if (missions == null) {
            missionHelper.setMissionsLoaded(null);
            return false;
        } else {
            // Missions are stored
            missionHelper.setMissionsLoaded(missions.getValue1());
            // The number of UAVs is updated
            API.getArduSim().setNumUAVs(Math.min(missions.getValue1().length,API.getArduSim().getNumUAVs()));
            return true;
        }
    }
}
