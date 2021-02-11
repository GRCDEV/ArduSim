package com.setup.arduSimSetup;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.ArduSim;
import com.api.ValidationTools;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.io.File;
import java.util.Timer;

public class ArduSimSetup {
    protected ArduSim ardusim = API.getArduSim();
    protected File parentFolder = API.getFileTools().getCurrentFolder();
    protected final ValidationTools validationTools = API.getValidationTools();
    protected static Timer timer;

    public ArduSimSetup(){ }

    public void start(){
        beforeSetup();
        clickSetup();
        setup();
        clickStart();
        runExperiment();
        Finishing();
    }

    private void beforeSetup() {
        ArduSimTools.loadAndStoreProtocols();
        ArduSimTools.detectOS();
        ArduSimTools.parseIniFile();

        startPCCompanion();

        System.setProperty("sun.java2d.opengl", "true"); //enable Swing graphics acceleration
        Param.simStatus = Param.SimulatorState.CONFIGURING;

        loadDependencies();

        // 4. Data structures initializing
        ArduSimTools.initializeDataStructures();
        ArduSimTools.selectedProtocolInstance.initializeDataStructures();

        launchProgressDialog();

        // Configuration feedback
        ArduSimTools.logGlobal(Text.PROTOCOL_IN_USE + " " + ArduSimTools.selectedProtocol);

        buildAndStartVirtualUAV();
        // 9. Start UAV controllers, wait for MAVLink link, send basic configuration, and wait for GPS fix
        ArduSimTools.startUAVControllers();
        ArduSimTools.waitMAVLink();

        forceGPS();
        ArduSimTools.sendBasicConfiguration1();
        ArduSimTools.getGPSFix();
        ArduSimTools.sendBasicConfiguration2();	// It requires GPS fix to set the current location for takeoff

        startDistanceAndRangeCalculusThread();

        // 11. Launch the threads of the protocol under test and wait the GUI to be built
        ArduSimTools.logGlobal(Text.LAUNCHING_PROTOCOL_THREADS);
        ArduSimTools.selectedProtocolInstance.startThreads();

        setUAVsConfigured();
    }

    protected void startPCCompanion(){}
    protected void loadDependencies(){}
    protected void launchProgressDialog(){}
    protected void buildAndStartVirtualUAV(){}
    protected void forceGPS(){}
    protected void startDistanceAndRangeCalculusThread(){}
    protected void setUAVsConfigured(){}

    protected void clickSetup(){}

    private void setup() {
        while (Param.simStatus == Param.SimulatorState.UAVS_CONFIGURED) { ardusim.sleep(SimParam.SHORT_WAITING_TIME); }
        checkUnsafeState(Param.SimulatorState.SETUP_IN_PROGRESS);
        setTimerExperimentRunning();
        ArduSimTools.logGlobal(Text.SETUP_START);
        ArduSimTools.selectedProtocolInstance.setupActionPerformed();
        Param.simStatus = Param.SimulatorState.READY_FOR_TEST;
    }
    protected void setTimerExperimentRunning(){}

    protected void clickStart(){}

    protected void runExperiment(){
        while (Param.simStatus == Param.SimulatorState.READY_FOR_TEST) {
            ardusim.sleep(SimParam.SHORT_WAITING_TIME);
        }
        checkUnsafeState(Param.SimulatorState.TEST_IN_PROGRESS);

        // 15. Start the experiment, only if the program is not being closed
        ArduSimTools.logGlobal(Text.TEST_START);
        ArduSimTools.selectedProtocolInstance.startExperimentActionPerformed();

        // 16. Waiting while the experiment is is progress and detecting the experiment end
        int check = 0;
        boolean allStarted = false;
        while (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
            // Detect if all the UAVs have started the experiment
            if (!allStarted) {
                if (ArduSimTools.isTestStarted()) {
                    allStarted = true;
                }
            }
            // Check the battery level periodically
            if (check % UAVParam.BATTERY_PRINT_PERIOD == 0) {
                ArduSimTools.checkBatteryLevel();
            }
            check++;
            // Force the UAVs to land if needed
            ArduSimTools.selectedProtocolInstance.forceExperimentEnd();
            // Detects if all UAVs are on the ground in order to finish the experiment
            if (allStarted) {
                if (ArduSimTools.isTestFinished()) {
                    Param.simStatus = Param.SimulatorState.TEST_FINISHED;
                }
            }

            if (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
                ardusim.sleep(SimParam.LONG_WAITING_TIME);
            }
        }
    }

    private void Finishing() {
        setUAVInSafeMode();
        informTestIsFinished();
        closeVirtualCommunications();
        gatherResults();
        shutdown();
    }

    protected void setUAVInSafeMode(){}
    protected void informTestIsFinished(){
        if (Param.simStatus != Param.SimulatorState.TEST_FINISHED) {
            return;
        }
        // 17. Inform that the experiment has finished, and wait virtual communications to be closed
        ArduSimTools.logGlobal(validationTools.timeToString(Param.startTime, Param.latestEndTime) + " " + Text.TEST_FINISHED);
    }
    protected void closeVirtualCommunications() {}
    protected void gatherResults(){
        // 18. Gather information to show the results dialog
        if(Param.storeData) {
            String res = ArduSimTools.getTestResults();
            String s = ArduSimTools.selectedProtocolInstance.getExperimentResults();
            if (s != null && s.length() > 0) {
                res += "\n" + ArduSimTools.selectedProtocol + ":\n\n";
                res += s;
            }
            res += ArduSimTools.getTestGlobalConfiguration();
            s = ArduSimTools.selectedProtocolInstance.getExperimentConfiguration();
            if (s != null && s.length() > 0) {
                res += "\n\n" + ArduSimTools.selectedProtocol + " " + Text.CONFIGURATION + ":\n";
                res += s;
            }
            saveResults(res);
        }
    }
    protected void saveResults(String results){}
    protected void shutdown(){}

    protected void checkUnsafeState(Param.SimulatorState state){
        if (Param.simStatus != state) {
            API.getCopter(0).cancelRCOverride();
            System.exit(1);
        }
    }
}
