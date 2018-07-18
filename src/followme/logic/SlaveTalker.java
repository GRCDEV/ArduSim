package followme.logic;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.UTMCoordinates;
import followme.logic.FollowMeParam.FollowMeState;

public class SlaveTalker extends Thread {

	private int numUAV;
	private AtomicReference<Point2D.Double> point;

	public SlaveTalker(int numUAV, AtomicReference<Point2D.Double> point) {
		this.numUAV = numUAV;
		this.point = point;
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

			// System.out.println("SlaveTalker" + idSlave + "-->\t" + "Envio mi ID: " +
			// idSlave);
			Tools.waiting(1000);
		}

		if (FollowMeParam.uavs[numUAV] == FollowMeState.TAKE_OFF) {
			Copter.takeOff(numUAV, FollowMeParam.AlturaInitFollowers / 2);
			FollowMeParam.uavs[numUAV] = FollowMeState.GOTO_POSITION;
			GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		}

		double dist = 99999;
		Point2D.Double pDestino = null;
		while (point.get() == null) {
			Tools.waiting(100);
		}
		pDestino = point.get();
		UTMCoordinates UTMActual = Tools.geoToUTM(Copter.getGeoLocation(numUAV).y, Copter.getGeoLocation(numUAV).x);

		do {
			UTMActual = Tools.geoToUTM(Copter.getGeoLocation(numUAV).y, Copter.getGeoLocation(numUAV).x);
			dist = Math.sqrt(
					Math.pow((pDestino.x - UTMActual.Easting), 2) + Math.pow((pDestino.y - UTMActual.Northing), 2));
//			System.out.println("Slave " + numUAV + ": " + dist + " m");
			Tools.waiting(1000);
		} while (0.5 < dist);

		FollowMeParam.uavs[numUAV] = FollowMeState.WAIT_MASTER;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

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
	}
}