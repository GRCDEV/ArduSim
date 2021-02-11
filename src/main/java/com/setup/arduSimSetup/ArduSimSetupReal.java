package com.setup.arduSimSetup;

import com.api.API;
import com.api.pojo.location.Waypoint;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.cpuHelper.CPUUsageThread;
import com.uavController.TestListener;
import com.uavController.TestTalker;

import java.io.File;
import java.net.SocketException;
import java.util.Calendar;
import java.util.List;

public class ArduSimSetupReal extends ArduSimSetup{

    public ArduSimSetupReal(){ }

    @Override
    protected void loadDependencies() {
        // 1. We need to make constant the parameters shown in the GUI
        Param.numUAVs = 1;	// Always one UAV per Raspberry Pi or whatever the device where the application is deployed
        Param.numUAVsTemp.set(1);

        // 2. Establish the identifier of the UAV
        Param.id = new long[Param.numUAVs];
        Param.id[0] = ArduSimTools.getRealId();

        // 3. Mission file loading
        // The mission is loaded from a file on the same folder as the jar file
        boolean loadMission = ArduSimTools.selectedProtocolInstance.loadMission();
        if (loadMission) {
            List<Waypoint> mission = ArduSimTools.loadMission(parentFolder);
            if (mission == null) {
                ArduSimTools.closeAll(Text.MISSION_NOT_FOUND);
            }
            API.getCopter(0).getMissionHelper().setMissionsLoaded(new List[] {mission});
        }

        // 4. Start threads for waiting to commands to start the setup and start steps of the experiment
        (new TestTalker()).start();
        try {
            TestListener listener = new TestListener();
            listener.start();
        } catch (SocketException e) {
            ArduSimTools.closeAll(Text.BIND_ERROR_1);
        }

        // 5. Start thread to measure the CPU usage
        if (Param.measureCPUEnabled) {
            new CPUUsageThread().start();
        }
    }
    @Override
    protected void setUAVsConfigured(){
        Param.simStatus = Param.SimulatorState.UAVS_CONFIGURED;
        checkUnsafeState(Param.SimulatorState.UAVS_CONFIGURED);
    }
    @Override
    protected void setUAVInSafeMode(){
        API.getCopter(0).cancelRCOverride();
    }
    @Override
    protected void saveResults(String results){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cal.getTimeInMillis() + Param.timeOffset);
        String fileName = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1)
                + "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY)
                + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND) + " " + Text.DEFAULT_BASE_NAME;
        ArduSimTools.storeResults(results, new File(parentFolder, fileName));
    }
    @Override
    protected void shutdown(){
        ArduSimTools.shutdown();
    }
}
