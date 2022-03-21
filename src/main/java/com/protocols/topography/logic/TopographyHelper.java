package com.protocols.topography.logic;

//Project dependencies
import com.api.API;
import com.api.ArduSimTools;
import com.api.ProtocolHelper;
import com.api.copter.Copter;
import com.api.swarm.formations.Formation;
import com.protocols.topography.gui.TopographyDialogApp;
import com.protocols.topography.gui.TopographySimProperties;
import com.api.pojo.location.Waypoint;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.javatuples.Pair;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CyclicBarrier;

/** Implementation of the protocol Mission to allow the user to simply follow missions. It is based on MBCAP implementation.
 * <p>Developed by: Christian Morales Guerrero, from Trabajo Final de Grado in Universitat Politecnica de Valencia (Valencia, Spain).</p> */

public class TopographyHelper extends ProtocolHelper {

    //DEM object
    private DEM dem;
    private CyclicBarrier barrier;

    /**
     * Assign a protocol name to this implementation. Write something similar to:
     * <p>this.protocolString = "Some protocol name";</p>
     */
    @Override
    public void setProtocol() {this.protocolString = "Topography";}

    /**
     * Assert if it is needed to load a main.java.com.protocols.mission.
     * <p>This method is used when the protocol is deployed in a real multicopter (on simulations, the main.java.com.protocols.mission must be loaded in the dialog built in <i>openConfigurationDialog()</i> method).</p>
     *
     * @return must return true if this UAV must follow a main.java.com.protocols.mission.
     */
    @Override
    public boolean loadMission() {return true;}

    /**
     * Optional: Create a configuration dialog for protocol specific parameters. Otherwise, return null.
     * <p>Please, call <i>dispose</i> method to close the dialog when finishing, and never use JDialog methods like <i>setVisible, setModal, setResizable, setDefaultCloseOperation, setLocationRelativeTo, pack</i>, as they are automatically applied from inside ArduSim. The dialog will be constructed in the GUI thread (please, avoid heavy calculations).</p>
     */
    @Override
    public JDialog openConfigurationDialog() {return null;}

    /**
     * Optional: Create a configuration dialog (using javaFXML) for protocol specific parameters.
     * <p>The dialog will be constructed in the GUI thread (please, avoid heavy calculations</p>
     */
    @Override
    public void openConfigurationDialogFX() {Platform.runLater(()->new TopographyDialogApp().start(new Stage()));}

