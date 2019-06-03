package main.sim.board.pojo;

import java.awt.geom.Point2D;

import api.pojo.location.Location2DGeo;

/** This class generates a Mercator projection object that allows to transform between screen and Geographic coordinates.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MercatorProjection {

  private static final double DEFAULT_PROJECTION_WIDTH = 256;
  private static final double DEFAULT_PROJECTION_HEIGHT = 256;

  private double centerLatitude;
  private double centerLongitude;
  private int areaWidthPx;
  private int areaHeightPx;
  // the scale that we would need for the a projection to fit the given area
  // into a world view (1 = global, expect it to be > 1)
  private double areaScale;

  private double projectionWidth;
  private double projectionHeight;
  private double pixelsPerLonDegree;
  private double pixelsPerLonRadian;

  private double projectionCenterPx;
  private double projectionCenterPy;

  public MercatorProjection(double centerLatitude, double centerLongitude, int areaWidthPx, int areaHeightPx,
      double areaScale) {
    this.centerLatitude = centerLatitude;
    this.centerLongitude = centerLongitude;
    this.areaWidthPx = areaWidthPx;
    this.areaHeightPx = areaHeightPx;
    this.areaScale = areaScale;

    this.projectionWidth = DEFAULT_PROJECTION_WIDTH;
    this.projectionHeight = DEFAULT_PROJECTION_HEIGHT;
    this.pixelsPerLonDegree = this.projectionWidth / 360;
    this.pixelsPerLonRadian = this.projectionWidth / (2 * Math.PI);

    Point2D.Double centerPoint = projectLocation(this.centerLatitude, this.centerLongitude);
    this.projectionCenterPx = centerPoint.x * this.areaScale;
    this.projectionCenterPy = centerPoint.y * this.areaScale;
  }

  /** Gets the Mercator projection coordinates of a screen point (pixels). */
  public Location2DGeo getGeoLocation(int px, int py) {
    double x = this.projectionCenterPx + (px - this.areaWidthPx / 2);
    double y = this.projectionCenterPy + (py - this.areaHeightPx / 2);

    return projectPX(x / this.areaScale, y / this.areaScale);
  }

  // from
  // http://stackoverflow.com/questions/12507274/how-to-get-bounds-of-a-google-static-map
  /** Gets the Mercator projection coordinates of a screen point (pixels). */
  private Location2DGeo projectPX(double px, double py) {
    final double longitude = (px - this.projectionWidth / 2) / this.pixelsPerLonDegree;
    final double latitudeRadians = (py - this.projectionHeight / 2) / -this.pixelsPerLonRadian;
    final double latitude = rad2deg(2 * Math.atan(Math.exp(latitudeRadians)) - Math.PI / 2);
    return new Location2DGeo(latitude, longitude);
  }
  
  /** Gets the screen coordinates (pixels) of a point in Mercator projection (Geographic coordinates). */
  public Point2D.Double getPXLocation(double latitude, double longitude) {
    Point2D.Double point = projectLocation(latitude, longitude);
    double x = (point.x * this.areaScale - this.projectionCenterPx) + this.areaWidthPx / 2;
    double y = (point.y * this.areaScale - this.projectionCenterPy) + this.areaHeightPx / 2;

    return new Point2D.Double(x, y);
  }

  /** Auxiliary method to get the screen location of a geographic coordinates set. */
  private Point2D.Double projectLocation(double latitude, double longitude) {
    double px = this.projectionWidth / 2 + longitude * this.pixelsPerLonDegree;
    double siny = Math.sin(deg2rad(latitude));
    double py = this.projectionHeight / 2 + 0.5 * Math.log((1 + siny) / (1 - siny)) * -this.pixelsPerLonRadian;
    Point2D.Double result = new Point2D.Double(px, py);
    return result;
  }

  private double rad2deg(double rad) {
    return (rad * 180) / Math.PI;
  }

  private double deg2rad(double deg) {
    return (deg * Math.PI) / 180;
  }
}
