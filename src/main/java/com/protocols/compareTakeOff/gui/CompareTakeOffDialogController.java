package com.protocols.compareTakeOff.gui;

import com.api.swarm.assignement.AssignmentAlgorithm;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.swarm.formations.Formation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

public class CompareTakeOffDialogController {
    private final ResourceBundle resources;
    private final Stage stage;
    private final CompareTakeOffSimProperties properties;

    @FXML
    private ChoiceBox<String> groundFormation;
    @FXML
    private TextField groundMinDistance;
    @FXML
    private ChoiceBox<String> assignmentAlgorithm;
    @FXML
    private ChoiceBox<String> flyingFormation;
    @FXML
    private TextField flyingMinDistance;
    @FXML
    private ChoiceBox<String> numberOfClusters;
    @FXML
    private Button okButton;

    public CompareTakeOffDialogController(ResourceBundle resources, CompareTakeOffSimProperties properties, Stage stage){
        this.resources = resources;
        this.properties = properties;
        this.stage = stage;
    }

    @FXML
    public void initialize(){
        for(Formation.Layout l: Formation.Layout.values()){
            groundFormation.getItems().add(l.name());
            flyingFormation.getItems().add(l.name());
        }
        groundFormation.getSelectionModel().select(resources.getString("groundFormation").toUpperCase());

        groundMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        ObservableList<String> list = FXCollections.observableArrayList();
        for(AssignmentAlgorithm.AssignmentAlgorithms algo: AssignmentAlgorithm.AssignmentAlgorithms.values()){
            list.add(algo.name());
        }
        assignmentAlgorithm.setItems(list);
        assignmentAlgorithm.getSelectionModel().select(resources.getString("assignmentAlgorithm"));

        flyingFormation.getSelectionModel().select(resources.getString("flyingFormation").toUpperCase());

        flyingMinDistance.setTextFormatter(new TextFormatter<>(ArduSimTools.doubleFilter));

        //numberOfClusters.disableProperty().bind(Bindings.equal(flyingFormation.valueProperty(),FlightFormation.Formation.SPLITUP.getName()).not());
        ArrayList<String> nrClustersString = new ArrayList<>();
        for(int i=0;i<Integer.parseInt(resources.getString("numberOfClusters"));i++){
            nrClustersString.add("" + (i+1));
        }
        numberOfClusters.setItems(FXCollections.observableArrayList(nrClustersString));
        numberOfClusters.getSelectionModel().select(resources.getString("numberOfClusters"));

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
