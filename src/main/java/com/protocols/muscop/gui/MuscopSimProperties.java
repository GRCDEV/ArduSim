package com.protocols.muscop.gui;

import com.api.API;
import com.api.swarm.SwarmParam;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.formations.FormationFactory;
import com.api.pojo.location.Waypoint;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location3DUTM;
import com.api.ArduSimTools;
import com.setup.Text;
import com.api.MissionHelper;
import com.api.swarm.formations.Formation;
import org.javatuples.Pair;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.setup.Param.numUAVs;

public class MuscopSimProperties {

    // GUI parameters
    public List<File> missionFile;
    public Formation groundFormation;
    public static int numberOfClusters = 3;
    public double groundMinDistance;
    public static AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgorithm;
    public static Formation flyingFormation;
    public double flyingMinDistance;
    public double landingMinDistance;
    public static double altitude = 10;

    // Timeouts
    public static int RECEIVING_TIMEOUT = 50;        // (ms) The port is unlocked after this time when receiving messages
    public static long SENDING_TIMEOUT = 200; 		// (ms) Time between packets sent
    public static long LAND_CHECK_TIMEOUT = 250;  	// (ms) Between checks if the UAV has landed
    public static long STATE_CHANGE_TIMEOUT = 250; 	// (ms) Waiting time in sending messages or reading threads
    public static long MISSION_TIMEOUT = 2000; 		// (ms) Timeout to wait after the mission is received and the master UAV changes its state
    public static long TTL=5000; 					// (ms) Time to life for a UAV.
    public static int RECEIVETIMEOUT = 200;			// (ms) Timeout for link.receiveMessage()
    // Maximum number of waypoints that fit in a datagram.
    public static int MAX_WAYPOINTS = 61;	// main.java.main.java.api.CommLink.DATAGRAM_MAX_LENGTH - 2 - 2 - 3x8xn >= 0

    // (rad) Master UAV yaw or heading (first mission segment orientation in simulation).
    public static volatile double formationYaw;
    // Mission sent from master to slaves (center mission).
    public static AtomicReference<Location3DUTM[]> missionSent = new AtomicReference<>();// Array containing the mission sent to the slaves

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
                    for (String fileName : filesNames) {
                        fileName = API.getFileTools().getResourceFolder() + File.separator + fileName;
                        File f = new File(fileName);
                        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                        if (f.exists() && (extension.equals(Text.FILE_EXTENSION_WAYPOINTS) || extension.equals(Text.FILE_EXTENSION_KML))) {
                            files.add(f);
                        }
                    }
                    var.set(this, files);
                }else if(type.contains("Formation")){
                    var.set(this, FormationFactory.newFormation(Formation.Layout.valueOf(value.toUpperCase())));
                }else if(type.contains("AssignmentAlgorithms")){
                    var.set(this, AssignmentAlgorithm.AssignmentAlgorithms.valueOf(value));
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
        UAVParam.groundFormation.set(groundFormation);
        groundFormation.init(numUAVs,groundMinDistance);

        UAVParam.airFormation.set(flyingFormation);
        flyingFormation.init(numUAVs,flyingMinDistance);

        SwarmParam.assignmentAlgorithm = assignmentAlgorithm;
    }

    public void storeMissionFile(List<File> selection) {
        final Pair<String, List<Waypoint>[]> missions = API.getGUI(0).loadMissions(selection);
        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        if (missions == null) {
            missionHelper.setMissionsLoaded(null);
        } else {
            int numUAVs = API.getArduSim().getNumUAVs();
            // The master is assigned the first mission in the list
            List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
            // The master UAV is always in the position 0 of arrays
            missionsFinal[0] = missions.getValue1()[0];
            missionHelper.setMissionsLoaded(missionsFinal);
        }
    }
}
