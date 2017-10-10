package main;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.javatuples.Pair;

import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.Waypoint;
import main.Param.Protocol;
import main.Param.SimulatorState;
import sim.board.BoardHelper;
import sim.gui.ConfigDialog;
import sim.gui.MainWindow;
import sim.gui.ProgressDialog;
import sim.gui.ResultsDialog;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVParam;

/** This class contains the main method and the chronological logic followed by the whole application. */

public class Main {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Tools.detectOS();
		if (Param.IS_REAL_UAV) {
			// 1. Protocol file loading (file containing the name of the protocol to be used)
			// Protocol, mission, and speed are loaded from files on the same folder as the jar file
			File parentFolder = GUIHelper.getCurrentFolder();
			Protocol protocol = null;
			protocol = Tools.loadProtocol(parentFolder);
			if (protocol == null) {
				GUIHelper.exit(Text.PROTOCOL_NOT_FOUND);
			}
			Param.selectedProtocol = protocol;
			Param.simulationIsMissionBased = Param.selectedProtocol.isMissionBased();
			
			// 2. We need to make constant the parameters shown in the GUI
			Param.numUAVs = 1;	// Always one UAV per Raspberry Pi or whatever the device where the application is deployed
			
			// Establish the identifier of the UAV
			Param.id = new long[Param.numUAVs];
			Param.id[0] = Tools.getRealId();
			
			// Mission file loading
			boolean loadMission;
			if (Param.simulationIsMissionBased) {
				loadMission = MissionHelper.loadMission();
			} else {
				loadMission = SwarmHelper.loadMission();
			}
			if (loadMission) {
				List<Waypoint> mission = Tools.loadMission(parentFolder);
				if (mission == null) {
					GUIHelper.exit(Text.MISSION_NOT_FOUND);
				}
				UAVParam.missionGeoLoaded = new ArrayList[1];
				UAVParam.missionGeoLoaded[0] = mission;
			}
			
			// Speeds file loading
			Double speed = null;
			speed = Tools.loadSpeed(parentFolder);
			if (speed == null) {
				GUIHelper.exit(Text.SPEEDS_NOT_FOUND);
			}
			UAVParam.initialSpeeds = new double[1];
			UAVParam.initialSpeeds[0] = speed;
			
			
		} else {
			// 1. Opening the general configuration dialog
			Param.simStatus = SimulatorState.CONFIGURING;
			
			Tools.locateSITL();
			EventQueue.invokeLater(new Runnable() {
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
			EventQueue.invokeLater(new Runnable() {
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
		SimTools.println(Text.CAP_IN_USE + " " + Param.selectedProtocol.getName());
		if (!Param.IS_REAL_UAV) {
			SimTools.println(Text.WIRELESS_MODEL_IN_USE + " " + Param.selectedWirelessModel.getName());
			if (Param.windSpeed != 0.0) {
				SimTools.println(Text.SIMULATED_WIND_SPEED + " " + Param.windSpeed);
			}
		}

		// 7. Automatic screen update activation (position logging when in a real UAV
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

		// 10. Launch the threads of the protocol under test
		if (Param.simulationIsMissionBased) {
			MissionHelper.launchMissionThreads();
		} else {
			SwarmHelper.launchSwarmThreads();
		}

		// 11. Build auxiliary elements to be drawn and prepare the user interaction
		//    The background map can not be downloaded until the GUI detects that all the missions are loaded
		//      AND the drawing scale is calculated
		if (Param.IS_REAL_UAV) {
			Param.simStatus = SimulatorState.UAVS_CONFIGURED;
		}
		while (Param.simStatus == SimulatorState.STARTING_UAVS) {
			GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
		}
		
		if (Param.IS_REAL_UAV) {
			Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
		} else {
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
			// Waiting for the user to start the configuration step
			if (Param.simulationIsMissionBased) {
				Param.simStatus = SimulatorState.SETUP_IN_PROGRESS;
			} else {
				SimTools.println(Text.WAITING_FOR_USER);
			}
		}
		while (Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
			GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
		}
		
		// 12. Apply the configuration step, only if the program is not being closed (state SHUTTING_DOWN)
		if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
			if (Param.simulationIsMissionBased) {
				Param.simStatus = SimulatorState.READY_FOR_TEST;
			} else {
				SwarmHelper.swarmSetupActionPerformed();
			}
			while (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
				GUIHelper.waiting(SimParam.SHORT_WAITING_TIME);
			}
			
			// 13. Waiting for the user to start the experiment, only if the program is not being closed
			if (Param.simStatus == SimulatorState.READY_FOR_TEST) {
				if (Param.IS_REAL_UAV) {
					Param.simStatus = SimulatorState.TEST_IN_PROGRESS;
				} else {
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
				
				// 14. Start the experiment, only if the program is not being closed
				if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
					Param.startTime = System.currentTimeMillis();
					SimTools.println(Text.TEST_START);
					if (Param.simulationIsMissionBased) {
						MissionHelper.startMissionTestActionPerformed();
					} else {
						SwarmHelper.startSwarmTestActionPerformed();
					}

					// 15. Waiting while the experiment is is progress and detecting the experiment end
					while (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						if (Param.simulationIsMissionBased) {
							// Land all the UAVs when reach the last waypoint
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

					// 16. Wait for the user to close the simulator, only if the program is not being closed
					if (Param.simStatus == SimulatorState.TEST_FINISHED) {
						SimTools.println(Text.TEST_FINISHED);
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
							Tools.storeResults(res2, new File(GUIHelper.getCurrentFolder(), Text.DEFAULT_BASE_NAME));
							Tools.shutdown(null);	// Closes the simulator
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
