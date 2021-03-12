package com.setup.sim.gui;

import com.api.ArduSimTools;
import com.setup.Text;
import com.uavController.UAVParam;

import java.lang.reflect.Field;
import java.util.*;

import static com.uavController.UAVParam.*;

public class MissionKmlSimProperties {
    /** Used to leave the main.java.com.protocols.mission as it is loaded from a Google Earth .kml file. */
    public static final String MISSION_END_UNMODIFIED = "unmodified";// If you change this text, also do it in ardusim.ini
    /** Used to end the main.java.com.protocols.mission with a LAND command when it is loaded from a Google Earth .kml file. */
    public static final String MISSION_END_LAND = "land";
    /** Used to end the main.java.com.protocols.mission with a RTL command when it is loaded from a Google Earth .kml file. */
    public static final String MISSION_END_RTL = "RTL";
    /** Last waypoint behavior for a main.java.com.protocols.mission loaded from a Google Earth .kml file. Please, set the same default value in <i>ardusim.ini</i> file. */
    public static volatile String missionEnd = MISSION_END_UNMODIFIED;
    /** (m) Final altitude when performing RTL. */
    public static volatile double finalAltitudeForRTL = 5.0;
    public static volatile boolean success = false;	//To check if the dialog was closed correctly
    private double minimumWaypointRelativeAltitude;
    /**
     * (s) Hovering time over each waypoint before going on to the next waypoint.
     * <p>Please, modify WPNAV_RADIUS [10-1000 cm] parameter if needed to change where the main.java.com.protocols.mission waypoint is reached.</p> */
    public static volatile int inputMissionDelay = 0;
    public static volatile int distanceToWaypointReached = 200;
    private boolean overrideIncludedAltitudeValues;
    private double waypointsRelativeAltitude;
    private boolean overrideIncludedYawValues;
    private String yawValue;

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
            if(!variablesDict.containsKey(key)){
                //ArduSimTools.warnGlobal(Text.LOADING_ERROR, key + " not recognized");
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
                }else if(type.contains("java.lang.String")){
                    var.set(this,value);
                }else if(type.contains("boolean")){
                    var.set(this,Boolean.parseBoolean(value));
                }else{
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_STORE_PARAMETERS + type);
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        String specificVariablesPassed = checkSpecificVariables();
        if(specificVariablesPassed.equals(" ")){
            setSimulationParameters();
            return true;
        }else{
            ArduSimTools.warnGlobal(Text.LOADING_ERROR,"Error in parameter: " + specificVariablesPassed);
            return false;
        }
    }

    private void setSimulationParameters() {
        UAVParam.minAltitude = minimumWaypointRelativeAltitude;
        UAVParam.overrideAltitude = overrideIncludedAltitudeValues;
        UAVParam.minFlyingAltitude = waypointsRelativeAltitude;
        UAVParam.overrideYaw = overrideIncludedYawValues;

        int yawValueIndex = 0;
        for(int i = 0;i< YAW_VALUES.length;i++){
            String yawBehaviour = YAW_VALUES[i];
            if(yawBehaviour.equals(yawValue)){
                yawValueIndex = i;
                break;
            }
        }
        UAVParam.yawBehavior = yawValueIndex;
        success = true;
    }

    private String checkSpecificVariables() {
        if(finalAltitudeForRTL < 0){return "finalAltitudeForRTL";}
        if(minimumWaypointRelativeAltitude < 0){return "minimumWaypointRelativeAltitude";}
        if(inputMissionDelay <0){return "inputMissionDelay";}
        if(distanceToWaypointReached < 10 || distanceToWaypointReached > 1000){return "distanceToWaypointReached";}
        if(waypointsRelativeAltitude < 0){return "waypointsRelativeAltitude";}
        return " ";
    }
}
