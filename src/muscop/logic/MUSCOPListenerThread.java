package muscop.logic;

import static muscop.pojo.State.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.io.Input;
import api.API;
import api.pojo.location.WaypointSimplified;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.Location3DGeo;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.SafeTakeOffHelper;
import main.api.MoveTo;
import main.api.communications.CommLink;
import main.api.masterslavepattern.MasterSlaveHelper;
import main.api.masterslavepattern.discovery.DiscoveryProgressListener;
import main.api.masterslavepattern.safeTakeOff.SafeTakeOffContext;
import main.api.masterslavepattern.safeTakeOff.SafeTakeOffListener;
import muscop.pojo.Message;

/** 
 * Thread used to listen for messages sent by other UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MUSCOPListenerThread extends Thread {
	
	private AtomicInteger currentState;

	private int numUAV;
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
		
		while (!ardusim.isAvailable()) {
			ardusim.sleep(MUSCOPParam.STATE_CHANGE_TIMEOUT);
		}
		
		/** START PHASE */
		gui.logUAV(MUSCOPText.START);
		Map<Long, Location2DUTM> UAVsDetected = null;
		if (this.isMaster) {
			gui.logVerboseUAV(MUSCOPText.MASTER_START_LISTENER);
			final AtomicInteger totalDetected = new AtomicInteger();
			UAVsDetected = msHelper.DiscoverSlaves(new DiscoveryProgressListener() {
				
				@Override
				public boolean onProgressCheckActionPerformed(int numUAVs) {
					// Just for logging purposes
					if (numUAVs > totalDetected.get()) {
						totalDetected.set(numUAVs);
						gui.log(MUSCOPText.MASTER_DETECTED_UAVS + numUAVs);
					}
					// We decide to continue when the setup button is pressed
					if (ardusim.isSetupInProgress() || ardusim.isSetupFinished()) {
						return true;
					}
					
					return false;
				}
			});
		} else {
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			msHelper.DiscoverMaster();
		}
		
		/** SHARE TAKE OFF DATA PHASE */
		currentState.set(SHARE_TAKE_OFF_DATA);
		gui.logUAV(MUSCOPText.SETUP);
		gui.updateProtocolState(MUSCOPText.SETUP);
		SafeTakeOffContext takeOff;
		List<WaypointSimplified> screenMission = null;
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
		
		/** SHARE MISSION PHASE */
		// The mission could be sent when  they are in the air, but it is better to do it now
		//   because the are closer and messages are less prone to be lost.
		currentState.set(SHARE_MISSION);
		AtomicBoolean missionReceived = new AtomicBoolean();
		AtomicInteger wpReachedSemaphore = new AtomicInteger();	// We start in waypoint 0
		final AtomicInteger moveSemaphore = new AtomicInteger(1);	// We start in waypoint 0 and move to waypoint 1
		(new MUSCOPTalkerThread(numUAV, isMaster, takeOff.isCenter(), missionReceived, wpReachedSemaphore, moveSemaphore)).start();
		gui.logUAV(MUSCOPText.SEND_MISSION);
		gui.updateProtocolState(MUSCOPText.SEND_MISSION);
		Map<Long, Long> acks = null;
		int numSlaves = takeOff.getNumUAVs() - 1;
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
			acks = new HashMap<Long, Long>((int)Math.ceil(numSlaves / 0.75) + 1);
			while (currentState.get() == SHARE_MISSION) {
				inBuffer = link.receiveMessage();
				if (inBuffer != null) {
					input.setBuffer(inBuffer);
					short type = input.readShort();

					if (type == Message.DATA_ACK) {
						long idSlave = input.readLong();
						acks.put(idSlave, idSlave);
						if (acks.size() == numSlaves) {
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
		
		/** TAKING OFF PHASE */
		gui.logUAV(MUSCOPText.TAKING_OFF);
		gui.updateProtocolState(MUSCOPText.TAKING_OFF);
		gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
		takeOffHelper.start(takeOff, new SafeTakeOffListener() {
			
			@Override
			public void onCompleteActionPerformed() {
				currentState.set(SETUP_FINISHED);
			}
		});
		while (currentState.get() < SETUP_FINISHED) {
			// Discard message
			link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
		}
		
		/** SETUP FINISHED PHASE */
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
		
		/** COMBINED PHASE MOVE_TO_WP & WP_REACHED */
		Map<Long, Long> reached = null;
		boolean iAmCenter = takeOff.isCenter();
		if (iAmCenter) {
			reached = new HashMap<Long, Long>((int)Math.ceil(numSlaves / 0.75) + 1);
		}
		int currentWP = 0;
		Location2DUTM centerUAVFinalLocation = null;
		Location3D destinationGeo;
		while (currentState.get() == FOLLOWING_MISSION) {
			
			/** WP_REACHED PHASE */
			gui.logUAV(MUSCOPText.WP_REACHED);
			gui.updateProtocolState(MUSCOPText.WP_REACHED);
			if (iAmCenter) {
				gui.logVerboseUAV(MUSCOPText.CENTER_WP_REACHED_ACK_LISTENER);
				reached.clear();
				
				while (wpReachedSemaphore.get() == currentWP) {
					inBuffer = link.receiveMessage();
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();

						if (type == Message.WAYPOINT_REACHED_ACK) {
							long id = input.readLong();
							int wp = input.readInt();
							if (wp == currentWP) {
								reached.put(id, id);
								if (reached.size() == numSlaves) {
									if (currentWP == selfMission.length - 1) {
										currentState.set(LANDING);
									}
									wpReachedSemaphore.incrementAndGet();
								}
							}
						}
					}
				}
			} else {
				gui.logVerboseUAV(MUSCOPText.NO_CENTER_WAIT_ORDER_LISTENER);
				while (wpReachedSemaphore.get() == currentWP) {
					inBuffer = link.receiveMessage();
					if (inBuffer != null) {
						input.setBuffer(inBuffer);
						short type = input.readShort();
						
						if (type == Message.MOVE_TO_WAYPOINT){
							int wp = input.readInt();
							if (wp > currentWP) {
								wpReachedSemaphore.incrementAndGet();
							}
						}
						
						if (type == Message.LAND) {
							centerUAVFinalLocation = new Location2DUTM(input.readDouble(), input.readDouble());
							currentState.set(MOVE_TO_LAND);
							wpReachedSemaphore.incrementAndGet();
						}
					}
				}
			}
			
			/** MOVE_TO_WP PHASE */
			if (currentState.get() == FOLLOWING_MISSION) {
				currentWP++;
				gui.logUAV(MUSCOPText.MOVE_TO_WP + " " + currentWP);
				gui.updateProtocolState(MUSCOPText.MOVE_TO_WP + " " + currentWP);
				gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
				
				destinationGeo = new Location3D(selfMission[currentWP]);
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
					// Discard message
					link.receiveMessage(MUSCOPParam.RECEIVING_TIMEOUT);
				}
			}
		}
		
		/** MOVE TO LAND PHASE */
		long waitingTime;
		// This only happens for UAVs not located in the center of the formation
		if (currentState.get() == MOVE_TO_LAND) {
			gui.logUAV(MUSCOPText.MOVE_TO_LAND);
			gui.updateProtocolState(MUSCOPText.MOVE_TO_LAND);
			gui.logVerboseUAV(MUSCOPText.LISTENER_WAITING);
			Location2DUTM landingLocation = takeOff.getFormationLanding().getLocation(takeOff.getFormationPosition(),
					centerUAVFinalLocation, takeOff.getInitialYaw());
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
				} catch (InterruptedException e) {}
				currentState.set(LANDING);
			} catch (LocationNotReadyException e) {
				gui.log(e.getMessage());
				e.printStackTrace();
				currentState.set(LANDING);
			}
		}
		
		/** LANDING PHASE */
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
		
		/** FINISH PHASE */
		gui.logUAV(MUSCOPText.FINISH);
		gui.updateProtocolState(MUSCOPText.FINISH);
		gui.logVerboseUAV(MUSCOPText.LISTENER_FINISHED);
	}

}