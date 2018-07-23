package lander.logic;

import api.pojo.GeoCoordinates;

public interface LanderSensorInterface {
	GeoCoordinates readGeoordinates();
	double Latitude();
	double Longitude();
}
