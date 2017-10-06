package sim.board;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import api.GUIHelper;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import sim.board.pojo.MercatorProjection;

public class BackgroundMap {

	public Image img;					// Downloaded image
	public UTMCoordinates originUTM;	// Upper-left corner UTM coordinates
	public double xScale, yScale;		// px/UTM scale
	public double alfa;				// (rad) Mercator-UTM turn angle
	public double centerX, centerY;		// UTM coordinates of the center of the image
	

	@SuppressWarnings("unused")
	private BackgroundMap() {
	}

	/**
	 * Retrieves a geopositioned image from Google Static Maps based on:
	 *   Latitude and longitude of the center, zoom level, and UTM width-height to be filled.
	 *   Also stores the image center in UTM coordinates
	 *   Returns with attribute img==null if the image could not be downloaded.
	 */
	public BackgroundMap(double latitude, double longitude, int zoom, int pxWidth, int pxHeight, double UTMx, double UTMy) {

		this.centerX = UTMx;
		this.centerY = UTMy;

		MercatorProjection projection = new MercatorProjection(latitude, longitude, pxWidth, pxHeight, Math.pow(2, zoom));
		GeoCoordinates upLeft = projection.getGeoLocation(0, 0);

		UTMCoordinates upLeftUTM = GUIHelper.geoToUTM(upLeft.latitude, upLeft.longitude);
		this.originUTM = upLeftUTM;

		GeoCoordinates upRight = projection.getGeoLocation(pxWidth, 0);
		UTMCoordinates upRightUTM = GUIHelper.geoToUTM(upRight.latitude, upRight.longitude);
		GeoCoordinates bottomRight = projection.getGeoLocation(pxWidth, pxHeight);
		UTMCoordinates bottomRightUTM = GUIHelper.geoToUTM(bottomRight.latitude, bottomRight.longitude);

		double incHorizontal = new Point2D.Double(upRightUTM.Easting, upRightUTM.Northing).distance(upLeftUTM.Easting,
				upLeftUTM.Northing);
		double incVertical = new Point2D.Double(upRightUTM.Easting, upRightUTM.Northing).distance(bottomRightUTM.Easting,
				bottomRightUTM.Northing);
		this.xScale = (incHorizontal * BoardParam.screenScale) / pxWidth;
		this.yScale = (incVertical * BoardParam.screenScale) / pxHeight;
		this.alfa = Math.acos((upRightUTM.Easting - upLeftUTM.Easting) / incHorizontal);
		try {
			URL imagen = new URL("http://maps.googleapis.com/maps/api/staticmap?center=" + latitude + "," + longitude
					+ "&zoom=" + zoom + "&size=" + pxWidth + "x" + pxHeight + "&maptype=satellite");
			BufferedImage im = ImageIO.read(imagen);
			if (im != null) {

				BufferedImage imgDest = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_RGB);

				Graphics g = imgDest.getGraphics();
				g.drawImage(im, 0, 0, null);

				LookupTable it = new ShortLookupTable(0, BoardParam.brightness);
				LookupOp lop = new LookupOp(it, null);
				lop.filter(imgDest, imgDest);

				this.img = Toolkit.getDefaultToolkit().createImage(imgDest.getSource());
			}

		} catch (IOException e) {
		}
	}
}
