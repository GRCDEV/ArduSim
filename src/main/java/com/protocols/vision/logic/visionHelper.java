package com.protocols.vision.logic;

import com.api.*;
import com.api.copter.*;
import com.setup.Param;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3D;
import org.javatuples.Pair;

import javax.swing.*;

/** Developed by: Jamie Wubben, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */
public class visionHelper extends ProtocolHelper {
	@Override
	public void setProtocol() {this.protocolString = "Vision";}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {
		return null;
	}

	@Override
	public void openConfigurationDialogFX() {
		Param.simStatus = Param.SimulatorState.STARTING_UAVS;}

	@Override
	public void configurationCLI() {

	}

	@Override
	public void initializeDataStructures() {		
	}

	@Override
	public String setInitialState() {return null;}

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
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) { return true;}

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
					public void onFailure() {
						gui2.warn("ERROR", "drone could not take off");
					}
					
					@Override
					public void onCompleteActionPerformed() {
						// Waiting with Thread.join()
					}
				});
				takeOff.start();
				try {
					takeOff.join();
				} catch (InterruptedException e1) {}
				
				gui.log(visionParam.LATITUDE + " : " + visionParam.LONGITUDE);
				
				MoveTo moveTo = copter.moveTo(new Location3D(visionParam.LATITUDE,visionParam.LONGITUDE, visionParam.ALTITUDE),
						new MoveToListener() {
							
							@Override
							public void onFailure() {
								gui2.warn("ERROR", "drone could not take off");
							}
							
							@Override
							public void onCompleteActionPerformed() {
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
	public void forceExperimentEnd() {	}

	@Override
	public String getExperimentResults() {return null;}

	@Override
	public String getExperimentConfiguration() {return null;}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {}

}
