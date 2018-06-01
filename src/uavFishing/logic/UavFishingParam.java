package uavFishing.logic;

import org.javatuples.Pair;
import api.pojo.GeoCoordinates;

public class UavFishingParam {

	public static GeoCoordinates startLocationBoat;
	public static GeoCoordinates startLocationUAV;
	Pair<GeoCoordinates, Double>[] startCoordinatesArray;
	public static double heading;
	public static double radius;
	public static double velocidad;
}
