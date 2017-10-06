package api.pojo;

public class UTMCoordinates {

  public double Easting;
  public double Northing;
  public int Zone;
  public char Letter;

  @SuppressWarnings("unused")
  private UTMCoordinates() {
  }

  public UTMCoordinates(double x, double y, int zone, char letter) {
    this.Easting = x;
    this.Northing = y;
    this.Zone = zone;
    this.Letter = letter;
  }
}
