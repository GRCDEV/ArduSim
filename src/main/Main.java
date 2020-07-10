package main;

import api.API;
import api.pojo.location.Waypoint;
import es.upv.grc.mapper.Location2DGeo;
import javafx.application.Platform;
import javafx.stage.Stage;
import main.Param.SimulatorState;
import main.api.ArduSim;
import main.api.ValidationTools;
import main.api.communications.CommLinkObject;
import main.api.communications.RangeCalculusThread;
import main.cpuHelper.CPUUsageThread;
import main.pccompanion.gui.PCCompanionGUI;
import main.sim.gui.ConfigDialogApp;
import main.sim.gui.MainWindow;
import main.sim.gui.ProgressDialog;
import main.sim.gui.ResultsDialog;
import main.sim.logic.*;
import main.uavController.TestListener;
import main.uavController.TestTalker;
import main.uavController.UAVParam;
import org.javatuples.Pair;

import javax.swing.*;
import java.io.File;
import java.net.SocketException;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** This class contains the main method and the chronological logic followed by the whole application.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class Main {

	private static Timer timer;	// Used to show the time elapsed from the experiment beginning

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// Parse the command line arguments and load configuration file
		if (!ArduSimTools.parseArgs(args)) {
			return;
		}
		
		// Load available protocols
		String[] existingProtocols = ArduSimTools.loadProtocols();
		if (existingProtocols == null) {
			ArduSimTools.warnGlobal(Text.VALIDATION_WARNING, Text.PROTOCOL_DUPLICATED);
			return;
		}
		ArduSimTools.ProtocolNames = existingProtocols;
		
		// Detect the running operating system
		ArduSimTools.detectOS();
		
		// Parse configuration file
		ArduSimTools.parseIniFile();
		
		// Open the PC Companion GUI en finish this thread
		if (Param.role == ArduSim.PCCOMPANION) {
			Param.numUAVs = 1;
			SwingUtilities.invokeLater(() -> PCCompanionGUI.companion = new PCCompanionGUI());
			return;	// In the PC Companion, the control is released to the GUI
		}
		
		// From this point, ArduSim would only run as multicopter or simulator
		System.setProperty("sun.java2d.opengl", "true");
		Param.simStatus = SimulatorState.CONFIGURING;
		
		ArduSim ardusim = API.getArduSim();
		File parentFolder = API.getFileTools().getCurrentFolder();
		if (Param.role == ArduSim.MULTICOPTER) {
			// 1. We need to make constant the parameters shown in the GUI
			Param.numUAVs = 1;	// Always one UAV per Raspberry Pi or whatever the device where the application is deployed
			Param.numUAVsTemp.set(1);

			// 2. Establish the identifier of the UAV
			Param.id = new long[Param.numUAVs];
			Param.id[0] = ArduSimTools.getRealId();

			// 3. Mission file loading
			// The mission is loaded from a file on the same folder as the jar file
			boolean loadMission = ArduSimTools.selectedProtocolInstance.loadMission();
			if (loadMission) {
				List<Waypoint> mission = ArduSimTools.loadMission(parentFolder);
				if (mission == null) {
					ArduSimTools.closeAll(Text.MISSION_NOT_FOUND);
				}
				API.getCopter(0).getMissionHelper().setMissionsLoaded(new List[] {mission});
			}
			
			// 4. Start threads for waiting to commands to start the setup and start steps of the experiment
			(new TestTalker()).start();
			try {
				TestListener listener = new TestListener();
				listener.start();
			} catch (SocketException e) {
				ArduSimTools.closeAll(Text.BIND_ERROR_1);
			}
			
			// 5. Start thread to measure the CPU usage
			if (Param.measureCPUEnabled) {
				new CPUUsageThread().start();
			}
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			// 1. Opening the general configuration dialog
			Param.simStatus = SimulatorState.CONFIGURING;
			Param.numUAVs = -1;

			ArduSimTools.locateSITL();
			ArduSimTools.checkAdminPrivileges();
			try {
				UAVParam.mavPort = ArduSimTools.getSITLPorts();
				if (UAVParam.mavPort.length < UAVParam.MAX_SITL_INSTANCES) {
					ArduSimTools.warnGlobal(Text.PORT_ERROR, Text.PORT_ERROR_1 + UAVParam.mavPort.length + " " + Text.UAV_ID + "s");
				}
			} catch (InterruptedException | ExecutionException e) {
				ArduSimTools.closeAll(Text.PORT_ERROR_2);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				ArduSimTools.checkImdiskInstalled();
			}

			if(Param.role == ArduSim.SIMULATOR_GUI) {
				// Start the javafxml GUI configDialogApp
				Platform.startup(() -> {new ConfigDialogApp().start(new Stage());});
				while(Param.simStatus != SimulatorState.CONFIGURING_PROTOCOL){
					API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
				}
			}else{
				// read .properties file
				SimProperties simProperties = new SimProperties();
				Properties resources = simProperties.readResourceCLI();
				if(resources == null){System.exit(0);}
				simProperties.storeParameters(resources);
				Param.simStatus = SimulatorState.CONFIGURING_PROTOCOL;
			}
			if (Param.numUAVs != Param.numUAVsTemp.get()) {
				Param.numUAVs = Param.numUAVsTemp.get();
			}

			// 2. Opening the configuration dialog of the protocol under test
			if (Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL) {
				final AtomicBoolean configurationOpened = new AtomicBoolean();
				if(Param.role == ArduSim.SIMULATOR_GUI) {
					ArduSimTools.selectedProtocolInstance.openConfigurationDialogFX();
					while(Param.simStatus != Param.SimulatorState.STARTING_UAVS){
						API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
					}

					if (Param.numUAVs != Param.numUAVsTemp.get()) {
						Param.numUAVs = Param.numUAVsTemp.get();
					}
				}else if(Param.role == ArduSim.SIMULATOR_CLI){
					ArduSimTools.selectedProtocolInstance.configurationCLI();
					Param.simStatus = SimulatorState.STARTING_UAVS;
				}
			}

			// 3. Launch the main window
			if(Param.role == ArduSim.SIMULATOR_GUI){
				SwingUtilities.invokeLater(() -> MainWindow.window = new MainWindow());
			}else if(Param.role == ArduSim.SIMULATOR_CLI){
				Param.setupTime = System.currentTimeMillis();
			}
		}
		
		// 4. Data structures initializing
		ArduSimTools.initializeDataStructures();

		ArduSimTools.selectedProtocolInstance.initializeDataStructures();
		// Waiting the main window to be built
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			while (MainWindow.boardPanel == null || MainWindow.buttonsPanel == null) {
				ardusim.sleep(SimParam.SHORT_WAITING_TIME);
			}

			// 5. Initial GUI configuration and launch the progress dialog
			SwingUtilities.invokeLater(() -> {
				MainWindow.buttonsPanel.logArea.setText(Text.STARTING_ENVIRONMENT + "\n");
				MainWindow.buttonsPanel.statusLabel.setText(Text.STARTING_ENVIRONMENT);
				new ProgressDialog(MainWindow.window.mainWindowFrame).toggleProgressShown();
			});
			// Waiting the progress dialog to be built
			while (!ProgressDialog.progressShowing || ProgressDialog.progressDialog == null) {
				ardusim.sleep(SimParam.SHORT_WAITING_TIME);
			}
			MainWindow.window.mainWindowFrame.toFront();

			// 6. Load needed resources
			SimParam.uavImage = API.getFileTools().loadImage(SimParam.UAV_IMAGE_PATH);
			if (SimParam.uavImage == null) {
				ArduSimTools.closeAll(Text.LOADING_UAV_IMAGE_ERROR);
			}
		}
		// Configuration feedback
		ArduSimTools.logGlobal(Text.PROTOCOL_IN_USE + " " + ArduSimTools.selectedProtocol);
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			if (SimParam.userIsAdmin
					&& ((Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled)
							|| Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)) {
				ArduSimTools.logVerboseGlobal(Text.USING_RAM_DRIVE);
			} else {
				ArduSimTools.logVerboseGlobal(Text.USING_HARD_DRIVE);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				if (SimParam.userIsAdmin && !SimParam.imdiskIsInstalled) {
					ArduSimTools.logGlobal(Text.INSTALL_IMDISK);
				}
				if (!SimParam.userIsAdmin) {
					if (SimParam.imdiskIsInstalled) {
						ArduSimTools.logGlobal(Text.USE_ADMIN);
					} else {
						ArduSimTools.logGlobal(Text.INSTALL_IMDISK_USE_ADMIN);
					}
				}
			}
			if ((Param.runningOperatingSystem == Param.OS_LINUX || Param.runningOperatingSystem == Param.OS_MAC)
					&& !SimParam.userIsAdmin) {
				ArduSimTools.logGlobal(Text.USE_ROOT);
			}
			ArduSimTools.logGlobal(Text.WIRELESS_MODEL_IN_USE + " " + Param.selectedWirelessModel.getName());
			if (Param.windSpeed != 0.0) {
				ArduSimTools.logGlobal(Text.SIMULATED_WIND_SPEED + " " + Param.windSpeed);
			}
		}

		// 7. Automatic progress dialog update activation
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SimTools.update();
		}

		// 8. Startup of the virtual UAVs
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			Pair<Location2DGeo, Double>[] start = ArduSimTools.selectedProtocolInstance.setStartingLocation();
			if(Param.role == ArduSim.SIMULATOR_GUI) {
				if (Param.windSpeed > 0.0) {
					MainWindow.window.buildWindImage();
				}
			}
			SimParam.tempFolderBasePath = ArduSimTools.defineTemporaryFolder();
			if (SimParam.tempFolderBasePath == null) {
				ArduSimTools.closeAll(Text.TEMP_PATH_ERROR);
			}
			if(Param.role == ArduSim.SIMULATOR_GUI) {
				SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(Text.WAITING_MAVLINK));
			}
			ArduSimTools.startVirtualUAVs(start);
		}

		// 9. Start UAV controllers, wait for MAVLink link, send basic configuration, and wait for GPS fix
		ArduSimTools.startUAVControllers();
		ArduSimTools.waitMAVLink();

		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(Text.WAITING_CONFIGURATION_UPLOAD));
		}
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI){
			ArduSimTools.forceGPS();
		}
		ArduSimTools.sendBasicConfiguration1();
		
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(Text.WAITING_GPS));
		}
		ArduSimTools.getGPSFix();
		ArduSimTools.sendBasicConfiguration2();	// It requires GPS fix to set the current location for takeoff

		// 10. Set communications online, and start collision detection if needed
		if ((Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) && Param.numUAVs > 1) {
			// Calculus of the distance between UAVs
			new DistanceCalculusThread().start();
			// Communications range calculation enable
			new RangeCalculusThread().start();

			// Collision check enable
			if (UAVParam.collisionCheckEnabled) {
				(new CollisionDetector()).start();
				ArduSimTools.logGlobal(Text.COLLISION_DETECTION_ONLINE);
			}
			// Wait the communications to be online
			while (!SimParam.communicationsOnline) {
				ardusim.sleep(SimParam.SHORT_WAITING_TIME);
			}
			ArduSimTools.logGlobal(Text.COMMUNICATIONS_ONLINE);
		}
		
		// 11. Launch the threads of the protocol under test and wait the GUI to be built
		ArduSimTools.logGlobal(Text.LAUNCHING_PROTOCOL_THREADS);
		ArduSimTools.selectedProtocolInstance.startThreads();
		if (Param.role == ArduSim.MULTICOPTER) {
			Param.simStatus = SimulatorState.UAVS_CONFIGURED;
		}else if(Param.role == ArduSim.SIMULATOR_CLI){
			Param.simStatus = SimulatorState.STARTING_UAVS;
		}
		while (Param.simStatus == SimulatorState.STARTING_UAVS) {
			// This can only happen during simulations
			if (InitialConfiguration2Thread.UAVS_CONFIGURED.get() == Param.numUAVs) {
				Param.simStatus = SimulatorState.UAVS_CONFIGURED;
			}
			ardusim.sleep(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.UAVS_CONFIGURED) {
			if (Param.role == ArduSim.MULTICOPTER) {
				API.getCopter(0).cancelRCOverride();
			}
			return;
		}
		
		// 12. Build auxiliary elements to be drawn and prepare the user interaction
		//    The background map cannot be downloaded until the GUI detects that all the missions are loaded
		//      AND the drawing scale is calculated
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> {
				MainWindow.buttonsPanel.setupButton.setEnabled(true);
				MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_FLY);
			});
			ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
		}else if(Param.role == ArduSim.SIMULATOR_CLI){
			Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
		}

		while (Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
			ardusim.sleep(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.SETUP_IN_PROGRESS) {
			if (Param.role == ArduSim.MULTICOPTER) {
				API.getCopter(0).cancelRCOverride();
			}
			return;
		}

		// 13. Apply the setup step
		final ValidationTools validationTools = API.getValidationTools();
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
						final String timeString = validationTools.timeToString(Param.setupTime, System.currentTimeMillis());
						SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE_2 + " " + timeString));
					} else if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						final String timeString = validationTools.timeToString(Param.startTime, System.currentTimeMillis());
						SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE + " " + timeString));
					} else {
						SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE));
						if (Param.simStatus != SimulatorState.READY_FOR_TEST) {
							timer.cancel();
						}
					}
				}
			}, 0, 1000);	// Once each second, without initial waiting time
		}else if(Param.role == ArduSim.SIMULATOR_CLI){
			Param.startTime = System.currentTimeMillis();
		}
		ArduSimTools.logGlobal(Text.SETUP_START);
		ArduSimTools.selectedProtocolInstance.setupActionPerformed();
		Param.simStatus = SimulatorState.READY_FOR_TEST;

		// 14. Waiting for the user to start the experiment
		if (Param.role == ArduSim.SIMULATOR_GUI) {
			SwingUtilities.invokeLater(() -> {
				MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_START);
				MainWindow.buttonsPanel.startTestButton.setEnabled(true);
			});
			ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
		}else if(Param.role == ArduSim.SIMULATOR_CLI){
			Param.simStatus = SimulatorState.TEST_IN_PROGRESS;
		}
		while (Param.simStatus == SimulatorState.READY_FOR_TEST) {
			ardusim.sleep(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			if (Param.role == ArduSim.MULTICOPTER) {
				API.getCopter(0).cancelRCOverride();
			}
			return;
		}

		// 15. Start the experiment, only if the program is not being closed
		ArduSimTools.logGlobal(Text.TEST_START);
		ArduSimTools.selectedProtocolInstance.startExperimentActionPerformed();

		// 16. Waiting while the experiment is is progress and detecting the experiment end
		int check = 0;
		boolean allStarted = false;
		while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
			// Detect if all the UAVs have started the experiment
			if (!allStarted) {
				if (ArduSimTools.isTestStarted()) {
					allStarted = true;
				}
			}
			// Check the battery level periodically
			if (check % UAVParam.BATTERY_PRINT_PERIOD == 0) {
				ArduSimTools.checkBatteryLevel();
			}
			check++;
			// Force the UAVs to land if needed
			ArduSimTools.selectedProtocolInstance.forceExperimentEnd();
			// Detects if all UAVs are on the ground in order to finish the experiment
			if (allStarted) {
				if (ArduSimTools.isTestFinished()) {
					Param.simStatus = SimulatorState.TEST_FINISHED;
				}
			}
			
			if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
				ardusim.sleep(SimParam.LONG_WAITING_TIME);
			}
		}
		if (Param.role == ArduSim.MULTICOPTER) {
			API.getCopter(0).cancelRCOverride();
		}else if(Param.role == ArduSim.SIMULATOR_GUI) {
			ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
		}
		if (Param.simStatus != SimulatorState.TEST_FINISHED) {
			return;
		}
		// 17. Inform that the experiment has finished, and wait virtual communications to be closed
		ArduSimTools.logGlobal(validationTools.timeToString(Param.startTime, Param.latestEndTime) + " " + Text.TEST_FINISHED);
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			if (Param.numUAVs > 1) {
				ArduSimTools.logGlobal(Text.SHUTTING_DOWN_COMM);
				int numThreads = 2 * Param.numUAVs;
				long now = System.currentTimeMillis();
				// Maybe the communications were not used at all
				while(CommLinkObject.communicationsClosed != null
						&& CommLinkObject.communicationsClosed.size() < numThreads
						&& System.currentTimeMillis() - now < CommLinkObject.CLOSSING_WAITING_TIME) {
					ardusim.sleep(SimParam.SHORT_WAITING_TIME);
				}
			}
			if(Param.role == ArduSim.SIMULATOR_GUI) {
				ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
				SwingUtilities.invokeLater(() -> MainWindow.buttonsPanel.statusLabel.setText(Text.TEST_FINISHED));
			}
		}

		// 18. Gather information to show the results dialog
		if(Param.storeData) {
			String res = ArduSimTools.getTestResults();
			String s = ArduSimTools.selectedProtocolInstance.getExperimentResults();
			if (s != null && s.length() > 0) {
				res += "\n" + ArduSimTools.selectedProtocol + ":\n\n";
				res += s;
			}
			res += ArduSimTools.getTestGlobalConfiguration();
			s = ArduSimTools.selectedProtocolInstance.getExperimentConfiguration();
			if (s != null && s.length() > 0) {
				res += "\n\n" + ArduSimTools.selectedProtocol + " " + Text.CONFIGURATION + ":\n";
				res += s;
			}
			if (Param.role == ArduSim.MULTICOPTER || Param.role == ArduSim.SIMULATOR_CLI) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(cal.getTimeInMillis() + Param.timeOffset);
				String fileName = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1)
						+ "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY)
						+ "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND) + " " + Text.DEFAULT_BASE_NAME;
				ArduSimTools.storeResults(res, new File(parentFolder, fileName));
			} else if (Param.role == ArduSim.SIMULATOR_GUI) {
				final String res2 = res;
				SwingUtilities.invokeLater(() -> new ResultsDialog(res2, MainWindow.window.mainWindowFrame, true));
			}
		}
		if(Param.role != ArduSim.SIMULATOR_GUI){
			// shutdown ardusim
			ArduSimTools.shutdown();
		}
		// Now, the user must close the application, even to do a new experiment
	}
}
