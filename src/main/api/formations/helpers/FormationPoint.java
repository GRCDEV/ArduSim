package main.api.formations.helpers;

/** 
 * Each of the points included in a flight formation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationPoint {
	
	public int position;
	public double offsetX;
	public double offsetY;
	
	@SuppressWarnings("unused")
	private FormationPoint() {}
	
	/**
	 * Build an object for a point in the formation.
	 * @param position Position of the UAV in the formation.
	 * @param offsetX Offset of the UAV in x axes from the center UAV.
	 * @param offsetY Offset of the UAV in y axes from the center UAV.
	 */
	public FormationPoint(int position, double offsetX, double offsetY) {
		this.position = position;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
}