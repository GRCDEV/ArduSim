package api.pojo;

import main.Param.SimulatorState;

/** This objects are used on PCCompanion to identify the UAVs connected to it. */

public class StatusPacket {
	
	public int row;					// Row in the table
	public long id;					// Id based on the MAC address
	public SimulatorState status;	// Simulation state of the UAV

}
