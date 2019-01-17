package api;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.javatuples.Pair;

import api.pojo.StatusPacket;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import main.ArduSimTools;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import pccompanion.logic.PCCompanionParam;
import sim.board.BoardHelper;
import sim.gui.MainWindow;
import sim.gui.MissionDelayDialog;
import sim.gui.ProgressDialog;
import sim.logic.SimParam;

/** This class consists exclusively of static methods that help the developer to validate and show information on screen.
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class GUI {
	
	// Used to avoid the exit dialog to be opened more than once at the same time.
	private static AtomicBoolean exiting = new AtomicBoolean();
	
	/**
	 * Send information to the main window log and console.
	 * <p>The window log is only updated when performing simulations</p>.
	 * @param text Text to be shown.
	 */
	public static void log(String text) {
		final String res;
		if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
			res = Tools.timeToString(Param.setupTime, System.currentTimeMillis())
					+ " " + text;
		} else if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			res = Tools.timeToString(Param.startTime, System.currentTimeMillis())
					+ " " + text;
		} else {
			res = text;
		}
		System.out.println(res);
		System.out.flush();
		// Update GUI only when using simulator and the main window is already loaded
		if (Param.role == Tools.SIMULATOR && MainWindow.buttonsPanel != null && MainWindow.buttonsPanel.logArea != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.logArea.append(res + "\n");
					int pos = MainWindow.buttonsPanel.logArea.getText().length();
					MainWindow.buttonsPanel.logArea.setCaretPosition( pos );
				}
			});
		}
	}
	
	/**
	 * Send information related to a specific UAV to the main window log and console.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * The window log is only updated when performing simulations.</p>
	 * @param numUAV UAV position in arrays.
	 * @param text Text to be shown.
	 */
	public static void log(int numUAV, String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.log(res + text);
	}
	
	/**
	 * Send information to the main window log and console, only if verbose mode is enabled.
	 * <p>The window log is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public static void logVerbose(String text) {
		if (Param.verboseLogging) {
			GUI.log(text);
		}
	}
	
	/**
	 * Send information related to a specific UAV to the main window log and console, only if verbose mode is enabled.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * The window log is only updated when performing simulations.</p>
	 * @param numUAV UAV position in arrays.
	 * @param text Text to be shown.
	 */
	public static void logVerbose(int numUAV, String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.logVerbose(res + text);
	}
	
	/**
	 * Send information to the main window upper-right corner label when a protocol needs it.
	 * <p>The label is only updated when performing simulations.</p>
	 * @param text Text to be shown.
	 */
	public static void updateGlobalInformation(final String text) {
		// Update GUI only when using simulator
		if (Param.role == Tools.SIMULATOR) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(text);
				}
			});
		}
	}
	
	/**
	 * Update the protocol state shown on the progress dialog.
	 * <p>The progress dialog is only updated when performing simulations.</p>
	 * @param numUAV UAV position in arrays.
	 * @param state String representation of the protocol state.
	 */
	public static void updateProtocolState(final int numUAV, final String state) {
		// Update GUI only when using simulator
		if (ProgressDialog.progressDialog != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ProgressDialog.progressDialog.panels[numUAV].protStateLabel.setText(state);
				}
			});
		}
	}

	/**
	 * Program termination when a fatal error happens.
	 * <p>On a real UAV it shows the message in console and exits.
	 * On simulation or PC Companion, the message is shown in a dialog.
	 * Virtual UAVs are stopped before exiting if needed.</p>
	 * @param message Error message to be shown.
	 */
	public static void exit(String message) {
		boolean exiting = GUI.exiting.getAndSet(true);
		if (!exiting) {
			if (Param.role == Tools.MULTICOPTER) {
				System.out.println(Text.FATAL_ERROR + ": " + message);
				System.out.flush();
			} else {
				JOptionPane.showMessageDialog(null, message, Text.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
				if (Param.role == Tools.SIMULATOR) {
					if (Param.simStatus != SimulatorState.CONFIGURING
						&& Param.simStatus != SimulatorState.CONFIGURING_PROTOCOL) {
						ArduSimTools.closeSITL();
					}
				}
			}
		}
		System.exit(1);
	}

	/**
	 * Warn the user.
	 * <p>A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV.</p>
	 * @param title Title for the dialog.
	 * @param message Message to be shown.
	 */
	public static void warn(String title, String message) {
		if (Param.role == Tools.MULTICOPTER) {
			System.out.println(title + ": " + message);
			System.out.flush();
		} else {
			JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/**
	 * Warn the user about something related to a specific UAV.
	 * <p>A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV.</p>
	 * @param numUAV UAV position in arrays.
	 * @param title Title for the dialog.
	 * @param message Message to be shown.
	 */
	public static void warn(int numUAV, String title, String message) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.warn(title, res + message);
	}
	
	/**
	 * Close the shown configuration dialog, and optionally ArduSim when the escape key is pressed on the keyboard.
	 * <p>This method should be invoked when the configuration dialog of each protocol is built.</p>
	 * @param dialog Dialog where this method enables the escape key.
	 * @param closeArduSim Whether to close ArduSim or not when the key is pressed.
	 */
	public static void addEscapeListener(final JDialog dialog, final boolean closeArduSim) {
	    ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
//	            dialog.setVisible(false);
	        	dialog.dispose();
				System.gc();
				if (closeArduSim) {
					System.exit(0);
				}
	        }
	    };

	    dialog.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

	}
	
	/**
	 * Open a dialog to load missions from a Google Earth <i>.kml</i> file.
	 * @return The path found, and an array of missions, or null if no file was selected or any error happens.
	 */
	public static Pair<String, List<Waypoint>[]> loadKMLMissions() {//TODO hacerlo con thread y callback para que no se ejecute en el hilo GUI
		File[] selection;
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(Tools.getCurrentFolder());
		chooser.setDialogTitle(Text.MISSIONS_DIALOG_TITLE_2);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_1, Text.FILE_EXTENSION_KML);
		chooser.addChoosableFileFilter(filter1);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setMultiSelectionEnabled(false);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		
		selection = new File[] {chooser.getSelectedFile()};
		Pair<String, List<Waypoint>[]> missions = GUI.loadAndParseMissions(selection);
		if (missions != null) {
			if (missions.getValue0().equals(Text.FILE_EXTENSION_KML)) {
				return new Pair<String, List<Waypoint>[]>(selection[0].getAbsolutePath(), missions.getValue1());
			}
			if (missions.getValue0().equals(Text.FILE_EXTENSION_WAYPOINTS)) {
				return new Pair<String, List<Waypoint>[]>(chooser.getCurrentDirectory().getAbsolutePath(), missions.getValue1());
			}
		}
		
		return null;
	}
	
	/**
	 * Open a dialog to load missions from <i>.waypoints</i> files.
	 * @return The path found, and an array of missions, or null if no file was selected or any error happens.
	 */
	public static Pair<String, List<Waypoint>[]> loadWaypointMissions() {//TODO hacerlo con thread y callback para que no se ejecute en el hilo GUI
		File[] selection;
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(Tools.getCurrentFolder());
		chooser.setDialogTitle(Text.MISSIONS_DIALOG_TITLE_1);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.MISSIONS_DIALOG_SELECTION_2, Text.FILE_EXTENSION_WAYPOINTS);
		chooser.addChoosableFileFilter(filter1);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setMultiSelectionEnabled(true);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		selection = chooser.getSelectedFiles();
		Pair<String, List<Waypoint>[]> missions = GUI.loadAndParseMissions(selection);
		if (missions != null) {
			if (missions.getValue0().equals(Text.FILE_EXTENSION_KML)) {
				return new Pair<String, List<Waypoint>[]>(selection[0].getAbsolutePath(), missions.getValue1());
			}
			if (missions.getValue0().equals(Text.FILE_EXTENSION_WAYPOINTS)) {
				return new Pair<String, List<Waypoint>[]>(chooser.getCurrentDirectory().getAbsolutePath(), missions.getValue1());
			}
		}

		return null;
	}
	
	/**
	 * Open a dialog to load missions from a Google Earth <i>.kml</i> file, or from <i>.waypoints</i> files.
	 * @return The path found, and an array of missions, or null if no file was selected or any error happens.
	 */
	public static Pair<String, List<Waypoint>[]> loadMissions() {//TODO hacerlo con thread y callback para que no se ejecute en el hilo GUI
		File[] selection;
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(Tools.getCurrentFolder());
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
		
		selection = chooser.getSelectedFiles();
		Pair<String, List<Waypoint>[]> missions = GUI.loadAndParseMissions(selection);
		if (missions != null) {
			if (missions.getValue0().equals(Text.FILE_EXTENSION_KML)) {
				GUI.log(Text.MISSIONS_LOADED + " " + selection[0].getName());
				return new Pair<String, List<Waypoint>[]>(selection[0].getName(), missions.getValue1());
			}
			if (missions.getValue0().equals(Text.FILE_EXTENSION_WAYPOINTS)) {
				GUI.log(Text.MISSIONS_LOADED + " " + selection[0].getName());
				return new Pair<String, List<Waypoint>[]>(chooser.getCurrentDirectory().getAbsolutePath(), missions.getValue1());
			}
		}
		
		return null;
	}
	
	/** Parse missions from files.
	 * <p>Returns null if any error happens.</p> */
	@SuppressWarnings("unchecked")
	private static Pair<String, List<Waypoint>[]> loadAndParseMissions(File[] files) {
		if (files == null || files.length == 0) {
			return null;
		}
		
		String extension = Tools.getFileExtension(files[0]);
		// Only one "kml" file is accepted
		if (extension.toUpperCase().equals(Text.FILE_EXTENSION_KML.toUpperCase()) && files.length > 1) {
			GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_1);
			return null;
		}
		// waypoints files can not be mixed with kml files
		if (extension.toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
			for (int i = 1; i < files.length; i++) {
				if (!Tools.getFileExtension(files[i]).toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
					GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_2);
					return null;
				}
			}
		}
		
		// kml file selected. All missions are loaded from one single file
		if (extension.toUpperCase().equals(Text.FILE_EXTENSION_KML.toUpperCase())) {
			// First, configure the missions
			new MissionDelayDialog(files[0].getName());
			// Next, load the missions
			List<Waypoint>[] missions = ArduSimTools.loadXMLMissionsFile(files[0]);
			if (missions == null) {
				GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_3);
				return null;
			}
			return new Pair<String, List<Waypoint>[]>(Text.FILE_EXTENSION_KML, missions);
		}
		
		// One or more .waypoints files selected
		if (extension.toUpperCase().equals(Text.FILE_EXTENSION_WAYPOINTS.toUpperCase())) {
			List<Waypoint>[] missions = new ArrayList[files.length];
			// Load each mission from one file
			int j = 0;
			for (int i = 0; i < files.length; i++) {
				List<Waypoint> current = ArduSimTools.loadMissionFile(files[i].getAbsolutePath());
				if (current != null) {
					missions[j] = current;
					j++;
				}
			}
			// If no valid missions were found, just ignore the action
			if (j == 0) {
				GUI.warn(Text.MISSIONS_SELECTION_ERROR, Text.MISSIONS_ERROR_4);
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
			return new Pair<String, List<Waypoint>[]>(Text.FILE_EXTENSION_WAYPOINTS, missions);
		}
		return null;
	}
	
	/**
	 * Locate a UTM coordinates point on the screen, using the current screen scale.
	 * @param utmX (meters)
	 * @param utmY (meters)
	 * @return Screen coordinates of the point.
	 */
	public static Point2D.Double locatePoint(double utmX, double utmY) {
		return BoardHelper.locatePoint(utmX, utmY);
	}
	
	/**
	 * Locates a UTM coordinates point on the screen, using the current screen scale.
	 * @param location UTM coordinates
	 * @return Screen coordinates of the point.
	 */
	public static Point2D.Double locatePoint(UTMCoordinates location) {
		return BoardHelper.locatePoint(location.x, location.y);
	}
	
	/**
	 * Get the Color associated to a UAV, and that should be used to draw protocol elements.
	 * @param numUAV UAV position in arrays.
	 * @return Color used to draw elements of the UAV.
	 */
	public static Color getUAVColor(int numUAV) {
		return SimParam.COLOR[numUAV % SimParam.COLOR.length];
	}
	
	/**
	 * Provide a list with the UAVs detected by the PCCompanion to be used by the protocol dialog.
	 * @return List of UAVs detected by the PCCompanion.
	 */
	public static StatusPacket[] getDetectedUAVs() {
		return PCCompanionParam.connectedUAVs.get();
	}

}
