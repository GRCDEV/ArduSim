package catchme.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Location3DUTM;
import catchme.logic.CatchMeParams;
import main.api.Copter;
import main.api.GUI;
import muscop.pojo.Message;

public class ListenerThread extends Thread{

	private DatagramSocket socketUDP;
	private DatagramSocket pysocketUDP;
	private InetAddress hostServidor; 
	private int javaPort = 5581;
	private int pythonPort = 5580;
	private byte[] buffer = new byte[1000];
	private Input input;
	private Output output;
	private byte[] message;
	
	private GUI gui;
	private Copter copter;
	
	private volatile boolean running = false;
	private CatchMeParams.status status = CatchMeParams.status.LOITER;
	
	public ListenerThread() {
		gui = API.getGUI(0);
		copter = API.getCopter(0);
		try {
			hostServidor = InetAddress.getByName(CatchMeParams.PTYHON_SERVER_IP);
			socketUDP = new DatagramSocket(javaPort);
			pysocketUDP = new DatagramSocket();
			running = true;
			this.input = new Input(buffer);
			this.output = new Output(buffer);
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

    public void sendMessage(byte[] message) throws IOException {   
        DatagramPacket packet = new DatagramPacket(message, message.length, hostServidor, pythonPort);
        pysocketUDP.send(packet);
}
	
	private void startListening() {
		try {
			System.out.println("Sending message...");
			output.clear();
			output.writeShort(1);
			message = Arrays.copyOf(buffer, output.position());
			DatagramPacket respons = new DatagramPacket(buffer, buffer.length);
			sendMessage(message);
			Location2DUTM offset = null;
			Location2DUTM targetUTM;
			
			while(running) {
				try {
					socketUDP.receive(respons);
					input.setBuffer(respons.getData());
					//message = new String(respons.getData(), respons.getOffset(), respons.getLength());
				}catch(Exception e) {
					System.err.println("cannot read DatagramPacket");
					e.printStackTrace();
				}
				short type = input.readShort();
				switch(type) {
				case 1:
					status = CatchMeParams.status.LAND;
					running = false;
					break;
				case 2:
					status = CatchMeParams.status.LAND;
					running = false;
					break;
				case 3:
					status = CatchMeParams.status.LOITER;
					break;
				case 4:
					double x = input.readDouble();
					double y = input.readDouble();
					System.out.println("(" + x + ", " +y + ")");
					double h = 0;//copter.getHeading();
					double xp = x * Math.cos(h) + y * Math.sin(h);
					double yp =  -x * Math.sin(h) + y * Math.cos(h);
					if (offset == null) {
						offset = new Location2DUTM(xp, yp);
					}
					double moveX = xp - offset.x;
					double moveY = yp - offset.y;
					targetUTM = copter.getLocationUTM();
					targetUTM.x = targetUTM.x + moveX;
					targetUTM.y = targetUTM.y + moveY;
					
					copter.moveTo(new Location3DUTM(targetUTM, copter.getAltitudeRelative()).getGeo3D());
					status = CatchMeParams.status.MOVE;
					break;
				case 5:
					System.out.println(message);
					status = CatchMeParams.status.LAND;
					running = false;
					break;
				default:
					status = CatchMeParams.status.LAND;
					running = false;
					break;
				}
				/*
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
					System.out.println(message);
					status = CatchMeParams.status.MOVE;
				}
				else if(message.equals("Finish")){
					System.out.println(message);
					status = CatchMeParams.status.LAND;
					running = false;
				}
				*/
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
