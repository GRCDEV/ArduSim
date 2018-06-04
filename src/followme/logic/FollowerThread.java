package followme.logic;

import api.pojo.GeoCoordinates;
import followme.logic.comunication.MessageStorage;

public class FollowerThread extends Thread {

	private int numUAV;
	private MessageStorage msgStrg;
	private GeoCoordinates geoLanding;
	private int[] posFormacion;
	private int myPos;

	@SuppressWarnings("unused")
	private FollowerThread() {
	}

	public FollowerThread(int numUAV) {
		this.numUAV = numUAV;
//		this.idDetectados = new HashSet<Integer>();
		this.msgStrg = new MessageStorage();
	}
//
//	@Override
//	public void run() {
//
//		SwarmHelper.log("Iniciando Follower " + numUAV);
//		SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//		// SwarmHelper.log("Follower "+numUAV+" State:"+
//		// FollowMeParam.uavs[this.numUAV]);
//		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		if (FollowMeParam.uavs[numUAV] == FollowMeState.START) {
//			FollowMeParam.uavs[numUAV] = FollowMeState.SEND_ID; // Si un follower se encuentra en modo START pasa a
//																// estar en SEND_ID
//			SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//		}
//
//		Message msg = new Message(FollowMeParam.EnvioId, FollowMeParam.posMaster, numUAV, numUAV + "");
//		FollowMeThreadSend thSend = new FollowMeThreadSend(numUAV, msg);
//		thSend.start();
//		FollowMeThreadRecive thRecive = new FollowMeThreadRecive(numUAV, msgStrg);
//		thRecive.start();
//
//		boolean posRecibe = false;
//		while (!posRecibe) {
//			waitSizeNotZero();
//			Message m = msgStrg.pop();
//			if (m.type == FollowMeParam.EnvioPosFormacion) {
//				this.posFormacion = m.posFormacion;
//				for (int i = 0; i < this.posFormacion.length; i++) {
//					if (posFormacion[i] == numUAV) {
//						myPos = i;
//					}
//				}
//				posRecibe = true;
//				SwarmHelper.log("Follower: " + numUAV + " posicion en formacion: " + myPos);
//			}
//		}
//		// SwarmHelper.log("Follower "+numUAV+" State:"+
//		// FollowMeParam.uavs[this.numUAV]);
//
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		SwarmHelper.log("Follower " + numUAV + " State:" + FollowMeParam.uavs[this.numUAV]);
//
//		if (FollowMeParam.uavs[numUAV] == FollowMeState.SEND_ID) {
//			FollowMeParam.uavs[numUAV] = FollowMeState.TAKE_OFF; // Si un follower se encuentra en modo SEND_ID pasa a
//																	// TAKE_OFF
//			SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//		}
//		// SwarmHelper.log("Follower "+numUAV+" State:"+
//		// FollowMeParam.uavs[this.numUAV]);
//
//		takeOffIndividual(numUAV, FollowMeParam.AlturaInitFollowers);
//		double altura;
//		do {
//			altura = UAVParam.uavCurrentData[numUAV].getZRelative();
//		} while (altura < (FollowMeParam.AlturaInitFollowers * 0.98));
//		msg = new Message(0, numUAV, altura);
//
//		thSend = new FollowMeThreadSend(numUAV, msg);
//		thSend.start();
//
//		if (FollowMeParam.uavs[numUAV] == FollowMeState.TAKE_OFF) {
//			FollowMeParam.uavs[numUAV] = FollowMeState.FOLLOW; // Si un follower se encuentra en modo SEND_ID pasa a
//																// TAKE_OFF
//			SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//		}
//
//		while (FollowMeParam.uavs[numUAV] == FollowMeState.FOLLOW) {
//			waitSizeNotZero();
//			Message m = msgStrg.pop();
//			if (m.type == FollowMeParam.EnvioCoordenadasMaster) {
//				GeoCoordinates geo = new GeoCoordinates(m.latitud, m.longitud);
//				// API.moveUAV(numUAV, geo , (float) m.altura, 0.95, 0.95);
//				try {
//					// SwarmHelper.log("Follower " + numUAV + " ");
//
//					// Formacion
//					Point2D.Double myGeo = UAVParam.uavCurrentData[numUAV].getGeoLocation();
//					Resultado r = Formacion.getPosition(
//									FollowMeParam.FormacionLineaHorizontal,
//									new GeoCoordinates(m.latitud, m.longitud), 
//									m.heading,
//									new GeoCoordinates(myGeo.getY(), myGeo.getX()), 
//									myPos,
//									UAVParam.uavCurrentData[numUAV].getSpeeds());
//
//					int mode = FollowMeParam.TypemsgTargetCoordinates;
//					if(r.speed) {
//						mode = FollowMeParam.TypemsgTargetCoordinatesAndSpeeds;
//					}
//					Param.controllers[numUAV].msgTarget(
//							mode, 
//							r.getGeo().latitude, 
//							r.getGeo().longitude, 
//							m.altura, 
//							1.0,
//							false, 
//							r.getSpeedX(), 
//							r.getSpeedY(), 
//							r.getSpeedZ());
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			} else if (m.type == FollowMeParam.EnvioLandingMaster) {
//				FollowMeParam.uavs[numUAV] = FollowMeState.LANDING_FOLLOWERS; // Recebir coordenadas de aterrizar
//				SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//				geoLanding = new GeoCoordinates(m.latitud, m.longitud);
//
//			}
//		}
//
//		while (FollowMeParam.uavs[numUAV] == FollowMeState.LANDING_FOLLOWERS) {
//			try {
//				Param.controllers[numUAV].msgTarget(1, geoLanding.latitude, geoLanding.longitude, 0.0, 1.0, false, 1.0,
//						1.0, 1.0);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			double alture = UAVParam.uavCurrentData[numUAV].getZRelative();
//			if (alture <= 0.2) {
//				FollowMeParam.uavs[numUAV] = FollowMeState.FINISH; // Recebir coordenadas de aterrizar
//				SwarmHelper.setSwarmState(numUAV, FollowMeParam.uavs[numUAV].toString());
//			}
//		}
//
//		// Pasar a despegar
//
//		/*
//		 * API.setMode(numUAV, Mode.GUIDED); boolean fly = false; while (true) { message
//		 * = API.receiveMessage(numUAV); in.setBuffer(message); int num = in.readInt();
//		 * double latitud = in.readDouble(); double longitud = in.readDouble(); double z
//		 * = in.readDouble(); System.out.println("Recibido" + numUAV + "-->UAV" + num +
//		 * "\t" + latitud + "\t" + longitud + "\t" + z); if (z > 2.0 && !fly) { fly =
//		 * true; } GeoCoordinates geo = new GeoCoordinates(latitud, longitud); float
//		 * relAltitude = (float) z;
//		 * 
//		 * 
//		 * if (fly) { Boolean ok = API.moveUAV(numUAV, geo, relAltitude, 0.1, 1.0); //
//		 * Param.controllers[numUAV].msgTarget(mode, latitude, longitude, altitude, yaw,
//		 * setYaw, speedX, speedY, speedZ); System.out.println("MoveUAV" +
//		 * numUAV+" "+ok); } }
//		 */
//
//	}
//
//	/**
//	 * Manually takes off all the UAVs: executes mode=guided, arm engines, and then
//	 * the take off. Blocking method.
//	 * <p>
//	 * Usefull when starting on the ground, on stabilize flight mode
//	 * <p>
//	 * Do not modify this method.
//	 */
//	public static void takeOffIndividual(int numUAV, double altitude) {
//		// Taking Off to first altitude step
//		if (!API.setMode(numUAV, UAVParam.Mode.GUIDED) || !API.armEngines(numUAV) || !API.doTakeOff(numUAV, altitude)) {
//			GUIHelper.exit(Text.TAKE_OFF_ERROR_1 + " " + Param.id[numUAV]);
//		}
//
//		// The application must wait until all UAVs reach the planned altitude
//		while (UAVParam.uavCurrentData[numUAV].getZRelative() < 0.95 * altitude) {
//			if (Param.VERBOSE_LOGGING) {
//				MissionHelper.log(SimParam.prefix[numUAV] + Text.ALTITUDE_TEXT + " = "
//						+ String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ()) + " " + Text.METERS);
//			}
//
//			GUIHelper.waiting(UAVParam.ALTITUDE_WAIT);
//		}
//	}
//
//	public void waitSizeNotZero() {
//		while (msgStrg.size() == 0) {
//			try {
//				// System.out.println("MsgStorage Wait: "+msgStorage.size());
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
}
