package com.protocols.mbcap.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.function.UnaryOperator;

public class MBCAPConfigDialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    private final MBCAPSimProperties properties;

    @FXML
    private TextField missionFile;
    @FXML
    private Button missionFileButton;
    @FXML
    private ChoiceBox<String> numUAVs;
    @FXML
    private TextField beaconInterval;
    @FXML
    private TextField beaconRenewalRate;
    @FXML
    private TextField interSampleTime;
    @FXML
    private TextField minAdvertismentSpeed;
    @FXML
    private TextField beaconExpirationTime;
    @FXML
    private TextField collisionWarningDistance;
    @FXML
    private TextField collisionWarningAltitudeOffset;
    @FXML
    private TextField collisionWarningTimeOffset;
    @FXML
    private TextField riskCheckPeriod;
    @FXML
    private TextField maxNrExpectedConsecutivePacketsLost;
    @FXML
    private TextField GPSExpectedError;
    @FXML
    private TextField hoveringTimeout;
    @FXML
    private TextField overtakeDelayTimeout;
    @FXML
    private TextField defaultFlightModeResumeDelay;
    @FXML
    private TextField checkRiskSameUAVDelay;
    @FXML
    private TextField deadlockBaseTimeout;
    @FXML
    private Button okButton;

    public MBCAPConfigDialogController(ResourceBundle resources, MBCAPSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        // TextFields are already set by the resource
        setDoubleFiltersForTextFields();
        setIntegerFiltersForTextFields();

        missionFileButton.setOnAction(e->searchMissionFile());
        okButton.setOnAction(e->{
            if(ok()){
                Platform.setImplicitExit(false); // so that the application does not close
                Param.simStatus = Param.SimulatorState.STARTING_UAVS;
                okButton.getScene().getWindow().hide();
            }else{
                ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
            }
        });
        setNumUAVsComboBox();
        missionFile.setDisable(true);
    }

    private boolean ok() {
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

    private void searchMissionFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(API.getFileTools().getSourceFolder() + "/main/resources"));
        fileChooser.setTitle(Text.MISSIONS_DIALOG_TITLE_1);
        FileChooser.ExtensionFilter extFilterKML = new FileChooser.ExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, "*."+Text.FILE_EXTENSION_KML);
        // waypoints don`t work in mbcap
        //FileChooser.ExtensionFilter extFilterWaypoints = new FileChooser.ExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_2, "*."+Text.FILE_EXTENSION_WAYPOINTS);
        fileChooser.getExtensionFilters().addAll(extFilterKML);
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
            setNumUAVsComboBox();
            Path absolute = Paths.get(text);
            Path base = API.getFileTools().getResourceFolder();
            missionFile.setText(base.relativize(absolute).toString());
        }else{
            missionFile.setText("");
            numUAVs.setDisable(true);
        }
    }

    private void setNumUAVsComboBox(){
        List<String> comboBoxtext = new ArrayList<>();
        for(int i=0;i<Param.numUAVsTemp.get();i++){
            comboBoxtext.add(""+(i+1));
        }
        ObservableList<String> comboBoxObserv = FXCollections.observableList(comboBoxtext);
        numUAVs.setItems(comboBoxObserv);
        numUAVs.setDisable(false);
        numUAVs.getSelectionModel().select(Param.numUAVsTemp.get()-1);
    }

    private void setIntegerFiltersForTextFields() {
        beaconInterval.setTextFormatter(new TextFormatter<>(integerFilter));
        maxNrExpectedConsecutivePacketsLost.setTextFormatter(new TextFormatter<>(integerFilter));
        deadlockBaseTimeout.setTextFormatter(new TextFormatter<>(integerFilter));
    }

    private void setDoubleFiltersForTextFields() {
        interSampleTime.setTextFormatter(new TextFormatter<>(doubleFilter));
        minAdvertismentSpeed.setTextFormatter(new TextFormatter<>(doubleFilter));
        beaconExpirationTime.setTextFormatter(new TextFormatter<>(doubleFilter));
        collisionWarningDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
        collisionWarningAltitudeOffset.setTextFormatter(new TextFormatter<>(doubleFilter));
        collisionWarningTimeOffset.setTextFormatter(new TextFormatter<>(doubleFilter));
        riskCheckPeriod.setTextFormatter(new TextFormatter<>(doubleFilter));
        GPSExpectedError.setTextFormatter(new TextFormatter<>(doubleFilter));
        hoveringTimeout.setTextFormatter(new TextFormatter<>(doubleFilter));
        overtakeDelayTimeout.setTextFormatter(new TextFormatter<>(doubleFilter));
        defaultFlightModeResumeDelay.setTextFormatter(new TextFormatter<>(doubleFilter));
        checkRiskSameUAVDelay.setTextFormatter(new TextFormatter<>(doubleFilter));
    }

    private final UnaryOperator<TextFormatter.Change> doubleFilter = t -> {
        if (t.isReplaced())
            if(t.getText().matches("[^0-9]"))
                t.setText(t.getControlText().substring(t.getRangeStart(), t.getRangeEnd()));

        if (t.isAdded()) {
            if (t.getControlText().contains(".")) {
                if (t.getText().matches("[^0-9]")) {
                    t.setText("");
                }
            } else if (t.getText().matches("[^0-9.]")) {
                t.setText("");
            }
        }
        return t;
    };

    private final UnaryOperator<TextFormatter.Change> integerFilter = t -> {
        if (t.isReplaced())
            if(t.getText().matches("[^0-9]"))
                t.setText(t.getControlText().substring(t.getRangeStart(), t.getRangeEnd()));

        if (t.isAdded()) {
            if (t.getText().matches("[^0-9]")) {
                t.setText("");
            }
        }
        return t;
    };
}
