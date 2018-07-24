package uavFishing.logic;

import org.javatuples.Pair;
import api.pojo.GeoCoordinates;

public class UavFishingParam {

	public static GeoCoordinates startLocationBoat,startLocationUAV;
	Pair<GeoCoordinates, Double>[] startCoordinatesArray;
	public static double heading,radius,angle,boatSpeed;
	public static boolean clockwise;
	public static int boatID,fisherID;
	
	public static double[] initial_speeds,vOrigin;
	
	
	// TCP parameters
	public static final int DATAGRAM_MAX_LENGTH = 1472; // (B) 1500-20-8 (MTU - IP - UDP)
}
