package pollution;

import org.javatuples.Pair;

import java.awt.Container;

import javax.swing.*;

import api.API;
import api.GUIHelper;
import api.pojo.GeoCoordinates;
import main.Main;
import main.Param;
import main.Text;
import main.Param.SimulatorState;
import sim.logic.SimTools;
import uavController.UAVParam;

public class PollutionHelper {
	/** Opens the configuration dialog of the pollution protocol. */
	public static void openConfigurationDialog() {
		PollutionConfigDialog cofigDialog = new PollutionConfigDialog();
		cofigDialog.setVisible(true);
		//sim.logic.SimTools.println(PollutionParam.startLocation.latitude + ", " + PollutionParam.startLocation.longitude);
	} // TODO
	
	/** Initializes data structures related to the pollution protocol. */
	public static void initializeDataStructures() {
		try {
			PollutionParam.sensor = PollutionParam.isSimulation ? new PollutionSensorSim() : null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PollutionParam.ready = false;
	} // TODO
	
	/** Gets the initial position of the UAV for the pollution protocol. */
	public static Pair<GeoCoordinates, Double>[] getStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(PollutionParam.startLocation, 0.0);
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
	} // TODO
	
	/** Launch threads related to the pollution protocol. */
	public static void launchThreads() {
		new Pollution().start();
	}
	
	public static void setupActionPerformed() {
		Param.simStatus = SimulatorState.READY_FOR_TEST;
	}
	
	public static void startTestActionPerformed() {
		/* Takeoff */
		UAVParam.takeOffAltitude[0] = PollutionParam.altitude;
		if (!API.setMode(0, UAVParam.Mode.GUIDED) || !API.armEngines(0) || !API.doTakeOff(0)) {
			GUIHelper.exit(Text.TAKE_OFF_ERROR_1 + " " + Param.id[0]);
		}
		//API.moveUAV(0, PollutionParam.startLocation, (float) PollutionParam.altitude, 1.0);
		try {
			while (UAVParam.uavCurrentData[0].getZRelative() < 0.95 * PollutionParam.altitude) Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PollutionParam.ready = true;
	}
}
