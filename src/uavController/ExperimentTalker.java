package uavController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

import api.GUI;
import api.Tools;
import main.Param;
import main.Text;
import main.Param.SimulatorState;
import pccompanion.PCCompanionParam;

public class ExperimentTalker extends Thread {

	@Override
	public void run() {
		DatagramSocket sendSocket = null;
		byte[] sendBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		String broadcastAddress;
		if (Param.isRealUAV) {
			broadcastAddress = UAVParam.BROADCAST_IP;
		} else {
			broadcastAddress = UAVParam.MAV_NETWORK_IP;
		}
		DatagramPacket sentPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
		        new InetSocketAddress(broadcastAddress, PCCompanionParam.COMPUTER_PORT));
		Output output = new Output(sendBuffer);
		
		try {
			sendSocket = new DatagramSocket();
			sendSocket.setBroadcast(true);
		} catch (SocketException e) {
			GUI.exit(Text.BIND_ERROR_2);
		}
		
		long time = System.currentTimeMillis();
		long sleep;
		boolean finish;
		while (true) {
			if (Param.simStatus == SimulatorState.CONFIGURING
				|| Param.simStatus == SimulatorState.CONFIGURING_PROTOCOL
				|| Param.simStatus == SimulatorState.STARTING_UAVS
				|| Param.simStatus == SimulatorState.UAVS_CONFIGURED
				|| Param.simStatus == SimulatorState.SETUP_IN_PROGRESS
				|| Param.simStatus == SimulatorState.READY_FOR_TEST) {
				finish = false;
			} else if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
				if (PCCompanionParam.lastStartCommandTime != null
						&& (System.currentTimeMillis() - PCCompanionParam.lastStartCommandTime.get() > PCCompanionParam.MAX_TIME_SINCE_LAST_START_COMMAND)) {
					finish = true;
				} else {
					finish = false;
				}
			} else {
				finish = true;
			}
			if (finish) {
				break;
			}
			
			try {
				output.clear();
				output.writeLong(Param.id[0]);
				output.writeInt(Param.simStatus.getStateId());
				sentPacket.setData(sendBuffer, 0, output.position());
				sendSocket.send(sentPacket);
			} catch (KryoException e) {} catch (IOException e) {}
			
			sleep = PCCompanionParam.STATUS_SEND_TIMEOUT - (System.currentTimeMillis() - time);
			if (sleep > 0) {
				Tools.waiting((int)sleep);
			}
			time = time + PCCompanionParam.STATUS_SEND_TIMEOUT;
		}
		output.close();
		sendSocket.close();
	}
}
