package followme.logic;

import java.awt.geom.Point2D;
import java.io.IOException;

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

	private int[] idsFormacion;

	public SlaveListener(int numUAV) {
		this.numUAV = numUAV;
		this.idSlave = Tools.getIdFromPos(numUAV);

	}

	@Override
	public void run() {

		GUI.log("SlaveListener " + numUAV + " run");

		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}

		FollowMeParam.uavs[numUAV] = FollowMeState.SEND_ID;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

		// Recibir MsgTakeOff
		byte[] message;
		Input in = new Input();
		int idSender, typeRecibido;
		boolean received = false;
		Point2D.Double offset = null;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (typeRecibido == FollowMeParam.MsgTakeOff) {
				idMaster = idSender;
				int size = in.readInt();
				idsFormacion = in.readInts(size);
				// Precálculo de la formación
				int posFormacion = -1;
				for (int i = 0; i < idsFormacion.length; i++) {
					if (idsFormacion[i] == idSlave) {
						posFormacion = i;
					}
				} // TODO acción si no lo encuentra

				switch (FollowMeParam.FormacionUsada) {
				case FollowMeParam.FormacionLinea:
					offset = Formacion.getOffsetLineal(posFormacion);
					break;
				case FollowMeParam.FormacionMatriz:
					offset = Formacion.getOffsetMatrix(posFormacion, size);
					break;
				case FollowMeParam.FormacionCircular:
					offset = Formacion.getOffsetCircular(posFormacion, size);
					break;
				default:
					break;
				}
				// TODO comprobar que offset != null
				FollowMeParam.uavs[numUAV] = FollowMeState.TAKE_OFF;
				GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
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

		

		received = false;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (idSender == idMaster && typeRecibido == FollowMeParam.MsgCoordenadas) {
				double x, y, heading, z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();

				double incX, incY;
				incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
				incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);

				x += incX;
				y += incY;

				GeoCoordinates geo = Tools.UTMToGeo(x, y);

				try {
					Copter.getController(numUAV).msgTarget(FollowMeParam.TypemsgTargetCoordinates, geo.latitude,
							geo.longitude, z, 1.0, false, speedX, speedY, speedZ);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				received = true;
			}
		}

		FollowMeParam.uavs[numUAV] = FollowMeState.FOLLOW;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

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
				double x, y, heading, z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();

				double incX, incY;
				incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
				incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);

				x += incX;
				y += incY;

				GeoCoordinates geo = Tools.UTMToGeo(x, y);

				double speed = Copter.getSpeed(numUAV);
				if (speed < 0.1) {
					System.err.println("Slave " + numUAV + " parado");
				}
				try {
					Copter.getController(numUAV).msgTarget(FollowMeParam.TypemsgTargetCoordinates, geo.latitude,
							geo.longitude, z, 1.0, false, speedX, speedY, speedZ);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (idSender == idMaster && typeRecibido == FollowMeParam.MsgLanding) {

				Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);

				received = true;
			}
		}

		FollowMeParam.uavs[numUAV] = FollowMeState.LANDING_FOLLOWERS;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

		GUI.log("SlaveListener " + idSlave + " Finaliza");

	}

}