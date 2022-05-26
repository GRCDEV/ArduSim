package com.setup.sim.logic;

import com.api.API;
import com.api.ArduSimTools;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.api.communications.RangeCalculusThread;
import com.api.communications.WirelessModel;
import com.api.cpuHelper.CPUUsageThread;
import com.setup.Param;
import com.setup.Text;
import com.uavController.UAVParam;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

public class SimProperties {

    // CAUTION: do not change the name of the variables
    // if this is necessary than you have to update the .fxml file
    // and the ConfigDialogController (where resource is used) as well
    private File arducopterFile;
    private File speedFile;
    private double startingAltitude;
    private int numUAVs;
    private String protocol;
    private File protocolParameterFile;
    private int screenRefreshRate;
    private double minScreenRedrawDistance;
    private boolean arduCopterLogging;
    private boolean measureCPU;
    private boolean restrictBattery;
    private int batteryCapacity;
    private boolean verboseLogging;
    private boolean storeData;
    private boolean carrierSensing;
    private boolean packetCollisionDetection;
    private int bufferSize;
    private WirelessModel communicationModel;
    private double fixedRangeDistance;
    private boolean collisionDetection;
    private boolean stopAtCollision;
    private double checkPeriod;
    private double distanceThreshold;
    private double altitudeThreshold;
    private boolean windEnabled;
    private int windDirection;
    private double windSpeed;
    private double simSpeedup;
    private boolean usingOmnetpp;
    private String tcpServerIP;
    private int tcpServerPort;



