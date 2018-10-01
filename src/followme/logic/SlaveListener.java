package followme.logic;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicReference;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;

public class SlaveListener extends Thread {

	private int numUAV;
	private long idSlave;
	private int idMaster;
	private AtomicReference<Point2D.Double> point;

	private int[] idsFormacion;

	public SlaveListener(int numUAV, AtomicReference<Point2D.Double> point) {
		this.numUAV = numUAV;
		this.idSlave = Tools.getIdFromPos(numUAV);
		this.point = point;
	}

	@Override
	public void run() {

		GUI.log("SlaveListener " + numUAV + " run");

		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}

		FollowMeParam.uavs[numUAV] = FollowMeState.SEND_ID;
		GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

		byte[] message;
		Input in = new Input();
		int idSender, typeRecibido;
		boolean received = false;
		Point2D.Double offset = null;
		double x = 0, y = 0, heading = 0;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (typeRecibido == FollowMeParam.MsgTakeOff) {
				idMaster = idSender;
				int size = in.readInt();
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				idsFormacion = in.readInts(size);
				// Precálculo de la formación
				int posFormacion = -1;
				//GUI.log("IdsFormacion = "+ idsFormacion + "Posformacion = " + posFormacion );
				for (int i = 0; i < idsFormacion.length; i++) {
					if (idsFormacion[i] == idSlave) {
						posFormacion = i;
					}
				} // TODO acción si no lo encuentra
				
				//GUI.log("Luego // IdsFormacion = "+ idsFormacion + "Posformacion = " + posFormacion );

				switch (FollowMeParam.FormacionUsada) {
				case FollowMeParam.FormacionLinea:
					offset = Formation.getOffsetLineal(posFormacion);
					break;
				case FollowMeParam.FormacionMatriz:
					offset = Formation.getOffsetMatrix(posFormacion, size);
					break;
				case FollowMeParam.FormacionCircular:
					offset = Formation.getOffsetCircular(posFormacion, size);
					break;
				default:
					break;
				}
				// TODO comprobar que offset != null
				FollowMeParam.uavs[numUAV] = FollowMeState.TAKE_OFF;
				GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
				received = true;
			}
			// else {
			// GUI.log("SlaveListener "+idSlave+" Msg no enviado por MASTER, enviado por:
			// "+idSender+" Msg tipo: "+FollowMeParam.getTypeMessage(typeRecibido));
			// }
		}

		while (FollowMeParam.uavs[numUAV] == FollowMeState.TAKE_OFF) {
			message = Copter.receiveMessage(numUAV);
		}
		
		//TODO Fase pruebas
		//heading = -Math.PI/2;
		//heading = -Math.PI/4;
		//////////////////////

		double incX, incY;
		incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
		incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);

		x += incX;
		y += incY;
		point.set(new Point2D.Double(x,y));
		GeoCoordinates geo = Tools.UTMToGeo(x, y);
		Copter.moveUAV(numUAV, geo, (float)FollowMeParam.AlturaInitFollowers);

		while (FollowMeParam.uavs[numUAV] == FollowMeState.GOTO_POSITION) {
			message = Copter.receiveMessage(numUAV);
		}

		received = false;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (idSender == idMaster && typeRecibido == FollowMeParam.MsgCoordenadas) {
				double  z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();

				incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
				incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);

				x += incX;
				y += incY;

				geo = Tools.UTMToGeo(x, y);
				Copter.moveUAV(numUAV, geo, (float)z);

				received = true;
			}
		}

		FollowMeParam.uavs[numUAV] = FollowMeState.FOLLOW;
		GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

		received = false;
		while (!received) {

			long t = System.currentTimeMillis();

			message = Copter.receiveMessage(numUAV); /// Espera bloqueante

			if (System.currentTimeMillis() - t > 3000) {
				System.out.println(numUAV);
			}

			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (idSender == idMaster && typeRecibido == FollowMeParam.MsgCoordenadas) {
				double  z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();

				incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
				incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);

				x += incX;
				y += incY;

				geo = Tools.UTMToGeo(x, y);
				Copter.moveUAV(numUAV, geo, (float)z);

			} else if (idSender == idMaster && typeRecibido == FollowMeParam.MsgLanding) {

				Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);

				received = true;
			}
		}
		in.close();

		FollowMeParam.uavs[numUAV] = FollowMeState.LANDING_FOLLOWERS;
		GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

		GUI.log("SlaveListener " + idSlave + " Finaliza");

	}

}