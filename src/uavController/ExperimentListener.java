package uavController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import api.GUIHelper;
import main.Param;
import main.Param.SimulatorState;
import pccompanion.PCCompanionParam;
import main.Text;

public class ExperimentListener extends Thread {

	@Override
	public void run() {
		DatagramSocket receiveSocket = null;
		byte[] receivedBuffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH];
		DatagramPacket receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
		Input input = new Input(receivedBuffer);
		try {
			receiveSocket = new DatagramSocket(PCCompanionParam.UAV_PORT);
			receiveSocket.setBroadcast(true);
		} catch (SocketException e) {
			GUIHelper.exit(Text.BIND_ERROR_1);
		}
		
		while (true) {
			try {
				if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
						|| Param.simStatus == SimulatorState.TEST_FINISHED
						|| Param.simStatus == SimulatorState.SHUTTING_DOWN) {
					receiveSocket.setSoTimeout(PCCompanionParam.RECEIVE_TIMEOUT);
				}
				try {
					receiveSocket.receive(receivedPacket);
					input.setBuffer(receivedPacket.getData());
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
				} catch (SocketTimeoutException e) {
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
							&& PCCompanionParam.lastStartCommandTime != null
							&& (System.currentTimeMillis() - PCCompanionParam.lastStartCommandTime.get() > PCCompanionParam.MAX_TIME_SINCE_LAST_START_COMMAND)) {
						break;
					}
				} catch (KryoException e) {}
			} catch (IOException e) {}
			receivedBuffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH];
			receivedPacket.setData(receivedBuffer, 0, receivedBuffer.length);
		}
		input.close();
		receiveSocket.close();
	}
}
