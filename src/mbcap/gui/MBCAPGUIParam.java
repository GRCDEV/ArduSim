package mbcap.gui;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import api.pojo.location.Location3DUTM;

/** This class contains parameters related to elements of the MBCAP protocol that are shown on the board panel.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPGUIParam {

	// List of the predicted positions of each UAV in UTM coordinates
	public static AtomicReference<List<Location3DUTM>>[] predictedLocation;

	// Parameters needed to draw the warning image when a collision risk is detected
	public static final String EXCLAMATION_IMAGE_PATH = "/resources/mbcap/Exclamation.png";	// Warning image file path
	public static BufferedImage exclamationImage;									// Warning image
	public static final double EXCLAMATION_PX_SIZE = 25.0;							// (px) Size of the image when it is drawn
	public static double exclamationDrawScale;										// Scale needed to draw the image

}
