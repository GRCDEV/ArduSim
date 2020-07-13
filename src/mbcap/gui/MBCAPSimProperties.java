package mbcap.gui;

import api.API;
import api.pojo.location.Waypoint;
import es.upv.grc.mapper.DrawableCirclesGeo;
import main.ArduSimTools;
import main.Text;
import main.api.MissionHelper;
import mbcap.logic.MBCAPParam;
import org.javatuples.Pair;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MBCAPSimProperties {

    // parameters set in GUI or file
    private File missionFile;
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
    public static final String EXCLAMATION_IMAGE_PATH = "/resources/mbcap/Exclamation.png";	// Warning image file path
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
                }else if(type.contains("java.io.File")) {
                    var.set(this, new File(value));
                }else{
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_STORE_PARAMETERS + type);
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        if(specificCheckVariables()){
            setSimulationParameters();
            return true;
        }else{
            return false;
        }
    }

    private void setSimulationParameters() {
        API.getArduSim().setNumUAVs(numUAVs);

        File[] fileArray = {missionFile};
        storeMissionFile(fileArray);
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
        MBCAPParam.safePlaceDistance = 2 * MBCAPParam.gpsError + MBCAPParam.EXTRA_ERROR + MBCAPParam.PRECISION_MARGIN;
        MBCAPParam.standStillTimeout = (long) (hoveringTimeout * 1000000000L);
        MBCAPParam.passingTimeout = (long) (overtakeDelayTimeout * 1000000000L);
        MBCAPParam.resumeTimeout = (long) (defaultFlightModeResumeDelay * 1000000000L);
        MBCAPParam.recheckTimeout = (long) (checkRiskSameUAVDelay * 1000L);
        MBCAPParam.globalDeadlockTimeout = (deadlockBaseTimeout * 1000000000L);
    }

    private boolean specificCheckVariables() {
        if(numUAVs<=0){return false;}
        if(beaconInterval<=0){return false;}
        if(beaconRenewalRate<=0){return false;}
        if(interSampleTime<=0){return false;}
        if(minAdvertismentSpeed<=0){return false;}
        if(beaconExpirationTime<=0){return false;}
        if(collisionWarningDistance<=0){return false;}
        if(collisionWarningAltitudeOffset<=0){return false;}
        if(collisionWarningTimeOffset<=0){return false;}
        if(riskCheckPeriod<=0){return false;}
        if(maxNrExpectedConsecutivePacketsLost<=0){return false;}
        if(GPSExpectedError<=0){return false;}
        if(hoveringTimeout<=0){return false;}
        if(overtakeDelayTimeout<=0){return false;}
        if(defaultFlightModeResumeDelay<=0){return false;}
        if(checkRiskSameUAVDelay<=0){return false;}
        return deadlockBaseTimeout > 0;
    }

    public boolean storeMissionFile(File[] selection) {
        // TODO check why paco used a file array instead of a single file
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
