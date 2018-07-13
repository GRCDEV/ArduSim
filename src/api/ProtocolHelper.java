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
	
	/** Assign a protocol to this implementation. It is mandatory to do this. Sentence similar to:
	 * <p>this.protocol = ProtocolHelper.Protocol.SOME_PROTOCOL; */
	public abstract void setProtocol();
	
	/** Asserts if it is needed to load a mission, when using protocol on a real UAV. On mission based protocols always load a mission file. */
	public abstract boolean loadMission();
	
	/** Opens a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
	 * <p>When the dialog is accepted please use the following command:
	 * <p>api.Tools.setProtocolConfigured(true); */
	public abstract void openConfigurationDialog();
	
	/** Initilizes data structures used by the protocol. */
	public abstract void initializeDataStructures();
	
	/** Sets the initial state of the protocol shown in the progress dialog. */
	public abstract String setInitialState();
	
	/** Rescales specific data structures of the protocol when the visualization scale changes. */
	public abstract void rescaleDataStructures();
	
	/** Loads resources to be shown on screen. */
	public abstract void loadResources();
	
	/** Rescales for visualization the resources used in protocol, each time the visualization scale changes. */
	public abstract void rescaleShownResources();
	
	/** Draws resources used in the protocol. */
	public abstract void drawResources(Graphics2D g2, BoardPanel p);
	
	/** Sets the initial position where the UAVs will appear in the simulator.
	 * <p>Returns the current Geographic coordinates, and the heading. */
	public abstract Pair<GeoCoordinates, Double>[] setStartingLocation();
	
	/** Sends to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
	 * <p>Must be a blocking method.
	 * <p>Must return true only if all the commands issued to the drone were successful. */
	public abstract boolean sendInitialConfiguration(int numUAV);
	
	/** Launch threads needed by the protocol.
	 * <p>In general, they must wait until a condition is met before doing things, as they start before pressing the Setup or Start buttons.*/
	public abstract void startThreads();
	
	/** Actions to perform when the user presses the Setup button.
	 * <p>Must be a blocking method until the setup process if finished.*/
	public abstract void setupActionPerformed();
	
	/** Starts the experiment.
	 * <p>Must NOT be a blocking method, just should force a protocol thread to move the UAV. */
	public abstract void startExperimentActionPerformed();
	
	/** Analyzes if the experiment must be finished and applies measures to make the UAVs land.
	 * <p>For example, it can be finished when the user presses a button.
	 * <p>Finally, ArduSim stops the experiment when all the UAVs landed.*/
	public abstract void forceExperimentEnd();
	
	/** Optionally, provides general results of the experiment to be appended to text shown on the results dialog.*/
	public abstract String getExperimentResults();
	
	/** Optionally, provides the protocol configuration to be appended to the text shown on the results dialog.*/
	public abstract String getExperimentConfiguration();
	
	/** Optionally, store files with information gathered while applying the protocol at the end of the experiment.
	 * <p>folder. Folder where the files will be stored, the same as the main log file.
	 * <p>baseFileName. Base name that must be prepended to the final file name.*/
	public abstract void logData(String folder, String baseFileName);
	
	/** Opens a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
	 * <p>PCCompanionFrame can be set as the owner of the dialog if needed. */
	public abstract void openPCCompanionDialog(JFrame PCCompanionFrame);
	
}
