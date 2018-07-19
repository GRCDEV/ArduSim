package followme.logic;

import java.util.HashMap;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import followme.logic.FollowMeParam.FollowMeState;

public class MasterListener extends Thread {

	private int idMaster;
	private HashMap<Integer, Integer> slaveReady;
	public MasterListener(int idMaster) {
		this.idMaster = idMaster;
		if (FollowMeParam.posMaster != idMaster) {
			System.err.println("Master id distinto");
		}
		slaveReady = new HashMap<>();
	}

	@Override
	public void run() {
		GUI.log("MasterListener run");
		
		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}
		
		FollowMeParam.uavs[idMaster] = FollowMeState.LISTEN_ID;
		GUI.updateProtocolState(idMaster, FollowMeParam.uavs[idMaster].getName());
		
		Input in = new Input();
		byte[] message;
		int idSender, typeMsg;
		
		while (Tools.areUAVsReadyForSetup()) { //TODO algo no finciona bien
			// Recibir MSG_IDs
			message = Copter.receiveMessage(idMaster); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeMsg = in.readInt();
			if (typeMsg == FollowMeParam.MsgIDs) {// Msg esperado
				FollowMeParam.posFormacion.put(idSender, idSender);
				String m = "[";
				for (int i : FollowMeParam.posFormacion.values()) {
					m += "" + i + ",";
				}
				GUI.log("IDs detectados: " + m + "]");
			}

		}
		
		while (FollowMeParam.uavs[idMaster] != FollowMeState.WAIT_TAKE_OFF_MASTER) {
			Tools.waiting(100);
		}
		
		int totUAVs = FollowMeParam.posFormacion.size();
		
		while (slaveReady.size() != totUAVs) {
			message = Copter.receiveMessage(FollowMeParam.posMaster);
			in.setBuffer(message);
			idSender = in.readInt();
			typeMsg = in.readInt();

			if (typeMsg == FollowMeParam.MsgReady) {
				slaveReady.put(idSender, idSender);

				String msg = "[";
				for (int i : slaveReady.values()) {
					msg += "" + i + ",";
				}
				GUI.log("MasterListener Recibe Ready:" + msg + "]");
			}
		}
		
		FollowMeParam.setupFinished = true;
		FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.READY_TO_START;
		GUI.updateProtocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		

		GUI.log("MasterListener Finaliza");

	}
}
