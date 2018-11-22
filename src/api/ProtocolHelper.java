package api;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.pojo.GeoCoordinates;
import sim.board.BoardPanel;

/** Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public abstract class ProtocolHelper {
	
	// Available protocols (Internal use by ArduSim)
	public static Class<?>[] ProtocolClasses;
	public static volatile String[] ProtocolNames = null;
	public static volatile String noneProtocolName = null;
	// Selected protocol (Internal use by ArduSim)
	public static volatile String selectedProtocol;
	public static volatile ProtocolHelper selectedProtocolInstance;
	
	// Protocol identifier
	public String protocolString = null;
	
	/** Assign a protocol name to this implementation. Write something similar to:
	 * <p>this.protocol = "Some protocol name";</p> */
	public abstract void setProtocol();
	
	/**
	 * Assert if it is needed to load a mission.
	 * <p>This method is used when the protocol is deployed in a real multicopter (on simulations, the mission must be loaded in the dialog built in <i>openConfigurationDialog()</i> method).</p>
	 * @return true if this UAV must follow a mission.
	 */
	public abstract boolean loadMission();
	
	/**
	 * Open a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (please, avoid heavy calculations).
	 * When the dialog is accepted, please use the following command:</p>
	 * <p>api.Tools.setProtocolConfigured(true);</p> */
	public abstract void openConfigurationDialog();
	
	/**
	 * Initialize data structures used by the protocol, once the number of multicopters running in the same machine is known:
	 * <p>int numUAVs = api.Tools.getNumUAVs().</p>
	 * <p>numUAVs > 1 in simulation, numUAVs == 1 running in a real UAV.</p> */
	public abstract void initializeDataStructures();
	
	/**
	 * Set the state to be shown in the progress dialog when ArduSim starts.
	 * @return The state to be shown in the progress dialog.
	 */
	public abstract String setInitialState();
	
	/**
	 * Optional: Re-scale specific data structures of the protocol when the visualization scale changes.
	 * <p>It is used when the protocol shows additional elements in the main panel.</p> */
	public abstract void rescaleDataStructures();
	
	/**
	 * Optional: Load resources to be shown on screen. */
	public abstract void loadResources();
	
	/**
	 * Optional: Re-scale loaded resources when the visualization scale changes.
	 * <p>It is used when the protocol shows additional elements in the main panel.</p> */
	public abstract void rescaleShownResources();
	
	/**
	 * Optional: Periodically draws the resources used in the protocol in the Graphics2D element of the specified BoardPanel.
	 * @param graphics Element of the panel where any element of the protocol can be drawn.
	 * @param panel Panel where the <i>graphics</i> element belongs to.
	 */
	public abstract void drawResources(Graphics2D graphics, BoardPanel panel);
	
	/**
	 * Set the initial location where all the UAVs running will appear (only for simulation).
	 * @return The calculated Geographic coordinates (latitude and longitude), and the heading (degrees).
	 */
	public abstract Pair<GeoCoordinates, Double>[] setStartingLocation();
	
	/**
	 * Send to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
	 * <p>Must be a blocking method!</p>
	 * @param numUAV UAV position in arrays.
	 * @return true if all the commands included in the method end successfully.
	 */
	public abstract boolean sendInitialConfiguration(int numUAV);
	
	/**
	 * Launch threads needed by the protocol.
	 * <p>In general, these threads must wait until a condition is met before doing any action.
	 * For example, a UAV thread must wait until the setup or start button is pressed to interact with other multicopters.</p> */
	public abstract void startThreads();
	
	/**
	 * Action automatically performed when the user presses the Setup button.
	 * <p>This must be a blocking method until the setup process if finished!</p> */
	public abstract void setupActionPerformed();
	
	/**
	 * Action automatically performed when the user presses the Start button.
	 * <p>This must NOT be a blocking method, just should force a protocol thread to move the UAVs.</p> */
	public abstract void startExperimentActionPerformed();
	
	/**
	 * Optional: Periodically issued to analyze if the experiment must be finished, and to apply measures to make the UAVs land.
	 * <p>For example, it can be finished when the user presses a button, the UAV is approaching to a location, or ending a mission.
	 * ArduSim stops the experiment when all the UAVs have landed.</p>*/
	public abstract void forceExperimentEnd();
	
	/**
	 * Optional: Provide general results of the experiment to be appended to the text shown on the results dialog.
	 * @return String with the results to be included on the results dialog.
	 */
	public abstract String getExperimentResults();
	
	/**
	 * Optional: Provide the protocol configuration to be appended to the text shown on the results dialog.
	 * @return String with the configuration to be included on the results dialog.
	 */
	public abstract String getExperimentConfiguration();
	
	/**
	 * Optional: Store at the end of the experiment files with information gathered while applying the protocol.
	 * @param folder Folder where the files will be stored, the same as the main log file.
	 * @param baseFileName Base name that must be prepended to the final file name.
	 * @param baseNanoTime Base time (ns) when the experiment started. It the arbitrary value provided by <i>System.nanoTime()</i> when the experiment started.
	 */
	public abstract void logData(String folder, String baseFileName, long baseNanoTime);
	
	/**
	 * Optional: Opens a configuration dialog for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
	 * Launch dialog information updates in an independent thread to let the dialog construction finish.</p>
	 * @param PCCompanionFrame The Frame of the PC Companion instance. It can be set as the owner of the dialog to be built, if needed.
	 */
	public abstract void openPCCompanionDialog(JFrame PCCompanionFrame);
	
}
