package followme.logic;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import followme.logic.FollowMeParam.FollowMeState;
import followme.logic.comunication.FollowMeThreadRecive;
import followme.logic.comunication.FollowMeThreadSend;
import followme.logic.comunication.Message;
import followme.logic.comunication.MessageStorage;
import main.Param;
import main.Param.SimulatorState;
import uavController.UAVParam;

public class MasterThread extends Thread {

	private int Master;
	private MessageStorage msgStorage;
	private boolean allDetectedId;
	private Set<Integer> idFollowers = new HashSet<Integer>();
	private Set<Integer> alturasFollowers;
	private Map<Integer, Integer> posFollowers;

	public MasterThread() {
		this.Master = 0;
		this.msgStorage = new MessageStorage();
		this.allDetectedId = false;
		this.alturasFollowers = new HashSet<Integer>();
		this.posFollowers = new HashMap<Integer, Integer>();
	}

	@Override
	public void run() {

//		SwarmHelper.log("Iniciando Master");
//		SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		if (FollowMeParam.uavs[this.Master] == FollowMeState.START)
//			FollowMeParam.uavs[this.Master] = FollowMeState.LISTEN_ID; // Si el Master se encuentra en el estado Start
//																		// pasar a Listen_Id
//		SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//
//		FollowMeThreadRecive thRecive = new FollowMeThreadRecive(this.Master, this.msgStorage);
//		thRecive.start(); // Iniciar thread recibe mensajes
//		FollowMeThreadSend thSend;
//
//		// Lectura de mensajes recibidos
//		while (FollowMeParam.uavs[this.Master] == FollowMeState.LISTEN_ID) {
//			// System.out.println("MsgStorage Antes: "+msgStorage.size());
//			waitSizeNotZero();
//			Message m = msgStorage.pop();
//			// System.out.println("MsgStorage Despues: "+msgStorage.size());
//			SwarmHelper.log("Master: message " + m.toString());
//			if (m.type == FollowMeParam.EnvioId) {
//				idFollowers.add(m.idSender); // Añade id a la lista de IDs
//			} else {
//				msgStorage.put(m);
//				System.err.println("Msg no toca: " + m.toString());
//			}
//
//			SwarmHelper.log("Numero de UAVs " + Param.numUAVs + " idfollowers: " + idFollowers.toString());
//			// Simulacion
//			if (idFollowers.size() >= (Param.numUAVs - 1)) {
//				// Enviar pos Formacion
//
//				Integer[] posiciones = idFollowers.toArray(new Integer[idFollowers.size()]);
//
//				int[] pos = new int[posiciones.length];
//				for (int i = 0; i < posiciones.length; i++) {
//					pos[i] = posiciones[i];
//				}
//				Message msg = new Message(pos);
//				thSend = new FollowMeThreadSend(Master, msg);
//				thSend.start();
//
//				FollowMeParam.uavs[this.Master] = FollowMeState.WAIT_TAKEOFF; // Al obtener todos los ids, pasa a
//																				// esperar que despeguen el resto de
//																				// followers
//				SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//
//			}
//			// Real por implementar
//
//			// SwarmHelper.log("Master State:"+ FollowMeParam.uavs[this.Master]);
//
//		}
//
//		SwarmHelper.log("Master State:" + FollowMeParam.uavs[this.Master]);
//
//		while (FollowMeParam.uavs[this.Master] == FollowMeState.WAIT_TAKEOFF) {
//			waitSizeNotZero();
//			Message m = msgStorage.pop();
//			if (m.type == FollowMeParam.EnvioAlturaTakeOff) {
//				alturasFollowers.add(m.idSender);
//			} else {
//				msgStorage.put(m);
//				System.err.println("Msg no toca: " + m.toString());
//			}
//
//			// SwarmHelper.log("Size -> alturas: "+alturasFollowers.size()+", ids:
//			// "+idFollowers.size());
//			if (alturasFollowers.size() == idFollowers.size()) {
//				FollowMeParam.uavs[this.Master] = FollowMeState.READY;
//				SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//			}
//
//			// SwarmHelper.log("Master State: "+ FollowMeParam.uavs[this.Master]);
//		}
//		msgStorage = null;
//
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		boolean vuelo = false;
//		while (FollowMeParam.uavs[this.Master] == FollowMeState.READY) {
//			Point2D.Double geo = UAVParam.uavCurrentData[this.Master].getGeoLocation();
//			double zRel = UAVParam.uavCurrentData[this.Master].getZRelative();
//			Mode flyMode = UAVParam.flightMode.get(Master);
//			double heading = UAVParam.uavCurrentData[this.Master].getHeading();
//			SwarmHelper.log("Heading Masterº: " + (heading * 180 / Math.PI));
//			Message m = new Message(geo.y, geo.x, zRel, flyMode.getBaseMode(), flyMode.getCustomMode(), heading);
//			thSend = new FollowMeThreadSend(Master, m);
//			thSend.start();
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			if (zRel > 0.5)
//				vuelo = true;
//			if ((UAVParam.flightMode.get(Master) == Mode.LAND || UAVParam.flightMode.get(Master) == Mode.LAND_ARMED)
//					&& vuelo) {
//				FollowMeParam.uavs[this.Master] = FollowMeState.LANDING_MASTER;
//				SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//			}
//			// if(flyMode == Mode.LAND || flyMode == Mode.LAND_ARMED) {
//			SwarmHelper.log("Master Mode: " + flyMode);
//			// }
//
//		}
//
//		while (FollowMeParam.uavs[this.Master] == FollowMeState.LANDING_MASTER) {
//			Point2D.Double geo = UAVParam.uavCurrentData[this.Master].getGeoLocation();
//			Message msg = new Message(geo.y, geo.x);
//			thSend = new FollowMeThreadSend(Master, msg);
//			thSend.start();
//			FollowMeParam.uavs[this.Master] = FollowMeState.FINISH;
//			SwarmHelper.setSwarmState(this.Master, FollowMeParam.uavs[this.Master].toString());
//		}
//
//		/*
//		 * 
//		 * byte[] buffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH]; Output out = new
//		 * Output(buffer);
//		 * 
//		 * int count = 0;
//		 * 
//		 * while (true) { try { Thread.sleep(1000); } catch (InterruptedException e) {
//		 * // TODO Auto-generated catch block e.printStackTrace(); } int idUAV = 0;
//		 * double latitud; double longitud; double z;
//		 * 
//		 * latitud = UAVParam.uavCurrentData[0].getGeoLocation().getX(); longitud =
//		 * UAVParam.uavCurrentData[0].getGeoLocation().getY(); z =
//		 * UAVParam.uavCurrentData[0].getZ();
//		 * 
//		 * out.writeInt(idUAV); out.writeDouble(latitud); out.writeDouble(longitud);
//		 * out.writeDouble(z); out.flush(); byte[] message = Arrays.copyOf(buffer,
//		 * out.position()); System.out.println("Enviado" + idUAV + "-->\t" + latitud +
//		 * "\t" + longitud + "\t" + z); API.sendBroadcastMessage(0, message);
//		 * out.clear(); }
//		 */

	}

	public static boolean areAllTrue(boolean[] array) {
		for (boolean b : array)
			if (!b)
				return false;
		return true;
	}

	public void waitSizeNotZero() {
		while (msgStorage.size() == 0) {
			try {
				// System.out.println("MsgStorage Wait: "+msgStorage.size());
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
