package followme.logic;

import java.util.List;

import org.javatuples.Triplet;

import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import uavController.UAVParam;

public class Formacion {

	private int type;
	private int numUAVs;
	private int idMaster;
	private List<Integer> ordenFollowers;

	public Formacion() {
		// TODO Auto-generated constructor stub
	}

	public static Resultado getPosition(int type, GeoCoordinates geoMaster, double headingMaster,
			GeoCoordinates geoFollow, int posFormacion, Triplet<Double, Double, Double> triplet) {
		Resultado res = null;

		switch (type) {
		case FollowMeParam.FormacionLineaHorizontal:
			res = getPositionLineaH(geoMaster, headingMaster, posFormacion);
			break;
		case FollowMeParam.FormacionLineaVertical:
			res = getPositionLineaV(geoMaster, headingMaster, posFormacion);
			break;
		case FollowMeParam.FormacionCircular:
			res = getPositionCircular(geoMaster, headingMaster, posFormacion);
			break;
		default:
			break;
		}

		return res;
	}

	private static Resultado getPositionCircular(GeoCoordinates geoMaster, double headingMaster, int posFormacion) {
		GeoCoordinates geo = geoMaster;
		double distanciaEntreDrones = 0.0003;
		double sinheading = Math.sin(headingMaster);
		double cosheading = Math.cos(headingMaster);

		if (posFormacion == 0) {
			SwarmHelper.log("SinH: " + sinheading + " CosH: " + cosheading);
		}

		geo.latitude += cosheading * distanciaEntreDrones * (posFormacion + 1);
		geo.longitude += sinheading * distanciaEntreDrones * (1 + posFormacion);
		// SwarmHelper.log("HeadingMaster: "+ (headingMaster*180/Math.PI));
		return new Resultado(geo, headingMaster);
	}

	private static Resultado getPositionLineaV(GeoCoordinates geoMaster, double headingMaster, int posFormacion) {
		GeoCoordinates geo = geoMaster;
		double distanciaEntreDrones = 0.0003;
		double sinheading = Math.sin(headingMaster);
		double cosheading = Math.cos(headingMaster);

		if (posFormacion == 0) {
			SwarmHelper.log("SinH: " + sinheading + " CosH: " + cosheading);
		}

		geo.latitude += cosheading * distanciaEntreDrones * (posFormacion + 1);
		geo.longitude += sinheading * distanciaEntreDrones * (1 + posFormacion);
		// SwarmHelper.log("HeadingMaster: "+ (headingMaster*180/Math.PI));
		return new Resultado(geo, headingMaster);
	}

	private static Resultado getPositionLineaH(GeoCoordinates geoMaster, double headingMaster, int posFormacion) {
		GeoCoordinates geo = geoMaster;
		double distHorizontal = 0.001;
		double distVertical = 0.0005;
		double sinheading = Math.sin(headingMaster);
		double cosheading = Math.cos(headingMaster);

		if (posFormacion == 0) {
			SwarmHelper.log("SinH: " + sinheading + " CosH: " + cosheading);
		}
//		Mejorar
		if (posFormacion % 2 == 0) {
			geo.latitude += sinheading * distHorizontal * ((int) (posFormacion + 1) / 2) - cosheading * distVertical;
			geo.longitude += cosheading * distHorizontal * ((int) (posFormacion + 1) / 2) - sinheading * distVertical;
		} else {
			geo.latitude += sinheading * distHorizontal * ((int) (posFormacion + 1) / 2) - cosheading * distVertical;
			geo.longitude -= cosheading * distHorizontal * ((int) (posFormacion + 1) / 2) - sinheading * distVertical;
		}
		// SwarmHelper.log("HeadingMaster: "+ (headingMaster*180/Math.PI));
		return new Resultado(geo, headingMaster);
	}

	private static double getSpeedX(GeoCoordinates geoFin, GeoCoordinates geoAhora, int pos, Double speedX) {
		double difX = distanceMeters(geoAhora, geoFin);
		double vel = speedX;
		// SwarmHelper.log("Follower " + pos + " distancia " + difX);
		return vel;
	}

	private static double getSpeedY(GeoCoordinates geoFin, GeoCoordinates geoAhora, int pos, Double speedY) {
		double difY = geoFin.longitude - geoAhora.longitude;
		double vel = speedY + difY * 1000;
		// SwarmHelper.log("Follower "+pos+" speedY "+speedY+" final "+vel);
		return vel;
	}

	private static double distanceMeters(GeoCoordinates geo1, GeoCoordinates geo2) {
		double R = 6378.137; // Radius of earth in KM
		double dLat = geo2.latitude * Math.PI / 180 - geo1.latitude * Math.PI / 180;
		double dLon = geo2.longitude * Math.PI / 180 - geo1.longitude * Math.PI / 180;
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(geo1.latitude * Math.PI / 180)
				* Math.cos(geo2.latitude * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return d * 1000; // meters
	}

}
