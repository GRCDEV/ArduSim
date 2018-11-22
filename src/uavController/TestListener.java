package uavController;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import main.Param;
import main.Param.SimulatorState;
import pccompanion.logic.PCCompanionParam;
import main.Text;

/** Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class TestListener extends Thread {
	
	private static byte[] receivedBuffer;
	private static DatagramSocket receiveSocket = null;
	private static DatagramPacket receivedPacket;
	private static Input input;
	
	public TestListener() throws SocketException {
		receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
		input = new Input(receivedBuffer);
		receiveSocket = new DatagramSocket(PCCompanionParam.uavPort);
		receiveSocket.setBroadcast(true);
	}

	@Override
	public void run() {
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
					receivedPacket.setData(new byte[Tools.DATAGRAM_MAX_LENGTH]);
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
								Param.simStatus = receivedState;
							}

							if (receivedState == SimulatorState.TEST_IN_PROGRESS) {
								if (PCCompanionParam.lastStartCommandTime == null) {
									PCCompanionParam.lastStartCommandTime = new AtomicLong(System.currentTimeMillis());
								} else {
									PCCompanionParam.lastStartCommandTime.set(System.currentTimeMillis());
								}
								if (Param.simStatus == SimulatorState.READY_FOR_TEST) {
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
							if (Copter.returnRCControl(0)) {
								action = Text.RECOVER_CONTROL;
								commandSuccess = true;
							} else {

							}
						}
						if (emergency == PCCompanionParam.ACTION_RTL) {
							commandFound = true;
							if (Copter.setFlightMode(0, FlightMode.RTL_ARMED)) {
								action = Text.RTL;
								commandSuccess = true;
							} else {
								
							}
						}
						if (emergency == PCCompanionParam.ACTION_LAND) {
							commandFound = true;
							if (Copter.setFlightMode(0, FlightMode.LAND_ARMED)) {
								action = Text.LAND;
								commandSuccess = true;
							} else {

							}
						}
						if (commandFound) {
							if (commandSuccess) {
								GUI.log(Text.EMERGENCY_SUCCESS + " " + action);
								goOn = false;
								FileDescriptor.out.sync();
							} else {
								GUI.log(Text.EMERGENCY_FAILED + " " + emergency);
							}
						} else {
							GUI.log(Text.EMERGENCY_NOT_FOUND + " " + emergency);
						}
						break;
					}
				} catch (SocketTimeoutException e) {
				} catch (KryoException e) {}
			} catch (IOException e) {}
			receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			receivedPacket.setData(receivedBuffer, 0, receivedBuffer.length);
		}
		
		input.close();
		receiveSocket.close();
	}
}
