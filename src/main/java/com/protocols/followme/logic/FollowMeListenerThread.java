package com.protocols.followme.logic;

import com.api.*;
import com.api.communications.LowLevelCommLink;
import com.api.swarm.Swarm;
import com.api.swarm.SwarmParam;
import com.api.swarm.takeoff.TakeoffAlgorithm;
import com.esotericsoftware.kryo.io.Input;
import com.protocols.followme.pojo.Message;
import com.uavController.UAVParam;
import es.upv.grc.mapper.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.protocols.followme.pojo.State.*;

/** Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain). */

public class FollowMeListenerThread extends Thread {
	
	private AtomicInteger currentState;
	
	private int numUAV;
	private long selfId;
	private boolean isMaster;
	private Copter copter;
	private GUI gui;
	private ArduSim ardusim;
	
	private LowLevelCommLink link;
	byte[] inBuffer;
	Input input;

	@SuppressWarnings("unused")
	private FollowMeListenerThread() {}
	
	public FollowMeListenerThread(int numUAV) {
		super(FollowMeText.LISTENER_THREAD + numUAV);
		currentState = FollowMeParam.state[numUAV];
		this.numUAV = numUAV;
		this.copter = API.getCopter(numUAV);
		this.selfId = this.copter.getID();
		this.gui = API.getGUI(numUAV);
		this.link = LowLevelCommLink.getCommLink(numUAV);
		this.inBuffer = new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.ardusim = API.getArduSim();
		this.isMaster = numUAV == SwarmParam.masterId;
	}

	@Override
	public void run() {
		Swarm swarm = setup();
		takeoff(swarm);
		follow();
		land();
		finish();
	}

	private Swarm setup() {
		waitUntilArdusimIsAvailable();
		Swarm swarm = getSwarm();
		waitUntilExperimentIsInProcess();
		return swarm;
	}

	private void waitUntilExperimentIsInProcess() {
		while (!ardusim.isExperimentInProgress()){
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		}
	}

	private Swarm getSwarm() {
		Swarm swarm =  new Swarm.Builder(copter.getID())
				.assignmentAlgorithm(SwarmParam.assignmentAlgorithm)
				.airFormationLayout(UAVParam.airFormation.get().getLayout(),30,FollowMeParam.altitude)
				.takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms.SIMULTANEOUSLY)
				.build();
		return swarm;
	}

	private void waitUntilArdusimIsAvailable() {
		do{
			ardusim.sleep(FollowMeParam.STATE_CHANGE_TIMEOUT);
		} while (!(ardusim.isAvailable() && ardusim.isSetupInProgress()));
		gui.logUAV(FollowMeText.START);
	}

	private void follow() {
		currentState.set(FOLLOWING);
		gui.logUAV(FollowMeText.FOLLOWING);
		gui.updateProtocolState(FollowMeText.FOLLOWING);
		if (this.isMaster) {
			startRemoteAndTalkerThreadAndWaitUntilRemoteIsDone();
		} else {
			gui.logVerboseUAV(FollowMeText.SLAVE_WAIT_ORDER_LISTENER);
			while (currentState.get() == FOLLOWING) {
				processMessages();
			}
		}
	}

	private void processMessages() {
		inBuffer = link.receiveMessage();
		if (inBuffer != null) {
			input.setBuffer(inBuffer);
			short type = input.readShort();
			switch (type){
				case Message.I_AM_HERE:
					processIamHereMessage();
					break;
				case Message.LAND:
					Location3D targetLocationLanding = processLandingMessage();
					moveToLand(targetLocationLanding);
					break;
				default:
					break;
			}
		}
	}

	private void processIamHereMessage() {
		Location3DUTM masterLocation = new Location3DUTM(input.readDouble(), input.readDouble(),input.readDouble());
		double yaw = input.readDouble();
		try {
			Location3DGeo targetLocation = UAVParam.airFormation.get().get3DUTMLocation(masterLocation,numUAV).getGeo3D();
			copter.moveTo(masterLocation.getGeo3D());
		} catch (LocationNotReadyException e) {
			gui.log(e.getMessage());
			e.printStackTrace();
			// Fatal error. It lands
			currentState.set(LANDING);
		}
	}

	private Location3D processLandingMessage() {
		Location3D targetLocationLanding = null;
		Location3DUTM centerUAVFinalLocation = new Location3DUTM(input.readDouble(), input.readDouble(),0);//TODO send z
		double yaw = input.readDouble();
		Location2DUTM landingLocationUTM = UAVParam.groundFormation.get().get3DUTMLocation(centerUAVFinalLocation,numUAV);
		try {
			targetLocationLanding = new Location3D(landingLocationUTM, copter.getAltitudeRelative());
			currentState.set(MOVE_TO_LAND);
		} catch (LocationNotReadyException e) {
			gui.log(e.getMessage());
			e.printStackTrace();
			// Fatal error. It lands
			currentState.set(LANDING);
		}
		return targetLocationLanding;
	}

	private void startRemoteAndTalkerThreadAndWaitUntilRemoteIsDone() {
		Thread remote = new RemoteThread(numUAV);
		remote.start();
		new FollowMeTalkerThread(numUAV).start();
		try {
			remote.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void moveToLand(Location3D targetLocationLanding) {
		if (currentState.get() == MOVE_TO_LAND) {
			gui.logUAV(FollowMeText.MOVE_TO_LAND);
			gui.updateProtocolState(FollowMeText.MOVE_TO_LAND);
			gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
			MoveTo moveTo = copter.moveTo(targetLocationLanding, new MoveToListener() {
						@Override
						public void onFailure() {
							gui.exit(FollowMeText.MOVE_ERROR + " " + selfId);
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
		}
	}

	private void land() {
		startLanding();
		waitUntilLanded();
	}

	private void startLanding() {
		if (!copter.land()) {
			gui.exit(FollowMeText.LAND_ERROR + " " + selfId);
		}
		gui.logUAV(FollowMeText.LANDING);
		gui.updateProtocolState(FollowMeText.LANDING);
		gui.logVerboseUAV(FollowMeText.LISTENER_WAITING);
	}

	private void waitUntilLanded() {
		long waitingTime;
		long cicleTime = System.currentTimeMillis();
		while (currentState.get() == LANDING) {
			if(!copter.isFlying()) {
				currentState.set(FINISH);
			} else {
				cicleTime = cicleTime + FollowMeParam.LAND_CHECK_TIMEOUT;
				waitingTime = cicleTime - System.currentTimeMillis();
				if (waitingTime > 0) {
					ardusim.sleep(waitingTime);
				}
			}
		}
	}

	private void finish() {
		gui.logUAV(FollowMeText.FINISH);
		gui.updateProtocolState(FollowMeText.FINISH);
		gui.logVerboseUAV(FollowMeText.LISTENER_FINISHED);
	}

	private void takeoff(Swarm swarm) {
		currentState.set(TAKE_OFF);
		gui.logUAV(FollowMeText.TAKE_OFF);
		gui.updateProtocolState(FollowMeText.TAKE_OFF);
		swarm.takeOff(numUAV);
		currentState.set(SETUP_FINISHED);
	}

}
