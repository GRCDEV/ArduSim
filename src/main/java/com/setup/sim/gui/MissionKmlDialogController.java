package com.setup.sim.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.uavController.UAVParam;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.ResourceBundle;

public class MissionKmlDialogController {

    private ResourceBundle resources;
    private MissionKmlSimProperties properties;
    private Stage stage;

    @FXML
    private ChoiceBox<String> missionEnd;
    @FXML
    private TextField finalAltitudeForRTL;
    @FXML
    private TextField minimumWaypointRelativeAltitude;
    @FXML
    private TextField inputMissionDelay;
    @FXML
    private TextField distanceToWaypointReached;
    @FXML
    private CheckBox overrideIncludedAltitudeValues;
    @FXML
    private TextField waypointsRelativeAltitude;
    @FXML
    private CheckBox overrideIncludedYawValues;
    @FXML
    private ChoiceBox<String> yawValue;
    @FXML
    private Button okButton;

    public MissionKmlDialogController(ResourceBundle resources, MissionKmlSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        missionEnd.setItems(FXCollections.observableArrayList(MissionKmlSimProperties.MISSION_END_UNMODIFIED,
                MissionKmlSimProperties.MISSION_END_LAND,MissionKmlSimProperties.MISSION_END_RTL));
        missionEnd.getSelectionModel().select(MissionKmlSimProperties.MISSION_END_UNMODIFIED);

        finalAltitudeForRTL.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));
        minimumWaypointRelativeAltitude.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));
        inputMissionDelay.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));
        distanceToWaypointReached.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));
        waypointsRelativeAltitude.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        yawValue.setItems(FXCollections.observableArrayList(UAVParam.YAW_VALUES));
        yawValue.getSelectionModel().select(UAVParam.YAW_VALUES[2]);
        finalAltitudeForRTL.disableProperty().bind(Bindings.equal(missionEnd.valueProperty(),MissionKmlSimProperties.MISSION_END_RTL).not());
        distanceToWaypointReached.disableProperty().bind(Bindings.greaterThan(inputMissionDelay.textProperty(),"0").not());
        waypointsRelativeAltitude.disableProperty().bind(overrideIncludedAltitudeValues.selectedProperty().not());
        yawValue.disableProperty().bind(overrideIncludedYawValues.selectedProperty().not());

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
}
