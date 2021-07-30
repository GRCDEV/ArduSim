package com.protocols.mission.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.api.swarm.formations.Formation;
import com.setup.Param;
import com.setup.Text;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class MissionDialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    private final MissionSimProperties properties;

    @FXML
    private TextField missionFile;
    @FXML
    private Button missionFileButton;
    @FXML
    private Button okButton;
    @FXML
    private TextField minDistance;
    @FXML
    private ChoiceBox<String> formation;

    public MissionDialogController(ResourceBundle resources, MissionSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        missionFile.setDisable(true);
        for(Formation.Layout l: Formation.Layout.values()){
            formation.getItems().add(l.name());
        }
        formation.getSelectionModel().select(resources.getString("formation").toUpperCase());
        minDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        missionFileButton.setOnAction(e -> searchMissionFile());
        okButton.setOnAction(e->{
            if(ok()){
                Platform.setImplicitExit(false); // so that the application does not close
                Param.simStatus = Param.SimulatorState.STARTING_UAVS;
                okButton.getScene().getWindow().hide();
            }else{
                ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
            }
        });
    }

    private boolean ok(){
        Properties p = createProperties();
        return properties.storeParameters(p,resources);
    }

    private Properties createProperties(){
        Properties p = new Properties();
        Field[] variables = this.getClass().getDeclaredFields();
        for(Field var:variables){
            String annotation = var.getAnnotatedType().getType().getTypeName();
            if(annotation.contains("javafx")) {
                try {
                    Method getValue = null;
                    if (annotation.contains("TextField")) {
                        getValue = var.get(this).getClass().getMethod("getCharacters");
                    }
                    if(getValue != null) {
                        String value = String.valueOf(getValue.invoke(var.get(this)));
                        p.setProperty(var.getName(), value);
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
        return p;
    }

    private void searchMissionFile(){

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(API.getFileTools().getSourceFolder() + "/main/resources"));
        fileChooser.setTitle(Text.MISSIONS_DIALOG_TITLE_1);
        FileChooser.ExtensionFilter extFilterKML = new FileChooser.ExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, "*."+Text.FILE_EXTENSION_KML);
        FileChooser.ExtensionFilter extFilterWaypoints = new FileChooser.ExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_2, "*."+Text.FILE_EXTENSION_WAYPOINTS);
        fileChooser.getExtensionFilters().addAll(extFilterKML,extFilterWaypoints);

        List<File> missionPath = fileChooser.showOpenMultipleDialog(stage);
        if(missionPath != null && missionPath.size()>0) {
            String text = "";
            if(missionPath.size() > 1){
                for(File mission : missionPath){
                    text = text + mission.getAbsolutePath() + ";";
                }
            }else{
                text = missionPath.get(0).getAbsolutePath();
            }
            Path absolute = Paths.get(text);
            Path base = API.getFileTools().getResourceFolder();
            missionFile.setText(base.relativize(absolute).toString());
        }else{
            missionFile.setText("");
        }

    }

}
