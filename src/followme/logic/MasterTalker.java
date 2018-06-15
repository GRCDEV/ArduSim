package followme.logic;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import followme.logic.FollowMeParam.FollowMeState;

public class MasterTalker extends Thread {

	private int idMaster;
	public int contador1 = 0, contador2 = 0;
	private boolean sendTakeOff;

	public MasterTalker(int idMaster) {
		this.idMaster = idMaster;
		sendTakeOff = false;
	}

	@Override
	public void run() {
		GUI.log("MasterTalker run");
		
		while (!Tools.isSetupInProgress()) {
			Tools.waiting(100);
		}
		
		FollowMeParam.uavs[idMaster] = FollowMeState.WAIT_TAKE_OFF_MASTER;
		GUI.updateprotocolState(idMaster, FollowMeParam.uavs[idMaster].getName());
		
		String msg = null;
		byte[] buffer;
		Output out = new Output();
		int[] idsFormacion = new int[FollowMeParam.posFormacion.size()];
		int i = 0;
		for (int p : FollowMeParam.posFormacion.values()) {
			idsFormacion[i++] = p;
		}
		
		while (FollowMeParam.uavs[idMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);
			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgTakeOff);
			out.writeInt(idsFormacion.length);
			out.writeInts(idsFormacion);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();

//			System.out.println("Enviado" + idMaster + "-->\tDespegad todos posiciones: " + Arrays.toString(idsFormacion));

			// Tools.waiting(5000);
//			if(FollowMeParam.uavs[idMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
//				GUI.log("Master envia TAKE OFF!!");
//				FollowMeParam.uavs[idMaster] = FollowMeState.READY_TO_START;
//				GUI.updateprotocolState(idMaster, FollowMeParam.uavs[idMaster].getName());
//			}
			
			Tools.waiting(500);
		}
//		GUI.log("MasterTalker Finaliza");
		
		while (!Tools.isExperimentInProgress()) {
			Tools.waiting(200);
		}
		
		FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.TAKE_OFF;
		GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		
		while (Copter.getZRelative(FollowMeParam.posMaster) < FollowMeParam.AlturaInitSend) {
			Tools.waiting(200);
		}
		
		FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.SENDING;
		GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		
		while (Copter.getFlightMode(idMaster) != FlightMode.LAND_ARMED
				&& Copter.getFlightMode(idMaster) != FlightMode.LAND) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);;
			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgCoordenadas);
			double lat, lon, heading, z, speedX, speedY, speedZ;
			Point2D.Double utm = Copter.getUTMLocation(FollowMeParam.posMaster);
			heading = Copter.getHeading(FollowMeParam.posMaster);
			z = Copter.getZRelative(FollowMeParam.posMaster);
			Triplet<Double, Double, Double> speed = Copter.getSpeeds(FollowMeParam.posMaster);
			speedX = speed.getValue0();
			speedY = speed.getValue1();
			speedZ = speed.getValue2();

			out.writeDouble(utm.x);
			out.writeDouble(utm.y);
			out.writeDouble(heading);
			out.writeDouble(z);
			out.writeDouble(speedX);
			out.writeDouble(speedY);
			out.writeDouble(speedZ);

//			msg = "MsgCoordenadas lat: " + lat + " lon: " + lon + ", heading: " + heading + ", z: " + z + ", speedX: "
//					+ speedX + ", speedY: " + speedY + ", speedZ: " + speedZ;
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
//			System.out.println("Enviado" + idMaster + "-->\t" + msg);
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();
			Tools.waiting(1000);
		}
		
		while (true) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);

			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgLanding);
			msg = "Aterrizad todos";
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
//			System.out.println("Enviado" + idMaster + "-->\t" + msg);
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();
			Tools.waiting(1000);
		}
		
		
		
		
		
		
		
		
		

	}

