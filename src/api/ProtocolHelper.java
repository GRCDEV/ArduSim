package api;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.pojo.GeoCoordinates;
import sim.board.BoardPanel;

public abstract class ProtocolHelper {
	
	// Available protocols
	public static Class<?>[] ProtocolClasses;
	public static volatile String[] ProtocolNames = null;
	public static volatile String noneProtocolName = null;
	
	// Selected protocol
	public static volatile String selectedProtocol;
	public static volatile ProtocolHelper selectedProtocolInstance;
	
	// Protocol identifier
	public String protocolString = null;
	
	/** Assign a protocol name to this implementation. Write something similar to:
	 * <p>this.protocol = "Some protocol name"; */
	public abstract void setProtocol();
	
	/** Used for deployment in real multicopter. Assert if it is needed to load a mission. */
	public abstract boolean loadMission();
	
	/** Open a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (please, avoid heavy calculations).
	 * <p>When the dialog is accepted, please use the following command:
	 * <p>api.Tools.setProtocolConfigured(true); */
	public abstract void openConfigurationDialog();
	
	/** Initialize data structures used by the protocol once the number of multicopters running in the same machine is known:
	 * <p>int numUAVs = api.Tools.getNumUAVs().
	 * <p>numUAVs > 1 in simulation, numUAVs == 1 running in a real UAV. */
	public abstract void initializeDataStructures();
	
	/** Optional: Sets the initial state of the protocol shown in the progress dialog. */
	public abstract String setInitialState();
	
	/** Optional: Rescales specific data structures of the protocol when the visualization scale changes. It is used when the protocol shows additional elements in the main pannel. */
	public abstract void rescaleDataStructures();
	
	/** Optional: Loads resources to be shown on screen. */
	public abstract void loadResources();
	
	/** Optional: Rescales for visualization the resources of the protocol (images) when the visualization scale changes. It is used when the protocol shows additional elements in the main pannel. */
	public abstract void rescaleShownResources();
	
	/** Optional: Periodically draws the resources used in the protocol in the Graphics2D element of the specified BoardPanel. */
	public abstract void drawResources(Graphics2D g2, BoardPanel p);
	
	/** Sets the initial position where the UAVs will appear in the simulator.
	 * <p>Returns the calculated Geographic coordinates (latitude and longitude), and the heading (degrees). */
	public abstract Pair<GeoCoordinates, Double>[] setStartingLocation();
	
	/** Sends to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
	 * <p>Must be a blocking method!
	 * <p>Must return true only if all the commands issued to the drone were successful. */
	public abstract boolean sendInitialConfiguration(int numUAV);
	
	/** Launches threads needed by the protocol.
	 * <p>In general, the threads must wait until a condition is met before doing any action. For example, a UAV thread must wait until the setup or start button is pressed to interact with other multicopters.*/
	public abstract void startThreads();
	
	/** Actions to perform when the user presses the Setup button.
	 * <p>Must be a blocking method until the setup process if finished! */
	public abstract void setupActionPerformed();
	
	/** Starts the experiment.
	 * <p>Must NOT be a blocking method, just should force a protocol thread to move the UAV. */
	public abstract void startExperimentActionPerformed();
	
	/** Optional: Periodically issued to analyze if the experiment must be finished and to apply measures to make the UAVs land.
	 * <p>For example, it can be finished when the user presses a button, the UAV is approaching to a location or ending a mission.
	 * <p>Finally, ArduSim stops the experiment when all the UAVs landed.*/
	public abstract void forceExperimentEnd();
	
	/** Optional: Provides general results of the experiment to be appended to text shown on the results dialog.*/
	public abstract String getExperimentResults();
	
	/** Optional: Provides the protocol configuration to be appended to the text shown on the results dialog.*/
	public abstract String getExperimentConfiguration();
	
	/** Optional: Stores files with information gathered while applying the protocol, at the end of the experiment.
	 * <p>folder. Folder where the files will be stored, the same as the main log file.
	 * <p>baseFileName. Base name that must be prepended to the final file name.*/
	public abstract void logData(String folder, String baseFileName);
	
	/** Optional: Opens a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
	 * <p>Launch dialog information updates in an independent thread to let the dialog construction finish.
	 * <p>PCCompanionFrame can be set as the owner of the dialog if needed. */
	public abstract void openPCCompanionDialog(JFrame PCCompanionFrame);
	
}
