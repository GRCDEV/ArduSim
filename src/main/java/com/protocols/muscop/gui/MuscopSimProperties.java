package com.protocols.muscop.gui;

import com.api.API;
import com.api.pojo.location.Waypoint;
import es.upv.grc.mapper.Location3DUTM;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.MissionHelper;
import com.api.formations.Formation;
import com.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;
import org.javatuples.Pair;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MuscopSimProperties {

    // GUI parameters
    public List<File> missionFile;
    public Formation groundFormation;
    public static int numberOfClusters = 3;
    public double groundMinDistance;
    public TakeOffAlgorithm takeOffStrategy;
    public Formation flyingFormation;
    public double flyingMinDistance;
    public double landingMinDistance;

    // Timeouts
    public static int RECEIVING_TIMEOUT = 50;        // (ms) The port is unlocked after this time when receiving messages
    public static long SENDING_TIMEOUT = 200; 		// (ms) Time between packets sent
    public static long LAND_CHECK_TIMEOUT = 250;  	// (ms) Between checks if the UAV has landed
    public static long STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
    public static long MISSION_TIMEOUT = 2000; 		// (ms) Timeout to wait after the main.java.com.protocols.mission is received and the master UAV changes its state
    public static long TTL=5000; 					// (ms) Time to life for a UAV.
    public static int RECEIVETIMEOUT = 200;			// (ms) Timeout for link.receiveMessage()
    // Maximum number of waypoints that fit in a datagram.
    public static int MAX_WAYPOINTS = 61;	// main.java.main.java.api.CommLink.DATAGRAM_MAX_LENGTH - 2 - 2 - 3x8xn >= 0

    // (rad) Master UAV yaw or heading (first main.java.com.protocols.mission segment orientation in simulation).
    public static volatile double formationYaw;
    // Mission sent from master to slaves (center main.java.com.protocols.mission).
    public static AtomicReference<Location3DUTM[]> missionSent = new AtomicReference<>();// Array containing the main.java.com.protocols.mission sent to the slaves

    public static AtomicInteger[] state;


    public boolean storeParameters(Properties guiParams, ResourceBundle fileParams){
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
        Field[] variables = this.getClass().getFields();
        Map<String,Field> variablesDict = new HashMap<>();
        for(Field var:variables){variablesDict.put(var.getName(),var);}

        // loop through all the parameters in the file
        while(itr.hasNext()){
            String key = itr.next().toString();
            String value = parameters.getProperty(key);
            if(!variablesDict.containsKey(key)){
                continue;
            }
            Field var = variablesDict.get(key);
            // set the value of the variable
            try {
                String type = var.getType().toString();
                if(type.equals("int")){
                    var.setInt(this,Integer.parseInt(value));
                }else if(type.equals("double")){
                    var.setDouble(this,Double.parseDouble(value));
                }else if(type.equals("long")){
                    var.setLong(this,Long.parseLong(value));
                }else if(type.contains("java.lang.String")){
                    var.set(this,value);
                }else if(type.contains("java.util.List")) {
                    String[] filesNames = value.split(";");
                    List<File> files = new ArrayList<>();
                    for (String filesName : filesNames) {
                        File f = new File(filesName);
                        String extension = filesName.substring(filesName.lastIndexOf('.') + 1);
                        if (f.exists() && (extension.equals(Text.FILE_EXTENSION_WAYPOINTS) || extension.equals(Text.FILE_EXTENSION_KML))) {
                            files.add(f);
                        }
                    }
                    var.set(this, files);
                }else if(type.contains("Formation")){
                    //var.set(this,FlightFormation.Formation.getFormation(value));
                }else if(type.contains("TakeOffAlgorithm")){
                    var.set(this,TakeOffAlgorithm.getAlgorithm(value));
                }else{
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_STORE_PARAMETERS + type);
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        String error = checkSpecificVariables();
        if(error.equals(" ")){
            setSimulationParameters();
            return true;
        }else{
            ArduSimTools.warnGlobal(Text.LOADING_ERROR,"Error in parameter: " + error);
            return false;
        }
    }
    private String checkSpecificVariables(){
        if(groundMinDistance<0){return "groundMinDistance";}
        if(flyingMinDistance<0){return "flyingMindistance";}
        if(landingMinDistance<0){return "landingMinDistance";}
        for(File f :missionFile){
            if(!f.exists()){return "missionFile";}
        }
        return " ";
    }

    private void setSimulationParameters(){
        storeMissionFile(missionFile);
        /*
        FlightFormationTools f = API.getFlightFormationTools();
        f.setGroundFormation(groundFormation.getName(),groundMinDistance);
        API.getCopter(0).getSafeTakeOffHelper().setTakeOffAlgorithm(takeOffStrategy.getName());
        f.setFlyingFormation(flyingFormation.getName(),flyingMinDistance);
        f.setLandingFormationMinimumDistance(landingMinDistance);
        */
    }

    public void storeMissionFile(List<File> selection) {
        final Pair<String, List<Waypoint>[]> missions = API.getGUI(0).loadMissions(selection);
        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        if (missions == null) {
            missionHelper.setMissionsLoaded(null);
        } else {
            int numUAVs = API.getArduSim().getNumUAVs();
            // The master is assigned the first main.java.com.protocols.mission in the list
            List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
            // The master UAV is always in the position 0 of arrays
            missionsFinal[0] = missions.getValue1()[0];
            missionHelper.setMissionsLoaded(missionsFinal);
        }
    }
}
