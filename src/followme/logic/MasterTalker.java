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
	
	public MasterTalker(int idMaster) {
		this.idMaster = idMaster;
	}

	@Override
	public void run() {
		GUI.log("MasterTalker run");
		
		while (!Tools.isSetupInProgress()) {
			Tools.waiting(100);
		}
		
		FollowMeParam.uavs[idMaster] = FollowMeState.WAIT_TAKE_OFF_MASTER;
		GUI.updateProtocolState(idMaster, FollowMeParam.uavs[idMaster].getName());
		
		byte[] buffer;
		Output out = new Output();
		int[] idsFormacion = new int[FollowMeParam.posFormacion.size()];
		int i = 0;
		for (int p : FollowMeParam.posFormacion.values()) {
			idsFormacion[i++] = p;
		}
		
		double lat, lon, heading, z, speedX, speedY, speedZ;
		Point2D.Double utm = Copter.getUTMLocation(FollowMeParam.posMaster);
		heading = Copter.getHeading(FollowMeParam.posMaster);
		
		while (FollowMeParam.uavs[idMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);
			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgTakeOff);
			out.writeInt(idsFormacion.length);
			out.writeDouble(utm.x);
			out.writeDouble(utm.y);
			out.writeDouble(heading);
			out.writeInts(idsFormacion);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();
			Tools.waiting(500);
		}
		
		while (!Tools.isExperimentInProgress()) {
			Tools.waiting(200);
		}
		
		FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.TAKE_OFF;
		GUI.updateProtocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		
		while (Copter.getZRelative(FollowMeParam.posMaster) < FollowMeParam.AlturaInitSend) {
			Tools.waiting(200);
		}
		
		FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.SENDING;
		GUI.updateProtocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		

		
		while (Copter.getFlightMode(idMaster) != FlightMode.LAND_ARMED
				&& Copter.getFlightMode(idMaster) != FlightMode.LAND) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);;
			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgCoordenadas);
			utm = Copter.getUTMLocation(FollowMeParam.posMaster);
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
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();
			Tools.waiting(1000);
		}
		
		while (true) {
			buffer = new byte[Tools.DATAGRAM_MAX_LENGTH];
			out.setBuffer(buffer);

			out.writeInt(idMaster);
			out.writeInt(FollowMeParam.MsgLanding);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			Copter.sendBroadcastMessage(idMaster, message);
			out.clear();
			Tools.waiting(1000);
		}
	}
}