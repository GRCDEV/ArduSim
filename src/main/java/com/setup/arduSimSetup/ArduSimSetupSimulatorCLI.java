package com.setup.arduSimSetup;

import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimProperties;

import java.io.File;
import java.util.Calendar;
import java.util.Properties;

public class ArduSimSetupSimulatorCLI extends ArduSimSetupSimulator{

    public ArduSimSetupSimulatorCLI (){ }

    @Override
    protected void setGeneralParameters(){
        // read .properties file
        SimProperties simProperties = new SimProperties();
        Properties resources = simProperties.readResourceCLI();
        if(resources == null){System.exit(0);}
        simProperties.storeParameters(resources);
        Param.simStatus = Param.SimulatorState.CONFIGURING_PROTOCOL;
    }
    @Override
    protected void loadProtocolConfiguration(){
        ArduSimTools.selectedProtocolInstance.configurationCLI();
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
    }
    @Override
    protected void setTimerExperimentRunning(){
        Param.startTime = System.currentTimeMillis();
    }
    @Override
    protected void clickSetup(){
        Param.simStatus = Param.SimulatorState.SETUP_IN_PROGRESS;
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
    protected void clickStart(){
        Param.simStatus = Param.SimulatorState.TEST_IN_PROGRESS;
    }
    @Override
    protected void shutdown(){
        ArduSimTools.shutdown();
    }
}
