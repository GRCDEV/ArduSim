package pollution;

import org.javatuples.Pair;

import java.awt.Container;

import javax.swing.*;

import api.pojo.GeoCoordinates;
import sim.logic.SimTools;

public class PollutionHelper {
	/** Opens the configuration dialog of the pollution protocol. */
	public static void openConfigurationDialog() {
		PollutionConfigDialog cofigDialog = new PollutionConfigDialog();
		cofigDialog.setVisible(true);
		//sim.logic.SimTools.println(PollutionParam.startLocation.latitude + ", " + PollutionParam.startLocation.longitude);
	} // TODO
	
	/** Initializes data structures related to the pollution protocol. */
	public static void initializeDataStructures() {} // TODO
	
	/** Gets the initial position of the UAV for the pollution protocol. */
	public static Pair<GeoCoordinates, Double>[] getStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(PollutionParam.startLocation, 0.0);
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
	} // TODO
	
	/** Launch threads related to the pollution protocol. */
	public static void launchThreads() {} // TODO
}
