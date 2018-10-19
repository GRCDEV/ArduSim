package followme.logic;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import followme.logic.FollowMeParam.FollowMeState;

public class SlaveTalker extends Thread {

	private int numUAV;

	public SlaveTalker(int numUAV) {
		this.numUAV = numUAV;
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
			GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		}

		double dist;
		while (FollowMeParam.takeoffLocation.get(numUAV) == null) {
			Tools.waiting(100);
		}
		UTMCoordinates pDestino = FollowMeParam.takeoffLocation.get(numUAV);
		GeoCoordinates geoActual;
		UTMCoordinates UTMActual;

		do {
			geoActual = Copter.getGeoLocation(numUAV);
			UTMActual = Tools.geoToUTM(geoActual.latitude, geoActual.longitude);
			dist = Math.sqrt(
					Math.pow((pDestino.x - UTMActual.x), 2) + Math.pow((pDestino.y - UTMActual.y), 2));
//			System.out.println("Slave " + numUAV + ": " + dist + " m");
			Tools.waiting(1000);
		} while (0.5 < dist);

		FollowMeParam.uavs[numUAV] = FollowMeState.WAIT_MASTER;
		GUI.updateProtocolState(numUAV, FollowMeParam.uavs[numUAV].getName());

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
		
		out.close();

		GUI.log("SlaveTalker " + numUAV + " Finaliza");
	}
}