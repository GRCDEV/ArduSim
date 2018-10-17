package api;

import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.javatuples.Pair;

import api.pojo.StatusPacket;
import api.pojo.Waypoint;
import main.ArduSimTools;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import pccompanion.logic.PCCompanionParam;
import sim.board.BoardHelper;
import sim.gui.MainWindow;
import sim.logic.SimParam;

/** This class consists exclusively of static methods that help the developer to validate and show information on screen. */

public class GUI {
	
	private static AtomicBoolean exiting = new AtomicBoolean();
	
	/** Sends information to the main window log and console.
	 * <p>The window log is only updated when performing simulations. */
	public static void log(String text) {
		final String res;
		if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
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
	
	/** Sends information related to a specific UAV to the main window log and console.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * <p>The window log is only updated when performing simulations. */
	public static void log(int numUAV, String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.log(res + text);
	}
	
	/** Sends information to the main window log and console, only if verbose mode is enabled.
	 * <p>The window log is only updated when performing simulations. */
	public static void logVerbose(String text) {
		if (Param.verboseLogging) {
			GUI.log(text);
		}
	}
	
	/** Sends information related to a specific UAV to the main window log and console, only if verbose mode is enabled.
	 * <p>This function adds a prefix to the message with the UAV ID.
	 * <p>The window log is only updated when performing simulations. */
	public static void logVerbose(int numUAV, String text) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.logVerbose(res + text);
	}
	
	/** Sends information to the main window upper-right corner label when a protocol needs it.
	 * <p>The label is only updated when performing simulations. */
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
	
	/** Updates the protocol state on the progress dialog.
	 * <p>The progress dialog is only updated when performing simulations. */
	public static void updateProtocolState(final int numUAV, final String state) {
		// Update GUI only when using simulator
		if (MainWindow.progressDialog != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.progressDialog.panels[numUAV].protStateLabel.setText(state);
				}
			});
		}
	}

	/** Program termination when a fatal error happens.
	 * <p>On a real UAV shows the message in console and exits.
	 * <p>On simulation or PC Companion, the message is shown in a dialog. Virtual UAVs are stopped before exiting if needed. */
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

	/** Warns the user. A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV. */
	public static void warn(String title, String message) {
		if (Param.role == Tools.MULTICOPTER) {
			System.out.println(title + ": " + message);
			System.out.flush();
		} else {
			JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/** Warns the user about something related to a specific UAV.
	 * <p>A dialog is used when performing simulations and in the PC Companion, and the console is used on a real UAV.*/
	public static void warn(int numUAV, String title, String message) {
		String res = "";
		if (SimParam.prefix != null && SimParam.prefix[numUAV] != null) {
			res += SimParam.prefix[numUAV];
		}
		GUI.warn(title, res + message);
	}
	
	/** Closes the shown configuration dialog, and ArduSim.
	 * <p>This method should be invoked when the configuration dialog of each protocol is built. */
	public static void addEscapeListener(final JDialog dialog) {
	    ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
//	            dialog.setVisible(false);
	        	dialog.dispose();
				System.gc();
				System.exit(0);
	        }
	    };

	    dialog.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

	}
	
	/** Opens a dialog to load missions from a Google Earth .kml file.
	 * <p>Returns the path found, and an array of missions, or null if no file was selected or any error happens. */
	public static Pair<String, List<Waypoint>[]> loadKMLMissions() {
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
	
	
	/** Opens a dialog to load missions from Waypoint .waypoints files.
	 * <p>Returns the path found, and an array of missions, or null if no file was selected or any error happens. */
	public static Pair<String, List<Waypoint>[]> loadWaypointMissions() {
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
	
	
	/** Opens a dialog to load missions from a Google Earth .kml file, or from Waypoint .waypoints files.
	 * <p>Returns the path found, and an array of missions, or null if no file was selected or any error happens. */
	public static Pair<String, List<Waypoint>[]> loadMissions() {
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
				return new Pair<String, List<Waypoint>[]>(selection[0].getAbsolutePath(), missions.getValue1());
			}
			if (missions.getValue0().equals(Text.FILE_EXTENSION_WAYPOINTS)) {
				return new Pair<String, List<Waypoint>[]>(chooser.getCurrentDirectory().getAbsolutePath(), missions.getValue1());
			}
		}
		
		return null;
	}
	
	/** Parses missions from files.
	 * <p>Returns null if any error happens. */
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
		
		// kml file selected
		if (extension.toUpperCase().equals(Text.FILE_EXTENSION_KML.toUpperCase())) {
			// All missions are loaded from one single file
			String missionEnd = GUI.askUserForMissionEnd();
			GUI.askUserForDelay();
			List<Waypoint>[] missions = ArduSimTools.loadXMLMissionsFile(files[0], missionEnd);
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
	
	/** Asks the user if the mission must be finished with a Land or RTL command when it is loaded from Google Earth .kml file.
	 * <p>This function must be used only from the GUI for simulations, not in a real multicopter or from the PC Companion.
	 * <p>Returns value MISSION_END_UNMODIFIED, MISSION_END_LAND or MISSION_END_RTL found on api.pojo.Waypoint class
	 * <p>If user closes the dialog, the default value is applied. */
	private static String askUserForMissionEnd() {
		String res = Waypoint.missionEnd;
		String[] options = {Waypoint.MISSION_END_UNMODIFIED, Waypoint.MISSION_END_LAND, Waypoint.MISSION_END_RTL};
		int pos = -1;
		for (int i = 0; i < options.length; i++) {
			if (options[i].equals(Waypoint.missionEnd)) {
				pos = i;
			}
		}
		int x = JOptionPane.showOptionDialog(null, Text.EXTEND_MISSION, Text.EXTEND_MISSION_TITLE,
				JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, pos);
		if (x != JOptionPane.CLOSED_OPTION) {
			res = options[x];
		}
		return res;
	}
	
	/** Asks the user for the delay duration in the intermediate waypoints of the mission.
	 * <p>If a delay is added to the waypoints, the multicopter avoids cutting corners when it arrives a waypoint. */
	private static void askUserForDelay() {
		SpinnerNumberModel sModel = new SpinnerNumberModel(Waypoint.waypointDelay, 0, 65535, 1);
		JSpinner spinner = new JSpinner(sModel);
		spinner.setSize(350, spinner.getHeight());
		spinner.setPreferredSize(new Dimension(350, spinner.getPreferredSize().height));
		String[] options = {Text.OK};
		int option = JOptionPane.showOptionDialog(null, spinner, Text.XML_DELAY,
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (option == JOptionPane.OK_OPTION) {
			Waypoint.waypointDelay = (Integer)spinner.getValue();
		}
	}
	
	/** Locates a UTM point on the screen, using the current screen scale. */
	public static Point2D.Double locatePoint(double inUTMX, double inUTMY) {
		return BoardHelper.locatePoint(inUTMX, inUTMY);
	}
	
	/** Provides the Color associated to a UAV and that should be used to draw protocol elements. */
	public static Color getUAVColor(int numUAV) {
		return SimParam.COLOR[numUAV % SimParam.COLOR.length];
	}
	
	/** Provides a list with the UAVs detected by the PCCompanion to be used by the protocol dialog. */
	public static StatusPacket[] getDetectedUAVs() {
		return PCCompanionParam.connectedUAVs.get();
	}

}
