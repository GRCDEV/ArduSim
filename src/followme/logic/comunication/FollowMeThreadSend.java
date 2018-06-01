package followme.logic.comunication;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import followme.logic.FollowMeParam;
import uavController.UAVParam;

public class FollowMeThreadSend extends Thread {

	public int idUAV;
	public Message msg;

	public FollowMeThreadSend(int id, Message msg) {
		this.idUAV = id;
		this.msg = msg;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		byte[] buffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH];
		Output out = new Output(buffer);

		int replicacion = 2;
//		while (replicacion-- > 0) {
//			out.writeInt(this.idUAV);
//			out.writeInt(this.msg.type);
//			switch (msg.type) {
//			case FollowMeParam.EnvioId:
//				out.writeInt(msg.idReceiver);
//				out.writeInt(msg.idSender);
//				break;
//			case FollowMeParam.EnvioPosFormacion:
//				out.writeInt(msg.posFormacion.length);
//				out.writeInts(msg.posFormacion);
//				break;
//			case FollowMeParam.EnvioAlturaTakeOff:
//				out.writeInt(msg.idReceiver);
//				out.writeInt(msg.idSender);
//				out.writeDouble(msg.altura);
//				break;
//			case FollowMeParam.EnvioCoordenadasMaster:
//				out.writeDouble(msg.latitud);
//				out.writeDouble(msg.longitud);
//				out.writeDouble(msg.altura);
//				out.writeInt(msg.flyModeBaseMaster);
//				out.writeLong(msg.flyModeCustomMaster);
//				out.writeDouble(msg.heading);
//				break;
//			case FollowMeParam.EnvioLandingMaster:
//				out.writeDouble(msg.latitud);
//				out.writeDouble(msg.longitud);
//			default:
//				break;
//			}
//			out.flush();
//			byte[] message = Arrays.copyOf(buffer, out.position());
//			System.out.println("Enviado" + this.idUAV + "-->\t" + this.msg.toString());
//			API.sendBroadcastMessage(idUAV, message);
//			out.clear();
//		}
	}

}
