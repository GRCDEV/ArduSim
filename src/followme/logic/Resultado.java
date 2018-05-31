package followme.logic;

import api.pojo.GeoCoordinates;

public class Resultado {

	private GeoCoordinates geo;
	private double heading;
	private double speedX;
	private double speedY;
	private double speedZ;
	public boolean speed;
	
	public Resultado(GeoCoordinates geo, double heading){
		this.geo = geo;
		this.heading = heading;
		this.speed = false;
	}
	
	public Resultado(GeoCoordinates geo, double heading, double speedX, double speedY, double speedZ) {
		this.geo = geo;
		this.heading = heading;
		this.speedX = speedX;
		this.speedY = speedY;
		this.speedZ = speedZ;
		this.speed = true;
	}
	
	public GeoCoordinates getGeo() {
		return geo;
	}
	public void setGeo(GeoCoordinates geo) {
		this.geo = geo;
	}
	public double getHeading() {
		return heading;
	}
	public void setHeading(double heading) {
		this.heading = heading;
	}
	public double getSpeedX() {
		return speedX;
	}
	public void setSpeedX(double speedX) {
		this.speedX = speedX;
	}
	public double getSpeedY() {
		return speedY;
	}
	public void setSpeedY(double speedY) {
		this.speedY = speedY;
	}
	public double getSpeedZ() {
		return speedZ;
	}
	public void setSpeedZ(double speedZ) {
		this.speedZ = speedZ;
	}

	
	
}
