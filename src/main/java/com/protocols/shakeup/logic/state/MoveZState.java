package com.protocols.shakeup.logic.state;

import com.esotericsoftware.kryo.io.Input;

import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import com.protocols.shakeup.pojo.Param;

public class MoveZState extends State{

	private final Location3DUTM targetLocation;
	
	public MoveZState(int selfId, boolean isMaster, Location3DUTM targetLocation, int numUAVs) {
		super(selfId, isMaster);
		super.stateNr = Param.MOVE_Z;
		super.numUAVs = numUAVs;
		
		gui.logUAV("Start state " + stateNr);
		gui.updateProtocolState("" + stateNr);
		gui.logUAV("Changing altitude with " + targetLocation.z + " meters");
		
		this.targetLocation = targetLocation;
	}

	@Override
	public void executeOnce() {
		try {
			// stay at xy increase altitude with targetLocation.z
			copter.moveTo(new Location3DUTM(copter.getLocationUTM(), copter.getAltitude() + targetLocation.z).getGeo3D());
		} catch (LocationNotReadyException e) {
			gui.log("not able to move UAV to new Z location");
		}
	}

	@Override
	public void executeContinously() {}

	@Override
	public State transit(Boolean transit) {
		// check if altitude is reached
		if(Math.abs(copter.getAltitude() - (Param.altitude + targetLocation.z)) < Param.ALTITUDE_MARGIN) {
			super.send_ack = true;
			if(transit) {
				// for the master check if all the slaves have acknowledged your message and then move to next state
				if(isMaster && (acknowledged.size() == numUAVs -1)) {return new MoveXYState(selfId, isMaster, targetLocation, numUAVs);}
				// slaves can go to next slave whenever the altitude is reached
				else if(!isMaster){return new MoveXYState(selfId, isMaster, targetLocation, numUAVs);}
			}
		}
		return this;
	}

	@Override
	public void inspect(Input message) {}

}
