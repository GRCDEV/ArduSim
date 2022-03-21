package com.protocols.magnetics.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.api.MissionHelper;
import com.api.pojo.location.Waypoint;
import com.setup.Text;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import io.dronefleet.mavlink.util.EnumValue;
import org.javatuples.Pair;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class MagneticsSimProperties {

    public static double SWlat;
    public static double SWlon;
    public static double NElat;
    public static double NElon;
    public static double minFlightDistance;
    public static double altitude = 40;
    public static double maxspeed;
    public static double frd; //full repulsion distance
    public static double a;
    public static String repulsionMagnitude;
    public static boolean randomPath;
    public static List<File> missionFile;
    public static int seed;

    private Random random;

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
                }else if(type.contains("java.lang.String")) {
                    var.set(this, value);
                }else if(type.contains("boolean")){
                        var.set(this,Boolean.parseBoolean(value));
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
        if(error == null){
            setSimulationParameters();
            return true;
        }else{
            ArduSimTools.warnGlobal(Text.LOADING_ERROR,"Error in parameter: " + error);
            return false;
        }
    }

    private String specificCheckVariables() {
        if(!randomPath) {
            if (missionFile.size() == 0) {
                return "missionFile is zero";
            }
            for (File f : missionFile) {
                if (!f.exists()) {
                    return "missionFile does not exist";
                }
            }
        }
        return null;
    }

    private void setSimulationParameters() {
        storeMissionFile();
    }

    private void storeMissionFile() {
        Pair<String, List<Waypoint>[]> missions = null;
        if(randomPath) {
            missions = setMissionWaypoints();
        }else{
            missions = API.getGUI(0).loadMissions(missionFile);
        }

        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        // Missions are stored
        missionHelper.setMissionsLoaded(missions.getValue1());
        // The number of UAVs is updated
        API.getArduSim().setNumUAVs(Math.min(missions.getValue1().length,API.getArduSim().getNumUAVs()));
    }

    private Pair<String, List<Waypoint>[]> setMissionWaypoints() {
        random = new Random(seed);
        int numUAVs = API.getArduSim().getNumUAVs();
        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        EnumValue<MavCmd> cmd_waypoint = EnumValue.of(MavCmd.MAV_CMD_NAV_WAYPOINT);
        EnumValue<MavCmd> cmd_takeoff = EnumValue.of(MavCmd.MAV_CMD_NAV_TAKEOFF);
        EnumValue<MavCmd> cmd_land = EnumValue.of(MavCmd.MAV_CMD_NAV_LAND);

        Waypoint start = new Waypoint(0, true, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, cmd_waypoint, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1);
        List<Waypoint>[] al = new ArrayList[numUAVs];
        List<Location2DGeo> initialPositions = new ArrayList<>();
        for (int i = 0; i < al.length; i++) {
            al[i] = new ArrayList<>();
            al[i].add(start);

            boolean farEnoughFromOthersStarting = true ;
            Location2DGeo l1 = null;

            while(l1 == null){
                l1 = getRandomLocation();
                for(Location2DGeo ip: initialPositions){
                    if(l1.getUTM().distance(ip.getUTM()) < 15){
                        farEnoughFromOthersStarting = false;
                    }
                }
                if(farEnoughFromOthersStarting){
                    initialPositions.add(l1);
                }else{
                    l1 = null;
                }
            }

            Location2DGeo l2 = null;

            while (l2 == null){
                l2 = getRandomLocation();
                boolean flightDistanceLongEnough = l1.getUTM().distance(l2.getUTM()) > minFlightDistance;
                if(!flightDistanceLongEnough){
                    l2 = null;
                }
            }


            Waypoint takeoff = new Waypoint(1, false, MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, cmd_takeoff, 0.0, 0.0, 0.0, 0.0, l1.latitude, l1.longitude, altitude, 1);
            al[i].add(takeoff);
            Waypoint w1 = new Waypoint(2, false, MavFrame.MAV_FRAME_GLOBAL, cmd_waypoint, 0.0, 0.0, 0.0, 0.0, l2.latitude, l2.longitude, altitude, 1);
            al[i].add(w1);
            Waypoint last = new Waypoint(3,false,MavFrame.MAV_FRAME_GLOBAL,cmd_land,0,0,0,0,l2.latitude, l2.longitude,30,	0);
            al[i].add(last);
        }
        missionHelper.setMissionsLoaded(al);
        return new Pair<>("fakePath",al);
    }

    private Location2DGeo getRandomLocation(){
        double lat = SWlat + (NElat - SWlat)*random.nextDouble();
        double lon = SWlon + (NElon - SWlon)*random.nextDouble();
        return new Location2DGeo(lat,lon);
    }
}
