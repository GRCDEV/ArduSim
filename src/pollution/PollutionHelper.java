package pollution;

import org.javatuples.Pair;
import javax.swing.*;

import api.pojo.GeoCoordinates;
import sim.logic.SimTools;

public class PollutionHelper {
	/** Opens the configuration dialog of the pollution protocol. */
	public static void openConfigurationDialog() {
		SimTools.println("openConfigurationDialog() called");
		JDialog dlg = new JDialog((java.awt.Dialog)null, "Pollution Config", true);
		dlg.setSize(400, 400);
		
		// TODO
		dlg.setVisible(true);
	} // TODO
	
	/** Initializes data structures related to the pollution protocol. */
	public static void initializeDataStructures() {} // TODO
	
	/** Gets the initial position of the UAV for the pollution protocol. */
	public static Pair<GeoCoordinates, Double>[] getStartingLocation() {
		Pair<GeoCoordinates, Double> startCoordinates = new Pair<GeoCoordinates, Double>(new GeoCoordinates(0, 0), 5.0);
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[1];
		startCoordinatesArray[0] = startCoordinates;
		return startCoordinatesArray;
	} // TODO
	
	/** Launch threads related to the pollution protocol. */
	public static void launchThreads() {} // TODO
}
