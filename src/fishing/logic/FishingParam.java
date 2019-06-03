package fishing.logic;

import java.util.List;

import org.javatuples.Pair;

import api.pojo.location.Location2DGeo;
import api.pojo.location.Waypoint;

public class FishingParam {

	public static Location2DGeo startLocationBoat,startLocationUAV;
	Pair<Location2DGeo, Double>[] startCoordinatesArray;
	public static double radius,rotationAngle,boatSpeed,UavAltitude;
	public static double distanceTreshold=10;
	public static double estimateTreshold=1.2;
	public static boolean clockwise;
	public static int fisherID=1;
	public static int boatID=0;
	public static double[] vOrigin = {0,1};
	public static List<Waypoint>[] boatMission;
	
	
	// TCP parameters
	public static final int DATAGRAM_MAX_LENGTH = 1472; // (B) 1500-20-8 (MTU - IP - UDP)
	public static final int FLIGTH_MAX_TIME = 1800; // Maximum UAV flight time
}
