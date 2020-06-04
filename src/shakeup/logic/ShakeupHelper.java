package shakeup.logic;


import javax.swing.JDialog;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.api.FlightFormationTools;
import main.api.formations.FlightFormation;
import main.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;
import shakeup.pojo.Param;
import shakeup.pojo.Text;
import shakeup.pojo.TargetFormation;

public class ShakeupHelper extends ProtocolHelper {
	
	private ShakeupListenerThread listner;
	
	@Override
	public void setProtocol() {this.protocolString = Text.PROTOCOL_TEXT;}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {return null;}

	@Override
	public void initializeDataStructures() {
		FlightFormationTools formationTools = API.getFlightFormationTools();
		
		//set formation: groundformation random, airformation linear
		formationTools.setGroundFormation(FlightFormation.Formation.RANDOM.getName(), 5);
		API.getCopter(0).getSafeTakeOffHelper().setTakeOffAlgorithm(TakeOffAlgorithm.SIMPLIFIED.getName());
		formationTools.setFlyingFormation(FlightFormation.Formation.LINEAR.getName(), 20);
		
		//set target formations
		int numUAVs = API.getArduSim().getNumUAVs();
		TargetFormation[] formations = new TargetFormation[Param.formations.length];
		for(int i = 0;i < Param.formations.length;i++) {
			formations[i] = new TargetFormation(Param.formations[i], numUAVs, 15, 0);
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
	public boolean sendInitialConfiguration(int numUAV) {return true;}

	@Override
	public void startThreads() {
		for (int i = 0; i < API.getArduSim().getNumUAVs(); i++) {
			listner = new ShakeupListenerThread(i);
			listner.start();
		}
	}

	@Override
	public void setupActionPerformed() {
		while(API.getArduSim().isSetupFinished()) {
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

}
