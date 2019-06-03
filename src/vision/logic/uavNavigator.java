package vision.logic;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import api.API;
import api.pojo.FlightMode;
import api.pojo.location.Location2DGeo;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import vision.logic.visionParam.status;

public class uavNavigator extends Thread {
	private static uavNavigator instance; 
	
	private int numUAV;
	private long id;
	private ArduSim ardusim;
	private ClientSocket commandSocket;
	private Copter copter;
	private GUI gui;

	private visionParam.status droneStatus = visionParam.status.LOITER;
	
	ListeningServer listeningServer;
	private Thread listeningThread;
	
	private ReentrantLock lock = new ReentrantLock();
	private volatile Boolean running;
	
	private long timeOutTime = java.lang.System.currentTimeMillis();
	
	private float difx = 0.0f;
	private float dify = 0.0f;
	private float difAlfa = 0.0f;
	
	private static File logFile = new File("experimentLog.csv");
	private static FileWriter logWriter = null;
	
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
		this.id = copter.getID();
		//TCP ports already in use: 5760 + n10
		//TODO check portnumbers
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
	
	public void setTarget(float x, float y, float angle) {
		lock.lock();
		
		if(false) {
			//turn the axis such that the coordinates are given in the axis of the drone not the marker
			double heading = copter.getHeading();
			this.difx = (float) (x*Math.cos(heading) + y*Math.sin(heading));
			this.dify = (float) -(x*Math.sin(heading) - y*Math.cos(heading));
		}else {
			this.difx = x;
			this.dify = y;
		}
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

	public void run() {
		
		listeningServer = new ListeningServer(numUAV);
		listeningThread = new Thread(listeningServer);
		running = listeningServer.getRunning() && running;
		
		/*
		GUI.log("drone "+id+" follows path");
		followPath();
		GUI.log("drone "+id+" arrived at end point. Starting camera now");
		*/
		
		if(this.running) {
			log("vision" + ";");
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
		
		System.out.println("listeningServer setting running to false");
		listeningServer.setRunning(false);
		System.out.println("exit listeningServer");
		listeningServer.exit();
		System.out.println("shutting down program");
	}
	
	/**
	 * This method takes all the waypoints convert them to GeoCoordinates and lets the drone fly until the last one
	 * notice that the drone does not land on the last waypoint. Landing is proceed in visionGuidance()
	 */
	/*
	public void followPath() {
		List<Waypoint>[] waypoints;
		waypoints = UAVParam.missionGeoLoaded;
		for(int i = 1;i<waypoints[numUAV].size();i++) {
			Waypoint waypoint = waypoints[numUAV].get(i);
			Copter.moveUAV(numUAV, new GeoCoordinates(waypoint.getLatitude(),
					waypoint.getLongitude()), (float) visionParam.ALTITUDE, visionParam.LAST_WP_THRESHOLD, visionParam.LAST_WP_THRESHOLD);
		}
	}
	*/
	
	/**
	 * In this method the camera on the drone is activated.
	 * The xy position of the aruco marker is determend in python.
	 * The drone centers itself above the arucomarker .
	 * The drone lowers it altitude until it`s on the bottom.
	 */
	public void visionGuidance() {
    ////////////////////////EXPERIMENT : TAKE OF AND LAND WITH CAMERA ///////////////
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
				}while(running && copter.getAltitudeRelative()>0.3);
			}
		}
		
