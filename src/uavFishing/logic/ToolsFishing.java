package uavFishing.logic;

import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;

public class ToolsFishing {
	
	/** Transforms UTM coordinates to Geographic coordinates. 
	 *  <p>Example: Tools.UTMToGeo(312915.84, 4451481.33).
	 *  <p>It is assumed that this function is used when at least one coordinate set is received from the UAV, in order to get the zone and the letter of the UTM projection, available on GUIParam.zone and GUIParam.letter. */
	public static GeoCoordinates UTMToGeo(UTMCoordinates utmCoord) {
		return Tools.UTMToGeo(utmCoord.x,utmCoord.y);
	}

	/** Transforms Geographic coordinates to UTM coordinates. */
	public static UTMCoordinates geoToUTM(GeoCoordinates geoCoord) {
		return Tools.geoToUTM(geoCoord.latitude, geoCoord.longitude);
	}
	
	
}
