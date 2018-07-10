package main;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;

import org.javatuples.Pair;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.Waypoint;
import main.Param.SimulatorState;
import sim.board.BoardHelper;
import sim.gui.ConfigDialog;
import sim.gui.MainWindow;
import sim.gui.ProgressDialog;
import sim.gui.ResultsDialog;
import sim.logic.CollisionDetector;
import sim.logic.DistanceCalculusThread;
import sim.logic.FakeReceiverThread;
import sim.logic.FakeSenderThread;
import sim.logic.RangeCalculusThread;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.ExperimentListener;
import uavController.ExperimentTalker;
import uavController.UAVParam;

/** This class contains the main method and the chronological logic followed by the whole application. */

public class Main {

	private static Timer timer;	// Used to show the time elapsed from the experiment beginning

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String[] existingProtocols = ArduSimTools.loadProtocols();
		if (existingProtocols == null) {
			return;
		}
		ProtocolHelper.ProtocolNames = existingProtocols;
		
		// Parse the command line arguments
		if (!ArduSimTools.parseArgs(args)) {
			return;
		}
		
		System.setProperty("sun.java2d.opengl", "true");
		Param.simStatus = SimulatorState.CONFIGURING;
		ArduSimTools.detectOS();
		if (Param.isRealUAV) {
			// 1. We need to make constant the parameters shown in the GUI
			Param.numUAVs = 1;	// Always one UAV per Raspberry Pi or whatever the device where the application is deployed
			Param.numUAVsTemp.set(1);

			// 2. Establish the identifier of the UAV
			Param.id = new long[Param.numUAVs];
			Param.id[0] = ArduSimTools.getRealId();

			// 3. Mission file loading
			// The mission is loaded from a file on the same folder as the jar file
			boolean loadMission = ProtocolHelper.selectedProtocolInstance.loadMission();
			File parentFolder = Tools.getCurrentFolder();
			if (loadMission) {
				List<Waypoint> mission = Tools.loadMission(parentFolder);
				if (mission == null) {
					GUI.exit(Text.MISSION_NOT_FOUND);
				}
				Tools.setLoadedMissionsFromFile(new List[] {mission});
			}
			
			// 4. Start threads for waiting to commands to start the setup and start steps of the experiment
			(new ExperimentTalker()).start();
			(new ExperimentListener()).start();
		} else {
			// 1. Opening the general configuration dialog
			Param.simStatus = SimulatorState.CONFIGURING;
			Param.numUAVs = -1;

			ArduSimTools.locateSITL();
			ArduSimTools.checkAdminPrivileges();
			try {
				UAVParam.mavPort = ArduSimTools.getSITLPorts();
				if (UAVParam.mavPort.length < UAVParam.MAX_SITL_INSTANCES) {
					GUI.warn(Text.PORT_ERROR, Text.PORT_ERROR_1 + UAVParam.mavPort.length + " " + Text.UAV_ID + "s");
				}
			} catch (InterruptedException | ExecutionException e) {
				GUI.exit(Text.PORT_ERROR_2);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				ArduSimTools.checkImdiskInstalled();
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					new ConfigDialog();
				}
			});
			// Waiting the configuration to be finished
			while (Param.simStatus == SimulatorState.CONFIGURING) {
				Tools.waiting(SimParam.SHORT_WAITING_TIME);
			}
			if (Param.numUAVs != Param.numUAVsTemp.get()) {
				Param.numUAVs = Param.numUAVsTemp.get();
			}

			// 2. Opening the configuration dialog of the protocol under test
			if (Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ProtocolHelper.selectedProtocolInstance.openConfigurationDialog();
					}
				});
				
				// Waiting the protocol configuration to be finished
				while (Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL) {
					Tools.waiting(SimParam.SHORT_WAITING_TIME);
				}
				if (Param.numUAVs != Param.numUAVsTemp.get()) {
					Param.numUAVs = Param.numUAVsTemp.get();
				}
			}

			// 3. Launch the main window
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.window = new MainWindow();
				}
			});
		}
		
		// 4. Data structures initializing
		ArduSimTools.initializeDataStructures();
		ProtocolHelper.selectedProtocolInstance.initializeDataStructures();
		// Waiting the main window to be built
		if (!Param.isRealUAV) {
			while (MainWindow.boardPanel == null || MainWindow.buttonsPanel == null) {
				Tools.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 5. Initial GUI configuration and launch the progress dialog
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.logArea.setText(Text.STARTING_ENVIRONMENT + "\n");
					MainWindow.buttonsPanel.progressDialogButton.setEnabled(false);
					MainWindow.buttonsPanel.setupButton.setEnabled(false);
					MainWindow.buttonsPanel.startTestButton.setEnabled(false);
					MainWindow.buttonsPanel.statusLabel.setText(Text.STARTING_ENVIRONMENT);
					MainWindow.progressDialog = new ProgressDialog(MainWindow.window.mainWindowFrame);
					MainWindow.progressDialog.setVisible(true);
				}
			});
			// Waiting the progress dialog to be built
			while (!SimParam.progressShowing || MainWindow.progressDialog == null) {
				Tools.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 6. Load needed resources
			SimTools.loadUAVImage();
			ProtocolHelper.selectedProtocolInstance.loadResources();
		}
		// Configuration feedback
		GUI.log(Text.PROTOCOL_IN_USE + " " + ProtocolHelper.selectedProtocol);
		if (!Param.isRealUAV) {
			if (SimParam.userIsAdmin
					&& ((Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled)
							|| Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)) {
				GUI.log(Text.USING_RAM_DRIVE);
			} else {
				GUI.log(Text.USING_HARD_DRIVE);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				if (SimParam.userIsAdmin && !SimParam.imdiskIsInstalled) {
					GUI.log(Text.INSTALL_IMDISK);
				}
				if (!SimParam.userIsAdmin) {
					if (SimParam.imdiskIsInstalled) {
						GUI.log(Text.USE_ADMIN);
					} else {
						GUI.log(Text.INSTALL_IMDISK_USE_ADMIN);
					}
				}
			}
			if (Param.runningOperatingSystem == Param.OS_LINUX && !SimParam.userIsAdmin) {
				GUI.log(Text.USE_ROOT);
			}
			GUI.log(Text.WIRELESS_MODEL_IN_USE + " " + Param.selectedWirelessModel.getName());
			if (Param.windSpeed != 0.0) {
				GUI.log(Text.SIMULATED_WIND_SPEED + " " + Param.windSpeed);
			}
		}

		// 7. Automatic screen update activation (position logging when in a real UAV)
		SimTools.update();

		// 8. Startup of the virtual UAVs
		if (!Param.isRealUAV) {
			BoardHelper.buildWindImage(MainWindow.boardPanel);
			Pair<GeoCoordinates, Double>[] start = ProtocolHelper.selectedProtocolInstance.setStartingLocation();
			SimParam.tempFolderBasePath = ArduSimTools.defineTemporaryFolder();
			if (SimParam.tempFolderBasePath == null) {
				GUI.exit(Text.TEMP_PATH_ERROR);
			}
			ArduSimTools.startVirtualUAVs(start);
		}

		// 9. Start UAV controllers, wait for MAVLink link, wait for GPS fix, and send basic configuration
		ArduSimTools.startUAVControllers();
		ArduSimTools.waitMAVLink();
		if (!Param.isRealUAV) {
			ArduSimTools.forceGPS();
		}
		ArduSimTools.getGPSFix();//TODO descomentar
		
		// 10. Set communications online, and start collision detection if needed
		if (!Param.isRealUAV && Param.numUAVs > 1) {
			// Calculus of the distance between UAVs
			new DistanceCalculusThread().start();
			// Communications range calculation enable
			new RangeCalculusThread().start();
			GUI.log(Text.COMMUNICATIONS_ONLINE);
			// Collision check enable
			if (UAVParam.collisionCheckEnabled) {
				(new CollisionDetector()).start();
				GUI.log(Text.COLLISION_DETECTION_ONLINE);
			}
		}
		
		// 11. Send basic UAV configuration and wait the communications to be online
		ArduSimTools.sendBasicConfiguration();
		if (!Param.isRealUAV && Param.numUAVs > 1) {
			while (!SimParam.communicationsOnline) {
				Tools.waiting(SimParam.SHORT_WAITING_TIME);
			}
		}
		
		// 12. Launch the threads of the protocol under test and wait the GUI to be built
		ProtocolHelper.selectedProtocolInstance.startThreads();
		if (Param.isRealUAV) {
			Param.simStatus = SimulatorState.UAVS_CONFIGURED;
		}
		while (Param.simStatus == SimulatorState.STARTING_UAVS) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.UAVS_CONFIGURED) {
			return;
		}
		
		// 13. Build auxiliary elements to be drawn and prepare the user interaction
		//    The background map can not be downloaded until the GUI detects that all the missions are loaded
		//      AND the drawing scale is calculated
		if (!Param.isRealUAV) {
			BoardHelper.downloadBackground();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.setupButton.setEnabled(true);
					MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_FLY);
				}
			});
			GUI.log(Text.WAITING_FOR_USER);
		}
		while (Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.SETUP_IN_PROGRESS) {
			return;
		}

		// 14. Apply the configuration step
		ProtocolHelper.selectedProtocolInstance.setupActionPerformed();
		Param.simStatus = SimulatorState.READY_FOR_TEST;
		while (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.READY_FOR_TEST) {
			return;
		}

		// 15. Waiting for the user to start the experiment
		if (!Param.isRealUAV) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_START);
					MainWindow.buttonsPanel.startTestButton.setEnabled(true);
				}
			});
			GUI.log(Text.WAITING_FOR_USER);
		}
		while (Param.simStatus == SimulatorState.READY_FOR_TEST) {
			Tools.waiting(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			return;
		}

		// 16. Start the experiment, only if the program is not being closed
		// TODO remove new threads and clean the console parameters (args)
		if (UAVParam.doFakeSending) {
			for (int i = 0; i < Param.numUAVs; i++) {
				(new FakeSenderThread(i)).start();
				(new FakeReceiverThread(i)).start();
			}
		}

		Param.startTime = System.currentTimeMillis();
		GUI.log(Text.TEST_START);
		if (!Param.isRealUAV) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				long time = Param.startTime;
				public void run() {
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						final String timeString = Tools.timeToString(Param.startTime, time);
						time = time + 1000;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MainWindow.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE + " " + timeString);
							}
						});
					} else {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MainWindow.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE);
							}
						});
						timer.cancel();
					}
				}
			}, 0, 1000);	// Once each second, without initial waiting time
		}
		ProtocolHelper.selectedProtocolInstance.startExperimentActionPerformed();

		// 17. Waiting while the experiment is is progress and detecting the experiment end
		int check = 0;
		boolean checkEnd = false;
		while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			// Check the battery level periodically
			if (check % UAVParam.BATTERY_PRINT_PERIOD == 0) {
				ArduSimTools.checkBatteryLevel();
			}
			check++;
			// Force the UAVs to land if needed
			ProtocolHelper.selectedProtocolInstance.forceExperimentEnd();
			// Detects if all UAVs are on the ground in order to finish the experiment
			if (checkEnd) {
				if (ArduSimTools.isTestFinished()) {// TODO descomentar tras hacer pruebas en el poli
					Param.simStatus = SimulatorState.TEST_FINISHED;
				}
			} else {
				if (System.currentTimeMillis() - Param.startTime > Param.STARTING_TIMEOUT) {
					checkEnd = true;
				}
			}
			if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
				Tools.waiting(SimParam.LONG_WAITING_TIME);
			}
		}
		if (Param.simStatus != SimulatorState.TEST_FINISHED) {
			return;
		}

		// 18. Wait for the user to close the simulator, only if the program is not being closed
		GUI.log(Tools.timeToString(Param.startTime, Param.latestEndTime) + " " + Text.TEST_FINISHED);
		if (!Param.isRealUAV) {
			GUI.log(Text.WAITING_FOR_USER);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.statusLabel.setText(Text.TEST_FINISHED);
				}
			});
		}

		// Gather information to show the results dialog
		String res = null;
		res = ArduSimTools.getTestResults();
		String s = ProtocolHelper.selectedProtocolInstance.getExperimentResults();
		if (s != null && s.length() > 0) {
			res += "\n" + ProtocolHelper.selectedProtocol + ":\n\n";
			res += s;
		}
		res += ArduSimTools.getTestGlobalConfiguration();
		s = ProtocolHelper.selectedProtocolInstance.getExperimentConfiguration();
		if (s != null && s.length() > 0) {
			res += "\n\n" + ProtocolHelper.selectedProtocol + " " + Text.CONFIGURATION + ":\n";
			res += s;
		}
		if (Param.isRealUAV) {
			Calendar cal = Calendar.getInstance();
			String fileName = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH)+1)
					+ "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY)
					+ "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND) + " " + Text.DEFAULT_BASE_NAME;
			ArduSimTools.storeResults(res, new File(Tools.getCurrentFolder(), fileName));
			ArduSimTools.shutdown();	// Closes the simulator
		} else {
			final String res2 = res;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					new ResultsDialog(res2, MainWindow.window.mainWindowFrame, true);
				}
			});
		}

		// Now, the user must close the application, even to do a new experiment
	}
}
