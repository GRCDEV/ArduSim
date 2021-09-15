package com.protocols.vision.logic;


import com.api.API;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.api.GUI;
import com.api.pojo.FlightMode;
import com.protocols.vision.logic.visionParam.status;
import es.upv.grc.mapper.Location2DGeo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class uavNavigator extends Thread {
	private static uavNavigator instance; 
	
	private final int numUAV;
//	private long id;
	private final ArduSim ardusim;
	private ClientSocket commandSocket;
	private final Copter copter;
	private final GUI gui;

	private visionParam.status droneStatus = visionParam.status.LOITER;
	
	ListeningServer listeningServer;
	private Thread listeningThread;
	
	private final ReentrantLock lock = new ReentrantLock();
	private volatile Boolean running;
	
	private long timeOutTime = java.lang.System.currentTimeMillis();
	
	private float difx = 0.0f;
	private float dify = 0.0f;
	private float difAlfa = 0.0f;
	
	private static final File logFile = new File("experimentLog.csv");
	private static FileWriter logWriter = null;
	
	private double recoverTimer = -1;
	
	/**
	 * returns instance of this class
	 * @param numUAV
	 * @return instance of UAVnavigator
	 */
	public static uavNavigator getInstance(int numUAV) {
		if(instance != null) {
			return instance;
		}else {
			instance = new uavNavigator(numUAV);
			return instance;
		}
	}
	
	private  uavNavigator(int numUAV) {
		this.numUAV =numUAV;
		this.ardusim = API.getArduSim();
		this.copter = API.getCopter(numUAV);
		this.gui = API.getGUI(numUAV);
//		this.id = copter.getID();
		//TCP ports already in use: 5760 + n10
		gui.log("setting up serversocket");
		boolean succesClient;
		try {
			this.commandSocket = new ClientSocket(visionParam.PTYHON_SERVER_IP,5761);	
			succesClient = this.commandSocket.isConnected();
		}catch(Exception e) {
			gui.warn("error", "Could not setup commandSocket");
			e.printStackTrace();
			succesClient = false;
		}
		this.running = succesClient;
		
		try {
			logWriter = new FileWriter(logFile, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Boolean isRunning() {
		return this.running;
	}
	
	/**
	 * set the internal status of the drone. this is not the same the different flighmodes
	 * @param status
	 */
	public void setDroneStatus( status status) {
		lock.lock();
		if(status != this.droneStatus) {
			gui.log("status changed: " + status);
			this.droneStatus = status;
			if(this.droneStatus == visionParam.status.LOITER) {
				timeOutTime = java.lang.System.currentTimeMillis();
			}
		}
		lock.unlock();
	}
	
	/**
	 * sets the location of the target
	 * @param x distance in m along the x axis of the marker
	 * @param y distance in m along the y axis of the marker
	 * @param angle yaw angle of marker in degrees
	 */
	public void setTarget(float x, float y, float angle) {
		lock.lock();
		this.difx = x;
		this.dify = y;
		this.difAlfa = angle;
		lock.unlock();
	}
	
	private static void log(String text) {
		try {
			logWriter.write(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getCurrentTimeStamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	    Date now = new Date();
	    String strDate = sdf.format(now);
	    return strDate;
	}

	/**
	 * methods which starts visionGuidance and does some extra logging
	 */
	public void run() {
		
		listeningServer = new ListeningServer(numUAV);
		listeningThread = new Thread(listeningServer);
		running = listeningServer.getRunning() && running;
		
		if(this.running) {
			copter.setFlightMode(FlightMode.LOITER);
			log("main/java/com.api.protocols/vision" + ";");
			log(getCurrentTimeStamp() + ";");
			visionGuidance();
			log(getCurrentTimeStamp() + ";");
			Location2DGeo loc = copter.getLocationGeo();
			log(Double.toString(loc.latitude).replace(".", ",") + ";" + 
					Double.toString(loc.longitude).replace(".", ",") + "\n");
			try {
				logWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			gui.warn("ERROR", "no connection with python");
		}
		listeningServer.setRunning(false);
		listeningServer.exit();
	}
	
	/**
	 * In this method the camera on the drone is activated.
	 * The xy position of the aruco marker is determend in python.
	 * The drone centers itself above the arucomarker .
	 * The drone lowers it altitude until it`s on the bottom.
	 */
	public void visionGuidance() {
		gui.log("Send message to python: start_camera");
		running = commandSocket.sendMessage("start_camera").equalsIgnoreCase("ACK");
		if(running) {
			timeOutTime = java.lang.System.currentTimeMillis();
			gui.log("send message to python: find_target");
			running = commandSocket.sendMessage("find_target").equalsIgnoreCase("ACK");
			if(running) {
				//listen to incomming messages from markersocket thread	
				this.listeningThread.start();
				//move uav with signal overwrite
				do {						
					running = moveUAV();
					ardusim.sleep(100);
					if(this.droneStatus == visionParam.status.LOITER &&(java.lang.System.currentTimeMillis()-timeOutTime > 30*1000)) {
						running = false;
					}
				}while(running && copter.getAltitudeRelative()>0.5);
			}
		}
		//loiter
		copter.channelsOverride(0.0, 0.0, 0.0, 0.0);
		copter.land();
		// wait until the uav is on the ground to stop the camera
		while (copter.getFlightMode() != FlightMode.LAND) {
			ardusim.sleep(100);
		}
        commandSocket.sendMessage("stop_camera");
		commandSocket.sendMessage("exit");
	}
	
	
	/**
	 * moves the uav with the use of channeloverride
	 * 5 cases are possible: move (uav moves in xy plane), rotate (uav rotates allong his yaw axis),
	 * descend (uav descends),loiter(uav stays still in the air),
	 * land(entire process stops no more messages gets accepted)
	 * @return true is the uav is still moving, false if land. returning false
	 * will result in stopping the protocol and landing
	 */
	public boolean moveUAV() {
		//using lock.lock gives more freedom then using a synchronized method
		boolean running = false;
		lock.lock();
		try {
			switch(droneStatus){
				case MOVE:
					//move uav to target position
					float picthVelocity = 0;
					float rollVelocity = 0;
					float speed = 0.5f;
					//if x is much bigger then y move the UAV in the x axis
					if(Math.abs(this.difx) > Math.abs(this.dify*0.5)) {
						if(Math.abs(this.difx) > 1) {
							speed = 0.15f;
						}
						//0 if x is 0 else +10% or -10%
						rollVelocity = (difx == 0) ? 0.0f: (difx > 0 ? speed:-speed);
					}else {
						if(Math.abs(this.dify) > 1) {
							speed = 0.15f;
						}
						//else move over the y axis
						picthVelocity = (dify == 0) ? 0.0f: (dify> 0 ? -speed:speed);
					}
					copter.channelsOverride(rollVelocity, picthVelocity,0, 0);
					running = true;
					break;
				case ROTATE:
					float rotationSpeed = 0.05f;
					float velocity = (this.difAlfa > 0) ? rotationSpeed :-rotationSpeed;
					//rotate drone to right angle
					copter.channelsOverride(0, 0, 0, velocity);
					running = true;
					break;
				case DESCEND:
					float descendSpeed = -0.1f;
					double z =copter.getAltitudeRelative(); 
					if( z> 10) {
						if(z > 30) {
							descendSpeed = -0.3f;
						}else {
							descendSpeed = (float) (-z/100);
						}
					}
					copter.channelsOverride(0, 0,descendSpeed, 0);
					running = true;
					break;
				case LOITER:
					//do nothing, drone stays where it is
					copter.channelsOverride(0.0, 0.0, 0.0, 0.0);
					running = true;
					break;
				case LAND:
					running = false;
					break;
				case RECOVER:
					if(this.recoverTimer == -1) {
						recoverTimer = System.currentTimeMillis();
					}
					//first 2 seconds of loiter
					if(System.currentTimeMillis() <= recoverTimer +2000) {
						copter.channelsOverride(0.0, 0.0, 0.0, 0.0);
					}else {
						//later (4 s changed in python) going up
						//check if the UAV is not already flying to high
						if(copter.getAltitudeRelative() < 50) {
							System.out.println("up");
							copter.channelsOverride(0, 0, 0.1, 0);
						}else {
							copter.channelsOverride(0, 0, 0, 0);
						}
					}
					running = true;
					break;
				default:
					gui.warn("unknown dronestatus", "drone status is unknown, switched to default and land UAV");
					running = false;
					break;
			}
			if(recoverTimer != -1 && droneStatus != status.RECOVER) {
				recoverTimer = -1;
			}
		}finally {
			lock.unlock();
		}
		return running;
	}
}
