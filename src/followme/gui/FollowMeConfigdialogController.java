package followme.gui;

import api.API;
import followme.logic.FollowMeText;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.api.formations.FlightFormation;
import main.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class FollowMeConfigdialogController {

    private final ResourceBundle resources;
    private final Stage stage;
    private final FollowmeSimProperties properties;

    @FXML
    private TextField latitude;
    @FXML
    private TextField longitude;
    @FXML
    private TextField yaw;
    @FXML
    private ChoiceBox<String> groundFormation;
    @FXML
    private TextField groundMinDistance;
    @FXML
    private ChoiceBox<String> takeOffStrategy;
    @FXML
    private ChoiceBox<String> flyingFormation;
    @FXML
    private TextField flyingMinDistance;
    @FXML
    private TextField initialAltitude;
    @FXML
    private TextField landingMinDistance;
    @FXML
    private TextField simulatedFlightFile;
    @FXML
    private TextField masterUAVSpeed;
    @FXML
    private TextField masterLocationAdvisePeriod;
    @FXML
    private Button simulatedFlightFileButton;
    @FXML
    private Button okButton;

    public FollowMeConfigdialogController(ResourceBundle resources, FollowmeSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        // TextFields are already set by the resource
        setDoubleFiltersForTextFields();
        setIntegerFiltersForTextFields();

        simulatedFlightFileButton.setOnAction(e->searchSimulatedFlightFile());
        simulatedFlightFile.setDisable(true);

        okButton.setOnAction(e->{
            if(ok()){
                Platform.setImplicitExit(false); // so that the application does not close
                Param.simStatus = Param.SimulatorState.STARTING_UAVS;
                okButton.getScene().getWindow().hide();
            }else{
                ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_LOADING_FXML);
            }
        });

        groundFormation.setItems(FXCollections.observableArrayList(FlightFormation.Formation.getAllFormations()));
        groundFormation.getSelectionModel().select(resources.getString("groundFormation"));

        takeOffStrategy.setItems(FXCollections.observableArrayList(TakeOffAlgorithm.getAvailableAlgorithms()));
        takeOffStrategy.getSelectionModel().select(resources.getString("takeOffStrategy"));

        flyingFormation.setItems(FXCollections.observableArrayList(FlightFormation.Formation.getAllFormations()));
        flyingFormation.getSelectionModel().select(resources.getString("flyingFormation"));
    }

    private void searchSimulatedFlightFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        fileChooser.setTitle(FollowMeText.SIMULATED_DATA_DIALOG_TITLE);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(FollowMeText.DATA_TXT_FILE, "*."+FollowMeText.DATA_TXT_EXTENSION);
        fileChooser.getExtensionFilters().add(extFilter);
        File f = fileChooser.showOpenDialog(stage);
        if(f != null){
            simulatedFlightFile.setText(f.getAbsolutePath());
        }else{
            simulatedFlightFile.setText("");
        }

    }

    private boolean ok(){
        Properties p = createProperties();
        return properties.storeParameters(p,resources);
    }

    private void setIntegerFiltersForTextFields() {
        masterUAVSpeed.setTextFormatter(new TextFormatter<>(integerFilter));
        masterLocationAdvisePeriod.setTextFormatter(new TextFormatter<>(integerFilter));
    }

    private void setDoubleFiltersForTextFields() {
        latitude.setTextFormatter(new TextFormatter<>(doubleFilter));
        longitude.setTextFormatter(new TextFormatter<>(doubleFilter));
        yaw.setTextFormatter(new TextFormatter<>(doubleFilter));
        groundMinDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
        flyingMinDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
        initialAltitude.setTextFormatter(new TextFormatter<>(doubleFilter));
        landingMinDistance.setTextFormatter(new TextFormatter<>(doubleFilter));
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
