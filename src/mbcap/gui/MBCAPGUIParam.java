package mbcap.gui;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.Point3D;

/** This class contains parameters related to elements of the MBCAP protocol that are shown on the board panel. */

public class MBCAPGUIParam {

	// List of the predicted positions of each UAV in UTM coordinates
	public static AtomicReferenceArray<List<Point3D>> predictedLocation;

	// Parameters needed to draw the warning image when a collision risk is detected
	public static final String EXCLAMATION_IMAGE_PATH = "/files/Exclamation.png";	// Warning image file path
	public static BufferedImage exclamationImage;									// Warning image
	public static final double EXCLAMATION_PX_SIZE = 25.0;							// (px) Size of the image when it is drawn
	public static double exclamationDrawScale;										// Scale needed to draw the image

	// Parameters that store the point where the collision risk image has to be drawn
	public static Point3D[][] impactLocationUTM;
	public static Point3D[][] impactLocationPX;

	// Auxiliary variable needed to ensure that the message thrown when the UAV gets to the end is shown only once
	public static boolean[] lastWaypointReached;

}
