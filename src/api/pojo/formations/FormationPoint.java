package api.pojo.formations;

public class FormationPoint {
	
	public int position;
	public double offsetX;
	public double offsetY;
	
	@SuppressWarnings("unused")
	private FormationPoint() {}
	
	/** Builds an object for a point in the formation. */
	public FormationPoint(int position, double offsetX, double offsetY) {
		this.position = position;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
}
