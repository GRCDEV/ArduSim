package followme.logic;

import java.awt.geom.Point2D;

public class Formacion {

	private Formacion() {
		// TODO Auto-generated constructor stub
	}
	
	public static Point2D.Double getOffsetMatrix(int posFormation, int numSlaves) {
		int posMasterLineal = (numSlaves + 1) / 2;
		int l = (int)Math.ceil(Math.sqrt(numSlaves + 1));
		
		int rowsBeforeMaster = posMasterLineal / l;
		int columnsBeforeMaster = posMasterLineal - rowsBeforeMaster * l;
		
		int posMasterX = columnsBeforeMaster;
		int posMasterY = rowsBeforeMaster;
		
		if (posFormation >= posMasterLineal) {
			posFormation++;
		}
		
		int rowsBeforeSlave = posFormation / l;
		int columnsBeforeSlave = posFormation - rowsBeforeSlave * l;
		
		int posSlaveX = columnsBeforeSlave;
		int posSlaveY = rowsBeforeSlave;
		
		double distX = (posSlaveX - posMasterX) * FollowMeParam.DistanciaSeparacionHorizontal;
		double distY = (posSlaveY - posMasterY) * FollowMeParam.DistanciaSeparacionHorizontal;
		
		return new Point2D.Double(distX, distY);
	}
	
