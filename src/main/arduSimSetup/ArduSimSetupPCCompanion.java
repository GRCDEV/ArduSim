package main.arduSimSetup;

import api.API;
import main.Param;
import main.pccompanion.gui.PCCompanionGUI;

import javax.swing.*;

public class ArduSimSetupPCCompanion extends ArduSimSetup {

    public ArduSimSetupPCCompanion(){ }

    @Override
    protected void startPCCompanion() {
        // Open the PC Companion GUI en finish this thread
        Param.numUAVs = 1;
        SwingUtilities.invokeLater(() -> PCCompanionGUI.companion = new PCCompanionGUI());
        while(Param.simStatus != Param.SimulatorState.SHUTTING_DOWN){
            API.getArduSim().sleep(1000);
        }
    }
}
