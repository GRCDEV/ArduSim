package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;

import org.javatuples.Pair;

import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
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
		// Parse the command line arguments
		if (!Tools.parseArgs(args)) {
			return;
		}

		System.setProperty("sun.java2d.opengl", "true");
		Param.simStatus = SimulatorState.CONFIGURING;
		Tools.detectOS();
		if (Param.IS_REAL_UAV) {
			// 1. We need to make constant the parameters shown in the GUI
			Param.numUAVs = 1;	// Always one UAV per Raspberry Pi or whatever the device where the application is deployed

			// 2. Establish the identifier of the UAV
			Param.id = new long[Param.numUAVs];
			Param.id[0] = Tools.getRealId();

			// 3. Mission file loading
			// The mission is loaded from a file on the same folder as the jar file
			boolean loadMission;
			if (Param.simulationIsMissionBased) {
				loadMission = MissionHelper.loadMission();
			} else {
				loadMission = SwarmHelper.loadMission();
			}
			File parentFolder = GUIHelper.getCurrentFolder();
			if (loadMission) {
				List<Waypoint> mission = Tools.loadMission(parentFolder);
				if (mission == null) {
					GUIHelper.exit(Text.MISSION_NOT_FOUND);
				}
				UAVParam.missionGeoLoaded = new ArrayList[1];
				UAVParam.missionGeoLoaded[0] = mission;
			}
			
			// 4. Start threads for waiting to commands to start the setup and start steps of the experiment
			(new ExperimentTalker()).start();
			(new ExperimentListener()).start();
		} else {
			// 1. Opening the general configuration dialog
			Param.simStatus = SimulatorState.CONFIGURING;

			Tools.locateSITL();
			Tools.checkAdminPrivileges();
			try {
				UAVParam.mavPort = Tools.getSITLPorts();
				if (UAVParam.mavPort.length < UAVParam.MAX_SITL_INSTANCES) {
					GUIHelper.warn(Text.PORT_ERROR, Text.PORT_ERROR_1 + UAVParam.mavPort.length + " " + Text.UAV_ID + "s");
				}
			} catch (InterruptedException | ExecutionException e) {
				GUIHelper.exit(Text.PORT_ERROR_2);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				Tools.checkImdiskInstalled();
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					new ConfigDialog();
				}
			});
			// Waiting the configuration to be finished
			while (Param.simStatus == SimulatorState.CONFIGURING) {
				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 2. Opening the configuration dialog of the protocol under test
			if (Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL) {
				if (Param.simulationIsMissionBased) {
					MissionHelper.openMissionConfigurationDialog();
				} else {
					SwarmHelper.openSwarmConfigurationDialog();
				}
				// Waiting the protocol configuration to be finished
				while (Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL) {
					GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
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
		Tools.initializeDataStructures();
		if (Param.simulationIsMissionBased) {
			MissionHelper.initializeMissionDataStructures();
		} else {
			SwarmHelper.initializeSwarmDataStructures();
		}
		// Waiting the main window to be built
		if (!Param.IS_REAL_UAV) {
			while (MainWindow.boardPanel == null || MainWindow.buttonsPanel == null) {
				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 5. Initial GUI configuration and launch the progress dialog
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow.buttonsPanel.logArea.setText(Text.STARTING_ENVIRONMENT + "\n");
					MainWindow.buttonsPanel.progressDialogButton.setEnabled(false);
					if (!Param.simulationIsMissionBased) {
						MainWindow.buttonsPanel.setupButton.setEnabled(false);
					}
					MainWindow.buttonsPanel.startTestButton.setEnabled(false);
					MainWindow.buttonsPanel.statusLabel.setText(Text.STARTING_ENVIRONMENT);
					MainWindow.progressDialog = new ProgressDialog(MainWindow.window.mainWindowFrame);
					MainWindow.progressDialog.setVisible(true);
				}
			});
			// Waiting the progress dialog to be built
			while (!SimParam.progressShowing || MainWindow.progressDialog == null) {
				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 6. Load needed resources
			SimTools.loadUAVImage();
			if (Param.simulationIsMissionBased) {
				MissionHelper.loadMissionResources();
			} else {
				SwarmHelper.loadSwarmResources();
			}
		}
		// Configuration feedback
		SimTools.println(Text.PROTOCOL_IN_USE + " " + Param.selectedProtocol.getName());
		if (!Param.IS_REAL_UAV) {
			if (SimParam.userIsAdmin
					&& ((Param.runningOperatingSystem == Param.OS_WINDOWS && SimParam.imdiskIsInstalled)
							|| Param.runningOperatingSystem == Param.OS_LINUX)) {
				SimTools.println(Text.USING_RAM_DRIVE);
			} else {
				SimTools.println(Text.USING_HARD_DRIVE);
			}
			if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
				if (SimParam.userIsAdmin && !SimParam.imdiskIsInstalled) {
					SimTools.println(Text.INSTALL_IMDISK);
				}
				if (!SimParam.userIsAdmin) {
					if (SimParam.imdiskIsInstalled) {
						SimTools.println(Text.USE_ADMIN);
					} else {
						SimTools.println(Text.INSTALL_IMDISK_USE_ADMIN);
					}
				}
			}
			if (Param.runningOperatingSystem == Param.OS_LINUX && !SimParam.userIsAdmin) {
				SimTools.println(Text.USE_ROOT);
			}
			SimTools.println(Text.WIRELESS_MODEL_IN_USE + " " + Param.selectedWirelessModel.getName());
			if (Param.windSpeed != 0.0) {
				SimTools.println(Text.SIMULATED_WIND_SPEED + " " + Param.windSpeed);
			}
		}

		// 7. Automatic screen update activation (position logging when in a real UAV)
		SimTools.update();

		// 8. Startup of the virtual UAVs
		if (!Param.IS_REAL_UAV) {
			BoardHelper.buildWindImage(MainWindow.boardPanel);
			Pair<GeoCoordinates, Double>[] start;
			if (Param.simulationIsMissionBased) {
				start = MissionHelper.getMissionStartingLocation();
			} else {
				start = SwarmHelper.getSwarmStartingLocation();
			}
			SimParam.tempFolderBasePath = Tools.defineTemporaryFolder();
			if (SimParam.tempFolderBasePath == null) {
				GUIHelper.exit(Text.TEMP_PATH_ERROR);
			}
			Tools.startVirtualUAVs(start);
		}

		// 9. Start UAV controllers, wait for MAVLink link, wait for GPS fix, and send basic configuration
		Tools.startUAVControllers();
		Tools.waitMAVLink();
		if (!Param.IS_REAL_UAV) {
			Tools.forceGPS();
		}
		Tools.getGPSFix();
		Tools.sendBasicConfiguration();
		
		// 10. Set communications online, and start collision detection if needed
		if (!Param.IS_REAL_UAV && Param.numUAVs > 1) {
			(new DistanceCalculusThread()).start();
			(new RangeCalculusThread()).start();
			if (UAVParam.collisionCheckEnabled) {
				(new CollisionDetector()).start();
			}
		}
		
		// 11. Launch the threads of the protocol under test
		if (Param.simulationIsMissionBased) {
			MissionHelper.launchMissionThreads();
		} else {
			SwarmHelper.launchSwarmThreads();
		}

		// 12. Build auxiliary elements to be drawn and prepare the user interaction
		//    The background map can not be downloaded until the GUI detects that all the missions are loaded
		//      AND the drawing scale is calculated
		if (Param.IS_REAL_UAV) {
			Param.simStatus = SimulatorState.UAVS_CONFIGURED;
		}
		while (Param.simStatus == SimulatorState.STARTING_UAVS) {
			GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
		}
		if (Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
			if (!Param.IS_REAL_UAV) {
				BoardHelper.downloadBackground();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (Param.simulationIsMissionBased) {
							MainWindow.buttonsPanel.startTestButton.setEnabled(true);
						} else {
							MainWindow.buttonsPanel.setupButton.setEnabled(true);
						}
						MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_FLY);
					}
				});
				if (Param.simulationIsMissionBased) {
					Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
				} else {
					SimTools.println(Text.WAITING_FOR_USER);
				}
			}
			while (Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
			}

			// 13. Apply the configuration step, only if the program is not being closed (state SHUTTING_DOWN)
			if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
				if (Param.simulationIsMissionBased) {
					Param.simStatus = SimulatorState.READY_FOR_TEST;
				} else {
					SwarmHelper.swarmSetupActionPerformed();
				}
				while (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
					GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
				}

				// 14. Waiting for the user to start the experiment, only if the program is not being closed
				if (Param.simStatus == SimulatorState.READY_FOR_TEST) {
					if (!Param.IS_REAL_UAV) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_START);
								MainWindow.buttonsPanel.startTestButton.setEnabled(true);
							}
						});
						SimTools.println(Text.WAITING_FOR_USER);
					}
					while (Param.simStatus == SimulatorState.READY_FOR_TEST) {
						GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
					}

					// 15. Start the experiment, only if the program is not being closed
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						
						
