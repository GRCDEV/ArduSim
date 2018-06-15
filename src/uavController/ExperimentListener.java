package uavController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import api.GUI;
import api.Tools;
import main.Param;
import main.Param.SimulatorState;
import pccompanion.PCCompanionParam;
import main.Text;

public class ExperimentListener extends Thread {

	@Override
	public void run() {
		DatagramSocket receiveSocket = null;
		byte[] receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		DatagramPacket receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
		Input input = new Input(receivedBuffer);
		try {
			receiveSocket = new DatagramSocket(PCCompanionParam.UAV_PORT);
			receiveSocket.setBroadcast(true);
		} catch (SocketException e) {
			GUI.exit(Text.BIND_ERROR_1);
		}
		
		while (true) {
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
					switch (input.readInt()) {
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
					// Emergency action command
					case PCCompanionParam.EMERGENCY_COMMAND:
						int command = input.readInt();
						if (command == PCCompanionParam.ACTION_RECOVER_CONTROL) {
//							solo una vez y contemplar errores y cerrar hilo tras el comando
						}
						if (command == PCCompanionParam.ACTION_RTL) {
							
						}
						if (command == PCCompanionParam.ACTION_LAND) {
							
						}
						break;
					}
				} catch (SocketTimeoutException e) {
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
							&& PCCompanionParam.lastStartCommandTime != null
							&& (System.currentTimeMillis() - PCCompanionParam.lastStartCommandTime.get() > PCCompanionParam.MAX_TIME_SINCE_LAST_START_COMMAND)) {
						break;
					}
				} catch (KryoException e) {}
			} catch (IOException e) {}
			receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			receivedPacket.setData(receivedBuffer, 0, receivedBuffer.length);
		}
		input.close();
		receiveSocket.close();
	}
}
