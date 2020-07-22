package muscop.gui;

import api.API;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import java.util.ArrayList;
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
    private ChoiceBox<String> takeOffStrategy;
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
        groundFormation.setItems(FXCollections.observableArrayList(FlightFormation.Formation.getAllFormations()));
        groundFormation.getSelectionModel().select(resources.getString("groundFormation"));

        groundMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        takeOffStrategy.setItems(FXCollections.observableArrayList(TakeOffAlgorithm.getAvailableAlgorithms()));
        takeOffStrategy.getSelectionModel().select(resources.getString("takeOffStrategy"));

        flyingFormation.setItems(FXCollections.observableArrayList(FlightFormation.Formation.getAllFormations()));
        flyingFormation.getSelectionModel().select(resources.getString("flyingFormation"));

        flyingMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        numberOfClusters.disableProperty().bind(Bindings.equal(flyingFormation.valueProperty(),FlightFormation.Formation.SPLITUP.getName()).not());
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
        fileChooser.setInitialDirectory(API.getFileTools().getCurrentFolder());
        fileChooser.setTitle(Text.MISSIONS_DIALOG_TITLE_1);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, "*."+Text.FILE_EXTENSION_KML);
        fileChooser.getExtensionFilters().add(extFilter);
        File missionPath = fileChooser.showOpenDialog(stage);
        if(missionPath != null) {
            missionFile.setText(missionPath.getAbsolutePath());
        }else{
            missionFile.setText("");
        }

    }

}
