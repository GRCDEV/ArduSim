package followme.logic;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import followme.logic.FollowMeParam.FollowMeState;
import followme.pojo.Nodo;
import followme.pojo.RecursoCompartido;

public class MasterMando extends Thread {

	private RecursoCompartido recurso = null;
	private int type = 1;
	

	public void run() {
		
		// espera al inicio del experimento

		while (!Tools.isExperimentInProgress()) {
			Tools.waiting(500);
		}

		int counter = 0;

		recurso = FollowMeParam.recurso.get();
		Copter.setFlightMode(FollowMeParam.posMaster, FlightMode.LOITER);
		System.out.println("Inicio PrintTh");
		long tfin = 0;
		long tSysIni = System.nanoTime();
		Nodo n = recurso.pop(type);
//		long tfinExperimento = tSysIni + (FollowMeParam.TiempoMaxExperimento  * 1000000);
//
//		do {
//			Copter.channelsOverride(FollowMeParam.posMaster, n.chan1, n.chan2, n.chan3, n.chan4);
//			n = recurso.pop(type);
//			long espera = (n.time - (System.nanoTime() - tSysIni)) / 1000000;
//			 if (espera > 0) {
//				 Tools.waiting((int)espera);
//			 }
//			if (n.next == null || System.nanoTime() > tfinExperimento) {
//				tfin = n.time;
//				Copter.setFlightMode(FollowMeParam.posMaster, FlightMode.LAND_ARMED);
//				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.LANDING_MASTER;
//				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
//				
//				//GUI.log("Mode Master " + FlightMode.LAND_ARMED);
//				break;
//			}
//		} while (n != null || n.next != null);
		
		
		
		do {
			Copter.channelsOverride(FollowMeParam.posMaster, n.chan1, n.chan2, n.chan3, n.chan4);
			n = recurso.pop(type);
			long espera = (n.time - (System.nanoTime() - tSysIni)) / 1000000;
			 if (espera > 0) {
				 Tools.waiting((int)espera);
			 }
			if (n.next == null) {
				tfin = n.time;
				Copter.setFlightMode(FollowMeParam.posMaster, FlightMode.LAND_ARMED);
				FollowMeParam.uavs[FollowMeParam.posMaster] = FollowMeState.LANDING_MASTER;
				GUI.updateprotocolState(FollowMeParam.posMaster, FollowMeParam.uavs[FollowMeParam.posMaster].getName());
				
				//GUI.log("Mode Master " + FlightMode.LAND_ARMED);
				break;
			}
		} while (n != null || n.next != null);
		
		
		
		System.out.println("T final:" + ((System.nanoTime() - tSysIni) / 1000000) + " \t fin-ini: " + tfin / 1000000);
		
	}

}