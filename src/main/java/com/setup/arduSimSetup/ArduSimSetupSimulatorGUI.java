package com.setup.arduSimSetup;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.gui.ConfigDialogApp;
import com.setup.sim.gui.MainWindow;
import com.setup.sim.gui.ProgressDialog;
import com.setup.sim.gui.ResultsDialog;
import com.setup.sim.logic.SimParam;
import com.setup.sim.logic.SimTools;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArduSimSetupSimulatorGUI extends ArduSimSetupSimulator{

    public ArduSimSetupSimulatorGUI(){}

    @Override
    protected void setGeneralParameters(){
        // Start the javafxml GUI configDialogApp
        Platform.startup(() -> new ConfigDialogApp().start(new Stage()));
        while(Param.simStatus != Param.SimulatorState.CONFIGURING_PROTOCOL){
            API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
        }
    }
    @Override
    protected void lauchMainWindow(){
        SwingUtilities.invokeLater(() -> MainWindow.window = new MainWindow());
    }

    @Override
    protected void loadProtocolConfiguration(){
        ArduSimTools.selectedProtocolInstance.openConfigurationDialogFX();
        //startSwingconfigurationDialog();
        while(Param.simStatus != Param.SimulatorState.STARTING_UAVS){
            API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
        }
    }

    @Override
    protected void launchProgressDialog(){
        // Waiting the main window to be built
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

    @Override
    protected void updateWindImage(){
        if (Param.windSpeed > 0.0) {
        MainWindow.window.buildWindImage();
        }
    }

    @Override
    protected void clickSetup(){
        SwingUtilities.invokeLater(() -> {
            MainWindow.buttonsPanel.setupButton.setEnabled(true);
            MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_FLY);
        });
        ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
        while(Param.simStatus != Param.SimulatorState.SETUP_IN_PROGRESS){
            ardusim.sleep(SimParam.SHORT_WAITING_TIME);
        }
    }

    @Override
    protected void clickStart(){
        SwingUtilities.invokeLater(() -> {
            MainWindow.buttonsPanel.statusLabel.setText(Text.READY_TO_START);
            MainWindow.buttonsPanel.startTestButton.setEnabled(true);
        });
        ArduSimTools.logGlobal(Text.WAITING_FOR_USER);
    }

    @Override
    protected void setTimerExperimentRunning(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (Param.simStatus == Param.SimulatorState.SETUP_IN_PROGRESS) {
                    final String timeString = validationTools.timeToString(Param.setupTime, System.currentTimeMillis());
                    SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE_2 + " " + timeString));
                } else if (Param.simStatus == Param.SimulatorState.TEST_IN_PROGRESS) {
                    final String timeString = validationTools.timeToString(Param.startTime, System.currentTimeMillis());
                    SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE + " " + timeString));
                } else {
                    SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.setTitle(Text.PROGRESS_DIALOG_TITLE));
                    if (Param.simStatus != Param.SimulatorState.READY_FOR_TEST) {
                        timer.cancel();
                    }
                }
            }
        }, 0, 1000);	// Once each second, without initial waiting time
    }

    @Override
    protected void saveResults(String results){
        SwingUtilities.invokeLater(() -> new ResultsDialog(results, MainWindow.window.mainWindowFrame, true));
    }
    @Deprecated
    private void startSwingconfigurationDialog() {
        final AtomicBoolean configurationOpened = new AtomicBoolean();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final JDialog configurationDialog = ArduSimTools.selectedProtocolInstance.openConfigurationDialog();
                    if (configurationDialog ==null) {
                        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
                    } else {
                        configurationDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                        configurationDialog.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent we) {
                                configurationDialog.dispose();
                                System.gc();
                                System.exit(0);
                            }

                            @Override
                            public void windowClosed(WindowEvent e) {
                                configurationOpened.set(false);
                            }
                        });

                        SimTools.addEscListener(configurationDialog, true);

                        configurationDialog.pack();
                        configurationDialog.setResizable(false);
                        configurationDialog.setLocationRelativeTo(null);
                        configurationDialog.setModal(true);
                        configurationDialog.setVisible(true);
                        configurationOpened.set(true);
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            ArduSimTools.closeAll("configuration error");
        }

        // Waiting the protocol configuration to be finished
        while (Param.simStatus == Param.SimulatorState.CONFIGURING_PROTOCOL) {
            ardusim.sleep(SimParam.SHORT_WAITING_TIME);

            if (!configurationOpened.get()) {
                Param.simStatus = Param.SimulatorState.STARTING_UAVS;
            }
        }
        updateNumUAVs();
    }
}
