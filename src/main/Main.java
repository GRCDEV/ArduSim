package main;

import main.api.ArduSim;
import main.arduSimSetup.ArduSimSetupPCCompanion;
import main.arduSimSetup.ArduSimSetupReal;
import main.arduSimSetup.ArduSimSetupSimulatorCLI;
import main.arduSimSetup.ArduSimSetupSimulatorGUI;

/** This class contains the main method and the chronological logic followed by the whole application.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Main {
	public static void main(String[] args) {
		ArduSimTools.parseArgs(args);

		if(Param.role == ArduSim.MULTICOPTER){
			new ArduSimSetupReal().start();
		}else if(Param.role == ArduSim.SIMULATOR_GUI){
			new ArduSimSetupSimulatorGUI().start();
		}else if(Param.role == ArduSim.SIMULATOR_CLI){
			new ArduSimSetupSimulatorCLI().start();
		}else if(Param.role == ArduSim.PCCOMPANION){
			new ArduSimSetupPCCompanion().start();
		}
	}
}
