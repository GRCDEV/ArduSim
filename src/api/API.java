package api;

import java.util.concurrent.atomic.AtomicReferenceArray;

import main.Param;
import main.api.ArduSim;
import main.api.Copter;
import main.api.FileTools;
import main.api.FlightFormationTools;
import main.api.GUI;
import main.api.ValidationTools;
import main.api.communications.CommLink;
import main.uavController.UAVParam;

/**
 * This class contains the API needed to implement a protocol.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class API {
	
	private static ArduSim ardusim = new ArduSim();
	
	private static volatile AtomicReferenceArray<CommLink> publicCommLink = null;
	private static final Object lockComm = new Object();
	
	private static volatile AtomicReferenceArray<Copter> copter = null;
	private static final Object lockCopter = new Object();
	
	private static FileTools fileTools = new FileTools();
	
	private static FlightFormationTools flightTools = new FlightFormationTools();
	
	private static volatile AtomicReferenceArray<GUI> gui = null;
	private static final Object lockGUI = new Object();
	
	private static volatile ValidationTools validationTools = new ValidationTools();
	
	private API() {}
	
	/**
	 * Get the application context needed to coordinate the protocol with ArduSim.
	 * @return The application context
	 */
	public static ArduSim getArduSim() {
		
		return API.ardusim;

	}
	
	/**
	 * Get the communication link needed for UAV-to-UAV communications.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Communication link used by the UAV to communicate with the other UAVs.
	 */
	public static CommLink getCommLink(int numUAV) {
		
		synchronized (lockComm) {
			if (API.publicCommLink == null) {
				API.publicCommLink = new AtomicReferenceArray<CommLink>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					API.publicCommLink.set(i, new CommLink(i, Param.numUAVs, UAVParam.broadcastPort));
				}
			}
		}
		
		return API.publicCommLink.get(numUAV);
	}
	
	/**
	 * Get the Copter instance to control the multicopter.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Copter object that allows to manipulate the multicopter.
	 */
	public static Copter getCopter(int numUAV) {
		
		synchronized (lockCopter) {
			if (API.copter == null) {
				API.copter = new AtomicReferenceArray<Copter>(Param.numUAVs);
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
	 * Get tools to build and manage flight formations.
	 * @return Tools to manage flight formations.
	 */
	public static FlightFormationTools getFlightFormationTools() {
		
		return API.flightTools;
		
	}
	
	/**
	 * Get the GUI to interact with it.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return GUI object that allows to interact with the graphical user interface.
	 */
	public static GUI getGUI(int numUAV) {
		
		synchronized (lockGUI) {
			if (API.gui == null) {
				API.gui = new AtomicReferenceArray<GUI>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					API.gui.set(i, new GUI(i));
				}
			}
			// We have to resize because the GUI object could be used before closing the configuration dialog
			int size = API.gui.length();
			if (size != Param.numUAVs) {
				AtomicReferenceArray<GUI> aux = new AtomicReferenceArray<GUI>(Param.numUAVs);
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
