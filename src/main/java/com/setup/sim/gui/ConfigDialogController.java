package com.setup.sim.gui;

import com.api.API;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.communications.WirelessModel;
import com.setup.sim.logic.SimParam;
import com.setup.sim.logic.SimProperties;
import com.uavController.UAVParam;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

public class ConfigDialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    private final SimProperties properties;
    @FXML
    private TextField arducopterFile;
    @FXML
    private TextField speedFile;
    @FXML
    private TextField startingAltitude;
    @FXML
    private ChoiceBox<String> numUAVs;
    @FXML
    private ChoiceBox<String> protocol;
    @FXML
    private TextField protocolParameterFile;
    @FXML
    private Button protocolParametersPathButton;
    @FXML
    private TextField screenRefreshRate;
    @FXML
    private TextField minScreenRedrawDistance;
    @FXML
    private TextField simSpeedup;
    @FXML
    private CheckBox arduCopterLogging;
    @FXML
    private CheckBox measureCPU;
    @FXML
    private CheckBox restrictBattery;
    @FXML
    private TextField batteryCapacity;
    @FXML
    private CheckBox verboseLogging;
    @FXML
    private CheckBox storeData;
    @FXML
    private CheckBox usingOmnetpp;
    @FXML
    private CheckBox carrierSensing;
    @FXML
    private CheckBox packetCollisionDetection;
    @FXML
    private TextField bufferSize;
    @FXML
    private ChoiceBox<String> communicationModel;
    @FXML
    private TextField fixedRangeDistance;
    @FXML
    private CheckBox collisionDetection;
    @FXML
    private CheckBox stopAtCollision;
    @FXML
    private TextField checkPeriod;
    @FXML
    private TextField distanceThreshold;
    @FXML
    private TextField altitudeThreshold;
    @FXML
    private CheckBox windEnabled;
    @FXML
    private TextField windDirection;
    @FXML
    private ImageView windArrow;
    @FXML
    private TextField windSpeed;
    @FXML
    private Button arducopterPathButton;
    @FXML
    private Button speedFileButton;
    @FXML
    private Button okButton;
    @FXML
    private Button saveButton;

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



    public ConfigDialogController(ResourceBundle resources, SimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        // all strings are set already (mis) using internationalization
        updateSpeedFile(new File(speedFile.getText()));
        // booleans still need to be set manually
        try {
            arduCopterLogging.setSelected(Boolean.parseBoolean(resources.getString("arduCopterLogging")));
            measureCPU.setSelected(Boolean.parseBoolean(resources.getString("measureCPU")));
            restrictBattery.setSelected(Boolean.parseBoolean(resources.getString("restrictBattery")));
            verboseLogging.setSelected(Boolean.parseBoolean(resources.getString("verboseLogging")));
            storeData.setSelected(Boolean.parseBoolean(resources.getString("storeData")));
            usingOmnetpp.setSelected(Boolean.parseBoolean(resources.getString("usingOmnetpp")));
            carrierSensing.setSelected(Boolean.parseBoolean(resources.getString("carrierSensing")));
            packetCollisionDetection.setSelected(Boolean.parseBoolean(resources.getString("packetCollisionDetection")));
            collisionDetection.setSelected(Boolean.parseBoolean(resources.getString("collisionDetection")));
            stopAtCollision.setSelected(Boolean.parseBoolean(resources.getString("stopAtCollision")));
            windEnabled.setSelected(Boolean.parseBoolean(resources.getString("windEnabled")));
        }catch(MissingResourceException e){
            ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
        }
        // add methods to the buttons
        arducopterPathButton.setOnAction(e->searchArduCopterPath());
        speedFileButton.setOnAction(e->searchSpeedFile());
        protocolParametersPathButton.setOnAction(e->searchProtocolParameterFile());
        okButton.setOnAction(e->{
            if(ok()){
                Platform.setImplicitExit(false); // so that the application does not close
                Param.simStatus = Param.SimulatorState.CONFIGURING_PROTOCOL;
                okButton.getScene().getWindow().hide();
            }else{
                ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
            }
        });
        saveButton.setOnAction(e->save());

        //fill the comboBoxes
        protocol.setItems(FXCollections.observableArrayList(ArduSimTools.ProtocolNames));
        protocol.getSelectionModel().select(resources.getString("protocol"));
        communicationModel.setItems(FXCollections.observableArrayList(WirelessModel.getAllModels()));
        communicationModel.getSelectionModel().select(resources.getString("communicationModel"));

        //set restrictions on the textfields
        startingAltitude.setTextFormatter(new TextFormatter<>(doubleFilter));
        fixedRangeDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
        checkPeriod.setTextFormatter(new TextFormatter<>(doubleFilter));
        distanceThreshold.setTextFormatter(new TextFormatter<>(doubleFilter));
        altitudeThreshold.setTextFormatter(new TextFormatter<>(doubleFilter));
        windSpeed.setTextFormatter(new TextFormatter<>(doubleFilter));
        simSpeedup.setTextFormatter(new TextFormatter<>(doubleFilter));

        minScreenRedrawDistance.setTextFormatter(new TextFormatter<>(integerFilter));
        bufferSize.setTextFormatter(new TextFormatter<>(integerFilter));
        windDirection.setTextFormatter(new TextFormatter<>(integerFilter));
        batteryCapacity.setTextFormatter(new TextFormatter<>(integerFilter));

        //rotate the picture
        windDirection.textProperty().addListener((observable, oldvalue, newValue) -> {
            double rotation;
            // try to get the new rotation
            try {
                rotation = Double.parseDouble(newValue);
            }catch (NumberFormatException ignored){
                // if it fails use the old one
                try{ rotation = Double.parseDouble(oldvalue);}catch (NumberFormatException ignored2){
                    //if that fails to use rotation 0
                    rotation=0;
                }
            }
            windArrow.setRotate(rotation);
        });

        // disable textfields if boolean is false
        batteryCapacity.disableProperty().bind(restrictBattery.selectedProperty().not());
        fixedRangeDistance.disableProperty().bind(communicationModel.valueProperty().isNotEqualTo(WirelessModel.FIXED_RANGE.getName()));
        checkPeriod.disableProperty().bind(collisionDetection.selectedProperty().not());
        distanceThreshold.disableProperty().bind(collisionDetection.selectedProperty().not());
        altitudeThreshold.disableProperty().bind(collisionDetection.selectedProperty().not());
        stopAtCollision.disableProperty().bind(collisionDetection.selectedProperty().not());

        windDirection.disableProperty().bind(windEnabled.selectedProperty().not());
        windSpeed.disableProperty().bind(windEnabled.selectedProperty().not());


    }

    private Boolean ok() {
        Properties p = createProperties();
        return properties.storeParameters(p);
    }

    private void save(){
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Property File", "*.properties");
        fileChooser.getExtensionFilters().add(extFilter);
        String fs = File.separator;
        fileChooser.setInitialDirectory(new File(API.getFileTools().getSourceFolder().toString() + fs + "main" + fs +
                "resources" + fs + "setup"));
        fileChooser.setInitialFileName("SimulationParam.properties");
        File file = fileChooser.showSaveDialog(stage);
        String filePath = file.getAbsolutePath();
        if(!filePath.endsWith(".properties")){
            file = new File(filePath + ".properties");
        }
        if(properties.storeParameters(createProperties())){
            properties.createPropertiesFile(file);
        }else{
            ArduSimTools.warnGlobal(Text.SAVE_ERROR, Text.GUI_NOT_COMPLETE);
        }
    }

    private void searchArduCopterPath() {
        //open a filechooser, set some filters and open a dialog to select the file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        fileChooser.setTitle(Text.BASE_PATH_DIALOG_TITLE);
        if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(Text.BASE_PATH_DIALOG_SELECTION, Text.BASE_PATH_DIALOG_EXTENSION));
        }
        File sitlPath = fileChooser.showOpenDialog(stage);
        if(properties.validateArduCopterPath(sitlPath)) {
            Path absolute = Paths.get(sitlPath.getAbsolutePath());
            Path base = API.getFileTools().getArdusimFolder();
            arducopterFile.setText(base.relativize(absolute).toString());
        }else{
            arducopterFile.setText("");
        }
    }

    private void searchSpeedFile() {
        // used when user presses button
        // Create a filechooser with some restrictions and invote updateSpeedFile
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        fileChooser.setTitle(Text.SPEEDS_DIALOG_TITLE);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(Text.SPEEDS_DIALOG_SELECTION, "*."+Text.FILE_EXTENSION_CSV);
        fileChooser.getExtensionFilters().add(extFilter);
        File speedPath = fileChooser.showOpenDialog(stage);
        updateSpeedFile(speedPath);
    }
    public void searchProtocolParameterFile(){
        // used when user presses button
        // Create a filechooser with some restrictions and invote updateSpeedFile
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(API.getFileTools().getSourceFolder() + "/main/resources/protocols"));
        fileChooser.setTitle(Text.PROTOCOL_DIALOG_TITLE);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(Text.PROTOCOL_DIALOG_SELECTION , "*."+Text.PROPERITES_EXTENSTION);
        fileChooser.getExtensionFilters().add(extFilter);
        File parameterFile = fileChooser.showOpenDialog(stage);
        if(parameterFile.exists()){
            Path absolute = Paths.get(parameterFile.getAbsolutePath());
            Path base = API.getFileTools().getArdusimFolder();
            protocolParameterFile.setText(base.relativize(absolute).toString());
        }else{
            protocolParameterFile.setText("");
        }
    }

    private void updateSpeedFile(File speedPath){
        // Use properties.speedFile to do the logic
        // If that went well set the text and update the combobox numUAVs
        if(properties.validateSpeedFile(speedPath)){
            // set the combo box for number of UAVs
            int n = -1;
            if (SimParam.sitlPath != null) {
                n = Math.min(UAVParam.initialSpeeds.length, UAVParam.mavPort.length);
            }
            final int numUAVs_ = n;
            List<String> comboBoxtext = new ArrayList<>();
            for(int i=0;i<numUAVs_;i++){
                comboBoxtext.add(""+(i+1));
            }
            ObservableList<String> comboBoxObserv = FXCollections.observableList(comboBoxtext);
            numUAVs.setItems(comboBoxObserv);
            numUAVs.setDisable(false);
            numUAVs.getSelectionModel().select(resources.getString("numUAVs"));
        }else{
            speedFile.setText("");
        }
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
                    } else if (annotation.contains("CheckBox")){
                        getValue = var.get(this).getClass().getMethod("isSelected");
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
