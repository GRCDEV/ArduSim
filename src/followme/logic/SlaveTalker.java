package followme.logic;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.SwarmHelper;
import followme.logic.FollowMeParam.FollowMeState;
import main.Param;
import main.Param.SimulatorState;
import uavController.UAVParam;

public class SlaveTalker extends Thread {

	private int idSlave;

	public SlaveTalker(int idSlave) {
		this.idSlave = idSlave;
	}

	@Override
	public void run() {

		SwarmHelper.setSwarmState(idSlave, FollowMeParam.uavs[idSlave].getName());

		while (Param.simStatus != SimulatorState.READY_FOR_TEST) {
			enviarMessage(FollowMeParam.MsgIDs);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (FollowMeParam.uavs[idSlave] == FollowMeState.START) {
			FollowMeParam.uavs[idSlave] = FollowMeState.WAIT_TAKE_OFF_SLAVE;
			SwarmHelper.setSwarmState(idSlave, FollowMeParam.uavs[idSlave].getName());
		}

		while (FollowMeParam.uavs[idSlave] != FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (UAVParam.uavCurrentData[idSlave].getZ() >= FollowMeParam.AlturaInitFollowers * 0.98) {
				enviarMessage(FollowMeParam.MsgReady);
			}
		}
		
		

	}

	private void enviarMessage(int typeMsg) {
		String msg = null;
		byte[] buffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH];
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
			out.writeInt(idSlave);
			msg = "Slave Ready: " + idSlave;
		default:
			break;
		}
		out.flush();
		byte[] message = Arrays.copyOf(buffer, out.position());
		System.out.println("Enviado" + idSlave + "-->\t" + msg);
		API.sendBroadcastMessage(idSlave, message);
		out.clear();
	}
}
