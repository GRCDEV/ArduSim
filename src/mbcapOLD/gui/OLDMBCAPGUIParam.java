package mbcapOLD.gui;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import api.pojo.Point3D;

/** This class contains parameters related to elements of the MBCAP protocol that are shown on the board panel. */

public class OLDMBCAPGUIParam {

	// List of the predicted positions of each UAV in UTM coordinates
	public static AtomicReferenceArray<List<Point3D>> predictedLocation;

	// Parameters needed to draw the warning image when a collision risk is detected
	public static final String EXCLAMATION_IMAGE_PATH = "/files/Exclamation.png";	// Warning image file path
	public static BufferedImage exclamationImage;									// Warning image
	public static final double EXCLAMATION_PX_SIZE = 25.0;							// (px) Size of the image when it is drawn
	public static double exclamationDrawScale;										// Scale needed to draw the image

}
