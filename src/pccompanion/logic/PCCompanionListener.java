package pccompanion.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import api.GUI;
import api.Tools;
import api.pojo.StatusPacket;
import main.Param;
import main.Param.SimulatorState;
import pccompanion.gui.PCCompanionGUI;
import main.Text;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class PCCompanionListener extends Thread {

	@Override
	public void run() {
		DatagramSocket receiveSocket = null;
		byte[] receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		DatagramPacket receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
		Input input = new Input(receivedBuffer);
		try {
			receiveSocket = new DatagramSocket(PCCompanionParam.computerPort);
			receiveSocket.setBroadcast(true);
		} catch (SocketException e) {
			GUI.exit(Text.BIND_ERROR_3);
		}
		
		Map<Long, StatusPacket> receiving = new HashMap<>();
		StatusPacket received;
		long id;
		String ip;
		SimulatorState status;
		long time = System.currentTimeMillis();
		Iterator<Entry<Long, StatusPacket>> it;
		Map.Entry<Long, StatusPacket> entry;
		while (true) {
			try {
				receiveSocket.receive(receivedPacket);
				input.setBuffer(receivedPacket.getData());
				
				// 1. Update received information and GUI
				id = input.readLong();
				status = SimulatorState.getStateById(input.readInt());
				
				if (receiving.containsKey(id)) {
					received = receiving.get(id);
					if (status != received.status) {
						received.status = status;
						PCCompanionGUI.companion.setState(received.row, status.name());
					}
				} else {
					synchronized(PCCompanionGUI.semaphore) {
						if (!PCCompanionGUI.setupPressed) {
							received = new StatusPacket();
							received.id = id;
							received.status = status;
							receiving.put(id, received);
							ip = receivedPacket.getAddress().getHostAddress();
							int row = PCCompanionGUI.companion.insertRow(id, ip, status.name());
							received.row = row;
						}
					}
				}
				
				// Peridically check if all UAVs are waiting for the user interaction
				if (System.currentTimeMillis() - time > PCCompanionParam.STATUS_CHANGE_CHECK_TIMEOUT) {
					// 2. Are all UAVs connected?
					if (Param.simStatus == SimulatorState.STARTING_UAVS && receiving.size() > 0) {
						it = receiving.entrySet().iterator();
						boolean allConnected = true;
						while (it.hasNext() && allConnected) {
							entry = it.next();
							if (entry.getValue().status != SimulatorState.UAVS_CONFIGURED) {
								allConnected = false;
							}
						}
						
						boolean readyForSetup = PCCompanionGUI.companion.setupButton.isEnabled();
						if (allConnected && !readyForSetup) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									PCCompanionGUI.companion.setupButton.setEnabled(true);
								}
							});
						}
						if (!allConnected && readyForSetup) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									PCCompanionGUI.companion.setupButton.setEnabled(false);
								}
							});
						}
					}
					// 3. Have all UAVs finished the setup step?
					if (Param.simStatus == SimulatorState.SETUP_IN_PROGRESS) {
						Collection<StatusPacket> col = receiving.values();
						PCCompanionParam.connectedUAVs.compareAndSet(null, col.toArray(new StatusPacket[col.size()]));
						it = receiving.entrySet().iterator();
						boolean allReady = true;
						while (it.hasNext() && allReady) {
							entry = it.next();
							if (entry.getValue().status != SimulatorState.READY_FOR_TEST
									&& entry.getValue().status != SimulatorState.TEST_IN_PROGRESS) {
								allReady = false;
							}
						}
						if (allReady) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									PCCompanionGUI.companion.startButton.setEnabled(true);
								}
							});
							Param.simStatus = SimulatorState.READY_FOR_TEST;
						}
					}
					// 4. Have all UAVs started the experiment?
					if (Param.simStatus == SimulatorState.TEST_IN_PROGRESS) {
						it = receiving.entrySet().iterator();
						boolean allRunning = true;
						while (it.hasNext() && allRunning) {
							entry = it.next();
							if (entry.getValue().status != SimulatorState.TEST_IN_PROGRESS) {
								allRunning = false;
							}
						}
						if (allRunning) {
							break;
						}
					}
					time = time + PCCompanionParam.STATUS_CHANGE_CHECK_TIMEOUT;
				}
			} catch (KryoException e) {} catch (IOException e) {}
			receivedBuffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			receivedPacket.setData(receivedBuffer, 0, receivedBuffer.length);
		}
		input.close();
		receiveSocket.close();
	}

}
