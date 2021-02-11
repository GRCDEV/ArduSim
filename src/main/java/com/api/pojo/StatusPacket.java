package com.api.pojo;


import com.setup.Param;

/** This objects are used on PCCompanion to identify the UAVs connected to it.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class StatusPacket {
	
	/** Row in the table. */
	public int row;
	/** ID based on the MAC address. */
	public long id;
	/** Simulation state of the UAV. */
	public Param.SimulatorState status;

}
