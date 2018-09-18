package pollution;

import java.util.ArrayList;

import api.pojo.UTMCoordinates;
import smile.data.SparseDataset;

public class PollutionParam {
	public static api.pojo.GeoCoordinates startLocation;
	public static double altitude;
	public static int width;
	public static int length;
	public static double density;
	public static UTMCoordinates origin;
	public static boolean isSimulation;
	public static String pollutionDataFile;
	public static PollutionSensor sensor;
	public static volatile boolean ready;
	public static SparseDataset measurements;
	public static final double pThreshold = 5.0;
	public static ArrayList<pollution.pojo.Value> measurements_temp;
	public static pollution.pojo.ValueSet measurements_set;
}
