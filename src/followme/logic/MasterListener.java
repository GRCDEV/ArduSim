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
	private int contador;

	public MasterListener(int idMaster) {
		this.idMaster = idMaster;
		if (FollowMeParam.posMaster != idMaster) {
			System.err.println("Master id distinto");
		}
		slaveReady = new HashMap<>();
		contador = 5;
	}

	@Override
	public void run() {
		GUI.log("MasterListener run");
		
		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}
		
		FollowMeParam.uavs[idMaster] = FollowMeState.LISTEN_ID;
		GUI.updateprotocolState(idMaster, FollowMeParam.uavs[idMaster].getName());
		
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
//			else {
//				System.out.println("MasterListener recibe un mensaje no esperado de SlaveTalker" + idSender
//						+ " de tipo " + FollowMeParam.getTypeMessage(typeMsg));
//			}
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
		GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
		
//		while (!Tools.isExperimentInProgress()) {
//			Tools.waiting(200);
//		}

		GUI.log("MasterListener Finaliza");

	}

//	public void run1() {
//
//		if (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.START) {
//			FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.LISTEN_ID;
//			GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.LISTEN_ID) {
//			// Escuchar nuevos IDs
//			// recibirMessage(FollowMeParam.MsgIDs);
//			String msg = null;
//			byte[] message = null;
//			Input in = new Input();
//			int idSlave;
//			message = Copter.receiveMessage(FollowMeParam.posMaster); /// Espera bloqueante
//
//			in.setBuffer(message);
//			int idSender = in.readInt();
//			int typeMsg = in.readInt();
//			if (typeMsg == FollowMeParam.MsgIDs) {
//				msg = "Id nuevo: " + idSender;
//				idDetectados.add(idSender);
//				FollowMeParam.posFormacion.put(idSender, idSender);
//				String m = "[";
//				for (int i : FollowMeParam.posFormacion.values()) {
//					m += "" + i + ",";
//				}
//				GUI.log("IDs detectados: " + m);
//				System.out.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": " + msg);
//			}
//
//		}
//
//		while (FollowMeParam.uavs[FollowMeParam.posMaster] == FollowMeState.WAIT_TAKE_OFF_MASTER) {
//			// Escuchar IDs Ready
//			// recibirMessage(FollowMeParam.MsgReady);
//
//			String msg = null;
//			byte[] message = null;
//			Input in = new Input();
//			int idSlave;
//			message = Copter.receiveMessage(FollowMeParam.posMaster); /// Espera bloqueante
//
//			in.setBuffer(message);
//			int idSender = in.readInt();
//			int typeMsg = in.readInt();
//			if (typeMsg == FollowMeParam.MsgReady) {
//				idSlave = in.readInt();
//				msg = "Slave Ready: " + idSlave + " total: [";
//				slaveReady.put(idSlave, idSlave);
//				for (int i : slaveReady.values()) {
//					msg += "" + i + ",";
//				}
//				System.out.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": " + msg);
//			}
//
//			GUI.log("Size idDetectados: " + idDetectados.size() + " slaveReady: " + slaveReady.size());
//			if (idDetectados.size() == slaveReady.size()) {
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.READY_TO_START;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//			}
//		}
//		System.out.println("Fin MasterListener");
//
//	}

//	private void recibirMessage(int type) {
//
//		String msg = null;
//		byte[] message = null;
//		Input in = new Input();
//		int idSlave;
//		message = Copter.receiveMessage(FollowMeParam.posMaster);
//
//		in.setBuffer(message);
//		int idSender = in.readInt();
//		int typeMsg = in.readInt();
//		if (typeMsg == type) {
//			switch (typeMsg) {
//			case FollowMeParam.MsgIDs:
//				idSlave = in.readInt();
//				msg = "Id nuevo: " + idSlave;
//				idDetectados.add(idSlave);
//				FollowMeParam.posFormacion.put(idSlave, idSlave);
//				String m = "[";
//				for (int i : FollowMeParam.posFormacion.values()) {
//					m += "" + i + ",";
//				}
//				GUI.log("IDs detectados: " + m);
//				break;
//			case FollowMeParam.MsgReady:
//				idSlave = in.readInt();
//				msg = "Slave Ready: " + idSlave + " total: [";
//				slaveReady.put(idSlave, idSlave);
//				for (int i : slaveReady.values()) {
//					msg += "" + i + ",";
//				}
//				break;
//			default:
//				msg = "" + typeMsg;
//				break;
//			}
//			if (!msg.isEmpty())
//				System.out.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": " + msg);
//		} else {
//			System.out
//					.println("MasterListener " + FollowMeParam.posMaster + "<--" + idSender + ": ignorado " + typeMsg);
//
//		}
//	}
}
