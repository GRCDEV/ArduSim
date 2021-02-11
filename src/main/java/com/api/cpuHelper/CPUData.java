package com.api.cpuHelper;

import com.setup.Param;

/** Stores the current CPU usage level at a certain instant.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CPUData {
	
	public long time;			// (ns) JVM instant when the data was read
	public double globalCPU;	// (%) CPU usage level for the whole system
	public double processCPU;	// (%) CPU usage of the Java VM (ArduSim, and not considering virtual multicopters during simulation)
	public String simState;		// Current ArduSim state name
	
	@SuppressWarnings("unused")
	private CPUData() {}
	
	public CPUData(long time, double globalCPU, double processCPU, Param.SimulatorState simState) {
		this.time = time;
		this.globalCPU = globalCPU;
		this.processCPU = processCPU;
		this.simState = simState.name();
	}
	
}
