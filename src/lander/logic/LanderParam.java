package lander.logic;

//import smile.data.SparseDataset;

public class LanderParam {

	public static api.pojo.GeoCoordinates LocationStart;
	public static api.pojo.GeoCoordinates LocationEnd;
	
	public static int altitude;
	//public static int width;
	public static double distMax;
	public static int otherValue;
	
	public static boolean mIsSimulation;
	public static String LanderDataFile;
	public static LanderSensorInterface sensor;
	
	public static volatile boolean ready;

	//public static SparseDataset measurements;
	public static final double pThreshold = 5.0;
	
}
