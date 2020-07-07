package main.sim.gui;

import api.API;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.ArduSimTools;
import main.Param;
import main.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ConfigDialogApp extends Application {

    private final String resourcesPath = "target/SimulationParam.properties";

    @Override
    public void start(Stage primaryStage) throws Exception {
        Param.simStatus = Param.SimulatorState.CONFIGURING;
        // load the resource file
        File resourceFile = new File(resourcesPath);
        FileInputStream fis;
        if(resourceFile.exists()){
             fis = new FileInputStream(resourceFile);
        }else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
            fileChooser.setTitle("Select a property file");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Property File", "*.properties");
            fileChooser.getExtensionFilters().add(extFilter);
            resourceFile = fileChooser.showOpenDialog(primaryStage);
            if(resourceFile == null || !resourceFile.exists() || !resourceFile.getAbsolutePath().endsWith("properties")){
                ArduSimTools.warnGlobal(Text.LOADING_ERROR , Text.FILE_NOT_FOUND);
                resourceFile = getAndCreateDefaultPropertiesFile();
            }
            fis = new FileInputStream(resourceFile);
        }

        ResourceBundle resources = new PropertyResourceBundle(fis);
        fis.close();

        //load the fxml and the controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("configScene.fxml"));
        ConfigDialogController controller = new ConfigDialogController(resources,primaryStage);
        loader.setController(controller);
        loader.setResources(resources);

        //set the scene
        primaryStage.setTitle("Configuration Dialog");
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }

    public File getAndCreateDefaultPropertiesFile(){
        String[] parameters = {"","","0.0","1","shakeup",
            "500","5.0","false","false","false","3300",
            "false","true",
            "true","true","163840","unrestricted","800",
            "false","0.5","5.0","5.0",
            "false","90","0.5"};
        createPropertiesFile(parameters,new File(API.getFileTools().getCurrentFolder(),"default.properties"));
        return new File(API.getFileTools().getCurrentFolder(),"default.properties");
    }
    public static void createPropertiesFile(String[] parameters, File f){
        try{
            PrintWriter writer = new PrintWriter(f);
            String content = "# Simulation parameters \n" +
                    "arduCopterPath=" + parameters[0] +"\n" +
                    "speedFile="+ parameters[1] +"\n" +
                    "startingAltitude=" + parameters[2] + "\n" +
                    "numberOfUAVS=" + parameters[3] + "\n" +
                    "Protocol=" + parameters[4] + "\n" +

                    "#performance parameters \n" +
                    "screenRefreshRate=" + parameters[5]  + "\n" +
                    "minScreenRedrawDistance=" + parameters[6]  + "\n" +
                    "ArduCopterLogging=" + parameters[7]  + "\n" +
                    "MeasureCPUUse=" + parameters[8]  + "\n" +
                    "RestricBatteryCapacity=" + parameters[9] + "\n" +
                    "batterycapacity=" + parameters[10]  + "\n" +

                    "#General parameters\n"+
                    "VerboseLogging=" + parameters[11]  + "\n" +
                    "VerboseStorage=" + parameters[12]  + "\n" +

                    "#communications parameters\n"+
                    "carrierSensing=" + parameters[13]  + "\n" +
                    "packetCollisiondetection=" + parameters[14]  + "\n"+
                    "bufferSize=" + parameters[15] + "\n" +
                    "WirelessCommunicationModel=" + parameters[16]  + "\n" +
                    "fixedRangeDistance=" + parameters[17]  + "\n"+

                    "#UAV collision detection parameters\n" +
                    "collisionDetection=" + parameters[18]  + "\n" +
                    "checkPeriod=" + parameters[19]  + "\n" +
                    "distanceThreshold=" + parameters[20]  + "\n" +
                    "altitudeThreshold=" + parameters[21]  + "\n" +
                    "wind=" + parameters[22] + "\n" +
                    "windDirection=" + parameters[23]+ "\n" +
                    "windSpeed=" + parameters[24] + "\n";
            writer.write(content);
            writer.close();
        }catch(IOException ignored){}
    }
}
