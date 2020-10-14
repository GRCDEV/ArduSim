package protocols.shakeup.pojo;

import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;

public class TargetFormation {

	private String name;
	private double minDistance;
	private double heading;
	private FlightFormation f;
	
	public TargetFormation(String name, int numUAVs, double minDistance, double heading) {
		this.name = name;
		this.minDistance = minDistance;
		this.heading = heading;
		this.f = FlightFormation.getFormation(Formation.getFormation(name), numUAVs, this.minDistance);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getMinDistance() {
		return minDistance;
	}
	public void setMinDistance(double minDistance) {
		this.minDistance = minDistance;
	}
	public double getHeading() {
		return heading;
	}
	public void setHeading(double heading) {
		this.heading = heading;
	}
	public FlightFormation getFlightFormation() {
		return this.f;
	}
	public void setFlightFormation(FlightFormation ff) {
		this.f = ff;
	}
}
