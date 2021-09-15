package com.uavController;

import com.api.API;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;
import com.api.ArduSim;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.setup.pccompanion.logic.PCCompanionParam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/** 
 * Thread used to send status information to the PC Companion to enable it to start the experiment.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TestTalker extends Thread {

	@Override
	public void run() {
		DatagramSocket sendSocket = null;
		byte[] sendBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		String broadcastAddress;
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			broadcastAddress = UAVParam.MAV_NETWORK_IP;
		} else {
			broadcastAddress = UAVParam.broadcastIP;
		}
		DatagramPacket sentPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
		        new InetSocketAddress(broadcastAddress, PCCompanionParam.computerPort));
		Output output = new Output(sendBuffer);
		
		try {
			sendSocket = new DatagramSocket();
			sendSocket.setBroadcast(true);
		} catch (SocketException e) {
			ArduSimTools.closeAll(Text.BIND_ERROR_2);
		}
		
		ArduSim ardusim = API.getArduSim();
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
				// Inform that the experiment has started for some time before stopping the thread
				// stop the tread after this because you cannot be sure that the UAV has finished the protocol or the communication is lost
				finish = PCCompanionParam.lastStartCommandTime != null
						&& (System.currentTimeMillis() - PCCompanionParam.lastStartCommandTime.get() > PCCompanionParam.MAX_TIME_SINCE_LAST_START_COMMAND);
			} else {
				finish = true;
			}
			if (finish) {
				break;
			}
			
			try {
				output.reset();
				output.writeLong(Param.id[0]);
				output.writeInt(Param.simStatus.getStateId());
				sentPacket.setData(sendBuffer, 0, output.position());
				sendSocket.send(sentPacket);
			} catch (KryoException | IOException ignored) {}

			sleep = PCCompanionParam.STATUS_SEND_TIMEOUT - (System.currentTimeMillis() - time);
			if (sleep > 0) {
				ardusim.sleep(sleep);
			}
			time = time + PCCompanionParam.STATUS_SEND_TIMEOUT;
		}
		output.close();
		sendSocket.close();
	}
}
