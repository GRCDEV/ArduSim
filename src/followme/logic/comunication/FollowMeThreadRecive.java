package followme.logic.comunication;

import java.util.NoSuchElementException;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import followme.logic.FollowMeParam;

public class FollowMeThreadRecive extends Thread {

	public int idUAV;
	public MessageStorage msgStorage;

	public FollowMeThreadRecive(int id, MessageStorage msgStrg) {
		this.idUAV = id;
		this.msgStorage = msgStrg;
	}

	@Override
	public void run() {
		byte[] message = null;
		Input in = new Input();

//		while (true) {
//			 try {
//			message = API.receiveMessage(idUAV);
//
//			in.setBuffer(message);
//			Message msg = null;
//			int num = in.readInt();
//			int type = in.readInt();
//			int receiver, sender, flyModeBase;
//			int lenPosFormacion;
//			int[] posFormacion;
//			double altura, latitud, longitud, heading;
//			long flyModeCustom;
//			switch (type) {
//			case FollowMeParam.EnvioId:
//				receiver = in.readInt();
//				sender = in.readInt();
//				msg = new Message(receiver, sender);
//				break;
//			case FollowMeParam.EnvioPosFormacion:
//				lenPosFormacion = in.readInt();
//				posFormacion = in.readInts(lenPosFormacion);
//				msg = new Message(posFormacion);
//				break;
//			case FollowMeParam.EnvioAlturaTakeOff:
//				receiver = in.readInt();
//				sender = in.readInt();
//				altura = in.readDouble();
//				msg = new Message(receiver, sender, altura);
//				break;
//			case FollowMeParam.EnvioCoordenadasMaster:
//				latitud = in.readDouble();
//				longitud = in.readDouble();
//				altura = in.readDouble();
//				flyModeBase = in.readInt();
//				flyModeCustom = in.readLong();
//				heading = in.readDouble();
//				msg = new Message(latitud, longitud, altura, flyModeBase, flyModeCustom, heading);
//				break;
//			case FollowMeParam.EnvioLandingMaster:
//				latitud = in.readDouble();
//				longitud = in.readDouble();
//				msg = new Message(latitud, longitud);
//				break;
//			default:
//				break;
//			}
//			System.out.println("Recibidos" + idUAV + "<--" + num + ": " + msg.toString());
//			this.msgStorage.put(msg);
//			 } catch (NullPointerException e) {
//			 System.err.println("Error de recibir NullPointerException:");
//			 e.printStackTrace();
//			 } catch (NoSuchElementException e) {
//			 System.err.println("Error de recibir NoSuchElementException: ");
//			 e.printStackTrace();
//			 }
//		}

	}

}
