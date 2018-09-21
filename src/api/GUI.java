package api;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
	
	/** Asks the user if the mission must be finished with a Land or RTL command when it is loaded from Google Earth .kml file.
	 * <p>This function must be used only from the GUI for simulations, not in a real multicopter or from the PC Companion.
	 * <p>Returns value MISSION_END_UNMODIFIED, MISSION_END_LAND or MISSION_END_RTL found on api.pojo.Waypoint class
	 * <p>If user closes the dialog, the default value is applied. */
	public static String askUserForMissionEnd() {
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
