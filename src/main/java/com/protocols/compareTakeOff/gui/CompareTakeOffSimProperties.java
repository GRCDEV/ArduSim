package com.protocols.compareTakeOff.gui;

import com.api.ArduSimTools;
import com.api.swarm.SwarmParam;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.setup.Text;
import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;
import com.uavController.UAVParam;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static com.setup.Param.numUAVs;

public class CompareTakeOffSimProperties {

    //GUI parameters
    public static Formation groundFormation;
    public static int numberOfClusters = 3;
    public double groundMinDistance;
    public static AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgorithm;
    public static Formation flyingFormation;
    public static double flyingMinDistance;
    public double landingMinDistance;

    public static int timeout = 200;
    public static double masterInitialYaw = 0.0;
    public static double altitude = 15;
    public static double masterInitialLatitude = 39.482615; // (degrees) Latitude for simulations
    public static double masterInitialLongitude = -0.34629; // (degrees) Longitude for simulations

    public static String outputFile = "compareTakeOff.csv";

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
                }else if(type.contains("boolean")){
                    var.set(this,Boolean.parseBoolean(value));
                } else if(type.contains("java.util.List")) {
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

        return " ";
    }

    private void setSimulationParameters(){
        UAVParam.groundFormation.set(groundFormation);
        groundFormation.init(numUAVs,groundMinDistance);

        UAVParam.airFormation.set(flyingFormation);
        flyingFormation.init(numUAVs,flyingMinDistance);

        SwarmParam.assignmentAlgorithm = assignmentAlgorithm;
    }
}
