package com.setup.arduSimSetup;

import com.api.ArduSim;
import com.api.ArduSimTools;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.api.communications.RangeCalculusThread;
import com.setup.InitialConfiguration2Thread;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.gui.MainWindow;
import com.setup.sim.logic.CollisionDetector;
import com.setup.sim.logic.DistanceCalculusThread;
import com.setup.sim.logic.SimParam;
import com.setup.sim.logic.SimTools;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DGeo;
import org.javatuples.Pair;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

public class ArduSimSetupSimulator extends ArduSimSetup{

    public ArduSimSetupSimulator(){
    }

    @Override
    protected void loadDependencies() {
        Param.simStatus = Param.SimulatorState.CONFIGURING;
        Param.numUAVs = -1;

        ArduSimTools.locateSITL();
        ArduSimTools.checkAdminPrivileges();
        setMavPort();
        if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
            ArduSimTools.checkImdiskInstalled();
        }
        setGeneralParameters();
        updateNumUAVs();
        ProtocolConfiguration();
        updateNumUAVs();
        lauchMainWindow();
        Param.setupTime = System.currentTimeMillis();
    }

    private void setMavPort() {
        try {
            UAVParam.mavPort = ArduSimTools.getSITLPorts();
            if (UAVParam.mavPort.length < UAVParam.MAX_SITL_INSTANCES) {
                ArduSimTools.warnGlobal(Text.PORT_ERROR, Text.PORT_ERROR_1 + UAVParam.mavPort.length + " " + Text.UAV_ID + "s");
            }
        } catch (InterruptedException | ExecutionException e) {
            ArduSimTools.closeAll(Text.PORT_ERROR_2);
        }
    }
    protected void setGeneralParameters(){}
    private void ProtocolConfiguration() {
        // 2. Opening the configuration dialog of the protocol under test
        if (Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL) {
            loadProtocolConfiguration();
        }
    }
    protected void lauchMainWindow() {}

    protected void loadProtocolConfiguration(){}

    protected void updateNumUAVs() {
        if (Param.numUAVs != Param.numUAVsTemp.get()) {
            Param.numUAVs = Param.numUAVsTemp.get();
        }
    }
    @Override
    protected void buildAndStartVirtualUAV(){
        setImdiskInstalled();
        informUser();
        SimTools.update();

        // 8. Startup of the virtual UAVs
        Pair<Location2DGeo, Double>[] start = ArduSimTools.selectedProtocolInstance.setStartingLocation();
        updateWindImage();
        SimParam.tempFolderBasePath = ArduSimTools.defineTemporaryFolder();
        if (SimParam.tempFolderBasePath == null) {
            ArduSimTools.closeAll(Text.TEMP_PATH_ERROR);
        }
        ArduSimTools.startVirtualUAVs(start);
    }

    private void informUser() {
        if ((Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)
                && !SimParam.userIsAdmin) {
            ArduSimTools.logGlobal(Text.USE_ROOT);
        }
        ArduSimTools.logGlobal(Text.WIRELESS_MODEL_IN_USE + " " + Param.selectedWirelessModel.getName());
        if (Param.windSpeed != 0.0) {
            ArduSimTools.logGlobal(Text.SIMULATED_WIND_SPEED + " " + Param.windSpeed);
        }
    }

    private void setImdiskInstalled() {
        if (SimParam.userIsAdmin
            && ((Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled)
            || Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)) {
        ArduSimTools.logVerboseGlobal(Text.USING_RAM_DRIVE);
        } else {
            ArduSimTools.logVerboseGlobal(Text.USING_HARD_DRIVE);
        }
        if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
            if (SimParam.userIsAdmin && !SimParam.imdiskIsInstalled) {
                ArduSimTools.logGlobal(Text.INSTALL_IMDISK);
            }
            if (!SimParam.userIsAdmin) {
                if (SimParam.imdiskIsInstalled) {
                    ArduSimTools.logGlobal(Text.USE_ADMIN);
                } else {
                    ArduSimTools.logGlobal(Text.INSTALL_IMDISK_USE_ADMIN);
                }
            }
        }

    }

    protected void updateWindImage(){}
    @Override
    protected void forceGPS(){
        ArduSimTools.forceGPS();
    }
    @Override
    protected void startDistanceAndRangeCalculusThread(){
        // 10. Set communications online, and start collision detection if needed
        if (Param.numUAVs > 1) {
            // Calculus of the distance between UAVs
            new DistanceCalculusThread().start();
            // Communications range calculation enable
            new RangeCalculusThread().start();

            // Collision check enable
            if (UAVParam.collisionCheckEnabled) {
                (new CollisionDetector()).start();
                ArduSimTools.logGlobal(Text.COLLISION_DETECTION_ONLINE);
            }
            // Wait the communications to be online
            while (!SimParam.communicationsOnline) {
                ardusim.sleep(SimParam.SHORT_WAITING_TIME);
            }
            ArduSimTools.logGlobal(Text.COMMUNICATIONS_ONLINE);
        }
    }
    @Override
    protected void setUAVsConfigured(){
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
        while (Param.simStatus == Param.SimulatorState.STARTING_UAVS) {
            if (InitialConfiguration2Thread.UAVS_CONFIGURED.get() == Param.numUAVs) {
                Param.simStatus = Param.SimulatorState.UAVS_CONFIGURED;
            }
            ardusim.sleep(SimParam.SHORT_WAITING_TIME);
        }
    }
    @Override
    protected void closeVirtualCommunications(){
        if (Param.numUAVs > 1) {
            ArduSimTools.logGlobal(Text.SHUTTING_DOWN_COMM);
            LowLevelCommLink.close();
        }
        if (Param.role == ArduSim.SIMULATOR_GUI) {
            ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
            SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(Text.TEST_FINISHED));
        }
    }

}
