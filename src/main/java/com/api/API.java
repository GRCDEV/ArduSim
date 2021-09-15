package com.api;

import com.api.communications.lowLevel.LowLevelCommLink;
import com.api.copter.Copter;
import com.setup.Param;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class contains the API needed to implement a protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class API {
	
	private static final ArduSim ardusim = new ArduSim();
	
	private static volatile AtomicReferenceArray<LowLevelCommLink> publicCommLink = null;
	private static final Object lockComm = new Object();
	
	private static volatile AtomicReferenceArray<Copter> copter = null;
	private static final Object lockCopter = new Object();
	
	private static final FileTools fileTools = new FileTools();

	private static volatile AtomicReferenceArray<GUI> gui = null;
	private static final Object lockGUI = new Object();
	
	private static final ValidationTools validationTools = new ValidationTools();
	
	private API() {}
	
	/**
	 * Get the application context needed to coordinate the protocol with ArduSim.
	 * @return The application context
	 */
	public static ArduSim getArduSim() {
		
		return API.ardusim;

	}

	/**
	 * Get the Copter instance to control the multicopter.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Copter object that allows to manipulate the multicopter.
	 */
	public static Copter getCopter(int numUAV) {
		
		synchronized (lockCopter) {
			if (API.copter == null) {
				API.copter = new AtomicReferenceArray<>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					API.copter.set(i, new Copter(i));
				}
			}
		}
		
		return API.copter.get(numUAV);
	}
	
	/**
	 * Get tools for parsing and manage files.
	 * @return Tools to manage files.
	 */
	public static FileTools getFileTools() {
		return API.fileTools;
	}

	/**
	 * Get the GUI to interact with it.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return GUI object that allows to interact with the graphical user interface.
	 */
	public static GUI getGUI(int numUAV) {
		
		synchronized (lockGUI) {
			if (API.gui == null) {
				API.gui = new AtomicReferenceArray<>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					API.gui.set(i, new GUI(i));
				}
			}
			// We have to resize because the GUI object could be used before closing the configuration dialog
			int size = API.gui.length();
			if (size != Param.numUAVs) {
				AtomicReferenceArray<GUI> aux = new AtomicReferenceArray<>(Param.numUAVs);
				int i = 0;
				while (i < size && i < aux.length()) {
					aux.set(i, API.gui.get(i));
					i++;
				}
				while (i < Param.numUAVs) {
					aux.set(i, new GUI(i));
					i++;
				}
				API.gui = aux;
			}
		}
		
		return API.gui.get(numUAV);
	}
	
	/**
	 * Get tools to transform text to numbers, and time values to strings.
	 * @return Tools for validating texts.
	 */
	public static ValidationTools getValidationTools() {
		
		return API.validationTools;
		
	}
	
}
