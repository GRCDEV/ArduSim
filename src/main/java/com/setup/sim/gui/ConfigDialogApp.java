package com.setup.sim.gui;

import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimProperties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfigDialogApp extends Application {

    @Override
    public void start(Stage primaryStage){
        Param.simStatus = Param.SimulatorState.CONFIGURING;
        // load the resource file
        SimProperties properties = new SimProperties();
        ResourceBundle resources = properties.readResourceGUI();

        //load the fxml and the controller.
        FXMLLoader loader = null;
        try {
            URL url = new File("src/main/resources/setup/configScene.fxml").toURI().toURL();
            loader = new FXMLLoader(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
