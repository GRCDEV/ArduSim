package com.setup.sim.logic;

import es.upv.grc.mapper.Location2DUTM;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import com.api.ArduSim;
import com.setup.sim.gui.ProgressDialog;
import com.uavController.UAVParam;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** This class contains method used internally by the application for its own profit.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class SimTools {
	
	/** Updates the MAVLink flight mode on the progress dialog. */
	public static void updateUAVMAVMode(final int numUAV, final String mode) {
		if (ProgressDialog.progressDialog != null) {
			SwingUtilities.invokeLater(() -> ProgressDialog.progressDialog.panels[numUAV].MAVModeLabel.setText(mode));
		}
		ArduSimTools.logGlobal(SimParam.prefix[numUAV] + Text.FLIGHT_MODE + " = " + mode);
	}
	
	/** Loads initial speed of UAVs from CSV file without header.
	 * <p>One value (m/s) per line. Returns null if the file is not valid or it is empty.</p> */
	public static double[] loadSpeedsFile(String txtFile) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(txtFile))) {
			String line;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Check file length
		if (lines.size()<1) {
			ArduSimTools.logGlobal(Text.SPEEDS_PARSING_ERROR_1);
			return null;
		}
		// Only one line per speed value
		List<Double> speedsList = new ArrayList<>(lines.size());
		String x;
		for (int i=0; i<lines.size(); i++) {
			x = lines.get(i).trim();
			if (x.length() != 0) {
				try {
					speedsList.add(Double.parseDouble(x));
				} catch (NumberFormatException e) {
					ArduSimTools.logGlobal(Text.SPEEDS_PARSING_ERROR_2 + " " + (i+1));
					return null;
				}
			}
		}

		double[] speeds = new double[speedsList.size()];
		for (int i=0; i<speeds.length; i++) {
			speeds[i] = speedsList.get(i);
		}
		return speeds;
	}
	
	/**
	 * For ArduSim internal purposes only.
	 * @param dialog Dialog where this method enables the escape key.
	 * @param closeArduSim Whether to close ArduSim or not when the key is pressed.
	 */
	public static void addEscListener(final JDialog dialog, final boolean closeArduSim) {
	    ActionListener escListener = e -> {
			dialog.dispose();
			System.gc();
			if (closeArduSim) {
				System.exit(0);
			}
		};

	    dialog.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);

	}

	/** Updates the progress dialog periodically. */
	public static void update() {
		ActionListener taskPerformer = evt -> {
			if (ProgressDialog.progressDialog != null) {
				Location2DUTM locationUTM;
				for (int i=0; i<Param.numUAVs; i++) {
					locationUTM = UAVParam.uavCurrentData[i].getUTMLocation();
					if (locationUTM!=null) {
						SimParam.xUTM[i] = locationUTM.x;
						SimParam.yUTM[i] = locationUTM.y;
						SimParam.z[i] = UAVParam.uavCurrentData[i].getZ();
						SimParam.speed[i] = UAVParam.uavCurrentData[i].getHorizontalSpeed();
					}
				}
				if(Param.role == ArduSim.SIMULATOR_GUI) {
					SwingUtilities.invokeLater(() -> {
						for (int i = 0; i < Param.numUAVs; i++) {
							ProgressDialog.progressDialog.panels[i].xLabel.setText(String.format("%.2f", SimParam.xUTM[i]));
							ProgressDialog.progressDialog.panels[i].yLabel.setText(String.format("%.2f", SimParam.yUTM[i]));
							ProgressDialog.progressDialog.panels[i].zLabel.setText(String.format("%.2f", SimParam.z[i]));
							ProgressDialog.progressDialog.panels[i].speedLabel.setText(String.format("%.2f", SimParam.speed[i]));
						}
					});
				}
			}
		};
		new Timer(SimParam.screenUpdatePeriod, taskPerformer).start();
	}
}
