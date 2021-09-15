package com.uavController;

import com.api.API;
import com.api.pojo.FlightMode;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;
import com.api.copter.Copter;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.setup.pccompanion.logic.PCCompanionParam;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/** 
 * Thread used to listen to commands sent from the PC Companion.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TestListener extends Thread {
	
	private static byte[] receivedBuffer;
	private static DatagramSocket receiveSocket = null;
	private static DatagramPacket receivedPacket;
	private static Input input;
	
	public TestListener() throws SocketException {
		receivedBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
		input = new Input(receivedBuffer);
		receiveSocket = new DatagramSocket(PCCompanionParam.uavPort);
		receiveSocket.setBroadcast(true);
	}

	@Override
	public void run() {
		Copter copter = API.getCopter(0);
		
		int command;
		boolean goOn = true;
		while (goOn) {
			try {
				if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
						|| Param.simStatus == SimulatorState.TEST_FINISHED
						|| Param.simStatus == SimulatorState.SHUTTING_DOWN) {
					receiveSocket.setSoTimeout(PCCompanionParam.RECEIVE_TIMEOUT);
				}
				try {
					receivedPacket.setData(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH]);
					receiveSocket.receive(receivedPacket);
					input.setBuffer(receivedPacket.getData());
					command = input.readInt();
					switch (command) {
					// Change simulation state command
					case PCCompanionParam.CHANGE_STATE_COMMAND:
						SimulatorState receivedState = SimulatorState.getStateById(input.readInt());
						if (receivedState != null) {
							if (receivedState == SimulatorState.SETUP_IN_PROGRESS
									&& Param.simStatus == SimulatorState.UAVS_CONFIGURED) {
								Param.timeOffset = input.readLong() - System.currentTimeMillis();
								Param.setupTime = System.currentTimeMillis();
								Param.simStatus = receivedState;
							}

							if (receivedState == SimulatorState.TEST_IN_PROGRESS) {
								if (PCCompanionParam.lastStartCommandTime == null) {
									PCCompanionParam.lastStartCommandTime = new AtomicLong(System.currentTimeMillis());
								} else {
									PCCompanionParam.lastStartCommandTime.set(System.currentTimeMillis());
								}
								if (Param.simStatus == SimulatorState.READY_FOR_TEST) {
									Param.startTime = System.currentTimeMillis();
									Param.simStatus = receivedState;
								}
							}
						}
						break;
						// Emergency action command (applied only once)
					case PCCompanionParam.EMERGENCY_COMMAND:
						int emergency = input.readInt();
						boolean commandFound = false;
						boolean commandSuccess = false;
						String action = null;
						if (emergency == PCCompanionParam.ACTION_RECOVER_CONTROL) {
							commandFound = true;
							if (copter.cancelRCOverride()) {
								action = Text.RECOVER_CONTROL;
								commandSuccess = true;
							}
						}
						if (emergency == PCCompanionParam.ACTION_RTL) {
							commandFound = true;
							if (copter.setFlightMode(FlightMode.RTL)) {
								action = Text.RTL;
								commandSuccess = true;
							}
						}
						if (emergency == PCCompanionParam.ACTION_LAND) {
							commandFound = true;
							if (copter.setFlightMode(FlightMode.LAND)) {
								action = Text.LAND;
								commandSuccess = true;
							}
						}
						if (commandFound) {
							if (commandSuccess) {
								ArduSimTools.logGlobal(Text.EMERGENCY_SUCCESS + " " + action);
								goOn = false;
								FileDescriptor.out.sync();
							} else {
								ArduSimTools.logGlobal(Text.EMERGENCY_FAILED + " " + emergency);
							}
						} else {
							ArduSimTools.logGlobal(Text.EMERGENCY_NOT_FOUND + " " + emergency);
						}
						break;
					}
				} catch (SocketTimeoutException | KryoException ignored) {
				}
			} catch (IOException ignored) {}
			receivedBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
			receivedPacket.setData(receivedBuffer, 0, receivedBuffer.length);
		}
		
		input.close();
		receiveSocket.close();
	}
}
