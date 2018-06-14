package followme.logic;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;

public class SlaveTalker extends Thread {

	private int numUAV;
	private long idSlave;
	private boolean takeOff;
	private AtomicReference<byte[]> bufferMsg;
	private boolean imReady;
	private int posFormacion;
	private int numMaxUAV;
	private byte[] messageAux = null;

	public SlaveTalker(int numUAV) {
		this.numUAV = numUAV;
		this.idSlave = Tools.getIdFromPos(numUAV);
		this.takeOff = false;
		this.imReady = false;

	}

	@Override
	public void run() {

		GUI.log("SlaveTalker " + numUAV + " run");
		
		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}
		
		while (FollowMeParam.uavs[numUAV] == FollowMeState.START) {
			Tools.waiting(100);
		}
		
		Output out = new Output();
		byte[] buffer;
		while (FollowMeParam.uavs[numUAV] == FollowMeState.SEND_ID) {

			// Processo del estado actual
			// Enviar mi idSlave
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);
			out.writeInt(numUAV);
			out.writeInt(FollowMeParam.MsgIDs);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(numUAV, message);
			out.clear();

//			System.out.println("SlaveTalker" + idSlave + "-->\t" + "Envio mi ID: " + idSlave);
			Tools.waiting(1000);
		}
		
		while (FollowMeParam.uavs[numUAV] == FollowMeState.WAIT_TAKE_OFF_SLAVE
				|| FollowMeParam.uavs[numUAV] == FollowMeState.TAKE_OFF) {
			Tools.waiting(200);
		}
		
		while (FollowMeParam.uavs[numUAV] == FollowMeState.WAIT_MASTER) {
			// Enviar MsgReady
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);
			out.writeInt(numUAV);
			out.writeInt(FollowMeParam.MsgReady);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(numUAV, message);
			out.clear();

			Tools.waiting(1000);

		}
		
		
		GUI.log("SlaveTalker " + numUAV + " Finaliza");
		
		/*
		while (FollowMeParam.uavs[idSlave] == FollowMeState.TAKE_OFF) {
			if (Copter.getZRelative(idSlave) >= FollowMeParam.AlturaInitFollowers * 0.98) {

				byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
				Output out = new Output(buffer);

				out.writeInt(idSlave);
				out.writeInt(FollowMeParam.MsgReady);
				out.flush();
				byte[] message = Arrays.copyOf(buffer, out.position());
				Copter.sendBroadcastMessage(idSlave, message);
				out.clear();

				GUI.log("SlaveTalker " + idSlave + " altura correcta. Enviar READY!");

				// Cambio de estado
				if (FollowMeParam.uavs[idSlave] == FollowMeState.TAKE_OFF) {
					FollowMeParam.uavs[idSlave] = FollowMeState.WAIT_MASTER;
					GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
				}

			}
			Tools.waiting(1000);

		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_MASTER) {// Cambio de estado en SlaveListener
			Tools.waiting(2000);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.FOLLOW) {// Cambio de estado en SlaveListener
			Tools.waiting(2000);
		}
		 */

	}

	/*public void run1() {

		GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());

		while (FollowMeParam.uavs[idSlave] == FollowMeState.START) {
			// enviarMessage(FollowMeParam.MsgIDs);

			String msg = null;
			byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			Output out = new Output(buffer);

			out.writeInt(idSlave);
			out.writeInt(FollowMeParam.MsgIDs);
			msg = "ID nuevo " + idSlave;
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(idSlave, message);
			out.clear();
			System.out.println("SlaveTalker" + idSlave + "-->\t" + msg);

			Tools.waiting(1000);
			if (Tools.isSetupFinished()) {
				FollowMeParam.uavs[idSlave] = FollowMeState.WAIT_TAKE_OFF_SLAVE;
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			}
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			// if (!takeOff) {
			// byte[] message = bufferMsg.get();
			// if (message != messageAux) {
			// messageAux = message;
			// Input in = new Input();
			// in.setBuffer(message);
			// int idSender = in.readInt();
			// if (idSender == FollowMeParam.posMaster) {
			// int typeRecibido = in.readInt();
			// if (typeRecibido == FollowMeParam.MsgTakeOff) {
			// numMaxUAV = in.readInt();
			// int[] posiciones = in.readInts(numMaxUAV);
			// for (int i = 0; i < posiciones.length; i++) {
			// if (posiciones[i] == idSlave) {
			// this.posFormacion = i;
			// break;
			// }
			// }
			// String msg = "Master indica el despegue y la posicion en la formacion sera
			// pos:"
			// + this.posFormacion;
			// takeOff = true;
			// } else {
			// System.out.println("MSG " + idSlave + " type: " + typeRecibido);
			// }
			// }
			// } else {
			// System.out.println("MSG " + idSlave + " null");
			// }
			// }
			Tools.waiting(500);
			// if (takeOff) {
			// FollowMeParam.uavs[idSlave] = FollowMeState.TAKE_OFF;
			// GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			// Copter.takeOff(idSlave, FollowMeParam.AlturaInitFollowers);
			// }
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.TAKE_OFF) {

			if (Copter.getZRelative(idSlave) >= FollowMeParam.AlturaInitFollowers * 0.98) {
				FollowMeParam.uavs[idSlave] = FollowMeState.WAIT_MASTER;
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			}
			Tools.waiting(100);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_MASTER) {
			if (!imReady) {
				enviarMessage(FollowMeParam.MsgReady);
				Tools.waiting(500);
				imReady = true;
			}

			Input in = new Input();
			byte[] message = bufferMsg.get();
			if (message != messageAux) {
				messageAux = message;
				in.setBuffer(message);
				int idSender = in.readInt();
				if (idSender == FollowMeParam.posMaster) {
					int typeRecibido = in.readInt();
					if (typeRecibido == FollowMeParam.MsgCoordenadas) {
						masterReady = true;
					}
				}
			}

			if (masterReady) {
				FollowMeParam.uavs[idSlave] = FollowMeState.FOLLOW;
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			}
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.FOLLOW) {
			/*
			 * Input in = new Input(); String msg = null;
			 * 
			 * byte[] message = bufferMsg.get(); if (message != messageAux) { messageAux =
			 * message; in.setBuffer(message); int idSender = in.readInt(); if (idSender ==
			 * FollowMeParam.posMaster) { int typeRecibido = in.readInt(); if (typeRecibido
			 * == FollowMeParam.MsgCoordenadas) { double lat, lon, heading, z, speedX,
			 * speedY, speedZ; lat = in.readDouble(); lon = in.readDouble(); heading =
			 * in.readDouble(); z = in.readDouble(); if (z <
			 * FollowMeParam.AlturaMiminaFollowers) { z =
			 * FollowMeParam.AlturaMiminaFollowers; } speedX = in.readDouble(); speedY =
			 * in.readDouble(); speedZ = in.readDouble(); double distanciaAlPunto = 0.0; msg
			 * = "lat: " + lat + ", lon: " + lon + ", heading: " + heading + ", z: " + z +
			 * ", speedX: " + speedX + ", speedY: " + speedY + ", speedZ: " + speedZ +
			 * ", distanciaAlpunto: "; System.out.println("Lectura Slave" + idSlave + "<---"
			 * + idSender + " Coordenadas " + msg);
			 * 
			 * Point2D geoActual = Copter.getGeoLocation(idSlave); // // Resultado res =
			 * Formacion.getPosition(FollowMeParam.FormacionMatriz, // new
			 * GeoCoordinates(lat, lon), heading, // new GeoCoordinates(geoActual.getY(),
			 * geoActual.getX()), posFormacion, // new Triplet<Double, Double,
			 * Double>(speedX, speedY, speedZ), numMaxUAV); // // distanciaAlPunto =
			 * Formacion.distanceMeters( // new GeoCoordinates(res.getGeo().latitude,
			 * res.getGeo().longitude), // new GeoCoordinates(geoActual.getY(),
			 * geoActual.getX())); // int typeTarget =
			 * FollowMeParam.TypemsgTargetCoordinates; // if (distanciaAlPunto > 75) { // //
			 * typeTarget = FollowMeParam.TypemsgTargetSpeeds; // } try {
			 * Copter.getController(idSlave).msgTarget( typeTarget, //
			 * res.getGeo().latitude, // res.getGeo().longitude, lat, lon, z, 1.0, false,
			 * speedX, speedY, speedZ);
			 * 
			 * } catch (IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } } } }
			 * 
			 * Tools.waiting(800);
			 *
		}

	}

	private void mensageTakeOff() {
		if (!takeOff) {
			byte[] message = bufferMsg.get();

			if (message != messageAux) {
				messageAux = message;
				Input in = new Input();
				in.setBuffer(message);
				int idSender = in.readInt();
				if (idSender == FollowMeParam.posMaster) {
					int typeRecibido = in.readInt();
					if (typeRecibido == FollowMeParam.MsgTakeOff) {
						numMaxUAV = in.readInt();///TODO renombrar a numSlaves
						int[] posiciones = in.readInts(numMaxUAV);
						for (int i = 0; i < posiciones.length; i++) {
							if (posiciones[i] == idSlave) {
								this.posFormacion = i;
								break;
							}
						}
						String msg = "Master indica el despegue y la posicion en la formacion sera pos:"
								+ this.posFormacion;
						takeOff = true;
					}
				}
			} else {
				// System.out.println("MSG null");
			}
		}
	}

	private void processCoordendas() {

		Input in = new Input();
		String msg = null;

		byte[] message = bufferMsg.get();
		if (message != messageAux) {
			messageAux = message;
			in.setBuffer(message);
			int idSender = in.readInt();
			if (idSender == FollowMeParam.posMaster) {
				int typeRecibido = in.readInt();
				if (typeRecibido == FollowMeParam.MsgCoordenadas) {
					double lat, lon, heading, z, speedX, speedY, speedZ;
					lat = in.readDouble();
					lon = in.readDouble();
					heading = in.readDouble();
					z = in.readDouble();
					if (z < FollowMeParam.AlturaMiminaFollowers) {
						z = FollowMeParam.AlturaMiminaFollowers;
					}
					speedX = in.readDouble();
					speedY = in.readDouble();
					speedZ = in.readDouble();
					double distanciaAlPunto = 0;
					if (masterReady) {
						Point2D geoActual = Copter.getGeoLocation(idSlave);

						Resultado res = Formacion.getPosition(FollowMeParam.FormacionMatriz,
								new GeoCoordinates(lat, lon), heading,
								new GeoCoordinates(geoActual.getY(), geoActual.getX()), posFormacion,
								new Triplet<Double, Double, Double>(speedX, speedY, speedZ), numMaxUAV);
						distanciaAlPunto = Formacion.distanceMeters(
								new GeoCoordinates(res.getGeo().latitude, res.getGeo().longitude),
								new GeoCoordinates(geoActual.getY(), geoActual.getX()));

						int typeTarget = FollowMeParam.TypemsgTargetCoordinates;
						if (distanciaAlPunto > 75) {
							// typeTarget = FollowMeParam.TypemsgTargetSpeeds;
						}
						// try {
						// Copter.getController(idSlave).msgTarget(typeTarget, res.getGeo().latitude,
						// res.getGeo().longitude, z, 1.0, false, speedX, speedY, speedZ);
						//
						// } catch (IOException e) {
						// // TODO Auto-generated catch block
						// e.printStackTrace();
						// }
						System.out.println("Slave " + idSlave + " me tengo que mover a una posicion");

					} else {
						this.masterReady = true;
						System.out.println("Mater ready " + idSlave + ": true");
					}

					msg = "lat: " + lat + ", lon: " + lon + ", heading: " + heading + ", z: " + z + ", speedX: "
							+ speedX + ", speedY: " + speedY + ", speedZ: " + speedZ + ", \tdistanciaAlpunto: "
							+ distanciaAlPunto;
					System.out.println("Recibido" + idSlave + "<---" + idSender + " Coordenadas " + msg);

				}
			}
		} else {
			System.out.println("Mensage identico " + message);
		}

	}

	private void enviarMessage(int typeMsg) {
		String msg = null;
		byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		Output out = new Output(buffer);

		out.writeInt(idSlave);
		switch (typeMsg) {
		case FollowMeParam.MsgIDs:
			out.writeInt(FollowMeParam.MsgIDs);
			out.writeInt(idSlave);
			msg = "ID nuevo " + idSlave;
			break;
		case FollowMeParam.MsgReady:
			out.writeInt(FollowMeParam.MsgReady);
			out.writeInt(idSlave);// ******************************************Sobra
			msg = "Slave Ready: " + idSlave;
		default:
			break;
		}
		out.flush();
		byte[] message = Arrays.copyOf(buffer, out.position());
		System.out.println("SlaveTalker" + idSlave + "-->\t" + msg);
		Copter.sendBroadcastMessage(idSlave, message);
		out.clear();
	}*/
}