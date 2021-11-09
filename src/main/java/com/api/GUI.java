package com.api;

import com.api.pojo.StatusPacket;
import com.api.pojo.location.Waypoint;
import com.setup.Param;
import com.setup.Text;
import com.setup.pccompanion.logic.PCCompanionParam;
import com.setup.sim.gui.MissionKmlDialogApp;
import com.setup.sim.gui.MissionKmlSimProperties;
import com.setup.sim.gui.MissionWaypointsDialog;
import com.setup.sim.gui.ProgressDialog;
import com.setup.sim.logic.SimParam;
import javafx.stage.Stage;
import org.javatuples.Pair;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/** API to update the information shown on screen during simulations.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class GUI {

	private int numUAV;
	
	@SuppressWarnings("unused")
	private GUI() {}
	
	public GUI(int numUAV) {
		this.numUAV = numUAV;
	}
	
	/**
	 * Program termination when a fatal error happens.
	 * <p>On a real UAV it shows the message in console, performs RTL, and exits.
	 * On simulation or PC Companion, the message is shown in a dialog, and exits.
	 * Virtual UAVs are stopped before exiting if needed.</p>
	 * @param message Error message to be shown.
	 */
	public void exit(String message) {
		ArduSimTools.closeAll(message);
	}
	
	/**
	 * Provide a list with the UAVs detected by the PCCompanion to be used by the protocol dialog IN PCCompanion, not in multicopter or simulator roles.
	 * @return List of UAVs detected by the PCCompanion.
	 */
	public StatusPacket[] getDetectedUAVs() {
		return PCCompanionParam.connectedUAVs.get();
	}

	/**
	 * Open a dialog to load missions from a Google Earth <i>.kml</i> file, or from <i>.waypoints</i> files.
	 * @return The path found, and an array of missions, or null if no file was selected or any error happens.
	 */
	public File[] searchMissionFiles(){
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(API.getFileTools().getCurrentFolder());
		chooser.setDialogTitle(Text.MISSIONS_DIALOG_TITLE_1);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, Text.FILE_EXTENSION_KML);
		chooser.addChoosableFileFilter(filter1);
		FileNameExtensionFilter filter2 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_2, Text.FILE_EXTENSION_WAYPOINTS);
		chooser.addChoosableFileFilter(filter2);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setMultiSelectionEnabled(true);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		return chooser.getSelectedFiles();
	}

	/**
	 * Loads the mission files
	 * @param missionFiles list with files
	 * @return pair of string and waypoints
	 */
	public Pair<String, List<Waypoint>[]> loadMissions(List<File> missionFiles) {//TODO hacerlo con thread y callback listener para que no se ejecute en el hilo GUI
		File[] selection = new File[missionFiles.size()];
		for(int i =0;i<missionFiles.size();i++){
			selection[i] = missionFiles.get(i);
		}
		Pair<String, List<Waypoint>[]> missions = this.loadAndParseMissions(selection);
		if (missions != null) {
			if (missions.getValue0().equals(Text.FILE_EXTENSION_KML)) {
				ArduSimTools.logGlobal(Text.MISSIONS_LOADED + " " + selection[0].getName());
				return new Pair<>(selection[0].getName(), missions.getValue1());
			}
			if (missions.getValue0().equals(Text.FILE_EXTENSION_WAYPOINTS)) {
				ArduSimTools.logGlobal(Text.MISSIONS_LOADED + " " + selection[0].getName());
				return new Pair<>(selection[0].getAbsolutePath(), missions.getValue1());
			}
		}
		return null;
	}
	
	/** Parse missions from files.
	 * <p>Returns null if any error happens.</p> */
	@SuppressWarnings("unchecked")
	private Pair<String, List<Waypoint>[]> loadAndParseMissions(File[] files) {
		if (files == null || files.length == 0) {
			return null;
		}
		FileTools fileTools = API.getFileTools();
		String extension = fileTools.getFileExtension(files[0]);
		// Only one "kml" file is accepted
		boolean correctExtension = extension.equalsIgnoreCase(Text.FILE_EXTENSION_KML);
		if (correctExtension && files.length > 1) {
			ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_1);
			return null;
		}
		// waypoints files can not be mixed with kml files
		boolean correctWaypointExtension = extension.equalsIgnoreCase(Text.FILE_EXTENSION_WAYPOINTS);
		if (correctWaypointExtension) {
			for (int i = 1; i < files.length; i++) {
				if (!fileTools.getFileExtension(files[i]).equalsIgnoreCase(Text.FILE_EXTENSION_WAYPOINTS)) {
					ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_2);
					return null;
				}
			}
		}
		// kml file selected. All missions are loaded from one single file
		if (correctExtension) {
			// First, configure the missions
			MissionKmlSimProperties.success = false;
			//new MissionKmlDialog(files[0].getName());
			if(Param.role == ArduSim.SIMULATOR_GUI) {
				new MissionKmlDialogApp().start(new Stage());
				while (!MissionKmlSimProperties.success) {
					API.getArduSim().sleep(200);
				}
			}else if(Param.role == ArduSim.SIMULATOR_CLI){
				try {
					MissionKmlSimProperties properties = new MissionKmlSimProperties();
					FileInputStream fis;
					if(!ArduSimTools.isRunningFromJar()) {
						fis = new FileInputStream(SimParam.missionParameterFile);
					}else{
						fis = new FileInputStream(API.getFileTools().getResourceFolder() + File.separator + "missionParam.properties");
					}
					ResourceBundle resources = new PropertyResourceBundle(fis);
					fis.close();
					Properties p = new Properties();
					for(String key: resources.keySet()){
						p.setProperty(key,resources.getString(key));
					}
					properties.storeParameters(p,resources);
					MissionKmlSimProperties.success = true;
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (!MissionKmlSimProperties.success) {
				ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_8);
				return null;
			}
			// Next, load the missions
			List<Waypoint>[] missions = ArduSimTools.loadXMLMissionsFile(files[0]);
			if (missions == null) {
				ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_3);
				return null;
			}

			return new Pair<>(Text.FILE_EXTENSION_KML, missions);
		}
		
		// One or more .waypoints files selected
		if (correctWaypointExtension) {
			// First, configure the missions
			MissionWaypointsDialog.success = false;
			String name;
			if (files.length == 1) {
				name = files[0].getName();
			} else {
				name = files[0].getParentFile().getAbsolutePath();
			}
			new MissionWaypointsDialog(name);
			if (!MissionWaypointsDialog.success) {
				ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_8);
				return null;
			}
			List<Waypoint>[] missions = new ArrayList[files.length];
			// Next, load each main.java.com.protocols.mission from one file
			int j = 0;
			for (File file : files) {
				List<Waypoint> current = ArduSimTools.loadMissionFile(file.getAbsolutePath());
				if (current != null) {
					missions[j] = current;
					j++;
				}
			}
			// If no valid missions were found, just ignore the action
			if (j == 0) {
				ArduSimTools.warnGlobal(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_4);
				return null;
			}
			
			// The array must be resized if some file was incorrect
			if (j != files.length) {
				List<Waypoint>[] aux = missions;
				missions = new ArrayList[j];
				int m = 0;
				for (int k = 0; k < files.length; k++) {
					if (aux[k] != null) {
						missions[m] = aux[k];
						m++;
					}
				}
			}
			return new Pair<>(Text.FILE_EXTENSION_WAYPOINTS, missions);
		}
		return null;
	}

	/**
	 * Send information to the main window log and console.
	 * <p>The window log is only updated when performing simulations</p>.
	 * @param text Text to be shown.
	 */
	public void log(String text) {
		ArduSimTools.logGlobal(text);
	}
	
	/**
	 * Send information related to a specific UAV to the main window log and console.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * The window log is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public void logUAV(String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		ArduSimTools.logGlobal(res + text);
	}
	
	/**
	 * Send information to the main window log and console, only if verbose mode is enabled.
	 * <p>The window log is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public void logVerbose(String text) {
		ArduSimTools.logVerboseGlobal(text);
	}
	
	/**
	 * Send information related to a specific UAV to the main window log and console, only if verbose mode is enabled.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * The window log is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public void logVerboseUAV(String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		ArduSimTools.logVerboseGlobal(res + text);
	}
	
	/**
	 * Send information to the main window upper-right corner label when a protocol needs it.
	 * <p>The label is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public void updateGlobalInformation(final String text) {
		ArduSimTools.updateGlobalInformation(text);
	}
	
	/**
	 * Update the protocol state shown on the progress dialog for this UAV.
	 * <p>The progress dialog is only updated when performing simulations.</p>
	 * @param state String representation of the protocol state.
	 */
	public void updateProtocolState(final String state) {
		// Update GUI only when using simulator
		if (ProgressDialog.progressDialog != null) {
			SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.panels[numUAV].protStateLabel.setText(state));
		}
	}

	/**
	 * Warn the user.
	 * <p>A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV.</p>
	 * @param title Title for the dialog.
	 * @param message Message to be shown.
	 */
	public void warn(String title, String message) {
		ArduSimTools.warnGlobal(title, message);
	}
	
	/**
	 * Warn the user about something related to this specific UAV.
	 * <p>A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV.</p>
	 * @param title Title for the dialog.
	 * @param message Message to be shown.
	 */
	public void warnUAV(String title, String message) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		ArduSimTools.warnGlobal(title, res + message);
	}
	
}
