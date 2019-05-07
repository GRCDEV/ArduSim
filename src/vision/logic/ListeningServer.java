package vision.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.Location2D;

public class ListeningServer implements Runnable{

	private ServerSocket serverSocket;
	private Socket markerSocket;
	private BufferedReader reader;
	
	private volatile boolean running = false;
	private ReentrantLock lock = new ReentrantLock();
	private visionParam.status status = visionParam.status.LOITER;
	
	private uavNavigator navi;
	private int numUAV = 0;
	
	public ListeningServer(int numUAV) {
		this.numUAV = numUAV;
		navi = uavNavigator.getInstance(numUAV);
		
		try {
			//TODO port number serverSocket
			serverSocket = new ServerSocket(5764);
			running = true;
		} catch (Exception e) {
			GUI.warn("error", "Could not setup markerSocket");
			e.printStackTrace();
			running = false;
		}	
	}
	
	@Override
	public void run() {
		GUI.log("run listeningSever");
		listenToSocket();
	}

	public boolean exit() {
		System.out.println("start exiting in listeingServer");
		running = false;
		try {
			if(reader != null)
				reader.close();
			if(markerSocket != null)
				markerSocket.close();
			if(serverSocket != null)
				serverSocket.close();
		} catch (Exception e) {
			GUI.warn("error", "Could not close the serverSockets");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean getRunning() {
		return this.running;
	}
	
	public void setRunning(Boolean running) {
		this.running = running;
	}

	
	public void listenToSocket() {
		try {
			markerSocket = serverSocket.accept();
			reader = new BufferedReader(new InputStreamReader(markerSocket.getInputStream()));
			String respons = "";
			while(running) {
				try {
					respons = reader.readLine();
				}catch(Exception e) {
					//TODO fix this error
					System.err.println("cannot read reader");
					//e.printStackTrace();
				}
				if(respons.equals("error")) {
					status = visionParam.status.LAND;
					running = false;
				}else if(respons.equals("land")) {
					status = visionParam.status.LAND;
				}else if(respons.equals("descend")) {
					status = visionParam.status.DESCEND;
				}else if(respons.contains("move")){
					//message from Python format: move,x,y
					String[] parts = respons.split(",");
					navi.setTarget(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
					status = visionParam.status.MOVE;
				}else if(respons.equals("loiter")) {
					status = visionParam.status.LOITER;
				}else {
					status = visionParam.status.LAND;
				}
				navi.setDroneStatus(status);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
