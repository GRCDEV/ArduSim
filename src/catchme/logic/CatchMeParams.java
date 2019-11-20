package catchme.logic;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.concurrent.atomic.AtomicReference;

import es.upv.grc.mapper.Location2DUTM;

public class CatchMeParams {
	
	/* Default params*/
	/*Position*/
	public static double ALTITUDE = 7.0;
	public static AtomicReference<Location2DUTM> startingLocation = new AtomicReference<Location2DUTM>();
	
	
	public static final String PTYHON_SERVER_IP = "127.0.0.1";
	public static enum status {LAND, DESCEND, MOVE, ROTATE, LOITER};
	
	public static final Stroke STROKE_POINT = new BasicStroke(1f);
}
