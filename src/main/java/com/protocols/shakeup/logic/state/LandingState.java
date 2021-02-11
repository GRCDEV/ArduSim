package com.protocols.shakeup.logic.state;

import com.esotericsoftware.kryo.io.Input;

import com.api.pojo.FlightMode;
import com.protocols.shakeup.pojo.Param;

public class LandingState extends State{

	public LandingState(int selfId, boolean isMaster) {
		super(selfId, isMaster);
		super.stateNr = Param.LANDING;
		super.numUAVs = numUAVs;
		
		gui.logUAV("Start state " + stateNr);
		gui.updateProtocolState("" + stateNr);
	}

	@Override
	public void inspect(Input message) {}

	@Override
	public void executeOnce() {
		gui.logUAV("change to landing mode");
		copter.setFlightMode(FlightMode.LAND);
		copter.land();
	}

	@Override
	public void executeContinously() {}

	@Override
	public State transit(Boolean transit) {
		return this;
	}

}
