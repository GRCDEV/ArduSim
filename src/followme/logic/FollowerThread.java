package followme.logic;

import com.esotericsoftware.kryo.io.Input;

import api.API;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.UAVCurrentData;
import main.Param;
import uavController.UAVControllerThread;
import uavController.UAVParam;
import uavController.UAVParam.Mode;

public class FollowerThread extends Thread {

	private int numUAV;

	@SuppressWarnings("unused")
	private FollowerThread() {
	}

	public FollowerThread(int numUAV) {
		this.numUAV = numUAV;
	}

	@Override
	public void run() {
		byte[] message;
		Input in = new Input();
		SwarmHelper.log("Iniciando Follower " + numUAV);

		API.setMode(numUAV, Mode.GUIDED);
		boolean fly = false;
		while (true) {
			message = API.receiveMessage(numUAV);
			in.setBuffer(message);
			int num = in.readInt();
			double latitud = in.readDouble();
			double longitud = in.readDouble();
			double z = in.readDouble();
			System.out.println("Recibido" + numUAV + "-->UAV" + num + "\t" + latitud + "\t" + longitud + "\t" + z);
			if (z > 2.0 && !fly) {
				fly = true;
			}
			GeoCoordinates geo = new GeoCoordinates(latitud, longitud);
			float relAltitude = (float) z;
			if (fly) {
				
//				boolean ok = API.moveUAV(num, geo, relAltitude, 1, 1);
//				Param.controllers[numUAV].msgTarget(mode, latitude, longitude, altitude, yaw, setYaw, speedX, speedY, speedZ);
//				System.out.println("MoveUAV" + numUAV+" "+ok);
			}
		}

	}
}