		copter.land();
		while (copter.getFlightMode() != FlightMode.LAND) {
			ardusim.sleep(100);
		};
		commandSocket.sendMessage("stop_camera");
		commandSocket.sendMessage("exit");
	}
	
	private float controlSpeed() {
		return (copter.getAltitudeRelative() > 7.0) ? 0.1f :0.05f; 
	}
	
	public boolean moveUAV() {
		//using lock.lock gives more freedom then using a synchronized method
		boolean running = false;
		lock.lock();
		try {
			switch(droneStatus){
				case MOVE:
					//move uav to target position
					int pitchValue = visionParam.pitchTrim;
					int rollValue = visionParam.rollTrim;
					float speed = controlSpeed();
					//if x is much bigger then y move the UAV in the x axis
					if(Math.abs(this.difx) > Math.abs(this.dify*0.5)) {
						//0 if x is 0 else +10% or -10%
						float velocity = (difx == 0) ? 0.0f: (difx > 0 ? speed:-speed);
						gui.log("roll value: " + velocity);
						rollValue = mapValues("roll",velocity);
					}else {
						//else move over the y axis
						float velocity = (dify == 0) ? 0.0f: (dify> 0 ? -speed:speed);
						gui.log("pitch value: " + velocity);
						pitchValue = mapValues("pitch",velocity);
					}
					//System.out.println("roll: " + rollValue + "\t pitch: " + pitchValue);
					copter.channelsOverride(rollValue, pitchValue,
							visionParam.throttleTrim, visionParam.yawTrim);
					running = true;
					break;
				case ROTATE:
					float rotationSpeed = 0.05f;
					float velocity = (this.difAlfa > 0) ? rotationSpeed :-rotationSpeed;
					//rotate drone to right angle
					copter.channelsOverride(visionParam.rollTrim, visionParam.pitchTrim,
							visionParam.throttleTrim, mapValues("yaw",velocity));
					running = true;
					break;
				case DESCEND:
					copter.channelsOverride(visionParam.rollTrim, visionParam.pitchTrim,
							mapValues("throttle",-0.1f), visionParam.yawTrim);
					running = true;
					break;
				case LOITER:
					//do nothing, drone stays where it is
					copter.channelsOverride(visionParam.rollTrim, visionParam.pitchTrim,
							visionParam.throttleTrim, visionParam.yawTrim);
					running = true;
					break;
				case LAND:
					running = false;
					break;
				default:
					gui.warn("unknown dronestatus", "drone status is unknown, switched to default and land UAV");
					running = false;
					break;
			}
		}finally {
			lock.unlock();
		}
		return running;
	}
	
	private int mapValues(String param,float value) {
		//TODO make use of rcx.reversed value
		if(param.equals("throttle")){
			return mapValues(visionParam.throttleMin, visionParam.throttleDeadzone,
					visionParam.throttleTrim,visionParam.throttleMax,value);
		}else if(param.equals("pitch")) {
			//use -value because pitch is defined for an plane and therefore a value higher then
			//the trim value will make the front of the UAV rise and make it go backwards
			return mapValues(visionParam.pitchMin, visionParam.pitchDeadzone,
					visionParam.pitchTrim, visionParam.pitchMax,-value);
		}else if(param.equals("yaw")) {
			return mapValues(visionParam.yawMin, visionParam.yawDeadzone,
					visionParam.yawTrim, visionParam.yawMax,value);
		}else if(param.equals("roll")) {
			return mapValues(visionParam.rollMin, visionParam.rollDeadzone,
					visionParam.rollTrim, visionParam.rollMax,value);
		}else {
			return mapValues(visionParam.pitchMin, visionParam.pitchDeadzone,
					visionParam.pitchTrim, visionParam.pitchMax,0);
		}
	}
	
	private int mapValues(int minValue,int deadzone, int trim, int maxValue,float value) {
		float max, min;
		if(value >0 && value <= 1) {
			//map value between 0 and 1  to value between trim+deadzone and maxValue
			max = maxValue;
			min = trim + deadzone;
			return  (int)((max-min)*Math.abs(value) + min);
		}else if(value <0 && value >=-1) {
			//map value between -1 and 0 to value between minValue and trim-deadzone
			max = trim - deadzone;
			min = minValue;
			return  (int)(max - (max-min)*Math.abs(value));
		}else {
			//value is either 0 or invalid => return trim value
			return trim;
		}
		
	}

}
