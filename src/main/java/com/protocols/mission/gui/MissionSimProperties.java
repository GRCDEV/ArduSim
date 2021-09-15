package com.protocols.mission.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.api.MissionHelper;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.api.pojo.location.Waypoint;
import com.setup.Text;
import com.uavController.UAVParam;
import org.javatuples.Pair;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class MissionSimProperties {

    // GUI parameters
    public List<File> missionFile;
    public Formation formation;
    public double minDistance;

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
                if(type.contains("java.util.List")) {
                    String[] filesNames = value.split(";");
                    List<File> files = new ArrayList<>();
                    for (String filesName : filesNames) {
                        File f = new File(API.getFileTools().getResourceFolder().toString(), filesName);
                        String extension = filesName.substring(filesName.lastIndexOf('.') + 1);
                        if (f.exists() && (extension.equals(Text.FILE_EXTENSION_WAYPOINTS) || extension.equals(Text.FILE_EXTENSION_KML))) {
                            files.add(f);
                        }
                    }
                    var.set(this, files);
                }else if(type.contains("Formation")){
                    var.set(this, FormationFactory.newFormation(Formation.Layout.valueOf(value.toUpperCase())));
                }else if(type.equals("double")){
                    var.setDouble(this,Double.parseDouble(value));
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
        }
        else{
            ArduSimTools.warnGlobal(Text.LOADING_ERROR,"Error in parameter: " + error);
            return false;
        }
    }

    private String checkSpecificVariables(){
        if(missionFile.size() == 0 ){
            return "missionFile";}
        for(File f :missionFile){
            if(!f.exists()){return "missionFile";}
        }
        return " ";
    }

    private void setSimulationParameters(){
        storeMissionFile(missionFile);
        UAVParam.groundFormation.set(formation);
        formation.init(API.getArduSim().getNumUAVs(),minDistance);

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
