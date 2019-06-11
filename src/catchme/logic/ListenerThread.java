package catchme.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import api.API;
import catchme.logic.CatchMeParams;
import main.api.GUI;

public class ListenerThread extends Thread{

	private DatagramSocket socketUDP;
	private DatagramSocket pysocketUDP;
	private InetAddress hostServidor; 
	private int javaPort = 5581;
	private int pythonPort = 5580;
	private byte[] buffer = new byte[1000];
	
	private GUI gui;
	
	private volatile boolean running = false;
	private CatchMeParams.status status = CatchMeParams.status.LOITER;
	
	public ListenerThread() {
		gui = API.getGUI(0);
		try {
			hostServidor = InetAddress.getByName(CatchMeParams.PTYHON_SERVER_IP);
			socketUDP = new DatagramSocket(javaPort);
			pysocketUDP = new DatagramSocket();
			running = true;
		} catch (Exception e) {
			gui.warn("error", "Could not setup SocketUDP");
			e.printStackTrace();
			running = false;
		}
	}
	
	@Override
	public void run() {
		startListening();
	}
	
	public boolean getRunning() {
		return this.running;
	}
	
	public void setRunning(Boolean running) {
		this.running = running;
	}
	
	public boolean exit() {
		System.out.println("start exiting in listeingThread");
		running = false;
		try {
			if(socketUDP != null)
				socketUDP.close();
		} catch (Exception e) {
			gui.warn("error", "Could not close the serverSockets");
			e.printStackTrace();
			return false;
		}
		return true;
	}

    public void sendMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, hostServidor, pythonPort);
        pysocketUDP.send(packet);
}
	
	private void startListening() {
		try {
			System.out.println("Sending message...");
			sendMessage("Start");
			System.out.println("Message sent");
			DatagramPacket respons = new DatagramPacket(buffer, buffer.length);
			String message = "";
			while(running) {
				try {
					socketUDP.receive(respons);
					message = new String(respons.getData());
					System.out.println("Receiving data...");
					System.out.println(message);
				}catch(Exception e) {
					System.err.println("cannot read DatagramPacket");
					e.printStackTrace();
				}
				if(message.equals("error")) {
					status = CatchMeParams.status.LAND;
					running = false;
				}
				else if(message.equals("stopped")) {
					status = CatchMeParams.status.LAND;
					running = false;
				}
				else if(message.equals("undetected") && status != CatchMeParams.status.LOITER) {
					status = CatchMeParams.status.LOITER;
				}
				else if(message.equals("undetected") && status == CatchMeParams.status.LOITER) {
					status = CatchMeParams.status.LAND;
					running = false;
				}
				else if(message.contains("DisAndPos")) {
					String[] parts = message.split(",");
					System.out.println(parts);
					status = CatchMeParams.status.MOVE;
				}
				else {
					status = CatchMeParams.status.LAND;
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
