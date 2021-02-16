package com.api.communications;

import com.api.API;
import com.api.ArduSim;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UAV-to-UAV communications link.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CommLink {
	
	/**
	 * TCP parameter: maximum size of the byte array used on messages.
	 * It is based on the Ethernet MTU, and assumes that IP and UDP main.java.com.api.protocols are used. */
	public static final int DATAGRAM_MAX_LENGTH = 1472; 		// (bytes) 1500-20-8 (MTU - IP - UDP)
	
	private int numUAV;

	private static volatile CommLinkObject commLinkObject = null;
	private static final Object PUBLIC_LOCK = new Object();
	
	private volatile boolean advertised = false;

	/**
	 * Get the communication link needed for UAV-to-UAV communications.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Communication link used by the UAV to communicate with the other UAVs.
	 */
	public static CommLink getCommLink(int numUAV) {
		return new CommLink(numUAV,Param.numUAVs,UAVParam.broadcastPort);
	}

	/**
	 * Private constructor of CommLink in order to facilitate the singletonPattern
	 * @param numUAV: the number of the current UAV (identifier)
	 * @param numUAVs: total number of UAVs
	 * @param port: port used while sending messages
	 */
	private CommLink(int numUAV, int numUAVs, int port) {
		this.numUAV = numUAV;
		synchronized(PUBLIC_LOCK) {
			if (commLinkObject == null) {
				commLinkObject = new CommLinkObject(numUAVs, port);
			}
		}
	}

	/**
	 * Method to initialize various parameters
	 * @param numUAVs: number of UAVs
	 * @param carrierSensing: boolean to turn on carrier sensing
	 * @param packetCollisionDetection: boolean to turn on packet Collision Detection
	 * @param bufferSize: size of the buffer
	 */
	public static void init(int numUAVs,boolean carrierSensing, boolean packetCollisionDetection, int bufferSize){
		CommLinkObject.carrierSensingEnabled = carrierSensing;
		CommLinkObject.pCollisionEnabled = packetCollisionDetection;
		CommLinkObject.receivingBufferSize = bufferSize;
		CommLinkObject.receivingvBufferSize = CommLinkObject.V_BUFFER_SIZE_FACTOR * CommLinkObject.receivingBufferSize;
		CommLinkObject.receivingvBufferTrigger = (int)Math.rint(CommLinkObject.BUFFER_FULL_THRESHOLD * CommLinkObject.receivingvBufferSize);

		// Collision and communication range parameters
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			UAVParam.distances = new AtomicReference[numUAVs][numUAVs];
			CommLinkObject.isInRange = new AtomicBoolean[numUAVs][numUAVs];
			for (int i = 0; i < numUAVs; i++) {
				for (int j = 0; j < numUAVs; j++) {
					UAVParam.distances[i][j] = new AtomicReference<>();
					CommLinkObject.isInRange[i][j] = new AtomicBoolean();
				}
			}
		}
	}

	/**
	 * Method to close all the communicationLinks
	 */
	public static void close(){
		int numThreads = 2 * Param.numUAVs;
		long now = System.currentTimeMillis();
		// Maybe the communications were not used at all
		while (CommLinkObject.communicationsClosed != null
				&& CommLinkObject.communicationsClosed.size() < numThreads
				&& System.currentTimeMillis() - now < CommLinkObject.CLOSSING_WAITING_TIME) {
			API.getArduSim().sleep(SimParam.SHORT_WAITING_TIME);
		}
	}
	
	/**
	 * Send a message to other UAVs. This function is only allowed for real or virtual multicopters. In other words, the PC Companion cannot inject messages during the experiment, but it can listen to messages from the real multicopters.
	 * <p>Blocking method. Depending on the system implementation, the message can also be received by the sender UAV. Please, avoid sending messages from more than one thread at a time.</p>
	 * @param message Message to be sent, and encoded by the protocol.
	 */
	public void sendBroadcastMessage(byte[] message) {
		if (Param.role == ArduSim.PCCOMPANION) {
			if (!advertised) {
				API.getGUI(0).log(Text.SEND_NOT_PERMITTED);
				advertised = true;
			}
		} else {
			commLinkObject.sendBroadcastMessage(numUAV, message);
		}
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @return The message received, or null if a fatal error with the socket happens.
	 */
	public byte[] receiveMessage() {
		return commLinkObject.receiveMessage(numUAV, 0);
	}
	
	/**
	 * Receive a message from another UAV.
	 * <p>Blocking method until the socketTimeout is reached. Please, avoid receiving messages from more than one thread at a time.</p>
	 * @param socketTimeout (ms) Duration of the timeout. A zero or negative value blocks until a message is received.
	 * @return The message received, or null if a fatal error with the socket happens or the timeout is reached.
	 */
	public byte[] receiveMessage(int socketTimeout) {
		return commLinkObject.receiveMessage(numUAV, socketTimeout);
	}

	/** 
	 * Generates a String representation of the communication statistics, once the experiment has finished.
	 */
	@Override
	public String toString() {
		return commLinkObject.toString();
	}
}
