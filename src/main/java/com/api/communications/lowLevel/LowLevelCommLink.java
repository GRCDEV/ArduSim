package com.api.communications.lowLevel;

import com.api.API;
import com.api.ArduSim;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that provides link for UAV-to-UAV communications.
 * @author Francisco jos&eacute; Fabra Collado
 * @author Jamie wubben
 */

public class LowLevelCommLink {
	
	/**
	 * TCP parameter: maximum size of the byte array used on messages.
	 * It is based on the Ethernet MTU, and assumes that IP and UDP protocols are used. 1500-20-8 (MTU - IP - UDP)
	 */
	public static final int DATAGRAM_MAX_LENGTH = 1472;
	/**
	 * Identifier of the UAV using the link
	 */
	private final int numUAV;
	/**
	 * Port to use for communication
	 */
	private final int port;
	/**
	 * Reference to the CommLinkObject by port
	 */
	private static volatile Map<Integer, InterfaceCommLinkObject> links = new HashMap<>();
	/**
	 * To ensure threat safety while creating the new commLinkObject
	 * */
	private static final Object LOCK = new Object();
	/**
	 * boolean to make sure that the errorMessage is only shown once
	 */
	private volatile boolean errorMessageShown = false;


	/**
	 * Get the communication link needed for UAV-to-UAV communications.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Communication link used by the UAV to communicate with the other UAVs.
	 */
	public static LowLevelCommLink getCommLink(int numUAV) {
		return new LowLevelCommLink(numUAV,Param.numUAVs,UAVParam.broadcastPort);
	}

	public static LowLevelCommLink getCommLink(int numUAV, int portNumber) {
		return new LowLevelCommLink(numUAV,Param.numUAVs,portNumber);
	}

	public static LowLevelCommLink getExternalCommLink(int numUAV){
		return new LowLevelCommLink(numUAV);
	}
	/**
	 * Private constructor of CommLink in order to facilitate the singletonPattern
	 * @param numUAV: the number of the current UAV (identifier)
	 * @param numUAVs: total number of UAVs
	 * @param port: port used while sending messages
	 */
	private LowLevelCommLink(int numUAV, int numUAVs, int port) {
		this.numUAV = numUAV;
		if(UAVParam.usingOmnetpp){
			port = 9000;
		}
		this.port = port;
		synchronized(LOCK) {
			if(!links.containsKey(port)) {
				if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
					if(UAVParam.usingOmnetpp){
						links.put(port, new CommLinkObjectOmnetpp(UAVParam.broadcastIP,port));
					}else{
						links.put(port, new CommLinkObjectSimulation(numUAVs, port));
					}
				}else{
					links.put(port, new CommLinkObjectReal(UAVParam.broadcastIP,port,true));
				}
			}
		}
	}

	private LowLevelCommLink(int numUAV){
		this.port = UAVParam.broadcastPort;
		this.numUAV = numUAV;
		synchronized(LOCK) {
			if(!links.containsKey(port)) {
				links.put(port, new CommLinkObjectReal(UAVParam.broadcastIP,port, false));
			}
		}
	}


	/**
	 * Send a message to other UAVs.
	 * The PC companion is not allowed to send messages
	 * Blocking method.
	 * Please, avoid sending messages from more than one thread at a time.
	 * @param message Message to be sent, and encoded by the protocol.
	 */
	public void sendBroadcastMessage(byte[] message) {
		if (Param.role == ArduSim.PCCOMPANION) {
			if (!errorMessageShown) {
				API.getGUI(0).log(Text.SEND_NOT_PERMITTED);
				errorMessageShown = true;
			}
		} else {
			links.get(port).sendBroadcastMessage(numUAV, message);
		}
	}

	/**
	 * Receive a message from another UAV.
	 * Blocking method.
	 * Please, avoid receiving messages from more than one thread at a time.
	 * @return The message received, or null if a fatal error with the socket happens.
	 */
	public byte[] receiveMessage() {
		return links.get(port).receiveMessage(numUAV,0);
	}
	/**
	 * Receive a message from another UAV.
	 * Blocking method until the socketTimeout is reached.
	 * Please, avoid receiving messages from more than one thread at a time.
	 * @param socketTimeout (ms) Duration of the timeout. A zero or negative value blocks until a message is received.
	 * @return The message received, or null if a fatal error with the socket happens or the timeout is reached.
	 */
	public byte[] receiveMessage(int socketTimeout) {
		return links.get(port).receiveMessage(numUAV,socketTimeout);
	}
	/**
	 * Method to close all the communicationLinks
	 */
	public static void close(){
		int numThreads = 2 * Param.numUAVs;
		long now = System.currentTimeMillis();
		// Maybe the communications were not used at all
		while (CommLinkObjectSimulation.communicationsClosed != null
				&& CommLinkObjectSimulation.communicationsClosed.size() < numThreads
				&& System.currentTimeMillis() - now < CommLinkObjectSimulation.CLOSSING_WAITING_TIME) {
			API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
		}
	}
	/** 
	 * Generates a String representation of the communication statistics, once the experiment has finished.
	 */
	@Override
	public String toString() {
		return links.get(port).toString();
	}


}
