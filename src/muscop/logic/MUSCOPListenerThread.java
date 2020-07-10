package muscop.logic;

import api.API;
import api.pojo.FlightMode;
import api.pojo.location.WaypointSimplified;
import com.esotericsoftware.kryo.io.Input;
import es.upv.grc.mapper.*;
import main.api.*;
import main.api.communications.CommLink;
import main.api.masterslavepattern.MasterSlaveHelper;
import main.api.masterslavepattern.safeTakeOff.SafeTakeOffContext;
import muscop.pojo.Message;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static muscop.pojo.State.*;

/** 
 * Thread used to listen for messages sent by other UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPListenerThread extends Thread {
	
	private AtomicInteger currentState;

	private int numUAV;		//number of the uav
	private long selfId;
	private boolean isMaster;
	private Copter copter;
	private GUI gui;
	private byte[] inBuffer;
	private Input input;
	private CommLink link;
	private ArduSim ardusim;
	private MasterSlaveHelper msHelper;
	private SafeTakeOffHelper takeOffHelper;
	private MUSCOPTalkerThread talker;
	private Map<Long, Long> lastTimeUAV;
	private List<Long> masterOrder;
	private Set<Long> popIds;
	private int numUAVs;
	private boolean iAmCenter;
	private Set<Long> reached;
	private SafeTakeOffContext takeOff;
	private List<WaypointSimplified> screenMission = null;
	private final AtomicBoolean missionReceived = new AtomicBoolean();
	private final AtomicInteger wpReachedSemaphore = new AtomicInteger();	// We start in waypoint 0
	private final AtomicInteger moveSemaphore = new AtomicInteger(1);	// We start in waypoint 0 and move to waypoint 1

	@SuppressWarnings("unused")
	private MUSCOPListenerThread() {}
	
	public MUSCOPListenerThread(int numUAV) {
		this.currentState = MUSCOPParam.state[numUAV];
		this.numUAV = numUAV;
		this.copter = API.getCopter(numUAV);
		this.selfId = this.copter.getID();
		this.gui = API.getGUI(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.link = API.getCommLink(numUAV);
		this.ardusim = API.getArduSim();
		this.msHelper = this.copter.getMasterSlaveHelper();
		this.isMaster = this.msHelper.isMaster();
		this.takeOffHelper = this.copter.getSafeTakeOffHelper();
	}

	@Override
	public void run() {
		while (!ardusim.isAvailable()) {ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);}
		Location3DGeo[] selfMission = setup();
		while(!ardusim.isExperimentInProgress()) { ardusim.sleep(1000); }
		Location2DUTM centerUAVFinalLocation = fly(selfMission);
		landProcedure(centerUAVFinalLocation);
	}

	
	/** SETUP PHASE */
	private Location3DGeo[] setup() {
		/* START PHASE */
		Map<Long, Location2DUTM> UAVsDetected = startPhase();
		
		/* SHARE TAKE OFF DATA PHASE */
		shareTakeOffDataPhase(UAVsDetected);
		
		/* SHARE MISSION PHASE */
		Location3DGeo[] selfMission = shareMission();
		
		/* TAKING OFF PHASE */
		takingOff(takeOff);
		
		/* SETUP FINISHED PHASE */
		setupFinishedPhase();
		return selfMission;
	}
	
	private Map<Long, Location2DUTM> startPhase() {
		gui.logUAV(MUSCOPText.START);
		Map<Long, Location2DUTM> UAVsDetected = null;
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_START_LISTENER);
			final AtomicInteger totalDetected = new AtomicInteger();
			UAVsDetected = msHelper.DiscoverSlaves(numUAVs -> {
				// Just for logging purposes
				if (numUAVs > totalDetected.get()) {
					totalDetected.set(numUAVs);
					gui.log(MUSCOPText.MASTER_DETECTED_UAVS + numUAVs);
				}
				// We decide to continue when the setup button is pressed or when the number of UAVs detected is equal to the total number of UAVs -1
				return ardusim.isSetupInProgress() || ardusim.isSetupFinished() || (numUAVs == ardusim.getNumUAVs()-1);
			});
		} else {
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			msHelper.DiscoverMaster();
		}
		return UAVsDetected;
	}
	
	private void shareTakeOffDataPhase(Map<Long, Location2DUTM> UAVsDetected) {
		currentState.set(SHARE_TAKE_OFF_DATA);
		gui.logUAV(MUSCOPText.SETUP);
		gui.updateProtocolState(MUSCOPText.SETUP);
		if (this.isMaster) {
			double formationYaw;
			if (ardusim.getArduSimRole() == ArduSim.MULTICOPTER) {
				formationYaw = copter.getHeading();
			} else {
				formationYaw = MUSCOPParam.formationYaw;
			}
			
			screenMission = copter.getMissionHelper().getSimplified();
			if (screenMission == null || screenMission.size() > MUSCOPParam.MAX_WAYPOINTS) {
				gui.exit(MUSCOPText.MAX_WP_REACHED);
			}
			takeOff = takeOffHelper.getMasterContext(UAVsDetected,
					API.getFlightFormationTools().getFlyingFormation(UAVsDetected.size() + 1),
					formationYaw, screenMission.get(0).z, false, false);
		} else {
			takeOff = takeOffHelper.getSlaveContext(false);
		}
		
		this.numUAVs = takeOff.getNumUAVs();
		// set expected size of data structures for performance
		lastTimeUAV = new HashMap<>(numUAVs);
		masterOrder = new ArrayList<>(numUAVs);
		popIds = new HashSet<>(numUAVs);
		
		// set masterOrder
		long[] masterArray = takeOff.getMasterOrder();
		for (long l : masterArray) {
			this.masterOrder.add(l);
		}
	}
	
	private Location3DGeo[] shareMission() {
		// The mission could be sent when  they are in the air, but it is better to do it now
		//   because the are closer and messages are less prone to be lost.
		currentState.set(SHARE_MISSION);
		talker = new MUSCOPTalkerThread(numUAV, isMaster, masterOrder.get(0) == this.selfId, missionReceived, wpReachedSemaphore, moveSemaphore);
		talker.start();
		gui.logUAV(MUSCOPText.SEND_MISSION);
		gui.updateProtocolState(MUSCOPText.SEND_MISSION);
		Map<Long, Long> acks;
		Location3DGeo[] selfMission = null;
		
		if (this.isMaster) {
			// 1. Calculus of the mission for the UAV in the center of the formation
			Location3DUTM[] centerMission = new Location3DUTM[screenMission.size()];
			centerMission[0] = new Location3DUTM(takeOff.getCenterUAVLocation(), screenMission.get(0).z);
			for (int i = 1; i < screenMission.size(); i++) {
				WaypointSimplified wp = screenMission.get(i);
				centerMission[i] = new Location3DUTM(wp.x, wp.y, wp.z);
			}
			
			// 2. Calculate the mission for the master
			selfMission = new Location3DGeo[centerMission.length];
			Location3DUTM centerLocation;
			for (int i = 0; i < selfMission.length; i++) {
				centerLocation = centerMission[i];
				try {
					selfMission[i] = new Location3DGeo(takeOff.getFormationFlying().getLocation(takeOff.getFormationPosition(), centerLocation, takeOff.getInitialYaw()).getGeo(), centerLocation.z);
				} catch (LocationNotReadyException e) {
					e.printStackTrace();
					gui.exit(e.getMessage());
				}
			}
			// Share data with the talker thread
			missionReceived.set(true);
			// Store the mission of the center UAV to allow the talker thread to send it
			MUSCOPParam.missionSent.set(centerMission);
			
			// 3. Wait for data ack from all the slaves
			gui.logVerboseUAV(MUSCOPText.MASTER_DATA_ACK_LISTENER);
			acks = new HashMap<>((int)Math.ceil((numUAVs-1) / 0.75) + 1);
			while (currentState.get() == SHARE_MISSION) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numUAVs-1) {
							currentState.set(TAKING_OFF);
						}
					}
				}
			}
			ardusim.sleep(MUSCOPParam.MISSION_TIMEOUT);	// Wait to slaves timeout
		} else {
			gui.logVerboseUAV(MUSCOPText.SLAVE_WAIT_DATA_LISTENER);
			long lastReceivedData = 0;
			while (currentState.get() == SHARE_MISSION) {
				inBuffer = link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					if (type == Message.DATA) {
						lastReceivedData = System.currentTimeMillis();
						if (selfMission == null) {
							// The number of WP is read to know the loop length
							int size = input.readShort();
							selfMission = new Location3DGeo[size];
							Location3DUTM centerLocation = new Location3DUTM(0, 0, 0);
							for (int i = 0; i < selfMission.length; i++) {
								centerLocation.x = input.readDouble();
								centerLocation.y = input.readDouble();
								centerLocation.z = input.readDouble();
								try {
									selfMission[i] = new Location3DGeo(takeOff.getFormationFlying().getLocation(takeOff.getFormationPosition(), centerLocation, takeOff.getInitialYaw()).getGeo(), centerLocation.z);
								} catch (LocationNotReadyException e) {
									e.printStackTrace();
									gui.exit(e.getMessage());
								}
							}
							
							missionReceived.set(true);
						}
					}
				}
				
				if (selfMission != null && System.currentTimeMillis() - lastReceivedData > MUSCOPParam.MISSION_TIMEOUT) {
					currentState.set(TAKING_OFF);
				}
			}
		}
		return selfMission;
	}

	private void takingOff(SafeTakeOffContext takeOff) {
		gui.logUAV(MUSCOPText.TAKING_OFF);
		gui.updateProtocolState(MUSCOPText.TAKING_OFF);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		takeOffHelper.start(takeOff, () -> currentState.set(SETUP_FINISHED));
		while (currentState.get() < SETUP_FINISHED) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
		}
	}
	
	private void setupFinishedPhase() {
		gui.logUAV(MUSCOPText.SETUP_FINISHED);
		gui.updateProtocolState(MUSCOPText.SETUP_FINISHED);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		while (currentState.get() == SETUP_FINISHED) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
			// Coordination with ArduSim
			if (ardusim.isExperimentInProgress()) {
				currentState.set(FOLLOWING_MISSION);
			}
		}
	}
	
	
	/** FLY PHASE */
	
	private Location2DUTM fly(Location3DGeo[] selfMission) {
		/* COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		iAmCenter = (masterOrder.get(0) == this.selfId);
		if (iAmCenter) {
			StringBuilder text = new StringBuilder("Master order: ");
			for (Long aLong : masterOrder) {
				text.append(" ").append(aLong.toString());
			}
			gui.log(text.toString());
			reached = new HashSet<>((int)Math.ceil((numUAVs-1) / 0.75) + 1);
		}
		int currentWP = 0;
		Location2DUTM centerUAVFinalLocation = null;
		
		// Important step: all the UAVs must have this list initialized otherwise things like splitup wont work
		for(long id=0;id<numUAVs;id++) {
			if(id != selfId) {
				lastTimeUAV.put(id, System.currentTimeMillis());
			}
		}
		
		while (currentState.get() == FOLLOWING_MISSION) {
			/* WP_REACHED PHASE */
			centerUAVFinalLocation = wpReached(selfMission, currentWP, centerUAVFinalLocation);
			
			/* MOVE_TO_WP PHASE */
			currentWP = moveToWP(selfMission, currentWP);
		}
		return centerUAVFinalLocation;
	}

	private Location2DUTM wpReached(Location3DGeo[] selfMission, int currentWP, Location2DUTM centerUAVFinalLocation) {
		gui.logUAV(MUSCOPText.WP_REACHED);
		gui.updateProtocolState(MUSCOPText.WP_REACHED);
		reached = new HashSet<>((int)Math.ceil((numUAVs-1) / 0.75) + 1);
		reached.add(selfId);
		
		long start = System.currentTimeMillis();
		// As long as the UAVs are at the waypoint
		while(wpReachedSemaphore.get() == currentWP) {
			// update the swarm to see if some uav died
			updateSwarm();
			// if there is only one UAV let him fly until the end
			if(numUAVs <= 1) {
				wpReachedSemaphore.incrementAndGet();
				if (currentWP >= selfMission.length - 1) {currentState.set(LANDING);}
			}
			// check if a message is received
			inBuffer = link.receiveMessage(MUSCOPParam.RECEIVETIMEOUT);
			if (inBuffer != null) {
				// read the type there are 3: waypoint_reached_ack, move_to_waypoint and land
				// in each case: get all the information from the message, update lastTimeUAV , and do additional case bounded stuff
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				switch(type) {
					case Message.WAYPOINT_REACHED_ACK:
						long id = input.readLong();
						lastTimeUAV.put(id, System.currentTimeMillis());						
						if (input.readInt() == currentWP && iAmCenter) {						
							reached.add(id);
							// check if all the UAVs are at the waypoint and only then start flying
							if (reached.size() == numUAVs) {
								if (currentWP == selfMission.length - 1) {currentState.set(LANDING);}
								wpReachedSemaphore.incrementAndGet();
							}
						}
						break;
					case Message.MOVE_TO_WAYPOINT:
						lastTimeUAV.put(input.readLong(), System.currentTimeMillis());
						
						if (input.readInt() > currentWP && !iAmCenter) {
							wpReachedSemaphore.incrementAndGet();
						}
						break;
					case Message.LAND:
						if (!iAmCenter) {
							centerUAVFinalLocation = new Location2DUTM(input.readDouble(), input.readDouble());
							currentState.set(MOVE_TO_LAND);
							wpReachedSemaphore.incrementAndGet();
						}
						break;
				}
			}
		}
		System.out.println(API.getFlightFormationTools().getFlyingFormationMinimumDistance()+ "\t" +  (System.currentTimeMillis()-start) );
		return centerUAVFinalLocation;
	}

	private int moveToWP(Location3DGeo[] selfMission, int currentWP) {
		
		if (currentState.get() == FOLLOWING_MISSION) {
			currentWP++;
			gui.logUAV(MUSCOPText.MOVE_TO_WP + " " + currentWP);
			gui.updateProtocolState(MUSCOPText.MOVE_TO_WP + " " + currentWP);
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			
			Location3D destinationGeo = new Location3D(selfMission[currentWP]);
			copter.moveTo(destinationGeo, new MoveToListener() {
				
				@Override
				public void onFailure() {
					gui.exit(MUSCOPText.MOVE_ERROR_1 + " " + selfId);
				}
				
				@Override
				public void onCompleteActionPerformed() {
					moveSemaphore.incrementAndGet();
				}
			}).start();

			while (moveSemaphore.get() == currentWP) {	
				// This loop is executed as long as the UAVs are moving towards a waypoint
				// All the UAVs are broadcasting the messages with in interval of 200 ms
				// Design decision is to use this message to check if the UAVs are still alive and not send additional messages like heartbeat 
				inBuffer = link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();
					
					// Slaves send WAYPOINT_REACHED_ACK and master sends MOVE_TO_WAYPOINT
					if (type == Message.WAYPOINT_REACHED_ACK || type == Message.MOVE_TO_WAYPOINT){
						Long id = input.readLong();
						// now that we have the id of the UAV 
						// put id in the map if it exist it overrides the old value, if it doesnt exist it adds a new one
						lastTimeUAV.put(id, System.currentTimeMillis());
					}
				}
				
				// just in case the master would be some other uav not included in the above
				if( iAmCenter && currentWP == 1 && (destinationGeo.distance(copter.getLocationUTM()) < 15) ) {
					System.out.println("kill master UAV with ID " + selfId);
					copter.setFlightMode(FlightMode.LAND);
					this.talker.setRunning(false);
					currentState.set(FAILED);
					return -1;
				}
			}
		}
		return currentWP;
		
	}

	private void updateSwarm() {
		// check if some uavs has died
		for (Entry<Long, Long> uav : lastTimeUAV.entrySet()) {
			Long id = uav.getKey();
			Long time = uav.getValue();
			if((System.currentTimeMillis() - time > MUSCOPParam.TTL)) {
				gui.logUAV("UAV with id: " + id + " died");
				numUAVs--;
				popIds.add(id);
				int index = masterOrder.indexOf(id);
				if(index != -1) { masterOrder.remove(index);}
			}
		}
		
		//since removing entry while iterating is dangerous so do it now
		popIds.forEach(id -> lastTimeUAV.remove(id));
		popIds.clear();
		//setting new master
		if(selfId == masterOrder.get(0) && !iAmCenter) {
			gui.logUAV("I am the new master");
			iAmCenter = true;
			talker.setCenter(true);
		}
	}
	
	
	/* LAND PHASE */
	
	private void landProcedure(Location2DUTM centerUAVFinalLocation) {
		/* MOVE TO LAND PHASE */
		moveToLand(takeOff, centerUAVFinalLocation);
		
		/* LANDING PHASE */
		land();
		
		/* FINISH PHASE */
		gui.logUAV(MUSCOPText.FINISH);
		gui.updateProtocolState(MUSCOPText.FINISH);
		gui.logVerboseUAV(MUSCOPText.LISTENER_FINISHED);
	}

	private void moveToLand(SafeTakeOffContext takeOff, Location2DUTM centerUAVFinalLocation) {
		Location3D destinationGeo;
		// This only happens for UAVs not located in the center of the formation
		if (currentState.get() == MOVE_TO_LAND) {
			gui.logUAV(MUSCOPText.MOVE_TO_LAND);
			gui.updateProtocolState(MUSCOPText.MOVE_TO_LAND);
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			/*
			Location2DUTM landingLocation = takeOff.getFormationLanding().getLocation(takeOff.getFormationPosition(),
					centerUAVFinalLocation, takeOff.getInitialYaw());
					*/
			//TODO never run this in real experiment
			Location2DUTM landingLocation = centerUAVFinalLocation;

			try {
				destinationGeo = new Location3D(landingLocation, copter.getAltitudeRelative());
				MoveTo moveTo = copter.moveTo(destinationGeo, new MoveToListener() {
					
					@Override
					public void onFailure() {
						gui.exit(MUSCOPText.MOVE_ERROR_2 + " " + selfId);
					}
					
					@Override
					public void onCompleteActionPerformed() {
						// Nothing to do, as we wait until the target location is reached with Thread.join()
					}
				});
				moveTo.start();
				try {
					moveTo.join();
				} catch (InterruptedException ignored) {}
				currentState.set(LANDING);
			} catch (LocationNotReadyException e) {
				gui.log(e.getMessage());
				e.printStackTrace();
				currentState.set(LANDING);
			}
		}else {
			if(iAmCenter) {
				System.out.println(selfId + " not moving to land because I am master");
			}else {
				System.out.println(selfId + " not moving to land because onknown reason");
			}
		}
	}
	
	private void land() {
		long waitingTime;
		if (!copter.land()) {
			gui.exit(MUSCOPText.LAND_ERROR + " " + selfId);
		}
		gui.logUAV(MUSCOPText.LANDING);
		gui.updateProtocolState(MUSCOPText.LANDING);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		long cicleTime = System.currentTimeMillis();
		while (currentState.get() == LANDING) {
			if(!copter.isFlying()) {
				currentState.set(FINISH);
			} else {
				cicleTime = cicleTime + MUSCOPParam.LAND_CHECK_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
	}

	
}