package protocols.shakeup.logic;


import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.ArduSimTools;
import main.api.FileTools;
import main.api.FlightFormationTools;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;
import main.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;
import org.javatuples.Pair;
import protocols.shakeup.pojo.Param;
import protocols.shakeup.pojo.TargetFormation;
import protocols.shakeup.pojo.Text;

import javax.swing.*;
import java.io.File;
import java.util.Map;

import static main.api.formations.FlightFormation.Formation.REGULAR_MATRIX;

public class ShakeupHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {this.protocolString = Text.PROTOCOL_TEXT;}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {return null;}

	@Override
	public void openConfigurationDialogFX() {
		main.Param.simStatus = main.Param.SimulatorState.STARTING_UAVS;
	}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {
		readIniFile("Shakeup_settings.ini");
		FlightFormationTools formationTools = API.getFlightFormationTools();
		
		//set formation: groundformation random, airformation linear
		formationTools.setGroundFormation(FlightFormation.Formation.COMPACT_MESH.getName(), 10);
		API.getCopter(0).getSafeTakeOffHelper().setTakeOffAlgorithm(TakeOffAlgorithm.SIMPLIFIED.getName());
		formationTools.setFlyingFormation(Param.startFormation.getName(), 10);
		//set target formations
		int numUAVs = API.getArduSim().getNumUAVs();
		TargetFormation[] formations = new TargetFormation[Param.formations.length];
		//TODO change so that it works for multiple formations
		for(int i = 0;i < Param.formations.length;i++) {
			formations[i] = new TargetFormation(Param.endFormation.getName(), numUAVs, 10, 0);
		}
		Param.flightFormations = formations;
	}

	@Override
	public String setInitialState() {return null;}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Location2D masterLocation = new Location2D(Param.masterInitialLatitude, Param.masterInitialLongitude);
		
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = API.getArduSim().getNumUAVs();
		FlightFormation groundFormation = API.getFlightFormationTools().getGroundFormation(numUAVs);
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
		Location2DUTM locationUTM;
		// We put the master UAV in the position 0 of the formation
		// Another option would be to put the master UAV in the center of the ground formation, and the remaining UAVs surrounding it
		startingLocation[0] = Pair.with(masterLocation.getGeoLocation(), Param.masterInitialYaw);
		Location2DUTM offsetMasterToCenterUAV = groundFormation.getOffset(0, Param.masterInitialYaw);
		for (int i = 1; i < numUAVs; i++) {
			Location2DUTM offsetToCenterUAV = groundFormation.getOffset(i, Param.masterInitialYaw);
			locationUTM = new Location2DUTM(masterLocation.getUTMLocation().x - offsetMasterToCenterUAV.x + offsetToCenterUAV.x,
					masterLocation.getUTMLocation().y - offsetMasterToCenterUAV.y + offsetToCenterUAV.y);
			try {
				startingLocation[i] = Pair.with(locationUTM.getGeo(), Param.masterInitialYaw);
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
				API.getGUI(0).exit(e.getMessage());
			}
		}
		
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;}

	@Override
	public void startThreads() {
		for (int i = 0; i < API.getArduSim().getNumUAVs(); i++) {
			ShakeupListenerThread listner = new ShakeupListenerThread(i);
			listner.start();
		}
	}

	@Override
	public void setupActionPerformed() {
		while (API.getArduSim().isSetupFinished()) {
			API.getArduSim().sleep(200);
		}

	}

	@Override
	public void startExperimentActionPerformed() {
	}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {return null;}

	@Override
	public String getExperimentConfiguration() {return null;}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}

	public static void readIniFile(String filename) {
		FileTools fileTools = API.getFileTools();
		File iniFile = new File(fileTools.getCurrentFolder() + "/" + filename);

		if(iniFile.exists()) {
			Map<String, String> params = fileTools.parseINIFile(iniFile);
			Param.ALTITUDE_DIFF_SECTORS = Integer.parseInt(params.get("ALTITUDE_DIFF_SECTORS"));
			Param.NUMBER_OF_SECTORS = Integer.parseInt(params.get("NUMBER_OF_SECTORS"));
			
			String algo = params.get("TAKE_OFF_ALGORITHM");
			if(algo.equalsIgnoreCase("RANDOM")) {Param.TAKE_OFF_ALGORITHM = TakeOffAlgorithm.RANDOM;}
			else if(algo.equalsIgnoreCase("SIMPLIFIED")) {Param.TAKE_OFF_ALGORITHM = TakeOffAlgorithm.SIMPLIFIED;}
			
			String startFormation = params.get("START_FORMATION");
			Param.startFormation = stringToFormation(startFormation);
			String endFormation = params.get("END_FORMATION");
			Param.endFormation = stringToFormation(endFormation);
			
			Param.ALTITUDE_MARGIN = Double.parseDouble(params.get("ALTITUDE_MARGIN"));
			ArduSimTools.logGlobal("settings set from shakeupsettings.ini");
		}
	}
	
	private static Formation stringToFormation(String formation) {
		Formation f;
		if(formation.equalsIgnoreCase("LINEAR")) {
			f = Formation.LINEAR;
		}else if(formation.equalsIgnoreCase("CIRCLE")) {
			f = Formation.CIRCLE;
		}else if(formation.equalsIgnoreCase("MATRIX")) {
			f = REGULAR_MATRIX;
		}else if(formation.equalsIgnoreCase("COMPACT_MESH")) {
			f = Formation.COMPACT_MESH;
		}else {
			f = null;
		}
		return f;
	}
}
