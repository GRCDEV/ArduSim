package com.protocols.vision.logic;

import com.api.API;
import com.api.GUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ListeningServer implements Runnable{

	private ServerSocket serverSocket;
	private Socket markerSocket;
	private BufferedReader reader;
	
	private volatile boolean running = false;
	private visionParam.status status = visionParam.status.LOITER;
	private final uavNavigator navi;
	private final GUI gui;
	
	/**
	 * creates a listener to listen to the python messages at port 5764.
	 * uses an instance of the uavNavigator in order to change the uavMode
	 * @param numUAV
	 */
	public ListeningServer(int numUAV) {
		navi = uavNavigator.getInstance(numUAV);
		this.gui = API.getGUI(0);
		
		try {
			serverSocket = new ServerSocket(5764);
			running = true;
		} catch (Exception e) {
			gui.warn("error", "Could not setup markerSocket");
			e.printStackTrace();
			running = false;
		}	
	}
	
	@Override
	public void run() {
		gui.log("run listeningSever");
		listenToSocket();
	}

	/**
	 * closing all the open ports
	 * @return false is error occurred
	 */
	public boolean exit() {
		running = false;
		try {
			if(reader != null)
				reader.close();
			if(markerSocket != null)
				markerSocket.close();
			if(serverSocket != null)
				serverSocket.close();
		} catch (Exception e) {
			gui.warn("error", "Could not close the serverSockets");
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

	
	/**
	 * listening continuously for messages from python and changing the status of the UAVnavigator
	 */
	public void listenToSocket() {
		try {
			markerSocket = serverSocket.accept();
			reader = new BufferedReader(new InputStreamReader(markerSocket.getInputStream()));
			String respons = "";
			while(running) {
				try {
					respons = reader.readLine();
				}catch(Exception e) {
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
				}else if(respons.equals("recover")) {
					status = visionParam.status.RECOVER;
				}else if(respons.contains("move")){
					//message from Python format: move,x,y,angle
					String[] parts = respons.split(",");
					navi.setTarget(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
					status = visionParam.status.MOVE;
				}else if(respons.contains("rotate")) {
					//message from Python format: move,x,y,angle
					String[] parts = respons.split(",");
					navi.setTarget(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
					status = visionParam.status.ROTATE;
				}
				else if(respons.equals("loiter")) {
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
