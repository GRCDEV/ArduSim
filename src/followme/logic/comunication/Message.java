package followme.logic.comunication;

import followme.logic.FollowMeParam;

public class Message {

	public int type;
	public int idReceiver; 
	public int idSender;
	public int[] posFormacion;
	public double altura;
	public double latitud;
	public double longitud;
	public double heading;
	public String msg;
	public boolean msgFinish;
	public int flyModeBaseMaster;
	public long flyModeCustomMaster;

	public Message(int type, int receiver, int sender, String msg) {
		this.type = type;
		this.idReceiver = receiver;
		this.idSender = sender;
		this.msg = msg;
	}

//	public Message(int reiceve, int sender) {
//		this.type = FollowMeParam.EnvioId;
//		this.idReceiver = reiceve;
//		this.idSender = sender;
//	}
//	
//	public Message(int[] posFormacion) {
//		this.type = FollowMeParam.EnvioPosFormacion;
//		this.posFormacion = posFormacion;
//	}
//
//	public Message(int receiver, int sender, double altura) {
//		this.type = FollowMeParam.EnvioAlturaTakeOff;
//		this.idReceiver = receiver;
//		this.idSender = sender;
//		this.altura = altura;
//	}
//
//	public Message(double latitud, double longitud, double zRel, int flyModeBase, long flyModeCustom, double heading) {
//		this.type = FollowMeParam.EnvioCoordenadasMaster;
//		this.latitud = latitud;
//		this.longitud = longitud;
//		this.altura = zRel;
//		this.flyModeBaseMaster = flyModeBase;
//		this.flyModeCustomMaster = flyModeCustom;
//		this.heading = heading;
//	}
//
//	public Message(double latitud, double longitud) {
//		this.type = FollowMeParam.EnvioLandingMaster;
//		this.latitud = latitud;
//		this.longitud = longitud;
//	}
//
//	@Override
//	public String toString() {
//		switch (type) {
//		case FollowMeParam.EnvioId:
//			return "Type: EnvioId, IdReceiver: " + idReceiver + ", IdSender: " + idSender;
//		case FollowMeParam.EnvioPosFormacion:
//			String str = "Type: EnvioPosFormacion, posFormacion: [";
//			for(int i = 0; i< posFormacion.length; i++) {
//				str+=posFormacion[i]+" ";
//			}
//			str+="]";
//			return str;
//		case FollowMeParam.EnvioAlturaTakeOff:
//			return "Type: EnvioAlturaTakeOff, IdReceiver: " + idReceiver + ", IdSender: " + idSender + ", Altura: "
//					+ altura;
//		case FollowMeParam.EnvioCoordenadasMaster:
//			return "Type: EnvioCoordenadasMaster, latitud: " + latitud + ", longitud: " + longitud + ", altura: "
//					+ altura +", heading: "+heading;
//		case FollowMeParam.EnvioLandingMaster:
//			return "Type: EnvioLandingMaster, latitud: " + latitud + ", longitud: " + longitud;
//		}
//		return super.toString();
//	}
}
