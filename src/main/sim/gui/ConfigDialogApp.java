package main.sim.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.Param;
import main.sim.logic.SimProperties;

import java.util.ResourceBundle;

public class ConfigDialogApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
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
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }
}
