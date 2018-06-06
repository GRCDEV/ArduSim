package pccompanion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import javax.swing.SwingUtilities;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import uavController.UAVParam;

public class PCCompanionTalker extends Thread {

	@Override
	public void run() {
		PCCompanionListener listener = new PCCompanionListener();
		listener.start();
		
		DatagramSocket sendSocket = null;
		byte[] sendBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		String broadcastAddress;
		if (Param.isPCCompanion) {
			broadcastAddress = UAVParam.BROADCAST_IP;
		} else {
			broadcastAddress = UAVParam.MAV_NETWORK_IP;
		}
		DatagramPacket sentPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
				new InetSocketAddress(broadcastAddress, PCCompanionParam.UAV_PORT));
		Output output = new Output(sendBuffer);

		try {
			sendSocket = new DatagramSocket();
			sendSocket.setBroadcast(true);
		} catch (SocketException e) {
			GUI.exit(Text.BIND_ERROR_2);
		}

		long time = System.currentTimeMillis();
		long sleep;
		boolean send;
		while (true) {
			if (!listener.isAlive()) {
				break;
			}
			
			if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS
					|| Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
				send = true;
			} else {
				send = false;
			}

			if (send) {
				try {
					output.clear();
					output.writeInt(Param.simStatus.getStateId());
					sentPacket.setData(sendBuffer, 0, output.position());
					sendSocket.send(sentPacket);
				} catch (KryoException e) {} catch (IOException e) {}
			}

			sleep = PCCompanionParam.COMMAND_SEND_TIMEOUT - (System.currentTimeMillis() - time);
			if (sleep > 0) {
				Tools.waiting((int)sleep);
			}
			time = time + PCCompanionParam.COMMAND_SEND_TIMEOUT;
		}
		output.close();
		sendSocket.close();
		
		// Launch the protocol dialog for PC Companion
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ProtocolHelper.selectedProtocolInstance.openPCCompanionDialog(PCCompanionGUI.companion.assistantFrame);
			}
		});
	}

}