    public ResourceBundle readResourceGUI(){
        /* return the resource file
            1 Try to load the default resource file SimParam.resourcesFile
            2 If SimParm.resourcesFile does not exist open a filechooser to select another .properties file
            3 If the user presses cancel or opens a file that is not a .properties file a SimulationParam.properties is created and loaded
        */
        // 1
        File resourceFile = SimParam.resourcesFile;
        FileInputStream fis;
        ResourceBundle resources = null;
        try {
            if (!resourceFile.exists()) {
                // 2
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(API.getFileTools().getResourceFolder() + File.separator + "setup"));
                fileChooser.setTitle("Select a property file");
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Property File", "*.properties");
                fileChooser.getExtensionFilters().add(extFilter);
                resourceFile = fileChooser.showOpenDialog(null);
                // 3
                if (resourceFile == null || !resourceFile.exists() || !resourceFile.getAbsolutePath().endsWith("properties")) {
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.FILE_NOT_FOUND);
                    setDefaultParameters();
                    File defaultProperties = new File(API.getFileTools().getResourceFolder() + File.separator +
                            "setup" + File.separator + "SimulationParam.properties");
                    createPropertiesFile(defaultProperties);
                    resourceFile = defaultProperties;
                }
            }
            // load the resource file and store them in this object
            fis = new FileInputStream(resourceFile);
            resources = new PropertyResourceBundle(fis);
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return resources;
    }
    public boolean storeParameters(Properties res){
        // Read a resource file and safe the data from that file to this object
        // make the keyset iteratable
        Iterator<Object> itr = res.keySet().iterator();
        // get all the fields in this class
        Field[] variables = this.getClass().getDeclaredFields();
        Map<String,Field> variablesDict = new HashMap<>();
        for(Field var:variables){variablesDict.put(var.getName(),var);}

        // loop through all the parameters in the file
        while(itr.hasNext()){
            String key = itr.next().toString();
            String value = res.getProperty(key);
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
                }else if(type.equals("boolean")){
                    var.setBoolean(this,Boolean.parseBoolean(value));
                }else if(type.contains("java.lang.String")){
                    var.set(this,value);
                }else if(type.contains("java.io.File")){
                    var.set(this,new File(value));
                }else if(type.split(" ")[1].contains("WirelessModel")) {
                    var.set(this, WirelessModel.getModelByName(value));
                }else if(type.contains("String")){
                    var.set(this,value);
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
    private boolean specificCheckVariables(){
        // this method checks for specific conditions for the variables e.g. positive integer
        if(protocolParameterFile.exists()){
            return protocolParameterFile.getAbsolutePath().endsWith(".properties");
        }else{
            System.out.println("protocolParameterFile does not exist");
        }
        if(screenRefreshRate < SimParam.MIN_SCREEN_UPDATE_PERIOD ||
                screenRefreshRate > SimParam.MAX_SCREEN_UPDATE_PERIOD){return false;}
        if(minScreenRedrawDistance < 0 || minScreenRedrawDistance >= SimParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD){return false;}
        if(batteryCapacity <0 || batteryCapacity > UAVParam.VIRT_BATTERY_MAX_CAPACITY){return false;}
        if(bufferSize < 0 || bufferSize < LowLevelCommLink.DATAGRAM_MAX_LENGTH){return false;}
        if(fixedRangeDistance < 0 || fixedRangeDistance >= Param.FIXED_MAX_RANGE){return false;}
        if(checkPeriod < 0){return false;}
        if(distanceThreshold < 0){return false;}
        if(altitudeThreshold < 0){return false;}
        if(windDirection < 0){return false;}
        if(simSpeedup <= 0){return false;}
        return !(windSpeed < UAVParam.WIND_THRESHOLD);
    }
    public Properties readResourceCLI(){
        try {
            FileInputStream fis = new FileInputStream(SimParam.resourcesFile);
            ResourceBundle resources = new PropertyResourceBundle(fis);
            // create properties from resource file
            Properties properties = new Properties();
            Enumeration<String> keys = resources.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                properties.put(key, resources.getString(key));
            }
            return properties;
        } catch (IOException e) {
            ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PARAMETERS_FILE_NOT_FOUND);
        }
        return null;
    }
    private void setDefaultParameters(){
        // set default parameters intern so that a SimulationParam.properties can be created
        String fs = File.separator;
        arducopterFile = new File("target" + fs + "arducopter");
        speedFile = new File("target" + fs + "speed.csv");
        protocolParameterFile = new File( "src" + fs + "main" + fs + "resources" + fs + "protocols" + fs +
                "mission" + fs + "missionParam.properties");
        startingAltitude = 0.0;
        numUAVs = 5;
        protocol = "Mission";
        screenRefreshRate = 500;
        minScreenRedrawDistance = 5.0;
        arduCopterLogging = false;
        measureCPU = false;
        restrictBattery = false;
        batteryCapacity = 3300;
        verboseLogging = false;
        storeData = true;
        carrierSensing = true;
        packetCollisionDetection = true;
        bufferSize = 163840;
        communicationModel = WirelessModel.NONE;
        fixedRangeDistance = 800;
        collisionDetection = false;
        stopAtCollision = false;
        checkPeriod = 0.5;
        distanceThreshold = 5.0;
        altitudeThreshold = 20.0;
        windEnabled = false;
        windDirection = 90;
        windSpeed = 0.5;
        simSpeedup = 1.0;
        usingOmnetpp = true;
        tcpServerIP="127.0.0.1";
        tcpServerPort=4242;
    }
    public void createPropertiesFile(File f){
        // take the current parameters (intern in SimProperties) and write them to a file
        Field[] variables = this.getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        try{
            for(Field var:variables){
                if(var.get(this) != null) {
                    sb.append(var.getName()).append("=").append(var.get(this)).append("\n");
                }
            }
            PrintWriter writer = new PrintWriter(f);
            writer.write(sb.toString());
            writer.close();
        }catch(IOException | IllegalAccessException ignored){}
    }
    private void setSimulationParameters(){
        if(!validateSpeedFile(speedFile)){System.exit(0);}
        if(!validateArduCopterPath(arducopterFile)){System.exit(0);}

        UAVParam.initialAltitude = startingAltitude;
        Param.numUAVsTemp.set(numUAVs);
        ArduSimTools.selectedProtocol = protocol;
        if(protocolParameterFile.exists()){
            SimParam.protocolParamFile = protocolParameterFile;
        }
        ArduSimTools.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();
        SimParam.screenUpdatePeriod = screenRefreshRate;
        SimParam.minScreenMovement = minScreenRedrawDistance;
        SimParam.arducopterLoggingEnabled = arduCopterLogging;
        Param.measureCPUEnabled = measureCPU;
        if(Param.measureCPUEnabled){new CPUUsageThread().start();}
        if(restrictBattery) {
            UAVParam.batteryCapacity = batteryCapacity;
        }else{
            UAVParam.batteryCapacity = UAVParam.VIRT_BATTERY_MAX_CAPACITY;
        }
        UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
        if (UAVParam.batteryLowLevel % 50 != 0) {
            UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
        }
        Param.verboseLogging = verboseLogging;
        Param.storeData = storeData;
        CommLinkObjectSimulation.init(numUAVs,carrierSensing,packetCollisionDetection,bufferSize);
        Param.selectedWirelessModel = communicationModel;

        if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
            Param.fixedRange = fixedRangeDistance;
        }
        UAVParam.collisionCheckEnabled= collisionDetection;
        UAVParam.stopAtCollision = stopAtCollision;
        if (UAVParam.collisionCheckEnabled) {
            // check period
            UAVParam.collisionCheckPeriod = checkPeriod;
            UAVParam.appliedCollisionCheckPeriod = Math.round(UAVParam.collisionCheckPeriod * 1000);
            // distance threshold
            UAVParam.collisionDistance =  distanceThreshold;
            // altitude threshold
            UAVParam.collisionAltitudeDifference = altitudeThreshold;
            // Distance calculus slightly faster than the collision check frequency
            UAVParam.distanceCalculusPeriod = Math.min(RangeCalculusThread.RANGE_CHECK_PERIOD / 2, Math.round(UAVParam.collisionCheckPeriod * 950));
        }else {
            UAVParam.distanceCalculusPeriod = RangeCalculusThread.RANGE_CHECK_PERIOD / 2;
        }
        if (windEnabled) {
            Param.windDirection = windDirection;
            Param.windSpeed = windSpeed;
        }else {
            Param.windDirection = Param.DEFAULT_WIND_DIRECTION;
            Param.windSpeed = Param.DEFAULT_WIND_SPEED;
        }
        UAVParam.SIM_SPEEDUP = simSpeedup;
        UAVParam.usingOmnetpp = usingOmnetpp;
        if(usingOmnetpp){
            //TODO get from properties file
            UAVParam.broadcastIP = "127.0.0.1";
            UAVParam.broadcastPort = 4242;
        }
    }
    public boolean validateSpeedFile(File f){
        String path = f.getAbsolutePath();
        if(!new File(path).exists() || !path.endsWith(Text.FILE_EXTENSION_CSV)) {
            ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_2);
            return false;
        }
        UAVParam.initialSpeeds = SimTools.loadSpeedsFile(path);
        if (UAVParam.initialSpeeds == null) {
            ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_1);
            return false;
        }
        return true;
    }
    public boolean validateArduCopterPath(File sitlPath){
        if(sitlPath.exists()) {
            if (!sitlPath.canExecute()) {
                ArduSimTools.logGlobal(Text.SITL_ERROR_1);
                ArduSimTools.warnGlobal(Text.SITL_SELECTION_ERROR, Text.SITL_ERROR_1);
                SimParam.sitlPath = null;
                SimParam.paramPath = null;
                return false;
            }
            // Automatically select the parameter file (copter.parm)
            File paramPath = new File(getParent(sitlPath) + SimParam.PARAM_FILE_NAME);
            if (!paramPath.exists()) {
                ArduSimTools.logGlobal(Text.SITL_ERROR_2 + "\n" + SimParam.PARAM_FILE_NAME);
                ArduSimTools.warnGlobal(Text.SITL_SELECTION_ERROR, Text.SITL_ERROR_2 + "\n" + SimParam.PARAM_FILE_NAME);
                SimParam.sitlPath = null;
                SimParam.paramPath = null;
                return false;
            }

            SimParam.sitlPath = sitlPath.getAbsolutePath();
            SimParam.paramPath = paramPath.getAbsolutePath();

            return true;
        }else{return false;}
    }

    private String getParent(File sitlPath) {
        //File paramPath = new File(sitlPath.getParent() + File.separator + SimParam.PARAM_FILE_NAME);
        // for some reason the above does not work so implemented it myself
        String oldValue = sitlPath.getAbsolutePath();
        int lastIndex = oldValue.lastIndexOf(File.separator, oldValue.length() - 2);
        String newValue = oldValue.substring(0, lastIndex + 1);
        return newValue;
    }


}
