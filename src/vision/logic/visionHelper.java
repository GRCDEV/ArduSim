package vision.logic;

import java.awt.Graphics2D;
import java.io.File;
import java.util.Map;

import javax.swing.JFrame;
import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.CopterParam;
import api.pojo.location.Location2DGeo;
import main.api.Copter;
import main.api.FileTools;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.MoveTo;
import main.api.TakeOffListener;
import main.api.TakeOff;
import main.sim.board.BoardPanel;

/** Developed by: Jamie Wubben, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */
public class visionHelper extends ProtocolHelper {
	@Override
	public void setProtocol() {this.protocolString = "Vision";}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public void openConfigurationDialog() {
		API.getArduSim().setProtocolConfigured();
	}

	@Override
	public void initializeDataStructures() {		
		//TODO set param here
	}

	@Override
	public String setInitialState() {return null;}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D graphics, BoardPanel panel) {}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation(){

		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];
		for(int id=0;id<numUAVs;id++) {
			// just a random location e.g. UPV campus vera
			startingLocations[id] = Pair.with(new Location2DGeo(39.725064, -0.733661),0.0);
		}
		return startingLocations;
		/*
		// Gets the current coordinates from the mission when it is loaded, and the heading pointing towards the next waypoint
		int numUAVs = Tools.getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocations = new Pair[numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		List<Waypoint>[] missions = Tools.getLoadedMissions();
		List<Waypoint> mission;
		for (int i = 0; i < numUAVs; i++) {
			mission = missions[i];
			if (mission != null) {
				waypointFound = false;
				for (int j=0; j<mission.size() && !waypointFound; j++) {
					waypoint1 = mission.get(j);
					if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
						waypoint1pos = j;
						waypointFound = true;
					}
				}
				if (!waypointFound) {
					GUI.exit("No valid coordinates could be found to stablish the home of the UAV "+ Tools.getIdFromPos(i));
				}
				waypointFound = false;
				for (int j=waypoint1pos+1; j<mission.size() && !waypointFound; j++) {
					waypoint2 = mission.get(j);
					if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
						waypointFound = true;
					}
				}
				if (waypointFound) {
					// We only can set a heading if at least two points with valid coordinates are found
					p1UTM = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
					p2UTM = Tools.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
					incX = p2UTM.x - p1UTM.x;
					incY = p2UTM.y - p1UTM.y;
					if (incX != 0 || incY != 0) {
						if (incX == 0) {
							if (incY > 0)	heading = 0.0;
							else			heading = 180.0;
						} else if (incY == 0) {
							if (incX > 0)	heading = 90;
							else			heading = 270.0;
						} else {
							double gamma = Math.atan(incY/incX);
							if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
							else 			heading = 270.0 - gamma * 180 / Math.PI;
						}
					}
				}
			} else {
				// Assuming that all UAVs have a mission loaded
				GUI.exit("ArduSim: Failed locating the home position of the UAV " + Tools.getIdFromPos(i) + ".");
			}
			startingLocations[i] = Pair.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), heading);
		}
		return startingLocations;
		*/
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		Copter copter = API.getCopter(numUAV);
		
		Double rcMapRoll = null;
		rcMapRoll = copter.getParameter(CopterParam.RCMAP_ROLL);
		if(rcMapRoll == null) {return false;}
		
		Double rcMapPitch = null;
		rcMapPitch = copter.getParameter(CopterParam.RCMAP_PITCH);
		if(rcMapPitch == null) {return false;}
		
		Double rcMapThrottle = null;
		rcMapThrottle = copter.getParameter(CopterParam.RCMAP_THROTTLE);
		if(rcMapThrottle == null) {return false;}
		
		Double rcMapYaw = null;
		rcMapYaw = copter.getParameter(CopterParam.RCMAP_YAW);
		if(rcMapYaw == null) {return false;}
		
		
		int[] controls = new int[] {rcMapRoll.intValue(),rcMapPitch.intValue(),rcMapThrottle.intValue(),rcMapYaw.intValue()};
		int[][] paramValues = new int[4][4];
		
		//really tedious but probably the only way
		for(int i=0;i<4;i++) {
			switch (controls[i]) {
				case 1:
					Double rc1Min = null;
					rc1Min = copter.getParameter(CopterParam.MIN_RC1);
					if (rc1Min == null) {return false;}
					paramValues[i][0]= rc1Min.intValue();
					
					Double rc1Max = null;
					rc1Max = copter.getParameter(CopterParam.MAX_RC1);
					if (rc1Max == null) {return false;}
					paramValues[i][1] = rc1Max.intValue();
					
					Double rc1Trim = null;
					rc1Trim = copter.getParameter(CopterParam.TRIM_RC1);
					if (rc1Trim == null) {return false;}
					paramValues[i][2] = rc1Trim.intValue();
					
					Double rc1DZ = null;
					rc1DZ = copter.getParameter(CopterParam.DZ_RC1);
					if(rc1DZ == null) {return false;}
					paramValues[i][3] = rc1DZ.intValue();
					break;
				case 2:
					Double rc2Min = null;
					rc2Min = copter.getParameter(CopterParam.MIN_RC2);
					if (rc2Min == null) {return false;}
					paramValues[i][0]= rc2Min.intValue();
					
					Double rc2Max = null;
					rc2Max = copter.getParameter(CopterParam.MAX_RC2);
					if (rc2Max == null) {return false;}
					paramValues[i][1] = rc2Max.intValue();
					
					Double rc2Trim = null;
					rc2Trim = copter.getParameter(CopterParam.TRIM_RC2);
					if (rc2Trim == null) {return false;}
					paramValues[i][2] = rc2Trim.intValue();
					
					Double rc2DZ = null;
					rc2DZ = copter.getParameter(CopterParam.DZ_RC2);
					if(rc2DZ == null) {return false;}
					paramValues[i][3] = rc2DZ.intValue();
					break;
				case 3:
					Double rc3Min = null;
					rc3Min = copter.getParameter(CopterParam.MIN_RC3);
					if (rc3Min == null) {return false;}
					paramValues[i][0]= rc3Min.intValue();
					
					Double rc3Max = null;
					rc3Max = copter.getParameter(CopterParam.MAX_RC3);
					if (rc3Max == null) {return false;}
					paramValues[i][1] = rc3Max.intValue();
					
					Double rc3Trim = null;
					rc3Trim = copter.getParameter(CopterParam.TRIM_RC3);
					if (rc3Trim == null) {return false;}
					paramValues[i][2] = rc3Trim.intValue();
					
					Double rc3DZ = null;
					rc3DZ = copter.getParameter(CopterParam.DZ_RC3);
					if(rc3DZ == null) {return false;}
					paramValues[i][3] = rc3DZ.intValue();
					break;
				case 4:
					Double rc4Min = null;
					rc4Min = copter.getParameter(CopterParam.MIN_RC4);
					if (rc4Min == null) {return false;}
					paramValues[i][0]= rc4Min.intValue();
					
					Double rc4Max = null;
					rc4Max = copter.getParameter(CopterParam.MAX_RC4);
					if (rc4Max == null) {return false;}
					paramValues[i][1] = rc4Max.intValue();
					
					Double rc4Trim = null;
					rc4Trim = copter.getParameter(CopterParam.TRIM_RC4);
					if (rc4Trim == null) {return false;}
					paramValues[i][2] = rc4Trim.intValue();
					
					Double rc4DZ = null;
					rc4DZ = copter.getParameter(CopterParam.DZ_RC4);
					if(rc4DZ == null) {return false;}
					paramValues[i][3] = rc4DZ.intValue();
					break;
				case 5:
					Double rc5Min = null;
					rc5Min = copter.getParameter(CopterParam.MIN_RC5);
					if (rc5Min == null) {return false;}
					paramValues[i][0]= rc5Min.intValue();
					
					Double rc5Max = null;
					rc5Max = copter.getParameter(CopterParam.MAX_RC5);
					if (rc5Max == null) {return false;}
					paramValues[i][1] = rc5Max.intValue();
					
					Double rc5Trim = null;
					rc5Trim = copter.getParameter(CopterParam.TRIM_RC5);
					if (rc5Trim == null) {return false;}
					paramValues[i][2] = rc5Trim.intValue();
					
					Double rc5DZ = null;
					rc5DZ = copter.getParameter(CopterParam.DZ_RC5);
					if(rc5DZ == null) {return false;}
					paramValues[i][3] = rc5DZ.intValue();
					break;
				case 6:
					Double rc6Min = null;
					rc6Min = copter.getParameter(CopterParam.MIN_RC6);
					if (rc6Min == null) {return false;}
					paramValues[i][0]= rc6Min.intValue();
					
					Double rc6Max = null;
					rc6Max = copter.getParameter(CopterParam.MAX_RC6);
					if (rc6Max == null) {return false;}
					paramValues[i][1] = rc6Max.intValue();
					
					Double rc6Trim = null;
					rc6Trim = copter.getParameter(CopterParam.TRIM_RC6);
					if (rc6Trim == null) {return false;}
					paramValues[i][2] = rc6Trim.intValue();
					
					Double rc6DZ = null;
					rc6DZ = copter.getParameter(CopterParam.DZ_RC6);
					if(rc6DZ == null) {return false;}
					paramValues[i][3] = rc6DZ.intValue();
					break;
				case 7:
					Double rc7Min = null;
					rc7Min = copter.getParameter(CopterParam.MIN_RC7);
					if (rc7Min == null) {return false;}
					paramValues[i][0]= rc7Min.intValue();
					
					Double rc7Max = null;
					rc7Max = copter.getParameter(CopterParam.MAX_RC7);
					if (rc7Max == null) {return false;}
					paramValues[i][1] = rc7Max.intValue();
					
					Double rc7Trim = null;
					rc7Trim = copter.getParameter(CopterParam.TRIM_RC7);
					if (rc7Trim == null) {return false;}
					paramValues[i][2] = rc7Trim.intValue();
					
					Double rc7DZ = null;
					rc7DZ = copter.getParameter(CopterParam.DZ_RC7);
					if(rc7DZ == null) {return false;}
					paramValues[i][3] = rc7DZ.intValue();
					break;
				case 8:
					Double rc8Min = null;
					rc8Min = copter.getParameter(CopterParam.MIN_RC8);
					if (rc8Min == null) {return false;}
					paramValues[i][0]= rc8Min.intValue();
					
					Double rc8Max = null;
					rc8Max = copter.getParameter(CopterParam.MAX_RC8);
					if (rc8Max == null) {return false;}
					paramValues[i][1] = rc8Max.intValue();
					
					Double rc8Trim = null;
					rc8Trim = copter.getParameter(CopterParam.TRIM_RC8);
					if (rc8Trim == null) {return false;}
					paramValues[i][2] = rc8Trim.intValue();
					
					Double rc8DZ = null;
					rc8DZ = copter.getParameter(CopterParam.DZ_RC8);
					if(rc8DZ == null) {return false;}
					paramValues[i][3] = rc8DZ.intValue();
					break;
				default:
					return false;
			}
		}
				
		// paramValues
		// roll pitch throttle yaw
		// min max trim dz
		
		visionParam.rollMin = paramValues[0][0];
		visionParam.rollMax = paramValues[0][1];
		visionParam.rollTrim = paramValues[0][2];
		visionParam.rollDeadzone = paramValues[0][3];
		
		visionParam.pitchMin = paramValues[1][0];
		visionParam.pitchMax = paramValues[1][1];
		visionParam.pitchTrim = paramValues[1][2];
		visionParam.pitchDeadzone = paramValues[1][3];
		
		visionParam.throttleMin = paramValues[2][0];
		visionParam.throttleMax = paramValues[2][1];
		//TODO find true mid value somewhere
		visionParam.throttleTrim = visionParam.throttleMin + (visionParam.throttleMax - visionParam.throttleMin)/2;
		visionParam.throttleDeadzone = copter.getParameter(CopterParam.DZ_THROTTLE).intValue();
		
		visionParam.yawMin = paramValues[3][0];
		visionParam.yawMax = paramValues[3][1];
		visionParam.yawTrim = paramValues[3][2];
		visionParam.yawDeadzone = paramValues[3][3];
		
		readIniFile("location.ini");
		
		return true;}

	@Override
	public void startThreads() {}

	@Override
	public void setupActionPerformed() {
		/*
		//only for testing purpuse without flying the drone
		
		GUI.log("start setup testing");
		ServerSocket serverSocket = null;
		BufferedReader reader =null;
		try {
			serverSocket = new ServerSocket(5764);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//socket to talk with python
		ClientSocket socket = new ClientSocket(visionParam.PTYHON_SERVER_IP,5761);
		GUI.log("connected: " + socket.isConnected());
		GUI.log("setup testing camera");
		Boolean succes = socket.sendMessage("start_camera").equalsIgnoreCase("ACK");
		if(succes) {
			GUI.log("find_target");
			socket.sendMessage("find_target");
			
			Socket markerSocket;
			try {
				markerSocket = serverSocket.accept();
				reader = new BufferedReader(new InputStreamReader(markerSocket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				
			}
			String respons = "";
			int count = 0;
			long time = System.currentTimeMillis(); 
			//in the test fase wait for 60 seconds
			//since readLine is blocking only stops after 60 and seeing a marker
			while(System.currentTimeMillis() < time + 60*1000) {
				GUI.log("count is " + count);
				try {
					respons = reader.readLine();
					if(respons != null) {
						count +=1;
						GUI.log(respons);
					}
				}catch(Exception e) {
					//TODO fix this error
					System.err.println("cannot read reader");
					//e.printStackTrace();
				}
			}
		}
		GUI.log("stopping test");
		socket.sendMessage("stop_camera");
		socket.sendMessage("exit");
		GUI.log("camera tested");
		*/
	}

	@Override
	/**
	 * This methods lets all drones take off
	 * Starts a thread for each drone and let them navigate trough the planned path.
	 */
	public void startExperimentActionPerformed() {
		int numUAVs = API.getArduSim().getNumUAVs();
		GUI gui;
		Copter copter;
		for(int numUAV = 0; numUAV< numUAVs;numUAV++) {
			uavNavigator drone = uavNavigator.getInstance(numUAV);
			// if during the construction of drone anything went wrong getSucces will return false
			// and the drone won`t take off
			if(drone.isRunning()) {
				gui = API.getGUI(numUAV);
				copter = API.getCopter(numUAV);
				gui.log("taking of drones");
				final GUI gui2 = gui;
				TakeOff takeOff = copter.takeOff(visionParam.ALTITUDE, new TakeOffListener() {
					
					@Override
					public void onFailureListener() {
						gui2.warn("ERROR", "drone could not take off");
					}
					
					@Override
					public void onCompletedListener() {
						// Waiting with Thread.join()
					}
				});
				takeOff.start();
				try {
					takeOff.join();
				} catch (InterruptedException e1) {}
				
				gui.log(visionParam.LATITUDE + " : " + visionParam.LONGITUDE);
				
				MoveTo moveTo = copter.moveTo(new Location2DGeo(visionParam.LATITUDE,visionParam.LONGITUDE),
						visionParam.ALTITUDE, new MoveToListener() {
							
							@Override
							public void onFailureListener() {
								// TODO something in case of failure
							}
							
							@Override
							public void onCompletedListener() {
								// Nothing to do, as we wait until the target location is reached with Thread.join()
							}
						});
				moveTo.start();
				try {
					moveTo.join();
				} catch (InterruptedException e) {}
				
				drone.start();
			}
		}
		
	}

	@Override
	public void forceExperimentEnd() {
		// When the UAVs are close to the last waypoint a LAND command is issued
//		int numUAVs = Tools.getNumUAVs();
//		for (int i = 0; i < numUAVs; i++) {
//			Copter.landIfMissionEnded(i, visionParam.LAST_WP_THRESHOLD);
//		}
	}

	@Override
	public String getExperimentResults() {return null;}

	@Override
	public String getExperimentConfiguration() {return null;}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}

	public static void readIniFile(String filename) {
		FileTools fileTools = API.getFileTools();
		File iniFile = new File(fileTools.getCurrentFolder() + "/" + filename);
		
		if(iniFile.exists()) {
			Map<String, String> params = fileTools.parseINIFile(iniFile);
			visionParam.LATITUDE = Double.parseDouble(params.get("LATITUDE"));
			visionParam.LONGITUDE = Double.parseDouble(params.get("LONGITUDE"));
			visionParam.ALTITUDE = Double.parseDouble(params.get("ALTITUDE"));
			API.getGUI(0).log("landing location set from location.ini");
		}
	}
	
	
}