    /**
     * Optional: when extra parameters are used.
     * In this method the step to load the protocol specific parameters must be called
     */
    @Override
    public void configurationCLI() {
        TopographySimProperties properties = new TopographySimProperties();
        ResourceBundle resources;
        try {
            FileInputStream fis = new FileInputStream(SimParam.protocolParamFile);
            resources = new PropertyResourceBundle(fis);
            fis.close();
            Properties p = new Properties();
            for(String key: resources.keySet()){
                p.setProperty(key,resources.getString(key));
            }
            properties.storeParameters(p,resources);
        } catch (IOException e) {
            e.printStackTrace();
            ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.PROTOCOL_PARAMETERS_FILE_NOT_FOUND );
            System.exit(0);
        }
    }

    /**
     * Initialize data structures used by the protocol. At this point, the number of multicopters running in the same machine is known:
     * <p>int numUAVs = API.getArduSim().getNumUAVs().</p>
     * <p>numUAVs > 1 in simulation, numUAVs == 1 running in a real UAV.</p>
     * <p>We suggest you to initialize data structures as arrays with length depending on <i>numUAVs</i> value.</p>
     */
    @Override
    public void initializeDataStructures() {
        //Create DEM object
        dem = new DEM(TopographySimProperties.ascFile.getAbsolutePath());

        //Initialize CyclicBarrier
        barrier = new CyclicBarrier(API.getArduSim().getNumUAVs());
    }

    /**
     * Set the protocol state to be shown in the progress dialog when ArduSim starts.
     *
     * @return The state to be shown in the progress dialog.
     */
    @Override
    public String setInitialState() { return "Loading DEM"; }

    /**
     * Set the initial location where all the running UAVs will appear (only for simulation).
     *
     * @return The calculated Geographic coordinates (latitude and longitude), and the heading (radians).
     */
    @Override
    public Pair<Location2DGeo, Double>[] setStartingLocation() {
        int numUAVs = API.getArduSim().getNumUAVs();
        Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
        List<Waypoint>[] missions = API.getCopter(0).getMissionHelper().getMissionsLoaded();

        // missions[0].get(0) is somewhere in Africa
        for(int i = 0; i < numUAVs; i++) {
            try {
                startingLocation[i] = new Pair<>(missions[0].get(1).getUTM().getGeo(), 0.0);
                System.out.println(startingLocation[i].getValue0());
            } catch (LocationNotReadyException e) {
                e.printStackTrace();
                return null;
            }
        }
        return startingLocation;
    }

    /**
     * Send to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
     * <p>Must be a blocking method!</p>
     *
     * @param numUAV This specific UAV position in the data arrays (see documentation).
     * @return true if all the commands included in the method end successfully, or you don't include commands at all.
     */
    @Override
    public boolean sendInitialConfiguration(int numUAV) { return true; }

    /**
     * Launch threads needed by the protocol.
     * <p>In general, these threads must wait until a condition is met before doing any action.
     * For example, a UAV thread must wait until the setup or start button is pressed to interact with other multicopters.</p>
     */
    @Override
    public void startThreads() {}

    /**
     * Action automatically performed when the user presses the Setup button.
     * <p>This must be a blocking method until the setup process is finished!</p>
     */
    @Override
    public void setupActionPerformed() {
        //Take off UAVs
        int numUAVs = API.getArduSim().getNumUAVs();
        List<Thread> threads = new ArrayList<>();

        for(int i = 0;i<numUAVs;i++){
            Copter copter = API.getCopter(i);
            Thread t = copter.takeOff(DroneThread.desiredAltitude, null);
            threads.add(t);
            t.start();
        }

        //Wait for take off to be finished
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void joinThreads(List<DroneThread> threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Action automatically performed when the user presses the Start button.
     * <p>This must NOT be a blocking method, just should force a protocol thread to start the protocol.</p>
     */
    @Override
    public void startExperimentActionPerformed() {
        int numUAVs = API.getArduSim().getNumUAVs();
        List<DroneThread> threads = startMission(numUAVs);
        joinThreads(threads);
        landUAVs(numUAVs);
    }

    private List<DroneThread> startMission(int numUAVs) {
        List<DroneThread> threads = new ArrayList<>();
        for(int i = 0; i < numUAVs; i++) {
            DroneThread t = new DroneThread(i,dem,barrier);
            threads.add(t);
            t.start();
        }
        return threads;
    }

    private void landUAVs(int numUAVs) {
        for(int i = 0; i< numUAVs; i++) {
            API.getCopter(i).land();
        }
    }

    /**
     * Optional: Periodically issued to analyze if the experiment must be finished, and to apply measures to make the UAVs land.
     * <p>For example, it can be finished when the user presses a button, the UAV is approaching to a location, or ending a main.java.com.protocols.mission.
     * ArduSim stops the experiment when all the UAVs have landed. Please, see an example in MBCAP protocol.</p>
     */
    @Override
    public void forceExperimentEnd() {
        // When the UAVs are close to the last waypoint a LAND command is issued
        int numUAVs = API.getArduSim().getNumUAVs();
        for (int i = 0; i < numUAVs; i++) {
            API.getCopter(i).getMissionHelper().landIfEnded(UAVParam.LAST_WP_THRESHOLD);
        }
    }

    /**
     * Optional: Provide general results of the experiment to be appended to the text shown on the results dialog.
     *
     * @return String with the results of the protocol to be included on the results dialog.
     */
    @Override
    public String getExperimentResults() {
        return null;
    }

    /**
     * Optional: Provide the protocol configuration to be appended to the text shown on the results dialog.
     *
     * @return String with the configuration of the protocol to be included on the results dialog.
     */
    @Override
    public String getExperimentConfiguration() {
        return null;
    }

    /**
     * Optional: Store at the end of the experiment files with information gathered while applying the protocol.
     *
     * @param folder       Folder where the files will be stored, the same as the main log file.
     * @param baseFileName Base name that must be prepended to the final file name.
     * @param baseNanoTime Base time (ns) when the experiment started. It the arbitrary value provided by <i>System.nanoTime()</i> when the experiment started.
     */
    @Override
    public void logData(String folder, String baseFileName, long baseNanoTime) { }

    /**
     * Optional: Open a configuration dialog for protocol specific parameters, when running ArduSim as a PC Companion.
     * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
     * Please, launch dialog information updates in an independent thread to let the dialog construction finish (see example in MBCAP protocol).</p>
     *
     * @param PCCompanionFrame The Frame of the PC Companion instance. It can be set as the owner of the dialog to be built, if needed.
     */
    @Override
    public void openPCCompanionDialog(JFrame PCCompanionFrame) { }


}