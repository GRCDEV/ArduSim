package com.protocols.muscop.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.formations.Formation;
import com.setup.Param;
import com.setup.Text;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class MuscopConfigDialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    private final MuscopSimProperties properties;
    @FXML
    private TextField missionFile;
    @FXML
    private ChoiceBox<String> groundFormation;
    @FXML
    private ChoiceBox<String> numberOfClusters;
    @FXML
    private TextField groundMinDistance;
    @FXML
    private ChoiceBox<String> assignmentAlgorithm;
    @FXML
    private ChoiceBox<String> flyingFormation;
    @FXML
    private TextField flyingMinDistance;
    @FXML
    private TextField landingMinDistance;
    @FXML
    private Button missionFileButton;
    @FXML
    private Button okButton;


    public MuscopConfigDialogController(ResourceBundle resources, MuscopSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        missionFile.setDisable(true);
        missionFileButton.setOnAction(e->searchMissionFile());
        for(Formation.Layout l: Formation.Layout.values()){
            groundFormation.getItems().add(l.name());
            flyingFormation.getItems().add(l.name());
        }

        for(AssignmentAlgorithm.AssignmentAlgorithms a: AssignmentAlgorithm.AssignmentAlgorithms.values()){
            assignmentAlgorithm.getItems().add(a.name());
        }
        groundFormation.getSelectionModel().select(resources.getString("groundFormation"));

        groundMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        assignmentAlgorithm.getSelectionModel().select(resources.getString("assignmentAlgorithm"));

        flyingFormation.getSelectionModel().select(resources.getString("flyingFormation"));

        flyingMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        //numberOfClusters.disableProperty().bind(Bindings.equal(flyingFormation.valueProperty(),FlightFormation.Formation.SPLITUP.getName()).not());
        ArrayList<String> nrClustersString = new ArrayList<>();
        for(int i=0;i<Integer.parseInt(resources.getString("numberOfClusters"));i++){
            nrClustersString.add("" + (i+1));
        }
        numberOfClusters.setItems(FXCollections.observableArrayList(nrClustersString));
        numberOfClusters.getSelectionModel().select(resources.getString("numberOfClusters"));

        landingMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));
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
                    }else if(annotation.contains("ChoiceBox")){
                        getValue = var.get(this).getClass().getMethod("getValue");
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
