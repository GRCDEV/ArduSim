package protocols.chemotaxis.logic;

import java.awt.BasicStroke;
import java.awt.Stroke;

import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import smile.data.SparseDataset;

public class ChemotaxisParam {
	public static Location2DGeo startLocation;
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
	public static protocols.chemotaxis.pojo.ValueSet measurements_set;
	
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
}