//	public void run1() {
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.START) {
//			Tools.waiting(1000);
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.LISTEN_ID) {
//			// Esperar
//			Tools.waiting(1000);
//			if (Tools.isSetupFinished()) {
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.WAIT_TAKE_OFF_MASTER;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//			}
//		}
//		// Estado del simulador SETUP
//		// Enviar despegar SLAVEs
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
//			// enviarMessage(FollowMeParam.MsgTakeOff);
//			if (!sendTakeOff) {
//				enviarmessageTakeOff();
//				sendTakeOff = true;
//			} else {
//				Tools.waiting(1000);
//			}
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.READY_TO_START) {
//			// enviarMessage(FollowMeParam.MsgCoordenadas);
//			enviarMessageCoordenadas();
//			Tools.waiting(1000);
//
//			if (Tools.isExperimentInProgress()) {
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.SENDING;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//			}
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.SENDING) {
//			// enviarMessage(FollowMeParam.MsgCoordenadas);
//			enviarMessageCoordenadas();
//			Tools.waiting(1000);
//
//			if (Copter.getFlightMode(idMaster) == FlightMode.LAND_ARMED
//					|| Copter.getFlightMode(idMaster) == FlightMode.LAND) {
//				// GUI.log("Master quiere aterrizar");
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.LANDING_MASTER;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//			}
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.LANDING_MASTER) {
//			// enviarMessage(FollowMeParam.MsgLanding);
//			enviarMessageLanding();
//			Tools.waiting(1000);
//			if (!Copter.isFlying(idMaster)) {
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.FINISH;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//			}
//		}
//	}

//	private void enviarMessageLanding() {
//		String msg = null;
//		byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
//		Output out = new Output(buffer);
//
//		out.writeInt(idMaster);
//		out.writeInt(FollowMeParam.MsgLanding);
//		msg = "Aterrizad todos";
//		out.flush();
//		byte[] message = Arrays.copyOf(buffer, out.position());
//		System.out.println("Enviado" + idMaster + "-->\t" + msg);
//		Copter.sendBroadcastMessage(idMaster, message);
//		out.clear();
//	}
//
//	private void enviarmessageTakeOff() {
//		String msg = null;
//		byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
//		Output out = new Output(buffer);
//
//		int size = FollowMeParam.posFormacion.size();
//		int[] idsFormacion = new int[size];
//		int i = 0;
//
//		for (int p : FollowMeParam.posFormacion.values()) {
//			idsFormacion[i++] = p;
//		}
//
//		out.writeInt(idMaster);
//		out.writeInt(FollowMeParam.MsgTakeOff);
//		out.writeInt(size);
//		out.writeInts(idsFormacion);
//		out.flush();
//		byte[] message = Arrays.copyOf(buffer, out.position());
//		Copter.sendBroadcastMessage(idMaster, message);
//		out.clear();
//
//		msg = "Despegad todos posiciones: [";
//		for (i = 0; i < idsFormacion.length; i++) {
//			msg += "" + idsFormacion[i] + ",";
//		}
//		msg += "]";
//		System.out.println("Enviado" + idMaster + "-->\t" + msg);
//
//	}
//
//	private void enviarMessageCoordenadas() {
//		String msg = null;
//		byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
//		Output out = new Output(buffer);
//
//		out.writeInt(idMaster);
//		out.writeInt(FollowMeParam.MsgCoordenadas);
//		double lat, lon, heading = 0.01, z, speedX, speedY, speedZ;
//		Point2D.Double geo = Copter.getGeoLocation(FollowMeParam.posMaster);
//		lat = geo.getY();
//		lon = geo.getX();
//		heading = Copter.getHeading(FollowMeParam.posMaster);
//		z = Copter.getZRelative(FollowMeParam.posMaster);
//		Triplet<Double, Double, Double> speed = Copter.getSpeeds(FollowMeParam.posMaster);
//		speedX = speed.getValue0();
//		speedY = speed.getValue1();
//		speedZ = speed.getValue2();
//
//		out.writeDouble(lat);
//		out.writeDouble(lon);
//		out.writeDouble(heading);
//		out.writeDouble(z);
//		out.writeDouble(speedX);
//		out.writeDouble(speedY);
//		out.writeDouble(speedZ);
//
//		msg = "MsgCoordenadas lat: " + lat + " lon: " + lon + ", heading: " + heading + ", z: " + z + ", speedX: "
//				+ speedX + ", speedY: " + speedY + ", speedZ: " + speedZ;
//		out.flush();
//		byte[] message = Arrays.copyOf(buffer, out.position());
//		System.out.println("Enviado" + idMaster + "-->\t" + msg);
//		Copter.sendBroadcastMessage(idMaster, message);
//		out.clear();
//
//	}

	// private void enviarMessage(int typeMsg) {
	// String msg = null;
	// byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
	// Output out = new Output(buffer);
	//
	// out.writeInt(idMaster);
	// out.writeInt(typeMsg);
	// switch (typeMsg) {
	// case FollowMeParam.MsgTakeOff:
	//
	// int size = FollowMeParam.posFormacion.size();
	// int[] idsFormacion = new int[size];
	// int i = 0;
	//
	// for (int p : FollowMeParam.posFormacion.values()) {
	// idsFormacion[i++] = p;
	// }
	//
	// out.writeInt(size);
	// out.writeInts(idsFormacion);
	// msg = "Despegad todos posiciones: [";
	// for (i = 0; i < idsFormacion.length; i++) {
	// msg += "" + idsFormacion[i] + ",";
	// }
	// msg += "]";
	// break;
	//
	// case FollowMeParam.MsgCoordenadas:
	//
	// double lat, lon, heading = 0.01, z, speedX, speedY, speedZ;
	// Point2D.Double geo = Copter.getGeoLocation(FollowMeParam.posMaster);
	// lat = geo.getY();
	// lon = geo.getX();
	// heading = Copter.getHeading(FollowMeParam.posMaster);
	// // if (contador1 == 10) {
	// // contador1 = 0;
	// // }
	// // contador2++;
	// // if(contador2<35) {
	// // heading = 0.5235 * contador1;
	// // }else {
	// // heading = 0.5235 * contador1++;
	// // contador2 = 0;
	// // }
	// z = Copter.getZRelative(FollowMeParam.posMaster);
	// // z=10.0;
	// Triplet<Double, Double, Double> speed =
	// Copter.getSpeeds(FollowMeParam.posMaster);
	// speedX = speed.getValue0();
	// speedY = speed.getValue1();
	// speedZ = speed.getValue2();
	// out.writeDouble(lat);
	// out.writeDouble(lon);
	// out.writeDouble(heading);
	// out.writeDouble(z);
	// out.writeDouble(speedX);
	// out.writeDouble(speedY);
	// out.writeDouble(speedZ);
	// msg = "MsgCoordenadas lat: " + lat + " lon: " + lon + ", heading: " + heading
	// + ", z: " + z + ", speedX: "
	// + speedX + ", speedY: " + speedY + ", speedZ: " + speedZ;
	// break;
	//
	// case FollowMeParam.MsgLanding:
	//
	// msg = "Aterrizad todos";
	// break;
	//
	// default:
	// break;
	// }
	// out.flush();
	// byte[] message = Arrays.copyOf(buffer, out.position());
	// System.out.println("Enviado" + idMaster + "-->\t" + msg);
	// Copter.sendBroadcastMessage(idMaster, message);
	// out.clear();
	//
	// }
}