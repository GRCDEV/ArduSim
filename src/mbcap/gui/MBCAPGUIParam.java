package mbcap.gui;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import es.upv.grc.mapper.DrawableCirclesGeo;

/** This class contains parameters related to elements of the MBCAP protocol that are shown on the board panel.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MBCAPGUIParam {

	// List of the predicted positions of each UAV in Geographic coordinates
	public static AtomicReference<DrawableCirclesGeo>[] predictedLocation;

	// Parameters needed to draw the warning image when a collision risk is detected
	public static final String EXCLAMATION_IMAGE_PATH = "/resources/mbcap/Exclamation.png";	// Warning image file path
	public static BufferedImage exclamationImage;								// Warning image
	public static final int EXCLAMATION_PX_SIZE = 25;							// (px) Size of the image when it is drawn

}
