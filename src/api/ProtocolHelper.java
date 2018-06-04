package api;

import java.awt.Graphics2D;

import org.javatuples.Pair;

import api.pojo.GeoCoordinates;
import main.Param;
import main.Text;
import mbcap.logic.MBCAPText;
import sim.board.BoardPanel;
import swarmprot.logic.SwarmProtText;

public abstract class ProtocolHelper {
	
	// Selected protocol
	public static volatile Protocol selectedProtocol;
	public static volatile ProtocolHelper selectedProtocolInstance;
	
	// Protocols enumerator
	public enum Protocol {
		NONE(0, Text.PROTOCOL_NONE, true),			// Without protocol, only a mission based experiment can be done
		MBCAP_V1(1, MBCAPText.MBCAP_V1, true),	// Changes the prediction objective when the UAV reaches a waypoint
		MBCAP_V2(2, MBCAPText.MBCAP_V2, true),	// Adapts the prediction to the theoretical like a magnet
		MBCAP_V3(3, MBCAPText.MBCAP_V3, true),	// v2 taking the UAV acceleration into account
		MBCAP_V4(4, MBCAPText.MBCAP_V4, true),	// v3 with a variable acceleration
		SWARM_PROT_V1(5, SwarmProtText.PROTOCOL_TEXT, false),
		FOLLOW_ME_V1(6, "Sigueme", false),
		POLLUTION(7, "Pollución", false),
		UAVFISHING(8, "UAV fishing", false);
		// New protocols should follow the increasing numeration

		private final int id;
		private final String name;
		private final boolean isMissionBased;
		private Protocol(int id, String name, boolean isMissionBased) {
			this.id = id;
			this.name= name;
			this.isMissionBased = isMissionBased;
		}
		public int getId() {
			return this.id;
		}
		public String getName() {
			return this.name;
		}
		public boolean isMissionBased() {
			return this.isMissionBased;
		}
		public static Protocol getHighestIdProtocol() {
			Protocol res = null;
			for (Protocol p : Protocol.values()) {
				if (Param.simulationIsMissionBased == p.isMissionBased) {
					if (res == null) {
						res = p;
					} else if (p.getId() > res.getId()) {
						res = p;
					}
				}
			}
			return res;
		}
		public static String getProtocolNameById(int id) {
			for (Protocol p : Protocol.values()) {
				if (p.getId() == id) {
					return p.getName();
				}
			}
			return "";
		}
		/** Returns the protocol given its name.
		 * <p>Returns null if the protocol was not found. */
		public static Protocol getProtocolByName(String name) {
			for (Protocol p : Protocol.values()) {
				if (p.getName().toUpperCase().equals(name.toUpperCase())) {
					return p;
				}
			}
			return null;
		}
	}
	
	public ProtocolHelper.Protocol protocol = null;
	
	/** Assign a protocol to this implementation. It is mandatory to do this. Sentence similar to:
	 * <p>this.protocol = ProtocolHelper.Protocol.SOME_PROTOCOL; */
	public abstract void setProtocol();
	
	/** Asserts if it is needed to load a mission, when using protocol on a real UAV. On mission based protocols always load a mission file. */
	public abstract boolean loadMission();
	
	/** Opens a configuration dialog for protocol specific parameters.
	 * <p>When the dialog is accepted please use the following command:
	 * <p>api.Tools.setProtocolConfigured(true); */
	public abstract void openConfigurationDialog();
	
	/** Initilizes data structures used by the protocol. */
	public abstract void initializeDataStructures();
	
	/** Sets the initial state of the protocol shown in the progress dialog. */
	public abstract String setInitialState();
	
	/** Rescales specific data structures of the protocol when the visualization scale changes. */
	public abstract void rescaleDataStructures();
	
	/** Loads resorces to be shown on screen. */
	public abstract void loadResources();
	
	/** Rescales for visualization the resources used in protocol, each time the visualization scale changes. */
	public abstract void rescaleShownResources();
	
	/** Draws resources used in the protocol. */
	public abstract void drawResources(Graphics2D g2, BoardPanel p);
	
	/** Sets the initial position where the UAVs will appear in the simulator.
	 * <p>Returns the current Geographic coordinates, and the heading. */
	public abstract Pair<GeoCoordinates, Double>[] setStartingLocation();
	
	/** Sends to the specific UAV the basic configuration needed by the protocol, in an early stage before the setup step.
	 * <p>Must be a blocking method.
	 * <p>Must return true only if all the commands issued to the drone were successful. */
	public abstract boolean sendInitialConfiguration(int numUAV);
	
	/** Launch threads needed by the protocol.
	 * <p>In general, they must wait until a condition is met before doing things, as they start before pressing the Setup or Start buttons.*/
	public abstract void startThreads();
	
	/** Actions to perform when the user presses the Setup button.
	 * <p>Must be a blocking method until the setup process if finished.*/
	public abstract void setupActionPerformed();
	
	/** Starts the experiment, moving the UAVs if needed. */
	public abstract void startExperimentActionPerformed();
	
	/** Analyzes if the experiment must be finished and applies measures to make the UAVs land.
	 * <p>For example, it can be finished when the user presses a button.
	 * <p>Finally, ArduSim stops the experiment when all the UAVs landed.*/
	public abstract void forceExperimentEnd();
	
	/** Optionally, provides general results of the experiment to be appended to text shown on the results dialog.*/
	public abstract String getExperimentResults();
	
	/** Optionally, provides the protocol configuration to be appended to the text shown on the results dialog.*/
	public abstract String getExperimentConfiguration();
	
	/** Optionally, store files with information gathered while applying the protocol at the end of the experiment.
	 * <p>folder. Folder where the files will be stored, the same as the main log file.
	 * <p>baseFileName. Base name that must be prepended to the final file name.*/
	public abstract void logData(String folder, String baseFileName);
	
	
}
