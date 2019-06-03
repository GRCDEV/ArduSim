package followme.logic;

import static followme.pojo.State.SETUP_FINISHED;

import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.CopterParam;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2D;
import api.pojo.location.Location2DUTM;
import followme.gui.FollowMeConfigDialog;
import main.api.ArduSim;
import main.api.ArduSimNotReadyException;
import main.api.formations.FlightFormation;
import main.sim.board.BoardPanel;

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
	public void openConfigurationDialog() {
		new FollowMeConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = API.getArduSim().getNumUAVs();
		
		FollowMeParam.state = new AtomicIntegerArray(numUAVs);	// Implicit value State.START, as it is equals to 0
		
		FollowMeParam.data = new AtomicReference<>();
		FollowMeParam.idNext = new AtomicLongArray(numUAVs);
		FollowMeParam.flyingFormation = new AtomicReferenceArray<>(numUAVs);
	}

	@Override
	public String setInitialState() {
		return FollowMeText.START;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D graphics, BoardPanel panel) {}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		Location2D masterLocation = new Location2D(FollowMeParam.masterInitialLatitude, FollowMeParam.masterInitialLongitude);
		
		// With the ground formation centered on the master, get ground coordinates
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = API.getArduSim().getNumUAVs();
		FlightFormation groundFormation = FlightFormation.getFormation(FlightFormation.getGroundFormation(),
				numUAVs, FlightFormation.getGroundFormationDistance());
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocation = new Pair[numUAVs];
		Location2DUTM locationUTM;
		double yawRad = FollowMeParam.masterInitialYaw;
		double yawDeg = yawRad * 180 / Math.PI;
		Location2DUTM offsetMasterToCenterUAV = groundFormation.getOffset(FollowMeParam.MASTER_POSITION, yawRad);
		for (int i = 0; i < numUAVs; i++) {
			// Master UAV located in the position 0 of the ground formation
			Location2DUTM offsetToCenterUAV = groundFormation.getOffset(i, yawRad);
			if (i == FollowMeParam.SIMULATION_MASTER_ID) {
				startingLocation[i] = Pair.with(masterLocation.getGeoLocation(), yawDeg);
			} else {
				locationUTM = new Location2DUTM(masterLocation.getUTMLocation().x - offsetMasterToCenterUAV.x + offsetToCenterUAV.x,
						masterLocation.getUTMLocation().y - offsetMasterToCenterUAV.y + offsetToCenterUAV.y);
				try {
					startingLocation[i] = Pair.with(locationUTM.getGeo(), yawDeg);
				} catch (ArduSimNotReadyException e) {
					e.printStackTrace();
					API.getGUI(0).exit(e.getMessage());
				}
			}
			// Another option would be to put the master UAV in the center of the ground formation, and the remaining UAVs surrounding it
		}
		
		return startingLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// No need of specific initial configuration
		// The following code is valid for ArduCopter version 3.5.7
		
		if (FollowMeHelper.isMaster(numUAV)) {
			if (!API.getCopter(numUAV).setParameter(CopterParam.LOITER_SPEED_357, FollowMeParam.masterSpeed)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void startThreads() {
		int numUAVs = API.getArduSim().getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			new ListenerThread(i).start();
			new TalkerThread(i).start();
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
				if (FollowMeParam.state.get(i) < SETUP_FINISHED) {
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
		new RemoteThread(FollowMeParam.MASTER_POSITION).start();
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
		// TODO Auto-generated method stub
	}
	
	/** Asserts if the UAV is master. */
	public static boolean isMaster(int numUAV) {
		boolean b = false;
		int role = API.getArduSim().getArduSimRole();
		if (role == ArduSim.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = API.getCopter(FollowMeParam.MASTER_POSITION).getID();
			for (int i = 0; i < FollowMeParam.MAC_ID.length; i++) {
				if (FollowMeParam.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == ArduSim.SIMULATOR) {
			if (numUAV == FollowMeParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}

}
