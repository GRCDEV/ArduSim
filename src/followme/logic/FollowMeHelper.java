package followme.logic;

import api.API;
import api.ProtocolHelper;
import api.pojo.CopterParam;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import followme.gui.FollowMeConfigDialog;
import main.api.ArduSim;
import main.api.Copter;
import main.api.formations.FlightFormation;
import main.uavController.UAVParam;
import org.javatuples.Pair;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

import static followme.pojo.State.SETUP_FINISHED;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class FollowMeHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = FollowMeText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public JDialog openConfigurationDialog() {
		return new FollowMeConfigDialog();
	}

	@Override
	public void openConfigurationDialogFX() {

	}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = API.getArduSim().getNumUAVs();
		AtomicInteger[] state = new AtomicInteger[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			state[i] = new AtomicInteger();	// Implicit value State.START, as it is equals to 0
		}
		
		FollowMeParam.state = state;	
	}

	@Override
	public String setInitialState() {
		return FollowMeText.START;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Location2D masterLocation = new Location2D(FollowMeParam.masterInitialLatitude, FollowMeParam.masterInitialLongitude);
		
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = API.getArduSim().getNumUAVs();
		FlightFormation groundFormation = API.getFlightFormationTools().getGroundFormation(numUAVs);
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
		Location2DUTM locationUTM;
		// We put the master UAV in the position 0 of the formation
		// Another option would be to put the master UAV in the center of the ground formation, and the remaining UAVs surrounding it
		startingLocation[0] = Pair.with(masterLocation.getGeoLocation(), FollowMeParam.masterInitialYaw);
		Location2DUTM offsetMasterToCenterUAV = groundFormation.getOffset(0, FollowMeParam.masterInitialYaw);
		for (int i = 1; i < numUAVs; i++) {
			Location2DUTM offsetToCenterUAV = groundFormation.getOffset(i, FollowMeParam.masterInitialYaw);
			locationUTM = new Location2DUTM(masterLocation.getUTMLocation().x - offsetMasterToCenterUAV.x + offsetToCenterUAV.x,
					masterLocation.getUTMLocation().y - offsetMasterToCenterUAV.y + offsetToCenterUAV.y);
			try {
				startingLocation[i] = Pair.with(locationUTM.getGeo(), FollowMeParam.masterInitialYaw);
			} catch (LocationNotReadyException e) {
				e.printStackTrace();
				API.getGUI(0).exit(e.getMessage());
			}
		}
		
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// The following code is valid for ArduCopter version 3.6.12 or lower
		Copter copter = API.getCopter(numUAV);
		if (copter.getMasterSlaveHelper().isMaster()) {
			String version = UAVParam.arducopterVersion.get();
			if(version.contains("3.5")) {
				if (!copter.setParameter(CopterParam.LOITER_SPEED_357,FollowMeParam.masterSpeed)) {
					return false;
				}
			}else if(version.contains("3.6")){
				if (!copter.setParameter(CopterParam.LOITER_SPEED_36X, FollowMeParam.masterSpeed)) {	
					return false;
				}
			}else {
				System.out.println("this version is not checked yet,"
						+ " if ardusim does not run as expected please check"
						+ " src:followme:followmeHelper.java:sendInitialConfiguration");
				if (!copter.setParameter(CopterParam.LOITER_SPEED_36X, FollowMeParam.masterSpeed)) {	
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void startThreads() {
		int numUAVs = API.getArduSim().getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			new FollowMeListenerThread(i).start();
		}
		API.getGUI(0).log(FollowMeText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		ArduSim ardusim = API.getArduSim();
		int numUAVs = ardusim.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (FollowMeParam.state[i].get() < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {
		
		// The master UAV will always be in position 0 in a real UAV, so we also set it in the position 0 for simulations.
		if (API.getCopter(0).getMasterSlaveHelper().isMaster()) {
			new RemoteThread(0).start();
		}
		
	}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
	}

}
