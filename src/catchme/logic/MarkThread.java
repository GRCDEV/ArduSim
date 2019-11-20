package catchme.logic;

import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.API;
import es.upv.grc.mapper.DrawableSymbol;
import es.upv.grc.mapper.DrawableSymbolGeo;
import es.upv.grc.mapper.GUIMapPanelNotReadyException;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import es.upv.grc.mapper.Mapper;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;

public class MarkThread extends Thread {
	
	private DrawableSymbolGeo target;
	
	private Mark mark;
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
	private boolean isRealUAV;
	
	private volatile boolean running = false;
	
	@SuppressWarnings("unused")
	private MarkThread() {}
	
	public MarkThread(Mark mark) {
		this.mark = mark;
		
		this.isRealUAV = API.getArduSim().getArduSimRole() == ArduSim.MULTICOPTER;
		
		if (!this.isRealUAV) {
			try {
				this.target = Mapper.Drawables.addSymbolGeo(1, CatchMeParams.startingLocation.get().getGeo(),
						DrawableSymbol.CROSS, 10, Color.BLACK, CatchMeParams.STROKE_POINT);
			} catch (GUIMapPanelNotReadyException | LocationNotReadyException e1) {
				e1.printStackTrace();
			}
		}
		
		gui = API.getGUI(0);
		try {
			hostServidor = InetAddress.getByName(CatchMeParams.PTYHON_SERVER_IP);
			socketUDP = new DatagramSocket();
			pysocketUDP = new DatagramSocket(pythonPort);
			running = true;
			input = new Input(buffer);
			output = new Output(buffer);
		} catch (Exception e) {
			gui.warn("error", "Could not setup SocketUDP");
			e.printStackTrace();
			running = false;
		}
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
	
	public void sendMessage(byte [] message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, hostServidor, javaPort);
        socketUDP.send(packet);
    }
	
	@Override
	public void run() {
		ArduSim ardusim = API.getArduSim();
		Copter copter = API.getCopter(0);
		double diferenceX;
		double diferenceY;
//		double distance;
		DatagramPacket respons = new DatagramPacket(buffer, buffer.length);
		System.out.println("Waiting message");
		
		
		
		
		
		while(running) {
			try {
				pysocketUDP.setSoTimeout(500);
				pysocketUDP.receive(respons);
				input.setBuffer(respons.getData());
				System.out.println(message);
			} catch (SocketTimeoutException e) {
				mark.move();
				if (!this.isRealUAV) {
					try {
						this.target.updateLocation(CatchMeParams.startingLocation.get().getGeo());
					} catch (LocationNotReadyException e1) {
						e1.printStackTrace();
					}
				}
			} catch(Exception e) {
				System.err.println("cannot read DatagramPacket");
				e.printStackTrace();
			}
			short type = input.readShort();
			if(type == 1) {
				Location2DUTM currentUAV, currentMark;
				while(true) {
					mark.move();
					if (!this.isRealUAV) {
						try {
							this.target.updateLocation(CatchMeParams.startingLocation.get().getGeo());
						} catch (LocationNotReadyException e1) {
							e1.printStackTrace();
						}
					}
					
					currentUAV = copter.getLocationUTM();
					currentMark = CatchMeParams.startingLocation.get();
					diferenceY = currentMark.y - currentUAV.y;
					diferenceX = currentMark.x - currentUAV.x;
					
					
					
//					distance = Math.sqrt(Math.pow(diferenceY, 2) + Math.pow(copter.getAltitude(), 2));
					try {
						output.clear();
						output.writeShort(4);
						output.writeDouble(diferenceX);
						output.writeDouble(diferenceY);
//						output.writeDouble(distance);
						output.flush();
						message = Arrays.copyOf(buffer, output.position());
						sendMessage(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ardusim.sleep(500);
				}
			}
		}
	}
}
