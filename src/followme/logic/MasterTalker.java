package followme.logic;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Output;

import api.Copter;
import api.GUI;
import api.Tools;
import followme.logic.FollowMeParam.FollowMeState;

public class MasterTalker extends Thread {

	private int idMaster;

	public MasterTalker(int idMaster) {
		this.idMaster = idMaster;
	}

	@Override
	public void run() {
		
		while (FollowMeParam.uavs[FollowMeParam.posMaster] != FollowMeState.WAIT_TAKE_OFF_MASTER) {
			// Esperar
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(Tools.isSetupFinished()) {
				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.WAIT_TAKE_OFF_MASTER;
				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
			}
		}
		// Estado del simulador SETUP
		// Enviar despegar SLAVEs
		
		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			enviarMessage(FollowMeParam.MsgTakeOff);
		}

		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.READY_TO_START) {
			enviarMessage(FollowMeParam.MsgCoordenadas);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void enviarMessage(int typeMsg) {
		String msg = null;
		byte[] buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
		Output out = new Output(buffer);

		out.writeInt(idMaster);
		out.writeInt(typeMsg);
		switch (typeMsg) {
		case FollowMeParam.MsgTakeOff:
			
			int size = FollowMeParam.posFormacion.size();
			int[] idsFormacion = new int[size];
			int i = 0;
			
			
			for (int p : FollowMeParam.posFormacion.values()) {
				idsFormacion[i++] = p;
			}
			
			out.writeInt(size);
			out.writeInts(idsFormacion);
			msg = "Despegad todos posiciones: [";
			for(i=0;i<idsFormacion.length;i++) {
				msg+=""+idsFormacion[i]+",";
			}
			msg+="]";
			break;
			
		case FollowMeParam.MsgCoordenadas:
			
			double lat, lon, heading, z, speedX, speedY, speedZ;
			Point2D.Double geo = Copter.getGeoLocation(FollowMeParam.posMaster);
			lat = geo.getY();
			lon = geo.getX();
			heading = Copter.getHeading(FollowMeParam.posMaster);
			z = Copter.getZRelative(FollowMeParam.posMaster);
			Triplet<Double, Double, Double> speed = Copter.getSpeeds(FollowMeParam.posMaster);
			speedX = speed.getValue0();
			speedY = speed.getValue1();
			speedZ = speed.getValue2();
			out.writeDouble(lat);
			out.writeDouble(lon);
			out.writeDouble(heading);
			out.writeDouble(z);
			out.writeDouble(speedX);
			out.writeDouble(speedY);
			out.writeDouble(speedZ);
			msg = "MsgCoordenadas lat: " + lat + " lon: " + lon + ", heading: " + heading + ", z: " + z + ", speedX: "
					+ speedX + ", speedY: " + speedY + ", speedZ: " + speedZ;
			break;
			
		case FollowMeParam.MsgLanding:
			
			msg = "Aterrizad todos";
			break;
			
		default:
			break;
		}
		out.flush();
		byte[] message = Arrays.copyOf(buffer, out.position());
		System.out.println("Enviado" + idMaster + "-->\t" + msg);
		Copter.sendBroadcastMessage(idMaster, message);
		out.clear();

	}
}
