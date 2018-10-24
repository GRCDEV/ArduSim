package uavFishing.logic;

import java.util.List;

import org.javatuples.Pair;
import api.pojo.GeoCoordinates;
import api.pojo.Waypoint;

public class UavFishingParam {

	public static GeoCoordinates startLocationBoat,startLocationUAV;
	Pair<GeoCoordinates, Double>[] startCoordinatesArray;
	public static double heading,radius,angle,boatSpeed,distanceTreshold;
	public static boolean clockwise;
	public static int fisherID=1;
	public static int boatID=0;
	public static double[] vOrigin = {1,0};
	public static List<Waypoint>[] boatMission;
	
	
	// TCP parameters
	public static final int DATAGRAM_MAX_LENGTH = 1472; // (B) 1500-20-8 (MTU - IP - UDP)
}
