package followme.logic;

import static followme.pojo.State.SETUP_FINISHED;

import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.Location2D;
import api.pojo.UTMCoordinates;
import api.pojo.formations.FlightFormation;
import followme.gui.FollowMeConfigDialog;
import sim.board.BoardPanel;
import uavController.UAVParam.ControllerParam;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

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
		int numUAVs = Tools.getNumUAVs();
		
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
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		Location2D masterLocation = Location2D.NewLocation(FollowMeParam.masterInitialLatitude, FollowMeParam.masterInitialLongitude);
		
		// With the ground formation centered on the master, get ground coordinates
		//   As this is simulation, ID and position on the ground are the same for all the UAVs
		int numUAVs = Tools.getNumUAVs();
		FlightFormation groundFormation = FlightFormation.getFormation(FlightFormation.getGroundFormation(),
				numUAVs, FlightFormation.getGroundFormationDistance());
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocation = new Pair[numUAVs];
		UTMCoordinates locationUTM;
		double yawRad = FollowMeParam.masterInitialYaw;
		double yawDeg = yawRad * 180 / Math.PI;
		UTMCoordinates offsetMasterToCenterUAV = groundFormation.getOffset(FollowMeParam.MASTER_POSITION, yawRad);
		for (int i = 0; i < numUAVs; i++) {
			// Master UAV located in the position 0 of the ground formation
			UTMCoordinates offsetToCenterUAV = groundFormation.getOffset(i, yawRad);
			if (i == FollowMeParam.SIMULATION_MASTER_ID) {
				startingLocation[i] = Pair.with(masterLocation.getGeoLocation(), yawDeg);
			} else {
				locationUTM = new UTMCoordinates(masterLocation.getUTMLocation().x - offsetMasterToCenterUAV.x + offsetToCenterUAV.x,
						masterLocation.getUTMLocation().y - offsetMasterToCenterUAV.y + offsetToCenterUAV.y);
				startingLocation[i] = Pair.with(Tools.UTMToGeo(locationUTM), yawDeg);
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
			if (!Copter.setParameter(numUAV, ControllerParam.LOITER_SPEED_357, FollowMeParam.masterSpeed)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void startThreads() {
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			new ListenerThread(i).start();
			new TalkerThread(i).start();
		}
		GUI.log(FollowMeText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		int numUAVs = Tools.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (FollowMeParam.state.get(i) < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				Tools.waiting(FollowMeParam.STATE_CHANGE_TIMEOUT);
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
		int role = Tools.getArduSimRole();
		if (role == Tools.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = Tools.getIdFromPos(FollowMeParam.MASTER_POSITION);
			for (int i = 0; i < FollowMeParam.MAC_ID.length; i++) {
				if (FollowMeParam.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == Tools.SIMULATOR) {
			if (numUAV == FollowMeParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}

}
