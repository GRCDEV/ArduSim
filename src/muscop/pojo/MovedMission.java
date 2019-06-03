package muscop.pojo;

import api.pojo.location.Location3DUTM;

/** 
 * Mission used for each of the UAVs.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MovedMission implements Comparable<MovedMission> {

	public long id;
	public int formationPosition;
	public Location3DUTM[] locations;
	
	@SuppressWarnings("unused")
	private MovedMission() {
		
	}
	
	public MovedMission(long id, int formationPosition, Location3DUTM[] locations) {
		this.id=id;
		this.formationPosition = formationPosition;
		this.locations = locations;
	}
	
	/**
	 * Two displaced missions are equals if they are related to the same position in the flight formation.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !(obj instanceof MovedMission)) {
			return false;
		}
		return this.formationPosition == ((MovedMission)obj).formationPosition;
	}
	
	@Override
	public int hashCode() {
		return Integer.hashCode(this.formationPosition);
	}

	@Override
	public int compareTo(MovedMission o) {
		return this.formationPosition - o.formationPosition;
	}
	
}
