package pccompanion.logic;

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
import pccompanion.gui.PCCompanionGUI;
import main.Text;
import uavController.UAVParam;

/** Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class PCCompanionTalker extends Thread {

	@Override
	public void run() {
		PCCompanionListener listener = new PCCompanionListener();
		listener.start();
		
		DatagramSocket sendSocket = null;
		byte[] sendBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		String broadcastAddress;
		if (Param.role == Tools.SIMULATOR) {
			broadcastAddress = UAVParam.MAV_NETWORK_IP;
		} else {
			broadcastAddress = UAVParam.broadcastIP;
		}
		DatagramPacket sentPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
				new InetSocketAddress(broadcastAddress, PCCompanionParam.uavPort));
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
		boolean dialogAlreadyOpened = false;
		while (Param.simStatus != SimulatorState.TEST_FINISHED
				&& Param.simStatus != SimulatorState.SHUTTING_DOWN) {
			if (listener.isAlive()) {
				// Send simulation states
				if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS
						|| Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
					send = true;
				} else {
					send = false;
				}
				if (send) {
					try {
						output.clear();
						output.writeInt(PCCompanionParam.CHANGE_STATE_COMMAND);
						output.writeInt(Param.simStatus.getStateId());
						sentPacket.setData(sendBuffer, 0, output.position());
						sendSocket.send(sentPacket);
					} catch (KryoException e) {} catch (IOException e) {}
				}
			} else {
				if (!dialogAlreadyOpened) {
					// Launch the protocol dialog for PC Companion
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							ProtocolHelper.selectedProtocolInstance.openPCCompanionDialog(PCCompanionGUI.companion.assistantFrame);
						}
					});
					dialogAlreadyOpened = true;
				}
			}
			
			// Send emergency command when needed
			if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS
					&& PCCompanionParam.action.get() != PCCompanionParam.ACTION_NONE) {
				try {
					output.clear();
					output.writeInt(PCCompanionParam.EMERGENCY_COMMAND);
					output.writeInt(PCCompanionParam.action.get());
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
		output.close();// Code execution will never get here
		sendSocket.close();
		
		
	}

}
