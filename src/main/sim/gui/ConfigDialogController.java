package main.sim.gui;

import api.API;
import api.ProtocolHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.api.ValidationTools;
import main.api.communications.CommLink;
import main.api.communications.CommLinkObject;
import main.api.communications.WirelessModel;
import main.cpuHelper.CPUUsageThread;
import main.sim.logic.SimParam;
import main.sim.logic.SimTools;
import main.uavController.UAVParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class ConfigDialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    @FXML
    private TextField arducopterPath;
    @FXML
    private TextField speedfileTextfield;
    @FXML
    private TextField startingAltitude;
    @FXML
    private ChoiceBox<String> numUAVsChoiceBox;
    @FXML
    private ChoiceBox<String> protocol;
    @FXML
    private TextField screenRefreshRate;
    @FXML
    private TextField minScreenRedrawDistance;
    @FXML
    private CheckBox arduCopterLogging;
    @FXML
    private CheckBox measureCPU;
    @FXML
    private CheckBox restrictbattery;
    @FXML
    private TextField batteryCapacity;
    @FXML
    private CheckBox verboseLogging;
    @FXML
    private CheckBox verboseStorage;
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



    public ConfigDialogController(ResourceBundle resources, Stage stage){
        this.resources = resources;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        // all strings are set already (mis) using internationalization
        // but special attention is paid to the copter and speed file
        File copterFile = new File(resources.getString("arduCopterPath"));
        if(validateArduCopterPath(copterFile)){
            arducopterPath.setText(copterFile.getAbsolutePath());
        }else{ arducopterPath.setText(""); }

        File speedFile = new File(resources.getString("speedFile"));
        if(validateSpeedFile(speedFile)){
            speedfileTextfield.setText(speedFile.getAbsolutePath());
        }else { speedfileTextfield.setText(""); }

        // booleans still need to be set manually
        arduCopterLogging.setSelected(Boolean.parseBoolean(resources.getString("ArduCopterLogging")));
        measureCPU.setSelected(Boolean.parseBoolean(resources.getString("MeasureCPUUse")));
        restrictbattery.setSelected(Boolean.parseBoolean(resources.getString("RestricBatteryCapacity")));
        verboseLogging.setSelected(Boolean.parseBoolean(resources.getString("VerboseLogging")));
        verboseStorage.setSelected(Boolean.parseBoolean(resources.getString("VerboseStorage")));
        carrierSensing.setSelected(Boolean.parseBoolean(resources.getString("carrierSensing")));
        packetCollisionDetection.setSelected(Boolean.parseBoolean(resources.getString("packetCollisiondetection")));
        collisionDetection.setSelected(Boolean.parseBoolean(resources.getString("collisionDetection")));
        windEnabled.setSelected(Boolean.parseBoolean(resources.getString("wind")));

        // add methods to the buttons
        arducopterPathButton.setOnAction(e->searchArduCopterPath());
        speedFileButton.setOnAction(e->searchSpeedFile());
        okButton.setOnAction(e->{
            boolean valid = ok();
            if(valid){
                Param.simStatus = Param.SimulatorState.CONFIGURING_PROTOCOL;
                Stage stage = (Stage) okButton.getScene().getWindow();
                stage.close();
            }
        });
        saveButton.setOnAction(e->save());

        //fill the comboBoxes
        protocol.setItems(FXCollections.observableArrayList(ArduSimTools.ProtocolNames));
        protocol.getSelectionModel().select(resources.getString("Protocol"));
        communicationModel.setItems(FXCollections.observableArrayList(WirelessModel.getAllModels()));
        communicationModel.getSelectionModel().select(resources.getString("WirelessCommunicationModel"));

        //set restrictions on the textfields
        startingAltitude.setTextFormatter(new TextFormatter<>(doubleFilter));
        fixedRangeDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
        checkPeriod.setTextFormatter(new TextFormatter<>(doubleFilter));
        distanceThreshold.setTextFormatter(new TextFormatter<>(doubleFilter));
        altitudeThreshold.setTextFormatter(new TextFormatter<>(doubleFilter));
        windSpeed.setTextFormatter(new TextFormatter<>(doubleFilter));

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
        batteryCapacity.disableProperty().bind(restrictbattery.selectedProperty().not());
        // TODO set this binding correctly
        //fixedRangeDistance.disableProperty().bind(communicationModel.valueProperty().isNotEqualTo(WirelessModel.FIXED_RANGE));
        checkPeriod.disableProperty().bind(collisionDetection.selectedProperty().not());
        distanceThreshold.disableProperty().bind(collisionDetection.selectedProperty().not());
        altitudeThreshold.disableProperty().bind(collisionDetection.selectedProperty().not());

        windDirection.disableProperty().bind(windEnabled.selectedProperty().not());
        windSpeed.disableProperty().bind(windEnabled.selectedProperty().not());

    }

    private Boolean ok() {
        // for each necessary element check if it is valid
        // if not give message and return
        // if it is valid save it directly (if not saved before)

        ValidationTools validationTools = API.getValidationTools();

        //  arducopterPath
        String validating = arducopterPath.getText();
        if (validationTools.isEmpty(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SITL_ERROR_3);
            return false;
        }
        // speedpath
        validating = speedfileTextfield.getText();
        if (validationTools.isEmpty(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SPEEDS_ERROR_2);
            return false;
        }
        // starting altitude
        validating = startingAltitude.getText();
        if (!validationTools.isValidDouble(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.INITIAL_ALTITUDE_ERROR);
            return false;
        }
        UAVParam.initialAltitude = Double.parseDouble(validating);

        // number of UAVs
        validating = numUAVsChoiceBox.getSelectionModel().getSelectedItem();
        if (validationTools.isEmpty(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.UAVS_NUMBER_ERROR);
            return false;
        }
        Param.numUAVsTemp.set(Integer.parseInt(validating));

        //  refresh rate
        validating = screenRefreshRate.getText();
        if (!validationTools.isValidPositiveInteger(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_1);
            return false;
        }
        int intValue = Integer.parseInt(validating);
        if (intValue < SimParam.MIN_SCREEN_UPDATE_PERIOD || intValue > SimParam.MAX_SCREEN_UPDATE_PERIOD) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.SCREEN_DELAY_ERROR_2);
            return false;
        }
        SimParam.screenUpdatePeriod = intValue;

        // minimal screen redraw distance
        validating = minScreenRedrawDistance.getText();
        if (!validationTools.isValidPositiveDouble(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_1);
            return false;
        }
        double doubleValue = Double.parseDouble(validating);
        if (doubleValue >= SimParam.MIN_SCREEN_MOVEMENT_UPPER_THRESHOLD) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.MIN_SCREEN_MOVEMENT_ERROR_2);
            return false;
        }
        SimParam.minScreenMovement = doubleValue;

        // arducopter logging
        SimParam.arducopterLoggingEnabled = arduCopterLogging.isSelected();

        // measure CPU use
        if (measureCPU.isSelected()) {
            Param.measureCPUEnabled = true;
            new CPUUsageThread().start();
        } else {
            Param.measureCPUEnabled = false;
        }

        // battery
        if (restrictbattery.isSelected()) {
            validating = batteryCapacity.getText();
            if (!validationTools.isValidPositiveInteger(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_1);
                return false;
            }
            intValue = Integer.parseInt(validating);
            if (intValue > UAVParam.VIRT_BATTERY_MAX_CAPACITY) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BATTERY_ERROR_2);
                return false;
            }
            UAVParam.batteryCapacity = Integer.parseInt(batteryCapacity.getText());
        } else {
            UAVParam.batteryCapacity = UAVParam.VIRT_BATTERY_MAX_CAPACITY;
        }
        UAVParam.batteryLowLevel = (int)Math.rint(UAVParam.batteryCapacity * UAVParam.BATTERY_DEPLETED_THRESHOLD);
        if (UAVParam.batteryLowLevel % 50 != 0) {
            UAVParam.batteryLowLevel = (UAVParam.batteryLowLevel / 50 + 1) * 50;	// Multiple of 50 roof value
        }

        // logging
        Param.verboseLogging = verboseLogging.isSelected();
        Param.verboseStore = verboseStorage.isSelected();

        //  Protocol parameter
        ArduSimTools.selectedProtocol = protocol.getSelectionModel().getSelectedItem();
        ProtocolHelper protocolInstance = ArduSimTools.getSelectedProtocolInstance();
        if (protocolInstance == null) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.PROTOCOL_IMPLEMENTATION_NOT_FOUND_ERROR + protocol.getSelectionModel().getSelectedItem());
            return false;
        }
        ArduSimTools.selectedProtocolInstance = ArduSimTools.getSelectedProtocolInstance();

        // carrier sensing
        CommLinkObject.carrierSensingEnabled = carrierSensing.isSelected();
        // packet collision detection
        CommLinkObject.pCollisionEnabled = packetCollisionDetection.isSelected();
        // receiving buffer size
        validating = bufferSize.getText();
        if (!validationTools.isValidPositiveInteger(validating)) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_1);
            return false;
        }
        intValue = Integer.parseInt(validating);
        if (intValue < CommLink.DATAGRAM_MAX_LENGTH) {
            ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.BUFFER_SIZE_ERROR_2);
            return false;
        }
        CommLinkObject.receivingBufferSize = intValue;
        CommLinkObject.receivingvBufferSize = CommLinkObject.V_BUFFER_SIZE_FACTOR * CommLinkObject.receivingBufferSize;
        CommLinkObject.receivingvBufferTrigger = (int)Math.rint(CommLinkObject.BUFFER_FULL_THRESHOLD * CommLinkObject.receivingvBufferSize);

        // wireless communication model
        Param.selectedWirelessModel = WirelessModel.getModelByName(communicationModel.getSelectionModel().getSelectedItem());
        if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
            validating = fixedRangeDistance.getText();
            if (!validationTools.isValidPositiveDouble(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_1);
                return false;
            }
            doubleValue = Double.parseDouble(validating);
            if (doubleValue >= Param.FIXED_MAX_RANGE) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIRELESS_MODEL_ERROR_2);
                return false;
            }
            Param.fixedRange = doubleValue;
        }

        // collision detection
        UAVParam.collisionCheckEnabled= collisionDetection.isSelected();
        if (UAVParam.collisionCheckEnabled) {
            // check period
            validating = checkPeriod.getText();
            if (!validationTools.isValidPositiveDouble(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.COLLISION_PERIOD_ERROR);
                return false;
            }
            UAVParam.collisionCheckPeriod = Double.parseDouble(validating);
            UAVParam.appliedCollisionCheckPeriod = Math.round(UAVParam.collisionCheckPeriod * 1000);
            // distance threshold
            validating = distanceThreshold.getText();
            if (!validationTools.isValidPositiveDouble(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.COLLISION_DISTANCE_THRESHOLD_ERROR);
                return false;
            }
            UAVParam.collisionDistance = Double.parseDouble(validating);
            // altitude threshold
            validating = altitudeThreshold.getText();
            if (!validationTools.isValidPositiveDouble(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING,  Text.COLLISION_ALTITUDE_THRESHOLD_ERROR);
                return false;
            }
            UAVParam.collisionAltitudeDifference = Double.parseDouble(validating);
            // Distance calculus slightly faster than the collision check frequency
            UAVParam.distanceCalculusPeriod = Math.min(CommLinkObject.RANGE_CHECK_PERIOD / 2, Math.round(UAVParam.collisionCheckPeriod * 950));
        }else {
            UAVParam.distanceCalculusPeriod = CommLinkObject.RANGE_CHECK_PERIOD / 2;
        }

        //  Wind parameters
        if (windEnabled.isSelected()) {
            // wind Direction
            validating = windDirection.getText();
            if (!validationTools.isValidNonNegativeInteger(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_DIRECTION_ERROR);
                return false;
            }
            Param.windDirection = Integer.parseInt(validating);

            // wind Speed
            validating = windSpeed.getText();
            if (!validationTools.isValidPositiveDouble(validating)) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_1);
                return false;
            }
            if (Double.parseDouble(validating) < UAVParam.WIND_THRESHOLD) {
                ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.WIND_SPEED_ERROR_2);
                return false;
            }
            Param.windSpeed = Double.parseDouble(validating);
        }else {
            Param.windDirection = Param.DEFAULT_WIND_DIRECTION;
            Param.windSpeed = Param.DEFAULT_WIND_SPEED;
        }

        return true;
    }

    private void save(){
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Property File", "*.properties");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        File file = fileChooser.showSaveDialog(stage);
        String filePath = file.getAbsolutePath();
        if(!filePath.endsWith(".properties")){
            file = new File(filePath + ".properties");
        }
        String[] parameters = {arducopterPath.getText(),speedfileTextfield.getText(),startingAltitude.getText(),
                numUAVsChoiceBox.getValue(),protocol.getValue(), screenRefreshRate.getText(),
                minScreenRedrawDistance.getText(), String.valueOf(arduCopterLogging.isSelected()),
                String.valueOf(measureCPU.isSelected()),String.valueOf(restrictbattery.isSelected()),
                batteryCapacity.getText(),String.valueOf(verboseLogging.isSelected()),
                String.valueOf(verboseStorage.isSelected()),String.valueOf(carrierSensing.isSelected()),
                String.valueOf(packetCollisionDetection.isSelected()),bufferSize.getText(),
                communicationModel.getSelectionModel().getSelectedItem(),fixedRangeDistance.getText(),
                String.valueOf(collisionDetection.isSelected()),checkPeriod.getText(),
                distanceThreshold.getText(),altitudeThreshold.getText(),String.valueOf(windEnabled.isSelected()),
                windDirection.getText(),windSpeed.getText()
        };
        ConfigDialogApp.createPropertiesFile(parameters,file);
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
        if(validateArduCopterPath(sitlPath)) {
            arducopterPath.setText(sitlPath.getAbsolutePath());
        }else{
            arducopterPath.setText("");
        }
    }

    private boolean validateArduCopterPath(File sitlPath){
        if(sitlPath.exists()) {
            if (!sitlPath.canExecute()) {
                ArduSimTools.logGlobal(Text.SITL_ERROR_1);
                ArduSimTools.warnGlobal(Text.SITL_SELECTION_ERROR, Text.SITL_ERROR_1);
                SimParam.sitlPath = null;
                SimParam.paramPath = null;
                return false;
            }
            // Automatically select the parameter file (copter.parm)
            File paramPath = new File(sitlPath.getParent() + File.separator + SimParam.PARAM_FILE_NAME);
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

    private void searchSpeedFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        fileChooser.setTitle(Text.SPEEDS_DIALOG_TITLE);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(Text.SPEEDS_DIALOG_SELECTION, "*."+Text.FILE_EXTENSION_CSV);
        fileChooser.getExtensionFilters().add(extFilter);
        File speedPath = fileChooser.showOpenDialog(stage);
        if(!speedPath.getAbsolutePath().endsWith(Text.FILE_EXTENSION_CSV)) {
            ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_2);
            speedfileTextfield.setText("");
            return;
        }
        UAVParam.initialSpeeds = SimTools.loadSpeedsFile(speedPath.getAbsolutePath());
        if (UAVParam.initialSpeeds == null) {
            ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_1);
            speedfileTextfield.setText("");
            return;
        }
        speedfileTextfield.setText(speedPath.getAbsolutePath());

        // set the combo box for number of UAVs
        int n = -1;
        if (SimParam.sitlPath != null) {
            n = Math.min(UAVParam.initialSpeeds.length, UAVParam.mavPort.length);
        }
        final int numUAVs = n;
        List<String> comboBoxtext = new ArrayList<>();
        for(int i=0;i<numUAVs;i++){
            comboBoxtext.add(""+(i+1));
        }
        ObservableList<String> comboBoxObserv = FXCollections.observableList(comboBoxtext);
        numUAVsChoiceBox.setItems(comboBoxObserv);
        numUAVsChoiceBox.setDisable(false);
    }

    private boolean validateSpeedFile(File speedPath){
        if(speedPath.exists()) {
            if (!speedPath.getAbsolutePath().endsWith(Text.FILE_EXTENSION_CSV)) {
                ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_2);
                return false;
            }
            UAVParam.initialSpeeds = SimTools.loadSpeedsFile(speedPath.getAbsolutePath());
            if (UAVParam.initialSpeeds == null) {
                ArduSimTools.warnGlobal(Text.SPEEDS_SELECTION_ERROR, Text.SPEEDS_ERROR_1);
                return false;
            }
            // set the combo box for number of UAVs
            int n = -1;
            if (SimParam.sitlPath != null) {
                n = Math.min(UAVParam.initialSpeeds.length, UAVParam.mavPort.length);
            }
            final int numUAVs = n;
            List<String> comboBoxtext = new ArrayList<>();
            for (int i = 0; i < numUAVs; i++) {
                comboBoxtext.add("" + (i + 1));
            }
            ObservableList<String> comboBoxObserv = FXCollections.observableList(comboBoxtext);
            numUAVsChoiceBox.setItems(comboBoxObserv);
            numUAVsChoiceBox.setDisable(false);
            return true;
        }else{return false;}
    }
}
