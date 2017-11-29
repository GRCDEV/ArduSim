package api.pojo;

import org.javatuples.Quartet;

/** This class generates and object that contains the current status information received from the UAV. */

public class UAVCurrentStatus {
	
	// Current values
	private double voltage;				// Volts
	private double current;				// Amperes
	private int remainingBattery;		// Percentage
	private double cpuLoad;				// Percentage
	
	/** Updates current values with raw data received. */
	public synchronized void update(int voltage, int current, int remainingBattery, int load) {
		this.voltage = voltage * 0.001;
		this.current = current * 0.01;
		this.remainingBattery = remainingBattery;
		this.cpuLoad = load * 0.1;
	}
	
	public synchronized double getVoltage() {
		return this.voltage;
	}
	public synchronized double getCurrent() {
		return this.current;
	}
	public synchronized int getRemainingBattery() {
		return this.remainingBattery;
	}
	public synchronized double getCPULoad() {
		return this.cpuLoad;
	}
	
	/** Gets the current status: voltage(V), current (mAh), cpu load (%), remaining battery (%). */
	public synchronized Quartet<Double, Double, Integer, Double> getStatus() {
		return Quartet.with(this.voltage, this.current, this.remainingBattery, this.cpuLoad);
	}
	
	public String toString() {
		return "Status: " + String.format("%.3f", this.voltage) + " V, " + String.format("%.2f", this.current)
		+ " A, battery " + this.remainingBattery + " %, CPUload " + String.format("%.1f", this.cpuLoad) + " %";
	}
}
