package com.api;

import es.upv.grc.mapper.Location2DGeo;
import org.javatuples.Pair;

import javax.swing.*;

/** 
 * The developer must extend this class to implement a new protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public abstract class ProtocolHelper {
	
	/** Text that identifies the protocol (protocol name). */
	public String protocolString = null;
	
	/** Assign a protocol name to this implementation. Write something similar to:
	 * <p>this.protocolString = "Some protocol name";</p> */
	public abstract void setProtocol();
	
	/**
	 * Assert if it is needed to load a main.java.com.protocols.mission.
	 * <p>This method is used when the protocol is deployed in a real multicopter (on simulations, the main.java.com.protocols.mission must be loaded in the dialog built in <i>openConfigurationDialog()</i> method).</p>
	 * @return must return true if this UAV must follow a main.java.com.protocols.mission.
	 */
	public abstract boolean loadMission();
	
	/**
	 * Optional: Create a configuration dialog for protocol specific parameters. Otherwise, return null.
	 * <p>Please, call <i>dispose</i> method to close the dialog when finishing, and never use JDialog methods like <i>setVisible, setModal, setResizable, setDefaultCloseOperation, setLocationRelativeTo, pack</i>, as they are automatically applied from inside ArduSim. The dialog will be constructed in the GUI thread (please, avoid heavy calculations).</p> */
	public abstract JDialog openConfigurationDialog();

	/**
	 * Optional: Create a configuration dialog (using javaFXML) for protocol specific parameters.
	 * <p>The dialog will be constructed in the GUI thread (please, avoid heavy calculations</p>
	 */
	public abstract void openConfigurationDialogFX();
	/**
	 * Optional: when extra parameters are used.
	 * In this method the step to load the protocol specific parameters must be called
	 */
	public abstract void configurationCLI();
	/**
	 * Initialize data structures used by the protocol. At this point, the number of multicopters running in the same machine is known:
	 * <p>int numUAVs = API.getArduSim().getNumUAVs().</p>
	 * <p>numUAVs > 1 in simulation, numUAVs == 1 running in a real UAV.</p>
	 * <p>We suggest you to initialize data structures as arrays with length depending on <i>numUAVs</i> value.</p> */
	public abstract void initializeDataStructures();
	
	/**
	 * Set the protocol state to be shown in the progress dialog when ArduSim starts.
	 * @return The state to be shown in the progress dialog.
	 */
	public abstract String setInitialState();
	
	/**
	 * Set the initial location where all the running UAVs will appear (only for simulation).
	 * @return The calculated Geographic coordinates (latitude and longitude), and the heading (radians).
	 */
	public abstract Pair<Location2DGeo, Double>[] setStartingLocation();
	
	/**
	 * Send to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
	 * <p>Must be a blocking method!</p>
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return true if all the commands included in the method end successfully, or you don't include commands at all.
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
	 * <p>This must NOT be a blocking method, just should force a protocol thread to start the protocol.</p> */
	public abstract void startExperimentActionPerformed();
	
	/**
	 * Optional: Periodically issued to analyze if the experiment must be finished, and to apply measures to make the UAVs land.
	 * <p>For example, it can be finished when the user presses a button, the UAV is approaching to a location, or ending a main.java.com.protocols.mission.
	 * ArduSim stops the experiment when all the UAVs have landed. Please, see an example in MBCAP protocol.</p>*/
	public abstract void forceExperimentEnd();
	
	/**
	 * Optional: Provide general results of the experiment to be appended to the text shown on the results dialog.
	 * @return String with the results of the protocol to be included on the results dialog.
	 */
	public abstract String getExperimentResults();
	
	/**
	 * Optional: Provide the protocol configuration to be appended to the text shown on the results dialog.
	 * @return String with the configuration of the protocol to be included on the results dialog.
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
	 * Optional: Open a configuration dialog for protocol specific parameters, when running ArduSim as a PC Companion.
	 * <p>The dialog will be constructed in the GUI thread (avoid heavy calculations).
	 * Please, launch dialog information updates in an independent thread to let the dialog construction finish (see example in MBCAP protocol).</p>
	 * @param PCCompanionFrame The Frame of the PC Companion instance. It can be set as the owner of the dialog to be built, if needed.
	 */
	public abstract void openPCCompanionDialog(JFrame PCCompanionFrame);
	
}
