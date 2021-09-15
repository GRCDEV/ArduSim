package com.protocols.followme.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.api.swarm.SwarmParam;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.formations.FormationFactory;
import com.setup.Text;
import com.api.swarm.formations.Formation;
import com.protocols.followme.logic.FollowMeParam;
import com.protocols.followme.pojo.RemoteInput;
import com.uavController.UAVParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static com.setup.Param.numUAVs;

public class FollowmeSimProperties {

    private double latitude;
    private double longitude;
    private double yaw;
    private Formation groundFormation;
    private double groundMinDistance;
    private AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgorithm;
    private Formation  flyingFormation;
    private double flyingMinDistance;
    private static double initialAltitude;
    private double landingMinDistance;
    private File simulatedFlightFile;
    private int masterUAVSpeed;
    private int masterLocationAdvisePeriod;

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
                    var.set(this, new File(API.getFileTools().getResourceFolder() + File.separator + value));
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
        if(specificCheckVariables()){
            setSimulationParameters();
            return true;
        }else{
            return false;
        }
    }

    public void storeSimulatedFlightFile(File f){
        Queue<RemoteInput> data = getData(f);
        if (data != null && !data.isEmpty()) {
            FollowMeParam.masterData = data;
        } else {
            FollowMeParam.masterData = null;
        }
    }

    private Queue<RemoteInput> getData(File file) {
        List<RemoteInput> content = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            return null;
        }
        // Check file length
        if (lines.size() < 1) {
            return null;
        }
        List<String> checkedLines = new ArrayList<>();
        for (String s : lines) {
            String line = s.trim();
            if (line.length() > 0 && line.startsWith("1")) {// type=1 for RC override
                checkedLines.add(line);
            }
        }
        if (checkedLines.size() == 0) {
            return null;
        }
        String[] tokens;
        RemoteInput value;
        for (String checkedLine : checkedLines) {
            tokens = checkedLine.split(",");
            if (tokens.length != 6) {
                return null;
            }
            try {
                value = new RemoteInput(Long.parseLong(tokens[1]),
                        Integer.parseInt(tokens[2]),
                        Integer.parseInt(tokens[3]),
                        Integer.parseInt(tokens[4]),
                        Integer.parseInt(tokens[5]));
            } catch (NumberFormatException e) {
                return null;
            }
            content.add(value);
        }

        if (content.size() == 0) {
            return null;
        }

        // Sort by date
        Collections.sort(content);
        // Reset initial time to zero
        long startingTime = content.get(0).time;
        RemoteInput current;
        for (RemoteInput remoteInput : content) {
            current = remoteInput;
            current.time = current.time - startingTime;
        }

        return new ArrayDeque<>(content);
    }

    private void setSimulationParameters() {
        storeSimulatedFlightFile(simulatedFlightFile);
        FollowMeParam.masterInitialLatitude = latitude;
        FollowMeParam.masterInitialLongitude = longitude;
        FollowMeParam.masterInitialYaw = yaw * Math.PI / 180.0;

        UAVParam.groundFormation.set(groundFormation);
        groundFormation.init(numUAVs,groundMinDistance);
        UAVParam.airFormation.set(flyingFormation);
        flyingFormation.init(numUAVs,flyingMinDistance);
        SwarmParam.assignmentAlgorithm = assignmentAlgorithm;

        FollowMeParam.slavesStartingAltitude = initialAltitude;
        FollowMeParam.masterSpeed = masterUAVSpeed;
        FollowMeParam.sendPeriod = masterLocationAdvisePeriod;
    }

    private boolean specificCheckVariables() {
        if(groundMinDistance <= 0){return false;}
        if(flyingMinDistance <= 0){return false;}
        if(initialAltitude <0){return false;}
        if(landingMinDistance <=0){return false;}
        if(masterUAVSpeed <=0){return false;}
        if(masterLocationAdvisePeriod <=0){return false;}
        return simulatedFlightFile.exists();
    }
}
