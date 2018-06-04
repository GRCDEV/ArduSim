package followme.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import followme.logic.FollowMeParam.FollowMeState;
import main.Param;
import main.Param.SimulatorState;

public class MasterListener extends Thread {

	private int idMaster;
	private Set<Integer> idDetectados;
	private HashMap<Integer,Integer> slaveReady;

	public MasterListener(int idMaster) {
		this.idMaster = idMaster;
		if (FollowMeParam.posMaster != idMaster) {
			System.err.println("Master id distinto");
		}
		idDetectados = new HashSet<Integer>();
		slaveReady = new HashMap();
	}

	@Override
	public void run() {

		if (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.START) {
			FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.LISTEN_ID;
			GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		}

		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.LISTEN_ID) {
			// Escuchar nuevos IDs
			recibirMessage(FollowMeParam.MsgIDs);
			
		}
		

		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
			// Escuchar IDs Ready
			recibirMessage(FollowMeParam.MsgReady);
			GUI.log("Size idDetectados: "+idDetectados.size()+" slaveReady: "+slaveReady.size());
			if (idDetectados.size() == slaveReady.size()) {
				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.READY_TO_START;
				GUI.updateprotocolState(FollowMeParam.posMaster,
						FollowMeParam.uavs[FollowMeParam.posMaster].getName());
			}
		}
		
		

	}

	private void recibirMessage(int type) {

		String msg = null;
		byte[] message = null;
		Input in = new Input();
		int idSlave;
		message = Copter.receiveMessage(FollowMeParam.posMaster);

		in.setBuffer(message);
		int idSender = in.readInt();
		int typeMsg = in.readInt();
		if (typeMsg == type) {
			switch (typeMsg) {
			case FollowMeParam.MsgIDs:
				idSlave = in.readInt();
				msg = "Id nuevo: " + idSlave;
				idDetectados.add(idSlave);
				FollowMeParam.posFormacion.put(idSlave, idSlave);
				String m = "[";
				for (int i : FollowMeParam.posFormacion.values()) {
					m += "" + i + ",";
				}
				GUI.log("IDs detectados: " + m);
				break;
			case FollowMeParam.MsgReady:
				idSlave = in.readInt();
				msg = "Slave Ready: " + idSlave +" total: [";
				slaveReady.put(idSlave, idSlave);
				for (int i : slaveReady.values()) {
					msg += "" + i + ",";
				}
				break;
			default:
				msg = "" + typeMsg;
				break;
			}
			if (!msg.isEmpty())
				System.out.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": " + msg);
		}else {
			System.out.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": ignorado " + typeMsg);

		}
	}
}
