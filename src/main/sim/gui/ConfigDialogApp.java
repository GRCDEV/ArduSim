package main.sim.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.sim.logic.SimProperties;

import java.io.IOException;
import java.util.ResourceBundle;

public class ConfigDialogApp extends Application {

    @Override
    public void start(Stage primaryStage){
        Param.simStatus = Param.SimulatorState.CONFIGURING;
        // load the resource file
        SimProperties properties = new SimProperties();
        ResourceBundle resources = properties.readResourceGUI();

        //load the fxml and the controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("configScene.fxml"));
        ConfigDialogController controller = new ConfigDialogController(resources,properties,primaryStage);
        loader.setController(controller);
        loader.setResources(resources);

        //set the scene
        primaryStage.setTitle("Configuration Dialog");
        try {
            primaryStage.setScene(new Scene(loader.load()));
        } catch (IOException e) {
            ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
            e.printStackTrace();
        }
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }
}