	public static Point2D.Double getOffsetLineal(int posFormation) {
		double vert = -FollowMeParam.DistanciaSeparacionVertical;
		double horiz = FollowMeParam.DistanciaSeparacionHorizontal * ((int) (posFormation + 2) / 2);
		if (posFormation % 2 == 0) {
			return new Point2D.Double(horiz, vert);
		} else {
			return new Point2D.Double(-horiz, vert);
		}
	}

//	public static Resultado getPosition(int type, GeoCoordinates geoMaster, double headingMaster,
//			int posFormacion, int numSlaves) {
//		Resultado res = null;
//
//		switch (type) {
//		case FollowMeParam.FormacionLinea:
//			res = getPositionLinea(geoMaster, headingMaster, posFormacion);
//			break;
//		case FollowMeParam.FormacionMatriz:
//			res = getPositionMatriz(geoMaster, headingMaster, posFormacion, numSlaves);
//			break;
//		case FollowMeParam.FormacionCircular:
//			res = getPositionCircular(geoMaster, headingMaster, posFormacion);
//			break;
//		default:
//			break;
//		}
//
//		return res;
//	}
//
//	private static Resultado getPositionCircular(GeoCoordinates geoMaster, double headingMaster, int posFormacion) {
//		GeoCoordinates geo = geoMaster;
//		double distHorizontal = 0.0003 * ((int) (posFormacion + 2) / 2);
//		double distVertical = 0.000;
//
//		GUI.log("Posformacion: " + posFormacion + ", heading " + headingMaster);
//		if (posFormacion % 2 == 0) {
//			geo.latitude += distVertical * Math.cos(headingMaster) - distHorizontal * Math.sin(headingMaster);
//			geo.longitude += distHorizontal * Math.cos(headingMaster) + distVertical * Math.sin(headingMaster);
//		} else {
//
//			// Mejorar los movimientos del dron 2 y 4 en posiciones impares
//			geo.latitude += -distVertical * Math.cos(headingMaster - Math.PI)
//					- distHorizontal * Math.sin(headingMaster - Math.PI);
//			geo.longitude += distHorizontal * Math.cos(headingMaster - Math.PI)
//					+ -distVertical * Math.sin(headingMaster - Math.PI);
//
//		}
//		return new Resultado(geo, headingMaster);
//
//	}
//
//	private static Resultado getPositionMatriz(GeoCoordinates geoMaster, double headingMaster, int posFormacion,
//			int numMaxUAV) {
//		GeoCoordinates geo = geoMaster;
//		int matriz = (int) Math.sqrt(numMaxUAV) + 1;
//		double distHorizontal = 0.0005 * (((posFormacion + 1) % matriz));
//		double distVertical = -0.0005 * ((int) (posFormacion + 1) / matriz);
//
//		// GUI.log("Distancia Horizontal en metros: " + distanceMeters(new
//		// GeoCoordinates(0.0, 0.0), new GeoCoordinates(distHorizontal, 0.0)));
//		
//		int posV = 0;
//		int posH = 0;
//
//		//GUI.log("Posformacion: " + posFormacion + ", heading " + headingMaster);
//		geo.latitude += distVertical * Math.cos(headingMaster) - distHorizontal * Math.sin(headingMaster);
//		geo.longitude += distHorizontal * Math.cos(headingMaster) + distVertical * Math.sin(headingMaster);
//
//		// if (posFormacion % 2 == 0) {
//		//
//		// geo.latitude += distVertical * Math.cos(headingMaster) - distHorizontal *
//		// Math.sin(headingMaster);
//		// geo.longitude += distHorizontal * Math.cos(headingMaster) + distVertical *
//		// Math.sin(headingMaster);
//		//
//		// } else {
//		// // Mejorar los movimientos del dron 2 y 4 en posiciones impares
//		//
//		// geo.latitude += -distVertical * Math.cos(headingMaster - Math.PI)
//		// - distHorizontal * Math.sin(headingMaster - Math.PI);
//		// geo.longitude += distHorizontal * Math.cos(headingMaster - Math.PI)
//		// + -distVertical * Math.sin(headingMaster - Math.PI);
//		// }
//		return new Resultado(geo, headingMaster);
//
//	}
//	
//	
//
//	private static Resultado getPositionLinea(GeoCoordinates geoMaster, double headingMaster, int posFormacion) {
//		GeoCoordinates geo = geoMaster;
//		// GeoCoordinates geoDis =
//		// Tools.UTMToGeo(FollowMeParam.DistanciaSeparacionHorizontal,
//		// FollowMeParam.DistanciaSeparacionVertical);
//		// UTMCoordinates utmDis = Tools.geoToUTM(geoDis.latitude, geoDis.longitude);
//		// GUI.log("Distancia Vertical: "+FollowMeParam.DistanciaSeparacionVertical+"
//		// GeoDis.lat: "+geoDis.latitude+" UTMDis.North: "+utmDis.Northing);
//		// GUI.log("Distancia Horizontal:
//		// "+FollowMeParam.DistanciaSeparacionHorizontal+" GeoDis.lon:
//		// "+geoDis.longitude+" UTMDis.East: "+utmDis.Easting);
//
//		double distHorizontal = 0.0005 * ((int) (posFormacion + 2) / 2);
//		double distVertical = 0.000;
//
//		GUI.log("Posformacion: " + posFormacion + ", heading " + headingMaster);
//		if (posFormacion % 2 == 0) {
//			geo.latitude += distVertical * Math.cos(headingMaster) - distHorizontal * Math.sin(headingMaster);
//			geo.longitude += distHorizontal * Math.cos(headingMaster) + distVertical * Math.sin(headingMaster);
//		} else {
//
//			// Mejorar los movimientos del dron 2 y 4 en posiciones impares
//			geo.latitude += -distVertical * Math.cos(headingMaster - Math.PI)
//					- distHorizontal * Math.sin(headingMaster - Math.PI);
//			geo.longitude += distHorizontal * Math.cos(headingMaster - Math.PI)
//					+ -distVertical * Math.sin(headingMaster - Math.PI);
//
//		}
//		return new Resultado(geo, headingMaster);
//	}
//	
//	
//
//	public static double getSpeedX(GeoCoordinates geoFin, GeoCoordinates geoAhora, int pos, Double speedX) {
//		double difX = distanceMeters(geoAhora, geoFin);
//		double vel = speedX;
//		// SwarmHelper.log("Follower " + pos + " distancia " + difX);
//		return vel;
//	}
//
//	public static double getSpeedY(GeoCoordinates geoFin, GeoCoordinates geoAhora, int pos, Double speedY) {
//		double difY = geoFin.longitude - geoAhora.longitude;
//		double vel = speedY + difY * 1000;
//		// SwarmHelper.log("Follower "+pos+" speedY "+speedY+" final "+vel);
//		return vel;
//	}
//
//	public static double distanceMeters(GeoCoordinates geo1, GeoCoordinates geo2) {
//		double R = 6378.137; // Radius of earth in KM
//		double dLat = geo2.latitude * Math.PI / 180 - geo1.latitude * Math.PI / 180;
//		double dLon = geo2.longitude * Math.PI / 180 - geo1.longitude * Math.PI / 180;
//		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(geo1.latitude * Math.PI / 180)
//				* Math.cos(geo2.latitude * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
//		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//		double d = R * c;
//		return d * 1000; // meters
//	}

}