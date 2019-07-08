package catchme.logic;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicReference;

import api.pojo.location.Location2D;

public class CatchMeParams {
	
	/* Default params*/
	/*Position*/
	public static double ALTITUDE = 7.0;
	public static Location2D startingLocation = new Location2D(39.480221, -0.350269);
	
	
	public static final String PTYHON_SERVER_IP = "127.0.0.1";
	public static enum status {LAND, DESCEND, MOVE, ROTATE, LOITER};
	
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
//	public static Location2D startingLocation;
	public static AtomicReference<Point2D.Double> targetLocationPX;
}
