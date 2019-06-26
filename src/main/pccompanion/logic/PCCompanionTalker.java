package main.pccompanion.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import javax.swing.SwingUtilities;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import main.ArduSimTools;
import main.Param;
import main.Param.SimulatorState;
import main.api.ArduSim;
import main.api.communications.CommLink;
import main.pccompanion.gui.PCCompanionGUI;
import main.uavController.UAVParam;
import main.Text;

/** 
 * Thread used to send commands to real UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class PCCompanionTalker extends Thread {

	@Override
	public void run() {
		PCCompanionListener listener = new PCCompanionListener();
		listener.start();
		
		DatagramSocket sendSocket = null;
		byte[] sendBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		String broadcastAddress;
		if (Param.role == ArduSim.SIMULATOR) {
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
			ArduSimTools.closeAll(Text.BIND_ERROR_2);
		}

		long time = System.currentTimeMillis();
		long sleep;
		boolean send;
		boolean dialogAlreadyOpened = false;
		ArduSim ardusim = API.getArduSim();
		SimulatorState state;
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
						state = Param.simStatus;
						output.clear();
						output.writeInt(PCCompanionParam.CHANGE_STATE_COMMAND);
						output.writeInt(state.getStateId());
						if (state == SimulatorState.SETUP_IN_PROGRESS) {
							output.writeLong(System.currentTimeMillis());
						}
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
							ArduSimTools.selectedProtocolInstance.openPCCompanionDialog(PCCompanionGUI.companion.assistantFrame);
						}
					});
					dialogAlreadyOpened = true;
				}
			}
			
			// Send emergency command when needed
			if ((Param.simStatus == SimulatorState.SETUP_IN_PROGRESS || Param.simStatus == SimulatorState.TEST_IN_PROGRESS)
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
				ardusim.sleep(sleep);
			}
			time = time + PCCompanionParam.COMMAND_SEND_TIMEOUT;
		}
		output.close();// Code execution will never get here
		sendSocket.close();
		
		
	}

}
