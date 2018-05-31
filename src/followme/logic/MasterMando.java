package followme.logic;

import api.API;
import api.SwarmHelper;
import followme.logic.FollowMeParam.FollowMeState;
import main.Param;
import main.Param.SimulatorState;
import uavController.UAVParam.Mode;

public class MasterMando extends Thread {

	private RecursoCompartido recurso = null;
	private int type = 1;
	

	public void run() {
		
		// espera al inicio del experimento

		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		SwarmHelper.log("Iniciado SendTh");
		while (FollowMeParam.recurso.get() == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		int counter = 0;

		recurso = FollowMeParam.recurso.get();
		System.out.println("Inicio PrintTh");
		Nodo n = null;
		long timeAnterior = -1;
		long t = 0;
		long tini = 0;
		long tfin = 0;
		long tSysIni = System.nanoTime();

		//// pasar a loiter
		// if(!(API.setMode(0, Mode.LOITER))){
		// SwarmHelper.log("Error cambiar modo Loiter!");
		// }
		//// armar motores
		// if(!(API.armEngines(0))) {
		// SwarmHelper.log("Error armar motores");
		// }

		while (Param.simStatus != SimulatorState.TEST_IN_PROGRESS) {
			try {
				Thread.sleep(1000);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		do {
			n = recurso.pop(type);
			if (timeAnterior == -1) {
				timeAnterior = n.time;
				tini = n.time;
			}

			long espera = (n.time - (System.nanoTime() - tSysIni)) / 1000000;
			// timeAnterior = n.time;
			t += espera;
			// System.out.println(" Tiempo de espera: " + espera + " miliseg");
			if (espera > 0) {
				try {
					Thread.sleep(espera);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// System.out.println("Consumer #" + this.number + " pop: " + n.msg);

			if (type != 1) {
				n.print();
			} else {
				API.channelsOverride(FollowMeParam.posMaster, n.chan1, n.chan2, n.chan3, n.chan4);
			}
			// System.out.println(n.time);

			if (n.next == null) {
				tfin = n.time;
				API.setMode(FollowMeParam.posMaster, Mode.LAND_ARMED);
				SwarmHelper.log("Mode Master " + Mode.LAND_ARMED);
				break;
			}
		} while (n != null || n.next != null);
		System.out.println(
				"T fianl:" + ((System.nanoTime() - tSysIni) / 1000000) + " \t fin-ini: " + (tfin - tini) / 1000000);
	}

}
