package com.protocols.shakeup.logic.state;

import com.esotericsoftware.kryo.io.Input;

import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import com.protocols.shakeup.logic.ShakeupListenerThread;
import com.protocols.shakeup.pojo.Param;

public class MoveZIniState extends State{

	public MoveZIniState(int selfId, boolean isMaster, int numUAVs) {
		super(selfId, isMaster);
		super.stateNr = Param.MOVE_Z_INI;
		super.numUAVs = numUAVs;
		
		gui.logUAV("Start state " + stateNr);
		gui.updateProtocolState("" + stateNr);
	}

	@Override
	public void inspect(Input message) {}

	@Override
	public void executeOnce() {
		try {
			// stay at xy increase altitude with targetLocation.z
			copter.moveTo(new Location3DUTM(copter.getLocationUTM(), Param.altitude).getGeo3D());
		} catch (LocationNotReadyException e) {
			gui.log("not able to move UAV to new Z location");
		}
	}

	@Override
	public void executeContinously() {}

	@Override
	public State transit(Boolean transit) {
		// check if altitude is reached
		if(Math.abs(copter.getAltitude() - Param.altitude) < Param.ALTITUDE_MARGIN) {
			super.send_ack = true;
			if(transit) {
				// if all the formations are done land otherwise start from begining
				int formationIndex = ShakeupListenerThread.getFormationIndex();
				if(formationIndex == (Param.formations.length-1)) {
					return new LandingState(selfId, isMaster);
				}else {
					formationIndex++;
					ShakeupListenerThread.setFormationIndex(formationIndex);
					return this;
					// TODO implement how to go back
				}	
			}
		}
		return this;
	}

}
