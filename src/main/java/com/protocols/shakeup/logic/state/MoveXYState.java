package com.protocols.shakeup.logic.state;

import com.esotericsoftware.kryo.io.Input;

import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import com.protocols.shakeup.pojo.Param;

public class MoveXYState extends State{

	private final Location3DUTM targetLocation;
	
	public MoveXYState(int selfId, boolean isMaster, Location3DUTM targetLocation, int numUAVs) {
		super(selfId, isMaster);
		super.stateNr = Param.MOVE_XY;
		super.numUAVs = numUAVs;
		
		gui.logUAV("Start state " + stateNr);
		gui.updateProtocolState("" + stateNr);
		
		this.targetLocation = targetLocation;
	}

	@Override
	public void inspect(Input message) {}

	@Override
	public void executeOnce() {
		try {
			// move in xy plane but stay at current altitude
			copter.moveTo(new Location3DUTM(targetLocation.x, targetLocation.y,  copter.getAltitude()).getGeo3D());
		} catch (LocationNotReadyException e) {
			gui.log("not able to move UAV to new Z location");
		}
		
	}

	@Override
	public void executeContinously() {}

	@Override
	public State transit(Boolean transit) {
		// check is location is reached
		if(Math.abs(copter.getLocationUTM().distance(targetLocation)) < Param.ALTITUDE_MARGIN) {super.send_ack = true;}
		if(transit) {
			return new MoveZIniState(selfId, isMaster, numUAVs);
		}
		return this;
	}

}
