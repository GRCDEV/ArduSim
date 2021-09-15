package com.protocols.shakeup.pojo;

import com.api.swarm.formations.Formation;
import com.api.swarm.formations.FormationFactory;

public class TargetFormation {

	private String name;
	private double minDistance;
	private double heading;
	private Formation f;
	
	public TargetFormation(String name, int numUAVs, double minDistance, double heading) {
		this.name = name;
		this.minDistance = minDistance;
		this.heading = heading;
		this.f = FormationFactory.newFormation(Formation.Layout.valueOf(name));
		this.f.init(numUAVs,this.minDistance);
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
	public Formation getFlightFormation() {
		return this.f;
	}
	public void setFlightFormation(Formation ff) {
		this.f = ff;
	}
}