//						System.out.println("HA LLEGADO CORRECTAMENTE HASTA EJECUTARLO TODO");
//						GUIHelper.waiting(15000);//TODO borrar esto
//						System.exit(0);
						
						
						
						// TODO remove new threads and clean the console parameters (args)
						if (UAVParam.doFakeSending) {
							for (int i = 0; i < Param.numUAVs; i++) {
								(new FakeSenderThread(i)).start();
								(new FakeReceiverThread(i)).start();
							}
						}
						





						Param.startTime = System.currentTimeMillis();
						SimTools.println(Text.TEST_START);
						if (!Param.IS_REAL_UAV) {
							timer = new Timer();
							timer.scheduleAtFixedRate(new TimerTask() {
								long time = Param.startTime;
								public void run() {
									if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
										final String timeString = GUIHelper.timeToString(Param.startTime, time);
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
						if (Param.simulationIsMissionBased) {
							MissionHelper.startMissionTestActionPerformed();
						} else {
							SwarmHelper.startSwarmTestActionPerformed();
						}

						// 16. Waiting while the experiment is is progress and detecting the experiment end
						while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
							Tools.checkBatteryLevel();
							if (Param.simulationIsMissionBased) {
								// Land all the UAVs when they reach the last waypoint
								MissionHelper.detectMissionEnd();
							} else {
								SwarmHelper.detectSwarmEnd();
							}
							// Detects if all UAVs are on the ground in order to finish the experiment
							if (Tools.isTestFinished()) {
								Param.simStatus = SimulatorState.TEST_FINISHED;
							}
							if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
								GUIHelper.waiting(SimParam.LONG_WAITING_TIME);
							}
						}

						// 17. Wait for the user to close the simulator, only if the program is not being closed
						if (Param.simStatus == SimulatorState.TEST_FINISHED) {
							SimTools.println(GUIHelper.timeToString(Param.startTime, Param.latestEndTime) + " " + Text.TEST_FINISHED);
							if (!Param.IS_REAL_UAV) {
								SimTools.println(Text.WAITING_FOR_USER);
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										MainWindow.buttonsPanel.statusLabel.setText(Text.TEST_FINISHED);
									}
								});
							}

							// Gather information to show the results dialog
							String res = null;//después terminar revisión interfaz prot y nombre clases
							res = Tools.getTestResults();
							if (Param.simulationIsMissionBased) {
								res += MissionHelper.getMissionTestResults();
							} else {
								res += SwarmHelper.getSwarmTestResults();
							}
							res += Tools.getTestGlobalConfiguration();
							if (Param.simulationIsMissionBased) {
								res += MissionHelper.getMissionProtocolConfig();
							} else {
								res += SwarmHelper.getSwarmProtocolConfig();
							}

							final String res2 = res;
							if (Param.IS_REAL_UAV) {
								Calendar cal = Calendar.getInstance();
								String fileName = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH)+1)
										+ "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY)
										+ "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND) + " " + Text.DEFAULT_BASE_NAME;
								Tools.storeResults(res2, new File(GUIHelper.getCurrentFolder(), fileName));
								Tools.shutdown();	// Closes the simulator
							} else {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										new ResultsDialog(res2, MainWindow.window.mainWindowFrame, true);
									}
								});
							}

							// Now, the user must close the application, even to do a new experiment
						}
					}
				}
			}
		}
	}
}
