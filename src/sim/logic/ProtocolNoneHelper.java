package sim.logic;

import java.awt.Graphics2D;

import org.javatuples.Pair;

import api.Copter;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import main.Param;
import sim.board.BoardPanel;
import sim.board.BoardParam;

/** Implementation of the protocol NONE to allow the user to simply follow missions. */

public class ProtocolNoneHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocol = ProtocolHelper.Protocol.NONE;
	}

	@Override
	public boolean loadMission() {
		return true;
	}

	@Override
	public void openConfigurationDialog() {//TODO completar la implementación
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeDataStructures() {
		// TODO Auto-generated method stub

	}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		boolean success = false;
		// Erases, sends and retrieves the planned mission. Blocking procedure
		if (Copter.clearMission(numUAV)
				&& Copter.sendMission(numUAV, Tools.getLoadedMissions()[numUAV])
				&& Copter.retrieveMission(numUAV)
				&& Copter.setCurrentWaypoint(numUAV, 0)) {
			Param.numMissionUAVs.incrementAndGet();
			if (!Param.IS_REAL_UAV) {
				BoardParam.rescaleQueries.incrementAndGet();
			}
			success = true;
		}
		return success;
	}

	@Override
	public void startThreads() {}

	@Override
	public void setupActionPerformed() {}

	@Override
	public void startExperimentActionPerformed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void forceExperimentEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName) {}

}
