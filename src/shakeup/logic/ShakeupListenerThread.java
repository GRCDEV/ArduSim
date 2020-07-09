package shakeup.logic;

import api.API;
import com.esotericsoftware.kryo.io.Input;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.SafeTakeOffHelper;
import main.api.communications.CommLink;
import main.api.masterslavepattern.MasterSlaveHelper;
import main.api.masterslavepattern.safeTakeOff.SafeTakeOffContext;
import main.uavController.UAVParam;
import shakeup.logic.state.DataState;
import shakeup.pojo.Param;
import shakeup.pojo.Text;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ShakeupListenerThread extends Thread{

	private final ArduSim ardusim;
	private final GUI gui;
	private final Copter copter;
	private final int numUAV;
	private boolean isMaster;
	
	private static int formationIndex = 0;
	
	private FileWriter times;
	private final Set<java.util.Map.Entry<Integer,Integer>> collisionList= new HashSet<>();
	public ShakeupListenerThread(int numUAV) {
		this.ardusim = API.getArduSim();
		this.gui = API.getGUI(numUAV);
		this.copter = API.getCopter(numUAV);
		this.numUAV = numUAV;
		
		try {
			times = new FileWriter("timesState.csv",true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (!ardusim.isAvailable()) {ardusim.sleep(Param.TIMEOUT);}
		// discover and take of the UAVS
		Map<Long, Location2DUTM> UAVsDetected = setup();
		while(!ardusim.isExperimentInProgress()) {ardusim.sleep(Param.TIMEOUT);}
		takeOff(UAVsDetected);
		
		// create some necessary variables
		ShakeupTalkerThread talker = new ShakeupTalkerThread(numUAV);
		shakeup.logic.state.State currentState = new DataState(numUAV,isMaster, UAVsDetected);
		talker.setState(currentState);
		talker.start();
		byte[] inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Input input = new Input(inBuffer);
		CommLink link = API.getCommLink(numUAV);
		
		long time = System.currentTimeMillis();
		long distanceTimer = System.currentTimeMillis();
		double minDist = Double.MAX_VALUE;
		
		// go through all the states
		while(copter.isFlying()) {
			// Check for a message
			inBuffer = link.receiveMessage(200);
			if(inBuffer != null) {
				input.setBuffer(inBuffer);
				currentState.processMessage(input);
			}
			
			// for measuring the time past each waypoint
			int previousState = currentState.getStateNr();
			
			currentState = currentState.handle();
			talker.setState(currentState);
			
			if(previousState != currentState.getStateNr()) {
				try {
					times.write(numUAV + ";" + previousState + ";" + (System.currentTimeMillis()- time) + "\n");
					times.flush();
					time = System.currentTimeMillis();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(currentState.getStateNr() == 3 && (System.currentTimeMillis() - distanceTimer) > 500) {
				distanceTimer = System.currentTimeMillis();
				for (int i = 0; i < main.Param.numUAVs - 1; i++) {
					for (int j = i + 1; j < main.Param.numUAVs; j++) {
						Location3DUTM loc1 = new Location3DUTM(UAVParam.uavCurrentData[i].getUTMLocation(), UAVParam.uavCurrentData[i].getZ());
						Location3DUTM loc2 = new Location3DUTM(UAVParam.uavCurrentData[j].getUTMLocation(), UAVParam.uavCurrentData[j].getZ());
						double distance = loc1.distance3D(loc2);
						if(distance < minDist) {minDist = distance;}
						if(distance < 4.9) {
							java.util.Map.Entry<Integer,Integer> pair=new java.util.AbstractMap.SimpleEntry<>(i,j);
							collisionList.add(pair);
						}
					}
				}
			}
		}
		collisionList.forEach(e -> System.out.print(e.getKey() + ":" + e.getValue() + " ; "));
		System.out.println("minimal distance = " + minDist);
	}

	private Map<Long, Location2DUTM> setup() {
		// DISCOVER MASTER AND SLAVES
		gui.logUAV(Text.START);
		// Let the master detect slaves until the setup button is pressed
		Map<Long, Location2DUTM> UAVsDetected = null;
		MasterSlaveHelper msHelper = copter.getMasterSlaveHelper();
		isMaster = msHelper.isMaster();
		if(isMaster) {
			final AtomicInteger totalDetected = new AtomicInteger();
			UAVsDetected = msHelper.DiscoverSlaves(numUAVs -> {
				// Just for logging purposes
				if(numUAVs > totalDetected.get()) {
					totalDetected.set(numUAVs);
					gui.log(Text.MASTER_DETECTED_UAVS + numUAVs);
				}
				//We decide to continue when the setup button is pressed
				return ardusim.isSetupInProgress() || ardusim.isSetupFinished();
			});
		}else {
			msHelper.DiscoverMaster();
		}
		return UAVsDetected;
	}
	
	private void takeOff(Map<Long, Location2DUTM> UAVsDetected) {
		// TAKE OFF PHASE
		gui.logUAV(Text.START_TAKE_OFF);
		gui.updateProtocolState(Text.TAKE_OFF);
		// 1. Synchronize master with slaves to get the takeoff sequence in the take off context object
		SafeTakeOffContext takeOff;
		SafeTakeOffHelper takeOffHelper = copter.getSafeTakeOffHelper();
		if(isMaster) {
			double formationYaw;
			if(ardusim.getArduSimRole() == ArduSim.MULTICOPTER) {
				formationYaw = copter.getHeading();				
			}else {
				formationYaw = Param.masterInitialYaw;
			}
			takeOff = takeOffHelper.getMasterContext(UAVsDetected, 
					API.getFlightFormationTools().getFlyingFormation(UAVsDetected.size() + 1),
					formationYaw, Param.altitude, true, false);
		}else {
			takeOff = takeOffHelper.getSlaveContext(false);
		}
		// 2. Take off all the UAVs
		AtomicBoolean isTakeOffFinished = new AtomicBoolean(false);
		takeOffHelper.start(takeOff, () -> isTakeOffFinished.set(true));
		while (!isTakeOffFinished.get()) {ardusim.sleep(Param.TIMEOUT);}
	}
	
	public static int getFormationIndex() {
		return formationIndex;
	}

	public static void setFormationIndex(int formationIndex) {
		ShakeupListenerThread.formationIndex = formationIndex;
	}
}
