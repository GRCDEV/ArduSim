package chemotaxis.logic;

import java.util.ArrayList;

import api.pojo.location.Location2DUTM;
import smile.data.SparseDataset;

public class ChemotaxisParam {
	public static api.pojo.location.Location2DGeo startLocation;
	public static double altitude;
	public static int width;
	public static int length;
	public static double density;
	public static Location2DUTM origin;
	public static boolean isSimulation;
	public static String pollutionDataFile;
	public static ChemotaxisSensor sensor;
	public static volatile boolean ready;
	public static SparseDataset measurements;
	public static final double pThreshold = 5.0;
	public static ArrayList<chemotaxis.pojo.Value> measurements_temp;
	public static chemotaxis.pojo.ValueSet measurements_set;
}
